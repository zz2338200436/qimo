package com._202510007517.platform.assignment.service;

import com._202510007517.platform.assignment.domain.AssignmentRecord;
import com._202510007517.platform.assignment.domain.AssignmentSubmissionRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class AssignmentKnowledgeMasterySynchronizer {

    private static final Logger logger = LoggerFactory.getLogger(AssignmentKnowledgeMasterySynchronizer.class);

    private final JdbcTemplate jdbcTemplate;

    public AssignmentKnowledgeMasterySynchronizer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void syncAfterAssignmentGraded(AssignmentRecord assignment, AssignmentSubmissionRecord submission) {
        if (assignment == null || submission == null || submission.getStudentId() == null) {
            return;
        }

        KnowledgePointScope knowledgePointScope = resolveKnowledgePointScope(assignment);
        List<Long> knowledgePointIds = knowledgePointScope.knowledgePointIds();
        if (knowledgePointIds.isEmpty()) {
            logger.warn(
                    "作业没有可同步的知识点，跳过掌握情况更新。assignmentId={}, courseId={}",
                    assignment.getId(),
                    assignment.getCourseId());
            return;
        }

        for (Long knowledgePointId : knowledgePointIds) {
            MasterySummary summary = knowledgePointScope.fallbackToCourseKnowledgePoints()
                    ? calculateCourseAssignmentMasterySummary(assignment.getCourseId(), submission.getStudentId())
                    : calculateMasterySummary(knowledgePointId, submission.getStudentId());
            if (summary.maxScore() <= 0) {
                continue;
            }
            double masteryRate = summary.masteryRate();
            upsertLegacyKnowledgeMastery(submission.getStudentId(), knowledgePointId, toMasteryLevel(masteryRate));
            upsertAnalysisKnowledgeMastery(assignment, submission, knowledgePointId, masteryRate);
        }
    }

    private MasterySummary calculateMasterySummary(Long knowledgePointId, Long studentId) {
        MasterySummary assignmentSummary = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(asub.score), 0) AS earned_score,
                       COALESCE(SUM(COALESCE(a.max_score, 100)), 0) AS max_score
                FROM assignment_submissions asub
                JOIN assignments a ON asub.assignment_id = a.id
                JOIN assignment_knowledge_points akp ON a.id = akp.assignment_id
                WHERE akp.knowledge_point_id = ?
                  AND asub.graded = 1
                  AND asub.score IS NOT NULL
                  AND asub.student_id = ?
                """, (rs, rowNum) -> new MasterySummary(
                rs.getDouble("earned_score"),
                rs.getDouble("max_score")
        ), knowledgePointId, studentId);

        MasterySummary examSummary = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(esub.score * 1.5), 0) AS earned_score,
                       COALESCE(SUM(100 * 1.5), 0) AS max_score
                FROM sc_exam.exam_submissions esub
                JOIN sc_exam.exams e ON esub.exam_id = e.id
                JOIN sc_exam.exam_knowledge_points ekp ON e.id = ekp.exam_id
                WHERE ekp.knowledge_point_id = ?
                  AND esub.graded = 1
                  AND esub.score IS NOT NULL
                  AND esub.student_id = ?
                """, (rs, rowNum) -> new MasterySummary(
                rs.getDouble("earned_score"),
                rs.getDouble("max_score")
        ), knowledgePointId, studentId);

        double earnedScore = valueOrZero(assignmentSummary).earnedScore() + valueOrZero(examSummary).earnedScore();
        double maxScore = valueOrZero(assignmentSummary).maxScore() + valueOrZero(examSummary).maxScore();
        return new MasterySummary(earnedScore, maxScore);
    }

    private MasterySummary calculateCourseAssignmentMasterySummary(Long courseId, Long studentId) {
        if (courseId == null) {
            return new MasterySummary(0, 0);
        }
        return valueOrZero(jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(asub.score), 0) AS earned_score,
                       COALESCE(SUM(COALESCE(a.max_score, 100)), 0) AS max_score
                FROM assignment_submissions asub
                JOIN assignments a ON asub.assignment_id = a.id
                WHERE a.course_id = ?
                  AND asub.graded = 1
                  AND asub.score IS NOT NULL
                  AND asub.student_id = ?
                """, (rs, rowNum) -> new MasterySummary(
                rs.getDouble("earned_score"),
                rs.getDouble("max_score")
        ), courseId, studentId));
    }

    private KnowledgePointScope resolveKnowledgePointScope(AssignmentRecord assignment) {
        List<Long> linkedKnowledgePointIds = jdbcTemplate.queryForList("""
                SELECT knowledge_point_id
                FROM assignment_knowledge_points
                WHERE assignment_id = ?
                """, Long.class, assignment.getId());
        if (!linkedKnowledgePointIds.isEmpty() || assignment.getCourseId() == null) {
            return new KnowledgePointScope(linkedKnowledgePointIds, false);
        }

        List<Long> courseKnowledgePointIds = jdbcTemplate.queryForList("""
                SELECT id
                FROM sc_course.teacher_knowledge_points
                WHERE course_id = ?
                ORDER BY order_index, id
                """, Long.class, assignment.getCourseId());
        return new KnowledgePointScope(courseKnowledgePointIds, true);
    }

    private void upsertLegacyKnowledgeMastery(Long studentId, Long knowledgePointId, String masteryLevel) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO major_assignment.knowledge_mastery (student_id, knowledge_point_id, mastery_level, last_assessed_date)
                    VALUES (?, ?, ?, NOW())
                    ON DUPLICATE KEY UPDATE
                        mastery_level = VALUES(mastery_level),
                        last_assessed_date = VALUES(last_assessed_date)
                    """, studentId, knowledgePointId, masteryLevel);
        } catch (DataAccessException ex) {
            logger.warn(
                    "同步旧单体知识点掌握情况失败，不影响作业批改。studentId={}, knowledgePointId={}",
                    studentId,
                    knowledgePointId,
                    ex);
        }
    }

    private void upsertAnalysisKnowledgeMastery(
            AssignmentRecord assignment,
            AssignmentSubmissionRecord submission,
            Long knowledgePointId,
            double masteryRate) {
        if (assignment.getCourseId() == null || assignment.getId() == null || submission.getId() == null) {
            return;
        }

        try {
            jdbcTemplate.update("""
                    INSERT INTO sc_analysis.kp_mastery (
                        student_id, course_id, class_id, knowledge_point_id, mastery_score,
                        evidence_count, last_source_type, last_source_id, last_event_id, updated_at
                    )
                    VALUES (?, ?, ?, ?, ?, 1, 'assignment', ?, ?, NOW(6))
                    ON DUPLICATE KEY UPDATE
                        class_id = VALUES(class_id),
                        mastery_score = VALUES(mastery_score),
                        evidence_count = GREATEST(evidence_count, 1),
                        last_source_type = VALUES(last_source_type),
                        last_source_id = VALUES(last_source_id),
                        last_event_id = VALUES(last_event_id),
                        updated_at = VALUES(updated_at)
                    """,
                    submission.getStudentId(),
                    assignment.getCourseId(),
                    resolveClassId(assignment.getCourseId(), submission.getStudentId()),
                    knowledgePointId,
                    toMasteryScore(masteryRate),
                    assignment.getId(),
                    "assignment-graded-" + submission.getId());
        } catch (DataAccessException ex) {
            logger.warn(
                    "同步分析库知识点掌握情况失败，不影响作业批改。studentId={}, assignmentId={}, knowledgePointId={}",
                    submission.getStudentId(),
                    assignment.getId(),
                    knowledgePointId,
                    ex);
        }
    }

    private Long resolveClassId(Long courseId, Long studentId) {
        List<Long> classIds = jdbcTemplate.queryForList("""
                SELECT cs.class_id
                FROM sc_course.class_students cs
                JOIN sc_course.class_courses cc ON cs.class_id = cc.class_id
                WHERE cs.student_id = ?
                  AND cc.course_id = ?
                ORDER BY cs.class_id
                LIMIT 1
                """, Long.class, studentId, courseId);
        return classIds.isEmpty() ? null : classIds.get(0);
    }

    private static BigDecimal toMasteryScore(double masteryRate) {
        return BigDecimal.valueOf(masteryRate)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }

    private static MasterySummary valueOrZero(MasterySummary summary) {
        return summary == null ? new MasterySummary(0, 0) : summary;
    }

    private static String toMasteryLevel(double masteryRate) {
        if (masteryRate >= 85) {
            return "优秀";
        }
        if (masteryRate >= 70) {
            return "良好";
        }
        if (masteryRate >= 60) {
            return "一般";
        }
        return "较差";
    }

    private record MasterySummary(double earnedScore, double maxScore) {
        double masteryRate() {
            return maxScore <= 0 ? 0 : (earnedScore / maxScore) * 100;
        }
    }

    private record KnowledgePointScope(List<Long> knowledgePointIds, boolean fallbackToCourseKnowledgePoints) {
    }
}
