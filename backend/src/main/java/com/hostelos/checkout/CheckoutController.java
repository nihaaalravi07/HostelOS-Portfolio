package com.hostelos.checkout;

import com.hostelos.checkout.CheckoutService.CheckoutDto;
import com.hostelos.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {
    public record PlanBody(@NotNull LocalDate plannedDate) {}
    public record ReviewBody(@Size(max = 300) String note) {}

    private final CheckoutService service;

    public CheckoutController(CheckoutService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public CheckoutDto request(@Valid @RequestBody PlanBody b) { return service.request(b.plannedDate()); }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public List<CheckoutDto> mine() { return service.mine(); }

    @GetMapping
    @PreAuthorize("hasAnyRole('WARDEN','ADMIN')")
    public List<CheckoutDto> all() { return service.all(); }

    @PostMapping("/{id}/clear")
    @PreAuthorize("hasAnyRole('WARDEN','ADMIN')")
    public CheckoutDto clear(@PathVariable Long id, @RequestBody(required = false) ReviewBody b) {
        return service.clear(id, CurrentUser.get().userId(), b == null ? null : b.note());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('WARDEN','ADMIN')")
    public CheckoutDto reject(@PathVariable Long id, @RequestBody(required = false) ReviewBody b) {
        return service.reject(id, CurrentUser.get().userId(), b == null ? null : b.note());
    }
}
