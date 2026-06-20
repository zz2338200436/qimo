package com._202510007517.platform.agent.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_sessions")
public class AgentSessionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_role", nullable = false, length = 64)
    private String userRole;

    @Column(nullable = false, length = 64)
    private String status;

    @Column(name = "pending_intent", length = 128)
    private String pendingIntent;

    @Column(name = "pending_slots_json")
    private String pendingSlotsJson;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getPendingSlotsJson() {
        return pendingSlotsJson;
    }

    public void setPendingSlotsJson(String pendingSlotsJson) {
        this.pendingSlotsJson = pendingSlotsJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
