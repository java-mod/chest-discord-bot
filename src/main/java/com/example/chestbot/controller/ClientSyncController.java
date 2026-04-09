package com.example.chestbot.controller;

import com.example.chestbot.dto.AdminConnectRequest;
import com.example.chestbot.dto.AdminConnectResponse;
import com.example.chestbot.dto.AdminFinalizeRequest;
import com.example.chestbot.dto.ClientChestLogRequest;
import com.example.chestbot.dto.ClientIslandBankLogRequest;
import com.example.chestbot.dto.ConnectRequest;
import com.example.chestbot.dto.IslandConfigResponse;
import com.example.chestbot.dto.LicenseConnectRequest;
import com.example.chestbot.dto.LicenseConnectResponse;
import com.example.chestbot.service.ClientSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/client")
public class ClientSyncController {

    private final ClientSyncService clientSyncService;

    public ClientSyncController(ClientSyncService clientSyncService) {
        this.clientSyncService = clientSyncService;
    }

    @PostMapping("/connect")
    public IslandConfigResponse connect() {
        return clientSyncService.connect();
    }

    @PostMapping("/connect/license")
    public LicenseConnectResponse connectWithLicense(@RequestBody LicenseConnectRequest request) {
        return clientSyncService.connectWithLicense(request.licenseKey());
    }

    @PostMapping("/admin/connect")
    public AdminConnectResponse adminConnect(@RequestBody AdminConnectRequest request) {
        return clientSyncService.adminConnect(request.adminCode());
    }

    @PostMapping("/admin/finalize")
    public IslandConfigResponse adminFinalize(@RequestBody AdminFinalizeRequest request) {
        return clientSyncService.adminFinalize(request);
    }

    @PostMapping("/events/chest-log")
    public ResponseEntity<Map<String, String>> logChestEvent(@RequestBody ClientChestLogRequest request) {
        clientSyncService.logChestEvent(request);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/events/island-bank-log")
    public ResponseEntity<Map<String, String>> logIslandBankEvent(@RequestBody ClientIslandBankLogRequest request) {
        clientSyncService.logIslandBankEvent(request);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
