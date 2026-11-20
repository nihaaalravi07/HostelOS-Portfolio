package com.hostelos.checkout;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckoutRepository extends JpaRepository<CheckoutRequest, Long> {
    List<CheckoutRequest> findByResidentIdOrderByRequestedOnDescIdDesc(Long residentId);
    List<CheckoutRequest> findAllByOrderByRequestedOnDescIdDesc();
    boolean existsByResidentIdAndStatus(Long residentId, CheckoutStatus status);
}
