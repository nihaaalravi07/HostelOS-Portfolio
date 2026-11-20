package com.hostelos.finance;

import com.hostelos.accommodation.AccommodationInfo;
import com.hostelos.accommodation.AccommodationService;
import com.hostelos.common.BusinessException;
import com.hostelos.resident.ResidentService;
import com.hostelos.resident.ResidentSummary;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The single owner of the ledger_entries table. Other modules post charges through here and never
 * write to the ledger themselves.
 */
@Service
@Transactional(readOnly = true)
public class LedgerService {
    public static final String MODULE_MESS = "MESS";
    public static final String MODULE_RENT = "ACCOMMODATION";
    public static final String MODULE_PAYMENT = "PAYMENT";

    /** @param at when the charge happened; null means "now" (only demo seeding back-dates it). */
    public record ChargePosting(Long residentId, BigDecimal amount, String description, String sourceModule, String sourceRef,
                                LocalDateTime at) {
        public ChargePosting(Long residentId, BigDecimal amount, String description, String sourceModule, String sourceRef) {
            this(residentId, amount, description, sourceModule, sourceRef, null);
        }
    }

    public record Summary(BigDecimal totalCharges, BigDecimal totalPayments, BigDecimal totalCredits,
                          BigDecimal outstanding) {}

    public record StatementLine(Long id, String reference, LedgerEntryType type, BigDecimal amount, BigDecimal signedAmount,
                                String description, LocalDateTime createdAt, String sourceModule, String sourceRef,
                                String status, BigDecimal runningBalance) {}

    public record ResidentBalance(Long residentId, String fullName, String enrollmentNo, Summary summary) {}

    private final LedgerRepository ledger;
    private final ResidentService residents;
    private final AccommodationService accommodation;
    private final AccommodationChargeCalculator rentCalculator;

    public LedgerService(LedgerRepository ledger, ResidentService residents, AccommodationService accommodation,
                         AccommodationChargeCalculator rentCalculator) {
        this.ledger = ledger;
        this.residents = residents;
        this.accommodation = accommodation;
        this.rentCalculator = rentCalculator;
    }

    // ------------------------------------------------------------------ posting

    /**
     * Idempotent: posting the same (module, reference) twice returns the original entry instead of
     * creating a second charge. The unique constraint in the database backs this up under concurrency.
     */
    @Transactional
    public LedgerEntry postCharge(ChargePosting p) {
        Optional<LedgerEntry> existing = ledger.findBySourceModuleAndSourceRef(p.sourceModule(), p.sourceRef());
        if (existing.isPresent()) return existing.get();
        requirePositive(p.amount());
        return ledger.saveAndFlush(new LedgerEntry(p.residentId(), LedgerEntryType.CHARGE, p.amount(),
                p.description(), p.sourceModule(), p.sourceRef(), p.at() == null ? LocalDateTime.now() : p.at()));
    }

    /** Simulated/manual payment: no gateway involved. The description says so. */
    @Transactional
    public LedgerEntry postPayment(Long residentId, BigDecimal amount, String method, String note) {
        return postPayment(residentId, amount, method, note, LocalDateTime.now());
    }

    @Transactional
    public LedgerEntry postPayment(Long residentId, BigDecimal amount, String method, String note, LocalDateTime at) {
        residents.requireById(residentId);
        requirePositive(amount);
        String label = "Payment (simulated, " + (method == null || method.isBlank() ? "manual" : method.trim()) + ")"
                + (note == null || note.isBlank() ? "" : " – " + note.trim());
        return ledger.saveAndFlush(new LedgerEntry(residentId, LedgerEntryType.PAYMENT, amount, label,
                MODULE_PAYMENT, "PAY-" + UUID.randomUUID().toString().substring(0, 12), at));
    }

    /** Corrections never edit history. Reversing a charge posts a refund; reversing a payment posts a debit. */
    @Transactional
    public LedgerEntry reverse(Long entryId, String reason) {
        LedgerEntry original = ledger.findById(entryId).orElseThrow(() -> BusinessException.notFound("Ledger entry not found"));
        if (original.getSourceRef().startsWith("REVERSAL-"))
            throw BusinessException.badRequest("A reversal cannot itself be reversed");
        String ref = "REVERSAL-" + original.getId();
        if (ledger.findBySourceModuleAndSourceRef(original.getSourceModule(), ref).isPresent())
            throw BusinessException.conflict("This entry has already been reversed");
        LedgerEntryType type = original.getType().isDebit() ? LedgerEntryType.REFUND : LedgerEntryType.ADJUSTMENT_DEBIT;
        String text = "Reversal of #" + original.getId() + " (" + original.getDescription() + ")"
                + (reason == null || reason.isBlank() ? "" : " – " + reason.trim());
        return ledger.saveAndFlush(new LedgerEntry(original.getResidentId(), type, original.getAmount(),
                text.length() > 250 ? text.substring(0, 250) : text, original.getSourceModule(), ref, LocalDateTime.now()));
    }

