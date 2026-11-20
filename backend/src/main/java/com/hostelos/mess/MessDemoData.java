package com.hostelos.mess;

import com.hostelos.finance.LedgerService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Component;

/**
 * Deterministic demo history (fixed random seed). Consumption goes through the real
 * confirmConsumption() path, so the ledger charges in the demo are produced by production code.
 */
@Component
public class MessDemoData {
    private static final Map<MealCategory, Double> BASE_DEMAND = Map.of(
            MealCategory.BREAKFAST, 0.45, MealCategory.LUNCH, 0.8, MealCategory.SNACKS, 0.5, MealCategory.DINNER, 0.85);
    private static final String[] GOOD = {"Really tasty today", "Fresh and hot, loved it", "Best meal this week", "Good portion size", "Spot on, please repeat this menu"};
    private static final String[] OKAY = {"Decent but a bit bland", "Average. Rice was slightly undercooked", "Okay for the price", "Could use more variety"};
    private static final String[] BAD = {"Too oily and cold when served", "Dal was watery", "Not worth the price today", "Roti was hard and the sabzi was stale"};
    private static final String[] HYGIENE_BAD = {"Plates were not clean", "Serving area needs cleaning", "Found a hair in the food"};

    private final MealRepository meals;
    private final MealBookingRepository bookings;
    private final FoodFeedbackRepository feedback;
    private final MessOperationsService operations;

    public MessDemoData(MealRepository meals, MealBookingRepository bookings, FoodFeedbackRepository feedback,
                        MessOperationsService operations) {
        this.meals = meals;
        this.bookings = bookings;
        this.feedback = feedback;
        this.operations = operations;
    }

    /** @param demoResidents residents we keep clear of today's bookings so the live demo starts clean. */
    public void seed(List<Long> residentIds, Set<Long> demoResidents, Long staffUserId) {
        Random rnd = new Random(42);
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        List<Meal> all = new ArrayList<>(meals.findByMealDateBetween(today.minusDays(14), today.plusDays(2)));
        all.sort(Comparator.comparing(Meal::getMealDate).thenComparing(m -> m.getCategory().ordinal()));

        for (Meal meal : all) {
            LocalDate d = meal.getMealDate();
            double demand = Math.min(0.95, BASE_DEMAND.get(meal.getCategory()) * (0.7 + 0.1 * quality(meal)));
            for (Long rid : residentIds) {
                boolean demoResident = demoResidents.contains(rid);
                if (d.isEqual(today) && demoResident) continue;
                if (d.isAfter(today) && !demoResident && rnd.nextDouble() > 0.4) continue;     // sparse advance bookings
                if (rnd.nextDouble() > demand) continue;
                MealBooking b = bookings.save(MealBooking.create(meal, rid, meal.getMealDate().atStartOfDay().minusHours(2)));
                double outcome = rnd.nextDouble();
                if (d.isAfter(today)) continue;                                                 // future: stays BOOKED
                if (d.isBefore(today)) {
                    if (outcome < 0.08) { b.cancel(b.getBookedAt().plusHours(1)); bookings.save(b); }
                    else if (outcome < 0.14) { b.markMissed(); bookings.save(b); }
                    else consume(b, meal, rnd, staffUserId, rid);
                } else {                                                                        // today: depends on the clock
                    if (outcome < 0.07) { b.cancel(now.minusHours(1)); bookings.save(b); }
                    else if (meal.endsAt().isBefore(now) && outcome < 0.85) consume(b, meal, rnd, staffUserId, rid);
                    // otherwise stays BOOKED: a pending confirmation for the mess staff
                }
            }
        }
    }

    private void consume(MealBooking b, Meal meal, Random rnd, Long staffUserId, Long residentId) {
        LocalDateTime at = meal.startsAt().plusMinutes(10 + rnd.nextInt(60));
        operations.confirmConsumption(b.getId(), staffUserId, at);
        if (rnd.nextDouble() < 0.55) {
            int taste = clamp(Math.round(quality(meal) + rnd.nextGaussian() * 0.8));
            int hygiene = clamp(Math.round(hygieneBase(meal) + rnd.nextGaussian() * 0.6));
            String[] pool = taste >= 4 ? GOOD : taste == 3 ? OKAY : BAD;
            String comment = hygiene <= 2 ? HYGIENE_BAD[rnd.nextInt(HYGIENE_BAD.length)]
                    : rnd.nextDouble() < 0.6 ? pool[rnd.nextInt(pool.length)] : null;
            feedback.save(new FoodFeedback(meal, residentId, taste, hygiene, comment, at.plusHours(1)));
        }
    }

    /** A stable "how good is this menu" score in [2.6, 4.6], derived from the dishes. */
    private static double quality(Meal m) { return 2.6 + (Math.abs(m.getMenuItems().hashCode()) % 21) / 10.0; }

    private static double hygieneBase(Meal m) { return 3.2 + (Math.abs((m.getMenuItems() + "#h").hashCode()) % 15) / 10.0; }

    private static int clamp(long v) { return (int) Math.max(1, Math.min(5, v)); }
}
