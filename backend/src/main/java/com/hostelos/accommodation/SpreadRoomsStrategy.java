package com.hostelos.accommodation;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/** Prefer emptier rooms and lower floors, giving each resident more space. */
@Component
public class SpreadRoomsStrategy implements AllocationStrategy {
    public String name() { return "SPREAD"; }

    public List<FreeBed> rank(List<FreeBed> freeBeds) {
        return freeBeds.stream()
                .sorted(Comparator.comparingInt(FreeBed::occupantsInRoom)
                        .thenComparing(f -> f.bed().getRoom().getFloorNo())
                        .thenComparing(f -> f.bed().getRoom().getRoomNumber()))
                .toList();
    }
}
