package com._202510007517.platform.auth.web.dto;

import jakarta.validation.constraints.NotBlank;

public class RefreshRequestDTO {
    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
