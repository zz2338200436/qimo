package com._202510007517.platform.analysis.service;

import com._202510007517.platform.analysis.repository.JdbcAnalysisRepository;
import com._202510007517.platform.common.event.idempotency.IdempotentEventHandler;
import com._202510007517.platform.common.event.idempotency.JdbcProcessedEventRepository;
import com._202510007517.platform.common.event.outbox.JdbcOutboxEventRepository;
import com._202510007517.platform.events.EventAggregate;
import com._202510007517.platform.events.exam.ExamFinishedEvent;
import com._202510007517.platform.events.exam.ExamFinishedPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisEventIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private JdbcTemplate jdbcTemplate;
    private ExamFinishedAnalysisHandler handler;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:analysis-integration;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                "");
        jdbcTemplate = new JdbcTemplate(dataSource);
        recreateSchema();
        handler = new ExamFinishedAnalysisHandler(
                new IdempotentEventHandler(new JdbcProcessedEventRepository(jdbcTemplate)),
                new JdbcAnalysisRepository(jdbcTemplate),
                new JdbcOutboxEventRepository(jdbcTemplate),
                OBJECT_MAPPER);
    }

    @Test
    void examFinishedEventUpdatesAnalysisTablesAndQueuesWarningOnce() throws Exception {
        ExamFinishedEvent event = examFinishedEvent();

        boolean firstHandled = handler.handle(event);
        boolean duplicateHandled = handler.handle(event);

        assertThat(firstHandled).isTrue();
        assertThat(duplicateHandled).isFalse();
        assertThat(count("processed_event")).isEqualTo(1);
        assertThat(count("score_trends")).isEqualTo(1);
        assertThat(count("kp_mastery")).isEqualTo(1);
        assertThat(count("outbox_event")).isEqualTo(1);

        Map<String, Object> scoreTrend = jdbcTemplate.queryForMap("""
                SELECT student_id, course_id, class_id, source_type, source_id, submission_id,
                       score, max_score, score_rate
                FROM score_trends
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
                SELECT student_id, course_id, class_id, knowledge_point_id, mastery_score, evidence_count, last_event_id
                FROM kp_mastery
                """);
        assertThat(mastery.get("student_id")).isEqualTo(42L);
        assertThat(mastery.get("course_id")).isEqualTo(5001L);
        assertThat(mastery.get("class_id")).isEqualTo(6001L);
        assertThat(mastery.get("knowledge_point_id")).isEqualTo(0L);
        assertThat((BigDecimal) mastery.get("mastery_score")).isEqualByComparingTo(new BigDecimal("0.4500"));
        assertThat(mastery.get("evidence_count")).isEqualTo(1);
        assertThat(mastery.get("last_event_id")).isEqualTo("exam-finished-analysis-integration-low-score");

        Map<String, Object> outbox = jdbcTemplate.queryForMap("""
                SELECT event_id, aggregate_type, aggregate_id, event_type, binding_name, payload, status, retry_count
                FROM outbox_event
                """);
        assertThat(outbox.get("event_id")).isEqualTo("early-warning-exam-finished-analysis-integration-low-score");
        assertThat(outbox.get("aggregate_type")).isEqualTo("early_warning");
        assertThat((String) outbox.get("aggregate_id")).startsWith("warning-");
        assertThat(outbox.get("event_type")).isEqualTo("EarlyWarningRaisedEvent");
        assertThat(outbox.get("binding_name")).isEqualTo("early.warning.raised");
        assertThat(outbox.get("status")).isEqualTo(0);
        assertThat(outbox.get("retry_count")).isEqualTo(0);

        JsonNode payload = OBJECT_MAPPER.readTree((String) outbox.get("payload"));
        assertThat(payload.path("payload").path("studentId").asLong()).isEqualTo(42L);
        assertThat(payload.path("payload").path("courseId").asLong()).isEqualTo(5001L);
        assertThat(payload.path("payload").path("warningType").asText()).isEqualTo("LOW_SCORE");
        assertThat(payload.path("payload").path("level").asText()).isEqualTo("HIGH");
        assertThat(payload.path("payload").path("reason").asText()).contains("45%", "60%");
    }

    private long count(String table) {
        Long value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        return value == null ? 0L : value;
    }

    private static ExamFinishedEvent examFinishedEvent() {
        return new ExamFinishedEvent(
                "exam-finished-analysis-integration-low-score",
                Instant.parse("2026-05-19T12:00:00Z"),
                new EventAggregate("exam_submission", "9001"),
                new ExamFinishedPayload(
                        77L,
                        9001L,
                        42L,
                        5001L,
                        6001L,
                        45,
                        100,
                        Instant.parse("2026-05-19T11:59:00Z")));
    }

    private void recreateSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS outbox_event");
        jdbcTemplate.execute("DROP TABLE IF EXISTS processed_event");
        jdbcTemplate.execute("DROP TABLE IF EXISTS kp_mastery");
        jdbcTemplate.execute("DROP TABLE IF EXISTS score_trends");
        jdbcTemplate.execute("""
                CREATE TABLE score_trends (
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
                CREATE TABLE kp_mastery (
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
        jdbcTemplate.execute("""
                CREATE TABLE processed_event (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    event_id VARCHAR(150) NOT NULL,
                    event_type VARCHAR(100) NOT NULL,
                    consumer_name VARCHAR(100) NOT NULL,
                    processed_at TIMESTAMP(6) NOT NULL,
                    CONSTRAINT uk_processed_event_event_consumer UNIQUE (event_id, consumer_name)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE outbox_event (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    event_id VARCHAR(150) NOT NULL,
                    aggregate_type VARCHAR(100) NOT NULL,
                    aggregate_id VARCHAR(100) NOT NULL,
                    event_type VARCHAR(200) NOT NULL,
                    binding_name VARCHAR(100) NOT NULL,
                    payload CLOB NOT NULL,
                    headers CLOB NULL,
                    status TINYINT NOT NULL DEFAULT 0,
                    retry_count INT NOT NULL DEFAULT 0,
                    next_retry_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                    last_error VARCHAR(1000) NULL,
                    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                    published_at TIMESTAMP(6) NULL,
                    CONSTRAINT uk_outbox_event_event_id UNIQUE (event_id)
                )
                """);
    }
}
