CREATE TABLE IF NOT EXISTS exam_knowledge_points (
    exam_id BIGINT NOT NULL,
    knowledge_point_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (exam_id, knowledge_point_id),
    KEY idx_exam_knowledge_points_point (knowledge_point_id)
);

CREATE TABLE IF NOT EXISTS exam_questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    question_text TEXT NOT NULL,
    question_type VARCHAR(50) NOT NULL,
    options_json JSON NULL,
    correct_answer TEXT NULL,
    score INT NOT NULL DEFAULT 0,
    knowledge_point_id BIGINT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_exam_questions_exam_order (exam_id, sort_order),
    KEY idx_exam_questions_knowledge_point (knowledge_point_id)
);
