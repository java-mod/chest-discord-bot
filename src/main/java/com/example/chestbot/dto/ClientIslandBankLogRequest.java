package com.example.chestbot.dto;

public record ClientIslandBankLogRequest(
        String playerName,
        String transactionType,
        Long amount,
        Long balanceAfter,
        String note
) {
}
