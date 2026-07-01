package com._202510007517.platform.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "agent.internet")
public class AgentInternetProperties {
    private boolean enabled = true;
    private String searchBaseUrl = "https://cn.bing.com/search";
    private int timeoutSeconds = 5;
    private int maxResults = 5;
    private int maxPageBytes = 200_000;
    private int maxContentChars = 4_000;
    private boolean blockPrivateNetwork = true;
    private String userAgent = "majorassignment-agent/1.0";
    private List<String> allowedDomains = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSearchBaseUrl() {
        return searchBaseUrl;
    }

    public void setSearchBaseUrl(String searchBaseUrl) {
        if (searchBaseUrl != null && !searchBaseUrl.isBlank()) {
            this.searchBaseUrl = searchBaseUrl;
        }
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = Math.max(1, timeoutSeconds);
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = Math.max(1, maxResults);
    }

    public int getMaxPageBytes() {
        return maxPageBytes;
    }

    public void setMaxPageBytes(int maxPageBytes) {
        this.maxPageBytes = Math.max(1_024, maxPageBytes);
    }

    public int getMaxContentChars() {
        return maxContentChars;
    }

    public void setMaxContentChars(int maxContentChars) {
        this.maxContentChars = Math.max(1, maxContentChars);
    }

    public boolean isBlockPrivateNetwork() {
        return blockPrivateNetwork;
    }

    public void setBlockPrivateNetwork(boolean blockPrivateNetwork) {
        this.blockPrivateNetwork = blockPrivateNetwork;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        if (userAgent != null && !userAgent.isBlank()) {
            this.userAgent = userAgent;
        }
    }

    public List<String> getAllowedDomains() {
        return allowedDomains;
    }

    public void setAllowedDomains(List<String> allowedDomains) {
        if (allowedDomains == null) {
            this.allowedDomains = new ArrayList<>();
            return;
        }
        this.allowedDomains = allowedDomains.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(java.util.Locale.ROOT))
                .toList();
    }
}
