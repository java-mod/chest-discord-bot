package com.example.chestbot.controller;

import com.example.chestbot.dto.LicenseIssueRequest;
import com.example.chestbot.dto.LicenseRow;
import com.example.chestbot.persistence.entity.LicenseEntity;
import com.example.chestbot.service.AdminAuthService;
import com.example.chestbot.service.LicenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 라이선스 관리 REST API (X-Admin-Key 인증 필요)
 */
@RestController
@RequestMapping("/api/v1/admin/licenses")
public class LicenseApiController {

    private final LicenseService licenseService;
    private final AdminAuthService adminAuthService;

    public LicenseApiController(LicenseService licenseService, AdminAuthService adminAuthService) {
        this.licenseService = licenseService;
        this.adminAuthService = adminAuthService;
    }

    /** 라이선스 발급 */
    @PostMapping
    public LicenseRow issue(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @RequestBody LicenseIssueRequest request
    ) {
        adminAuthService.requireAdmin(adminKey);
        LicenseEntity lic = licenseService.issue(request.islandName(), request.ownerNote());
        return LicenseRow.from(lic, null);
    }

    /** 전체 라이선스 목록 */
    @GetMapping
    public List<LicenseRow> list(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey
    ) {
        adminAuthService.requireAdmin(adminKey);
        return licenseService.getLicenseRows();
    }

    /** 라이선스 비활성화 */
    @PostMapping("/{id}/revoke")
    public ResponseEntity<Map<String, String>> revoke(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @PathVariable Long id
    ) {
        adminAuthService.requireAdmin(adminKey);
        licenseService.revoke(id);
        return ResponseEntity.ok(Map.of("status", "revoked"));
    }

    /** 라이선스 재활성화 */
    @PostMapping("/{id}/activate")
    public ResponseEntity<Map<String, String>> activate(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @PathVariable Long id
    ) {
        adminAuthService.requireAdmin(adminKey);
        licenseService.activate(id);
        return ResponseEntity.ok(Map.of("status", "activated"));
    }

    /** 섬에 라이선스 할당 */
    @PostMapping("/{id}/assign")
    public ResponseEntity<Map<String, String>> assign(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @PathVariable Long id,
            @RequestBody Map<String, Long> body
    ) {
        adminAuthService.requireAdmin(adminKey);
        Long islandId = body.get("islandId");
        if (islandId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "islandId is required"));
        }
        licenseService.assignToIsland(id, islandId);
        return ResponseEntity.ok(Map.of("status", "assigned"));
    }

    /** 섬에서 라이선스 해제 */
    @PostMapping("/islands/{islandId}/remove")
    public ResponseEntity<Map<String, String>> remove(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @PathVariable Long islandId
    ) {
        adminAuthService.requireAdmin(adminKey);
        licenseService.removeFromIsland(islandId);
        return ResponseEntity.ok(Map.of("status", "removed"));
    }
}
