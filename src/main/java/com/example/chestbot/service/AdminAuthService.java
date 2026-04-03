package com.example.chestbot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminAuthService {

    private final String adminKey;

    public AdminAuthService(@Value("${app.admin-key:}") String adminKey) {
        this.adminKey = adminKey;
    }

    public void requireAdmin(String providedKey) {
        if (adminKey == null || adminKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "admin key not configured");
        }

        if (providedKey == null || providedKey.isBlank() || !adminKey.equals(providedKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "admin authorization failed");
        }
    }
}
