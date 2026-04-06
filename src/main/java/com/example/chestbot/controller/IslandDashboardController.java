package com.example.chestbot.controller;

import com.example.chestbot.config.IslandDashboardInterceptor;
import com.example.chestbot.dto.BankLogRow;
import com.example.chestbot.dto.ChestLogRow;
import com.example.chestbot.dto.IslandConfigResponse;
import com.example.chestbot.persistence.entity.IslandEntity;
import com.example.chestbot.service.IslandDashboardService;
import com.example.chestbot.service.IslandService;
import com.example.chestbot.service.LicenseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/dashboard/island")
public class IslandDashboardController {

    private static final DateTimeFormatter FILE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final LicenseService licenseService;
    private final IslandDashboardService islandDashboardService;
    private final IslandService islandService;

    public IslandDashboardController(LicenseService licenseService,
                                      IslandDashboardService islandDashboardService,
                                      IslandService islandService) {
        this.licenseService = licenseService;
        this.islandDashboardService = islandDashboardService;
        this.islandService = islandService;
    }

    // ── 인증 ──────────────────────────────────────────────────────

    @GetMapping("/login")
    public String loginPage() {
        return "dashboard/island/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String licenseKey,
                        HttpServletRequest request,
                        RedirectAttributes ra) {
        try {
            IslandEntity island = licenseService.findIslandByLicenseKey(licenseKey);
            HttpSession session = request.getSession(true);
            session.setAttribute(IslandDashboardInterceptor.ISLAND_SESSION_KEY, island.getId());
            return "redirect:/dashboard/island/logs";
        } catch (ResponseStatusException e) {
            ra.addFlashAttribute("error", e.getReason());
            return "redirect:/dashboard/island/login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(IslandDashboardInterceptor.ISLAND_SESSION_KEY);
        }
        return "redirect:/dashboard/island/login?logout=1";
    }

    // ── 메인 ──────────────────────────────────────────────────────

    @GetMapping
    public String index() {
        return "redirect:/dashboard/island/logs";
    }

    // ── 창고 로그 ─────────────────────────────────────────────────

    @GetMapping("/logs")
    public String logs(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) String playerName,
                       @RequestParam(required = false) String chestKey,
                       HttpServletRequest request,
                       Model model) {
        IslandEntity island = currentIsland(request);
        Page<ChestLogRow> logs = islandDashboardService.getChestLogs(
                island.getId(), page, playerName, chestKey);

        model.addAttribute("island", island);
        model.addAttribute("logs", logs);
        model.addAttribute("currentPage", page);
        model.addAttribute("playerName", playerName != null ? playerName : "");
        model.addAttribute("chestKey", chestKey != null ? chestKey : "");
        return "dashboard/island/logs";
    }

    @GetMapping("/logs/export")
    public ResponseEntity<byte[]> logsExport(
            @RequestParam(required = false) String playerName,
            @RequestParam(required = false) String chestKey,
            HttpServletRequest request) {
        IslandEntity island = currentIsland(request);
        String content = islandDashboardService.buildChestLogExport(
                island.getId(), playerName, chestKey);
        String filename = "chest_log_" + island.getName() + "_"
                + LocalDateTime.now().format(FILE_FMT) + ".txt";
        return textFileResponse(content, filename);
    }

    // ── 은행 로그 ─────────────────────────────────────────────────

    @GetMapping("/bank")
    public String bank(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) String playerName,
                       @RequestParam(required = false) String transactionType,
                       HttpServletRequest request,
                       Model model) {
        IslandEntity island = currentIsland(request);
        Page<BankLogRow> logs = islandDashboardService.getBankLogs(
                island.getId(), page, playerName, transactionType);

        model.addAttribute("island", island);
        model.addAttribute("logs", logs);
        model.addAttribute("currentPage", page);
        model.addAttribute("playerName", playerName != null ? playerName : "");
        model.addAttribute("transactionType", transactionType != null ? transactionType : "");
        return "dashboard/island/bank";
    }

    @GetMapping("/bank/export")
    public ResponseEntity<byte[]> bankExport(
            @RequestParam(required = false) String playerName,
            @RequestParam(required = false) String transactionType,
            HttpServletRequest request) {
        IslandEntity island = currentIsland(request);
        String content = islandDashboardService.buildBankLogExport(
                island.getId(), playerName, transactionType);
        String filename = "bank_log_" + island.getName() + "_"
                + LocalDateTime.now().format(FILE_FMT) + ".txt";
        return textFileResponse(content, filename);
    }

    // ── 창고 설정 ─────────────────────────────────────────────────

    @GetMapping("/config")
    public String config(HttpServletRequest request, Model model) {
        IslandEntity island = currentIsland(request);
        IslandConfigResponse config = islandService.getActiveConfig(island.getId());

        model.addAttribute("island", island);
        model.addAttribute("config", config);
        return "dashboard/island/config";
    }

    // ── 내부 유틸 ─────────────────────────────────────────────────

    private IslandEntity currentIsland(HttpServletRequest request) {
        IslandEntity island = (IslandEntity) request.getAttribute(IslandDashboardInterceptor.ISLAND_ATTR);
        if (island == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 정보가 없습니다");
        }
        return island;
    }

    private static ResponseEntity<byte[]> textFileResponse(String content, String filename) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/plain; charset=UTF-8"));
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}
