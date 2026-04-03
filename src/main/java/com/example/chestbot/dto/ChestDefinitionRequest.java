package com.example.chestbot.dto;

public record ChestDefinitionRequest(
        String chestKey,
        String displayName,
        int x,
        int y,
        int z,
        String worldHint,
        String metadataJson
) {
}
