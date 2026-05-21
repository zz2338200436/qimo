CREATE TABLE IF NOT EXISTS majors (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    major_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS courses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_name VARCHAR(50) NOT NULL,
    course_code VARCHAR(20) NOT NULL,
    description VARCHAR(200),
    credit INT NOT NULL,
    course_category VARCHAR(50),
    total_hours INT NOT NULL,
    teacher_id BIGINT NOT NULL,
    course_director BIGINT,
    assessment_method VARCHAR(50),
    course_status VARCHAR(30),
    semester VARCHAR(50),
    start_date VARCHAR(20),
    end_date VARCHAR(20),
    max_students INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_courses_code (course_code)
);

CREATE TABLE IF NOT EXISTS course_classes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_name VARCHAR(100) NOT NULL,
    year VARCHAR(20),
    capacity INT,
    course_id BIGINT NULL,
    teacher_id BIGINT,
    major_id BIGINT,
    class_time VARCHAR(100),
    class_location VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_course_classes_course (course_id),
    KEY idx_course_classes_teacher (teacher_id),
    KEY idx_course_classes_major (major_id)
);

CREATE TABLE IF NOT EXISTS class_courses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    teacher_id BIGINT,
    class_time VARCHAR(100),
    class_location VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_class_courses (class_id, course_id),
    KEY idx_class_courses_course (course_id),
    KEY idx_class_courses_teacher (teacher_id)
);

CREATE TABLE IF NOT EXISTS class_students (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_class_students (class_id, student_id),
    KEY idx_class_students_student (student_id)
);

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
