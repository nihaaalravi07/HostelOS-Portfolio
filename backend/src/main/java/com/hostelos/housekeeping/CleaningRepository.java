package com.hostelos.housekeeping;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CleaningRepository extends JpaRepository<CleaningRequest, Long> {
    boolean existsByRoomIdAndRequestDateAndSlotAndStatusNot(Long roomId, LocalDate date, CleaningSlot slot, CleaningStatus status);

    List<CleaningRequest> findByRoomIdAndRequestDateAndStatusNot(Long roomId, LocalDate date, CleaningStatus status);

    List<CleaningRequest> findByResidentIdOrderByRequestDateDescSlotAsc(Long residentId);

    List<CleaningRequest> findByRequestDateBetweenOrderByRequestDateAscSlotAsc(LocalDate from, LocalDate to);

    long countByStatusIn(java.util.Collection<CleaningStatus> statuses);
}
