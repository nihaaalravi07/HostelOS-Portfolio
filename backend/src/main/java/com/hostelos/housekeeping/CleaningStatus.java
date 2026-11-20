package com.hostelos.housekeeping;

import java.util.EnumSet;
import java.util.Set;

/** A cleaning job's lifecycle. Each state lists the only states it may move to. */
public enum CleaningStatus {
    REQUESTED, SCHEDULED, ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED;

    public Set<CleaningStatus> allowedNext() {
        return switch (this) {
            case REQUESTED -> EnumSet.of(SCHEDULED, CANCELLED);
            case SCHEDULED -> EnumSet.of(ASSIGNED, CANCELLED);
            case ASSIGNED -> EnumSet.of(IN_PROGRESS, CANCELLED);
            case IN_PROGRESS -> EnumSet.of(COMPLETED);
            case COMPLETED, CANCELLED -> EnumSet.noneOf(CleaningStatus.class);
        };
    }

    public boolean canMoveTo(CleaningStatus next) { return allowedNext().contains(next); }

    /** Still needs work from staff. */
    public boolean isPending() { return this == REQUESTED || this == SCHEDULED || this == ASSIGNED || this == IN_PROGRESS; }
}
