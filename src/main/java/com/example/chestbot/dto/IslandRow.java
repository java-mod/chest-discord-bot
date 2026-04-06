package com.example.chestbot.dto;

import com.example.chestbot.persistence.entity.IslandEntity;
import com.example.chestbot.persistence.entity.LicenseEntity;

public final class IslandRow {

    private final Long id;
    private final String name;
    private final String joinCode;
    private final Long licenseId;
    private final String licenseKey;
    private final boolean licenseActive;

    public IslandRow(Long id, String name, String joinCode,
                     Long licenseId, String licenseKey, boolean licenseActive) {
        this.id = id;
        this.name = name;
        this.joinCode = joinCode;
        this.licenseId = licenseId;
        this.licenseKey = licenseKey;
        this.licenseActive = licenseActive;
    }

    public static IslandRow from(IslandEntity island) {
        LicenseEntity lic = island.getLicense();
        return new IslandRow(
                island.getId(),
                island.getName(),
                island.getJoinCode(),
                lic != null ? lic.getId() : null,
                lic != null ? lic.getLicenseKey() : null,
                lic != null && lic.isActive()
        );
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getJoinCode() { return joinCode; }
    public Long getLicenseId() { return licenseId; }
    public String getLicenseKey() { return licenseKey; }
    public boolean isLicenseActive() { return licenseActive; }
    public boolean isHasLicense() { return licenseId != null; }
}
