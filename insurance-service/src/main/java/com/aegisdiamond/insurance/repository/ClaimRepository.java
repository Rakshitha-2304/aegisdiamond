package com.aegisdiamond.insurance.repository;

import com.aegisdiamond.insurance.entity.InsuranceClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClaimRepository extends JpaRepository<InsuranceClaim, Long> {
}
