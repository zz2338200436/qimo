package com._202510007517.platform.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "gateway.security")
public class GatewaySecurityProperties {

    private List<String> whitelistPaths = List.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/captcha"
    );

    private final Jwt jwt = new Jwt();

    public List<String> getWhitelistPaths() {
        return whitelistPaths;
    }

    public void setWhitelistPaths(List<String> whitelistPaths) {
        this.whitelistPaths = whitelistPaths;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public static class Jwt {
        private long clockSkewSeconds = 30L;
        private String jtiBlacklistPrefix = "AUTH:JTI_BLACKLIST:";
        private Map<String, String> publicKeys = new LinkedHashMap<>();

        public long getClockSkewSeconds() {
            return clockSkewSeconds;
        }

        public void setClockSkewSeconds(long clockSkewSeconds) {
            this.clockSkewSeconds = clockSkewSeconds;
        }

        public String getJtiBlacklistPrefix() {
            return jtiBlacklistPrefix;
        }

        public void setJtiBlacklistPrefix(String jtiBlacklistPrefix) {
            this.jtiBlacklistPrefix = jtiBlacklistPrefix;
        }

        public Map<String, String> getPublicKeys() {
            return publicKeys;
        }

        public void setPublicKeys(Map<String, String> publicKeys) {
            this.publicKeys = publicKeys;
        }
    }
}
