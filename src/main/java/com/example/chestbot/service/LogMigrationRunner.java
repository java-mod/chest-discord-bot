package com.example.chestbot.service;

import com.example.chestbot.persistence.entity.ChestLogEntity;
import com.example.chestbot.persistence.entity.IslandBankLogEntity;
import com.example.chestbot.persistence.entity.IslandEntity;
import com.example.chestbot.persistence.repository.ChestLogRepository;
import com.example.chestbot.persistence.repository.IslandBankLogRepository;
import com.example.chestbot.persistence.repository.IslandRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

@Component
@Order(1)
public class LogMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LogMigrationRunner.class);
    private static final Path CHEST_LOG_PATH = Path.of("data", "chest-log-events.jsonl");
    private static final Path BANK_LOG_PATH = Path.of("data", "island-bank-log-events.jsonl");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${app.log-migration.enabled:true}")
    private boolean enabled;

    private final ChestLogRepository chestLogRepository;
    private final IslandBankLogRepository islandBankLogRepository;
    private final IslandRepository islandRepository;

    public LogMigrationRunner(
            ChestLogRepository chestLogRepository,
            IslandBankLogRepository islandBankLogRepository,
            IslandRepository islandRepository
    ) {
        this.chestLogRepository = chestLogRepository;
        this.islandBankLogRepository = islandBankLogRepository;
        this.islandRepository = islandRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("JSONL → SQL 로그 마이그레이션 비활성화됨");
            return;
        }
        migrateChestLogs();
        migrateIslandBankLogs();
    }

    @Transactional
    public void migrateChestLogs() {
        if (!Files.exists(CHEST_LOG_PATH)) {
            return;
        }
        if (chestLogRepository.count() > 0) {
            log.info("상자 로그 마이그레이션 스킵: 이미 DB에 데이터가 있습니다");
            return;
        }

        log.info("상자 로그 JSONL → SQL 마이그레이션 시작: {}", CHEST_LOG_PATH);
        int success = 0;
        int failed = 0;

        try {
            for (String line : Files.readAllLines(CHEST_LOG_PATH)) {
                if (line.isBlank()) continue;
                try {
                    JsonNode node = OBJECT_MAPPER.readTree(line);

                    String joinCode = node.path("joinCode").asText(null);
                    String islandName = node.path("islandName").asText("");
                    long configVersion = node.path("configVersion").asLong(0);
                    String playerName = node.path("playerName").asText("");
                    String chestKey = node.path("chestKey").asText("");
                    Instant createdAt = parseInstant(node.path("createdAt").asText(null));

                    Map<String, Integer> taken = OBJECT_MAPPER.convertValue(
                            node.path("taken"), new TypeReference<Map<String, Integer>>() {});
                    Map<String, Integer> added = OBJECT_MAPPER.convertValue(
                            node.path("added"), new TypeReference<Map<String, Integer>>() {});

                    String takenJson = OBJECT_MAPPER.writeValueAsString(taken != null ? taken : Map.of());
                    String addedJson = OBJECT_MAPPER.writeValueAsString(added != null ? added : Map.of());

                    IslandEntity island = joinCode != null
                            ? islandRepository.findByJoinCode(joinCode.trim().toUpperCase()).orElse(null)
                            : null;

                    chestLogRepository.save(new ChestLogEntity(
                            island, islandName, joinCode != null ? joinCode : "",
                            configVersion, playerName, null, null, null, null, chestKey,
                            takenJson, addedJson, createdAt
                    ));
                    success++;
                } catch (Exception e) {
                    log.warn("상자 로그 라인 마이그레이션 실패 (스킵): {}", e.getMessage());
                    failed++;
                }
            }
        } catch (IOException e) {
            log.error("상자 로그 파일 읽기 실패: {}", e.getMessage());
            return;
        }

        log.info("상자 로그 마이그레이션 완료: 성공={}, 실패={}", success, failed);
    }

    @Transactional
    public void migrateIslandBankLogs() {
        if (!Files.exists(BANK_LOG_PATH)) {
            return;
        }
        if (islandBankLogRepository.count() > 0) {
            log.info("섬 은행 로그 마이그레이션 스킵: 이미 DB에 데이터가 있습니다");
            return;
        }

        log.info("섬 은행 로그 JSONL → SQL 마이그레이션 시작: {}", BANK_LOG_PATH);
        int success = 0;
        int failed = 0;

        try {
            for (String line : Files.readAllLines(BANK_LOG_PATH)) {
                if (line.isBlank()) continue;
                try {
                    JsonNode node = OBJECT_MAPPER.readTree(line);

                    String joinCode = node.path("joinCode").asText(null);
                    String normalizedCode = joinCode != null ? joinCode.trim().toUpperCase() : "";
                    String islandName = node.path("islandName").asText("");
                    String playerName = node.path("playerName").asText("");
                    String transactionType = node.path("transactionType").asText("");
                    long amount = node.path("amount").asLong(0);
                    Long balanceAfter = node.path("balanceAfter").isNull() ? null : node.path("balanceAfter").asLong();
                    String note = node.path("note").isNull() ? null : node.path("note").asText(null);
                    Instant createdAt = parseInstant(node.path("createdAt").asText(null));

                    IslandEntity island = !normalizedCode.isEmpty()
                            ? islandRepository.findByJoinCode(normalizedCode).orElse(null)
                            : null;

                    islandBankLogRepository.save(new IslandBankLogEntity(
                            island, islandName, normalizedCode,
                            playerName, transactionType, amount,
                            balanceAfter, note, createdAt
                    ));
                    success++;
                } catch (Exception e) {
                    log.warn("섬 은행 로그 라인 마이그레이션 실패 (스킵): {}", e.getMessage());
                    failed++;
                }
            }
        } catch (IOException e) {
            log.error("섬 은행 로그 파일 읽기 실패: {}", e.getMessage());
            return;
        }

        log.info("섬 은행 로그 마이그레이션 완료: 성공={}, 실패={}", success, failed);
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            return Instant.now();
        }
    }
}
