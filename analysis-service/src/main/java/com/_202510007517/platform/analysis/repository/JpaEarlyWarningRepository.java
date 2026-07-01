package com._202510007517.platform.analysis.repository;

import com._202510007517.platform.analysis.controller.dto.EarlyWarningDTO;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Repository
public class JpaEarlyWarningRepository implements EarlyWarningRepository {

    private static final Sort DEFAULT_SORT = Sort.by(
            Sort.Order.desc("triggerDate"),
            Sort.Order.desc("id"));

    private final EarlyWarningJpaRepository earlyWarningJpaRepository;

    public JpaEarlyWarningRepository(EarlyWarningJpaRepository earlyWarningJpaRepository) {
        this.earlyWarningJpaRepository = earlyWarningJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EarlyWarningDTO> findUnresolvedByTeacherId(Long teacherId) {
        return findWarningsByCondition(teacherId, null, null, null, "pending", 0, 100);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EarlyWarningDTO> findByStudentId(Long studentId) {
        return earlyWarningJpaRepository.findAll(byStudentId(studentId), DEFAULT_SORT).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EarlyWarningDTO> findWarningsByCondition(
            Long teacherId,
            Long classId,
            Long courseId,
            String warningType,
            String status,
            int offset,
            int size) {
        int safeOffset = Math.max(offset, 0);
        int safeSize = Math.max(size, 1);
        Pageable pageable = PageRequest.of(safeOffset / safeSize, safeSize, DEFAULT_SORT);
        return earlyWarningJpaRepository.findAll(
                        buildFilterSpec(teacherId, classId, courseId, warningType, status, null, null),
                        pageable)
                .getContent()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countWarningsByCondition(Long teacherId, Long classId, Long courseId, String warningType, String status) {
        return earlyWarningJpaRepository.count(
                buildFilterSpec(teacherId, classId, courseId, warningType, status, null, null));
    }

    @Override
    @Transactional(readOnly = true)
    public long countTotalWarnings(Long teacherId, Long classId, Long courseId) {
        return countWarningsByCondition(teacherId, classId, courseId, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingWarnings(Long teacherId, Long classId, Long courseId) {
        return countWarningsByCondition(teacherId, classId, courseId, null, "pending");
    }

    @Override
    @Transactional(readOnly = true)
    public long countWarningsByType(Long teacherId, String warningType, Long classId, Long courseId) {
        return countWarningsByCondition(teacherId, classId, courseId, warningType, null);
    }

    @Override
    @Transactional(readOnly = true)
    public EarlyWarningDTO findWarningById(Long teacherId, Long warningId) {
        return earlyWarningJpaRepository.findOne(byTeacherIdAndWarningId(teacherId, warningId))
                .map(this::toDto)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EarlyWarningDTO> findWarningsForExport(
            Long teacherId,
            Long classId,
            Long courseId,
            String warningType,
            String status) {
        return earlyWarningJpaRepository.findAll(
                        buildFilterSpec(teacherId, classId, courseId, warningType, status, null, null),
                        DEFAULT_SORT)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EarlyWarningDTO> findByCourseId(
            Long teacherId,
            Long courseId,
            String warningType,
            String warningLevel,
            Boolean isResolved) {
        return earlyWarningJpaRepository.findAll(
                        buildFilterSpec(teacherId, null, courseId, warningType, null, warningLevel, isResolved),
                        DEFAULT_SORT)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsPendingWarning(Long teacherId, Long studentId, Long courseId, String warningType) {
        return earlyWarningJpaRepository.count((root, query, cb) -> cb.and(
                cb.equal(root.get("teacherId"), teacherId),
                cb.equal(root.get("studentId"), studentId),
                cb.equal(root.get("courseId"), courseId),
                cb.equal(root.get("warningType"), warningType),
                cb.isFalse(root.get("resolved")))) > 0;
    }

    @Override
    @Transactional
    public boolean updateWarningStatus(Long teacherId, Long warningId, String status, String resolvedNote) {
        Boolean resolved = switch (normalizeStatus(status)) {
            case "resolved" -> true;
            case "pending", "processing" -> false;
            default -> null;
        };
        if (resolved == null) {
            return false;
        }
        EarlyWarningEntity entity = earlyWarningJpaRepository.findOne(byTeacherIdAndWarningId(teacherId, warningId))
                .orElse(null);
        if (entity == null) {
            return false;
        }
        entity.setResolved(resolved);
        if (resolved) {
            entity.setResolvedBy(teacherId);
            entity.setResolvedDate(Instant.now());
        } else {
            entity.setResolvedDate(null);
        }
        if (resolvedNote != null) {
            entity.setResolvedNote(resolvedNote);
        }
        earlyWarningJpaRepository.save(entity);
        return true;
    }

    @Override
    @Transactional
    public EarlyWarningDTO insert(
            Long teacherId,
            Long studentId,
            Long courseId,
            String warningType,
            String warningLevel,
            String warningMessage,
            String assessmentType,
            Long relatedAssessmentId) {
        EarlyWarningEntity entity = new EarlyWarningEntity();
        entity.setTeacherId(teacherId);
        entity.setStudentId(studentId);
        entity.setCourseId(courseId);
        entity.setWarningType(warningType);
        entity.setWarningLevel(warningLevel);
        entity.setWarningMessage(warningMessage);
        entity.setResolved(false);
        entity.setAssessmentType(assessmentType);
        entity.setRelatedAssessmentId(relatedAssessmentId);
        entity.setStudentName("学生 " + studentId);
        entity.setCourseName("课程 " + courseId);
        return toDto(earlyWarningJpaRepository.save(entity));
    }

    @Override
    @Transactional
    public boolean deleteById(Long teacherId, Long warningId) {
        EarlyWarningEntity entity = earlyWarningJpaRepository.findOne(byTeacherIdAndWarningId(teacherId, warningId))
                .orElse(null);
        if (entity == null) {
            return false;
        }
        earlyWarningJpaRepository.delete(entity);
        return true;
    }

    private Specification<EarlyWarningEntity> byStudentId(Long studentId) {
        return (root, query, cb) -> cb.equal(root.get("studentId"), studentId);
    }

    private Specification<EarlyWarningEntity> byTeacherIdAndWarningId(Long teacherId, Long warningId) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("teacherId"), teacherId),
                cb.equal(root.get("id"), warningId));
    }

    private Specification<EarlyWarningEntity> buildFilterSpec(
            Long teacherId,
            Long classId,
            Long courseId,
            String warningType,
            String status,
            String warningLevel,
            Boolean isResolved) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("teacherId"), teacherId));
            if (classId != null) {
                predicates.add(cb.equal(root.get("classId"), classId));
            }
            if (courseId != null) {
                predicates.add(cb.equal(root.get("courseId"), courseId));
            }
            if (hasText(warningType)) {
                predicates.add(cb.equal(root.get("warningType"), warningType));
            }
            if (hasText(warningLevel)) {
                predicates.add(cb.equal(root.get("warningLevel"), warningLevel));
            }
            if (isResolved != null) {
                predicates.add(Boolean.TRUE.equals(isResolved)
                        ? cb.isTrue(root.get("resolved"))
                        : cb.isFalse(root.get("resolved")));
            } else {
                String normalizedStatus = normalizeStatus(status);
                if ("pending".equals(normalizedStatus) || "processing".equals(normalizedStatus)) {
                    predicates.add(cb.isFalse(root.get("resolved")));
                } else if ("resolved".equals(normalizedStatus)) {
                    predicates.add(cb.isTrue(root.get("resolved")));
                }
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private EarlyWarningDTO toDto(EarlyWarningEntity entity) {
        boolean resolved = Boolean.TRUE.equals(entity.getResolved());
        return new EarlyWarningDTO(
                entity.getId(),
                entity.getStudentId(),
                entity.getCourseId(),
                entity.getTeacherId(),
                entity.getWarningType(),
                entity.getWarningLevel(),
                entity.getWarningMessage(),
                entity.getTriggerDate(),
                resolved,
                entity.getResolvedBy(),
                entity.getResolvedDate(),
                entity.getResolvedNote(),
                entity.getAssessmentType(),
                entity.getRelatedAssessmentId(),
                entity.getStudentName(),
                entity.getCourseName(),
                resolved ? "resolved" : "pending",
                entity.getWarningMessage(),
                suggestionByWarningType(entity.getWarningType()));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
    }

    private static String suggestionByWarningType(String warningType) {
        return switch (warningType == null ? "" : warningType) {
            case "absent", "low_attendance", "LOW_ATTENDANCE" -> "建议与学生家长联系，了解缺勤原因";
            case "low_score", "LOW_SCORE" -> "建议安排课后辅导，重点讲解薄弱知识点";
            case "late_submission", "homework", "LATE_SUBMISSION", "HOMEWORK" -> "建议与学生沟通，了解作业完成困难";
            case "progress", "PROGRESS" -> "建议关注学生学习进度，提供额外辅导";
            default -> "建议根据具体情况采取相应措施";
        };
    }
}
