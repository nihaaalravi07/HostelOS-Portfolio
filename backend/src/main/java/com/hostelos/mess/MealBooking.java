package com.hostelos.mess;

import com.hostelos.common.BusinessException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A resident's participation in one meal. The status moves only through the methods below, which
 * enforce the lifecycle:
 *
 *   BOOKED -> CANCELLED (before cutoff) -> BOOKED (re-book)
 *   BOOKED -> CONSUMED  (staff confirmation, creates the charge)
 *   BOOKED -> MISSED    (meal over, nobody confirmed)
 */
@Entity
@Table(name = "meal_bookings")
@Getter
@NoArgsConstructor
public class MealBooking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "meal_id")
    private Meal meal;
    @Column(name = "resident_id", nullable = false)
    private Long residentId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;
    @Column(name = "booked_at", nullable = false)
    private LocalDateTime bookedAt;
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;
    @Column(name = "confirmed_by")
    private Long confirmedBy;
    @Column(name = "charged_amount", precision = 12, scale = 2)
    private BigDecimal chargedAmount;

    private MealBooking(Meal meal, Long residentId, LocalDateTime now) {
        this.meal = meal;
        this.residentId = residentId;
        this.status = BookingStatus.BOOKED;
        this.bookedAt = now;
    }

    public static MealBooking create(Meal meal, Long residentId, LocalDateTime now) {
        return new MealBooking(meal, residentId, now);
    }

    public void rebook(LocalDateTime now) {
        if (status != BookingStatus.CANCELLED) throw BusinessException.conflict("This meal is already " + describe());
        status = BookingStatus.BOOKED;
        bookedAt = now;
        cancelledAt = null;
    }

    public void cancel(LocalDateTime now) {
        if (status != BookingStatus.BOOKED) throw BusinessException.conflict("Only an active booking can be cancelled (it is " + describe() + ")");
        status = BookingStatus.CANCELLED;
        cancelledAt = now;
    }

    public void markConsumed(LocalDateTime now, Long staffUserId, BigDecimal amount) {
        switch (status) {
            case BOOKED -> { }
            case CONSUMED -> throw BusinessException.conflict("Consumption was already confirmed for this booking");
            case CANCELLED -> throw BusinessException.conflict("This booking was cancelled, so it cannot be billed");
            case MISSED -> throw BusinessException.conflict("This booking was closed as missed");
            default -> throw BusinessException.conflict("This meal was never booked");
        }
        status = BookingStatus.CONSUMED;
        consumedAt = now;
        confirmedBy = staffUserId;
        chargedAmount = amount;
    }

    public void markMissed() {
        if (status != BookingStatus.BOOKED) throw BusinessException.conflict("Only an active booking can be marked missed");
        status = BookingStatus.MISSED;
    }

    private String describe() { return status.name().toLowerCase(); }
}
