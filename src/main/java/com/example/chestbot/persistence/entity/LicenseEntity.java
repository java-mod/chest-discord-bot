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
@Table(name = "licenses")
public class LicenseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String licenseKey;

    @Column(nullable = false, length = 120)
    private String islandName;

    @Column(length = 255)
    private String ownerNote;

    @Column(nullable = false)
    private boolean active;

    @Column
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant lastUsedAt;

    protected LicenseEntity() {
    }

    public LicenseEntity(String licenseKey, String islandName, String ownerNote) {
        this.licenseKey = licenseKey;
        this.islandName = islandName;
        this.ownerNote = ownerNote;
        this.active = true;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public String getLicenseKey() { return licenseKey; }
    public String getIslandName() { return islandName; }
    public void setIslandName(String islandName) { this.islandName = islandName; }
    public String getOwnerNote() { return ownerNote; }
    public void setOwnerNote(String ownerNote) { this.ownerNote = ownerNote; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}
