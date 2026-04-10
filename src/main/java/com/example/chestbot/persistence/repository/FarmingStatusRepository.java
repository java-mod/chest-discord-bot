package com.example.chestbot.persistence.repository;

import com.example.chestbot.persistence.entity.FarmingStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FarmingStatusRepository extends JpaRepository<FarmingStatusEntity, Long> {
    Optional<FarmingStatusEntity> findByIslandIdAndPlayerUuid(Long islandId, String playerUuid);
    List<FarmingStatusEntity> findAllByIslandId(Long islandId);
}
