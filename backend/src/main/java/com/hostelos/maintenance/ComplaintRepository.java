package com.hostelos.maintenance;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByResidentIdOrderByCreatedAtDesc(Long residentId);
    List<Complaint> findAllByOrderByCreatedAtDesc();
    List<Complaint> findByAssignedToOrderByCreatedAtDesc(Long staffUserId);
    List<Complaint> findByStatusOrderByCreatedAtDesc(ComplaintStatus status);
    long countByStatusIn(Collection<ComplaintStatus> statuses);
    long countByResidentIdAndStatusIn(Long residentId, Collection<ComplaintStatus> statuses);
}
