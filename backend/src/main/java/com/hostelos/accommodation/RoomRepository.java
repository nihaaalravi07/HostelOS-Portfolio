package com.hostelos.accommodation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByHostelIdOrderByFloorNoAscRoomNumberAsc(Long hostelId);
    boolean existsByHostelIdAndRoomNumber(Long hostelId, String roomNumber);
}
