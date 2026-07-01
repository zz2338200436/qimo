SET @assign_teacher_id := 90052;
SET @assign_teacher_username := 'isolated_teacher_class_assign';
SET @assign_teacher_password_hash := '$2a$10$DZ6AB/86c2KKl3uaeAE6reBCU000H.ls53e3fS2ptsF3nsdGfw4MC';

SET @foreign_teacher_id := 90062;
SET @foreign_teacher_username := 'isolated_teacher_unmanaged';

SET @student_unassigned_id := 90081;
SET @student_unassigned_username := 'isolated_student_unassigned';
SET @student_managed_id := 90082;
SET @student_managed_username := 'isolated_student_managed';
SET @student_unmanaged_id := 90083;
SET @student_unmanaged_username := 'isolated_student_unmanaged';

SET @major_id := 90052;
SET @target_course_id := 90052;
SET @managed_course_id := 90053;
SET @unmanaged_course_id := 90054;
SET @target_class_id := 90052;
SET @managed_class_id := 90053;
SET @unmanaged_class_id := 90054;

USE sc_user;

INSERT IGNORE INTO roles (id, name) VALUES
    (1, 'ADMIN'),
    (2, 'TEACHER'),
    (3, 'STUDENT');

INSERT INTO users (id, username, name, email, phone, enabled)
VALUES
    (@assign_teacher_id, @assign_teacher_username, 'Isolated Teacher Assign', 'isolated-teacher-assign@example.com', '13900090052', 1),
    (@foreign_teacher_id, @foreign_teacher_username, 'Isolated Teacher Foreign', 'isolated-teacher-foreign@example.com', '13900090062', 1),
    (@student_unassigned_id, @student_unassigned_username, 'Isolated Student Unassigned', 'isolated-student-unassigned@example.com', '13900090081', 1),
    (@student_managed_id, @student_managed_username, 'Isolated Student Managed', 'isolated-student-managed@example.com', '13900090082', 1),
    (@student_unmanaged_id, @student_unmanaged_username, 'Isolated Student Unmanaged', 'isolated-student-unmanaged@example.com', '13900090083', 1)
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    name = VALUES(name),
    email = VALUES(email),
    phone = VALUES(phone),
    enabled = VALUES(enabled);

DELETE FROM user_roles
WHERE user_id IN (
    @assign_teacher_id,
    @foreign_teacher_id,
    @student_unassigned_id,
    @student_managed_id,
    @student_unmanaged_id
);

INSERT INTO user_roles (user_id, role_id)
VALUES
    (@assign_teacher_id, 2),
    (@foreign_teacher_id, 2),
    (@student_unassigned_id, 3),
    (@student_managed_id, 3),
    (@student_unmanaged_id, 3);

USE sc_auth;

INSERT INTO auth_credentials (user_id, username, password_hash, enabled)
VALUES
    (@assign_teacher_id, @assign_teacher_username, @assign_teacher_password_hash, 1)
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    password_hash = VALUES(password_hash),
    enabled = VALUES(enabled);

USE sc_course;

INSERT INTO majors (id, major_name)
VALUES
    (@major_id, 'Isolated Teacher Assign Major')
ON DUPLICATE KEY UPDATE
    major_name = VALUES(major_name);

DELETE FROM class_students
WHERE student_id IN (@student_unassigned_id, @student_managed_id, @student_unmanaged_id)
   OR class_id IN (@target_class_id, @managed_class_id, @unmanaged_class_id);

DELETE FROM class_courses
WHERE class_id IN (@target_class_id, @managed_class_id, @unmanaged_class_id)
   OR course_id IN (@target_course_id, @managed_course_id, @unmanaged_course_id);

DELETE FROM course_classes
WHERE id IN (@target_class_id, @managed_class_id, @unmanaged_class_id);

DELETE FROM courses
WHERE id IN (@target_course_id, @managed_course_id, @unmanaged_course_id)
   OR course_code IN ('ISO-ASSIGN-90052', 'ISO-ASSIGN-90053', 'ISO-ASSIGN-90054');

INSERT INTO courses (
    id, course_name, course_code, description, credit, course_category, total_hours,
    teacher_id, course_director, assessment_method, course_status, semester,
    start_date, end_date, max_students
)
VALUES
    (
        @target_course_id, 'Isolated Assign Target Course', 'ISO-ASSIGN-90052',
        'Target course for isolated teacher class assign verifier', 2, 'smoke', 32,
        @assign_teacher_id, @assign_teacher_id, 'exam', 'active', '2026 Spring',
        '2026-03-01', '2026-07-01', 30
    ),
    (
        @managed_course_id, 'Isolated Assign Managed Source Course', 'ISO-ASSIGN-90053',
        'Managed source course for isolated teacher class assign verifier', 2, 'smoke', 32,
        @assign_teacher_id, @assign_teacher_id, 'exam', 'active', '2026 Spring',
        '2026-03-01', '2026-07-01', 30
    ),
    (
        @unmanaged_course_id, 'Isolated Assign Unmanaged Source Course', 'ISO-ASSIGN-90054',
        'Unmanaged source course for isolated teacher class assign verifier', 2, 'smoke', 32,
        @foreign_teacher_id, @foreign_teacher_id, 'exam', 'active', '2026 Spring',
        '2026-03-01', '2026-07-01', 30
    );

INSERT INTO course_classes (
    id, class_name, year, capacity, course_id, teacher_id, major_id, class_time, class_location
)
VALUES
    (
        @target_class_id, 'Isolated Assign Target Class', '2026', 30,
        @target_course_id, @assign_teacher_id, @major_id, 'Monday 08:00-10:00', 'Assign Lab A'
    ),
    (
        @managed_class_id, 'Isolated Assign Managed Source Class', '2026', 30,
        @managed_course_id, @assign_teacher_id, @major_id, 'Tuesday 08:00-10:00', 'Assign Lab B'
    ),
    (
        @unmanaged_class_id, 'Isolated Assign Unmanaged Source Class', '2026', 30,
        @unmanaged_course_id, @foreign_teacher_id, @major_id, 'Wednesday 08:00-10:00', 'Assign Lab C'
    );

INSERT INTO class_students (class_id, student_id)
VALUES
    (@managed_class_id, @student_managed_id),
    (@unmanaged_class_id, @student_unmanaged_id);
