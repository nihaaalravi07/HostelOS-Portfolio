package com.hostelos.user;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    public record StaffDto(Long id, String fullName, Role role, String email) {}

    private final UserService users;

    public UserController(UserService users) { this.users = users; }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<StaffDto> all() {
        return users.all().stream().map(u -> new StaffDto(u.getId(), u.getFullName(), u.getRole(), u.getEmail())).toList();
    }

    /** Staff members of a given role, used by wardens/admins when assigning work. */
    @GetMapping("/staff")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public List<StaffDto> staff(@RequestParam Role role) {
        return users.byRole(role).stream().map(u -> new StaffDto(u.getId(), u.getFullName(), u.getRole(), u.getEmail())).toList();
    }
}
