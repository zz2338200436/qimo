CREATE TABLE IF NOT EXISTS ai_prompts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    prompt_key VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    template TEXT NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_ai_prompts_prompt_key (prompt_key),
    KEY idx_ai_prompts_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_generations (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_role VARCHAR(50) NULL,
    prompt_key VARCHAR(100) NOT NULL,
    request_type VARCHAR(100) NOT NULL,
    request_payload JSON NOT NULL,
    response_payload JSON NULL,
    model_name VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    error_message VARCHAR(1000) NULL,
    latency_ms BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_ai_generations_user_created (user_id, created_at),
    KEY idx_ai_generations_prompt_created (prompt_key, created_at),
    KEY idx_ai_generations_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
