package com.example.chestbot.dto;

import java.util.Map;

public record ClientChestLogRequest(
        String joinCode,
        long configVersion,
        String playerName,
        String playerUuid,
        String skinTexture,
        String takenVisualData,
        String addedVisualData,
        String chestKey,
        Map<String, Integer> taken,
        Map<String, Integer> added
) {
}
