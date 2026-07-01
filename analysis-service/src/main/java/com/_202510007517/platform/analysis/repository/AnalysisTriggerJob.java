package com._202510007517.platform.analysis.repository;

import java.time.Instant;

public record AnalysisTriggerJob(
        Long id,
        Long teacherId,
        String triggerType,
        Long classId,
        Long courseId,
        Long studentId,
        String status,
        int requestedCount,
        int warningCount,
        String message,
        Instant createdAt,
        Instant completedAt) {
}
