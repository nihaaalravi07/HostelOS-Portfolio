package com.hostelos.dashboard;

import com.hostelos.dashboard.DashboardService.*;
import com.hostelos.dashboard.StudentDashboard.AnnouncementDto;
import com.hostelos.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DashboardController {
    public record AnnouncementRequest(@NotBlank @Size(max = 150) String title, @NotBlank @Size(max = 600) String body) {}

    private final DashboardService dashboards;

    public DashboardController(DashboardService dashboards) { this.dashboards = dashboards; }

    @GetMapping("/dashboard/student")
    @PreAuthorize("hasRole('STUDENT')")
    public StudentDashboard student() { return dashboards.student(); }

    @GetMapping("/dashboard/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminDashboard admin() { return dashboards.admin(); }

    @GetMapping("/dashboard/warden")
    @PreAuthorize("hasAnyRole('WARDEN','ADMIN')")
    public WardenDashboard warden() { return dashboards.warden(); }

    @GetMapping("/announcements")
    public List<AnnouncementDto> announcements() { return dashboards.recentAnnouncements(20); }

    @PostMapping("/announcements")
    @PreAuthorize("hasAnyRole('WARDEN','ADMIN')")
    public AnnouncementDto announce(@Valid @RequestBody AnnouncementRequest r) {
        return dashboards.announce(r.title(), r.body(), CurrentUser.get().userId());
    }
}
