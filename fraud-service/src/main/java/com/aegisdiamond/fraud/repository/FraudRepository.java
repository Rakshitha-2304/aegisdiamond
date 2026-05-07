package com.aegisdiamond.fraud.repository;

import com.aegisdiamond.fraud.entity.FraudIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FraudRepository extends JpaRepository<FraudIncident, Long> {
    List<FraudIncident> findByShipmentIdOrderByDetectedAtDesc(Long shipmentId);
}
