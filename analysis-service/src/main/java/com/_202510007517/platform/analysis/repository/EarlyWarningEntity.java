package com._202510007517.platform.analysis.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "early_warnings")
public class EarlyWarningEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "warning_type", nullable = false, length = 50)
    private String warningType;

    @Column(name = "warning_level", nullable = false, length = 50)
    private String warningLevel;

    @Column(name = "warning_message", nullable = false, length = 1000)
    private String warningMessage;

    @Column(name = "trigger_date", nullable = false)
    private Instant triggerDate;

    @Column(name = "is_resolved", nullable = false)
    private Boolean resolved;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolved_date")
    private Instant resolvedDate;

    @Column(name = "resolved_note", length = 1000)
    private String resolvedNote;

    @Column(name = "assessment_type", length = 50)
    private String assessmentType;

    @Column(name = "related_assessment_id")
    private Long relatedAssessmentId;

    @Column(name = "student_name", length = 100)
    private String studentName;

    @Column(name = "course_name", length = 100)
    private String courseName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (triggerDate == null) {
            triggerDate = now;
        }
        if (resolved == null) {
            resolved = false;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public String getWarningType() {
        return warningType;
    }

    public void setWarningType(String warningType) {
        this.warningType = warningType;
    }

    public String getWarningLevel() {
        return warningLevel;
    }

    public void setWarningLevel(String warningLevel) {
        this.warningLevel = warningLevel;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public void setWarningMessage(String warningMessage) {
        this.warningMessage = warningMessage;
    }

    public Instant getTriggerDate() {
        return triggerDate;
    }

    public void setTriggerDate(Instant triggerDate) {
        this.triggerDate = triggerDate;
    }

    public Boolean getResolved() {
        return resolved;
    }

    public void setResolved(Boolean resolved) {
        this.resolved = resolved;
    }

    public Long getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(Long resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public Instant getResolvedDate() {
        return resolvedDate;
    }

    public void setResolvedDate(Instant resolvedDate) {
        this.resolvedDate = resolvedDate;
    }

    public String getResolvedNote() {
        return resolvedNote;
    }

    public void setResolvedNote(String resolvedNote) {
        this.resolvedNote = resolvedNote;
    }

    public String getAssessmentType() {
        return assessmentType;
    }

    public void setAssessmentType(String assessmentType) {
        this.assessmentType = assessmentType;
    }

    public Long getRelatedAssessmentId() {
        return relatedAssessmentId;
    }

    public void setRelatedAssessmentId(Long relatedAssessmentId) {
        this.relatedAssessmentId = relatedAssessmentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
