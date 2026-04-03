package com.example.chestbot.dto;

public record IslandSummaryResponse(
        Long id,
        Long guildId,
        String name,
        String slug,
        String status
) {
}
