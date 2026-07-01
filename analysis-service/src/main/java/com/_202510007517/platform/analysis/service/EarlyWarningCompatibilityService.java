package com._202510007517.platform.analysis.service;

import com._202510007517.platform.analysis.repository.EarlyWarningRepository;
import com._202510007517.platform.analysis.controller.dto.EarlyWarningDTO;
import com._202510007517.platform.analysis.controller.dto.EarlyWarningPageResult;
import com._202510007517.platform.analysis.controller.dto.WarningStatsDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EarlyWarningCompatibilityService {

    private final EarlyWarningRepository earlyWarningRepository;

    public EarlyWarningCompatibilityService(EarlyWarningRepository earlyWarningRepository) {
        this.earlyWarningRepository = earlyWarningRepository;
    }

    public List<EarlyWarningDTO> listPendingWarnings(Long teacherId) {
        requireTeacher(teacherId);
        return earlyWarningRepository.findUnresolvedByTeacherId(teacherId);
    }

    public List<EarlyWarningDTO> listStudentWarnings(Long studentId) {
        requireStudent(studentId);
        return earlyWarningRepository.findByStudentId(studentId);
    }

    public WarningStatsDTO getStats(Long teacherId, Long classId, Long courseId) {
        requireTeacher(teacherId);
        long totalWarnings = earlyWarningRepository.countTotalWarnings(teacherId, classId, courseId);
        long pendingWarnings = earlyWarningRepository.countPendingWarnings(teacherId, classId, courseId);
        return new WarningStatsDTO(
                totalWarnings,
                pendingWarnings,
                0,
                totalWarnings - pendingWarnings,
                earlyWarningRepository.countWarningsByType(teacherId, "LOW_SCORE", classId, courseId)
                        + earlyWarningRepository.countWarningsByType(teacherId, "low_score", classId, courseId),
                earlyWarningRepository.countWarningsByType(teacherId, "LOW_ATTENDANCE", classId, courseId)
                        + earlyWarningRepository.countWarningsByType(teacherId, "low_attendance", classId, courseId),
                earlyWarningRepository.countWarningsByType(teacherId, "LATE_SUBMISSION", classId, courseId)
                        + earlyWarningRepository.countWarningsByType(teacherId, "late_submission", classId, courseId),
                earlyWarningRepository.countWarningsByType(teacherId, "PROGRESS", classId, courseId)
                        + earlyWarningRepository.countWarningsByType(teacherId, "progress", classId, courseId));
    }

    public EarlyWarningPageResult listWarnings(
            Long teacherId,
            Long classId,
            Long courseId,
            String warningType,
            String status,
            int page,
            int size) {
        requireTeacher(teacherId);
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int offset = (safePage - 1) * safeSize;
        List<EarlyWarningDTO> warnings = earlyWarningRepository.findWarningsByCondition(
                teacherId,
                classId,
                courseId,
                warningType,
                status,
                offset,
                safeSize);
        long totalElements = earlyWarningRepository.countWarningsByCondition(
                teacherId,
                classId,
                courseId,
                warningType,
                status);
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);
        return new EarlyWarningPageResult(
                warnings,
                safePage,
                safeSize,
                totalElements,
                totalPages,
                safePage == 1,
                safePage >= totalPages,
                offset,
                warnings.size(),
                warnings.isEmpty());
    }

    public EarlyWarningDTO getDetail(Long teacherId, Long warningId) {
        requireTeacher(teacherId);
        EarlyWarningDTO warning = earlyWarningRepository.findWarningById(teacherId, warningId);
        if (warning == null) {
            throw new WarningNotFoundException();
        }
        return warning;
    }

    public List<EarlyWarningDTO> listCourseWarnings(
            Long teacherId,
            Long courseId,
            String warningType,
            String warningLevel,
            Boolean isResolved) {
        requireTeacher(teacherId);
        return earlyWarningRepository.findByCourseId(teacherId, courseId, warningType, warningLevel, isResolved);
    }

    public boolean updateStatus(Long teacherId, Long warningId, String status, String resolvedNote) {
        requireTeacher(teacherId);
        return earlyWarningRepository.updateWarningStatus(teacherId, warningId, status, resolvedNote);
    }

    public List<EarlyWarningDTO> listWarningsForExport(
            Long teacherId,
            Long classId,
            Long courseId,
            String warningType,
            String status) {
        requireTeacher(teacherId);
        return earlyWarningRepository.findWarningsForExport(teacherId, classId, courseId, warningType, status);
    }

    public EarlyWarningDTO createWarning(
            Long teacherId,
            Long studentId,
            Long courseId,
            String warningType,
            String warningLevel,
            String warningMessage,
            String assessmentType,
            Long relatedAssessmentId) {
        requireTeacher(teacherId);
        return earlyWarningRepository.insert(
                teacherId,
                studentId,
                courseId,
                warningType,
                warningLevel,
                warningMessage,
                assessmentType,
                relatedAssessmentId);
    }

    public boolean deleteWarning(Long teacherId, Long warningId) {
        requireTeacher(teacherId);
        return earlyWarningRepository.deleteById(teacherId, warningId);
    }

    private static void requireTeacher(Long teacherId) {
        if (teacherId == null) {
            throw new IllegalArgumentException("缺少教师身份");
        }
    }

    private static void requireStudent(Long studentId) {
        if (studentId == null) {
            throw new IllegalArgumentException("缺少学生身份");
        }
    }

    public static final class WarningNotFoundException extends RuntimeException {
    }
}
