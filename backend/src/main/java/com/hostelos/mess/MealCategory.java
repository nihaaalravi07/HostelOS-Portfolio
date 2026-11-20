package com.hostelos.mess;

import java.time.LocalTime;

public enum MealCategory {
    BREAKFAST("Breakfast", LocalTime.of(8, 0), LocalTime.of(9, 30)),
    LUNCH("Lunch", LocalTime.of(12, 30), LocalTime.of(14, 0)),
    SNACKS("Evening snacks", LocalTime.of(17, 0), LocalTime.of(18, 0)),
    DINNER("Dinner", LocalTime.of(20, 0), LocalTime.of(21, 30));

    private final String label;
    private final LocalTime start;
    private final LocalTime end;

    MealCategory(String label, LocalTime start, LocalTime end) {
        this.label = label;
        this.start = start;
        this.end = end;
    }

    public String getLabel() { return label; }
    public LocalTime getStart() { return start; }
    public LocalTime getEnd() { return end; }
}
