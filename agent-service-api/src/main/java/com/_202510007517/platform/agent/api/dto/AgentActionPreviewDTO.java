package com._202510007517.platform.agent.api.dto;

import java.util.Map;

public class AgentActionPreviewDTO {
    private Long actionId;
    private String intent;
    private String riskLevel;
    private String title;
    private String summary;
    private Map<String, Object> preview;
    private String idempotencyKey;
    private Boolean requiresSecondConfirmation;
    private String secondConfirmationPhrase;
    private String secondConfirmationPrompt;

    public Long getActionId() {
        return actionId;
    }

    public void setActionId(Long actionId) {
        this.actionId = actionId;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Map<String, Object> getPreview() {
        return preview;
    }

    public void setPreview(Map<String, Object> preview) {
        this.preview = preview;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Boolean getRequiresSecondConfirmation() {
        return requiresSecondConfirmation;
    }

    public void setRequiresSecondConfirmation(Boolean requiresSecondConfirmation) {
        this.requiresSecondConfirmation = requiresSecondConfirmation;
    }

    public String getSecondConfirmationPhrase() {
        return secondConfirmationPhrase;
    }

    public void setSecondConfirmationPhrase(String secondConfirmationPhrase) {
        this.secondConfirmationPhrase = secondConfirmationPhrase;
    }

    public String getSecondConfirmationPrompt() {
        return secondConfirmationPrompt;
    }

    public void setSecondConfirmationPrompt(String secondConfirmationPrompt) {
        this.secondConfirmationPrompt = secondConfirmationPrompt;
    }
}
