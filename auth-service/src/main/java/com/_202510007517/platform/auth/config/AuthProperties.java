package com._202510007517.platform.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@RefreshScope
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private String activeKid = "dev";
    private Duration accessTokenTtl = Duration.ofMinutes(30);
    private Duration refreshTokenTtl = Duration.ofDays(7);
    private Duration keyRotationGrace = Duration.ofHours(48);
    private String captchaPrefix = "CAPTCHA:IMG:";
    private String jtiBlacklistPrefix = "AUTH:JTI_BLACKLIST:";
    private String refreshTokenPrefix = "AUTH:REFRESH:";
    private Map<String, KeyPairProperties> keys = new LinkedHashMap<>();

    public String getActiveKid() {
        return activeKid;
    }

    public void setActiveKid(String activeKid) {
        this.activeKid = activeKid;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public Duration getKeyRotationGrace() {
        return keyRotationGrace;
    }

    public void setKeyRotationGrace(Duration keyRotationGrace) {
        this.keyRotationGrace = keyRotationGrace;
    }

    public String getCaptchaPrefix() {
        return captchaPrefix;
    }

    public void setCaptchaPrefix(String captchaPrefix) {
        this.captchaPrefix = captchaPrefix;
    }

    public String getJtiBlacklistPrefix() {
        return jtiBlacklistPrefix;
    }

    public void setJtiBlacklistPrefix(String jtiBlacklistPrefix) {
        this.jtiBlacklistPrefix = jtiBlacklistPrefix;
    }

    public String getRefreshTokenPrefix() {
        return refreshTokenPrefix;
    }

    public void setRefreshTokenPrefix(String refreshTokenPrefix) {
        this.refreshTokenPrefix = refreshTokenPrefix;
    }

    public Map<String, KeyPairProperties> getKeys() {
        return keys;
    }

    public void setKeys(Map<String, KeyPairProperties> keys) {
        this.keys = keys;
    }

    public static class KeyPairProperties {
        private String publicKey;
        private String privateKey;

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }
    }
}
