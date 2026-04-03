package com.example.chestbot.service;

import com.example.chestbot.dto.ChestDefinitionRequest;
import com.example.chestbot.dto.ChestDefinitionResponse;
import com.example.chestbot.dto.CreateGuildRequest;
import com.example.chestbot.dto.IslandConfigResponse;
import com.example.chestbot.dto.IslandSummaryResponse;
import com.example.chestbot.dto.UpdateChestConfigRequest;
import com.example.chestbot.persistence.entity.AdminCodeEntity;
import com.example.chestbot.persistence.entity.ChestConfigVersionEntity;
import com.example.chestbot.persistence.entity.ChestDefinitionEntity;
import com.example.chestbot.persistence.entity.GuildEntity;
import com.example.chestbot.persistence.entity.IslandChannelEntity;
import com.example.chestbot.persistence.entity.IslandEntity;
import com.example.chestbot.persistence.repository.AdminCodeRepository;
import com.example.chestbot.persistence.repository.ChestConfigVersionRepository;
import com.example.chestbot.persistence.repository.ChestDefinitionRepository;
import com.example.chestbot.persistence.repository.GuildRepository;
import com.example.chestbot.persistence.repository.IslandChannelRepository;
import com.example.chestbot.persistence.repository.IslandRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
public class IslandService {

    private static final String LOG_PURPOSE = "LOG";
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom rng = new SecureRandom();

    private final GuildRepository guildRepository;
    private final IslandRepository islandRepository;
    private final IslandChannelRepository islandChannelRepository;
    private final ChestConfigVersionRepository chestConfigVersionRepository;
    private final ChestDefinitionRepository chestDefinitionRepository;
    private final AdminCodeRepository adminCodeRepository;

    public IslandService(
            GuildRepository guildRepository,
            IslandRepository islandRepository,
            IslandChannelRepository islandChannelRepository,
            ChestConfigVersionRepository chestConfigVersionRepository,
            ChestDefinitionRepository chestDefinitionRepository,
            AdminCodeRepository adminCodeRepository
    ) {
        this.guildRepository = guildRepository;
        this.islandRepository = islandRepository;
        this.islandChannelRepository = islandChannelRepository;
        this.chestConfigVersionRepository = chestConfigVersionRepository;
        this.chestDefinitionRepository = chestDefinitionRepository;
        this.adminCodeRepository = adminCodeRepository;
    }

    // ── 슬래시 커맨드용 통합 셋업 ──────────────────────────────────

    @Transactional
    public IslandEntity setupIsland(String discordGuildId, String guildName, String islandName) {
        requireText(discordGuildId, "discordGuildId");
        requireText(islandName, "islandName");
        GuildEntity guild = guildRepository.findByDiscordGuildId(discordGuildId)
                .orElseGet(() -> guildRepository.save(new GuildEntity(discordGuildId, guildName)));

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
        String code = generateCode(8);
        adminCodeRepository.save(new AdminCodeEntity(island, code, Instant.now().plus(10, ChronoUnit.MINUTES)));
        return code;
    }

    // ── 채널 바인딩 ─────────────────────────────────────────────

    @Transactional
    public void bindLogChannel(Long islandId, String discordChannelId) {
        IslandEntity island = findIsland(islandId);
        requireText(discordChannelId, "discordChannelId");
        islandChannelRepository.findFirstByIslandIdAndPurpose(islandId, LOG_PURPOSE)
                .ifPresent(islandChannelRepository::delete);
        islandChannelRepository.save(new IslandChannelEntity(island, discordChannelId.trim(), LOG_PURPOSE));
    }

    @Transactional(readOnly = true)
    public String getLogChannelId(Long islandId) {
        return islandChannelRepository.findFirstByIslandIdAndPurpose(islandId, LOG_PURPOSE)
                .map(IslandChannelEntity::getDiscordChannelId)
                .orElse(null);
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

    // ── chest 설정 관리 ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public IslandConfigResponse getActiveConfig(Long islandId) {
        IslandEntity island = findIsland(islandId);
        ChestConfigVersionEntity version = chestConfigVersionRepository.findFirstByIslandIdAndActiveTrue(islandId)
                .orElse(null);

        if (version == null) {
            return new IslandConfigResponse(island.getId(), island.getName(), 0, List.of());
        }

        List<ChestDefinitionResponse> chests = chestDefinitionRepository.findAllByConfigVersionIdOrderByIdAsc(version.getId())
                .stream()
                .map(this::toChestResponse)
                .toList();

        return new IslandConfigResponse(island.getId(), island.getName(), version.getVersionNumber(), chests);
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
        return guildRepository.findByDiscordGuildId(request.discordGuildId())
                .orElseGet(() -> guildRepository.save(new GuildEntity(request.discordGuildId().trim(), request.name().trim())));
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
