package com._202510007517.platform.analysis.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

@Repository
public class JdbcAnalysisTriggerJobRepository implements AnalysisTriggerJobRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAnalysisTriggerJobRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AnalysisTriggerJob create(Long teacherId, String triggerType, Long classId, Long courseId, Long studentId) {
        jdbcTemplate.update("""
                        INSERT INTO analysis_trigger_jobs (
                            teacher_id, trigger_type, class_id, course_id, student_id, status
                        )
                        VALUES (?, ?, ?, ?, ?, 'RUNNING')
                        """,
                teacherId,
                triggerType,
                classId,
                courseId,
                studentId);
        Long id = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM analysis_trigger_jobs WHERE teacher_id = ?",
                Long.class,
                teacherId);
        return findById(id);
    }

    @Override
    public AnalysisTriggerJob complete(
            Long jobId,
            String status,
            int requestedCount,
            int warningCount,
            String message) {
        jdbcTemplate.update("""
                        UPDATE analysis_trigger_jobs
                        SET status = ?,
                            requested_count = ?,
                            warning_count = ?,
                            message = ?,
                            completed_at = CURRENT_TIMESTAMP(6)
                        WHERE id = ?
                        """,
                status,
                requestedCount,
                warningCount,
                message,
                jobId);
        return findById(jobId);
    }

    private AnalysisTriggerJob findById(Long id) {
        return jdbcTemplate.queryForObject("""
                        SELECT id, teacher_id, trigger_type, class_id, course_id, student_id, status,
                               requested_count, warning_count, message, created_at, completed_at
                        FROM analysis_trigger_jobs
                        WHERE id = ?
                        """,
                JdbcAnalysisTriggerJobRepository::mapJob,
                id);
    }

    private static AnalysisTriggerJob mapJob(ResultSet rs, int rowNum) throws SQLException {
        return new AnalysisTriggerJob(
                rs.getLong("id"),
                rs.getLong("teacher_id"),
                rs.getString("trigger_type"),
                readNullableLong(rs, "class_id"),
                readNullableLong(rs, "course_id"),
                readNullableLong(rs, "student_id"),
                rs.getString("status"),
                rs.getInt("requested_count"),
                rs.getInt("warning_count"),
                rs.getString("message"),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("completed_at")));
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static Long readNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
