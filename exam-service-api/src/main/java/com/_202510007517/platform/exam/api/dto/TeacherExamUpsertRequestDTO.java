package com._202510007517.platform.exam.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class TeacherExamUpsertRequestDTO {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Long courseId;

    @NotBlank
    private String startTime;

    @NotBlank
    private String endTime;

    private String publishDate;

    private Boolean isActive;

    private Boolean isOnline;

    private String location;

    @NotNull
    @Min(1)
    @Max(480)
    private Long duration;

    private List<Long> knowledgePointIds;

    private List<QuestionRequest> questions;

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

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
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

    public Boolean getIsOnline() {
        return isOnline;
    }

    public void setIsOnline(Boolean online) {
        isOnline = online;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public List<Long> getKnowledgePointIds() {
        return knowledgePointIds;
    }

    public void setKnowledgePointIds(List<Long> knowledgePointIds) {
        this.knowledgePointIds = knowledgePointIds;
    }

    public List<QuestionRequest> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionRequest> questions) {
        this.questions = questions;
    }

    public static class QuestionRequest {
        private String questionText;
        private String content;
        private String questionType;
        private String type;
        private List<String> options;
        private String correctAnswer;
        private String answer;
        private Integer score;
        private Long knowledgePointId;

        public String getQuestionText() {
            return questionText;
        }

        public void setQuestionText(String questionText) {
            this.questionText = questionText;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getQuestionType() {
            return questionType;
        }

        public void setQuestionType(String questionType) {
            this.questionType = questionType;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<String> getOptions() {
            return options;
        }

        public void setOptions(List<String> options) {
            this.options = options;
        }

        public String getCorrectAnswer() {
            return correctAnswer;
        }

        public void setCorrectAnswer(String correctAnswer) {
            this.correctAnswer = correctAnswer;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public Long getKnowledgePointId() {
            return knowledgePointId;
        }

        public void setKnowledgePointId(Long knowledgePointId) {
            this.knowledgePointId = knowledgePointId;
        }
    }
}
