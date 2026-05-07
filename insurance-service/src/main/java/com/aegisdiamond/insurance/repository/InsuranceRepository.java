package com.aegisdiamond.insurance.repository;

import com.aegisdiamond.insurance.entity.InsurancePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface InsuranceRepository extends JpaRepository<InsurancePolicy, Long> {
    Optional<InsurancePolicy> findByShipmentId(Long shipmentId);
}
