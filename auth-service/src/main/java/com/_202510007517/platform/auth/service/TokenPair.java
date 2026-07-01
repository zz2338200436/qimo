package com._202510007517.platform.auth.service;

import java.time.Instant;

public record TokenPair(
        String accessToken,
        String refreshToken,
        String tokenType,
        Instant expiresAt
) {
}
