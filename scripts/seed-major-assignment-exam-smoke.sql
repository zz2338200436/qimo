CREATE DATABASE IF NOT EXISTS major_assignment
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE major_assignment;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP VIEW IF EXISTS users;
CREATE VIEW users AS
SELECT
  id,
  username,
  NULL AS password,
  name,
  email,
  phone,
  avatar,
  enabled,
  created_at,
  updated_at
FROM sc_user.users;

DROP VIEW IF EXISTS courses;
CREATE VIEW courses AS
SELECT
  id,
  course_name,
  course_code,
  description,
  credit,
  course_category,
  total_hours,
  teacher_id,
  course_director,
  assessment_method,
  course_status,
  semester,
  start_date,
  end_date,
  max_students,
  created_at,
  updated_at
FROM sc_course.courses;

DROP VIEW IF EXISTS course_classes;
CREATE VIEW course_classes AS
SELECT
  id,
  class_name,
  year,
  capacity,
  course_id,
  teacher_id,
  major_id,
  class_time,
  class_location,
  created_at,
  updated_at
FROM sc_course.course_classes;

DROP VIEW IF EXISTS class_courses;
CREATE VIEW class_courses AS
SELECT
  id,
  class_id,
  course_id,
  teacher_id,
  class_time,
  class_location,
  created_at
FROM sc_course.class_courses;

DROP VIEW IF EXISTS class_students;
CREATE VIEW class_students AS
SELECT
  id,
  class_id,
  student_id,
  created_at
FROM sc_course.class_students;

DROP VIEW IF EXISTS assignments;
CREATE VIEW assignments AS
SELECT
  id,
  title,
  description,
  course_id,
  due_date,
  publish_date,
  is_active,
  teacher_id,
  max_score,
  submission_count,
  graded_count,
  status,
  total_students,
  created_at,
  updated_at
FROM sc_assignment.assignments;

DROP VIEW IF EXISTS assignment_classes;
CREATE VIEW assignment_classes AS
SELECT
  assignment_id,
  class_id
FROM sc_assignment.assignment_classes;

DROP VIEW IF EXISTS assignment_submissions;
CREATE VIEW assignment_submissions AS
SELECT
  id,
  assignment_id,
  student_id,
  content,
  submission_date,
  graded,
  is_late,
  late_penalty,
  score,
  teacher_comment,
  created_at,
  updated_at
FROM sc_assignment.assignment_submissions;

