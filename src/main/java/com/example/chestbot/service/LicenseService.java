package com.example.chestbot.service;

import com.example.chestbot.dto.IslandRow;
import com.example.chestbot.dto.LicenseRow;
import com.example.chestbot.persistence.entity.IslandEntity;
import com.example.chestbot.persistence.entity.LicenseEntity;
import com.example.chestbot.persistence.repository.IslandRepository;
import com.example.chestbot.persistence.repository.LicenseRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LicenseService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom rng = new SecureRandom();

    private final LicenseRepository licenseRepository;
    private final IslandRepository islandRepository;

    @Value("${app.license.enforcement:false}")
    private boolean enforcementEnabled;

    public LicenseService(LicenseRepository licenseRepository, IslandRepository islandRepository) {
        this.licenseRepository = licenseRepository;
        this.islandRepository = islandRepository;
    }

    // ── 라이선스 발급 ─────────────────────────────────────────────

    @Transactional
    public LicenseEntity issue(String islandName, String ownerNote) {
        if (islandName == null || islandName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "islandName is required");
        }
        String key = generateKey();
        String note = (ownerNote == null || ownerNote.isBlank()) ? null : ownerNote.trim();
        return licenseRepository.save(new LicenseEntity(key, islandName.trim(), note));
    }

    // ── 활성화 / 비활성화 ─────────────────────────────────────────

    @Transactional
    public void revoke(Long licenseId) {
        findById(licenseId).setActive(false);
    }

    @Transactional
    public void activate(Long licenseId) {
        findById(licenseId).setActive(true);
    }

    // ── 섬에 라이선스 할당 / 해제 ──────────────────────────────────

    @Transactional
    public void assignToIsland(Long licenseId, Long islandId) {
        LicenseEntity license = findById(licenseId);
        IslandEntity island = islandRepository.findById(islandId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "island not found"));
        island.setLicense(license);
    }

    @Transactional
    public void deleteLicense(Long id) {
        LicenseEntity license = findById(id);
        // 할당된 섬이 있으면 먼저 해제
        islandRepository.findByLicense(license)
                .ifPresent(island -> island.setLicense(null));
        licenseRepository.delete(license);
    }

    @Transactional
    public void removeFromIsland(Long islandId) {
        IslandEntity island = islandRepository.findById(islandId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "island not found"));
        island.setLicense(null);
    }

    // ── connect 시 라이선스 유효성 검증 ───────────────────────────

    /**
     * 라이선스 강제 적용이 활성화된 경우, 섬에 유효한 라이선스가 없으면 FORBIDDEN을 던진다.
     * 이 메서드를 호출하는 쪽은 반드시 @Transactional 컨텍스트 안에 있어야 한다 (lazy load).
     */
    public void requireActiveLicense(IslandEntity island) {
        if (!enforcementEnabled) return;

        LicenseEntity license = island.getLicense();
        if (license == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "라이선스가 할당되지 않은 섬입니다");
        }
        if (!license.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "비활성화된 라이선스입니다");
        }
        if (license.getExpiresAt() != null && license.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "만료된 라이선스입니다");
        }
        license.setLastUsedAt(Instant.now());
    }

    // ── 섬 대시보드 로그인 ─────────────────────────────────────────

    /**
     * 라이선스 키로 섬을 찾는다. 섬 대시보드 로그인 시 사용.
     * 키가 없거나 비활성화/만료/미할당인 경우 예외를 던진다.
     */
    @Transactional(readOnly = true)
    public IslandEntity findIslandByLicenseKey(String licenseKey) {
        if (licenseKey == null || licenseKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "라이선스 키를 입력하세요");
        }
        LicenseEntity license = licenseRepository.findByLicenseKey(licenseKey.trim().toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "등록되지 않은 라이선스 키입니다"));

        if (!license.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "비활성화된 라이선스입니다");
        }
        if (license.getExpiresAt() != null && license.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "만료된 라이선스입니다");
        }

        return islandRepository.findByLicense(license)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "아직 섬에 할당되지 않은 라이선스입니다. 운영자에게 문의하세요"));
    }

    // ── 대시보드용 데이터 조회 ─────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LicenseRow> getLicenseRows() {
        List<IslandEntity> islands = islandRepository.findAllWithLicense();
        Map<Long, IslandEntity> licenseToIsland = islands.stream()
                .filter(i -> i.getLicense() != null)
                .collect(Collectors.toMap(i -> i.getLicense().getId(), i -> i));

        return licenseRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(lic -> LicenseRow.from(lic, licenseToIsland.get(lic.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IslandRow> getIslandRows() {
        return islandRepository.findAllWithLicense().stream()
                .map(IslandRow::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LicenseRow> getAvailableLicenses() {
        List<IslandEntity> islands = islandRepository.findAllWithLicense();
        Set<Long> assignedIds = islands.stream()
                .filter(i -> i.getLicense() != null)
                .map(i -> i.getLicense().getId())
                .collect(Collectors.toSet());

        return licenseRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(lic -> lic.isActive() && !assignedIds.contains(lic.getId()))
                .map(lic -> LicenseRow.from(lic, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public LicenseEntity findById(Long id) {
        return licenseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "license not found"));
    }

    // ── 내부 유틸 ─────────────────────────────────────────────────

    private String generateKey() {
        StringBuilder sb = new StringBuilder("LIC-");
        for (int group = 0; group < 3; group++) {
            if (group > 0) sb.append('-');
            for (int i = 0; i < 4; i++) {
                sb.append(CODE_CHARS.charAt(rng.nextInt(CODE_CHARS.length())));
            }
        }
        return sb.toString(); // LIC-XXXX-XXXX-XXXX (18자)
    }
}
