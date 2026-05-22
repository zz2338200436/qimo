package com._202510007517.platform.analysis.repository;

import com._202510007517.platform.analysis.AnalysisServiceApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = AnalysisServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class JpaAnalysisTriggerJobRepositoryTest {

    private static final String DATABASE_NAME = "analysis-trigger-job-jpa-" + UUID.randomUUID();

    @Autowired
    private AnalysisTriggerJobRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("eureka.client.register-with-eureka", () -> "false");
        registry.add("eureka.client.fetch-registry", () -> "false");
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
    }

    @Test
    void createsAndCompletesTriggerJobThroughJpaRepository() {
        assertThat(AopUtils.getTargetClass(repository).getSimpleName()).isEqualTo("JpaAnalysisTriggerJobRepository");

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

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM analysis_trigger_jobs WHERE id = ?",
                String.class,
                created.id())).isEqualTo("COMPLETED");
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
