package com.hostelos.housekeeping;

import java.time.LocalTime;

public enum CleaningSlot {
    MORNING("9:00 – 11:00 AM", LocalTime.of(9, 0)),
    LATE_MORNING("11:00 AM – 1:00 PM", LocalTime.of(11, 0)),
    AFTERNOON("2:00 – 4:00 PM", LocalTime.of(14, 0)),
    EVENING("4:00 – 6:00 PM", LocalTime.of(16, 0));

    private final String label;
    private final LocalTime start;

    CleaningSlot(String label, LocalTime start) {
        this.label = label;
        this.start = start;
    }

    public String getLabel() { return label; }
    public LocalTime getStart() { return start; }
}
