package com._202510007517.platform.analysis.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisBackfillScriptTest {

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:analysis-backfill;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                "");
        jdbcTemplate = new JdbcTemplate(dataSource);
        recreateSchemas();
        seedHistory();
    }

    @Test
    void backfillScriptPopulatesAnalysisProjectionsAndCanBeRerun() throws Exception {
        Path script = findRepositoryRoot()
                .resolve("deploy/scripts/backfill/backfill-analysis-history.sql");
        assertThat(Files.exists(script)).as("backfill script exists").isTrue();

        ScriptUtils.executeSqlScript(jdbcTemplate.getDataSource().getConnection(), new FileSystemResource(script));
        ScriptUtils.executeSqlScript(jdbcTemplate.getDataSource().getConnection(), new FileSystemResource(script));

        assertThat(count("sc_analysis.score_trends")).isEqualTo(1);
        assertThat(count("sc_analysis.kp_mastery")).isEqualTo(1);

        Map<String, Object> scoreTrend = jdbcTemplate.queryForMap("""
                SELECT student_id, course_id, class_id, source_type, source_id, submission_id,
                       score, max_score, score_rate
                FROM sc_analysis.score_trends
                """);
        assertThat(scoreTrend.get("student_id")).isEqualTo(42L);
        assertThat(scoreTrend.get("course_id")).isEqualTo(5001L);
        assertThat(scoreTrend.get("class_id")).isEqualTo(6001L);
        assertThat(scoreTrend.get("source_type")).isEqualTo("exam");
        assertThat(scoreTrend.get("source_id")).isEqualTo(77L);
        assertThat(scoreTrend.get("submission_id")).isEqualTo(9001L);
        assertThat(scoreTrend.get("score")).isEqualTo(45);
        assertThat(scoreTrend.get("max_score")).isEqualTo(100);
        assertThat((BigDecimal) scoreTrend.get("score_rate")).isEqualByComparingTo(new BigDecimal("0.4500"));

        Map<String, Object> mastery = jdbcTemplate.queryForMap("""
                SELECT student_id, course_id, class_id, knowledge_point_id, mastery_score, evidence_count,
                       last_source_type, last_source_id, last_event_id
                FROM sc_analysis.kp_mastery
                """);
        assertThat(mastery.get("student_id")).isEqualTo(42L);
        assertThat(mastery.get("course_id")).isEqualTo(5001L);
        assertThat(mastery.get("class_id")).isEqualTo(6001L);
        assertThat(mastery.get("knowledge_point_id")).isEqualTo(0L);
        assertThat((BigDecimal) mastery.get("mastery_score")).isEqualByComparingTo(new BigDecimal("0.5250"));
        assertThat(mastery.get("evidence_count")).isEqualTo(2);
        assertThat(mastery.get("last_source_type")).isEqualTo("exam");
        assertThat(mastery.get("last_source_id")).isEqualTo(77L);
        assertThat(mastery.get("last_event_id")).isEqualTo("backfill-exam-9001");
    }

    private long count(String tableName) {
        Long value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
        return value == null ? 0L : value;
    }

    private void recreateSchemas() {
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS sc_analysis CASCADE");
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS sc_assignment CASCADE");
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS sc_exam CASCADE");
        jdbcTemplate.execute("CREATE SCHEMA sc_exam");
        jdbcTemplate.execute("CREATE SCHEMA sc_assignment");
        jdbcTemplate.execute("CREATE SCHEMA sc_analysis");
        jdbcTemplate.execute("""
                CREATE TABLE sc_exam.exams (
                    id BIGINT PRIMARY KEY,
                    course_id BIGINT NOT NULL,
                    total_score INT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE sc_exam.exam_classes (
                    exam_id BIGINT NOT NULL,
                    class_id BIGINT NOT NULL,
                    PRIMARY KEY (exam_id, class_id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE sc_exam.exam_submissions (
                    id BIGINT PRIMARY KEY,
                    exam_id BIGINT NOT NULL,
                    student_id BIGINT NOT NULL,
                    submission_date TIMESTAMP(6) NOT NULL,
                    graded TINYINT NOT NULL DEFAULT 0,
                    score INT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE sc_assignment.assignments (
                    id BIGINT PRIMARY KEY,
                    course_id BIGINT NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE sc_assignment.assignment_classes (
                    assignment_id BIGINT NOT NULL,
                    class_id BIGINT NOT NULL,
                    PRIMARY KEY (assignment_id, class_id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE sc_assignment.assignment_submissions (
                    id BIGINT PRIMARY KEY,
                    assignment_id BIGINT NOT NULL,
                    student_id BIGINT NOT NULL,
                    submission_date VARCHAR(30),
                    created_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6),
                    updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE sc_analysis.score_trends (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    student_id BIGINT NOT NULL,
                    course_id BIGINT NOT NULL,
                    class_id BIGINT NULL,
                    source_type VARCHAR(50) NOT NULL,
                    source_id BIGINT NOT NULL,
                    submission_id BIGINT NOT NULL,
                    score INT NULL,
                    max_score INT NULL,
                    score_rate DECIMAL(6,4) NULL,
                    occurred_at TIMESTAMP(6) NOT NULL,
                    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                    CONSTRAINT uk_score_trends_source_submission UNIQUE (source_type, submission_id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE sc_analysis.kp_mastery (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    student_id BIGINT NOT NULL,
                    course_id BIGINT NOT NULL,
                    class_id BIGINT NULL,
                    knowledge_point_id BIGINT NOT NULL DEFAULT 0,
                    mastery_score DECIMAL(6,4) NOT NULL,
                    evidence_count INT NOT NULL DEFAULT 0,
                    last_source_type VARCHAR(50) NULL,
                    last_source_id BIGINT NULL,
                    last_event_id VARCHAR(150) NULL,
                    updated_at TIMESTAMP(6) NOT NULL,
                    CONSTRAINT uk_kp_mastery_student_course_point UNIQUE (student_id, course_id, knowledge_point_id)
                )
                """);
    }

    private void seedHistory() {
        jdbcTemplate.update("INSERT INTO sc_exam.exams (id, course_id, total_score) VALUES (77, 5001, 100)");
        jdbcTemplate.update("INSERT INTO sc_exam.exam_classes (exam_id, class_id) VALUES (77, 6001)");
        jdbcTemplate.update("""
                INSERT INTO sc_exam.exam_submissions
                    (id, exam_id, student_id, submission_date, graded, score)
                VALUES
                    (9001, 77, 42, TIMESTAMP '2026-05-19 12:00:00', 1, 45),
                    (9002, 77, 43, TIMESTAMP '2026-05-19 12:05:00', 0, NULL)
                """);
        jdbcTemplate.update("INSERT INTO sc_assignment.assignments (id, course_id) VALUES (2001, 5001)");
        jdbcTemplate.update("INSERT INTO sc_assignment.assignment_classes (assignment_id, class_id) VALUES (2001, 6001)");
        jdbcTemplate.update("""
                INSERT INTO sc_assignment.assignment_submissions
                    (id, assignment_id, student_id, submission_date, created_at, updated_at)
                VALUES
                    (3001, 2001, 42, '2026-05-19 11:30:00', TIMESTAMP '2026-05-19 11:30:00', TIMESTAMP '2026-05-19 11:30:00')
                """);
    }

    private static Path findRepositoryRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("deploy"))
                    && Files.isDirectory(current.resolve("analysis-service"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate repository root from " + System.getProperty("user.dir"));
    }
}
