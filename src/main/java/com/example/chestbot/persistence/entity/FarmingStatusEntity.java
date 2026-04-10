package com.example.chestbot.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "farming_status", indexes = {
        @Index(name = "idx_farming_island", columnList = "island_id"),
        @Index(name = "idx_farming_player_uuid", columnList = "playerUuid")
})
public class FarmingStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "island_id", nullable = false)
    private IslandEntity island;

    @Column(nullable = false, length = 64)
    private String playerName;

    @Column(nullable = false, length = 64)
    private String playerUuid;

    @Column(length = 255)
    private String skinTexture;

    @Column(nullable = false, length = 64)
    private String cropKey;

    @Column
    private Instant lastFarmingAt;

    protected FarmingStatusEntity() {
    }

    public FarmingStatusEntity(IslandEntity island, String playerName, String playerUuid, String skinTexture, String cropKey, Instant lastFarmingAt) {
        this.island = island;
        this.playerName = playerName;
        this.playerUuid = playerUuid;
        this.skinTexture = skinTexture;
        this.cropKey = cropKey;
        this.lastFarmingAt = lastFarmingAt;
    }

    public Long getId() { return id; }
    public IslandEntity getIsland() { return island; }
    public String getPlayerName() { return playerName; }
    public String getPlayerUuid() { return playerUuid; }
    public String getSkinTexture() { return skinTexture; }
    public String getCropKey() { return cropKey; }
    public Instant getLastFarmingAt() { return lastFarmingAt; }

    public void updateProfile(String playerName, String skinTexture) {
        this.playerName = playerName;
        this.skinTexture = skinTexture;
    }

    public void updateCrop(String cropKey, Instant lastFarmingAt) {
        this.cropKey = cropKey;
        this.lastFarmingAt = lastFarmingAt;
    }

    public void updateActivity(Instant lastFarmingAt, String skinTexture) {
        this.lastFarmingAt = lastFarmingAt;
        this.skinTexture = skinTexture;
    }
}
