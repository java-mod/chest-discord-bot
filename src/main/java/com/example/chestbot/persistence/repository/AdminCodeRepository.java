package com.example.chestbot.persistence.repository;

import com.example.chestbot.persistence.entity.AdminCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminCodeRepository extends JpaRepository<AdminCodeEntity, Long> {
    Optional<AdminCodeEntity> findByCodeAndUsedFalse(String code);
}
