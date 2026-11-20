package com.hostelos.finance;

import com.hostelos.finance.LedgerService.*;
import com.hostelos.resident.ResidentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {
    public record PaymentRequest(@NotNull @DecimalMin("1.00") BigDecimal amount, String method, String note) {}
    public record ReverseRequest(String reason) {}
    public record RentRequest(String month) {}
    public record Report(BigDecimal totalOutstanding, BigDecimal totalCharges, BigDecimal totalPayments,
                         List<ResidentBalance> balances) {}

    private final LedgerService ledger;
    private final ResidentService residents;

    public FinanceController(LedgerService ledger, ResidentService residents) {
        this.ledger = ledger;
        this.residents = residents;
    }

    // ---- student: own account only ----

    @GetMapping("/me/summary")
    @PreAuthorize("hasRole('STUDENT')")
    public Summary mySummary() { return ledger.summary(residents.requireCurrent().getId()); }

    @GetMapping("/me/statement")
    @PreAuthorize("hasRole('STUDENT')")
    public List<StatementLine> myStatement() { return ledger.statement(residents.requireCurrent().getId()); }

    @PostMapping("/me/payments")
    @PreAuthorize("hasRole('STUDENT')")
    public StatementLine pay(@Valid @RequestBody PaymentRequest r) {
        Long rid = residents.requireCurrent().getId();
        LedgerEntry e = ledger.postPayment(rid, r.amount(), r.method(), r.note());
        return ledger.statement(rid).stream().filter(l -> l.id().equals(e.getId())).findFirst().orElseThrow();
    }

    // ---- admin ----

    @GetMapping("/report")
    @PreAuthorize("hasRole('ADMIN')")
    public Report report() {
        List<ResidentBalance> balances = ledger.balances();
        BigDecimal charges = balances.stream().map(b -> b.summary().totalCharges()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payments = balances.stream().map(b -> b.summary().totalPayments()).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Report(ledger.totalOutstanding(), charges, payments, balances);
    }

    @GetMapping("/residents/{id}/statement")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> residentStatement(@PathVariable Long id) {
        return Map.of("resident", residents.summary(id), "summary", ledger.summary(id), "lines", ledger.statement(id));
    }

    @PostMapping("/residents/{id}/payments")
    @PreAuthorize("hasRole('ADMIN')")
    public Summary recordPayment(@PathVariable Long id, @Valid @RequestBody PaymentRequest r) {
        ledger.postPayment(id, r.amount(), r.method(), r.note());
        return ledger.summary(id);
    }

    @PostMapping("/entries/{id}/reverse")
    @PreAuthorize("hasRole('ADMIN')")
    public Summary reverse(@PathVariable Long id, @RequestBody(required = false) ReverseRequest r) {
        LedgerEntry reversal = ledger.reverse(id, r == null ? null : r.reason());
        return ledger.summary(reversal.getResidentId());
    }

    @PostMapping("/rent")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> postRent(@RequestBody RentRequest r) {
        YearMonth month = r.month() == null || r.month().isBlank() ? YearMonth.now() : YearMonth.parse(r.month());
        return Map.of("month", month.toString(), "posted", ledger.postMonthlyRent(month));
    }
}
