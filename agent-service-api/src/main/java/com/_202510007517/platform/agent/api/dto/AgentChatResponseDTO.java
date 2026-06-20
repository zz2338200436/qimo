package com._202510007517.platform.agent.api.dto;

public class AgentChatResponseDTO {
    private String sessionId;
    private String responseType;
    private String message;
    private AgentActionPreviewDTO actionPreview;
    private Object data;

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
}
