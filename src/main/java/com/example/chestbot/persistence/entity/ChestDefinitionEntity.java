package com.example.chestbot.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "chest_definitions")
public class ChestDefinitionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "config_version_id", nullable = false)
    private ChestConfigVersionEntity configVersion;

    @Column(nullable = false, length = 120)
    private String chestKey;

    @Column(nullable = false, length = 120)
    private String displayName;

    @Column(nullable = false)
    private int x;

    @Column(nullable = false)
    private int y;

    @Column(nullable = false)
    private int z;

    @Column(length = 120)
    private String worldHint;

    @Lob
    private String metadataJson;

    protected ChestDefinitionEntity() {
    }

    public ChestDefinitionEntity(
            ChestConfigVersionEntity configVersion,
            String chestKey,
            String displayName,
            int x,
            int y,
            int z,
            String worldHint,
            String metadataJson
    ) {
        this.configVersion = configVersion;
        this.chestKey = chestKey;
        this.displayName = displayName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.worldHint = worldHint;
        this.metadataJson = metadataJson;
    }

    public String getChestKey() {
        return chestKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public String getWorldHint() {
        return worldHint;
    }

    public String getMetadataJson() {
        return metadataJson;
    }
}
