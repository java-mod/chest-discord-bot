package com.example.chestbot.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "island_channels")
public class IslandChannelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "island_id", nullable = false)
    private IslandEntity island;

    @Column(nullable = false, length = 32)
    private String discordChannelId;

    @Column(nullable = false, length = 32)
    private String purpose;

    @Column(nullable = false)
    private Instant createdAt;

    protected IslandChannelEntity() {
    }

    public IslandChannelEntity(IslandEntity island, String discordChannelId, String purpose) {
        this.island = island;
        this.discordChannelId = discordChannelId;
        this.purpose = purpose;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getDiscordChannelId() {
        return discordChannelId;
    }
}
