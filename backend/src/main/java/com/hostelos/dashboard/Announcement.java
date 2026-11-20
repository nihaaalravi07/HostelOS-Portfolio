package com.hostelos.dashboard;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "announcements")
@Getter
@NoArgsConstructor
public class Announcement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false)
    private String body;
    @Column(name = "author_id")
    private Long authorId;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Announcement(String title, String body, Long authorId, LocalDateTime createdAt) {
        this.title = title;
        this.body = body;
        this.authorId = authorId;
        this.createdAt = createdAt;
    }
}
