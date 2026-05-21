package com._202510007517.platform.gateway.security;

import java.util.List;

public record GatewayJwtAuthContext(
        String userId,
        List<String> roles,
        String activeRole,
        String jti,
        String subject
) {
}
