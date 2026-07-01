package com._202510007517.platform.agent.api.dto;

public class AgentChatResponseDTO {
    private String sessionId;
    private String responseType;
    private String message;
    private AgentActionPreviewDTO actionPreview;
    private Object data;
    private Object plannerDecision;
    private Object toolResult;
    private Object artifactSummary;
    private String retrievalStatus;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public AgentActionPreviewDTO getActionPreview() {
        return actionPreview;
    }

    public void setActionPreview(AgentActionPreviewDTO actionPreview) {
        this.actionPreview = actionPreview;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Object getPlannerDecision() {
        return plannerDecision;
    }

    public void setPlannerDecision(Object plannerDecision) {
        this.plannerDecision = plannerDecision;
    }

    public Object getToolResult() {
        return toolResult;
    }

    public void setToolResult(Object toolResult) {
        this.toolResult = toolResult;
    }

    public Object getArtifactSummary() {
        return artifactSummary;
    }

    public void setArtifactSummary(Object artifactSummary) {
        this.artifactSummary = artifactSummary;
    }

    public String getRetrievalStatus() {
        return retrievalStatus;
    }

    public void setRetrievalStatus(String retrievalStatus) {
        this.retrievalStatus = retrievalStatus;
    }
}
