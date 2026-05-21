package com._202510007517.platform.assignment.repository;

import com._202510007517.platform.assignment.domain.AssignmentRecord;
import com._202510007517.platform.assignment.domain.AssignmentSubmissionRecord;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcAssignmentRepository implements AssignmentRepository {

    private static final RowMapper<AssignmentRecord> ASSIGNMENT_ROW_MAPPER = new AssignmentRowMapper();
    private static final RowMapper<AssignmentSubmissionRecord> ASSIGNMENT_SUBMISSION_ROW_MAPPER = new AssignmentSubmissionRowMapper();
    private static final String ASSIGNMENT_SELECT = """
            SELECT a.id, a.title, a.description, a.course_id, a.due_date, a.publish_date, a.is_active,
                   a.teacher_id, a.max_score,
                   COALESCE((SELECT COUNT(*) FROM assignment_submissions s WHERE s.assignment_id = a.id), 0) AS submission_count,
                   COALESCE((SELECT COUNT(*) FROM assignment_submissions s WHERE s.assignment_id = a.id AND s.graded = TRUE), 0) AS graded_count,
                   a.status, a.total_students
            FROM assignments a
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcAssignmentRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<AssignmentRecord> findById(Long assignmentId) {
        List<AssignmentRecord> rows = jdbcTemplate.query(ASSIGNMENT_SELECT + """
                WHERE a.id = :assignmentId
                """, new MapSqlParameterSource("assignmentId", assignmentId), ASSIGNMENT_ROW_MAPPER);
        return rows.stream().findFirst();
    }

    @Override
    public List<AssignmentRecord> findByCourseId(Long courseId) {
        return jdbcTemplate.query(ASSIGNMENT_SELECT + """
                WHERE a.course_id = :courseId
                ORDER BY a.id DESC
                """, new MapSqlParameterSource("courseId", courseId), ASSIGNMENT_ROW_MAPPER);
    }

    @Override
    public List<AssignmentRecord> findByTeacherId(Long teacherId) {
        return jdbcTemplate.query(ASSIGNMENT_SELECT + """
                WHERE a.teacher_id = :teacherId
                ORDER BY a.id DESC
                """, new MapSqlParameterSource("teacherId", teacherId), ASSIGNMENT_ROW_MAPPER);
    }

    @Override
    public List<AssignmentRecord> findByStudentId(Long studentId) {
        return jdbcTemplate.query(ASSIGNMENT_SELECT + """
                JOIN assignment_classes ac ON ac.assignment_id = a.id
                JOIN class_students cs ON cs.class_id = ac.class_id
                WHERE cs.student_id = :studentId
                ORDER BY a.id DESC
                """, new MapSqlParameterSource("studentId", studentId), ASSIGNMENT_ROW_MAPPER);
    }

    @Override
    public List<AssignmentRecord> findByClassIds(List<Long> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query(ASSIGNMENT_SELECT + """
                JOIN assignment_classes ac ON ac.assignment_id = a.id
                WHERE ac.class_id IN (:classIds)
                ORDER BY a.id DESC
                """, new MapSqlParameterSource("classIds", classIds), ASSIGNMENT_ROW_MAPPER);
    }

    @Override
    public List<AssignmentSubmissionRecord> findSubmissionsByAssignmentId(Long assignmentId) {
        return jdbcTemplate.query("""
                SELECT id, assignment_id, student_id, content, submission_date, graded, is_late,
                       late_penalty, score, teacher_comment
                FROM assignment_submissions
                WHERE assignment_id = :assignmentId
                ORDER BY submission_date DESC, id DESC
                """, new MapSqlParameterSource("assignmentId", assignmentId), ASSIGNMENT_SUBMISSION_ROW_MAPPER);
    }

    @Override
    public Optional<AssignmentSubmissionRecord> findSubmissionByAssignmentAndStudent(Long assignmentId, Long studentId) {
        List<AssignmentSubmissionRecord> rows = jdbcTemplate.query("""
                SELECT id, assignment_id, student_id, content, submission_date, graded, is_late,
                       late_penalty, score, teacher_comment
                FROM assignment_submissions
                WHERE assignment_id = :assignmentId
                  AND student_id = :studentId
                ORDER BY id DESC
                LIMIT 1
                """, new MapSqlParameterSource()
                .addValue("assignmentId", assignmentId)
                .addValue("studentId", studentId), ASSIGNMENT_SUBMISSION_ROW_MAPPER);
        return rows.stream().findFirst();
    }

    @Override
    public List<AssignmentSubmissionRecord> findSubmissionsByStudentId(Long studentId) {
        return jdbcTemplate.query("""
                SELECT id, assignment_id, student_id, content, submission_date, graded, is_late,
                       late_penalty, score, teacher_comment
                FROM assignment_submissions
                WHERE student_id = :studentId
                ORDER BY submission_date DESC, id DESC
                """, new MapSqlParameterSource("studentId", studentId), ASSIGNMENT_SUBMISSION_ROW_MAPPER);
    }

    @Override
    public List<AssignmentSubmissionRecord> findGradedSubmissionsByStudentId(Long studentId) {
        return jdbcTemplate.query("""
                SELECT id, assignment_id, student_id, content, submission_date, graded, is_late,
                       late_penalty, score, teacher_comment
                FROM assignment_submissions
                WHERE student_id = :studentId
                  AND graded = TRUE
                  AND score IS NOT NULL
                ORDER BY submission_date DESC, id DESC
                """, new MapSqlParameterSource("studentId", studentId), ASSIGNMENT_SUBMISSION_ROW_MAPPER);
    }

    @Override
    public Optional<AssignmentSubmissionRecord> findSubmissionById(Long submissionId) {
        List<AssignmentSubmissionRecord> rows = jdbcTemplate.query("""
                SELECT id, assignment_id, student_id, content, submission_date, graded, is_late,
                       late_penalty, score, teacher_comment
                FROM assignment_submissions
                WHERE id = :submissionId
                ORDER BY id DESC
                """, new MapSqlParameterSource("submissionId", submissionId), ASSIGNMENT_SUBMISSION_ROW_MAPPER);
        return rows.stream().findFirst();
    }

    @Override
    public AssignmentSubmissionRecord insertSubmission(AssignmentSubmissionRecord submission) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                INSERT INTO assignment_submissions
                    (assignment_id, student_id, content, submission_date, graded, is_late,
                     late_penalty, score, teacher_comment)
                VALUES
                    (:assignmentId, :studentId, :content, :submissionDate, :graded, :isLate,
                     :latePenalty, :score, :teacherComment)
                """, submissionParams(submission), keyHolder, new String[]{"id"});
        if (keyHolder.getKey() != null) {
            submission.setId(keyHolder.getKey().longValue());
        }
        return submission;
    }

    @Override
    public void updateSubmission(AssignmentSubmissionRecord submission) {
        jdbcTemplate.update("""
                UPDATE assignment_submissions
                SET content = :content,
                    submission_date = :submissionDate,
                    graded = :graded,
                    is_late = :isLate,
                    late_penalty = :latePenalty,
                    score = :score,
                    teacher_comment = :teacherComment
                WHERE id = :id
                """, submissionParams(submission).addValue("id", submission.getId()));
    }

    @Override
    public void updateAssignment(AssignmentRecord assignment) {
        jdbcTemplate.update("""
                UPDATE assignments
                SET submission_count = :submissionCount,
                    graded_count = :gradedCount,
                    status = :status
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", assignment.getId())
                .addValue("submissionCount", assignment.getSubmissionCount())
                .addValue("gradedCount", assignment.getGradedCount())
                .addValue("status", assignment.getStatus()));
    }

    @Override
    public AssignmentRecord insertAssignment(AssignmentRecord assignment) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                INSERT INTO assignments
                    (title, description, course_id, due_date, publish_date, is_active, teacher_id,
                     max_score, submission_count, graded_count, status, total_students)
                VALUES
                    (:title, :description, :courseId, :dueDate, :publishDate, :isActive, :teacherId,
                     :maxScore, :submissionCount, :gradedCount, :status, :totalStudents)
                """, assignmentParams(assignment), keyHolder, new String[]{"id"});
        if (keyHolder.getKey() != null) {
            assignment.setId(keyHolder.getKey().longValue());
        }
        return assignment;
    }

    @Override
    public void updateAssignmentDetails(AssignmentRecord assignment) {
        jdbcTemplate.update("""
                UPDATE assignments
                SET title = :title,
                    description = :description,
                    course_id = :courseId,
                    due_date = :dueDate,
                    publish_date = :publishDate,
                    is_active = :isActive,
                    teacher_id = :teacherId,
                    max_score = :maxScore,
                    total_students = :totalStudents
                WHERE id = :id
                """, assignmentParams(assignment));
    }

    @Override
    public void replaceAssignmentClasses(Long assignmentId, List<Long> classIds) {
        jdbcTemplate.update("""
                DELETE FROM assignment_classes
                WHERE assignment_id = :assignmentId
                """, new MapSqlParameterSource("assignmentId", assignmentId));
        if (classIds == null || classIds.isEmpty()) {
            return;
        }
        List<Long> distinctClassIds = new LinkedHashSet<>(classIds).stream().toList();
        jdbcTemplate.batchUpdate("""
                INSERT INTO assignment_classes (assignment_id, class_id)
                VALUES (:assignmentId, :classId)
                """, distinctClassIds.stream()
                .map(classId -> new MapSqlParameterSource()
                        .addValue("assignmentId", assignmentId)
                        .addValue("classId", classId))
                .toArray(MapSqlParameterSource[]::new));
    }

    @Override
    public List<Long> findKnowledgePointIdsByAssignmentId(Long assignmentId) {
        return jdbcTemplate.query("""
                SELECT knowledge_point_id
                FROM assignment_knowledge_points
                WHERE assignment_id = :assignmentId
                ORDER BY knowledge_point_id ASC
                """, new MapSqlParameterSource("assignmentId", assignmentId),
                (rs, rowNum) -> rs.getLong("knowledge_point_id"));
    }

    @Override
    public void replaceAssignmentKnowledgePoints(Long assignmentId, List<Long> knowledgePointIds) {
        jdbcTemplate.update("""
                DELETE FROM assignment_knowledge_points
                WHERE assignment_id = :assignmentId
                """, new MapSqlParameterSource("assignmentId", assignmentId));
        if (knowledgePointIds == null || knowledgePointIds.isEmpty()) {
            return;
        }
        List<Long> distinctKnowledgePointIds = new LinkedHashSet<>(knowledgePointIds).stream().toList();
        jdbcTemplate.batchUpdate("""
                INSERT INTO assignment_knowledge_points (assignment_id, knowledge_point_id)
                VALUES (:assignmentId, :knowledgePointId)
                """, distinctKnowledgePointIds.stream()
                .map(knowledgePointId -> new MapSqlParameterSource()
                        .addValue("assignmentId", assignmentId)
                        .addValue("knowledgePointId", knowledgePointId))
                .toArray(MapSqlParameterSource[]::new));
    }

    @Override
    public void deleteAssignmentCascade(Long assignmentId) {
        MapSqlParameterSource params = new MapSqlParameterSource("assignmentId", assignmentId);
        jdbcTemplate.update("""
                DELETE FROM assignment_knowledge_points
                WHERE assignment_id = :assignmentId
                """, params);
        jdbcTemplate.update("""
                DELETE FROM assignment_submissions
                WHERE assignment_id = :assignmentId
                """, params);
        jdbcTemplate.update("""
                DELETE FROM assignment_classes
                WHERE assignment_id = :assignmentId
                """, params);
        jdbcTemplate.update("""
                DELETE FROM assignments
                WHERE id = :assignmentId
                """, params);
    }

    private static MapSqlParameterSource submissionParams(AssignmentSubmissionRecord submission) {
        return new MapSqlParameterSource()
                .addValue("assignmentId", submission.getAssignmentId())
                .addValue("studentId", submission.getStudentId())
                .addValue("content", submission.getContent())
                .addValue("submissionDate", submission.getSubmissionDate())
                .addValue("graded", submission.getGraded())
                .addValue("isLate", submission.getIsLate())
                .addValue("latePenalty", submission.getLatePenalty())
                .addValue("score", submission.getScore())
                .addValue("teacherComment", submission.getTeacherComment());
    }

    private static MapSqlParameterSource assignmentParams(AssignmentRecord assignment) {
        return new MapSqlParameterSource()
                .addValue("id", assignment.getId())
                .addValue("title", assignment.getTitle())
                .addValue("description", assignment.getDescription())
                .addValue("courseId", assignment.getCourseId())
                .addValue("dueDate", assignment.getDueDate())
                .addValue("publishDate", assignment.getPublishDate())
                .addValue("isActive", assignment.getIsActive())
                .addValue("teacherId", assignment.getTeacherId())
                .addValue("maxScore", assignment.getMaxScore())
                .addValue("submissionCount", assignment.getSubmissionCount())
                .addValue("gradedCount", assignment.getGradedCount())
                .addValue("status", assignment.getStatus())
                .addValue("totalStudents", assignment.getTotalStudents());
    }

    private static class AssignmentRowMapper implements RowMapper<AssignmentRecord> {
        @Override
        public AssignmentRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            AssignmentRecord record = new AssignmentRecord();
            record.setId(rs.getLong("id"));
            record.setTitle(rs.getString("title"));
            record.setDescription(rs.getString("description"));
            record.setCourseId((Long) rs.getObject("course_id"));
            record.setDueDate(rs.getString("due_date"));
            record.setPublishDate(rs.getString("publish_date"));
            record.setIsActive((Boolean) rs.getObject("is_active"));
            record.setTeacherId((Long) rs.getObject("teacher_id"));
            record.setMaxScore(toInteger(rs.getObject("max_score")));
            record.setSubmissionCount(toInteger(rs.getObject("submission_count")));
            record.setGradedCount(toInteger(rs.getObject("graded_count")));
            record.setStatus(rs.getString("status"));
            record.setTotalStudents(toInteger(rs.getObject("total_students")));
            return record;
        }
    }

    private static class AssignmentSubmissionRowMapper implements RowMapper<AssignmentSubmissionRecord> {
        @Override
        public AssignmentSubmissionRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            AssignmentSubmissionRecord record = new AssignmentSubmissionRecord();
            record.setId(rs.getLong("id"));
            record.setAssignmentId(rs.getLong("assignment_id"));
            record.setStudentId((Long) rs.getObject("student_id"));
            record.setContent(rs.getString("content"));
            record.setSubmissionDate(rs.getString("submission_date"));
            record.setGraded((Boolean) rs.getObject("graded"));
            record.setIsLate((Boolean) rs.getObject("is_late"));
            record.setLatePenalty(toInteger(rs.getObject("late_penalty")));
            record.setScore(toInteger(rs.getObject("score")));
            record.setTeacherComment(rs.getString("teacher_comment"));
            return record;
        }
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(value.toString());
    }
}
