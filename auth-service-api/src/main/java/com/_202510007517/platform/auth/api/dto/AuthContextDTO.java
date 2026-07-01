package com._202510007517.platform.auth.api.dto;

import java.io.Serializable;
import java.util.List;

public class AuthContextDTO implements Serializable {
    private Long userId;
    private List<String> roles;
    private String activeRole;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public String getActiveRole() {
        return activeRole;
    }

    public void setActiveRole(String activeRole) {
        this.activeRole = activeRole;
    }
}
