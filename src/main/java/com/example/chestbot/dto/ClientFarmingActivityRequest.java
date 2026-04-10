package com.example.chestbot.dto;

public record ClientFarmingActivityRequest(
        long configVersion,
        String playerName,
        String playerUuid,
        String skinTexture,
        String cropKey,
        long activityAtMillis
) {
}
