package com.hostelos.accommodation;

import java.util.List;

/**
 * Strategy pattern: a rule for ranking free beds for a resident. Rule-based only, no ML.
 * New policies are added by writing another implementation.
 */
public interface AllocationStrategy {
    String name();

    /** @return the free beds, best first. */
    List<FreeBed> rank(List<FreeBed> freeBeds);

    /** A free bed together with how full its room currently is. */
    record FreeBed(Bed bed, int occupantsInRoom) {}
}
