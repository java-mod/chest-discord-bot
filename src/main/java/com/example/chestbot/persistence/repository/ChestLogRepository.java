package com.example.chestbot.persistence.repository;

import com.example.chestbot.persistence.entity.ChestLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChestLogRepository extends JpaRepository<ChestLogEntity, Long> {
}
