package com._202510007517.platform.auth.controller.dto;

import jakarta.validation.constraints.NotBlank;

public class SwitchRoleRequestDTO {
    @NotBlank(message = "targetRole 不能为空")
    private String targetRole;

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }
}
