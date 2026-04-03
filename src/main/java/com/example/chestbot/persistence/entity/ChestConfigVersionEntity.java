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
@Table(name = "chest_config_versions")
public class ChestConfigVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "island_id", nullable = false)
    private IslandEntity island;

    @Column(nullable = false)
    private long versionNumber;

    @Column(nullable = false, length = 64)
    private String createdBy;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Instant createdAt;

    protected ChestConfigVersionEntity() {
    }

    public ChestConfigVersionEntity(IslandEntity island, long versionNumber, String createdBy, boolean active) {
        this.island = island;
        this.versionNumber = versionNumber;
        this.createdBy = createdBy;
        this.active = active;
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

    public long getVersionNumber() {
        return versionNumber;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
