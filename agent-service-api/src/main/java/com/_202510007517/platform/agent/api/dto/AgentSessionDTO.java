package com._202510007517.platform.agent.api.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AgentSessionDTO {
    private String sessionId;
    private String userRole;
    private String status;
    private String pendingIntent;
    private Map<String, Object> pendingSlots;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AgentMessageDTO> messages = new ArrayList<>();
    private List<AgentActionDTO> actions = new ArrayList<>();

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPendingIntent() {
        return pendingIntent;
    }

    public void setPendingIntent(String pendingIntent) {
        this.pendingIntent = pendingIntent;
    }

    public Map<String, Object> getPendingSlots() {
        return pendingSlots;
    }

    public void setPendingSlots(Map<String, Object> pendingSlots) {
        this.pendingSlots = pendingSlots;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<AgentMessageDTO> getMessages() {
        return messages;
    }

    public void setMessages(List<AgentMessageDTO> messages) {
        this.messages = messages == null ? new ArrayList<>() : messages;
    }

    public List<AgentActionDTO> getActions() {
        return actions;
    }

    public void setActions(List<AgentActionDTO> actions) {
        this.actions = actions == null ? new ArrayList<>() : actions;
    }
}
