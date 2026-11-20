package com.hostelos.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hostelos.accommodation.AccommodationService;
import com.hostelos.common.BusinessException;
import com.hostelos.finance.LedgerService.ChargePosting;
import com.hostelos.resident.ResidentService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Mockito tests for the ledger rules that do not need a database. */
class LedgerServiceTest {
    private LedgerRepository repo;
    private LedgerService service;

    @BeforeEach
    void setUp() {
        repo = mock(LedgerRepository.class);
        service = new LedgerService(repo, mock(ResidentService.class), mock(AccommodationService.class),
                new AccommodationChargeCalculator(new BigDecimal("3500")));
    }

    @Test
    void postingTheSameSourceTwiceReturnsTheExistingEntry() {
        LedgerEntry existing = new LedgerEntry(1L, LedgerEntryType.CHARGE, new BigDecimal("90"), "Lunch", "MESS", "BOOKING-5", LocalDateTime.now());
        when(repo.findBySourceModuleAndSourceRef("MESS", "BOOKING-5")).thenReturn(Optional.of(existing));

        LedgerEntry result = service.postCharge(new ChargePosting(1L, new BigDecimal("90"), "Lunch", "MESS", "BOOKING-5"));

        assertThat(result).isSameAs(existing);
        verify(repo, never()).saveAndFlush(any());
    }

    @Test
    void newChargeIsSavedAsChargeEntry() {
        when(repo.findBySourceModuleAndSourceRef("MESS", "BOOKING-6")).thenReturn(Optional.empty());
        when(repo.saveAndFlush(any(LedgerEntry.class))).thenAnswer(i -> i.getArgument(0));

        LedgerEntry saved = service.postCharge(new ChargePosting(1L, new BigDecimal("45"), "Breakfast", "MESS", "BOOKING-6"));

        assertThat(saved.getType()).isEqualTo(LedgerEntryType.CHARGE);
        assertThat(saved.signedAmount()).isEqualByComparingTo("45");
    }

    @Test
    void chargesMustBePositive() {
        assertThatThrownBy(() -> service.postCharge(new ChargePosting(1L, BigDecimal.ZERO, "x", "MESS", "B-1")))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.postCharge(new ChargePosting(1L, new BigDecimal("-5"), "x", "MESS", "B-2")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void summaryDistinguishesChargesPaymentsAndCredits() {
        when(repo.totalsByType(1L)).thenReturn(List.of(
                new Object[]{LedgerEntryType.CHARGE, new BigDecimal("1000")},
                new Object[]{LedgerEntryType.PAYMENT, new BigDecimal("400")},
                new Object[]{LedgerEntryType.REFUND, new BigDecimal("90")}));

        LedgerService.Summary s = service.summary(1L);

        assertThat(s.totalCharges()).isEqualByComparingTo("1000");
        assertThat(s.totalPayments()).isEqualByComparingTo("400");
        assertThat(s.totalCredits()).isEqualByComparingTo("90");
        assertThat(s.outstanding()).isEqualByComparingTo("510");
    }

    @Test
    void aReversalCannotBeReversedAgain() {
        LedgerEntry reversal = new LedgerEntry(1L, LedgerEntryType.REFUND, BigDecimal.TEN, "rev", "MESS", "REVERSAL-3", LocalDateTime.now());
        when(repo.findById(9L)).thenReturn(Optional.of(reversal));
        assertThatThrownBy(() -> service.reverse(9L, "oops")).isInstanceOf(BusinessException.class);
    }

    @Test
    void mealChargeUsesListedPriceToTwoDecimals() {
        assertThat(new MealChargeCalculator().calculate(new MealChargeCalculator.MealCharge(new BigDecimal("42.5"))))
                .isEqualByComparingTo("42.50");
    }
}
