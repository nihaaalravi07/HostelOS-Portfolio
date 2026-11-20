package com.hostelos.checkout;

import com.hostelos.common.BusinessException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "checkout_requests")
@Getter
@NoArgsConstructor
public class CheckoutRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "resident_id", nullable = false)
    private Long residentId;
    @Column(name = "requested_on", nullable = false)
    private LocalDate requestedOn;
    @Column(name = "planned_date", nullable = false)
    private LocalDate plannedDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckoutStatus status = CheckoutStatus.PENDING;
    @Column(name = "reviewed_by")
    private Long reviewedBy;
    @Column(name = "review_note")
    private String reviewNote;
    @Column(name = "dues_at_review", precision = 12, scale = 2)
    private BigDecimal duesAtReview;

    public CheckoutRequest(Long residentId, LocalDate plannedDate) {
        this.residentId = residentId;
        this.plannedDate = plannedDate;
        this.requestedOn = LocalDate.now();
    }

    public void clear(Long reviewer, String note, BigDecimal dues) { review(CheckoutStatus.CLEARED, reviewer, note, dues); }

    public void reject(Long reviewer, String note, BigDecimal dues) { review(CheckoutStatus.REJECTED, reviewer, note, dues); }

    private void review(CheckoutStatus outcome, Long reviewer, String note, BigDecimal dues) {
        if (status != CheckoutStatus.PENDING) throw BusinessException.conflict("This checkout request was already " + status.name().toLowerCase());
        this.status = outcome;
        this.reviewedBy = reviewer;
        this.reviewNote = note == null || note.isBlank() ? null : note.trim();
        this.duesAtReview = dues;
    }
}
