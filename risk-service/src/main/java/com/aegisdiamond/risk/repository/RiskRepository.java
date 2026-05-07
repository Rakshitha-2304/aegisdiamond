package com.aegisdiamond.risk.repository;

import com.aegisdiamond.risk.entity.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RiskRepository extends JpaRepository<RiskAssessment, Long> {
    List<RiskAssessment> findByShipmentIdOrderByAssessedAtDesc(Long shipmentId);
}
