package com.hostelos.accommodation;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/** Fill partly-occupied rooms first, so empty rooms stay free for later (and for cleaning/maintenance efficiency). */
@Component
public class PackRoomsStrategy implements AllocationStrategy {
    public String name() { return "PACK"; }

    public List<FreeBed> rank(List<FreeBed> freeBeds) {
        return freeBeds.stream()
                .sorted(Comparator.comparingInt(FreeBed::occupantsInRoom).reversed()
                        .thenComparing(f -> f.bed().getRoom().getFloorNo())
                        .thenComparing(f -> f.bed().getRoom().getRoomNumber()))
                .toList();
    }
}
