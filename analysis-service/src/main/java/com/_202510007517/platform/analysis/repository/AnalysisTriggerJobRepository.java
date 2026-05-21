package com._202510007517.platform.analysis.repository;

public interface AnalysisTriggerJobRepository {

    AnalysisTriggerJob create(Long teacherId, String triggerType, Long classId, Long courseId, Long studentId);

    AnalysisTriggerJob complete(Long jobId, String status, int requestedCount, int warningCount, String message);
}
