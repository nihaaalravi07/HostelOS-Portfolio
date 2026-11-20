package com.hostelos.finance;

/** Direction matters: debit types increase what the resident owes, credit types reduce it. */
public enum LedgerEntryType {
    CHARGE(true), ADJUSTMENT_DEBIT(true), PAYMENT(false), REFUND(false), ADJUSTMENT_CREDIT(false);

    private final boolean debit;

    LedgerEntryType(boolean debit) { this.debit = debit; }

    public boolean isDebit() { return debit; }
}
