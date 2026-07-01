CREATE DATABASE IF NOT EXISTS sc_exam
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE sc_exam;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS exam_classes (
  exam_id BIGINT NOT NULL,
  class_id BIGINT NOT NULL,
  PRIMARY KEY (exam_id, class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELETE FROM exam_submissions WHERE id IN (9002, 9003);
DELETE FROM exam_classes WHERE exam_id IN (9001, 9002);
DELETE FROM exams WHERE id IN (9001, 9002);

INSERT INTO exams (
  id, title, description, course_id, teacher_id, start_time, end_time, publish_date,
  duration, total_score, is_active, is_online, location, created_at, updated_at
) VALUES
  (
    9001,
    'StudentExamSmoke-Open',
    'student exams smoke data for current active course',
    2,
    7,
    DATE_SUB(NOW(), INTERVAL 2 DAY),
    DATE_ADD(NOW(), INTERVAL 7 DAY),
    DATE_SUB(NOW(), INTERVAL 3 DAY),
    90,
    100,
    1,
    1,
    CONVERT(0xE7BABFE4B88AE88083E8AF95 USING utf8mb4),
    NOW(),
    NOW()
  ),
  (
    9002,
    'StudentExamSmoke-Graded',
    'student exams graded smoke data for score query',
    2,
    7,
    DATE_SUB(NOW(), INTERVAL 12 DAY),
    DATE_SUB(NOW(), INTERVAL 10 DAY),
    DATE_SUB(NOW(), INTERVAL 13 DAY),
    60,
    100,
    1,
    1,
    CONVERT(0xE7BABFE4B88AE88083E8AF95 USING utf8mb4),
    NOW(),
    NOW()
  );

INSERT INTO exam_classes (exam_id, class_id) VALUES
  (9001, 2),
  (9002, 2);

INSERT INTO exam_submissions (
  id, exam_id, student_id, answers_json, content, submission_date, time_taken, graded, score, teacher_comment,
  created_at, updated_at
) VALUES
  (
    9002,
    9002,
    42,
    JSON_OBJECT('q1', 'A', 'q2', 'B'),
    '{\"q1\":\"A\",\"q2\":\"B\"}',
    DATE_SUB(NOW(), INTERVAL 10 DAY),
    48,
    1,
    89,
    'exam smoke graded comment',
    NOW(),
    NOW()
  );
