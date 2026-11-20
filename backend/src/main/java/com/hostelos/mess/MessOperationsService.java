package com.hostelos.mess;

import com.hostelos.common.BusinessException;
import com.hostelos.finance.LedgerService;
import com.hostelos.finance.LedgerService.ChargePosting;
import com.hostelos.finance.MealChargeCalculator;
import com.hostelos.resident.ResidentService;
import com.hostelos.resident.ResidentSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Mess-staff features: the daily board and, above all, confirming consumption (which bills the resident). */
@Service
@Transactional(readOnly = true)
public class MessOperationsService {
    public record BookingRow(Long bookingId, Long residentId, String residentName, String enrollmentNo,
                             BookingStatus status, LocalDateTime bookedAt, LocalDateTime consumedAt, BigDecimal chargedAmount) {}

    public record MealCounts(long booked, long consumed, long cancelled, long missed, long expected, long pending,
                             BigDecimal revenue) {}

    public record MealBoard(MealDto meal, MealCounts counts, List<BookingRow> bookings) {}

    public record DayBoard(LocalDate date, MealCounts totals, List<MealBoard> meals) {}

    public record FeedbackRow(Long id, Long mealId, LocalDate mealDate, String categoryLabel, String menu,
                              String residentName, int tasteRating, int hygieneRating, String comment, LocalDateTime createdAt) {}

    private final MealRepository meals;
    private final MealBookingRepository bookings;
    private final FoodFeedbackRepository feedback;
    private final MealService mealService;
    private final ResidentService residents;
    private final LedgerService ledger;
    private final MealChargeCalculator calculator;

    public MessOperationsService(MealRepository meals, MealBookingRepository bookings, FoodFeedbackRepository feedback,
                                 MealService mealService, ResidentService residents, LedgerService ledger,
                                 MealChargeCalculator calculator) {
        this.meals = meals;
        this.bookings = bookings;
        this.feedback = feedback;
        this.mealService = mealService;
        this.residents = residents;
        this.ledger = ledger;
        this.calculator = calculator;
    }

    // ------------------------------------------------------------------ board

    public DayBoard board(LocalDate date) {
        List<Meal> dayMeals = meals.findByMealDateBetween(date, date);
        List<MealDto> dtos = mealService.toDtos(dayMeals, null);
        Map<Long, List<MealBooking>> byMeal = new HashMap<>();
        for (Meal m : dayMeals) byMeal.put(m.getId(), bookings.findByMealIdOrderByBookedAtAsc(m.getId()));
        Map<Long, ResidentSummary> people = residents.summaries(byMeal.values().stream().flatMap(List::stream)
                .map(MealBooking::getResidentId).toList());

        List<MealBoard> boards = new ArrayList<>();
        for (MealDto dto : dtos) {
            List<MealBooking> rows = byMeal.get(dto.id());
            boards.add(new MealBoard(dto, counts(rows), rows.stream().map(b -> {
                ResidentSummary r = people.get(b.getResidentId());
                return new BookingRow(b.getId(), r.id(), r.fullName(), r.enrollmentNo(), b.getStatus(), b.getBookedAt(),
                        b.getConsumedAt(), b.getChargedAmount());
            }).toList()));
        }
        return new DayBoard(date, counts(byMeal.values().stream().flatMap(List::stream).toList()), boards);
    }

    // ------------------------------------------------------------------ confirmation = billing

    /**
     * Confirm that a resident really ate a booked meal. In ONE database transaction this
     *  1. locks the booking row, so concurrent confirmations queue up,
     *  2. moves it BOOKED -> CONSUMED (the entity rejects cancelled/duplicate/missed bookings),
     *  3. posts exactly one ledger CHARGE keyed by "BOOKING-<id>".
     * If any step fails the whole thing rolls back, so a meal is never "consumed" without being billed.
     */
    @Transactional
    public BookingRow confirmConsumption(Long bookingId, Long staffUserId) {
        return confirmConsumption(bookingId, staffUserId, LocalDateTime.now());
    }

