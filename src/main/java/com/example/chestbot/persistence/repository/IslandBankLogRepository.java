package com.example.chestbot.persistence.repository;

import com.example.chestbot.persistence.entity.IslandBankLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IslandBankLogRepository extends JpaRepository<IslandBankLogEntity, Long> {
}
