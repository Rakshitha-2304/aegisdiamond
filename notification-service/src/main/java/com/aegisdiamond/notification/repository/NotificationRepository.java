package com.aegisdiamond.notification.repository;

import com.aegisdiamond.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByShipmentIdOrderByTimestampDesc(Long shipmentId);
}
