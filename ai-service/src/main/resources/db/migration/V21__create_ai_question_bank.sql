CREATE TABLE IF NOT EXISTS knowledge_points (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    point_name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    difficulty VARCHAR(20) NULL,
    order_index INT NULL,
    course_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    KEY idx_ai_question_bank_point_name (point_name),
    KEY idx_ai_question_bank_difficulty (difficulty)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS questions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    analysis TEXT NULL,
    content TEXT NOT NULL,
    correct_answer VARCHAR(255) NOT NULL,
    difficulty VARCHAR(20) NULL,
    options JSON NULL,
    score INT NULL,
    type ENUM('SINGLE_CHOICE','MULTIPLE_CHOICE','TRUE_FALSE','FILL_BLANK','SHORT_ANSWER','ESSAY') NOT NULL,
    assignment_id BIGINT NULL,
    exam_id BIGINT NULL,
    knowledge_point_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    creator_id BIGINT NULL,
    KEY idx_ai_questions_knowledge_point (knowledge_point_id),
    KEY idx_ai_questions_difficulty (difficulty),
    CONSTRAINT fk_ai_questions_knowledge_point
        FOREIGN KEY (knowledge_point_id) REFERENCES knowledge_points (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
