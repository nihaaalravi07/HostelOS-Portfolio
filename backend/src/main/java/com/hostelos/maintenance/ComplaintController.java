package com.hostelos.maintenance;

import com.hostelos.maintenance.ComplaintService.ComplaintDto;
import com.hostelos.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/maintenance")
public class ComplaintController {
    public record SubmitRequest(@NotNull ComplaintCategory category, @NotNull ComplaintPriority priority,
                                @NotBlank @Size(max = 500) String description) {}
    public record AssignRequest(@NotNull Long staffId) {}
    public record StatusRequest(@NotNull ComplaintStatus status, @Size(max = 500) String notes) {}

    private final ComplaintService service;

    public ComplaintController(ComplaintService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ComplaintDto submit(@Valid @RequestBody SubmitRequest r) { return service.submit(r.category(), r.priority(), r.description()); }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public List<ComplaintDto> mine() { return service.mine(); }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('STUDENT')")
    public ComplaintDto close(@PathVariable Long id) { return service.closeOwn(id); }

    @GetMapping("/board")
    @PreAuthorize("hasAnyRole('MAINTENANCE_STAFF','WARDEN','ADMIN')")
    public List<ComplaintDto> board() { return service.board(CurrentUser.get()); }

    @PostMapping("/{id}/claim")
    @PreAuthorize("hasRole('MAINTENANCE_STAFF')")
    public ComplaintDto claim(@PathVariable Long id) { return service.claim(id, CurrentUser.get()); }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('WARDEN','ADMIN')")
    public ComplaintDto assign(@PathVariable Long id, @Valid @RequestBody AssignRequest r) { return service.assign(id, r.staffId()); }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MAINTENANCE_STAFF','WARDEN','ADMIN')")
    public ComplaintDto status(@PathVariable Long id, @Valid @RequestBody StatusRequest r) {
        return service.updateStatus(id, r.status(), r.notes(), CurrentUser.get());
    }
}
