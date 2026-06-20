package com._202510007517.platform.exam.repository;

import com._202510007517.platform.exam.ExamServiceApplication;
import com._202510007517.platform.exam.api.dto.StudentScoreDTO;
import com._202510007517.platform.exam.domain.ExamQuestionRecord;
import com._202510007517.platform.exam.domain.ExamRecord;
import com._202510007517.platform.exam.domain.ExamSubmissionRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = ExamServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class JpaExamRepositoryTest {

    private static final String DATABASE_NAME = "exam-jpa-" + UUID.randomUUID();

    @Autowired
    private ExamRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
        registry.add("spring.cloud.stream.enabled", () -> "false");
        registry.add("spring.datasource.url", () ->
                "jdbc:h2:mem:" + DATABASE_NAME + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @BeforeEach
    void setUp() {
        recreateSchema();
        seedData();
    }

    @Test
    void findsExamsThroughJpaRepository() {
        assertThat(repository).isInstanceOf(JpaExamRepository.class);

        List<ExamRecord> classExams = repository.findByClassIds(List.of(501L, 503L));
        List<ExamRecord> teacherExams = repository.findByTeacherId(7L);
        Optional<ExamRecord> exam = repository.findExam(9001L);
        Optional<ExamSubmissionRecord> submission = repository.findSubmission(9001L, 42L);
        int submissionCount = repository.countSubmissionsByExamId(9001L);
        List<ExamSubmissionRecord> examSubmissions = repository.findSubmissionsByExamId(9001L);

        assertThat(exam).isPresent();
        assertThat(exam.orElseThrow().getTitle()).isEqualTo("Midterm");
        assertThat(classExams).extracting(ExamRecord::getId).containsExactly(9001L, 9002L);
        assertThat(teacherExams).extracting(ExamRecord::getId).containsExactly(9001L, 9002L);
        assertThat(repository.isExamVisibleToClasses(9001L, List.of(501L))).isTrue();
        assertThat(repository.isExamVisibleToClasses(9001L, List.of(503L))).isFalse();
        assertThat(submission).isPresent();
        assertThat(submission.orElseThrow().getStudentName()).isNull();
        assertThat(submissionCount).isEqualTo(2);
        assertThat(examSubmissions).extracting(ExamSubmissionRecord::getId).containsExactly(9102L, 9101L);
    }

    @Test
    void mutatesExamRelationsThroughJpaRepository() {
        assertThat(repository).isInstanceOf(JpaExamRepository.class);

        ExamRecord exam = new ExamRecord();
        exam.setTitle("Final");
        exam.setDescription("Final assessment");
        exam.setCourseId(302L);
        exam.setTeacherId(7L);
        exam.setStartTime("2026-06-01 09:00:00");
        exam.setEndTime("2026-06-01 11:00:00");
        exam.setPublishDate("2026-05-20 08:00:00");
        exam.setDuration(120);
        exam.setTotalScore(100);
        exam.setActive(true);
        exam.setOnline(false);
        exam.setLocation("Room 202");

        ExamRecord inserted = repository.insert(exam);
        inserted.setTitle("Final Updated");
        inserted.setDescription("Final assessment updated");
        inserted.setLocation("Room 203");
        inserted.setTotalScore(120);
        repository.update(inserted);
        repository.replaceExamClasses(inserted.getId(), Set.of(502L, 504L));
        repository.replaceExamKnowledgePoints(inserted.getId(), List.of(6002L, 6001L, 6002L));
        repository.replaceExamQuestions(inserted.getId(), List.of(
                question(inserted.getId(), "essay", "Explain CAP", null, "Consistency, availability, partition tolerance", 20, 99L, 2),
                question(inserted.getId(), "single_choice", "Pick A", "[\"A\",\"B\",\"C\",\"D\"]", "A", 10, 100L, 1)
        ));

        ExamRecord loaded = repository.findExam(inserted.getId()).orElseThrow();
        List<ExamQuestionRecord> questions = repository.findQuestionsByExamId(inserted.getId());

        assertThat(inserted.getId()).isNotNull();
        assertThat(loaded.getTitle()).isEqualTo("Final Updated");
        assertThat(loaded.getLocation()).isEqualTo("Room 203");
        assertThat(repository.findByClassIds(List.of(504L))).extracting(ExamRecord::getId).contains(inserted.getId());
        assertThat(repository.findKnowledgePointIdsByExamId(inserted.getId())).containsExactly(6001L, 6002L);
        assertThat(questions)
                .extracting(row -> row.questionType() + ":" + row.questionText() + ":" + row.score() + ":" + row.knowledgePointId())
                .containsExactly(
                        "single_choice:Pick A:10:100",
                        "essay:Explain CAP:20:99");

        repository.delete(inserted.getId());

        assertThat(repository.findExam(inserted.getId())).isEmpty();
        assertThat(repository.findQuestionsByExamId(inserted.getId())).isEmpty();
        assertThat(repository.findKnowledgePointIdsByExamId(inserted.getId())).isEmpty();
    }

    @Test
    void mutatesSubmissionsAndTeacherViewsThroughJpaRepository() {
        assertThat(repository).isInstanceOf(JpaExamRepository.class);

        ExamSubmissionRecord first = repository.upsertSubmission(9002L, 44L, 35, "{\"q1\":\"A\"}");
        ExamSubmissionRecord updatedByUpsert = repository.upsertSubmission(9002L, 44L, 41, "{\"q1\":\"B\"}");

        ExamSubmissionRecord edited = new ExamSubmissionRecord();
        edited.setExamId(9002L);
        edited.setStudentId(44L);
        edited.setContent("{\"q1\":\"C\"}");
        edited.setTimeTaken(46);
        edited.setGraded(true);
        edited.setScore(88);
        edited.setTeacherComment("manual adjustment");
        int updated = repository.updateSubmission(updatedByUpsert.getId(), edited);
        int graded = repository.updateSubmissionGrade(updatedByUpsert.getId(), 91, "graded");

        Optional<ExamSubmissionRecord> byId = repository.findSubmissionById(updatedByUpsert.getId());
        List<ExamSubmissionRecord> teacherRows = repository.findSubmissionsByTeacherId(7L, 9002L, 44L, true, "id", "DESC", 0, 10);
        int teacherCount = repository.countSubmissionsByTeacherId(7L, 9002L, 44L, true);
        List<StudentScoreDTO> studentScores = repository.findStudentExamScores(44L);
        int deleted = repository.deleteSubmission(updatedByUpsert.getId());

        assertThat(first.getId()).isEqualTo(updatedByUpsert.getId());
        assertThat(updated).isEqualTo(1);
        assertThat(graded).isEqualTo(1);
        assertThat(byId).isPresent();
        assertThat(byId.orElseThrow().getScore()).isEqualTo(91);
        assertThat(byId.orElseThrow().getTeacherComment()).isEqualTo("graded");
        assertThat(teacherRows).extracting(ExamSubmissionRecord::getId).containsExactly(updatedByUpsert.getId());
        assertThat(teacherCount).isEqualTo(1);
        assertThat(studentScores).extracting(StudentScoreDTO::getRelatedId).contains(9002L);
        assertThat(deleted).isEqualTo(1);
        assertThat(repository.findSubmissionById(updatedByUpsert.getId())).isEmpty();
    }

    private void recreateSchema() {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS sc_user");
        jdbcTemplate.execute("DROP TABLE IF EXISTS sc_user.users");
        jdbcTemplate.execute("DROP TABLE IF EXISTS exam_questions");
        jdbcTemplate.execute("DROP TABLE IF EXISTS exam_knowledge_points");
        jdbcTemplate.execute("DROP TABLE IF EXISTS exam_classes");
        jdbcTemplate.execute("DROP TABLE IF EXISTS exam_submissions");
        jdbcTemplate.execute("DROP TABLE IF EXISTS exams");
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
                    description TEXT NULL,
                    course_id BIGINT NOT NULL,
                    teacher_id BIGINT NULL,
                    start_time TIMESTAMP NULL,
                    end_time TIMESTAMP NULL,
                    publish_date TIMESTAMP NULL,
                    duration INT NULL,
                    total_score INT NULL,
                    is_active BOOLEAN DEFAULT TRUE,
                    is_online BOOLEAN DEFAULT TRUE,
                    location VARCHAR(255) NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE exam_submissions (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    exam_id BIGINT NOT NULL,
                    student_id BIGINT NOT NULL,
                    answers_json VARCHAR(2000) NULL,
                    content TEXT NULL,
                    submission_date TIMESTAMP NULL,
                    time_taken INT NULL,
                    graded BOOLEAN DEFAULT FALSE,
                    score INT NULL,
                    teacher_comment TEXT NULL,
                    CONSTRAINT uq_exam_submission UNIQUE (exam_id, student_id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE exam_classes (
                    exam_id BIGINT NOT NULL,
                    class_id BIGINT NOT NULL,
                    PRIMARY KEY (exam_id, class_id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE exam_knowledge_points (
                    exam_id BIGINT NOT NULL,
                    knowledge_point_id BIGINT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (exam_id, knowledge_point_id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE exam_questions (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    exam_id BIGINT NOT NULL,
                    question_text TEXT NOT NULL,
                    question_type VARCHAR(50) NOT NULL,
                    options_json VARCHAR(2000) NULL,
                    correct_answer TEXT NULL,
                    score INT NOT NULL DEFAULT 0,
                    knowledge_point_id BIGINT NULL,
                    sort_order INT NOT NULL DEFAULT 0
                )
                """);
    }

    private void seedData() {
        jdbcTemplate.update("INSERT INTO sc_user.users (id, name) VALUES (?, ?)", 42L, "Alice");
        jdbcTemplate.update("INSERT INTO sc_user.users (id, name) VALUES (?, ?)", 43L, "Bob");
        jdbcTemplate.update("INSERT INTO sc_user.users (id, name) VALUES (?, ?)", 44L, "Carol");
        jdbcTemplate.update("""
                INSERT INTO exams (id, title, description, course_id, teacher_id, start_time, end_time, publish_date,
                                   duration, total_score, is_active, is_online, location)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                9001L, "Midterm", "Core distributed systems", 301L, 7L,
                java.sql.Timestamp.valueOf("2026-05-20 09:00:00"),
                java.sql.Timestamp.valueOf("2026-05-20 11:00:00"),
                java.sql.Timestamp.valueOf("2026-05-10 08:00:00"),
                120, 100, true, true, "Online");
        jdbcTemplate.update("""
                INSERT INTO exams (id, title, description, course_id, teacher_id, start_time, end_time, publish_date,
                                   duration, total_score, is_active, is_online, location)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                9002L, "Quiz", "Short quiz", 301L, 7L,
                java.sql.Timestamp.valueOf("2026-05-18 09:00:00"),
                java.sql.Timestamp.valueOf("2026-05-18 10:00:00"),
                java.sql.Timestamp.valueOf("2026-05-09 08:00:00"),
                60, 50, true, true, "Online");
        jdbcTemplate.update("INSERT INTO exam_classes (exam_id, class_id) VALUES (?, ?)", 9001L, 501L);
        jdbcTemplate.update("INSERT INTO exam_classes (exam_id, class_id) VALUES (?, ?)", 9002L, 503L);
        jdbcTemplate.update("INSERT INTO exam_knowledge_points (exam_id, knowledge_point_id) VALUES (?, ?)", 9001L, 6001L);
        jdbcTemplate.update("INSERT INTO exam_knowledge_points (exam_id, knowledge_point_id) VALUES (?, ?)", 9001L, 6003L);
        jdbcTemplate.update("""
                INSERT INTO exam_questions (id, exam_id, question_text, question_type, options_json, correct_answer,
                                            score, knowledge_point_id, sort_order)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                9201L, 9001L, "Pick A", "single_choice", "[\"A\",\"B\",\"C\",\"D\"]", "A", 10, 100L, 1);
        jdbcTemplate.update("""
                INSERT INTO exam_questions (id, exam_id, question_text, question_type, options_json, correct_answer,
                                            score, knowledge_point_id, sort_order)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                9202L, 9001L, "Explain CAP", "essay", null, "Consistency, availability, partition tolerance", 20, 99L, 2);
        jdbcTemplate.update("""
                INSERT INTO exam_submissions (id, exam_id, student_id, content, submission_date, time_taken, graded, score, teacher_comment)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                9101L, 9001L, 42L, "{\"q1\":\"A\"}", java.sql.Timestamp.valueOf("2026-05-20 10:00:00"), 30, false, null, null);
        jdbcTemplate.update("""
                INSERT INTO exam_submissions (id, exam_id, student_id, content, submission_date, time_taken, graded, score, teacher_comment)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                9102L, 9001L, 43L, "{\"q1\":\"B\"}", java.sql.Timestamp.valueOf("2026-05-20 10:05:00"), 28, true, 87, "graded");
    }

    private static ExamQuestionRecord question(
            Long examId,
            String questionType,
            String questionText,
            String optionsJson,
            String correctAnswer,
            int score,
            Long knowledgePointId,
            int sortOrder) {
        return new ExamQuestionRecord(
                null,
                examId,
                questionText,
                questionType,
                optionsJson,
                correctAnswer,
                score,
                knowledgePointId,
                sortOrder);
    }
}
