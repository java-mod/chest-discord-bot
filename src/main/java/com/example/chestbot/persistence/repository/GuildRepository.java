package com.example.chestbot.persistence.repository;

import com.example.chestbot.persistence.entity.GuildEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuildRepository extends JpaRepository<GuildEntity, Long> {
    Optional<GuildEntity> findByDiscordGuildId(String discordGuildId);
}
