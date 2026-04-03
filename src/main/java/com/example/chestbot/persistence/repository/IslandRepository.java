package com.example.chestbot.persistence.repository;

import com.example.chestbot.persistence.entity.IslandEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IslandRepository extends JpaRepository<IslandEntity, Long> {
    Optional<IslandEntity> findByGuildIdAndSlug(Long guildId, String slug);
    Optional<IslandEntity> findFirstByGuildId(Long guildId);
    Optional<IslandEntity> findByJoinCode(String joinCode);
}
