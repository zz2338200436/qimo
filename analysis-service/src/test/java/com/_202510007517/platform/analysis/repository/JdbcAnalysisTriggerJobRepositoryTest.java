package com._202510007517.platform.analysis.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcAnalysisTriggerJobRepositoryTest {

    private JdbcTemplate jdbcTemplate;
    private JdbcAnalysisTriggerJobRepository repository;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:analysis-trigger-job-repository;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                "");
        jdbcTemplate = new JdbcTemplate(dataSource);
        recreateSchema();
        repository = new JdbcAnalysisTriggerJobRepository(jdbcTemplate);
    }

    @Test
    void createsAndCompletesTriggerJob() {
        AnalysisTriggerJob created = repository.create(7L, "CLASS_ANALYSIS", 1L, 2L, null);

        assertThat(created.id()).isPositive();
        assertThat(created)
                .extracting(
                        AnalysisTriggerJob::teacherId,
                        AnalysisTriggerJob::triggerType,
                        AnalysisTriggerJob::classId,
                        AnalysisTriggerJob::courseId,
                        AnalysisTriggerJob::studentId,
                        AnalysisTriggerJob::status,
                        AnalysisTriggerJob::requestedCount,
                        AnalysisTriggerJob::warningCount,
                        AnalysisTriggerJob::message)
                .containsExactly(7L, "CLASS_ANALYSIS", 1L, 2L, null, "RUNNING", 0, 0, null);
        assertThat(created.createdAt()).isNotNull();
        assertThat(created.completedAt()).isNull();

        AnalysisTriggerJob completed = repository.complete(
                created.id(),
                "COMPLETED",
                3,
                2,
                "班级学情分析完成");

        assertThat(completed.id()).isEqualTo(created.id());
        assertThat(completed.status()).isEqualTo("COMPLETED");
        assertThat(completed.requestedCount()).isEqualTo(3);
        assertThat(completed.warningCount()).isEqualTo(2);
        assertThat(completed.message()).isEqualTo("班级学情分析完成");
        assertThat(completed.completedAt()).isNotNull();
    }

    private void recreateSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS analysis_trigger_jobs");
        jdbcTemplate.execute("""
                CREATE TABLE analysis_trigger_jobs (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    teacher_id BIGINT NOT NULL,
                    trigger_type VARCHAR(50) NOT NULL,
                    class_id BIGINT NULL,
                    course_id BIGINT NULL,
                    student_id BIGINT NULL,
                    status VARCHAR(30) NOT NULL,
                    requested_count INT NOT NULL DEFAULT 0,
                    warning_count INT NOT NULL DEFAULT 0,
                    message VARCHAR(500) NULL,
                    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                    completed_at TIMESTAMP(6) NULL
                )
                """);
    }
}
