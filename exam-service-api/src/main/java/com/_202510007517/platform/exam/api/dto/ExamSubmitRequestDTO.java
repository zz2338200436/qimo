package com._202510007517.platform.exam.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class ExamSubmitRequestDTO {
    @NotNull
    private Long studentId;

    private Integer timeTaken;

    private Map<String, String> answers;

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Integer getTimeTaken() {
        return timeTaken;
    }

    public void setTimeTaken(Integer timeTaken) {
        this.timeTaken = timeTaken;
    }

    public Map<String, String> getAnswers() {
        return answers;
    }

    public void setAnswers(Map<String, String> answers) {
        this.answers = answers;
    }
}
