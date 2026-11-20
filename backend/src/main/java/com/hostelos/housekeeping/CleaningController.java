package com.hostelos.housekeeping;

import com.hostelos.housekeeping.CleaningService.*;
import com.hostelos.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cleaning")
public class CleaningController {
    public record CreateRequest(@NotNull LocalDate date, @NotNull CleaningSlot slot, @Size(max = 300) String instructions) {}
    public record AssignRequest(@NotNull Long staffId) {}
    public record StatusRequest(@NotNull CleaningStatus status, @Size(max = 300) String notes) {}

    private final CleaningService service;

    public CleaningController(CleaningService service) { this.service = service; }

    // ---- student ----
    @GetMapping("/slots")
    @PreAuthorize("hasRole('STUDENT')")
    public List<SlotAvailability> slots(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.slots(date);
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public CleaningDto create(@Valid @RequestBody CreateRequest r) { return service.request(r.date(), r.slot(), r.instructions()); }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public List<CleaningDto> mine() { return service.mine(); }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('STUDENT')")
    public CleaningDto cancel(@PathVariable Long id) { return service.cancel(id); }

    // ---- staff / warden / admin ----
    @GetMapping("/board")
    @PreAuthorize("hasAnyRole('HOUSEKEEPING_STAFF','WARDEN','ADMIN')")
    public List<CleaningDto> board(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate start = from == null ? LocalDate.now() : from;
        return service.board(start, to == null ? start.plusDays(7) : to, CurrentUser.get());
    }

    @PostMapping("/{id}/claim")
    @PreAuthorize("hasRole('HOUSEKEEPING_STAFF')")
    public CleaningDto claim(@PathVariable Long id) { return service.claim(id, CurrentUser.get()); }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('WARDEN','ADMIN')")
    public CleaningDto assign(@PathVariable Long id, @Valid @RequestBody AssignRequest r) { return service.assign(id, r.staffId()); }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HOUSEKEEPING_STAFF','WARDEN','ADMIN')")
    public CleaningDto status(@PathVariable Long id, @Valid @RequestBody StatusRequest r) {
        return service.updateStatus(id, r.status(), r.notes(), CurrentUser.get());
    }
}
