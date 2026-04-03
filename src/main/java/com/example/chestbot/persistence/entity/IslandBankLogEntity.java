package com.example.chestbot.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "island_bank_log_events", indexes = {
        @Index(name = "idx_bank_log_island", columnList = "island_id"),
        @Index(name = "idx_bank_log_created", columnList = "created_at")
})
public class IslandBankLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "island_id")
    private IslandEntity island;

    @Column(nullable = false, length = 120)
    private String islandName;

    @Column(nullable = false, length = 16)
    private String joinCode;

    @Column(nullable = false, length = 64)
    private String playerName;

    @Column(nullable = false, length = 32)
    private String transactionType;

    @Column(nullable = false)
    private long amount;

    private Long balanceAfter;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private Instant createdAt;

    protected IslandBankLogEntity() {
    }

    public IslandBankLogEntity(
            IslandEntity island,
            String islandName,
            String joinCode,
            String playerName,
            String transactionType,
            long amount,
            Long balanceAfter,
            String note,
            Instant createdAt
    ) {
        this.island = island;
        this.islandName = islandName;
        this.joinCode = joinCode;
        this.playerName = playerName;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.note = note;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public IslandEntity getIsland() { return island; }
    public String getIslandName() { return islandName; }
    public String getJoinCode() { return joinCode; }
    public String getPlayerName() { return playerName; }
    public String getTransactionType() { return transactionType; }
    public long getAmount() { return amount; }
    public Long getBalanceAfter() { return balanceAfter; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
}
