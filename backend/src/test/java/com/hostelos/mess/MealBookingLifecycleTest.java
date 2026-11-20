package com.hostelos.mess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hostelos.common.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pure unit tests: the booking state machine has no Spring or database in it. */
class MealBookingLifecycleTest {
    private final LocalDateTime now = LocalDateTime.of(2026, 11, 14, 12, 0);
    private Meal meal;
    private MealBooking booking;

    @BeforeEach
    void setUp() {
        meal = new Meal(LocalDate.of(2026, 11, 14), MealCategory.LUNCH, "Rice, Dal", new BigDecimal("90.00"), now.plusHours(1), null);
        booking = MealBooking.create(meal, 1L, now);
    }

    @Test
    void newBookingIsBookedAndNotBilled() {
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.BOOKED);
        assertThat(booking.getChargedAmount()).isNull();
    }

    @Test
    void confirmingRecordsStaffTimeAndAmount() {
        booking.markConsumed(now, 7L, new BigDecimal("90.00"));
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONSUMED);
        assertThat(booking.getConfirmedBy()).isEqualTo(7L);
        assertThat(booking.getChargedAmount()).isEqualByComparingTo("90.00");
    }

    @Test
    void consumptionCannotBeConfirmedTwice() {
        booking.markConsumed(now, 7L, BigDecimal.TEN);
        assertThatThrownBy(() -> booking.markConsumed(now, 7L, BigDecimal.TEN))
                .isInstanceOf(BusinessException.class).hasMessageContaining("already confirmed");
    }

    @Test
    void cancelledBookingCannotBeBilled() {
        booking.cancel(now);
        assertThatThrownBy(() -> booking.markConsumed(now, 7L, BigDecimal.TEN))
                .isInstanceOf(BusinessException.class).hasMessageContaining("cancelled");
        assertThat(booking.getChargedAmount()).isNull();
    }

    @Test
    void missedBookingCannotBeBilled() {
        booking.markMissed();
        assertThatThrownBy(() -> booking.markConsumed(now, 7L, BigDecimal.TEN)).isInstanceOf(BusinessException.class);
    }

    @Test
    void eatenMealCannotBeCancelled() {
        booking.markConsumed(now, 7L, BigDecimal.TEN);
        assertThatThrownBy(() -> booking.cancel(now)).isInstanceOf(BusinessException.class);
    }

    @Test
    void cancelledBookingCanBeRebooked() {
        booking.cancel(now);
        booking.rebook(now.plusMinutes(5));
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.BOOKED);
        assertThat(booking.getCancelledAt()).isNull();
    }

    @Test
    void activeBookingCannotBeRebooked() {
        assertThatThrownBy(() -> booking.rebook(now)).isInstanceOf(BusinessException.class);
    }

    @Test
    void bookingClosesAtCutoff() {
        assertThat(meal.isBookingOpen(now)).isTrue();
        assertThat(meal.isBookingOpen(now.plusHours(1))).isFalse();
        meal.update("Rice", BigDecimal.ONE, now.plusHours(5), false);
        assertThat(meal.isBookingOpen(now)).as("withdrawn meals are never bookable").isFalse();
    }
}
