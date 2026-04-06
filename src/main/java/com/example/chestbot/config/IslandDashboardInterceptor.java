package com.example.chestbot.config;

import com.example.chestbot.persistence.entity.IslandEntity;
import com.example.chestbot.persistence.entity.LicenseEntity;
import com.example.chestbot.persistence.repository.IslandRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;

/**
 * 섬 대시보드 세션 인증 인터셉터.
 *
 * <p>매 요청마다 세션의 island_id로 라이선스 활성 여부를 DB에서 재검증합니다.
 * 라이선스가 비활성화되면 즉시 세션을 파기하고 로그인 페이지로 이동시킵니다.
 *
 * <p>검증 통과 시, 이후 컨트롤러에서 사용할 수 있도록 {@link #ISLAND_ATTR} 키로
 * {@link IslandEntity}를 request attribute에 설정합니다.
 */
@Component
public class IslandDashboardInterceptor implements HandlerInterceptor {

    public static final String ISLAND_SESSION_KEY = "island_dashboard_island_id";
    public static final String ISLAND_ATTR = "currentIsland";

    private final IslandRepository islandRepository;

    public IslandDashboardInterceptor(IslandRepository islandRepository) {
        this.islandRepository = islandRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String uri = request.getRequestURI();
        if (uri.startsWith("/dashboard/island/login")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect("/dashboard/island/login");
            return false;
        }

        Long islandId = (Long) session.getAttribute(ISLAND_SESSION_KEY);
        if (islandId == null) {
            response.sendRedirect("/dashboard/island/login");
            return false;
        }

        // 매 요청마다 라이선스 상태 재검증 (캐시 없이 DB 직접 확인)
        IslandEntity island = islandRepository.findByIdWithLicense(islandId).orElse(null);
        if (island == null || !isLicenseValid(island)) {
            session.invalidate();
            response.sendRedirect("/dashboard/island/login?expired=1");
            return false;
        }

        request.setAttribute(ISLAND_ATTR, island);
        return true;
    }

    private boolean isLicenseValid(IslandEntity island) {
        LicenseEntity license = island.getLicense();
        if (license == null || !license.isActive()) return false;
        if (license.getExpiresAt() != null && license.getExpiresAt().isBefore(Instant.now())) return false;
        return true;
    }
}
