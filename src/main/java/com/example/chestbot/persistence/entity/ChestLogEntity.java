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
@Table(name = "chest_log_events", indexes = {
        @Index(name = "idx_chest_log_island", columnList = "island_id"),
        @Index(name = "idx_chest_log_created", columnList = "created_at")
})
public class ChestLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "island_id")
    private IslandEntity island;

    @Column(nullable = false, length = 120)
    private String islandName;

    @Column(nullable = false, length = 16)
    private String joinCode;

    @Column(nullable = false)
    private long configVersion;

    @Column(nullable = false, length = 64)
    private String playerName;

    @Column(length = 64)
    private String playerUuid;

    @Column(length = 255)
    private String skinTexture;

    @Column(columnDefinition = "LONGTEXT")
    private String takenVisualData;

    @Column(columnDefinition = "LONGTEXT")
    private String addedVisualData;

    @Column(nullable = false, length = 120)
    private String chestKey;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String takenJson;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String addedJson;

    @Column(nullable = false)
    private Instant createdAt;

    protected ChestLogEntity() {
    }

    public ChestLogEntity(
            IslandEntity island,
            String islandName,
            String joinCode,
            long configVersion,
            String playerName,
            String playerUuid,
            String skinTexture,
            String takenVisualData,
            String addedVisualData,
            String chestKey,
            String takenJson,
            String addedJson,
            Instant createdAt
    ) {
        this.island = island;
        this.islandName = islandName;
        this.joinCode = joinCode;
        this.configVersion = configVersion;
        this.playerName = playerName;
        this.playerUuid = playerUuid;
        this.skinTexture = skinTexture;
        this.takenVisualData = takenVisualData;
        this.addedVisualData = addedVisualData;
        this.chestKey = chestKey;
        this.takenJson = takenJson;
        this.addedJson = addedJson;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public IslandEntity getIsland() { return island; }
    public String getIslandName() { return islandName; }
    public String getJoinCode() { return joinCode; }
    public long getConfigVersion() { return configVersion; }
    public String getPlayerName() { return playerName; }
    public String getPlayerUuid() { return playerUuid; }
    public String getSkinTexture() { return skinTexture; }
    public String getTakenVisualData() { return takenVisualData; }
    public String getAddedVisualData() { return addedVisualData; }
    public String getChestKey() { return chestKey; }
    public String getTakenJson() { return takenJson; }
    public String getAddedJson() { return addedJson; }
    public Instant getCreatedAt() { return createdAt; }
}
