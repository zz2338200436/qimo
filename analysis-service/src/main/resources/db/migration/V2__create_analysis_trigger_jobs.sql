CREATE TABLE IF NOT EXISTS analysis_trigger_jobs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    teacher_id BIGINT NOT NULL,
    trigger_type VARCHAR(50) NOT NULL,
    class_id BIGINT NULL,
    course_id BIGINT NULL,
    student_id BIGINT NULL,
    status VARCHAR(30) NOT NULL,
    requested_count INT NOT NULL DEFAULT 0,
    warning_count INT NOT NULL DEFAULT 0,
    message VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6) NULL,
    INDEX idx_analysis_trigger_jobs_teacher_time (teacher_id, created_at),
    INDEX idx_analysis_trigger_jobs_status_time (status, created_at)
);
