package com._202510007517.platform.assignment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AssignmentSubmitRequestDTO {
    @NotNull
    private Long studentId;

    @NotBlank
    private String content;

    private String submissionDate;

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(String submissionDate) {
        this.submissionDate = submissionDate;
    }
}
