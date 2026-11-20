package com.hostelos.resident;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "residents")
@Getter
@NoArgsConstructor
public class Resident {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    @Column(name = "enrollment_no", nullable = false, unique = true)
    private String enrollmentNo;
    private String phone;
    @Column(nullable = false)
    private String course;
    @Column(name = "year_of_study", nullable = false)
    private int yearOfStudy;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResidentStatus status = ResidentStatus.ACTIVE;

    public Resident(Long userId, String enrollmentNo, String phone, String course, int yearOfStudy) {
        this.userId = userId;
        this.enrollmentNo = enrollmentNo;
        this.phone = phone;
        this.course = course;
        this.yearOfStudy = yearOfStudy;
    }

    public boolean isActive() { return status == ResidentStatus.ACTIVE; }

    public void checkOut() { this.status = ResidentStatus.CHECKED_OUT; }
}
