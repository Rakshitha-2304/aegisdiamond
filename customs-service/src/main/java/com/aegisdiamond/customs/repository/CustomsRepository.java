package com.aegisdiamond.customs.repository;

import com.aegisdiamond.customs.entity.CustomsDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CustomsRepository extends JpaRepository<CustomsDeclaration, Long> {
    Optional<CustomsDeclaration> findByShipmentId(Long shipmentId);
}
