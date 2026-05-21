package com._202510007517.platform.analysis.repository;

import com._202510007517.platform.analysis.web.dto.EarlyWarningDTO;

import java.util.List;

public interface EarlyWarningRepository {

    List<EarlyWarningDTO> findUnresolvedByTeacherId(Long teacherId);

    List<EarlyWarningDTO> findByStudentId(Long studentId);

    List<EarlyWarningDTO> findWarningsByCondition(
            Long teacherId,
            Long classId,
            Long courseId,
            String warningType,
            String status,
            int offset,
            int size);

    long countWarningsByCondition(Long teacherId, Long classId, Long courseId, String warningType, String status);

    long countTotalWarnings(Long teacherId, Long classId, Long courseId);

    long countPendingWarnings(Long teacherId, Long classId, Long courseId);

    long countWarningsByType(Long teacherId, String warningType, Long classId, Long courseId);

    EarlyWarningDTO findWarningById(Long teacherId, Long warningId);

    List<EarlyWarningDTO> findWarningsForExport(Long teacherId, Long classId, Long courseId, String warningType, String status);

    List<EarlyWarningDTO> findByCourseId(
            Long teacherId,
            Long courseId,
            String warningType,
            String warningLevel,
            Boolean isResolved);

    boolean existsPendingWarning(Long teacherId, Long studentId, Long courseId, String warningType);

    boolean updateWarningStatus(Long teacherId, Long warningId, String status, String resolvedNote);

    EarlyWarningDTO insert(
            Long teacherId,
            Long studentId,
            Long courseId,
            String warningType,
            String warningLevel,
            String warningMessage,
            String assessmentType,
            Long relatedAssessmentId);

    boolean deleteById(Long teacherId, Long warningId);
}
