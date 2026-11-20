package com.hostelos.finance;

import java.math.BigDecimal;

/**
 * Polymorphism: every kind of charge knows how to price itself. The ledger does not care which
 * calculator produced the amount.
 *
 * @param <S> the thing being charged for (a meal, a month of rent, a service...)
 */
public interface ChargeCalculator<S> {
    BigDecimal calculate(S subject);
}
