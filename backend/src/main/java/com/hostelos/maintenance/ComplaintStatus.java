package com.hostelos.maintenance;

import java.util.EnumSet;
import java.util.Set;

/** Controlled lifecycle: a complaint can only move along the arrows below. */
public enum ComplaintStatus {
    OPEN, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED;

    public Set<ComplaintStatus> allowedNext() {
        return switch (this) {
            case OPEN -> EnumSet.of(ASSIGNED);
            case ASSIGNED -> EnumSet.of(IN_PROGRESS);
            case IN_PROGRESS -> EnumSet.of(RESOLVED);
            case RESOLVED -> EnumSet.of(CLOSED, IN_PROGRESS);   // IN_PROGRESS = re-opened because the fix did not hold
            case CLOSED -> EnumSet.noneOf(ComplaintStatus.class);
        };
    }

    public boolean canMoveTo(ComplaintStatus next) { return allowedNext().contains(next); }

    public boolean isActive() { return this == OPEN || this == ASSIGNED || this == IN_PROGRESS; }
}
