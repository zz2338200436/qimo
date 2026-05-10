package com._202510007517.major_assignment.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EarlyWarning {
    private Long id;
    private Long studentId;
    private Long courseId;
    private Long teacherId;
    private String warningType;
    private String warningLevel;
    private String warningMessage;
    private LocalDateTime triggerDate;
    private Boolean isResolved;
    private Long resolvedBy;
    private LocalDateTime resolvedDate;
    private String resolvedNote;
    private String assessmentType;
    private Long relatedAssessmentId;
    
    // 非数据库字段，用于前端展示
    private String studentName;
    private String courseName;
    private String status;
    private String reason;
    private String suggestion;
}