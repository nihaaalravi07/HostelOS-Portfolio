package com.hostelos.user;

import com.hostelos.common.BusinessException;
import com.hostelos.security.CurrentUser;
import com.hostelos.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    public record UserDto(Long id, String email, String fullName, Role role) {}
    public record LoginResponse(String token, UserDto user) {}
    public record DemoAccount(String email, String password, String fullName, Role role) {}

    private final UserService users;
    private final JwtService jwt;
    private final boolean demoMode;
    private final String demoPassword;

    public AuthController(UserService users, JwtService jwt,
                          @Value("${hostelos.demo-mode}") boolean demoMode,
                          @Value("${hostelos.demo-password}") String demoPassword) {
        this.users = users;
        this.jwt = jwt;
        this.demoMode = demoMode;
        this.demoPassword = demoPassword;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        User user = users.findByEmail(req.email().trim())
                .filter(u -> u.isActive() && users.matches(req.password(), u))
                .orElseThrow(() -> new BusinessException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"));
        return new LoginResponse(jwt.issue(user), toDto(user));
    }

    @GetMapping("/me")
    public UserDto me() {
        return toDto(users.require(CurrentUser.get().userId()));
    }

    /** Demo convenience: lists one account per role so the login screen can offer quick sign-in. */
    @GetMapping("/demo-accounts")
    public List<DemoAccount> demoAccounts() {
        if (!demoMode) return List.of();
        List<String> emails = List.of("aarav.sharma@iiita.ac.in", "priya.verma@iiita.ac.in", "admin@hostelos.in",
                "warden@hostelos.in", "mess@hostelos.in", "housekeeping@hostelos.in", "maintenance@hostelos.in");
        return emails.stream().map(users::findByEmail).flatMap(java.util.Optional::stream)
                .map(u -> new DemoAccount(u.getEmail(), demoPassword, u.getFullName(), u.getRole())).toList();
    }

    private UserDto toDto(User u) { return new UserDto(u.getId(), u.getEmail(), u.getFullName(), u.getRole()); }
}
