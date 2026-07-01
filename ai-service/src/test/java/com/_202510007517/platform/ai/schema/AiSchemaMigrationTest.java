package com._202510007517.platform.ai.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

class AiSchemaMigrationTest {

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        String databaseName = "ai-schema-" + UUID.randomUUID();
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + databaseName + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                "");
        jdbcTemplate = new JdbcTemplate(dataSource);
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    @Test
    void createsAiGenerationHistoryTables() {
        assertThat(tableExists("ai_prompts")).isTrue();
        assertThat(tableExists("ai_generations")).isTrue();

        jdbcTemplate.update("""
                INSERT INTO ai_prompts (
                    prompt_key, name, template, model_name, enabled, created_at, updated_at
                ) VALUES (
                    'question-generation', '题目生成', '请根据 {{topic}} 生成题目', 'mock-model', TRUE, NOW(6), NOW(6)
                )
                """);

        jdbcTemplate.update("""
                INSERT INTO ai_generations (
                    user_id, user_role, prompt_key, request_type, request_payload, response_payload,
                    model_name, status, latency_ms, created_at
                ) VALUES (
                    7, 'TEACHER', 'question-generation', 'generate-questions',
                    '{"topic":"Java"}', '{"questions":[]}', 'mock-model', 'SUCCESS', 120, NOW(6)
                )
                """);

        assertThat(countRows("ai_prompts")).isEqualTo(1);
        assertThat(countRows("ai_generations")).isEqualTo(1);
    }

    @Test
    void keepsPromptKeysUnique() {
        jdbcTemplate.update("""
                INSERT INTO ai_prompts (
                    prompt_key, name, template, model_name, enabled, created_at, updated_at
                ) VALUES (
                    'learning-suggestion', '学习建议', '请生成学习建议', 'mock-model', TRUE, NOW(6), NOW(6)
                )
                """);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO ai_prompts (
                    prompt_key, name, template, model_name, enabled, created_at, updated_at
                ) VALUES (
                    'learning-suggestion', '重复学习建议', '重复模板', 'mock-model', TRUE, NOW(6), NOW(6)
                )
                """))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void seedsEnoughMediumJavaQuestionsForFiveQuestionGeneration() {
        Integer knowledgePointCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM knowledge_points
                WHERE point_name = 'Java基础'
                  AND difficulty = '中等'
                """, Integer.class);
        Integer questionCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM questions q
                JOIN knowledge_points kp ON kp.id = q.knowledge_point_id
                WHERE kp.point_name = 'Java基础'
                  AND COALESCE(q.difficulty, kp.difficulty) = '中等'
                """, Integer.class);

        assertThat(knowledgePointCount).isNotNull();
        assertThat(knowledgePointCount).isGreaterThan(0);
        assertThat(questionCount).isNotNull();
        assertThat(questionCount).isGreaterThanOrEqualTo(5);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_NAME = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    private long countRows(String tableName) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
        return count == null ? 0L : count;
    }
}
