package com._202510007517.platform.analysis.repository;

import com._202510007517.platform.analysis.web.dto.EarlyWarningDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class JdbcEarlyWarningRepository implements EarlyWarningRepository {

    private static final RowMapper<EarlyWarningDTO> ROW_MAPPER = JdbcEarlyWarningRepository::mapWarning;

    private final JdbcTemplate jdbcTemplate;

    public JdbcEarlyWarningRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<EarlyWarningDTO> findUnresolvedByTeacherId(Long teacherId) {
        return findWarningsByCondition(teacherId, null, null, null, "pending", 0, 100);
    }

    @Override
    public List<EarlyWarningDTO> findByStudentId(Long studentId) {
        return jdbcTemplate.query("""
                        SELECT id, student_id, course_id, teacher_id, warning_type, warning_level, warning_message,
                               trigger_date, is_resolved, resolved_by, resolved_date, resolved_note,
                               assessment_type, related_assessment_id, student_name, course_name
                        FROM early_warnings
                        WHERE student_id = ?
                        ORDER BY trigger_date DESC, id DESC
                        """,
                ROW_MAPPER,
                studentId);
    }

    @Override
    public List<EarlyWarningDTO> findWarningsByCondition(
            Long teacherId,
            Long classId,
            Long courseId,
            String warningType,
            String status,
            int offset,
            int size) {
        QueryParts parts = buildFilterWhere(teacherId, classId, courseId, warningType, status, null, null);
        parts.sql.insert(0, """
                SELECT id, student_id, course_id, teacher_id, warning_type, warning_level, warning_message,
                       trigger_date, is_resolved, resolved_by, resolved_date, resolved_note,
                       assessment_type, related_assessment_id, student_name, course_name
                FROM early_warnings
                """);
        parts.sql.append(" ORDER BY trigger_date DESC, id DESC LIMIT ? OFFSET ?");
        parts.args.add(size);
        parts.args.add(offset);
        return jdbcTemplate.query(parts.sql.toString(), ROW_MAPPER, parts.args.toArray());
    }

    @Override
    public long countWarningsByCondition(Long teacherId, Long classId, Long courseId, String warningType, String status) {
        QueryParts parts = buildFilterWhere(teacherId, classId, courseId, warningType, status, null, null);
        parts.sql.insert(0, "SELECT COUNT(*) FROM early_warnings ");
        Long count = jdbcTemplate.queryForObject(parts.sql.toString(), Long.class, parts.args.toArray());
        return count == null ? 0 : count;
    }

    @Override
    public long countTotalWarnings(Long teacherId, Long classId, Long courseId) {
        return countWarningsByCondition(teacherId, classId, courseId, null, null);
    }

    @Override
    public long countPendingWarnings(Long teacherId, Long classId, Long courseId) {
        return countWarningsByCondition(teacherId, classId, courseId, null, "pending");
    }

    @Override
    public long countWarningsByType(Long teacherId, String warningType, Long classId, Long courseId) {
        return countWarningsByCondition(teacherId, classId, courseId, warningType, null);
    }

    @Override
    public EarlyWarningDTO findWarningById(Long teacherId, Long warningId) {
        List<EarlyWarningDTO> rows = jdbcTemplate.query("""
                        SELECT id, student_id, course_id, teacher_id, warning_type, warning_level, warning_message,
                               trigger_date, is_resolved, resolved_by, resolved_date, resolved_note,
                               assessment_type, related_assessment_id, student_name, course_name
                        FROM early_warnings
                        WHERE teacher_id = ? AND id = ?
                        """,
                ROW_MAPPER,
                teacherId,
                warningId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    public List<EarlyWarningDTO> findWarningsForExport(
            Long teacherId,
            Long classId,
            Long courseId,
            String warningType,
            String status) {
        QueryParts parts = buildFilterWhere(teacherId, classId, courseId, warningType, status, null, null);
        parts.sql.insert(0, """
                SELECT id, student_id, course_id, teacher_id, warning_type, warning_level, warning_message,
                       trigger_date, is_resolved, resolved_by, resolved_date, resolved_note,
                       assessment_type, related_assessment_id, student_name, course_name
                FROM early_warnings
                """);
        parts.sql.append(" ORDER BY trigger_date DESC, id DESC");
        return jdbcTemplate.query(parts.sql.toString(), ROW_MAPPER, parts.args.toArray());
    }

    @Override
    public List<EarlyWarningDTO> findByCourseId(
            Long teacherId,
            Long courseId,
            String warningType,
            String warningLevel,
            Boolean isResolved) {
        QueryParts parts = buildFilterWhere(
                teacherId,
                null,
                courseId,
                warningType,
                null,
                warningLevel,
                isResolved);
        parts.sql.insert(0, """
                SELECT id, student_id, course_id, teacher_id, warning_type, warning_level, warning_message,
                       trigger_date, is_resolved, resolved_by, resolved_date, resolved_note,
                       assessment_type, related_assessment_id, student_name, course_name
                FROM early_warnings
                """);
        parts.sql.append(" ORDER BY trigger_date DESC, id DESC");
        return jdbcTemplate.query(parts.sql.toString(), ROW_MAPPER, parts.args.toArray());
    }

    @Override
    public boolean existsPendingWarning(Long teacherId, Long studentId, Long courseId, String warningType) {
        Long count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM early_warnings
                        WHERE teacher_id = ?
                          AND student_id = ?
                          AND course_id = ?
                          AND warning_type = ?
                          AND is_resolved = FALSE
                        """,
                Long.class,
                teacherId,
                studentId,
                courseId,
                warningType);
        return count != null && count > 0;
    }

    @Override
    public boolean updateWarningStatus(Long teacherId, Long warningId, String status, String resolvedNote) {
        Boolean resolved = switch (normalizeStatus(status)) {
            case "resolved" -> true;
            case "pending", "processing" -> false;
            default -> null;
        };
        if (resolved == null) {
            return false;
        }
        int updated = jdbcTemplate.update("""
                        UPDATE early_warnings
                        SET is_resolved = ?,
                            resolved_by = CASE WHEN ? THEN ? ELSE resolved_by END,
                            resolved_date = CASE WHEN ? THEN CURRENT_TIMESTAMP(6) ELSE NULL END,
                            resolved_note = COALESCE(?, resolved_note)
                        WHERE id = ? AND teacher_id = ?
                        """,
                resolved,
                resolved,
                teacherId,
                resolved,
                resolvedNote,
                warningId,
                teacherId);
        return updated > 0;
    }

    @Override
    public EarlyWarningDTO insert(
            Long teacherId,
            Long studentId,
            Long courseId,
            String warningType,
            String warningLevel,
            String warningMessage,
            String assessmentType,
            Long relatedAssessmentId) {
        jdbcTemplate.update("""
                        INSERT INTO early_warnings (
                            student_id, course_id, teacher_id, warning_type, warning_level, warning_message,
                            trigger_date, is_resolved, assessment_type, related_assessment_id,
                            student_name, course_name
                        )
                        VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6), FALSE, ?, ?, ?, ?)
                        """,
                studentId,
                courseId,
                teacherId,
                warningType,
                warningLevel,
                warningMessage,
                assessmentType,
                relatedAssessmentId,
                "学生 " + studentId,
                "课程 " + courseId);
        Long id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM early_warnings WHERE teacher_id = ?", Long.class, teacherId);
        return findWarningById(teacherId, id);
    }

    @Override
    public boolean deleteById(Long teacherId, Long warningId) {
        return jdbcTemplate.update(
                "DELETE FROM early_warnings WHERE id = ? AND teacher_id = ?",
                warningId,
                teacherId) > 0;
    }

    private static QueryParts buildFilterWhere(
            Long teacherId,
            Long classId,
            Long courseId,
            String warningType,
            String status,
            String warningLevel,
            Boolean isResolved) {
        QueryParts parts = new QueryParts();
        parts.sql.append(" WHERE teacher_id = ?");
        parts.args.add(teacherId);
        if (classId != null) {
            parts.sql.append(" AND class_id = ?");
            parts.args.add(classId);
        }
        if (courseId != null) {
            parts.sql.append(" AND course_id = ?");
            parts.args.add(courseId);
        }
        if (warningType != null && !warningType.isBlank()) {
            parts.sql.append(" AND warning_type = ?");
            parts.args.add(warningType);
        }
        if (warningLevel != null && !warningLevel.isBlank()) {
            parts.sql.append(" AND warning_level = ?");
            parts.args.add(warningLevel);
        }
        if (isResolved != null) {
            parts.sql.append(" AND is_resolved = ?");
            parts.args.add(isResolved);
        } else {
            String normalizedStatus = normalizeStatus(status);
            if ("pending".equals(normalizedStatus) || "processing".equals(normalizedStatus)) {
                parts.sql.append(" AND is_resolved = FALSE");
            } else if ("resolved".equals(normalizedStatus)) {
                parts.sql.append(" AND is_resolved = TRUE");
            }
        }
        return parts;
    }

    private static String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toLowerCase();
    }

    private static EarlyWarningDTO mapWarning(ResultSet rs, int rowNum) throws SQLException {
        Boolean resolved = rs.getBoolean("is_resolved");
        String message = rs.getString("warning_message");
        String warningType = rs.getString("warning_type");
        return new EarlyWarningDTO(
                rs.getLong("id"),
                rs.getLong("student_id"),
                rs.getLong("course_id"),
                rs.getLong("teacher_id"),
                warningType,
                rs.getString("warning_level"),
                message,
                toInstant(rs.getTimestamp("trigger_date")),
                resolved,
                readNullableLong(rs, "resolved_by"),
                toInstant(rs.getTimestamp("resolved_date")),
                rs.getString("resolved_note"),
                rs.getString("assessment_type"),
                readNullableLong(rs, "related_assessment_id"),
                rs.getString("student_name"),
                rs.getString("course_name"),
                resolved ? "resolved" : "pending",
                message,
                suggestionByWarningType(warningType));
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

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static Long readNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static final class QueryParts {
        private final StringBuilder sql = new StringBuilder();
        private final List<Object> args = new ArrayList<>();
    }
}
