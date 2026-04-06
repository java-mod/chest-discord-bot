package com.example.chestbot.dto;

import com.example.chestbot.persistence.entity.IslandBankLogEntity;

import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class BankLogRow {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final Long id;
    private final String playerName;
    private final String transactionType;
    private final String transactionLabel;
    private final String amount;
    private final String note;
    private final String createdAt;

    public BankLogRow(Long id, String playerName, String transactionType, String transactionLabel,
                      String amount, String note, String createdAt) {
        this.id = id;
        this.playerName = playerName;
        this.transactionType = transactionType;
        this.transactionLabel = transactionLabel;
        this.amount = amount;
        this.note = note;
        this.createdAt = createdAt;
    }

    public static BankLogRow from(IslandBankLogEntity e) {
        String label = "DEPOSIT".equalsIgnoreCase(e.getTransactionType()) ? "입금" : "출금";
        String formatted = NumberFormat.getNumberInstance(Locale.KOREA).format(e.getAmount()) + "원";
        String note = (e.getNote() != null && !e.getNote().isBlank()) ? e.getNote() : "—";
        return new BankLogRow(
                e.getId(),
                e.getPlayerName(),
                e.getTransactionType(),
                label,
                formatted,
                note,
                ZonedDateTime.ofInstant(e.getCreatedAt(), KST).format(FMT)
        );
    }

    public Long getId() { return id; }
    public String getPlayerName() { return playerName; }
    public String getTransactionType() { return transactionType; }
    public String getTransactionLabel() { return transactionLabel; }
    public String getAmount() { return amount; }
    public String getNote() { return note; }
    public String getCreatedAt() { return createdAt; }
    public boolean isDeposit() { return "DEPOSIT".equalsIgnoreCase(transactionType); }
}
