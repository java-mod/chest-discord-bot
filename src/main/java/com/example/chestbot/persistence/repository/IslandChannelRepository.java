package com.example.chestbot.persistence.repository;

import com.example.chestbot.persistence.entity.IslandChannelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IslandChannelRepository extends JpaRepository<IslandChannelEntity, Long> {
    Optional<IslandChannelEntity> findFirstByIslandIdAndPurpose(Long islandId, String purpose);

    boolean existsByDiscordChannelIdAndPurpose(String discordChannelId, String purpose);
}
