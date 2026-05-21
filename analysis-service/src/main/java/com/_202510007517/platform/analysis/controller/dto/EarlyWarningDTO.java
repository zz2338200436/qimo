package com._202510007517.platform.analysis.controller.dto;

import java.time.Instant;

public record EarlyWarningDTO(
        Long id,
        Long studentId,
        Long courseId,
        Long teacherId,
        String warningType,
        String warningLevel,
        String warningMessage,
        Instant triggerDate,
        Boolean isResolved,
        Long resolvedBy,
        Instant resolvedDate,
        String resolvedNote,
        String assessmentType,
        Long relatedAssessmentId,
        String studentName,
        String courseName,
        String status,
        String reason,
        String suggestion) {
}
