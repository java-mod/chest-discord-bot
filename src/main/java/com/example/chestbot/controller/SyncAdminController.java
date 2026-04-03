package com.example.chestbot.controller;

import com.example.chestbot.dto.BindChannelRequest;
import com.example.chestbot.dto.CreateGuildRequest;
import com.example.chestbot.dto.IslandConfigResponse;
import com.example.chestbot.dto.IslandSummaryResponse;
import com.example.chestbot.dto.UpdateChestConfigRequest;
import com.example.chestbot.persistence.entity.GuildEntity;
import com.example.chestbot.service.AdminAuthService;
import com.example.chestbot.service.IslandService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class SyncAdminController {

    private final AdminAuthService adminAuthService;
    private final IslandService islandService;

    public SyncAdminController(AdminAuthService adminAuthService, IslandService islandService) {
        this.adminAuthService = adminAuthService;
        this.islandService = islandService;
    }

    @PostMapping("/guilds/register")
    public Map<String, Object> registerGuild(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @RequestBody CreateGuildRequest request
    ) {
        adminAuthService.requireAdmin(adminKey);
        GuildEntity guild = islandService.registerGuild(request);
        return Map.of(
                "id", guild.getId(),
                "discordGuildId", guild.getDiscordGuildId(),
                "name", guild.getName()
        );
    }

    @GetMapping("/islands/{islandId}")
    public IslandSummaryResponse getIsland(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @PathVariable Long islandId
    ) {
        adminAuthService.requireAdmin(adminKey);
        return islandService.getIsland(islandId);
    }

    @PostMapping("/islands/{islandId}/channels")
    public ResponseEntity<Void> bindChannel(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @PathVariable Long islandId,
            @RequestBody BindChannelRequest request
    ) {
        adminAuthService.requireAdmin(adminKey);
        islandService.bindLogChannel(islandId, request.discordChannelId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/islands/{islandId}/config")
    public IslandConfigResponse getActiveConfig(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @PathVariable Long islandId
    ) {
        adminAuthService.requireAdmin(adminKey);
        return islandService.getActiveConfig(islandId);
    }

    @PostMapping("/islands/{islandId}/config")
    public IslandConfigResponse updateConfig(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @PathVariable Long islandId,
            @RequestBody UpdateChestConfigRequest request
    ) {
        adminAuthService.requireAdmin(adminKey);
        return islandService.updateConfig(islandId, request);
    }
}
