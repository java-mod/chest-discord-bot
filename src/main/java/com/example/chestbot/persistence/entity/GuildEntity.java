package com.example.chestbot.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "guilds")
public class GuildEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String discordGuildId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private Instant createdAt;

    protected GuildEntity() {
    }

    public GuildEntity(String discordGuildId, String name) {
        this.discordGuildId = discordGuildId;
        this.name = name;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getDiscordGuildId() {
        return discordGuildId;
    }

    public String getName() {
        return name;
    }
}
