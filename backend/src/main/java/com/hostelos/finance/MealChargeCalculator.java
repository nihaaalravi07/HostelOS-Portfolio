package com.hostelos.finance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/** Pay-per-meal: the resident pays exactly the listed price of the meal they actually consumed. */
@Component
public class MealChargeCalculator implements ChargeCalculator<MealChargeCalculator.MealCharge> {
    public record MealCharge(BigDecimal listedPrice) {}

    @Override
    public BigDecimal calculate(MealCharge meal) {
        return meal.listedPrice().setScale(2, RoundingMode.HALF_UP);
    }
}
