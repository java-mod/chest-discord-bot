package com.example.chestbot.dto;

import java.util.List;

public record AdminFinalizeRequest(
        String adminCode,
        List<ChestDefinitionRequest> chests
) {
}
