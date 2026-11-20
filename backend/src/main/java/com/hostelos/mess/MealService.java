package com.hostelos.mess;

import com.hostelos.common.BusinessException;
import com.hostelos.resident.Resident;
import com.hostelos.resident.ResidentService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Student-facing meal features: menus, booking, cancelling, history and feedback. */
@Service
@Transactional(readOnly = true)
public class MealService {
    public record BookingHistoryItem(Long bookingId, MealDto meal, BookingStatus status, LocalDateTime bookedAt,
                                     LocalDateTime consumedAt, BigDecimal chargedAmount) {}

    private final MealRepository meals;
    private final MealBookingRepository bookings;
    private final FoodFeedbackRepository feedback;
    private final ResidentService residents;

    public MealService(MealRepository meals, MealBookingRepository bookings, FoodFeedbackRepository feedback,
                       ResidentService residents) {
        this.meals = meals;
        this.bookings = bookings;
        this.feedback = feedback;
        this.residents = residents;
    }

    // ------------------------------------------------------------------ reading

    /** Meals in a date range ordered chronologically. residentId may be null (staff view). */
    public List<MealDto> list(LocalDate from, LocalDate to, Long residentId) {
        return toDtos(meals.findByMealDateBetween(from, to), residentId);
    }

    public List<MealDto> listForCurrentResident(LocalDate from, LocalDate to) {
        return list(from, to, residents.requireCurrent().getId());
    }

    public List<BookingHistoryItem> history(Long residentId, int limit) {
        List<MealBooking> rows = bookings.findByResidentIdOrderByBookedAtDesc(residentId, PageRequest.of(0, limit));
        Map<Long, MealDto> dtoById = toDtos(rows.stream().map(MealBooking::getMeal).toList(), residentId).stream()
                .collect(Collectors.toMap(MealDto::id, d -> d));
        return rows.stream().map(b -> new BookingHistoryItem(b.getId(), dtoById.get(b.getMeal().getId()), b.getStatus(),
                b.getBookedAt(), b.getConsumedAt(), b.getChargedAmount())).toList();
    }

    public long consumedInMonth(Long residentId, YearMonth month) {
        return bookings.countByResidentIdAndStatusAndMealMealDateBetween(residentId, BookingStatus.CONSUMED,
                month.atDay(1), month.atEndOfMonth());
    }

    public long upcomingBookedCount(Long residentId) {
        return bookings.findByResidentIdOrderByBookedAtDesc(residentId, PageRequest.of(0, 200)).stream()
                .filter(b -> b.getStatus() == BookingStatus.BOOKED && !b.getMeal().getMealDate().isBefore(LocalDate.now())).count();
    }

    // ------------------------------------------------------------------ commands

    @Transactional
    public MealDto book(Long mealId) {
        Resident resident = residents.requireCurrent();
        if (!resident.isActive()) throw BusinessException.conflict("Checked-out residents cannot book meals");
        Meal meal = require(mealId);
        LocalDateTime now = LocalDateTime.now();
        if (!meal.isAvailable()) throw BusinessException.conflict("This meal is not available");
        if (!meal.isBookingOpen(now)) throw BusinessException.conflict("Booking for this meal has closed");
        Optional<MealBooking> existing = bookings.findByMealIdAndResidentId(mealId, resident.getId());
        if (existing.isPresent()) existing.get().rebook(now);
        else bookings.save(MealBooking.create(meal, resident.getId(), now));
        bookings.flush();
        return toDtos(List.of(meal), resident.getId()).get(0);
    }

    @Transactional
    public MealDto cancel(Long mealId) {
        Resident resident = residents.requireCurrent();
        Meal meal = require(mealId);
        MealBooking booking = bookings.findByMealIdAndResidentId(mealId, resident.getId())
                .orElseThrow(() -> BusinessException.conflict("You have not booked this meal"));
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(meal.getCutoffTime())) throw BusinessException.conflict("The cancellation cutoff has passed");
        booking.cancel(now);
        return toDtos(List.of(meal), resident.getId()).get(0);
    }

    /** Only residents who actually ate the meal may review it. */
    @Transactional
    public MealDto submitFeedback(Long mealId, int taste, int hygiene, String comment) {
        Resident resident = residents.requireCurrent();
        Meal meal = require(mealId);
        MealBooking booking = bookings.findByMealIdAndResidentId(mealId, resident.getId()).orElse(null);
        if (booking == null || booking.getStatus() != BookingStatus.CONSUMED)
            throw BusinessException.conflict("You can only review meals you have eaten");
        String text = comment == null || comment.isBlank() ? null : comment.trim();
        LocalDateTime now = LocalDateTime.now();
        feedback.findByMealIdAndResidentId(mealId, resident.getId()).ifPresentOrElse(
                f -> f.revise(taste, hygiene, text, now),
                () -> feedback.save(new FoodFeedback(meal, resident.getId(), taste, hygiene, text, now)));
        feedback.flush();
        return toDtos(List.of(meal), resident.getId()).get(0);
    }

    // ------------------------------------------------------------------ shared mapping

    public Meal require(Long id) {
        return meals.findById(id).orElseThrow(() -> BusinessException.notFound("Meal not found"));
    }

    public List<MealDto> toDtos(List<Meal> input, Long residentId) {
        if (input.isEmpty()) return List.of();
        LocalDateTime now = LocalDateTime.now();
        List<Long> ids = input.stream().map(Meal::getId).toList();
        Map<Long, MealBooking> mine = residentId == null ? Map.of()
                : bookings.findByMealIdInAndResidentId(ids, residentId).stream().collect(Collectors.toMap(b -> b.getMeal().getId(), b -> b));
        Map<Long, FoodFeedback> myFeedback = residentId == null ? Map.of()
                : feedback.findByMealIdInAndResidentId(ids, residentId).stream().collect(Collectors.toMap(f -> f.getMeal().getId(), f -> f));
        Map<Long, Object[]> averages = feedback.averagesForMeals(ids).stream().collect(Collectors.toMap(r -> (Long) r[0], r -> r));

        return input.stream().sorted(Comparator.comparing(Meal::getMealDate).thenComparing(m -> m.getCategory().ordinal()))
                .map(m -> {
                    MealBooking b = mine.get(m.getId());
                    FoodFeedback f = myFeedback.get(m.getId());
                    Object[] avg = averages.get(m.getId());
                    return new MealDto(m.getId(), m.getMealDate(), m.getCategory(), m.getCategory().getLabel(),
                            m.getCategory().getStart(), m.getCategory().getEnd(), m.menuList(), m.getPrice(),
                            m.getCutoffTime(), m.isBookingOpen(now), m.isAvailable(), m.getImageUrl(),
                            b == null ? BookingStatus.NOT_BOOKED : b.getStatus(), b == null ? null : b.getId(),
                            f == null ? null : new MealDto.MyFeedback(f.getTasteRating(), f.getHygieneRating(), f.getComment()),
                            avg == null ? null : round1((Double) avg[1]), avg == null ? null : round1((Double) avg[2]),
                            avg == null ? 0 : (Long) avg[3]);
                }).toList();
    }

    static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
}
