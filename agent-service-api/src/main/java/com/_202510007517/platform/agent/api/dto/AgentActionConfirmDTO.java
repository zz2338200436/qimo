package com._202510007517.platform.agent.api.dto;

import jakarta.validation.constraints.NotBlank;

public class AgentActionConfirmDTO {
    @NotBlank
    private String idempotencyKey;
    private String secondConfirmationText;

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getSecondConfirmationText() {
        return secondConfirmationText;
    }

    public void setSecondConfirmationText(String secondConfirmationText) {
        this.secondConfirmationText = secondConfirmationText;
    }
}
