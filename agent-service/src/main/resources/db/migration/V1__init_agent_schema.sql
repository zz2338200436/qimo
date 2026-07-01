CREATE TABLE IF NOT EXISTS agent_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    user_role VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    KEY idx_agent_sessions_user_time (user_id, created_at)
);

CREATE TABLE IF NOT EXISTS agent_actions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    intent VARCHAR(128) NOT NULL,
    status VARCHAR(64) NOT NULL,
    risk_level VARCHAR(64) NOT NULL,
    preview_json TEXT,
    request_json TEXT,
    result_json TEXT,
    idempotency_key VARCHAR(128) NOT NULL,
    error_message VARCHAR(512),
    expires_at TIMESTAMP(6) NULL,
    confirmed_at TIMESTAMP(6) NULL,
    executed_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_agent_actions_idempotency (idempotency_key),
    KEY idx_agent_actions_session_time (session_id, created_at),
    KEY idx_agent_actions_status_time (status, created_at)
);

CREATE TABLE IF NOT EXISTS agent_audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    action_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    user_role VARCHAR(64) NOT NULL,
    operation VARCHAR(128) NOT NULL,
    target_service VARCHAR(128) NOT NULL,
    target_resource VARCHAR(256),
    trace_id VARCHAR(128),
    success BOOLEAN NOT NULL,
    error_message VARCHAR(512),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_agent_audit_action (action_id),
    KEY idx_agent_audit_user_time (user_id, created_at)
);
