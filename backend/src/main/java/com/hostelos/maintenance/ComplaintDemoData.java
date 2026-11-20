package com.hostelos.maintenance;

import com.hostelos.accommodation.AccommodationInfo;
import com.hostelos.accommodation.AccommodationService;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ComplaintDemoData {
    private final ComplaintRepository complaints;
    private final AccommodationService accommodation;

    public ComplaintDemoData(ComplaintRepository complaints, AccommodationService accommodation) {
        this.complaints = complaints;
        this.accommodation = accommodation;
    }

    /** staff[0], staff[1] are maintenance users. */
    public void seed(List<Long> residentIds, List<Long> staff) {
        add(residentIds.get(0), ComplaintCategory.INTERNET, ComplaintPriority.MEDIUM, "Wi-Fi drops every evening after 9 PM in the room.", Stage.OPEN, null);
        add(residentIds.get(2), ComplaintCategory.ELECTRICAL, ComplaintPriority.URGENT, "Ceiling fan sparks when switched on. Unsafe to use.", Stage.IN_PROGRESS, staff.get(0));
        add(residentIds.get(4), ComplaintCategory.PLUMBING, ComplaintPriority.HIGH, "Tap in the bathroom is leaking continuously.", Stage.ASSIGNED, staff.get(1));
        add(residentIds.get(6), ComplaintCategory.FURNITURE, ComplaintPriority.LOW, "Study chair has a broken armrest.", Stage.OPEN, null);
        add(residentIds.get(8), ComplaintCategory.ELECTRICAL, ComplaintPriority.MEDIUM, "Tube light flickers and buzzes.", Stage.RESOLVED, staff.get(0));
        add(residentIds.get(10), ComplaintCategory.CLEANLINESS, ComplaintPriority.MEDIUM, "Corridor dustbin near room has not been emptied for 3 days.", Stage.CLOSED, staff.get(1));
        add(residentIds.get(12), ComplaintCategory.INTERNET, ComplaintPriority.HIGH, "LAN port in the room is not working.", Stage.IN_PROGRESS, staff.get(1));
        add(residentIds.get(14), ComplaintCategory.OTHER, ComplaintPriority.LOW, "Window latch is loose and does not lock.", Stage.OPEN, null);
    }

    private enum Stage { OPEN, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED }

    private void add(Long residentId, ComplaintCategory cat, ComplaintPriority pri, String text, Stage stage, Long staffId) {
        AccommodationInfo room = accommodation.requireCurrent(residentId);
        Complaint c = new Complaint(residentId, room.roomId(), room.roomLabel(), cat, pri, text);
        if (stage != Stage.OPEN) c.assignTo(staffId);
        if (stage == Stage.IN_PROGRESS || stage == Stage.RESOLVED || stage == Stage.CLOSED) c.start();
        if (stage == Stage.RESOLVED || stage == Stage.CLOSED) c.resolve("Fault fixed and tested with the resident.");
        if (stage == Stage.CLOSED) c.close();
        complaints.save(c);
    }
}
