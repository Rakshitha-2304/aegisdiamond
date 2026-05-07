package com.aegisdiamond.tracking.repository;

import com.aegisdiamond.tracking.entity.TrackingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrackingRepository extends JpaRepository<TrackingRecord, Long> {
    List<TrackingRecord> findByShipmentIdOrderByTimestampDesc(Long shipmentId);
    Optional<TrackingRecord> findFirstByShipmentIdOrderByTimestampDesc(Long shipmentId);
}
