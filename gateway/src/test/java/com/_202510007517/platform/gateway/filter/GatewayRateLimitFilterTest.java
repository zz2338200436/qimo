package com._202510007517.platform.gateway.filter;

import com._202510007517.platform.gateway.config.GatewayRateLimitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRateLimitFilterTest {

    @Test
    void appliesAiRouteSpecificRedisLimiterConfig() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        GatewayRateLimitProperties.RouteLimit aiLimit = new GatewayRateLimitProperties.RouteLimit();
        aiLimit.setReplenishRate(2);
        aiLimit.setBurstCapacity(4);
        aiLimit.setRequestedTokens(1);
        aiLimit.setRetryAfterSeconds(3);
        properties.getRoutes().put("ai-route", aiLimit);
        RedisRateLimiter redisRateLimiter = new RedisRateLimiter(20, 40, 1);
        GatewayRateLimitFilter filter = new GatewayRateLimitFilter(
                redisRateLimiter,
                new IpUserRouteKeyResolver(),
                properties,
                new ObjectMapper());
        filter.applyRouteLimit("ai-route");

        RedisRateLimiter.Config config = redisRateLimiter.getConfig().get("ai-route");
        assertThat(config).isNotNull();
        assertThat(config.getReplenishRate()).isEqualTo(2);
        assertThat(config.getBurstCapacity()).isEqualTo(4);
        assertThat(config.getRequestedTokens()).isEqualTo(1);
    }
}
