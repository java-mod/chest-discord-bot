package com.example.chestbot.dto;

import com.example.chestbot.persistence.entity.ChestLogEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public final class ChestLogRow {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Integer>> MAP_TYPE = new TypeReference<>() {};
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final Long id;
    private final String playerName;
    private final String chestKey;
    private final String takenSummary;
    private final String addedSummary;
    private final String createdAt;

    public ChestLogRow(Long id, String playerName, String chestKey,
                       String takenSummary, String addedSummary, String createdAt) {
        this.id = id;
        this.playerName = playerName;
        this.chestKey = chestKey;
        this.takenSummary = takenSummary;
        this.addedSummary = addedSummary;
        this.createdAt = createdAt;
    }

    public static ChestLogRow from(ChestLogEntity e) {
        return new ChestLogRow(
                e.getId(),
                e.getPlayerName(),
                e.getChestKey(),
                summarize(e.getTakenJson()),
                summarize(e.getAddedJson()),
                ZonedDateTime.ofInstant(e.getCreatedAt(), KST).format(FMT)
        );
    }

    private static String summarize(String json) {
        if (json == null || json.isBlank() || json.equals("{}")) return "—";
        try {
            Map<String, Integer> items = MAPPER.readValue(json, MAP_TYPE);
            if (items.isEmpty()) return "—";
            return items.entrySet().stream()
                    .map(entry -> entry.getKey() + " ×" + entry.getValue())
                    .collect(Collectors.joining(", "));
        } catch (Exception ex) {
            return json;
        }
    }

    public Long getId() { return id; }
    public String getPlayerName() { return playerName; }
    public String getChestKey() { return chestKey; }
    public String getTakenSummary() { return takenSummary; }
    public String getAddedSummary() { return addedSummary; }
    public String getCreatedAt() { return createdAt; }
}
