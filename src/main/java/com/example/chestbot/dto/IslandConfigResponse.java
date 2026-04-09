package com.example.chestbot.dto;

import java.util.List;

public record IslandConfigResponse(
        Long islandId,
        String islandName,
        long configVersion,
        List<ChestDefinitionResponse> chests,
        List<MemberHudEntryResponse> members
) {
}
