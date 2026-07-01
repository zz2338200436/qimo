package com._202510007517.platform.exam.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class TeacherExamGradeRequestDTO {
    @Min(0)
    @Max(100)
    private Integer score;
    private String teacherComment;
    private Boolean graded;

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getTeacherComment() {
        return teacherComment;
    }

    public void setTeacherComment(String teacherComment) {
        this.teacherComment = teacherComment;
    }

    public Boolean getGraded() {
        return graded;
    }

    public void setGraded(Boolean graded) {
        this.graded = graded;
    }
}
