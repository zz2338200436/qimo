package com._202510007517.platform.exam.repository;

import com._202510007517.platform.exam.api.dto.StudentScoreDTO;
import com._202510007517.platform.exam.domain.ExamRecord;
import com._202510007517.platform.exam.domain.ExamQuestionRecord;
import com._202510007517.platform.exam.domain.ExamSubmissionRecord;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public class JdbcExamRepository implements ExamRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcExamRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ExamRecord> findByClassIds(List<Long> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT DISTINCT e.id,
                       e.title,
                       e.description,
                       e.course_id AS courseId,
                       e.teacher_id AS teacherId,
                       DATE_FORMAT(e.start_time, '%Y-%m-%d %H:%i:%s') AS startTime,
                       DATE_FORMAT(e.end_time, '%Y-%m-%d %H:%i:%s') AS endTime,
                       DATE_FORMAT(e.publish_date, '%Y-%m-%d %H:%i:%s') AS publishDate,
                       e.is_active AS active,
                       e.is_online AS online,
                       e.location,
                       e.duration,
                       e.total_score AS totalScore,
                       e.start_time AS sortStartTime
                FROM exams e
                JOIN exam_classes ec ON e.id = ec.exam_id
                WHERE ec.class_id IN (:classIds)
                ORDER BY sortStartTime DESC, e.id DESC
                """, Map.of("classIds", new LinkedHashSet<>(classIds)), (rs, rowNum) -> mapExam(rs));
    }

    @Override
    public List<ExamRecord> findByTeacherId(Long teacherId) {
        if (teacherId == null) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT e.id,
                       e.title,
                       e.description,
                       e.course_id AS courseId,
                       e.teacher_id AS teacherId,
                       DATE_FORMAT(e.start_time, '%Y-%m-%d %H:%i:%s') AS startTime,
                       DATE_FORMAT(e.end_time, '%Y-%m-%d %H:%i:%s') AS endTime,
                       DATE_FORMAT(e.publish_date, '%Y-%m-%d %H:%i:%s') AS publishDate,
                       e.is_active AS active,
                       e.is_online AS online,
                       e.location,
                       e.duration,
                       e.total_score AS totalScore,
                       e.start_time AS sortStartTime
                FROM exams e
                WHERE e.teacher_id = :teacherId
                ORDER BY sortStartTime DESC, e.id DESC
                """, Map.of("teacherId", teacherId), (rs, rowNum) -> mapExam(rs));
    }

    @Override
    public boolean isExamVisibleToClasses(Long examId, List<Long> classIds) {
        if (examId == null || classIds == null || classIds.isEmpty()) {
            return false;
        }
        Integer visible = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM exam_classes
                WHERE exam_id = :examId
                  AND class_id IN (:classIds)
                """, Map.of("examId", examId, "classIds", new LinkedHashSet<>(classIds)), Integer.class);
        return visible != null && visible > 0;
    }

    @Override
    public Optional<ExamRecord> findExam(Long examId) {
        List<ExamRecord> rows = jdbcTemplate.query("""
                SELECT e.id,
                       e.title,
                       e.description,
                       e.course_id AS courseId,
                       e.teacher_id AS teacherId,
                       DATE_FORMAT(e.start_time, '%Y-%m-%d %H:%i:%s') AS startTime,
                       DATE_FORMAT(e.end_time, '%Y-%m-%d %H:%i:%s') AS endTime,
                       DATE_FORMAT(e.publish_date, '%Y-%m-%d %H:%i:%s') AS publishDate,
                       e.is_active AS active,
                       e.is_online AS online,
                       e.location,
                       e.duration,
                       e.total_score AS totalScore
                FROM exams e
                WHERE e.id = :examId
                """, Map.of("examId", examId), (rs, rowNum) -> mapExam(rs));
        return rows.stream().findFirst();
    }

    @Override
    public Optional<ExamSubmissionRecord> findSubmission(Long examId, Long studentId) {
        List<ExamSubmissionRecord> rows = jdbcTemplate.query("""
                SELECT es.id,
                       es.exam_id AS examId,
                       es.student_id AS studentId,
                       es.content,
                       DATE_FORMAT(es.submission_date, '%Y-%m-%d %H:%i:%s') AS submissionDate,
                       es.time_taken AS timeTaken,
                       es.graded,
                       es.score,
                       es.teacher_comment AS teacherComment
                FROM exam_submissions es
                WHERE es.exam_id = :examId
                  AND es.student_id = :studentId
                """, Map.of("examId", examId, "studentId", studentId), (rs, rowNum) -> mapSubmission(rs));
        return rows.stream().findFirst();
    }

    @Override
    public ExamSubmissionRecord upsertSubmission(Long examId, Long studentId, Integer timeTaken, String contentJson) {
        Optional<ExamSubmissionRecord> existing = findSubmission(examId, studentId);
        if (existing.isPresent()) {
            jdbcTemplate.update("""
                    UPDATE exam_submissions
                    SET content = :content,
                        submission_date = NOW(),
                        time_taken = :timeTaken,
                        graded = 0,
                        score = NULL,
                        teacher_comment = NULL
                    WHERE id = :id
                    """, new MapSqlParameterSource()
                    .addValue("id", existing.get().getId())
                    .addValue("content", contentJson)
                    .addValue("timeTaken", timeTaken));
            return findSubmission(examId, studentId).orElseThrow();
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                INSERT INTO exam_submissions (exam_id, student_id, content, submission_date, time_taken, graded)
                VALUES (:examId, :studentId, :content, NOW(), :timeTaken, 0)
                """, new MapSqlParameterSource()
                .addValue("examId", examId)
                .addValue("studentId", studentId)
                .addValue("content", contentJson)
                .addValue("timeTaken", timeTaken), keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("考试提交写入成功但未返回主键");
        }
        return findSubmission(examId, studentId).orElseThrow();
    }

    @Override
    public ExamRecord insert(ExamRecord exam) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                INSERT INTO exams (
                    title,
                    description,
                    course_id,
                    teacher_id,
                    start_time,
                    end_time,
                    publish_date,
                    duration,
                    total_score,
                    is_active,
                    is_online,
                    location
                ) VALUES (
                    :title,
                    :description,
                    :courseId,
                    :teacherId,
                    :startTime,
                    :endTime,
                    :publishDate,
                    :duration,
                    :totalScore,
                    :active,
                    :online,
                    :location
                )
                """, new MapSqlParameterSource()
                .addValue("title", exam.getTitle())
                .addValue("description", exam.getDescription())
                .addValue("courseId", exam.getCourseId())
                .addValue("teacherId", exam.getTeacherId())
                .addValue("startTime", exam.getStartTime())
                .addValue("endTime", exam.getEndTime())
                .addValue("publishDate", exam.getPublishDate())
                .addValue("duration", exam.getDuration())
                .addValue("totalScore", exam.getTotalScore())
                .addValue("active", exam.getActive())
                .addValue("online", exam.getOnline())
                .addValue("location", exam.getLocation()), keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("考试写入成功但未返回主键");
        }
        return findExam(key.longValue()).orElseThrow();
    }

    @Override
    public void update(ExamRecord exam) {
        jdbcTemplate.update("""
                UPDATE exams
                SET title = :title,
                    description = :description,
                    course_id = :courseId,
                    teacher_id = :teacherId,
                    start_time = :startTime,
                    end_time = :endTime,
                    publish_date = :publishDate,
                    duration = :duration,
                    total_score = :totalScore,
                    is_active = :active,
                    is_online = :online,
                    location = :location
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", exam.getId())
                .addValue("title", exam.getTitle())
                .addValue("description", exam.getDescription())
                .addValue("courseId", exam.getCourseId())
                .addValue("teacherId", exam.getTeacherId())
                .addValue("startTime", exam.getStartTime())
                .addValue("endTime", exam.getEndTime())
                .addValue("publishDate", exam.getPublishDate())
                .addValue("duration", exam.getDuration())
                .addValue("totalScore", exam.getTotalScore())
                .addValue("active", exam.getActive())
                .addValue("online", exam.getOnline())
                .addValue("location", exam.getLocation()));
    }

    @Override
    public void delete(Long examId) {
        jdbcTemplate.update("DELETE FROM exam_submissions WHERE exam_id = :examId", Map.of("examId", examId));
        jdbcTemplate.update("DELETE FROM exam_classes WHERE exam_id = :examId", Map.of("examId", examId));
        jdbcTemplate.update("DELETE FROM exam_knowledge_points WHERE exam_id = :examId", Map.of("examId", examId));
        jdbcTemplate.update("DELETE FROM exam_questions WHERE exam_id = :examId", Map.of("examId", examId));
        jdbcTemplate.update("DELETE FROM exams WHERE id = :examId", Map.of("examId", examId));
    }

    @Override
    public void replaceExamClasses(Long examId, Set<Long> classIds) {
        jdbcTemplate.update("DELETE FROM exam_classes WHERE exam_id = :examId", Map.of("examId", examId));
        if (classIds == null || classIds.isEmpty()) {
            return;
        }
        for (Long classId : classIds) {
            jdbcTemplate.update("""
                    INSERT INTO exam_classes (exam_id, class_id)
                    VALUES (:examId, :classId)
                    """, Map.of("examId", examId, "classId", classId));
        }
    }

    @Override
    public List<Long> findKnowledgePointIdsByExamId(Long examId) {
        return jdbcTemplate.query("""
                SELECT knowledge_point_id
                FROM exam_knowledge_points
                WHERE exam_id = :examId
                ORDER BY knowledge_point_id ASC
                """, Map.of("examId", examId), (rs, rowNum) -> rs.getLong("knowledge_point_id"));
    }

    @Override
    public void replaceExamKnowledgePoints(Long examId, List<Long> knowledgePointIds) {
        jdbcTemplate.update("DELETE FROM exam_knowledge_points WHERE exam_id = :examId", Map.of("examId", examId));
        if (knowledgePointIds == null || knowledgePointIds.isEmpty()) {
            return;
        }
        for (Long knowledgePointId : new LinkedHashSet<>(knowledgePointIds)) {
            jdbcTemplate.update("""
                    INSERT INTO exam_knowledge_points (exam_id, knowledge_point_id)
                    VALUES (:examId, :knowledgePointId)
                    """, Map.of("examId", examId, "knowledgePointId", knowledgePointId));
        }
    }

    @Override
    public List<ExamQuestionRecord> findQuestionsByExamId(Long examId) {
        return jdbcTemplate.query("""
                SELECT id,
                       exam_id AS examId,
                       question_text AS questionText,
                       question_type AS questionType,
                       options_json AS optionsJson,
                       correct_answer AS correctAnswer,
                       score,
                       knowledge_point_id AS knowledgePointId,
                       sort_order AS sortOrder
                FROM exam_questions
                WHERE exam_id = :examId
                ORDER BY sort_order ASC, id ASC
                """, Map.of("examId", examId), (rs, rowNum) -> new ExamQuestionRecord(
                rs.getLong("id"),
                rs.getLong("examId"),
                rs.getString("questionText"),
                rs.getString("questionType"),
                rs.getString("optionsJson"),
                rs.getString("correctAnswer"),
                asInteger(rs.getObject("score")),
                (Long) rs.getObject("knowledgePointId"),
                asInteger(rs.getObject("sortOrder"))));
    }

    @Override
    public void replaceExamQuestions(Long examId, List<ExamQuestionRecord> questions) {
        jdbcTemplate.update("DELETE FROM exam_questions WHERE exam_id = :examId", Map.of("examId", examId));
        if (questions == null || questions.isEmpty()) {
            return;
        }
        for (ExamQuestionRecord question : questions) {
            jdbcTemplate.update("""
                    INSERT INTO exam_questions (
                        exam_id,
                        question_text,
                        question_type,
                        options_json,
                        correct_answer,
                        score,
                        knowledge_point_id,
                        sort_order
                    ) VALUES (
                        :examId,
                        :questionText,
                        :questionType,
                        :optionsJson,
                        :correctAnswer,
                        :score,
                        :knowledgePointId,
                        :sortOrder
                    )
                    """, new MapSqlParameterSource()
                    .addValue("examId", examId)
                    .addValue("questionText", question.questionText())
                    .addValue("questionType", question.questionType())
                    .addValue("optionsJson", question.optionsJson())
                    .addValue("correctAnswer", question.correctAnswer())
                    .addValue("score", question.score() == null ? 0 : question.score())
                    .addValue("knowledgePointId", question.knowledgePointId())
                    .addValue("sortOrder", question.sortOrder() == null ? 0 : question.sortOrder()));
        }
    }

    @Override
    public int countSubmissionsByExamId(Long examId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM exam_submissions
                WHERE exam_id = :examId
                """, Map.of("examId", examId), Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    public List<ExamSubmissionRecord> findSubmissionsByExamId(Long examId) {
        return jdbcTemplate.query("""
                SELECT es.id,
                       es.exam_id AS examId,
                       es.student_id AS studentId,
                       u.name AS studentName,
                       e.title AS examTitle,
                       es.content,
                       DATE_FORMAT(es.submission_date, '%Y-%m-%d %H:%i:%s') AS submissionDate,
                       es.time_taken AS timeTaken,
                       es.graded,
                       es.score,
                       es.teacher_comment AS teacherComment
                FROM exam_submissions es
                JOIN exams e ON es.exam_id = e.id
                LEFT JOIN sc_user.users u ON es.student_id = u.id
                WHERE es.exam_id = :examId
                ORDER BY es.submission_date DESC, es.id DESC
                """, Map.of("examId", examId), (rs, rowNum) -> mapSubmission(rs));
    }

    @Override
    public List<ExamSubmissionRecord> findSubmissionsByTeacherId(Long teacherId,
                                                                 Long examId,
                                                                 Long studentId,
                                                                 Boolean graded,
                                                                 String sortBy,
                                                                 String order,
                                                                 int offset,
                                                                 int limit) {
        if (teacherId == null) {
            return List.of();
        }
        MapSqlParameterSource params = teacherSubmissionFilterParams(teacherId, examId, studentId, graded)
                .addValue("offset", Math.max(offset, 0))
                .addValue("limit", Math.max(limit, 1));
        return jdbcTemplate.query("""
                SELECT es.id,
                       es.exam_id AS examId,
                       es.student_id AS studentId,
                       u.name AS studentName,
                       e.title AS examTitle,
                       es.content,
                       DATE_FORMAT(es.submission_date, '%Y-%m-%d %H:%i:%s') AS submissionDate,
                       es.time_taken AS timeTaken,
                       es.graded,
                       es.score,
                       es.teacher_comment AS teacherComment
                FROM exam_submissions es
                JOIN exams e ON es.exam_id = e.id
                LEFT JOIN sc_user.users u ON es.student_id = u.id
                WHERE e.teacher_id = :teacherId
                """ + teacherSubmissionOptionalFilters(examId, studentId, graded)
                + " ORDER BY " + submissionSortColumn(sortBy) + " " + safeSortOrder(order) + ", es.id DESC LIMIT :limit OFFSET :offset",
                params,
                (rs, rowNum) -> mapSubmission(rs));
    }

    @Override
    public int countSubmissionsByTeacherId(Long teacherId, Long examId, Long studentId, Boolean graded) {
        if (teacherId == null) {
            return 0;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM exam_submissions es
                JOIN exams e ON es.exam_id = e.id
                WHERE e.teacher_id = :teacherId
                """ + teacherSubmissionOptionalFilters(examId, studentId, graded),
                teacherSubmissionFilterParams(teacherId, examId, studentId, graded),
                Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    public Optional<ExamSubmissionRecord> findSubmissionById(Long submissionId) {
        List<ExamSubmissionRecord> rows = jdbcTemplate.query("""
                SELECT es.id,
                       es.exam_id AS examId,
                       es.student_id AS studentId,
                       u.name AS studentName,
                       e.title AS examTitle,
                       es.content,
                       DATE_FORMAT(es.submission_date, '%Y-%m-%d %H:%i:%s') AS submissionDate,
                       es.time_taken AS timeTaken,
                       es.graded,
                       es.score,
                       es.teacher_comment AS teacherComment
                FROM exam_submissions es
                JOIN exams e ON es.exam_id = e.id
                LEFT JOIN sc_user.users u ON es.student_id = u.id
                WHERE es.id = :submissionId
                """, Map.of("submissionId", submissionId), (rs, rowNum) -> mapSubmission(rs));
        return rows.stream().findFirst();
    }

    @Override
    public int updateSubmission(Long submissionId, ExamSubmissionRecord submission) {
        return jdbcTemplate.update("""
                UPDATE exam_submissions
                SET exam_id = :examId,
                    student_id = :studentId,
                    content = :content,
                    time_taken = :timeTaken,
                    graded = :graded,
                    score = :score,
                    teacher_comment = :teacherComment
                WHERE id = :submissionId
                """, new MapSqlParameterSource()
                .addValue("submissionId", submissionId)
                .addValue("examId", submission.getExamId())
                .addValue("studentId", submission.getStudentId())
                .addValue("content", submission.getContent())
                .addValue("timeTaken", submission.getTimeTaken())
                .addValue("graded", Boolean.TRUE.equals(submission.getGraded()) ? 1 : 0)
                .addValue("score", submission.getScore())
                .addValue("teacherComment", submission.getTeacherComment()));
    }

    @Override
    public int updateSubmissionGrade(Long submissionId, Integer score, String teacherComment) {
        return jdbcTemplate.update("""
                UPDATE exam_submissions
                SET graded = 1,
                    score = :score,
                    teacher_comment = :teacherComment
                WHERE id = :submissionId
                """, new MapSqlParameterSource()
                .addValue("submissionId", submissionId)
                .addValue("score", score)
                .addValue("teacherComment", teacherComment));
    }

    @Override
    public int deleteSubmission(Long submissionId) {
        return jdbcTemplate.update(
                "DELETE FROM exam_submissions WHERE id = :submissionId",
                Map.of("submissionId", submissionId));
    }

    @Override
    public List<StudentScoreDTO> findStudentExamScores(Long studentId) {
        return jdbcTemplate.query("""
                SELECT es.id,
                       'exam' AS type,
                       e.id AS relatedId,
                       e.title,
                       e.course_id AS courseId,
                       NULL AS courseName,
                       DATE_FORMAT(es.submission_date, '%Y-%m-%d %H:%i:%s') AS completedAt,
                       es.score,
                       100 AS totalScore,
                    NULL AS rankValue
                FROM exam_submissions es
                JOIN exams e ON es.exam_id = e.id
                WHERE es.student_id = :studentId
                  AND es.graded = 1
                ORDER BY es.submission_date DESC, es.id DESC
                """, Map.of("studentId", studentId), (rs, rowNum) -> {
            StudentScoreDTO dto = new StudentScoreDTO();
            dto.setType(rs.getString("type"));
            dto.setRelatedId(rs.getLong("relatedId"));
            dto.setTitle(rs.getString("title"));
            dto.setCourseId((Long) rs.getObject("courseId"));
            dto.setCourseName(rs.getString("courseName"));
            dto.setCompletedAt(rs.getString("completedAt"));
            dto.setScore(asInteger(rs.getObject("score")));
            dto.setTotalScore(asInteger(rs.getObject("totalScore")));
            dto.setRank(asInteger(rs.getObject("rankValue")));
            return dto;
        });
    }

    private static Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(value.toString());
    }

    private static ExamRecord mapExam(ResultSet rs) throws SQLException {
        ExamRecord record = new ExamRecord();
        record.setId(rs.getLong("id"));
        record.setTitle(rs.getString("title"));
        record.setDescription(rs.getString("description"));
        record.setCourseId(rs.getLong("courseId"));
        record.setTeacherId((Long) rs.getObject("teacherId"));
        record.setStartTime(rs.getString("startTime"));
        record.setEndTime(rs.getString("endTime"));
        record.setPublishDate(rs.getString("publishDate"));
        record.setActive(readBoolean(rs, "active"));
        record.setOnline(readBoolean(rs, "online"));
        record.setLocation(rs.getString("location"));
        record.setDuration((Integer) rs.getObject("duration"));
        record.setTotalScore(asInteger(rs.getObject("totalScore")));
        return record;
    }

    private static ExamSubmissionRecord mapSubmission(ResultSet rs) throws SQLException {
        ExamSubmissionRecord record = new ExamSubmissionRecord();
        record.setId(rs.getLong("id"));
        record.setExamId(rs.getLong("examId"));
        record.setStudentId(rs.getLong("studentId"));
        record.setStudentName(readOptionalString(rs, "studentName"));
        record.setExamTitle(readOptionalString(rs, "examTitle"));
        record.setContent(rs.getString("content"));
        record.setSubmissionDate(rs.getString("submissionDate"));
        record.setTimeTaken((Integer) rs.getObject("timeTaken"));
        record.setGraded(readBoolean(rs, "graded"));
        record.setScore((Integer) rs.getObject("score"));
        record.setTeacherComment(rs.getString("teacherComment"));
        return record;
    }

    private static Boolean readBoolean(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private static String readOptionalString(ResultSet rs, String column) throws SQLException {
        java.sql.ResultSetMetaData metaData = rs.getMetaData();
        for (int index = 1; index <= metaData.getColumnCount(); index++) {
            if (column.equalsIgnoreCase(metaData.getColumnLabel(index))) {
                return rs.getString(column);
            }
        }
        return null;
    }

    private static MapSqlParameterSource teacherSubmissionFilterParams(Long teacherId,
                                                                       Long examId,
                                                                       Long studentId,
                                                                       Boolean graded) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("teacherId", teacherId);
        if (examId != null) {
            params.addValue("examId", examId);
        }
        if (studentId != null) {
            params.addValue("studentId", studentId);
        }
        if (graded != null) {
            params.addValue("graded", Boolean.TRUE.equals(graded) ? 1 : 0);
        }
        return params;
    }

    private static String teacherSubmissionOptionalFilters(Long examId, Long studentId, Boolean graded) {
        StringBuilder filters = new StringBuilder();
        if (examId != null) {
            filters.append(" AND es.exam_id = :examId");
        }
        if (studentId != null) {
            filters.append(" AND es.student_id = :studentId");
        }
        if (graded != null) {
            filters.append(" AND es.graded = :graded");
        }
        return filters.toString();
    }

    private static String submissionSortColumn(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "es.id";
        }
        return switch (sortBy) {
            case "id" -> "es.id";
            case "examId" -> "es.exam_id";
            case "studentId" -> "es.student_id";
            case "submissionDate" -> "es.submission_date";
            case "timeTaken" -> "es.time_taken";
            case "graded" -> "es.graded";
            case "score" -> "es.score";
            case "examTitle" -> "e.title";
            default -> "es.id";
        };
    }

    private static String safeSortOrder(String order) {
        return "ASC".equalsIgnoreCase(order) ? "ASC" : "DESC";
    }
}
