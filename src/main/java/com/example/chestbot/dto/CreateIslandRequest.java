package com.example.chestbot.dto;

public record CreateIslandRequest(
        Long guildId,
        String name,
        String slug
) {
}
