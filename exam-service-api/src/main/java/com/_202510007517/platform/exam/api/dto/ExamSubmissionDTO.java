package com._202510007517.platform.exam.api.dto;

public class ExamSubmissionDTO {
    private Long id;
    private Long examId;
    private Long studentId;
    private String studentName;
    private String content;
    private String submissionDate;
    private Integer timeTaken;
    private Boolean graded;
    private Integer score;
    private String teacherComment;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
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

    public Integer getTimeTaken() {
        return timeTaken;
    }

    public void setTimeTaken(Integer timeTaken) {
        this.timeTaken = timeTaken;
    }

    public Boolean getGraded() {
        return graded;
    }

    public void setGraded(Boolean graded) {
        this.graded = graded;
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
