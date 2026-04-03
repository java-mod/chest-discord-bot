package com.example.chestbot.persistence.repository;

import com.example.chestbot.persistence.entity.ChestConfigVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChestConfigVersionRepository extends JpaRepository<ChestConfigVersionEntity, Long> {
    Optional<ChestConfigVersionEntity> findFirstByIslandIdAndActiveTrue(Long islandId);

    List<ChestConfigVersionEntity> findAllByIslandId(Long islandId);

    Optional<ChestConfigVersionEntity> findFirstByIslandIdOrderByVersionNumberDesc(Long islandId);
}
