package com.example.chestbot.dto;

public record ClientFarmingDeclareRequest(
        long configVersion,
        String playerName,
        String playerUuid,
        String skinTexture,
        String cropKey
) {
}
