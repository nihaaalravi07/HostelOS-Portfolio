package com.hostelos.accommodation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllocationRepository extends JpaRepository<Allocation, Long> {
    Optional<Allocation> findByResidentIdAndEndDateIsNull(Long residentId);
    List<Allocation> findByEndDateIsNull();
    List<Allocation> findByBedRoomIdAndEndDateIsNull(Long roomId);
    boolean existsByBedIdAndEndDateIsNull(Long bedId);
    List<Allocation> findByResidentIdOrderByStartDateDesc(Long residentId);
}
