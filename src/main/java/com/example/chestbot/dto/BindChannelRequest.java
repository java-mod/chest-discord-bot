package com.example.chestbot.dto;

public record BindChannelRequest(
        String discordChannelId,
        String purpose
) {
}
