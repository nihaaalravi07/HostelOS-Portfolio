package com.hostelos.mess;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Keeps a rolling window of meals in the database so students always have upcoming menus to browse. */
@Service
public class MealScheduleService {
    private final MealRepository meals;
    private final int cutoffMinutes;
    private final boolean demoOpenToday;
    private final boolean demoMode;

    public MealScheduleService(MealRepository meals,
                               @Value("${hostelos.booking-cutoff-minutes}") int cutoffMinutes,
                               @Value("${hostelos.demo-open-today}") boolean demoOpenToday,
                               @Value("${hostelos.demo-mode}") boolean demoMode) {
        this.meals = meals;
        this.cutoffMinutes = cutoffMinutes;
        this.demoOpenToday = demoOpenToday;
        this.demoMode = demoMode;
    }

    @Scheduled(cron = "0 10 0 * * *")
    public void scheduledRefresh() { ensureMeals(LocalDate.now().minusDays(1), LocalDate.now().plusDays(7)); }

    /** Creates any missing meals between the two dates (inclusive). Safe to call repeatedly. */
    @Transactional
    public int ensureMeals(LocalDate from, LocalDate to) {
        int created = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            for (MealCategory category : MealCategory.values()) {
                if (meals.existsByMealDateAndCategory(d, category)) continue;
                List<MenuCatalog.Preset> presets = MenuCatalog.PRESETS.get(category);
                MenuCatalog.Preset p = presets.get((int) Math.floorMod(d.toEpochDay() + category.ordinal(), presets.size()));
                meals.save(new Meal(d, category, p.items(), p.priceValue(), cutoffFor(d, category), p.imageUrl()));
                created++;
            }
        }
        return created;
    }

    public LocalDateTime cutoffFor(LocalDate date, MealCategory category) {
        if (demoMode && demoOpenToday && date.equals(LocalDate.now())) return date.atTime(LocalTime.of(23, 59));
        return date.atTime(category.getStart()).minusMinutes(cutoffMinutes);
    }
}
