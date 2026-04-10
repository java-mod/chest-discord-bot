package com.example.chestbot.dto;

public record MemberHudEntryResponse(
        String playerName,
        String itemName,
        int itemCount,
        int totalKinds,
        int totalItems,
        String itemVisualData,
        String chestName,
        String playerUuid,
        String skinTexture,
        long updatedAtMillis,
        String farmingCropKey,
        long farmingUpdatedAtMillis
) {
}
