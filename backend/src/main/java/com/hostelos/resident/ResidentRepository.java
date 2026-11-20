package com.hostelos.resident;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResidentRepository extends JpaRepository<Resident, Long> {
    Optional<Resident> findByUserId(Long userId);
    long countByStatus(ResidentStatus status);
}
