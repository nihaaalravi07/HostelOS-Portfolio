package com.hostelos.housekeeping;

import com.hostelos.accommodation.AccommodationInfo;
import com.hostelos.accommodation.AccommodationService;
import com.hostelos.common.BusinessException;
import com.hostelos.resident.Resident;
import com.hostelos.resident.ResidentService;
import com.hostelos.resident.ResidentSummary;
import com.hostelos.security.AuthUser;
import com.hostelos.user.Role;
import com.hostelos.user.UserService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CleaningService {
    public record SlotAvailability(CleaningSlot slot, String label, boolean available) {}

    public record CleaningDto(Long id, Long residentId, String residentName, String enrollmentNo, String roomLabel,
                              LocalDate date, CleaningSlot slot, String slotLabel, String instructions,
                              CleaningStatus status, Long assignedToId, String assignedToName, String completionNotes,
                              boolean cancellable) {}

    private static final int MAX_DAYS_AHEAD = 14;

    private final CleaningRepository requests;
    private final ResidentService residents;
    private final AccommodationService accommodation;
    private final UserService users;

    public CleaningService(CleaningRepository requests, ResidentService residents, AccommodationService accommodation,
                           UserService users) {
        this.requests = requests;
        this.residents = residents;
        this.accommodation = accommodation;
        this.users = users;
    }

    // ------------------------------------------------------------------ student

    public List<SlotAvailability> slots(LocalDate date) {
        AccommodationInfo room = accommodation.requireCurrent(residents.requireCurrent().getId());
        Set<CleaningSlot> taken = new HashSet<>();
        requests.findByRoomIdAndRequestDateAndStatusNot(room.roomId(), date, CleaningStatus.CANCELLED)
                .forEach(r -> taken.add(r.getSlot()));
        LocalDateTime now = LocalDateTime.now();
        return Arrays.stream(CleaningSlot.values()).map(s -> new SlotAvailability(s, s.getLabel(),
                !taken.contains(s) && date.atTime(s.getStart()).isAfter(now))).toList();
    }

    @Transactional
    public CleaningDto request(LocalDate date, CleaningSlot slot, String instructions) {
        Resident resident = residents.requireCurrent();
        AccommodationInfo room = accommodation.requireCurrent(resident.getId());
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) throw BusinessException.badRequest("Choose today or a future date");
        if (date.isAfter(today.plusDays(MAX_DAYS_AHEAD))) throw BusinessException.badRequest("Cleaning can be booked up to " + MAX_DAYS_AHEAD + " days ahead");
        if (!date.atTime(slot.getStart()).isAfter(LocalDateTime.now())) throw BusinessException.badRequest("That slot has already started");
        if (requests.existsByRoomIdAndRequestDateAndSlotAndStatusNot(room.roomId(), date, slot, CleaningStatus.CANCELLED))
            throw BusinessException.conflict("That slot is already booked for your room. Please pick another.");

        CleaningRequest r = new CleaningRequest(resident.getId(), room.roomId(), room.roomLabel(), date, slot,
                instructions == null || instructions.isBlank() ? null : instructions.trim());
        r.moveTo(CleaningStatus.SCHEDULED);        // slot validated and reserved
        requests.saveAndFlush(r);                  // unique index is the safety net for races
        return toDtos(List.of(r)).get(0);
    }

    public List<CleaningDto> mine() {
        return toDtos(requests.findByResidentIdOrderByRequestDateDescSlotAsc(residents.requireCurrent().getId()));
    }

    @Transactional
    public CleaningDto cancel(Long id) {
        CleaningRequest r = require(id);
        if (!r.getResidentId().equals(residents.requireCurrent().getId()))
            throw BusinessException.forbidden("This is not your cleaning request");
        if (!r.slotStart().isAfter(LocalDateTime.now())) throw BusinessException.conflict("This slot has already started and cannot be cancelled");
        r.moveTo(CleaningStatus.CANCELLED);
        return toDtos(List.of(r)).get(0);
    }

    public CleaningRequest nextUpcoming(Long residentId) {
        LocalDateTime now = LocalDateTime.now();
        return requests.findByResidentIdOrderByRequestDateDescSlotAsc(residentId).stream()
                .filter(r -> r.getStatus().isPending() && r.slotStart().plusHours(2).isAfter(now))
                .min(Comparator.comparing(CleaningRequest::slotStart)).orElse(null);
    }

    public CleaningDto describe(CleaningRequest r) { return r == null ? null : toDtos(List.of(r)).get(0); }

    // ------------------------------------------------------------------ staff / warden / admin

    /** Housekeeping staff see unassigned jobs plus their own; wardens and admins see everything. */
    public List<CleaningDto> board(LocalDate from, LocalDate to, AuthUser caller) {
        List<CleaningRequest> all = requests.findByRequestDateBetweenOrderByRequestDateAscSlotAsc(from, to);
        if (caller.role() == Role.HOUSEKEEPING_STAFF) {
            all = all.stream().filter(r -> r.getStatus() != CleaningStatus.CANCELLED)
                    .filter(r -> r.getAssignedTo() == null || r.getAssignedTo().equals(caller.userId())).toList();
        }
        return toDtos(all);
    }

    @Transactional
    public CleaningDto claim(Long id, AuthUser caller) {
        CleaningRequest r = require(id);
        if (r.getAssignedTo() != null) throw BusinessException.conflict("This job is already assigned");
        r.assignTo(caller.userId());
        return toDtos(List.of(r)).get(0);
    }

    @Transactional
    public CleaningDto assign(Long id, Long staffUserId) {
        CleaningRequest r = require(id);
        var staff = users.require(staffUserId);
        if (staff.getRole() != Role.HOUSEKEEPING_STAFF) throw BusinessException.badRequest("That user is not housekeeping staff");
        if (r.getStatus() == CleaningStatus.ASSIGNED) {      // reassignment keeps the status
            r.setAssignee(staffUserId);
        } else {
            r.assignTo(staffUserId);
        }
        return toDtos(List.of(r)).get(0);
    }

    @Transactional
    public CleaningDto updateStatus(Long id, CleaningStatus next, String notes, AuthUser caller) {
        CleaningRequest r = require(id);
        if (caller.role() == Role.HOUSEKEEPING_STAFF && !caller.userId().equals(r.getAssignedTo()))
            throw BusinessException.forbidden("This job is not assigned to you");
        if (next == CleaningStatus.COMPLETED) r.complete(notes);
        else if (next == CleaningStatus.IN_PROGRESS || next == CleaningStatus.CANCELLED) r.moveTo(next);
        else throw BusinessException.badRequest("Staff can only start, complete or cancel a job");
        return toDtos(List.of(r)).get(0);
    }

    public long pendingCountFor(Long residentId) {
        return requests.findByResidentIdOrderByRequestDateDescSlotAsc(residentId).stream()
                .filter(r -> r.getStatus().isPending()).count();
    }

    public long pendingCount() {
        return requests.countByStatusIn(EnumSet.of(CleaningStatus.REQUESTED, CleaningStatus.SCHEDULED,
                CleaningStatus.ASSIGNED, CleaningStatus.IN_PROGRESS));
    }

    // ------------------------------------------------------------------ helpers

    private CleaningRequest require(Long id) {
        return requests.findById(id).orElseThrow(() -> BusinessException.notFound("Cleaning request not found"));
    }

    private List<CleaningDto> toDtos(List<CleaningRequest> list) {
        Map<Long, ResidentSummary> people = residents.summaries(list.stream().map(CleaningRequest::getResidentId).toList());
        Map<Long, String> staff = users.namesByIds(list.stream().map(CleaningRequest::getAssignedTo).toList());
        LocalDateTime now = LocalDateTime.now();
        return list.stream().map(r -> {
            ResidentSummary p = people.get(r.getResidentId());
            boolean cancellable = r.getStatus().canMoveTo(CleaningStatus.CANCELLED) && r.slotStart().isAfter(now);
            return new CleaningDto(r.getId(), p.id(), p.fullName(), p.enrollmentNo(), r.getRoomLabel(), r.getRequestDate(),
                    r.getSlot(), r.getSlot().getLabel(), r.getInstructions(), r.getStatus(), r.getAssignedTo(),
                    r.getAssignedTo() == null ? null : staff.get(r.getAssignedTo()), r.getCompletionNotes(), cancellable);
        }).toList();
    }
}
