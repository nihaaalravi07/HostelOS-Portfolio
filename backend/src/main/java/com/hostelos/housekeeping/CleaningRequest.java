package com.hostelos.housekeeping;

import com.hostelos.common.BusinessException;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cleaning_requests")
@Getter
@NoArgsConstructor
public class CleaningRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "resident_id", nullable = false)
    private Long residentId;
    @Column(name = "room_id", nullable = false)
    private Long roomId;
    @Column(name = "room_label", nullable = false)
    private String roomLabel;
    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CleaningSlot slot;
    private String instructions;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CleaningStatus status;
    @Column(name = "assigned_to")
    private Long assignedTo;
    @Column(name = "completion_notes")
    private String completionNotes;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public CleaningRequest(Long residentId, Long roomId, String roomLabel, LocalDate date, CleaningSlot slot, String instructions) {
        this.residentId = residentId;
        this.roomId = roomId;
        this.roomLabel = roomLabel;
        this.requestDate = date;
        this.slot = slot;
        this.instructions = instructions;
        this.status = CleaningStatus.REQUESTED;
        this.createdAt = this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime slotStart() { return requestDate.atTime(slot.getStart()); }

    /** The one place a status changes, so invalid jumps (e.g. REQUESTED -> COMPLETED) are impossible. */
    public void moveTo(CleaningStatus next) {
        if (!status.canMoveTo(next))
            throw BusinessException.conflict("A " + status + " cleaning job cannot become " + next);
        status = next;
        updatedAt = LocalDateTime.now();
    }

    public void assignTo(Long staffUserId) {
        if (status == CleaningStatus.REQUESTED) moveTo(CleaningStatus.SCHEDULED);
        moveTo(CleaningStatus.ASSIGNED);
        assignedTo = staffUserId;
    }

    public void setAssignee(Long staffUserId) {
        this.assignedTo = staffUserId;
        this.updatedAt = LocalDateTime.now();
    }

    public void complete(String notes) {
        moveTo(CleaningStatus.COMPLETED);
        completionNotes = notes == null || notes.isBlank() ? null : notes.trim();
    }
}
