package com.hostelos.housekeeping;

import com.hostelos.accommodation.AccommodationInfo;
import com.hostelos.accommodation.AccommodationService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CleaningDemoData {
    private final CleaningRepository requests;
    private final AccommodationService accommodation;

    public CleaningDemoData(CleaningRepository requests, AccommodationService accommodation) {
        this.requests = requests;
        this.accommodation = accommodation;
    }

    /** residentIds in seed order; staff[0], staff[1] are housekeeping users. */
    public void seed(List<Long> residentIds, List<Long> staff) {
        LocalDate today = LocalDate.now();
        add(residentIds.get(0), today.plusDays(1), CleaningSlot.LATE_MORNING, "Please sweep under the bed too", staff.get(0), null, false);
        add(residentIds.get(1), today.plusDays(2), CleaningSlot.AFTERNOON, null, null, null, false);
        add(residentIds.get(3), today.plusDays(1), CleaningSlot.MORNING, "Deep clean the bathroom", staff.get(0), null, false);
        add(residentIds.get(5), today.plusDays(1), CleaningSlot.EVENING, null, null, null, false);
        add(residentIds.get(7), today, CleaningSlot.EVENING, "Dust the study table", staff.get(1), null, false);
        add(residentIds.get(9), today.minusDays(1), CleaningSlot.MORNING, null, staff.get(0), "Cleaned and mopped. Replaced the dustbin liner.", true);
        add(residentIds.get(11), today.minusDays(2), CleaningSlot.AFTERNOON, "Window glass please", staff.get(1), "Done. Windows wiped.", true);
        add(residentIds.get(13), today.minusDays(3), CleaningSlot.LATE_MORNING, null, staff.get(1), "Room swept and mopped.", true);
    }

    private void add(Long residentId, LocalDate date, CleaningSlot slot, String instructions, Long staffId, String completion, boolean completed) {
        AccommodationInfo room = accommodation.requireCurrent(residentId);
        CleaningRequest r = new CleaningRequest(residentId, room.roomId(), room.roomLabel(), date, slot, instructions);
        r.moveTo(CleaningStatus.SCHEDULED);
        if (staffId != null) r.assignTo(staffId);
        if (completed) { r.moveTo(CleaningStatus.IN_PROGRESS); r.complete(completion); }
        requests.save(r);
    }
}
