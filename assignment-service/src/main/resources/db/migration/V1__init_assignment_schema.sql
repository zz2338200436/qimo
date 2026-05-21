CREATE TABLE IF NOT EXISTS assignments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    course_id BIGINT NOT NULL,
    due_date VARCHAR(30),
    publish_date VARCHAR(30),
    is_active BOOLEAN DEFAULT TRUE,
    teacher_id BIGINT NOT NULL,
    max_score INT,
    submission_count INT DEFAULT 0,
    graded_count INT DEFAULT 0,
    status VARCHAR(30),
    total_students INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_assignments_course (course_id),
    KEY idx_assignments_teacher (teacher_id)
);

CREATE TABLE IF NOT EXISTS assignment_submissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    content TEXT,
    submission_date VARCHAR(30),
    graded BOOLEAN DEFAULT FALSE,
    is_late BOOLEAN DEFAULT FALSE,
    late_penalty INT,
    score INT,
    teacher_comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_assignment_submissions_assignment (assignment_id),
    KEY idx_assignment_submissions_student (student_id),
    KEY idx_assignment_submissions_score (score)
);

CREATE TABLE IF NOT EXISTS assignment_classes (
    assignment_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    PRIMARY KEY (assignment_id, class_id),
    KEY idx_assignment_classes_class (class_id)
);

CREATE TABLE IF NOT EXISTS assignment_knowledge_points (
    assignment_id BIGINT NOT NULL,
    knowledge_point_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (assignment_id, knowledge_point_id),
    KEY idx_assignment_knowledge_points_point (knowledge_point_id)
);
