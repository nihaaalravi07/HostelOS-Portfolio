package com.hostelos.mess;

import com.hostelos.mess.MessAnalyticsService.Analytics;
import com.hostelos.mess.MessOperationsService.*;
import com.hostelos.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Mess-staff and admin endpoints. */
@RestController
@RequestMapping("/api/mess")
public class MessController {
    public record UpdateMealRequest(@NotBlank String menuItems, @NotNull BigDecimal price, LocalDateTime cutoffTime, boolean available) {}

    private final MessOperationsService operations;
    private final MessAnalyticsService analytics;
    private final MealService meals;

    public MessController(MessOperationsService operations, MessAnalyticsService analytics, MealService meals) {
        this.operations = operations;
        this.analytics = analytics;
        this.meals = meals;
    }

    @GetMapping("/board")
    @PreAuthorize("hasAnyRole('MESS_STAFF','ADMIN')")
    public DayBoard board(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return operations.board(date == null ? LocalDate.now() : date);
    }

    @PostMapping("/bookings/{id}/confirm")
    @PreAuthorize("hasAnyRole('MESS_STAFF','ADMIN')")
    public BookingRow confirm(@PathVariable Long id) {
        return operations.confirmConsumption(id, CurrentUser.get().userId());
    }

    @PostMapping("/meals/{id}/close-no-shows")
    @PreAuthorize("hasAnyRole('MESS_STAFF','ADMIN')")
    public Map<String, Integer> closeNoShows(@PathVariable Long id) {
        return Map.of("missed", operations.markMissed(id));
    }

    @PutMapping("/meals/{id}")
    @PreAuthorize("hasAnyRole('MESS_STAFF','ADMIN')")
    public MealDto updateMeal(@PathVariable Long id, @Valid @RequestBody UpdateMealRequest r) {
        return operations.updateMeal(id, r.menuItems(), r.price(), r.cutoffTime(), r.available());
    }

    @GetMapping("/feedback")
    @PreAuthorize("hasAnyRole('MESS_STAFF','ADMIN')")
    public List<FeedbackRow> feedback(@RequestParam(defaultValue = "50") int limit) {
        return operations.recentFeedback(Math.min(limit, 200));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('MESS_STAFF','ADMIN')")
    public Analytics analytics(@RequestParam(defaultValue = "14") int days) {
        LocalDate today = LocalDate.now();
        return analytics.analytics(today.minusDays(Math.max(1, Math.min(days, 90)) - 1), today);
    }

    /** Upcoming and recent meals for menu management (staff see all, with prices). */
    @GetMapping("/menu")
    @PreAuthorize("hasAnyRole('MESS_STAFF','ADMIN')")
    public List<MealDto> menu(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate start = from == null ? LocalDate.now() : from;
        return meals.list(start, to == null ? start.plusDays(6) : to, null);
    }
}
