package com._202510007517.platform.notification.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TeacherSendNotificationRequestDTO {

    @NotBlank
    private String type;

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    @NotNull
    private Long studentId;

    private Long relatedId;

    private Boolean isRead;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(Long relatedId) {
        this.relatedId = relatedId;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
}
