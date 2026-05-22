package com._202510007517.platform.analysis.repository;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Repository
public class JpaAnalysisTriggerJobRepository implements AnalysisTriggerJobRepository {

    private final AnalysisTriggerJobJpaRepository analysisTriggerJobJpaRepository;

    public JpaAnalysisTriggerJobRepository(AnalysisTriggerJobJpaRepository analysisTriggerJobJpaRepository) {
        this.analysisTriggerJobJpaRepository = analysisTriggerJobJpaRepository;
    }

    @Override
    @Transactional
    public AnalysisTriggerJob create(Long teacherId, String triggerType, Long classId, Long courseId, Long studentId) {
        AnalysisTriggerJobEntity entity = new AnalysisTriggerJobEntity();
        entity.setTeacherId(teacherId);
        entity.setTriggerType(triggerType);
        entity.setClassId(classId);
        entity.setCourseId(courseId);
        entity.setStudentId(studentId);
        entity.setStatus("RUNNING");
        entity.setRequestedCount(0);
        entity.setWarningCount(0);
        return toDomain(analysisTriggerJobJpaRepository.save(entity));
    }

    @Override
    @Transactional
    public AnalysisTriggerJob complete(
            Long jobId,
            String status,
            int requestedCount,
            int warningCount,
            String message) {
        AnalysisTriggerJobEntity entity = analysisTriggerJobJpaRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Analysis trigger job not found: " + jobId));
        entity.setStatus(status);
        entity.setRequestedCount(requestedCount);
        entity.setWarningCount(warningCount);
        entity.setMessage(message);
        entity.setCompletedAt(Instant.now());
        return toDomain(analysisTriggerJobJpaRepository.save(entity));
    }

    private AnalysisTriggerJob toDomain(AnalysisTriggerJobEntity entity) {
        return new AnalysisTriggerJob(
                entity.getId(),
                entity.getTeacherId(),
                entity.getTriggerType(),
                entity.getClassId(),
                entity.getCourseId(),
                entity.getStudentId(),
                entity.getStatus(),
                entity.getRequestedCount() == null ? 0 : entity.getRequestedCount(),
                entity.getWarningCount() == null ? 0 : entity.getWarningCount(),
                entity.getMessage(),
                entity.getCreatedAt(),
                entity.getCompletedAt());
    }
}
