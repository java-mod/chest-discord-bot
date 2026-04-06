package com.example.chestbot.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 관리자 대시보드 세션 인증 인터셉터.
 *
 * <p>비밀번호는 BCrypt 해시로만 저장합니다. 평문을 설정에 넣지 마세요.
 *
 * <h3>초기 해시 생성 방법</h3>
 * <pre>
 * 1) 애플리케이션 시작 시 환경변수 설정:
 *      GENERATE_ADMIN_HASH=true
 *      ADMIN_PLAIN_PASSWORD=원하는비밀번호
 *
 * 2) 서버 로그에서 아래 형태의 해시 복사:
 *      [ADMIN-HASH] $2a$12$...
 *
 * 3) local.properties 또는 환경변수에 설정:
 *      app.dashboard.password.hash=$2a$12$...
 *
 * 4) GENERATE_ADMIN_HASH, ADMIN_PLAIN_PASSWORD 환경변수 제거 후 재시작
 * </pre>
 */
@Component
public class DashboardAuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(DashboardAuthInterceptor.class);
    static final String SESSION_KEY = "dashboard_authenticated";

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Value("${app.dashboard.password.hash:}")
    private String passwordHash;

    // ── 초기 해시 생성용 (설정 시 1회만 사용) ─────────────────────
    @Value("${app.dashboard.generate-hash:false}")
    private boolean generateHash;

    @Value("${app.dashboard.plain-password:}")
    private String plainPasswordForHashGen;

    @jakarta.annotation.PostConstruct
    void init() {
        if (generateHash) {
            if (plainPasswordForHashGen.isBlank()) {
                log.warn("[ADMIN-HASH] app.dashboard.plain-password 를 설정하세요 (해시 생성 후 삭제 필수)");
            } else {
                String hash = encoder.encode(plainPasswordForHashGen);
                log.info("[ADMIN-HASH] ============================================================");
                log.info("[ADMIN-HASH] 생성된 BCrypt 해시 (아래 값을 복사하세요):");
                log.info("[ADMIN-HASH] {}", hash);
                log.info("[ADMIN-HASH] app.dashboard.password.hash 에 설정 후 generate-hash=false 로 변경");
                log.info("[ADMIN-HASH] ============================================================");
            }
        }

        if (passwordHash.isBlank()) {
            log.warn("[DASHBOARD] app.dashboard.password.hash 가 설정되지 않았습니다. " +
                     "관리자 로그인이 비활성화됩니다.");
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String uri = request.getRequestURI();
        if (uri.startsWith("/dashboard/login")) {
            return true;
        }
        HttpSession session = request.getSession(false);
        if (session != null && Boolean.TRUE.equals(session.getAttribute(SESSION_KEY))) {
            return true;
        }
        response.sendRedirect("/dashboard/login");
        return false;
    }

    public AuthResult authenticate(String password, HttpServletRequest request) {
        if (passwordHash.isBlank()) {
            return AuthResult.NOT_CONFIGURED;
        }
        if (password == null || password.isBlank()) {
            return AuthResult.WRONG_PASSWORD;
        }
        if (!encoder.matches(password, passwordHash)) {
            return AuthResult.WRONG_PASSWORD;
        }
        request.getSession(true).setAttribute(SESSION_KEY, Boolean.TRUE);
        return AuthResult.SUCCESS;
    }

    public enum AuthResult {
        SUCCESS, WRONG_PASSWORD, NOT_CONFIGURED
    }
}
