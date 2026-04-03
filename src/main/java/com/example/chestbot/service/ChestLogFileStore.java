package com.example.chestbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Map;

@Service
public class ChestLogFileStore {

    private static final Logger log = LoggerFactory.getLogger(ChestLogFileStore.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path LOG_PATH = Path.of("data", "chest-log-events.jsonl");

    public synchronized void append(
            String islandName,
            String joinCode,
            long configVersion,
            String playerName,
            String chestKey,
            Map<String, Integer> taken,
            Map<String, Integer> added
    ) {
        ChestLogFileRecord record = new ChestLogFileRecord(
                Instant.now().toString(),
                islandName,
                joinCode,
                configVersion,
                playerName,
                chestKey,
                taken,
                added
        );

        try {
            Files.createDirectories(LOG_PATH.getParent());
            String line = OBJECT_MAPPER.writeValueAsString(record) + System.lineSeparator();
            Files.writeString(
                    LOG_PATH,
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("상자 로그 JSON 직렬화 실패", e);
        } catch (IOException e) {
            throw new IllegalStateException("상자 로그 파일 저장 실패: " + LOG_PATH, e);
        }
    }

    public Path getLogPath() {
        return LOG_PATH;
    }

    public record ChestLogFileRecord(
            String createdAt,
            String islandName,
            String joinCode,
            long configVersion,
            String playerName,
            String chestKey,
            Map<String, Integer> taken,
            Map<String, Integer> added
    ) {
    }
}
