package com.hostelos.checkout;

import com.hostelos.accommodation.AccommodationService;
import com.hostelos.common.BusinessException;
import com.hostelos.finance.LedgerService;
import com.hostelos.housekeeping.CleaningService;
import com.hostelos.maintenance.ComplaintService;
import com.hostelos.resident.Resident;
import com.hostelos.resident.ResidentService;
import com.hostelos.resident.ResidentSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checkout = a resident leaves the hostel. The warden/admin can clear it only when nothing is owed
 * and nothing is open. Clearing releases the bed but keeps every historical record.
 */
@Service
@Transactional(readOnly = true)
public class CheckoutService {
    public record Clearance(BigDecimal outstanding, long openComplaints, long pendingCleaning, boolean canClear, List<String> blockers) {}

    public record CheckoutDto(Long id, Long residentId, String residentName, String enrollmentNo, String room,
                              LocalDate requestedOn, LocalDate plannedDate, CheckoutStatus status, String reviewNote,
                              BigDecimal duesAtReview, Clearance clearance) {}

    private final CheckoutRepository requests;
    private final ResidentService residents;
    private final AccommodationService accommodation;
    private final LedgerService ledger;
    private final ComplaintService complaints;
    private final CleaningService cleaning;

    public CheckoutService(CheckoutRepository requests, ResidentService residents, AccommodationService accommodation,
                           LedgerService ledger, ComplaintService complaints, CleaningService cleaning) {
        this.requests = requests;
        this.residents = residents;
        this.accommodation = accommodation;
        this.ledger = ledger;
        this.complaints = complaints;
        this.cleaning = cleaning;
    }

    @Transactional
    public CheckoutDto request(LocalDate plannedDate) {
        Resident resident = residents.requireCurrent();
        if (!resident.isActive()) throw BusinessException.conflict("You have already checked out");
        if (plannedDate.isBefore(LocalDate.now())) throw BusinessException.badRequest("Choose today or a future date");
        if (requests.existsByResidentIdAndStatus(resident.getId(), CheckoutStatus.PENDING))
            throw BusinessException.conflict("You already have a checkout request waiting for review");
        accommodation.requireCurrent(resident.getId());
        CheckoutRequest r = requests.saveAndFlush(new CheckoutRequest(resident.getId(), plannedDate));
        return toDtos(List.of(r)).get(0);
    }

    public List<CheckoutDto> mine() {
        return toDtos(requests.findByResidentIdOrderByRequestedOnDescIdDesc(residents.requireCurrent().getId()));
    }

    public List<CheckoutDto> all() { return toDtos(requests.findAllByOrderByRequestedOnDescIdDesc()); }

    @Transactional
    public CheckoutDto clear(Long id, Long reviewerId, String note) {
        CheckoutRequest r = require(id);
        Clearance c = clearanceFor(r.getResidentId());
        if (r.getStatus() == CheckoutStatus.PENDING && !c.canClear())
            throw BusinessException.conflict("Cannot clear yet: " + String.join("; ", c.blockers()));
        r.clear(reviewerId, note, c.outstanding());
        accommodation.release(r.getResidentId());          // bed freed; allocation history stays
        residents.markCheckedOut(r.getResidentId());       // resident record, ledger and history stay
        return toDtos(List.of(r)).get(0);
    }

    @Transactional
    public CheckoutDto reject(Long id, Long reviewerId, String note) {
        CheckoutRequest r = require(id);
        r.reject(reviewerId, note, clearanceFor(r.getResidentId()).outstanding());
        return toDtos(List.of(r)).get(0);
    }

    public Clearance clearanceFor(Long residentId) {
        BigDecimal outstanding = ledger.summary(residentId).outstanding();
        long openComplaints = complaints.openCountFor(residentId);
        long pendingCleaning = cleaning.pendingCountFor(residentId);
        List<String> blockers = new ArrayList<>();
        if (outstanding.signum() > 0) blockers.add("₹" + outstanding.toPlainString() + " is still due");
        if (openComplaints > 0) blockers.add(openComplaints + " open maintenance complaint(s)");
        if (pendingCleaning > 0) blockers.add(pendingCleaning + " pending cleaning job(s)");
        return new Clearance(outstanding, openComplaints, pendingCleaning, blockers.isEmpty(), blockers);
    }

    private CheckoutRequest require(Long id) {
        return requests.findById(id).orElseThrow(() -> BusinessException.notFound("Checkout request not found"));
    }

    private List<CheckoutDto> toDtos(List<CheckoutRequest> list) {
        Map<Long, ResidentSummary> people = residents.summaries(list.stream().map(CheckoutRequest::getResidentId).toList());
        return list.stream().map(r -> {
            ResidentSummary p = people.get(r.getResidentId());
            String room = accommodation.currentFor(r.getResidentId()).map(a -> a.roomLabel()).orElse("Released");
            Clearance c = r.getStatus() == CheckoutStatus.PENDING ? clearanceFor(r.getResidentId()) : null;
            return new CheckoutDto(r.getId(), p.id(), p.fullName(), p.enrollmentNo(), room, r.getRequestedOn(),
                    r.getPlannedDate(), r.getStatus(), r.getReviewNote(), r.getDuesAtReview(), c);
        }).toList();
    }
}
