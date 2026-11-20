package com.hostelos.accommodation;

import java.time.LocalDate;
import java.util.List;

/** What a resident sees about where they live. */
public record AccommodationInfo(Long allocationId, Long hostelId, String hostelName, Long roomId, String roomNumber,
                                int floorNo, int bedNumber, int roomCapacity, LocalDate since,
                                List<String> roommates) {
    public String roomLabel() { return hostelName + " · Room " + roomNumber; }
}
