package com._202510007517.platform.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.LinkedHashMap;
import java.util.Map;

@RefreshScope
@ConfigurationProperties(prefix = "gateway.rate-limit")
public class GatewayRateLimitProperties {

    private boolean enabled = true;
    private int replenishRate = 20;
    private int burstCapacity = 40;
    private int requestedTokens = 1;
    private long retryAfterSeconds = 1L;
    private Map<String, RouteLimit> routes = new LinkedHashMap<>();

    public RouteLimit resolve(String routeId) {
        RouteLimit routeLimit = routes.get(routeId);
        if (routeLimit == null) {
            routeLimit = new RouteLimit();
        }
        RouteLimit resolved = new RouteLimit();
        resolved.setReplenishRate(routeLimit.getReplenishRate() != null ? routeLimit.getReplenishRate() : replenishRate);
        resolved.setBurstCapacity(routeLimit.getBurstCapacity() != null ? routeLimit.getBurstCapacity() : burstCapacity);
        resolved.setRequestedTokens(routeLimit.getRequestedTokens() != null ? routeLimit.getRequestedTokens() : requestedTokens);
        resolved.setRetryAfterSeconds(routeLimit.getRetryAfterSeconds() != null ? routeLimit.getRetryAfterSeconds() : retryAfterSeconds);
        return resolved;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getReplenishRate() {
        return replenishRate;
    }

    public void setReplenishRate(int replenishRate) {
        this.replenishRate = replenishRate;
    }

    public int getBurstCapacity() {
        return burstCapacity;
    }

    public void setBurstCapacity(int burstCapacity) {
        this.burstCapacity = burstCapacity;
    }

    public int getRequestedTokens() {
        return requestedTokens;
    }

    public void setRequestedTokens(int requestedTokens) {
        this.requestedTokens = requestedTokens;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public void setRetryAfterSeconds(long retryAfterSeconds) {
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Map<String, RouteLimit> getRoutes() {
        return routes;
    }

    public void setRoutes(Map<String, RouteLimit> routes) {
        this.routes = routes;
    }

    public static class RouteLimit {

        private Integer replenishRate;
        private Integer burstCapacity;
        private Integer requestedTokens;
        private Long retryAfterSeconds;

        public Integer getReplenishRate() {
            return replenishRate;
        }

        public void setReplenishRate(Integer replenishRate) {
            this.replenishRate = replenishRate;
        }

        public Integer getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(Integer burstCapacity) {
            this.burstCapacity = burstCapacity;
        }

        public Integer getRequestedTokens() {
            return requestedTokens;
        }

        public void setRequestedTokens(Integer requestedTokens) {
            this.requestedTokens = requestedTokens;
        }

        public Long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }

        public void setRetryAfterSeconds(long retryAfterSeconds) {
            this.retryAfterSeconds = retryAfterSeconds;
        }
    }
}
