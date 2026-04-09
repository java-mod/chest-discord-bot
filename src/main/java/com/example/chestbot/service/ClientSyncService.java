package com.example.chestbot.service;

import com.example.chestbot.dto.AdminConnectResponse;
import com.example.chestbot.dto.AdminFinalizeRequest;
import com.example.chestbot.dto.ClientChestLogRequest;
import com.example.chestbot.dto.ClientIslandBankLogRequest;
import com.example.chestbot.dto.IslandConfigResponse;
import com.example.chestbot.dto.LicenseConnectResponse;
import com.example.chestbot.dto.UpdateChestConfigRequest;
import com.example.chestbot.persistence.entity.AdminCodeEntity;
import com.example.chestbot.persistence.entity.ChestLogEntity;
import com.example.chestbot.persistence.entity.IslandBankLogEntity;
import com.example.chestbot.persistence.entity.IslandEntity;
import com.example.chestbot.persistence.repository.ChestLogRepository;
import com.example.chestbot.persistence.repository.IslandBankLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

@Service
public class ClientSyncService {

    private static final Logger log = LoggerFactory.getLogger(ClientSyncService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final IslandService islandService;
    private final LicenseService licenseService;
    private final ChestLogRepository chestLogRepository;
    private final IslandBankLogRepository islandBankLogRepository;
    private final DiscordService discordService;

    public ClientSyncService(
            IslandService islandService,
            LicenseService licenseService,
            ChestLogRepository chestLogRepository,
            IslandBankLogRepository islandBankLogRepository,
            DiscordService discordService
    ) {
        this.islandService = islandService;
        this.licenseService = licenseService;
        this.chestLogRepository = chestLogRepository;
        this.islandBankLogRepository = islandBankLogRepository;
        this.discordService = discordService;
    }

    @Transactional
    public IslandConfigResponse connect() {
        IslandEntity island = islandService.getSingleIslandForClient();
        // 조인 코드 연결은 라이선스 검증 없이 허용 (이벤트 전송 시 검증)
        return islandService.getActiveConfig(island.getId());
    }

    @Transactional
    public LicenseConnectResponse connectWithLicense(String licenseKey) {
        // 라이선스 키로 섬을 찾고, 유효성 검증까지 함께 수행
        IslandEntity island = licenseService.findIslandByLicenseKey(licenseKey);
        IslandConfigResponse config = islandService.getActiveConfig(island.getId());
        return new LicenseConnectResponse(
                island.getId(),
                island.getName(),
                island.getJoinCode(),
                config.configVersion(),
                config.chests(),
                config.members()
        );
    }

    @Transactional
    public AdminConnectResponse adminConnect(String adminCode) {
        AdminCodeEntity code = islandService.validateAdminCodeForSingleIsland(adminCode);
        IslandEntity island = code.getIsland();
        licenseService.requireActiveLicense(island);
        return new AdminConnectResponse(island.getId(), island.getName());
    }

    @Transactional
    public IslandConfigResponse adminFinalize(AdminFinalizeRequest request) {
        AdminCodeEntity code = islandService.validateAdminCodeForSingleIsland(request.adminCode());
        code.markUsed();
        IslandEntity island = code.getIsland();
        licenseService.requireActiveLicense(island);

        UpdateChestConfigRequest configRequest = new UpdateChestConfigRequest("admin", request.chests());
        return islandService.updateConfig(island.getId(), configRequest);
    }

    @Transactional
    public void logChestEvent(ClientChestLogRequest request) {
        IslandEntity island = islandService.getSingleIslandForClient();
        licenseService.requireActiveLicense(island);
        Map<String, Integer> taken = request.taken() == null ? Collections.emptyMap() : request.taken();
        Map<String, Integer> added = request.added() == null ? Collections.emptyMap() : request.added();

        Instant now = Instant.now();
        chestLogRepository.save(new ChestLogEntity(
                island,
                island.getName(),
                island.getJoinCode(),
                request.configVersion(),
                request.playerName(),
                request.playerUuid(),
                request.skinTexture(),
                request.takenVisualData(),
                request.addedVisualData(),
                request.chestKey(),
                toJson(taken),
                toJson(added),
                now
        ));

        log.info("상자 로그 DB 저장: island={}, chest={}, player={}, takenItems={}, addedItems={}, configVersion={}",
                island.getName(), request.chestKey(), request.playerName(), taken.size(), added.size(), request.configVersion());

        String channelId = islandService.getLogChannelId(island.getId());
        if (channelId == null) {
            log.warn("상자 로그 Discord 전송 스킵: island={} 에 로그 채널이 연결되지 않았습니다", island.getName());
            return;
        }

        if (taken.isEmpty() && added.isEmpty()) {
            log.info("상자 로그 Discord 전송 스킵: island={}, chest={} 에 순변화가 없습니다", island.getName(), request.chestKey());
            return;
        }

        ChestLogDiscordPayload payload = format(taken, added);
        discordService.sendChestLogEmbed(
                channelId,
                island.getName(),
                request.playerName(),
                request.chestKey(),
                payload.takenText(),
                payload.addedText(),
                now
        );
    }

    @Transactional
    public void logIslandBankEvent(ClientIslandBankLogRequest request) {
        validateIslandBankEvent(request);
        IslandEntity island = islandService.getSingleIslandForClient();
        licenseService.requireActiveLicense(island);

        String normalizedCode = island.getJoinCode().trim().toUpperCase();
        String note = sanitizeNote(request.note());
        Instant now = Instant.now();

        islandBankLogRepository.save(new IslandBankLogEntity(
                island,
                island.getName(),
                normalizedCode,
                request.playerName(),
                request.transactionType(),
                request.amount(),
                request.balanceAfter(),
                note,
                now
        ));

        log.info("섬 은행 로그 DB 저장: island={}, player={}, transactionType={}, amount={}",
                island.getName(), request.playerName(), request.transactionType(), request.amount());

        String channelId = islandService.getBankLogChannelId(island.getId());
        if (channelId == null) {
            log.warn("섬 은행 로그 Discord 전송 스킵: island={} 에 은행 로그 채널이 연결되지 않았습니다", island.getName());
            return;
        }

        discordService.sendIslandBankLogEmbed(
                channelId,
                island.getName(),
                request.playerName(),
                request.transactionType(),
                request.amount(),
                request.balanceAfter(),
                request.note(),
                now
        );
    }

    private void validateIslandBankEvent(ClientIslandBankLogRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bank log request is required");
        }
        requireText(request.playerName(), "playerName");
        requireText(request.transactionType(), "transactionType");
        if (request.amount() == null || request.amount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be greater than 0");
        }

        String normalizedType = request.transactionType().trim().toUpperCase();
        if (!normalizedType.equals("DEPOSIT") && !normalizedType.equals("WITHDRAW")
                && !normalizedType.equals("입금") && !normalizedType.equals("출금")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported transactionType: " + request.transactionType());
        }
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
    }

    private String sanitizeNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        String trimmed = note.trim();
        return trimmed.length() <= 500 ? trimmed : trimmed.substring(0, 500);
    }

    private String toJson(Map<String, Integer> map) {
        try {
            return OBJECT_MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("아이템 맵 JSON 직렬화 실패", e);
        }
    }

    private ChestLogDiscordPayload format(Map<String, Integer> taken, Map<String, Integer> added) {
        return new ChestLogDiscordPayload(
                formatItems(taken),
                formatItems(added)
        );
    }

    private String formatItems(Map<String, Integer> items) {
        if (items == null || items.isEmpty()) {
            return "없음";
        }
        StringBuilder sb = new StringBuilder();
        items.forEach((item, count) -> {
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append("• ").append(item).append(" ×").append(count);
        });
        return sb.toString();
    }

    private record ChestLogDiscordPayload(String takenText, String addedText) {
    }
}
