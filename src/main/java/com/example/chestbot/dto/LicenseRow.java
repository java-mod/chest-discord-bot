package com.example.chestbot.dto;

import com.example.chestbot.persistence.entity.IslandEntity;
import com.example.chestbot.persistence.entity.LicenseEntity;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class LicenseRow {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final Long id;
    private final String licenseKey;
    private final String islandName;
    private final String ownerNote;
    private final boolean active;
    private final String createdAt;
    private final String lastUsedAt;
    private final Long assignedIslandId;
    private final String assignedIslandName;

    public LicenseRow(Long id, String licenseKey, String islandName, String ownerNote,
                      boolean active, String createdAt, String lastUsedAt,
                      Long assignedIslandId, String assignedIslandName) {
        this.id = id;
        this.licenseKey = licenseKey;
        this.islandName = islandName;
        this.ownerNote = ownerNote;
        this.active = active;
        this.createdAt = createdAt;
        this.lastUsedAt = lastUsedAt;
        this.assignedIslandId = assignedIslandId;
        this.assignedIslandName = assignedIslandName;
    }

    public static LicenseRow from(LicenseEntity lic, IslandEntity assignedIsland) {
        return new LicenseRow(
                lic.getId(),
                lic.getLicenseKey(),
                lic.getIslandName(),
                lic.getOwnerNote(),
                lic.isActive(),
                fmt(lic.getCreatedAt()),
                lic.getLastUsedAt() != null ? fmt(lic.getLastUsedAt()) : "미사용",
                assignedIsland != null ? assignedIsland.getId() : null,
                assignedIsland != null ? assignedIsland.getName() : null
        );
    }

    private static String fmt(java.time.Instant instant) {
        return ZonedDateTime.ofInstant(instant, KST).format(FMT);
    }

    public Long getId() { return id; }
    public String getLicenseKey() { return licenseKey; }
    public String getIslandName() { return islandName; }
    public String getOwnerNote() { return ownerNote; }
    public boolean isActive() { return active; }
    public String getCreatedAt() { return createdAt; }
    public String getLastUsedAt() { return lastUsedAt; }
    public Long getAssignedIslandId() { return assignedIslandId; }
    public String getAssignedIslandName() { return assignedIslandName; }
}
