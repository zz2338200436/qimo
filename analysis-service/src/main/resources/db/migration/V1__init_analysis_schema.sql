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

CREATE TABLE IF NOT EXISTS early_warnings (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    class_id BIGINT NULL,
    teacher_id BIGINT NOT NULL,
    warning_type VARCHAR(50) NOT NULL,
    warning_level VARCHAR(50) NOT NULL,
    warning_message VARCHAR(1000) NOT NULL,
    trigger_date DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    is_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_by BIGINT NULL,
    resolved_date DATETIME(6) NULL,
    resolved_note VARCHAR(1000) NULL,
    assessment_type VARCHAR(50) NULL,
    related_assessment_id BIGINT NULL,
    student_name VARCHAR(100) NULL,
    course_name VARCHAR(100) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_early_warnings_student_time (student_id, trigger_date),
    INDEX idx_early_warnings_teacher_status_time (teacher_id, is_resolved, trigger_date),
    INDEX idx_early_warnings_course_type_level (course_id, warning_type, warning_level)
);

CREATE TABLE IF NOT EXISTS processed_event (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(150) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    consumer_name VARCHAR(100) NOT NULL,
    processed_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_processed_event_event_consumer UNIQUE (event_id, consumer_name)
);
