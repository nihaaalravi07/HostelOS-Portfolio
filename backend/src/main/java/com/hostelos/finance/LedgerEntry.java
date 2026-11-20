package com.hostelos.finance;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One row of the resident ledger. Deliberately immutable: no setters, and the database refuses
 * UPDATE/DELETE. Corrections are made by posting a new reversing entry.
 */
@Entity
@Table(name = "ledger_entries")
@Getter
@NoArgsConstructor
public class LedgerEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "resident_id", nullable = false, updatable = false)
    private Long residentId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private LedgerEntryType type;
    @Column(nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, updatable = false)
    private String description;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "source_module", nullable = false, updatable = false)
    private String sourceModule;
    @Column(name = "source_ref", nullable = false, updatable = false)
    private String sourceRef;
    @Column(nullable = false, updatable = false)
    private String status = "POSTED";

    public LedgerEntry(Long residentId, LedgerEntryType type, BigDecimal amount, String description,
                       String sourceModule, String sourceRef, LocalDateTime createdAt) {
        this.residentId = residentId;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.sourceModule = sourceModule;
        this.sourceRef = sourceRef;
        this.createdAt = createdAt;
    }

    /** Positive for money owed, negative for money credited. */
    public BigDecimal signedAmount() { return type.isDebit() ? amount : amount.negate(); }
}
