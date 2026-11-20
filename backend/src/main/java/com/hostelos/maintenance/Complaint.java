package com.hostelos.maintenance;

import com.hostelos.common.BusinessException;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "maintenance_complaints")
@Getter
@NoArgsConstructor
public class Complaint {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "resident_id", nullable = false)
    private Long residentId;
    @Column(name = "room_id", nullable = false)
    private Long roomId;
    @Column(name = "room_label", nullable = false)
    private String roomLabel;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintCategory category;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintPriority priority;
    @Column(nullable = false)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintStatus status;
    @Column(name = "assigned_to")
    private Long assignedTo;
    @Column(name = "resolution_notes")
    private String resolutionNotes;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Complaint(Long residentId, Long roomId, String roomLabel, ComplaintCategory category,
                     ComplaintPriority priority, String description) {
        this.residentId = residentId;
        this.roomId = roomId;
        this.roomLabel = roomLabel;
        this.category = category;
        this.priority = priority;
        this.description = description;
        this.status = ComplaintStatus.OPEN;
        this.createdAt = this.updatedAt = LocalDateTime.now();
    }

    private void moveTo(ComplaintStatus next) {
        if (!status.canMoveTo(next))
            throw BusinessException.conflict("A complaint that is " + status + " cannot become " + next);
        status = next;
        updatedAt = LocalDateTime.now();
    }

    public void assignTo(Long staffUserId) {
        if (status == ComplaintStatus.ASSIGNED) { assignedTo = staffUserId; updatedAt = LocalDateTime.now(); return; } // reassign
        moveTo(ComplaintStatus.ASSIGNED);
        assignedTo = staffUserId;
    }

    public void start() { moveTo(ComplaintStatus.IN_PROGRESS); }

    public void resolve(String notes) {
        if (notes == null || notes.isBlank()) throw BusinessException.badRequest("Please record what was done to resolve the complaint");
        moveTo(ComplaintStatus.RESOLVED);
        resolutionNotes = notes.trim();
    }

    public void close() { moveTo(ComplaintStatus.CLOSED); }
}
