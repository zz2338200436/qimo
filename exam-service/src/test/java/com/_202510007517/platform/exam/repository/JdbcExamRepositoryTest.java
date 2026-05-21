package com._202510007517.platform.exam.repository;

import com._202510007517.platform.exam.domain.ExamQuestionRecord;
import com._202510007517.platform.exam.domain.ExamSubmissionRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcExamRepositoryTest {

    private JdbcTemplate jdbcTemplate;
    private JdbcExamRepository repository;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:exam-repository;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                "");
        jdbcTemplate = new JdbcTemplate(dataSource);
        recreateSchema();
        repository = new JdbcExamRepository(new NamedParameterJdbcTemplate(jdbcTemplate));
    }

    @Test
    void replacesAndListsExamQuestionsInDisplayOrder() {
        repository.replaceExamQuestions(9001L, List.of(
                question("essay", "Explain CAP", null, "Consistency, availability, partition tolerance", 20, 99L, 2),
                question("single_choice", "Pick A", "[\"A\",\"B\",\"C\",\"D\"]", "A", 10, 100L, 1)
        ));

        List<ExamQuestionRecord> questions = repository.findQuestionsByExamId(9001L);

        assertThat(questions)
                .extracting(row -> row.questionType() + ":" + row.questionText() + ":" + row.score() + ":" + row.knowledgePointId())
                .containsExactly(
                        "single_choice:Pick A:10:100",
                        "essay:Explain CAP:20:99");
        assertThat(questions.get(0).optionsJson()).isEqualTo("[\"A\",\"B\",\"C\",\"D\"]");
        assertThat(questions.get(0).sortOrder()).isEqualTo(1);
    }

    @Test
    void deletingExamAlsoRemovesOwnedQuestions() {
        jdbcTemplate.update("INSERT INTO exams (id, title, course_id) VALUES (9001, 'Midterm', 2)");
        repository.replaceExamQuestions(9001L, List.of(
                question("single_choice", "Pick A", "[\"A\",\"B\"]", "A", 10, 100L, 1)
        ));

        repository.delete(9001L);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exam_questions WHERE exam_id = 9001",
                Integer.class);
        assertThat(count).isZero();
    }

    @Test
    void updateSubmissionGradeMarksSubmissionGradedWithScore() {
        jdbcTemplate.update("""
                INSERT INTO exam_submissions
                    (id, exam_id, student_id, content, submission_date, time_taken, graded, score, teacher_comment)
                VALUES
                    (9101, 9001, 42, '{}', CURRENT_TIMESTAMP, 30, 0, NULL, NULL)
                """);

        int updated = repository.updateSubmissionGrade(9101L, 15, "自动阅卷");

        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT graded, score, teacher_comment
                FROM exam_submissions
                WHERE id = 9101
                """);
        assertThat(updated).isEqualTo(1);
        assertThat(toInt(row.get("graded"))).isEqualTo(1);
        assertThat(toInt(row.get("score"))).isEqualTo(15);
        assertThat(row.get("teacher_comment")).isEqualTo("自动阅卷");
    }

    @Test
    void findsTeacherOwnedSubmissionsWithFiltersAndPagination() {
        jdbcTemplate.update("INSERT INTO exams (id, title, course_id, teacher_id) VALUES (9001, 'Midterm', 2, 7)");
        jdbcTemplate.update("INSERT INTO exams (id, title, course_id, teacher_id) VALUES (9002, 'Other Exam', 2, 8)");
        jdbcTemplate.update("""
                INSERT INTO exam_submissions
                    (id, exam_id, student_id, content, submission_date, time_taken, graded, score, teacher_comment)
                VALUES
                    (9101, 9001, 42, '{}', CURRENT_TIMESTAMP, 30, 0, NULL, NULL),
                    (9102, 9002, 42, '{}', CURRENT_TIMESTAMP, 28, 0, NULL, NULL),
                    (9103, 9001, 43, '{}', CURRENT_TIMESTAMP, 26, 1, 90, 'graded')
                """);

        List<ExamSubmissionRecord> rows = repository.findSubmissionsByTeacherId(
                7L,
                9001L,
                42L,
                false,
                "id",
                "DESC",
                0,
                10);
        int count = repository.countSubmissionsByTeacherId(7L, 9001L, 42L, false);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getId()).isEqualTo(9101L);
        assertThat(rows.get(0).getExamTitle()).isEqualTo("Midterm");
        assertThat(count).isEqualTo(1);
    }

    @Test
    void updatesAndDeletesSubmissionRecord() {
        jdbcTemplate.update("INSERT INTO exams (id, title, course_id, teacher_id) VALUES (9001, 'Midterm', 2, 7)");
        jdbcTemplate.update("""
                INSERT INTO exam_submissions
                    (id, exam_id, student_id, content, submission_date, time_taken, graded, score, teacher_comment)
                VALUES
                    (9101, 9001, 42, '{\"q1\":\"A\"}', CURRENT_TIMESTAMP, 30, 0, NULL, NULL)
                """);
        ExamSubmissionRecord update = new ExamSubmissionRecord();
        update.setExamId(9001L);
        update.setStudentId(42L);
        update.setContent("{\"q1\":\"B\"}");
        update.setTimeTaken(41);
        update.setGraded(true);
        update.setScore(88);
        update.setTeacherComment("manual adjustment");

        int updated = repository.updateSubmission(9101L, update);
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT content, time_taken, graded, score, teacher_comment
                FROM exam_submissions
                WHERE id = 9101
                """);
        int deleted = repository.deleteSubmission(9101L);
        Integer remaining = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM exam_submissions WHERE id = 9101", Integer.class);

        assertThat(updated).isEqualTo(1);
        assertThat(row.get("content")).isEqualTo("{\"q1\":\"B\"}");
        assertThat(toInt(row.get("time_taken"))).isEqualTo(41);
        assertThat(toInt(row.get("graded"))).isEqualTo(1);
        assertThat(toInt(row.get("score"))).isEqualTo(88);
        assertThat(row.get("teacher_comment")).isEqualTo("manual adjustment");
        assertThat(deleted).isEqualTo(1);
        assertThat(remaining).isZero();
    }

    private static ExamQuestionRecord question(
            String questionType,
            String questionText,
            String optionsJson,
            String correctAnswer,
            int score,
            Long knowledgePointId,
            int sortOrder) {
        return new ExamQuestionRecord(
                null,
                9001L,
                questionText,
                questionType,
                optionsJson,
                correctAnswer,
                score,
                knowledgePointId,
                sortOrder);
    }

    private void recreateSchema() {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS sc_user");
        jdbcTemplate.execute("DROP TABLE IF EXISTS sc_user.users");
        jdbcTemplate.execute("DROP TABLE IF EXISTS exam_questions");
        jdbcTemplate.execute("DROP TABLE IF EXISTS exam_submissions");
        jdbcTemplate.execute("DROP TABLE IF EXISTS exam_classes");
        jdbcTemplate.execute("DROP TABLE IF EXISTS exam_knowledge_points");
        jdbcTemplate.execute("DROP TABLE IF EXISTS exams");
        jdbcTemplate.execute("""
                CREATE ALIAS IF NOT EXISTS DATE_FORMAT AS '
                String dateFormat(java.sql.Timestamp value, String pattern) {
                    return value == null ? null : new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(value);
                }'
                """);
        jdbcTemplate.execute("""
                CREATE TABLE sc_user.users (
                    id BIGINT PRIMARY KEY,
                    name VARCHAR(255) NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE exams (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    title VARCHAR(255) NOT NULL,
                    course_id BIGINT NOT NULL,
                    teacher_id BIGINT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE exam_submissions (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    exam_id BIGINT NOT NULL,
                    student_id BIGINT NOT NULL,
                    content TEXT NULL,
                    submission_date TIMESTAMP NULL,
                    time_taken INT NULL,
                    graded TINYINT(1) NOT NULL DEFAULT 0,
                    score INT NULL,
                    teacher_comment TEXT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE exam_classes (
                    exam_id BIGINT NOT NULL,
                    class_id BIGINT NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE exam_knowledge_points (
                    exam_id BIGINT NOT NULL,
                    knowledge_point_id BIGINT NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE exam_questions (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    exam_id BIGINT NOT NULL,
                    question_text TEXT NOT NULL,
                    question_type VARCHAR(50) NOT NULL,
                    options_json TEXT NULL,
                    correct_answer TEXT NULL,
                    score INT NOT NULL DEFAULT 0,
                    knowledge_point_id BIGINT NULL,
                    sort_order INT NOT NULL DEFAULT 0
                )
                """);
    }

    private static int toInt(Object value) {
        if (value instanceof Boolean bool) {
            return bool ? 1 : 0;
        }
        return ((Number) value).intValue();
    }
}
