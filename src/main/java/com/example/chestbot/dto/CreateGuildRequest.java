package com.example.chestbot.dto;

public record CreateGuildRequest(
        String discordGuildId,
        String name
) {
}
