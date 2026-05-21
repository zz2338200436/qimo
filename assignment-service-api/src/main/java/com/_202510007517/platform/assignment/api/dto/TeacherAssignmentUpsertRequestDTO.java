package com._202510007517.platform.assignment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class TeacherAssignmentUpsertRequestDTO {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Long courseId;

    @NotBlank
    private String dueDate;

    private String publishDate;

    private Boolean isActive;

    @NotNull
    private Integer maxScore;

    private List<Long> knowledgePointIds;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(String publishDate) {
        this.publishDate = publishDate;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public Integer getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Integer maxScore) {
        this.maxScore = maxScore;
    }

    public List<Long> getKnowledgePointIds() {
        return knowledgePointIds;
    }

    public void setKnowledgePointIds(List<Long> knowledgePointIds) {
        this.knowledgePointIds = knowledgePointIds;
    }
}
