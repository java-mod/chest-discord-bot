package com.example.chestbot.service;

import com.example.chestbot.dto.ChestDefinitionRequest;
import com.example.chestbot.dto.ChestDefinitionResponse;
import com.example.chestbot.dto.CreateGuildRequest;
import com.example.chestbot.dto.IslandConfigResponse;
import com.example.chestbot.dto.MemberHudEntryResponse;
import com.example.chestbot.dto.IslandSummaryResponse;
import com.example.chestbot.dto.UpdateChestConfigRequest;
import com.example.chestbot.persistence.entity.AdminCodeEntity;
import com.example.chestbot.persistence.entity.ChestConfigVersionEntity;
import com.example.chestbot.persistence.entity.ChestDefinitionEntity;
import com.example.chestbot.persistence.entity.ChestLogEntity;
import com.example.chestbot.persistence.entity.FarmingStatusEntity;
import com.example.chestbot.persistence.entity.GuildEntity;
import com.example.chestbot.persistence.entity.IslandChannelEntity;
import com.example.chestbot.persistence.entity.IslandEntity;
import com.example.chestbot.persistence.repository.AdminCodeRepository;
import com.example.chestbot.persistence.repository.ChestConfigVersionRepository;
import com.example.chestbot.persistence.repository.ChestDefinitionRepository;
import com.example.chestbot.persistence.repository.ChestLogRepository;
import com.example.chestbot.persistence.repository.FarmingStatusRepository;
import com.example.chestbot.persistence.repository.GuildRepository;
import com.example.chestbot.persistence.repository.IslandChannelRepository;
import com.example.chestbot.persistence.repository.IslandRepository;
import com.example.chestbot.util.TextSanitizer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class IslandService {

    public static final String CHEST_LOG_PURPOSE = "CHEST_LOG";
    public static final String BANK_LOG_PURPOSE = "BANK_LOG";
    private static final Duration FARMING_ACTIVE_WINDOW = Duration.ofMinutes(5);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom rng = new SecureRandom();

    private final GuildRepository guildRepository;
    private final IslandRepository islandRepository;
    private final IslandChannelRepository islandChannelRepository;
    private final ChestConfigVersionRepository chestConfigVersionRepository;
    private final ChestDefinitionRepository chestDefinitionRepository;
    private final ChestLogRepository chestLogRepository;
    private final AdminCodeRepository adminCodeRepository;
    private final FarmingStatusRepository farmingStatusRepository;

    public IslandService(
            GuildRepository guildRepository,
            IslandRepository islandRepository,
            IslandChannelRepository islandChannelRepository,
            ChestConfigVersionRepository chestConfigVersionRepository,
            ChestDefinitionRepository chestDefinitionRepository,
            ChestLogRepository chestLogRepository,
            AdminCodeRepository adminCodeRepository,
            FarmingStatusRepository farmingStatusRepository
    ) {
        this.guildRepository = guildRepository;
        this.islandRepository = islandRepository;
        this.islandChannelRepository = islandChannelRepository;
        this.chestConfigVersionRepository = chestConfigVersionRepository;
        this.chestDefinitionRepository = chestDefinitionRepository;
        this.chestLogRepository = chestLogRepository;
        this.adminCodeRepository = adminCodeRepository;
        this.farmingStatusRepository = farmingStatusRepository;
    }

    // ── 슬래시 커맨드용 통합 셋업 ──────────────────────────────────

    @Transactional
    public IslandEntity setupIsland(String discordGuildId, String guildName, String islandName) {
        requireText(discordGuildId, "discordGuildId");
        requireText(islandName, "islandName");
        String sanitizedGuildName = sanitizeGuildName(guildName, discordGuildId);
        GuildEntity guild = guildRepository.findByDiscordGuildId(discordGuildId)
                .orElseGet(() -> guildRepository.save(new GuildEntity(discordGuildId, sanitizedGuildName)));

        return islandRepository.findFirstByGuildId(guild.getId())
                .orElseGet(() -> {
                    String slug = toSlug(islandName);
                    String joinCode = generateCode(6);
                    return islandRepository.save(new IslandEntity(guild, islandName.trim(), slug, "ACTIVE", joinCode));
                });
    }

    @Transactional
    public String regenerateJoinCode(String discordGuildId) {
        IslandEntity island = getIslandByGuildDiscordId(discordGuildId);
        String newCode = generateCode(6);
        island.setJoinCode(newCode);
        return newCode;
    }

    @Transactional
    public String generateAdminCode(String discordGuildId) {
        IslandEntity island = getIslandByGuildDiscordId(discordGuildId);
        ensureClientIslandMatches(island);
        String code = generateCode(8);
        adminCodeRepository.save(new AdminCodeEntity(island, code, Instant.now().plus(10, ChronoUnit.MINUTES)));
        return code;
    }

    @Transactional
    public void declareFarming(IslandEntity island, String playerName, String playerUuid, String skinTexture, String cropKey, Instant now) {
        FarmingStatusEntity status = farmingStatusRepository.findByIslandIdAndPlayerUuid(island.getId(), playerUuid)
                .orElseGet(() -> new FarmingStatusEntity(island, playerName, playerUuid, skinTexture, cropKey, null));
        status.updateProfile(playerName, skinTexture);
        status.updateCrop(cropKey, status.getLastFarmingAt());
        farmingStatusRepository.save(status);
    }

    @Transactional
    public void reportFarmingActivity(IslandEntity island, String playerUuid, String cropKey, Instant activityAt, String skinTexture) {
        FarmingStatusEntity status = farmingStatusRepository.findByIslandIdAndPlayerUuid(island.getId(), playerUuid)
                .orElse(null);
        if (status == null) {
            return;
        }
        if (!status.getCropKey().equalsIgnoreCase(cropKey)) {
            return;
        }
        status.updateActivity(activityAt, skinTexture);
        farmingStatusRepository.save(status);
    }

    // ── 채널 바인딩 ─────────────────────────────────────────────

    @Transactional
    public void bindLogChannel(Long islandId, String discordChannelId) {
        bindChannel(islandId, discordChannelId, CHEST_LOG_PURPOSE);
    }

    @Transactional
    public void bindBankLogChannel(Long islandId, String discordChannelId) {
        bindChannel(islandId, discordChannelId, BANK_LOG_PURPOSE);
    }

    @Transactional
    public void bindChannel(Long islandId, String discordChannelId, String purpose) {
        IslandEntity island = findIsland(islandId);
        requireText(discordChannelId, "discordChannelId");
        String normalizedPurpose = normalizeChannelPurpose(purpose);
        islandChannelRepository.findAllByIslandIdAndPurpose(islandId, normalizedPurpose)
                .forEach(islandChannelRepository::delete);
        islandChannelRepository.save(new IslandChannelEntity(island, discordChannelId.trim(), normalizedPurpose));
    }

    @Transactional(readOnly = true)
    public String getLogChannelId(Long islandId) {
        return getChannelId(islandId, CHEST_LOG_PURPOSE);
    }

    @Transactional(readOnly = true)
    public String getBankLogChannelId(Long islandId) {
        return getChannelId(islandId, BANK_LOG_PURPOSE);
    }

    @Transactional(readOnly = true)
    public String getChannelId(Long islandId, String purpose) {
        String normalizedPurpose = normalizeChannelPurpose(purpose);
        return islandChannelRepository.findFirstByIslandIdAndPurpose(islandId, normalizedPurpose)
                .map(IslandChannelEntity::getDiscordChannelId)
                .orElse(null);
    }

    public String normalizeChannelPurpose(String purpose) {
        if (purpose == null || purpose.isBlank()) {
            return CHEST_LOG_PURPOSE;
        }

        String normalized = purpose.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        return switch (normalized) {
            case "LOG", "CHEST", "CHEST_LOG", "창고", "창고로그", "창고_로그" -> CHEST_LOG_PURPOSE;
            case "BANK", "BANK_LOG", "ISLAND_BANK", "ISLAND_BANK_LOG", "은행", "은행로그", "은행_로그" -> BANK_LOG_PURPOSE;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 채널 목적입니다: " + purpose);
        };
    }

    // ── joinCode 기반 조회 ──────────────────────────────────────

    @Transactional(readOnly = true)
    public IslandEntity getIslandByJoinCode(String joinCode) {
        String normalizedJoinCode = joinCode == null ? null : joinCode.trim().toUpperCase(Locale.ROOT);
        return islandRepository.findByJoinCode(normalizedJoinCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "잘못된 참여 코드입니다"));
    }

    @Transactional(readOnly = true)
    public IslandEntity getIslandByGuildDiscordId(String discordGuildId) {
        GuildEntity guild = guildRepository.findByDiscordGuildId(discordGuildId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "등록되지 않은 서버입니다. /창고 설정을 먼저 실행하세요."));
        return islandRepository.findFirstByGuildId(guild.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "섬을 찾을 수 없습니다. /창고 설정을 먼저 실행하세요."));
    }

    @Transactional(readOnly = true)
    public IslandEntity getSingleIslandForClient() {
        List<IslandEntity> islands = islandRepository.findAll();
        if (islands.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "연결 가능한 섬이 없습니다. Discord에서 /창고 설정을 먼저 실행하세요.");
        }
        if (islands.size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "현재 백엔드 DB에 섬이 여러 개 있습니다. 서버 주소 기반 모드는 섬 1개만 지원합니다. 사용하지 않는 섬 데이터를 정리하세요.");
        }
        return islands.getFirst();
    }

    @Transactional
    public IslandEntity updateIslandDisplayNameByJoinCode(String discordGuildId, String joinCode, String islandName) {
        requireText(discordGuildId, "discordGuildId");
        requireText(joinCode, "joinCode");
        requireText(islandName, "islandName");
        String trimmedName = islandName.trim();
        if (trimmedName.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "섬 이름은 120자를 초과할 수 없습니다");
        }

        IslandEntity island = getIslandByJoinCode(joinCode);
        if (!island.getGuild().getDiscordGuildId().equals(discordGuildId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 섬을 수정할 권한이 없습니다");
        }

        island.setName(trimmedName);
        return island;
    }

    @Transactional
    public IslandEntity updateIslandDisplayName(String discordGuildId, String islandName) {
        requireText(discordGuildId, "discordGuildId");
        requireText(islandName, "islandName");
        String trimmedName = islandName.trim();
        if (trimmedName.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "섬 이름은 120자를 초과할 수 없습니다");
        }

        IslandEntity island = getIslandByGuildDiscordId(discordGuildId);
        island.setName(trimmedName);
        return island;
    }

    // ── 어드민 코드 검증 ────────────────────────────────────────

    @Transactional
    public AdminCodeEntity validateAdminCode(String joinCode, String adminCode) {
        IslandEntity island = getIslandByJoinCode(joinCode);
        AdminCodeEntity entity = adminCodeRepository.findByCodeAndUsedFalse(adminCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 관리자 코드입니다"));
        if (!entity.getIsland().getId().equals(island.getId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "코드가 해당 섬과 일치하지 않습니다");
        }
        if (entity.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "관리자 코드가 만료되었습니다");
        }
        return entity;
    }

    @Transactional
    public AdminCodeEntity validateAdminCodeForSingleIsland(String adminCode) {
        IslandEntity island = getSingleIslandForClient();
        AdminCodeEntity entity = adminCodeRepository.findByCodeAndUsedFalse(adminCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 관리자 코드입니다"));
        if (!entity.getIsland().getId().equals(island.getId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "코드가 현재 서버의 섬과 일치하지 않습니다");
        }
        if (entity.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "관리자 코드가 만료되었습니다");
        }
        return entity;
    }

    private void ensureClientIslandMatches(IslandEntity island) {
        IslandEntity clientIsland = getSingleIslandForClient();
        if (!clientIsland.getId().equals(island.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "현재 서버가 바라보는 섬과 Discord 서버의 섬이 일치하지 않습니다. 사용하지 않는 섬 데이터를 정리하세요.");
        }
    }

    // ── chest 설정 관리 ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public IslandConfigResponse getActiveConfig(Long islandId) {
        IslandEntity island = findIsland(islandId);
        ChestConfigVersionEntity version = chestConfigVersionRepository.findFirstByIslandIdAndActiveTrue(islandId)
                .orElse(null);

        if (version == null) {
            return new IslandConfigResponse(island.getId(), island.getName(), 0, List.of(), hudMembers(island.getId()));
        }

        List<ChestDefinitionResponse> chests = chestDefinitionRepository.findAllByConfigVersionIdOrderByIdAsc(version.getId())
                .stream()
                .map(this::toChestResponse)
                .toList();

        return new IslandConfigResponse(island.getId(), island.getName(), version.getVersionNumber(), chests, hudMembers(island.getId()));
    }

    @Transactional
    public IslandConfigResponse updateConfig(Long islandId, UpdateChestConfigRequest request) {
        IslandEntity island = findIsland(islandId);
        List<ChestDefinitionRequest> requestedChests = request.chests() == null ? Collections.emptyList() : request.chests();

        for (ChestDefinitionRequest chest : requestedChests) {
            validateChestDefinition(chest);
        }

        chestConfigVersionRepository.findAllByIslandId(islandId)
                .forEach(existing -> existing.setActive(false));

        long nextVersion = chestConfigVersionRepository.findFirstByIslandIdOrderByVersionNumberDesc(islandId)
                .map(version -> version.getVersionNumber() + 1)
                .orElse(1L);

        ChestConfigVersionEntity version = chestConfigVersionRepository.save(
                new ChestConfigVersionEntity(island, nextVersion, blankToDefault(request.createdBy(), "system"), true)
        );

        List<ChestDefinitionEntity> definitions = requestedChests.stream()
                .map(chest -> new ChestDefinitionEntity(
                        version,
                        chest.chestKey(),
                        blankToDefault(chest.displayName(), chest.chestKey()),
                        chest.x(),
                        chest.y(),
                        chest.z(),
                        chest.worldHint(),
                        chest.metadataJson()
                ))
                .toList();
        chestDefinitionRepository.saveAll(definitions);

        return getActiveConfig(islandId);
    }

    // ── REST 관리 API 지원 (어드민 키 기반) ─────────────────────

    @Transactional
    public GuildEntity registerGuild(CreateGuildRequest request) {
        requireText(request.discordGuildId(), "discordGuildId");
        requireText(request.name(), "name");
        String sanitizedGuildName = sanitizeGuildName(request.name(), request.discordGuildId());
        return guildRepository.findByDiscordGuildId(request.discordGuildId())
                .orElseGet(() -> guildRepository.save(new GuildEntity(request.discordGuildId().trim(), sanitizedGuildName)));
    }

    @Transactional(readOnly = true)
    public IslandSummaryResponse getIsland(Long islandId) {
        return toSummary(findIsland(islandId));
    }

    // ── 내부 유틸 ────────────────────────────────────────────────

    private IslandEntity findIsland(Long islandId) {
        return islandRepository.findById(islandId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "island not found"));
    }

    private IslandSummaryResponse toSummary(IslandEntity island) {
        return new IslandSummaryResponse(
                island.getId(),
                island.getGuild().getId(),
                island.getName(),
                island.getSlug(),
                island.getStatus()
        );
    }

    private ChestDefinitionResponse toChestResponse(ChestDefinitionEntity entity) {
        return new ChestDefinitionResponse(
                entity.getChestKey(),
                entity.getDisplayName(),
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                entity.getWorldHint(),
                entity.getMetadataJson()
        );
    }

    private List<MemberHudEntryResponse> hudMembers(Long islandId) {
        LinkedHashMap<String, MemberHudEntryResponse> merged = new LinkedHashMap<>();

        for (ChestLogEntity event : chestLogRepository.findTop100ByIslandIdOrderByCreatedAtDesc(islandId)) {
            Map<String, Integer> taken = parseItemCounts(event.getTakenJson());
            if (taken.isEmpty()) {
                continue;
            }

            String key = memberKey(event.getPlayerUuid(), event.getPlayerName());
            if (merged.containsKey(key)) {
                continue;
            }

            ItemSummary summary = summarizeItems(taken);
            merged.put(key, new MemberHudEntryResponse(
                    event.getPlayerName(),
                    summary.itemName(),
                    summary.itemCount(),
                    taken.size(),
                    summary.totalItems(),
                    event.getTakenVisualData(),
                    event.getChestKey(),
                    event.getPlayerUuid(),
                    event.getSkinTexture(),
                    event.getCreatedAt().toEpochMilli(),
                    null,
                    0L
            ));
        }

        Instant cutoff = Instant.now().minus(FARMING_ACTIVE_WINDOW);
        for (FarmingStatusEntity status : farmingStatusRepository.findAllByIslandId(islandId)) {
            if (status.getLastFarmingAt() == null || status.getLastFarmingAt().isBefore(cutoff)) {
                continue;
            }

            String key = memberKey(status.getPlayerUuid(), status.getPlayerName());
            MemberHudEntryResponse existing = merged.get(key);
            if (existing == null) {
                merged.put(key, new MemberHudEntryResponse(
                        status.getPlayerName(),
                        "",
                        0,
                        0,
                        0,
                        null,
                        null,
                        status.getPlayerUuid(),
                        status.getSkinTexture(),
                        status.getLastFarmingAt().toEpochMilli(),
                        status.getCropKey(),
                        status.getLastFarmingAt().toEpochMilli()
                ));
                continue;
            }

            merged.put(key, new MemberHudEntryResponse(
                    existing.playerName(),
                    existing.itemName(),
                    existing.itemCount(),
                    existing.totalKinds(),
                    existing.totalItems(),
                    existing.itemVisualData(),
                    existing.chestName(),
                    firstNonBlank(existing.playerUuid(), status.getPlayerUuid()),
                    firstNonBlank(existing.skinTexture(), status.getSkinTexture()),
                    existing.updatedAtMillis(),
                    status.getCropKey(),
                    status.getLastFarmingAt().toEpochMilli()
            ));
        }

        return List.copyOf(merged.values());
    }

    private Map<String, Integer> parseItemCounts(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Integer> parsed = OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Integer>>() {});
            return parsed == null ? Collections.emptyMap() : parsed;
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private ItemSummary summarizeItems(Map<String, Integer> items) {
        String topItem = "";
        int topCount = 0;
        int total = 0;
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            int count = Math.max(entry.getValue() == null ? 0 : entry.getValue(), 0);
            total += count;
            if (count > topCount) {
                topItem = entry.getKey();
                topCount = count;
            }
        }
        return new ItemSummary(topItem, topCount, total);
    }

    private String memberKey(String playerUuid, String playerName) {
        if (playerUuid != null && !playerUuid.isBlank()) {
            return playerUuid.trim().toLowerCase(Locale.ROOT);
        }
        return playerName == null ? "" : playerName.trim().toLowerCase(Locale.ROOT);
    }

    private String firstNonBlank(String primary, String fallback) {
        return primary != null && !primary.isBlank() ? primary : fallback;
    }

    private record ItemSummary(String itemName, int itemCount, int totalItems) {
    }

    private String generateCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CODE_CHARS.charAt(rng.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private String toSlug(String value) {
        String slug = value.toLowerCase()
                .replaceAll("[^a-z0-9가-힣]+", "-")
                .replaceAll("^-+|-+$", "")
                .replaceAll("-+", "-");
        return slug.isBlank() ? "island" : slug;
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String sanitizeGuildName(String guildName, String discordGuildId) {
        String sanitized = TextSanitizer.stripEmoji(guildName);
        if (sanitized == null) {
            return "guild-" + discordGuildId.trim();
        }

        String normalized = sanitized.trim().replaceAll("\\s{2,}", " ");
        return normalized.isBlank() ? "guild-" + discordGuildId.trim() : normalized;
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
    }

    private void validateChestDefinition(ChestDefinitionRequest chest) {
        if (chest == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "chest definition is required");
        requireText(chest.chestKey(), "chestKey");
    }
}
