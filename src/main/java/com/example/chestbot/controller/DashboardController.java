package com.example.chestbot.controller;

import com.example.chestbot.config.DashboardAuthInterceptor;
import com.example.chestbot.dto.IslandRow;
import com.example.chestbot.dto.LicenseRow;
import com.example.chestbot.persistence.entity.LicenseEntity;
import com.example.chestbot.service.LicenseService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final LicenseService licenseService;
    private final DashboardAuthInterceptor dashboardAuth;

    public DashboardController(LicenseService licenseService,
                                DashboardAuthInterceptor dashboardAuth) {
        this.licenseService = licenseService;
        this.dashboardAuth = dashboardAuth;
    }

    // ── 인증 ──────────────────────────────────────────────────────

    @GetMapping
    public String index() {
        return "redirect:/dashboard/licenses";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "dashboard/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String password,
                        HttpServletRequest request,
                        RedirectAttributes ra) {
        DashboardAuthInterceptor.AuthResult result = dashboardAuth.authenticate(password, request);
        if (result == DashboardAuthInterceptor.AuthResult.SUCCESS) {
            return "redirect:/dashboard/licenses";
        }
        if (result == DashboardAuthInterceptor.AuthResult.NOT_CONFIGURED) {
            ra.addFlashAttribute("error", "관리자 비밀번호가 서버에 설정되지 않았습니다. 운영자에게 문의하세요.");
        } else {
            ra.addFlashAttribute("error", "비밀번호가 틀렸습니다.");
        }
        return "redirect:/dashboard/login";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        request.getSession().invalidate();
        return "redirect:/dashboard/login";
    }

    // ── 라이선스 페이지 ───────────────────────────────────────────

    @GetMapping("/licenses")
    public String licenses(Model model) {
        List<LicenseRow> rows = licenseService.getLicenseRows();
        long active = rows.stream().filter(LicenseRow::isActive).count();
        long assigned = rows.stream().filter(r -> r.getAssignedIslandId() != null).count();

        model.addAttribute("licenses", rows);
        model.addAttribute("totalLicenses", rows.size());
        model.addAttribute("activeLicenses", active);
        model.addAttribute("assignedLicenses", assigned);
        return "dashboard/licenses";
    }

    @PostMapping("/licenses/issue")
    public String issueLicense(@RequestParam String islandName,
                               @RequestParam(required = false) String ownerNote,
                               RedirectAttributes ra) {
        LicenseEntity issued = licenseService.issue(islandName, ownerNote);
        ra.addFlashAttribute("issuedKey", issued.getLicenseKey());
        ra.addFlashAttribute("issuedIsland", issued.getIslandName());
        return "redirect:/dashboard/licenses";
    }

    @PostMapping("/licenses/{id}/delete")
    public String deleteLicense(@PathVariable Long id) {
        licenseService.deleteLicense(id);
        return "redirect:/dashboard/licenses";
    }

    @PostMapping("/licenses/{id}/revoke")
    public String revoke(@PathVariable Long id) {
        licenseService.revoke(id);
        return "redirect:/dashboard/licenses";
    }

    @PostMapping("/licenses/{id}/activate")
    public String activate(@PathVariable Long id) {
        licenseService.activate(id);
        return "redirect:/dashboard/licenses";
    }

    // ── 섬 관리 페이지 ────────────────────────────────────────────

    @GetMapping("/islands")
    public String islands(Model model) {
        List<IslandRow> islandRows = licenseService.getIslandRows();
        List<LicenseRow> availableLicenses = licenseService.getAvailableLicenses();

        model.addAttribute("islands", islandRows);
        model.addAttribute("availableLicenses", availableLicenses);
        return "dashboard/islands";
    }

    @PostMapping("/islands/{islandId}/assign-license")
    public String assignLicense(@PathVariable Long islandId,
                                @RequestParam Long licenseId,
                                RedirectAttributes ra) {
        licenseService.assignToIsland(licenseId, islandId);
        ra.addFlashAttribute("successMsg", "라이선스가 할당되었습니다.");
        return "redirect:/dashboard/islands";
    }

    @PostMapping("/islands/{islandId}/remove-license")
    public String removeLicense(@PathVariable Long islandId,
                                RedirectAttributes ra) {
        licenseService.removeFromIsland(islandId);
        ra.addFlashAttribute("successMsg", "라이선스 할당이 해제되었습니다.");
        return "redirect:/dashboard/islands";
    }
}
