SET @smoke_user_id := 90042;
SET @smoke_username := 'isolated_api_smoke';
SET @smoke_password_hash := '$2a$10$DZ6AB/86c2KKl3uaeAE6reBCU000H.ls53e3fS2ptsF3nsdGfw4MC';
SET @smoke_course_id := 90042;
SET @smoke_class_id := 90042;
SET @smoke_class_course_id := 90042;
SET @smoke_exam_id := 90042;
SET @smoke_submission_id := 90042;

USE sc_user;

INSERT IGNORE INTO roles (id, name) VALUES
    (1, 'ADMIN'),
    (2, 'TEACHER'),
    (3, 'STUDENT');

INSERT INTO users (id, username, name, email, phone, enabled)
VALUES
    (@smoke_user_id, @smoke_username, 'Isolated API Smoke', 'isolated-api-smoke@example.com', '13900090042', 1)
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    name = VALUES(name),
    email = VALUES(email),
    phone = VALUES(phone),
    enabled = VALUES(enabled);

DELETE FROM user_roles WHERE user_id = @smoke_user_id;
INSERT INTO user_roles (user_id, role_id)
VALUES
    (@smoke_user_id, 2),
    (@smoke_user_id, 3);

USE sc_auth;

INSERT INTO auth_credentials (user_id, username, password_hash, enabled)
VALUES
    (@smoke_user_id, @smoke_username, @smoke_password_hash, 1)
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    password_hash = VALUES(password_hash),
    enabled = VALUES(enabled);

USE sc_course;

INSERT INTO majors (id, major_name)
VALUES
    (90042, 'Isolated API Smoke Major')
ON DUPLICATE KEY UPDATE
    major_name = VALUES(major_name);

DELETE FROM class_students WHERE student_id = @smoke_user_id OR class_id = @smoke_class_id;
DELETE FROM class_courses WHERE id = @smoke_class_course_id OR class_id = @smoke_class_id OR course_id = @smoke_course_id;
DELETE FROM course_classes WHERE id = @smoke_class_id;
DELETE FROM courses WHERE id = @smoke_course_id OR course_code = 'ISO-SIDE-90042';

INSERT INTO courses (
    id, course_name, course_code, description, credit, course_category, total_hours,
    teacher_id, course_director, assessment_method, course_status, semester,
    start_date, end_date, max_students
)
VALUES (
    @smoke_course_id, 'Isolated Side Effect Smoke Course', 'ISO-SIDE-90042',
    'Gateway isolated side-effect smoke fixture', 2, 'smoke', 32,
    @smoke_user_id, @smoke_user_id, 'exam', 'active', '2026 Spring',
    '2026-03-01', '2026-07-01', 20
);

INSERT INTO course_classes (
    id, class_name, year, capacity, course_id, teacher_id, major_id, class_time, class_location
)
VALUES (
    @smoke_class_id, 'Isolated Side Effect Smoke Class', '2026', 20,
    @smoke_course_id, @smoke_user_id, 90042, 'Friday 10:00-12:00', 'Smoke Lab'
);

INSERT INTO class_courses (id, class_id, course_id, teacher_id, class_time, class_location)
VALUES (
    @smoke_class_course_id, @smoke_class_id, @smoke_course_id, @smoke_user_id,
    'Friday 10:00-12:00', 'Smoke Lab'
);

INSERT INTO class_students (class_id, student_id)
VALUES
    (@smoke_class_id, @smoke_user_id);

USE sc_exam;

DELETE FROM exam_submissions WHERE id = @smoke_submission_id OR exam_id = @smoke_exam_id OR student_id = @smoke_user_id;
DELETE FROM exam_classes WHERE exam_id = @smoke_exam_id;
DELETE FROM exam_knowledge_points WHERE exam_id = @smoke_exam_id;
DELETE FROM exam_questions WHERE exam_id = @smoke_exam_id;
DELETE FROM exams WHERE id = @smoke_exam_id;

INSERT INTO exams (
    id, title, description, course_id, teacher_id, start_time, end_time,
    publish_date, duration, total_score, is_active, is_online, location
)
VALUES (
    @smoke_exam_id, 'Isolated Side Effect Smoke Exam',
    'Gateway isolated side-effect smoke fixture', @smoke_course_id, @smoke_user_id,
    '2026-06-01 09:00:00', '2026-06-01 10:00:00', '2026-05-31 09:00:00',
    60, 100, 1, 1, 'Smoke Lab'
);

INSERT INTO exam_classes (exam_id, class_id)
VALUES
    (@smoke_exam_id, @smoke_class_id);

INSERT INTO exam_questions (
    exam_id, question_text, question_type, options_json, correct_answer, score, knowledge_point_id, sort_order
)
VALUES (
    @smoke_exam_id, 'Isolated smoke question', 'single_choice',
    '["A","B","C","D"]', 'A', 100, NULL, 1
);

INSERT INTO exam_submissions (
    id, exam_id, student_id, content, submission_date, time_taken, graded, score, teacher_comment
)
VALUES (
    @smoke_submission_id, @smoke_exam_id, @smoke_user_id,
    '{"answers":{"1":"A"}}', '2026-05-21 10:00:00', 30, 1, 80,
    'Initial isolated smoke submission'
);

USE sc_notification;

DELETE FROM notifications
WHERE student_id = @smoke_user_id
   OR title LIKE 'IsolatedSmoke-%';
