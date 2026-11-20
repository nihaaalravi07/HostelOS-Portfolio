package com.hostelos.maintenance;

import com.hostelos.accommodation.AccommodationInfo;
import com.hostelos.accommodation.AccommodationService;
import com.hostelos.common.BusinessException;
import com.hostelos.resident.Resident;
import com.hostelos.resident.ResidentService;
import com.hostelos.resident.ResidentSummary;
import com.hostelos.security.AuthUser;
import com.hostelos.user.Role;
import com.hostelos.user.UserService;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ComplaintService {
    public record ComplaintDto(Long id, Long residentId, String residentName, String roomLabel, ComplaintCategory category,
                               ComplaintPriority priority, String description, ComplaintStatus status, Long assignedToId,
                               String assignedToName, String resolutionNotes, LocalDateTime createdAt, LocalDateTime updatedAt,
                               List<ComplaintStatus> nextStatuses) {}

    private static final Set<ComplaintStatus> ACTIVE = EnumSet.of(ComplaintStatus.OPEN, ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS);

    private final ComplaintRepository complaints;
    private final ResidentService residents;
    private final AccommodationService accommodation;
    private final UserService users;

    public ComplaintService(ComplaintRepository complaints, ResidentService residents, AccommodationService accommodation,
                            UserService users) {
        this.complaints = complaints;
        this.residents = residents;
        this.accommodation = accommodation;
        this.users = users;
    }

    // ------------------------------------------------------------------ student

    @Transactional
    public ComplaintDto submit(ComplaintCategory category, ComplaintPriority priority, String description) {
        Resident resident = residents.requireCurrent();
        AccommodationInfo room = accommodation.requireCurrent(resident.getId());
        Complaint c = complaints.save(new Complaint(resident.getId(), room.roomId(), room.roomLabel(), category, priority, description.trim()));
        return toDtos(List.of(c)).get(0);
    }

    public List<ComplaintDto> mine() {
        return toDtos(complaints.findByResidentIdOrderByCreatedAtDesc(residents.requireCurrent().getId()));
    }

    /** The student confirms the fix and closes their own complaint. */
    @Transactional
    public ComplaintDto closeOwn(Long id) {
        Complaint c = require(id);
        if (!c.getResidentId().equals(residents.requireCurrent().getId()))
            throw BusinessException.forbidden("This is not your complaint");
        c.close();
        return toDtos(List.of(c)).get(0);
    }

    public long openCountFor(Long residentId) { return complaints.countByResidentIdAndStatusIn(residentId, ACTIVE); }

    public long openCount() { return complaints.countByStatusIn(ACTIVE); }

    // ------------------------------------------------------------------ staff / warden / admin

    /** Maintenance staff see open complaints (to pick up) plus their own; wardens/admins see all. */
    public List<ComplaintDto> board(AuthUser caller) {
        List<Complaint> all = complaints.findAllByOrderByCreatedAtDesc();
        if (caller.role() == Role.MAINTENANCE_STAFF) {
            all = all.stream().filter(c -> c.getAssignedTo() == null ? c.getStatus() == ComplaintStatus.OPEN
                    : c.getAssignedTo().equals(caller.userId())).toList();
        }
        List<Complaint> sorted = new ArrayList<>(all);
        sorted.sort(Comparator.comparing((Complaint c) -> !c.getStatus().isActive())
                .thenComparing(c -> -c.getPriority().ordinal()).thenComparing(Complaint::getCreatedAt, Comparator.reverseOrder()));
        return toDtos(sorted);
    }

    @Transactional
    public ComplaintDto claim(Long id, AuthUser caller) {
        Complaint c = require(id);
        if (c.getAssignedTo() != null) throw BusinessException.conflict("This complaint is already assigned");
        c.assignTo(caller.userId());
        return toDtos(List.of(c)).get(0);
    }

    @Transactional
    public ComplaintDto assign(Long id, Long staffUserId) {
        Complaint c = require(id);
        if (users.require(staffUserId).getRole() != Role.MAINTENANCE_STAFF)
            throw BusinessException.badRequest("That user is not maintenance staff");
        c.assignTo(staffUserId);
        return toDtos(List.of(c)).get(0);
    }

    @Transactional
    public ComplaintDto updateStatus(Long id, ComplaintStatus next, String notes, AuthUser caller) {
        Complaint c = require(id);
        if (caller.role() == Role.MAINTENANCE_STAFF && !caller.userId().equals(c.getAssignedTo()))
            throw BusinessException.forbidden("This complaint is not assigned to you");
        switch (next) {
            case IN_PROGRESS -> c.start();
            case RESOLVED -> c.resolve(notes);
            case CLOSED -> {
                if (caller.role() == Role.MAINTENANCE_STAFF) throw BusinessException.forbidden("Only the resident, warden or admin can close a complaint");
                c.close();
            }
            default -> throw BusinessException.badRequest("Use the assign action to assign a complaint");
        }
        return toDtos(List.of(c)).get(0);
    }

    // ------------------------------------------------------------------ helpers

    private Complaint require(Long id) {
        return complaints.findById(id).orElseThrow(() -> BusinessException.notFound("Complaint not found"));
    }

    private List<ComplaintDto> toDtos(List<Complaint> list) {
        Map<Long, ResidentSummary> people = residents.summaries(list.stream().map(Complaint::getResidentId).toList());
        Map<Long, String> staff = users.namesByIds(list.stream().map(Complaint::getAssignedTo).toList());
        return list.stream().map(c -> new ComplaintDto(c.getId(), c.getResidentId(), people.get(c.getResidentId()).fullName(),
                c.getRoomLabel(), c.getCategory(), c.getPriority(), c.getDescription(), c.getStatus(), c.getAssignedTo(),
                c.getAssignedTo() == null ? null : staff.get(c.getAssignedTo()), c.getResolutionNotes(), c.getCreatedAt(),
                c.getUpdatedAt(), List.copyOf(c.getStatus().allowedNext()))).toList();
    }
}
