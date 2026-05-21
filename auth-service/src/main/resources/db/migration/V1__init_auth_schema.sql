CREATE TABLE IF NOT EXISTS auth_credentials (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_auth_credentials_user_id (user_id),
    UNIQUE KEY uk_auth_credentials_username (username)
);
