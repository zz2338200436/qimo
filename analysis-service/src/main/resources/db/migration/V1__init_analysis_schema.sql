CREATE TABLE IF NOT EXISTS score_trends (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    class_id BIGINT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_id BIGINT NOT NULL,
    submission_id BIGINT NOT NULL,
    score INT NULL,
    max_score INT NULL,
    score_rate DECIMAL(6,4) NULL,
    occurred_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_score_trends_source_submission UNIQUE (source_type, submission_id),
    INDEX idx_score_trends_student_course_time (student_id, course_id, occurred_at)
);

CREATE TABLE IF NOT EXISTS kp_mastery (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    class_id BIGINT NULL,
    mastery_score DECIMAL(6,4) NOT NULL,
    evidence_count INT NOT NULL DEFAULT 0,
    last_source_type VARCHAR(50) NULL,
    last_source_id BIGINT NULL,
    last_event_id VARCHAR(150) NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_kp_mastery_student_course UNIQUE (student_id, course_id),
    INDEX idx_kp_mastery_course_score (course_id, mastery_score)
);

CREATE TABLE IF NOT EXISTS processed_event (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(150) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    consumer_name VARCHAR(100) NOT NULL,
    processed_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_processed_event_event_consumer UNIQUE (event_id, consumer_name)
);