    /** {@code now} is a parameter only so demo seeding can back-date history through this same code path. */
    @Transactional
    public BookingRow confirmConsumption(Long bookingId, Long staffUserId, LocalDateTime now) {
        MealBooking booking = bookings.findByIdForUpdate(bookingId)
                .orElseThrow(() -> BusinessException.notFound("Booking not found"));
        Meal meal = booking.getMeal();
        if (meal.getMealDate().isAfter(now.toLocalDate()))
            throw BusinessException.conflict("This meal is on " + meal.getMealDate() + " and has not been served yet");
        if (!meal.isAvailable())
            throw BusinessException.conflict("This meal was withdrawn and cannot be billed");

        BigDecimal amount = calculator.calculate(new MealChargeCalculator.MealCharge(meal.getPrice()));
        booking.markConsumed(now, staffUserId, amount);
        ledger.postCharge(new ChargePosting(booking.getResidentId(), amount,
                meal.getCategory().getLabel() + " – " + meal.getMealDate() + " (" + String.join(", ", meal.menuList()) + ")",
                LedgerService.MODULE_MESS, "BOOKING-" + booking.getId(), now));
        bookings.flush();

        ResidentSummary r = residents.summary(booking.getResidentId());
        return new BookingRow(booking.getId(), r.id(), r.fullName(), r.enrollmentNo(), booking.getStatus(),
                booking.getBookedAt(), booking.getConsumedAt(), booking.getChargedAmount());
    }

    /** After the meal has ended, unconfirmed bookings are closed as MISSED (no charge). */
    @Transactional
    public int markMissed(Long mealId) {
        Meal meal = mealService.require(mealId);
        LocalDateTime now = LocalDateTime.now();
        if (meal.endsAt().isAfter(now))
            throw BusinessException.conflict("The meal is still being served; no-shows can be closed after " + meal.getCategory().getEnd());
        int n = 0;
        for (MealBooking b : bookings.findByMealIdOrderByBookedAtAsc(mealId)) {
            if (b.getStatus() == BookingStatus.BOOKED) { b.markMissed(); n++; }
        }
        return n;
    }

    /** Housekeeping for the data: yesterday's unconfirmed bookings become MISSED. Runs daily and at startup. */
    @Transactional
    public int closeStaleBookings() {
        List<MealBooking> stale = bookings.findStaleBookings(LocalDate.now());
        stale.forEach(MealBooking::markMissed);
        return stale.size();
    }

    // ------------------------------------------------------------------ menu management

    @Transactional
    public MealDto updateMeal(Long mealId, String menuItems, BigDecimal price, LocalDateTime cutoff, boolean available) {
        Meal meal = mealService.require(mealId);
        if (price.signum() < 0) throw BusinessException.badRequest("Price cannot be negative");
        meal.update(menuItems.trim(), price, cutoff == null ? meal.getCutoffTime() : cutoff, available);
        return mealService.toDtos(List.of(meal), null).get(0);
    }

    // ------------------------------------------------------------------ feedback

    public List<FeedbackRow> recentFeedback(int limit) {
        List<FoodFeedback> rows = feedback.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit));
        Map<Long, ResidentSummary> people = residents.summaries(rows.stream().map(FoodFeedback::getResidentId).toList());
        return rows.stream().map(f -> new FeedbackRow(f.getId(), f.getMeal().getId(), f.getMeal().getMealDate(),
                f.getMeal().getCategory().getLabel(), String.join(", ", f.getMeal().menuList()),
                people.get(f.getResidentId()).fullName(), f.getTasteRating(), f.getHygieneRating(), f.getComment(),
                f.getCreatedAt())).toList();
    }

    // ------------------------------------------------------------------ helpers

    private MealCounts counts(List<MealBooking> rows) {
        Map<BookingStatus, Long> by = rows.stream().collect(Collectors.groupingBy(MealBooking::getStatus, Collectors.counting()));
        long booked = by.getOrDefault(BookingStatus.BOOKED, 0L), consumed = by.getOrDefault(BookingStatus.CONSUMED, 0L),
                cancelled = by.getOrDefault(BookingStatus.CANCELLED, 0L), missed = by.getOrDefault(BookingStatus.MISSED, 0L);
        BigDecimal revenue = rows.stream().filter(b -> b.getStatus() == BookingStatus.CONSUMED)
                .map(MealBooking::getChargedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new MealCounts(booked, consumed, cancelled, missed, booked + consumed + missed, booked, revenue);
    }
}
