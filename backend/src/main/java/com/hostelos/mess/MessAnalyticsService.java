package com.hostelos.mess;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Numbers for the admin: demand, revenue, ratings. Everything is computed from real bookings and feedback. */
@Service
@Transactional(readOnly = true)
public class MessAnalyticsService {
    public record DayTrend(LocalDate date, long expected, long consumed, long cancelled, long missed, BigDecimal revenue) {}

    public record CategoryStat(MealCategory category, String label, long expected, long consumed, BigDecimal revenue,
                               Double avgTaste, Double avgHygiene, long reviews) {}

    public record MealRank(Long mealId, LocalDate date, String categoryLabel, String menu, long consumed,
                           Double avgTaste, Double avgHygiene, long reviews) {}

    public record RatingBucket(int rating, long taste, long hygiene) {}

    public record Totals(long expected, long consumed, long cancelled, long missed, BigDecimal revenue,
                         Double avgTaste, Double avgHygiene, long reviews) {}

    public record Analytics(LocalDate from, LocalDate to, Totals totals, List<DayTrend> trend, List<CategoryStat> categories,
                            List<MealRank> popularMeals, List<MealRank> lowestRatedMeals, List<RatingBucket> ratingDistribution) {}

    private final MealRepository meals;
    private final MealBookingRepository bookings;
    private final FoodFeedbackRepository feedback;

    public MessAnalyticsService(MealRepository meals, MealBookingRepository bookings, FoodFeedbackRepository feedback) {
        this.meals = meals;
        this.bookings = bookings;
        this.feedback = feedback;
    }

    public Analytics analytics(LocalDate from, LocalDate to) {
        List<Meal> inRange = meals.findByMealDateBetween(from, to);

        Map<Long, Map<BookingStatus, Long>> counts = new HashMap<>();
        for (Object[] r : bookings.countsByMealAndStatus(from, to))
            counts.computeIfAbsent((Long) r[0], k -> new EnumMap<>(BookingStatus.class)).put((BookingStatus) r[1], (Long) r[2]);

        Map<Long, BigDecimal> revenueByMeal = new HashMap<>();
        for (Object[] r : bookings.revenueByMeal(from, to)) revenueByMeal.put((Long) r[0], (BigDecimal) r[1]);

        List<FoodFeedback> reviews = feedback.inRange(from, to);
        Map<Long, List<FoodFeedback>> reviewsByMeal = reviews.stream().collect(Collectors.groupingBy(f -> f.getMeal().getId()));

        // ---- daily trend + category stats
        Map<LocalDate, long[]> day = new TreeMap<>();                 // expected, consumed, cancelled, missed
        Map<LocalDate, BigDecimal> dayRevenue = new HashMap<>();
        Map<MealCategory, long[]> cat = new EnumMap<>(MealCategory.class); // expected, consumed
        Map<MealCategory, BigDecimal> catRevenue = new EnumMap<>(MealCategory.class);
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) day.put(d, new long[4]);
        for (Meal m : inRange) {
            Map<BookingStatus, Long> c = counts.getOrDefault(m.getId(), Map.of());
            long consumed = c.getOrDefault(BookingStatus.CONSUMED, 0L);
            long expected = consumed + c.getOrDefault(BookingStatus.BOOKED, 0L) + c.getOrDefault(BookingStatus.MISSED, 0L);
            long[] d = day.get(m.getMealDate());
            d[0] += expected;
            d[1] += consumed;
            d[2] += c.getOrDefault(BookingStatus.CANCELLED, 0L);
            d[3] += c.getOrDefault(BookingStatus.MISSED, 0L);
            BigDecimal rev = revenueByMeal.getOrDefault(m.getId(), BigDecimal.ZERO);   // what was actually billed
            dayRevenue.merge(m.getMealDate(), rev, BigDecimal::add);
            long[] cc = cat.computeIfAbsent(m.getCategory(), k -> new long[2]);
            cc[0] += expected;
            cc[1] += consumed;
            catRevenue.merge(m.getCategory(), rev, BigDecimal::add);
        }
        BigDecimal exactRevenue = bookings.revenue(from, to);

        List<DayTrend> trend = day.entrySet().stream().map(e -> new DayTrend(e.getKey(), e.getValue()[0], e.getValue()[1],
                e.getValue()[2], e.getValue()[3], dayRevenue.getOrDefault(e.getKey(), BigDecimal.ZERO))).toList();

        List<CategoryStat> categories = new ArrayList<>();
        for (MealCategory mc : MealCategory.values()) {
            List<FoodFeedback> catReviews = reviews.stream().filter(f -> f.getMeal().getCategory() == mc).toList();
            long[] cc = cat.getOrDefault(mc, new long[2]);
            categories.add(new CategoryStat(mc, mc.getLabel(), cc[0], cc[1], catRevenue.getOrDefault(mc, BigDecimal.ZERO),
                    avg(catReviews, true), avg(catReviews, false), catReviews.size()));
        }

        // ---- meal rankings
        List<MealRank> ranks = inRange.stream().map(m -> {
            List<FoodFeedback> rv = reviewsByMeal.getOrDefault(m.getId(), List.of());
            return new MealRank(m.getId(), m.getMealDate(), m.getCategory().getLabel(), String.join(", ", m.menuList()),
                    counts.getOrDefault(m.getId(), Map.of()).getOrDefault(BookingStatus.CONSUMED, 0L),
                    avg(rv, true), avg(rv, false), rv.size());
        }).toList();
        List<MealRank> popular = ranks.stream().filter(r -> r.consumed() > 0)
                .sorted(Comparator.comparingLong(MealRank::consumed).reversed()).limit(5).toList();
        List<MealRank> lowest = ranks.stream().filter(r -> r.reviews() >= 2)
                .sorted(Comparator.comparingDouble(MealRank::avgTaste)).limit(5).toList();

        // ---- rating distribution
        List<RatingBucket> dist = new ArrayList<>();
        for (int star = 1; star <= 5; star++) {
            final int s = star;
            dist.add(new RatingBucket(s, reviews.stream().filter(f -> f.getTasteRating() == s).count(),
                    reviews.stream().filter(f -> f.getHygieneRating() == s).count()));
        }

        Totals totals = new Totals(trend.stream().mapToLong(DayTrend::expected).sum(),
                trend.stream().mapToLong(DayTrend::consumed).sum(), trend.stream().mapToLong(DayTrend::cancelled).sum(),
                trend.stream().mapToLong(DayTrend::missed).sum(), exactRevenue, avg(reviews, true), avg(reviews, false),
                reviews.size());
        return new Analytics(from, to, totals, trend, categories, popular, lowest, dist);
    }

    private static Double avg(List<FoodFeedback> list, boolean taste) {
        if (list.isEmpty()) return null;
        double v = list.stream().mapToInt(f -> taste ? f.getTasteRating() : f.getHygieneRating()).average().orElse(0);
        return MealService.round1(v);
    }
}
