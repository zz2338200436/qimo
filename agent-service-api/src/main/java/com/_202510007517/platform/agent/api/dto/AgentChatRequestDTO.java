package com._202510007517.platform.agent.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.LinkedHashMap;
import java.util.Map;

public class AgentChatRequestDTO {
    private String sessionId;

    @NotBlank
    private String message;

    private Map<String, Object> context = new LinkedHashMap<>();

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getContext() {
        if (context == null) {
            return Map.of();
        }
        return context;
    }

    public void setContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            this.context = new LinkedHashMap<>();
            return;
        }
        this.context = new LinkedHashMap<>(context);
    }
}
