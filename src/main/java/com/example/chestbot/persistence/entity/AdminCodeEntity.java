package com.example.chestbot.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "admin_codes")
public class AdminCodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "island_id", nullable = false)
    private IslandEntity island;

    @Column(nullable = false, unique = true, length = 16)
    private String code;

    @Column(nullable = false)
    private boolean used;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt;

    protected AdminCodeEntity() {
    }

    public AdminCodeEntity(IslandEntity island, String code, Instant expiresAt) {
        this.island = island;
        this.code = code;
        this.used = false;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public IslandEntity getIsland() {
        return island;
    }

    public String getCode() {
        return code;
    }

    public boolean isUsed() {
        return used;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void markUsed() {
        this.used = true;
    }
}
