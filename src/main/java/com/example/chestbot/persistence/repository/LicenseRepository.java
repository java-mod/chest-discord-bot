package com.example.chestbot.persistence.repository;

import com.example.chestbot.persistence.entity.LicenseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LicenseRepository extends JpaRepository<LicenseEntity, Long> {
    Optional<LicenseEntity> findByLicenseKey(String licenseKey);
    List<LicenseEntity> findAllByOrderByCreatedAtDesc();
}
