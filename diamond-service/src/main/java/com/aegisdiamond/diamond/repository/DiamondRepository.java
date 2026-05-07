package com.aegisdiamond.diamond.repository;

import com.aegisdiamond.diamond.entity.Diamond;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiamondRepository extends JpaRepository<Diamond, String> {
    Optional<Diamond> findByCertificateId(String certificateId);
    List<Diamond> findByCutContainingOrClarityContainingOrColorContaining(String cut, String clarity, String color);
}
