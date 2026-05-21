package com._202510007517.platform.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRateLimitConfig {

    @Bean
    public RedisRateLimiter redisRateLimiter(GatewayRateLimitProperties properties) {
        return new RedisRateLimiter(
                properties.getReplenishRate(),
                properties.getBurstCapacity(),
                properties.getRequestedTokens()
        );
    }
}
