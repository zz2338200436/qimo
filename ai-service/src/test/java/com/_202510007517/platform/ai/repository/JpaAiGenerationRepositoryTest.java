package com._202510007517.platform.ai.repository;

import com._202510007517.platform.ai.AiServiceApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = AiServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class JpaAiGenerationRepositoryTest {

    private static final String DATABASE_NAME = "ai-jpa-history-" + UUID.randomUUID();

    @Autowired
    private AiGenerationRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
        registry.add("spring.datasource.url", () ->
                "jdbc:h2:mem:" + DATABASE_NAME + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @BeforeEach
    void cleanHistory() {
        jdbcTemplate.update("DELETE FROM ai_generations");
    }

    @Test
    void savesAiGenerationHistoryThroughJpaRepository() {
        assertThat(repository).isInstanceOf(JpaAiGenerationRepository.class);

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
