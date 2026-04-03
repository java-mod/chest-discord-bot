package com.example.chestbot.dto;

public record ChestDefinitionResponse(
        String chestKey,
        String displayName,
        int x,
        int y,
        int z,
        String worldHint,
        String metadataJson
) {
}
