package com._202510007517.platform.agent.api.dto;

import jakarta.validation.constraints.NotBlank;

public class AgentSessionUpdateDTO {
    @NotBlank
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
