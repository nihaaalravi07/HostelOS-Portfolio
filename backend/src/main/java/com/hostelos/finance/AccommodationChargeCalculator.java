package com.hostelos.finance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Monthly room rent. The rate comes from configuration, never hard-coded in the ledger. */
@Component
public class AccommodationChargeCalculator implements ChargeCalculator<AccommodationChargeCalculator.RoomRent> {
    public record RoomRent(int roomCapacity) {}

    private final BigDecimal monthlyRent;

    public AccommodationChargeCalculator(@Value("${hostelos.monthly-room-rent}") BigDecimal monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    @Override
    public BigDecimal calculate(RoomRent rent) {
        return monthlyRent.setScale(2, RoundingMode.HALF_UP);
    }
}
