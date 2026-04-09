package com.example.chestbot.dto;

import java.util.List;

public record LicenseConnectResponse(
        Long islandId,
        String islandName,
        String joinCode,
        long configVersion,
        List<ChestDefinitionResponse> chests,
        List<MemberHudEntryResponse> members
) {
}
