CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    name VARCHAR(64) NULL,
    email VARCHAR(128) NULL,
    phone VARCHAR(32) NULL,
    avatar VARCHAR(255) NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_users_username (username)
);

CREATE TABLE IF NOT EXISTS roles (
    id BIGINT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    UNIQUE KEY uk_roles_name (name)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

INSERT IGNORE INTO roles (id, name) VALUES
    (1, 'ADMIN'),
    (2, 'TEACHER'),
    (3, 'STUDENT');
