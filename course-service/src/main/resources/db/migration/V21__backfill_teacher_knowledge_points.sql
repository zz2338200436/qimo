CREATE TABLE IF NOT EXISTS teacher_knowledge_points (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    point_name VARCHAR(255) NOT NULL,
    description TEXT,
    difficulty VARCHAR(50),
    order_index INT DEFAULT 0,
    course_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_teacher_knowledge_points_course (course_id)
);
