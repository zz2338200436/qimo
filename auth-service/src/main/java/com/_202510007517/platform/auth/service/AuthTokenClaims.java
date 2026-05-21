package com._202510007517.platform.auth.service;

import java.time.Instant;
import java.util.List;

public record AuthTokenClaims(
        Long userId,
        String subject,
        List<String> roles,
        String activeRole,
        String jti,
        String tokenType,
        Instant expiresAt
) {
}
