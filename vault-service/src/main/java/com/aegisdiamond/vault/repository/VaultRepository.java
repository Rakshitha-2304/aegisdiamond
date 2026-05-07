package com.aegisdiamond.vault.repository;

import com.aegisdiamond.vault.entity.Vault;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VaultRepository extends JpaRepository<Vault, Long> {
}
