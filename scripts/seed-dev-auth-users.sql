USE sc_user;

INSERT IGNORE INTO roles (id, name) VALUES
    (1, 'ADMIN'),
    (2, 'TEACHER'),
    (3, 'STUDENT');

INSERT INTO users (id, username, name, email, phone, enabled)
VALUES
    (7, 'teacher7', 'Teacher Seven', 'teacher7@example.com', '13800000007', 1),
    (42, 'student42', 'Student Forty Two', 'student42@example.com', '13800000042', 1)
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    name = VALUES(name),
    email = VALUES(email),
    phone = VALUES(phone),
    enabled = VALUES(enabled);

INSERT INTO user_roles (user_id, role_id)
VALUES
    (7, 2),
    (42, 3)
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id);

USE sc_auth;

INSERT INTO auth_credentials (user_id, username, password_hash, enabled)
VALUES
    (7, 'teacher7', '$2a$10$DZ6AB/86c2KKl3uaeAE6reBCU000H.ls53e3fS2ptsF3nsdGfw4MC', 1),
    (42, 'student42', '$2a$10$DZ6AB/86c2KKl3uaeAE6reBCU000H.ls53e3fS2ptsF3nsdGfw4MC', 1)
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    password_hash = VALUES(password_hash),
    enabled = VALUES(enabled);