CREATE TABLE IF NOT EXISTS knowledge_points (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  point_name VARCHAR(255) NOT NULL,
  description TEXT NULL,
  difficulty VARCHAR(50) NULL,
  order_index INT NULL,
  course_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS knowledge_mastery (
  student_id BIGINT NOT NULL,
  knowledge_point_id BIGINT NOT NULL,
  mastery_level VARCHAR(20) NOT NULL,
  last_assessed_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (student_id, knowledge_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS assignment_knowledge_points (
  assignment_id BIGINT NOT NULL,
  knowledge_point_id BIGINT NOT NULL,
  PRIMARY KEY (assignment_id, knowledge_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS exam_knowledge_points (
  exam_id BIGINT NOT NULL,
  knowledge_point_id BIGINT NOT NULL,
  PRIMARY KEY (exam_id, knowledge_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS exams (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(100) NOT NULL,
  description TEXT NOT NULL,
  course_id BIGINT NOT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  publish_date DATETIME NOT NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  is_online TINYINT(1) NOT NULL DEFAULT 1,
  location VARCHAR(255) NULL,
  duration BIGINT NOT NULL,
  teacher_id BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS exam_classes (
  exam_id BIGINT NOT NULL,
  class_id BIGINT NOT NULL,
  PRIMARY KEY (exam_id, class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS exam_submissions (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  graded TINYINT(1) NOT NULL DEFAULT 0,
  score INT NULL,
  submission_date DATETIME NOT NULL,
  teacher_comment TEXT NULL,
  content LONGTEXT NULL,
  time_taken INT NULL,
  exam_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE knowledge_points CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE knowledge_mastery CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE assignment_knowledge_points CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE exam_knowledge_points CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE exams CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE exam_classes CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE exam_submissions CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

DELETE FROM knowledge_mastery WHERE knowledge_point_id IN (8001, 8002);
DELETE FROM assignment_knowledge_points WHERE knowledge_point_id IN (8001, 8002);
DELETE FROM exam_knowledge_points WHERE knowledge_point_id IN (8001, 8002);
DELETE FROM knowledge_points WHERE id IN (8001, 8002);
DELETE FROM exam_submissions WHERE exam_id IN (9001, 9002);
DELETE FROM exam_classes WHERE exam_id IN (9001, 9002);
DELETE FROM exams WHERE id IN (9001, 9002);

INSERT INTO knowledge_points (
  id, point_name, description, difficulty, order_index, course_id, created_at, updated_at
) VALUES
  (
    8001,
    CONVERT(0xE587BDE695B0E69E81E99990 USING utf8mb4),
    'CourseSmokeA teacher knowledge smoke point: limit analysis basics',
    CONVERT(0xE4B8ADE7AD89 USING utf8mb4),
    1,
    2,
    NOW(),
    NOW()
  ),
  (
    8002,
    CONVERT(0xE5AFBCE695B0E5BA94E794A8 USING utf8mb4),
    'CourseSmokeA teacher knowledge smoke point: derivative application practice',
    CONVERT(0xE8BE83E99ABE USING utf8mb4),
    2,
    2,
    NOW(),
    NOW()
  );

INSERT INTO knowledge_mastery (
  student_id, knowledge_point_id, mastery_level, last_assessed_date, update_time
) VALUES
  (
    42,
    8001,
    CONVERT(0xE889AFE5A5BD USING utf8mb4),
    NOW(),
    NOW()
  ),
  (
    42,
    8002,
    CONVERT(0xE8BE83E5B7AE USING utf8mb4),
    NOW(),
    NOW()
  );

INSERT INTO exams (
  id, title, description, course_id, start_time, end_time, publish_date,
  is_active, is_online, location, duration, teacher_id
) VALUES
  (
    9001,
    'StudentExamSmoke-Open',
    'student exams smoke data for current active course',
    2,
    DATE_SUB(NOW(), INTERVAL 2 DAY),
    DATE_ADD(NOW(), INTERVAL 7 DAY),
    DATE_SUB(NOW(), INTERVAL 3 DAY),
    1,
    1,
    CONVERT(0xE7BABFE4B88AE88083E8AF95 USING utf8mb4),
    90,
    7
  ),
  (
    9002,
    'StudentExamSmoke-Graded',
    'student exams graded smoke data for score query',
    2,
    DATE_SUB(NOW(), INTERVAL 12 DAY),
    DATE_SUB(NOW(), INTERVAL 10 DAY),
    DATE_SUB(NOW(), INTERVAL 13 DAY),
    1,
    1,
    CONVERT(0xE7BABFE4B88AE88083E8AF95 USING utf8mb4),
    60,
    7
  );

INSERT INTO exam_classes (exam_id, class_id) VALUES
  (9001, 2),
  (9002, 2);

INSERT INTO assignment_knowledge_points (assignment_id, knowledge_point_id) VALUES
  (6, 8001),
  (6, 8002);

INSERT INTO exam_knowledge_points (exam_id, knowledge_point_id) VALUES
  (9001, 8001),
  (9002, 8002);

INSERT INTO exam_submissions (
  id, graded, score, submission_date, teacher_comment, content, time_taken, exam_id, student_id
) VALUES
  (
    9002,
    1,
    89,
    DATE_SUB(NOW(), INTERVAL 10 DAY),
    'exam smoke graded comment',
    '{"q1":"A","q2":"B"}',
    48,
    9002,
    42
  );
