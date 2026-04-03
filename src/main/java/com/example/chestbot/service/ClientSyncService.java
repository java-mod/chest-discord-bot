package com.example.chestbot.service;

import com.example.chestbot.dto.AdminConnectResponse;
import com.example.chestbot.dto.AdminFinalizeRequest;
import com.example.chestbot.dto.ClientChestLogRequest;
import com.example.chestbot.dto.IslandConfigResponse;
import com.example.chestbot.dto.UpdateChestConfigRequest;
import com.example.chestbot.persistence.entity.AdminCodeEntity;
import com.example.chestbot.persistence.entity.IslandEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

@Service
public class ClientSyncService {

    private static final Logger log = LoggerFactory.getLogger(ClientSyncService.class);

    private final IslandService islandService;
    private final ChestLogFileStore chestLogFileStore;
    private final DiscordService discordService;

    public ClientSyncService(
            IslandService islandService,
            ChestLogFileStore chestLogFileStore,
            DiscordService discordService
    ) {
        this.islandService = islandService;
        this.chestLogFileStore = chestLogFileStore;
        this.discordService = discordService;
    }

    @Transactional
    public IslandConfigResponse connect(String joinCode) {
        IslandEntity island = islandService.getIslandByJoinCode(joinCode);
        return islandService.getActiveConfig(island.getId());
    }

    @Transactional
    public AdminConnectResponse adminConnect(String joinCode, String adminCode) {
        AdminCodeEntity code = islandService.validateAdminCode(joinCode, adminCode);
        IslandEntity island = code.getIsland();
        return new AdminConnectResponse(island.getId(), island.getName());
    }

    @Transactional
    public IslandConfigResponse adminFinalize(AdminFinalizeRequest request) {
        AdminCodeEntity code = islandService.validateAdminCode(request.joinCode(), request.adminCode());
        code.markUsed();
        IslandEntity island = code.getIsland();

        UpdateChestConfigRequest configRequest = new UpdateChestConfigRequest("admin", request.chests());
        return islandService.updateConfig(island.getId(), configRequest);
    }

    @Transactional
    public void logChestEvent(ClientChestLogRequest request) {
        IslandEntity island = islandService.getIslandByJoinCode(request.joinCode());
        Map<String, Integer> taken = request.taken() == null ? Collections.emptyMap() : request.taken();
        Map<String, Integer> added = request.added() == null ? Collections.emptyMap() : request.added();

        chestLogFileStore.append(
                island.getName(),
                request.joinCode(),
                request.configVersion(),
                request.playerName(),
                request.chestKey(),
                taken,
                added
        );

        log.info("상자 로그 파일 저장: path={}, island={}, chest={}, player={}, takenItems={}, addedItems={}, configVersion={}",
                chestLogFileStore.getLogPath(), island.getName(), request.chestKey(), request.playerName(), taken.size(), added.size(), request.configVersion());

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
                Instant.now()
        );
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
