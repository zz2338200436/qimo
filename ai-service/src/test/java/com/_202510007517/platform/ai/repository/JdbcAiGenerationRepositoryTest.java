package com._202510007517.platform.ai.repository;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcAiGenerationRepositoryTest {

    private JdbcTemplate jdbcTemplate;
    private JdbcAiGenerationRepository repository;

    @BeforeEach
    void setUp() {
        String databaseName = "ai-history-" + UUID.randomUUID();
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + databaseName + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                "");
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcAiGenerationRepository(jdbcTemplate);
    }

    @Test
    void savesAiGenerationHistory() {
        repository.save(new AiGenerationRecord(
                7L,
                "TEACHER",
                "generate-questions",
                "generate-questions",
                "{\"topic\":\"Java基础\"}",
                "{\"questions\":[]}",
                "local-mock-model",
                "SUCCESS",
                null,
                12L));

        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT user_id, user_role, prompt_key, request_type, model_name, status, error_message, latency_ms
                FROM ai_generations
                """);

        assertThat(row.get("user_id")).isEqualTo(7L);
        assertThat(row.get("user_role")).isEqualTo("TEACHER");
        assertThat(row.get("prompt_key")).isEqualTo("generate-questions");
        assertThat(row.get("request_type")).isEqualTo("generate-questions");
        assertThat(row.get("model_name")).isEqualTo("local-mock-model");
        assertThat(row.get("status")).isEqualTo("SUCCESS");
        assertThat(row.get("error_message")).isNull();
        assertThat(row.get("latency_ms")).isEqualTo(12L);
    }
}
