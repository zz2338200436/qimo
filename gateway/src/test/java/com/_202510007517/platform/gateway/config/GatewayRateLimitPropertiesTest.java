package com._202510007517.platform.gateway.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRateLimitPropertiesTest {

    @Test
    void resolvesRouteSpecificLimitBeforeGlobalDefault() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setReplenishRate(20);
        properties.setBurstCapacity(40);
        properties.setRequestedTokens(1);
        GatewayRateLimitProperties.RouteLimit aiLimit = new GatewayRateLimitProperties.RouteLimit();
        aiLimit.setReplenishRate(2);
        aiLimit.setBurstCapacity(4);
        aiLimit.setRequestedTokens(1);
        aiLimit.setRetryAfterSeconds(3);
        properties.getRoutes().put("ai-route", aiLimit);

        GatewayRateLimitProperties.RouteLimit resolved = properties.resolve("ai-route");
        GatewayRateLimitProperties.RouteLimit fallback = properties.resolve("notification-route");

        assertThat(resolved.getReplenishRate()).isEqualTo(2);
        assertThat(resolved.getBurstCapacity()).isEqualTo(4);
        assertThat(resolved.getRequestedTokens()).isEqualTo(1);
        assertThat(resolved.getRetryAfterSeconds()).isEqualTo(3);
        assertThat(fallback.getReplenishRate()).isEqualTo(20);
        assertThat(fallback.getBurstCapacity()).isEqualTo(40);
        assertThat(fallback.getRequestedTokens()).isEqualTo(1);
    }
}
