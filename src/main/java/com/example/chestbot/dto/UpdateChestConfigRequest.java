package com.example.chestbot.dto;

import java.util.List;

public record UpdateChestConfigRequest(
        String createdBy,
        List<ChestDefinitionRequest> chests
) {
}
