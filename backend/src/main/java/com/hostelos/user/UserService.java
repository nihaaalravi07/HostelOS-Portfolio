package com.hostelos.user;

import com.hostelos.common.BusinessException;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The only entry point other modules use to read or create users. */
@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public User require(Long id) {
        return users.findById(id).orElseThrow(() -> BusinessException.notFound("User not found"));
    }

    public Map<Long, String> namesByIds(Collection<Long> ids) {
        Set<Long> unique = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (unique.isEmpty()) return Map.of();
        return users.findAllById(unique).stream().collect(Collectors.toMap(User::getId, User::getFullName));
    }

    public String nameOf(Long id) {
        return id == null ? null : namesByIds(List.of(id)).get(id);
    }

    public List<User> byRole(Role role) { return users.findByRoleOrderByFullName(role); }

    public List<User> all() { return users.findAll(); }

    public long count() { return users.count(); }

    public boolean emailExists(String email) { return users.findByEmailIgnoreCase(email).isPresent(); }

    public Optional<User> findByEmail(String email) { return users.findByEmailIgnoreCase(email); }

    public boolean matches(String raw, User user) { return encoder.matches(raw, user.getPasswordHash()); }

    @Transactional
    public User create(String email, String rawPassword, String fullName, Role role) {
        if (emailExists(email)) throw BusinessException.conflict("Email already registered");
        return users.save(new User(email.toLowerCase(), encoder.encode(rawPassword), fullName, role));
    }
}
