package com.hostelos.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(name = "full_name", nullable = false)
    private String fullName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    private boolean active = true;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public User(String email, String passwordHash, String fullName, Role role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
    }
}
