package com._202510007517.platform.assignment.domain;

public class AssignmentSubmissionRecord {
    private Long id;
    private Long assignmentId;
    private Long studentId;
    private String content;
    private String submissionDate;
    private Boolean graded;
    private Boolean isLate;
    private Integer latePenalty;
    private Integer score;
    private String teacherComment;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

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

    public Boolean getGraded() {
        return graded;
    }

    public void setGraded(Boolean graded) {
        this.graded = graded;
    }

    public Boolean getIsLate() {
        return isLate;
    }

    public void setIsLate(Boolean late) {
        isLate = late;
    }

    public Integer getLatePenalty() {
        return latePenalty;
    }

    public void setLatePenalty(Integer latePenalty) {
        this.latePenalty = latePenalty;
    }

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
}
