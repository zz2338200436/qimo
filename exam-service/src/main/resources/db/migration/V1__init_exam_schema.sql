CREATE TABLE IF NOT EXISTS exams (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    course_id BIGINT NOT NULL,
    teacher_id BIGINT NULL,
    start_time DATETIME NULL,
    end_time DATETIME NULL,
    publish_date DATETIME NULL,
    duration INT NULL,
    total_score INT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    is_online TINYINT(1) NOT NULL DEFAULT 1,
    location VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS exam_submissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    answers_json JSON NULL,
    content TEXT NULL,
    submission_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    time_taken INT NULL,
    graded TINYINT(1) NOT NULL DEFAULT 0,
    score INT NULL,
    teacher_comment TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_exam_submission UNIQUE (exam_id, student_id)
);

CREATE TABLE IF NOT EXISTS exam_classes (
    exam_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    PRIMARY KEY (exam_id, class_id)
);

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