    /** Posts rent for every allocated resident for the month. Re-running it never double-charges. */
    @Transactional
    public int postMonthlyRent(YearMonth month) {
        int posted = 0;
        for (var row : accommodation.residentDirectory()) {
            AccommodationInfo info = row.accommodation();
            if (info == null || row.resident().status() != com.hostelos.resident.ResidentStatus.ACTIVE) continue;
            String ref = "RENT-" + row.resident().id() + "-" + month;
            if (ledger.findBySourceModuleAndSourceRef(MODULE_RENT, ref).isPresent()) continue;
            BigDecimal amount = rentCalculator.calculate(new AccommodationChargeCalculator.RoomRent(info.roomCapacity()));
            postCharge(new ChargePosting(row.resident().id(), amount, "Room rent – " + month, MODULE_RENT, ref));
            posted++;
        }
        return posted;
    }

    // ------------------------------------------------------------------ reading

    public Summary summary(Long residentId) {
        return summarise(ledger.totalsByType(residentId));
    }

    public List<StatementLine> statement(Long residentId) {
        List<StatementLine> lines = new ArrayList<>();
        BigDecimal running = BigDecimal.ZERO;
        for (LedgerEntry e : ledger.findByResidentIdOrderByCreatedAtAscIdAsc(residentId)) {
            running = running.add(e.signedAmount());
            lines.add(new StatementLine(e.getId(), "TXN-" + String.format("%06d", e.getId()), e.getType(), e.getAmount(),
                    e.signedAmount(), e.getDescription(), e.getCreatedAt(), e.getSourceModule(), e.getSourceRef(),
                    e.getStatus(), running));
        }
        Collections.reverse(lines);   // newest first for display
        return lines;
    }

    /** Net spend (charges minus refunds) of one module in a month, e.g. MESS for the dashboard. */
    public BigDecimal netSpend(Long residentId, String module, YearMonth month) {
        List<Object[]> rows = ledger.moduleTotals(residentId, module, month.atDay(1).atStartOfDay(),
                month.plusMonths(1).atDay(1).atStartOfDay());
        BigDecimal net = BigDecimal.ZERO;
        for (Object[] r : rows) {
            LedgerEntryType t = (LedgerEntryType) r[0];
            BigDecimal amt = (BigDecimal) r[1];
            net = t.isDebit() ? net.add(amt) : net.subtract(amt);
        }
        return net;
    }

    public List<ResidentBalance> balances() {
        Map<Long, List<Object[]>> byResident = new HashMap<>();
        for (Object[] r : ledger.totalsByResidentAndType())
            byResident.computeIfAbsent((Long) r[0], k -> new ArrayList<>()).add(new Object[]{r[1], r[2]});
        Map<Long, ResidentSummary> names = residents.summaries(byResident.keySet());
        List<ResidentBalance> result = new ArrayList<>();
        byResident.forEach((rid, rows) -> {
            ResidentSummary s = names.get(rid);
            result.add(new ResidentBalance(rid, s.fullName(), s.enrollmentNo(), summarise(rows)));
        });
        result.sort(Comparator.comparing((ResidentBalance b) -> b.summary().outstanding()).reversed());
        return result;
    }

    public BigDecimal totalOutstanding() {
        return balances().stream().map(b -> b.summary().outstanding()).filter(o -> o.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ------------------------------------------------------------------ helpers

    private Summary summarise(List<Object[]> rows) {
        BigDecimal charges = BigDecimal.ZERO, payments = BigDecimal.ZERO, credits = BigDecimal.ZERO;
        for (Object[] r : rows) {
            LedgerEntryType t = (LedgerEntryType) r[0];
            BigDecimal amt = (BigDecimal) r[1];
            switch (t) {
                case CHARGE, ADJUSTMENT_DEBIT -> charges = charges.add(amt);
                case PAYMENT -> payments = payments.add(amt);
                case REFUND, ADJUSTMENT_CREDIT -> credits = credits.add(amt);
            }
        }
        return new Summary(charges, payments, credits, charges.subtract(payments).subtract(credits));
    }

    private void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw BusinessException.badRequest("Amount must be greater than zero");
    }
}
