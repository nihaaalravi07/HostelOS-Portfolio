package com.hostelos.mess;

import com.hostelos.mess.MealService.BookingHistoryItem;
import com.hostelos.resident.ResidentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Student-facing meal endpoints. */
@RestController
@RequestMapping("/api/meals")
public class MealController {
    public record FeedbackRequest(@Min(1) @Max(5) int tasteRating, @Min(1) @Max(5) int hygieneRating,
                                  @Size(max = 500) String comment) {}

    private final MealService meals;
    private final ResidentService residents;

    public MealController(MealService meals, ResidentService residents) {
        this.meals = meals;
        this.residents = residents;
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public List<MealDto> menu(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate start = from == null ? LocalDate.now() : from;
        return meals.listForCurrentResident(start, to == null ? start.plusDays(6) : to);
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('STUDENT')")
    public List<BookingHistoryItem> history(@RequestParam(defaultValue = "30") int limit) {
        return meals.history(residents.requireCurrent().getId(), Math.min(limit, 100));
    }

    @PostMapping("/{id}/book")
    @PreAuthorize("hasRole('STUDENT')")
    public MealDto book(@PathVariable Long id) { return meals.book(id); }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('STUDENT')")
    public MealDto cancel(@PathVariable Long id) { return meals.cancel(id); }

    @PostMapping("/{id}/feedback")
    @PreAuthorize("hasRole('STUDENT')")
    public MealDto feedback(@PathVariable Long id, @Valid @RequestBody FeedbackRequest r) {
        return meals.submitFeedback(id, r.tasteRating(), r.hygieneRating(), r.comment());
    }
}
