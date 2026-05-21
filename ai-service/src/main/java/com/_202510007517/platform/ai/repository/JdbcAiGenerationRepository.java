package com._202510007517.platform.ai.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAiGenerationRepository implements AiGenerationRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAiGenerationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(AiGenerationRecord record) {
        jdbcTemplate.update("""
                INSERT INTO ai_generations (
                    user_id, user_role, prompt_key, request_type, request_payload, response_payload,
                    model_name, status, error_message, latency_ms, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6))
                """,
                record.userId(),
                record.userRole(),
                record.promptKey(),
                record.requestType(),
                record.requestPayload(),
                record.responsePayload(),
                record.modelName(),
                record.status(),
                record.errorMessage(),
                record.latencyMs());
    }
}
