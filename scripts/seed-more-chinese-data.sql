CREATE DATABASE IF NOT EXISTS major_assignment
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE major_assignment;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- seed-more-chinese-data:start
SET @demo_teacher_id := 9201;
SET @demo_student_1 := 9211;
SET @demo_student_2 := 9212;
SET @demo_student_3 := 9213;
SET @demo_student_4 := 9214;
SET @demo_course_id := 9301;
SET @demo_class_id := 9401;
SET @demo_class_course_id := 9402;
SET @demo_assignment_1 := 9501;
SET @demo_assignment_2 := 9502;
SET @demo_exam_id := 9601;
SET @demo_kp_1 := 9701;
SET @demo_kp_2 := 9702;
SET @demo_kp_3 := 9703;

CREATE TABLE IF NOT EXISTS major_assignment.notifications (
  id BIGINT NOT NULL AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  teacher_id BIGINT NOT NULL,
  type VARCHAR(20) NOT NULL,
  title VARCHAR(100) NOT NULL,
  content TEXT NOT NULL,
  related_id BIGINT NULL,
  is_read TINYINT(1) NULL DEFAULT 0,
  created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_notifications_student (student_id),
  INDEX idx_notifications_teacher (teacher_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS major_assignment.early_warnings (
  id BIGINT NOT NULL AUTO_INCREMENT,
  assessment_type VARCHAR(255) NULL,
  related_assessment_id BIGINT NULL,
  is_resolved BIT(1) NOT NULL DEFAULT b'0',
  resolved_by BIGINT NULL,
  resolved_date DATETIME(6) NULL,
  resolved_note VARCHAR(500) NULL,
  trigger_date DATETIME(6) NOT NULL,
  warning_level VARCHAR(255) NOT NULL,
  warning_message VARCHAR(500) NOT NULL,
  warning_type VARCHAR(255) NOT NULL,
  course_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  teacher_id BIGINT NULL,
  PRIMARY KEY (id),
  INDEX idx_early_warnings_student (student_id),
  INDEX idx_early_warnings_course (course_id),
  INDEX idx_early_warnings_teacher (teacher_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS major_assignment.student_performance (
  id BIGINT NOT NULL AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  average_score DOUBLE NULL DEFAULT 0,
  pending_assignments INT NULL DEFAULT 0,
  overall_progress INT NULL DEFAULT 0,
  created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE INDEX uk_student_id (student_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS major_assignment.audit_logs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  operator_id BIGINT NULL,
  operation_type VARCHAR(100) NULL,
  table_name VARCHAR(100) NULL,
  record_id BIGINT NULL,
  old_data TEXT NULL,
  new_data TEXT NULL,
  ip_address VARCHAR(50) NULL,
  operation_time DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  INDEX idx_audit_logs_operator (operator_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT IGNORE INTO sc_user.roles (id, name)
VALUES
  (2, 'TEACHER'),
  (3, 'STUDENT');

INSERT INTO sc_user.users (id, username, name, email, phone, avatar, enabled, created_at, updated_at)
VALUES
  (@demo_teacher_id, 'teacher_demo_cn', '王明远', 'wangmingyuan@example.com', '13892010001', 'teacher-avatar-demo-9201.png', 1, '2026-01-10 08:00:00', '2026-01-10 08:00:00'),
  (@demo_student_1, 'student_demo_01', '李雨桐', 'liyutong@example.com', '13992110001', 'student-avatar-demo-9211.png', 1, '2026-01-10 08:05:00', '2026-01-10 08:05:00'),
  (@demo_student_2, 'student_demo_02', '赵思涵', 'zhaosihan@example.com', '13992110002', 'student-avatar-demo-9212.png', 1, '2026-01-10 08:06:00', '2026-01-10 08:06:00'),
  (@demo_student_3, 'student_demo_03', '陈嘉宁', 'chenjianing@example.com', '13992110003', 'student-avatar-demo-9213.png', 1, '2026-01-10 08:07:00', '2026-01-10 08:07:00'),
  (@demo_student_4, 'student_demo_04', '梁晓曼', 'liangxiaoman@example.com', '13992110004', 'student-avatar-demo-9214.png', 1, '2026-01-10 08:08:00', '2026-01-10 08:08:00')
ON DUPLICATE KEY UPDATE
  username = VALUES(username),
  name = VALUES(name),
  email = VALUES(email),
  phone = VALUES(phone),
  avatar = VALUES(avatar),
  enabled = VALUES(enabled),
  updated_at = VALUES(updated_at);

INSERT INTO sc_auth.auth_credentials (user_id, username, password_hash, enabled)
VALUES
  (@demo_teacher_id, 'teacher_demo_cn', '$2a$10$pmRCf38Yzlcu.kNEgaH1TOzmoIhuatiG7uyI3/7D3IWUTZR3HLsbe', 1),
  (@demo_student_1, 'student_demo_01', '$2a$10$pmRCf38Yzlcu.kNEgaH1TOzmoIhuatiG7uyI3/7D3IWUTZR3HLsbe', 1),
  (@demo_student_2, 'student_demo_02', '$2a$10$pmRCf38Yzlcu.kNEgaH1TOzmoIhuatiG7uyI3/7D3IWUTZR3HLsbe', 1),
  (@demo_student_3, 'student_demo_03', '$2a$10$pmRCf38Yzlcu.kNEgaH1TOzmoIhuatiG7uyI3/7D3IWUTZR3HLsbe', 1),
  (@demo_student_4, 'student_demo_04', '$2a$10$pmRCf38Yzlcu.kNEgaH1TOzmoIhuatiG7uyI3/7D3IWUTZR3HLsbe', 1)
ON DUPLICATE KEY UPDATE
  username = VALUES(username),
  password_hash = VALUES(password_hash),
  enabled = VALUES(enabled),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO sc_user.user_roles (user_id, role_id)
VALUES
  (@demo_teacher_id, 2),
  (@demo_student_1, 3),
  (@demo_student_2, 3),
  (@demo_student_3, 3),
  (@demo_student_4, 3)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO sc_course.courses (
  id, course_name, course_code, description, credit, course_category, total_hours,
  teacher_id, course_director, assessment_method, course_status, semester,
  start_date, end_date, max_students, created_at, updated_at
)
VALUES (
  @demo_course_id,
  '分布式框架技术',
  'DFT2026',
  '围绕 Spring Boot、服务注册、网关路由、配置中心、消息通知与学习分析构建中文演示课程数据。',
  4,
  '专业课',
  64,
  @demo_teacher_id,
  @demo_teacher_id,
  '项目实践+阶段测验+课堂表现',
  '进行中',
  '第二学期',
  '2026-03-01',
  '2026-06-30',
  80,
  '2026-01-10 08:20:00',
  '2026-01-10 08:20:00'
)
ON DUPLICATE KEY UPDATE
  course_name = VALUES(course_name),
  course_code = VALUES(course_code),
  description = VALUES(description),
  credit = VALUES(credit),
  course_category = VALUES(course_category),
  total_hours = VALUES(total_hours),
  teacher_id = VALUES(teacher_id),
  course_director = VALUES(course_director),
  assessment_method = VALUES(assessment_method),
  course_status = VALUES(course_status),
  semester = VALUES(semester),
  start_date = VALUES(start_date),
  end_date = VALUES(end_date),
  max_students = VALUES(max_students),
  updated_at = VALUES(updated_at);

INSERT INTO sc_course.course_classes (
  id, class_name, year, capacity, course_id, teacher_id, major_id,
  class_time, class_location, created_at, updated_at
)
VALUES (
  @demo_class_id,
  '智慧课堂演示班',
  '2026',
  48,
  @demo_course_id,
  @demo_teacher_id,
  NULL,
  '周二 3-4 节 / 周四 5-6 节',
  '知行楼 305 智慧教室',
  '2026-01-10 08:25:00',
  '2026-01-10 08:25:00'
)
ON DUPLICATE KEY UPDATE
  class_name = VALUES(class_name),
  year = VALUES(year),
  capacity = VALUES(capacity),
  course_id = VALUES(course_id),
  teacher_id = VALUES(teacher_id),
  class_time = VALUES(class_time),
  class_location = VALUES(class_location),
  updated_at = VALUES(updated_at);

DELETE FROM sc_course.class_courses
WHERE id = @demo_class_course_id
   OR (class_id = @demo_class_id AND course_id = @demo_course_id);

INSERT INTO sc_course.class_courses (id, class_id, course_id, teacher_id, class_time, class_location, created_at)
VALUES (
  @demo_class_course_id,
  @demo_class_id,
  @demo_course_id,
  @demo_teacher_id,
  '周二 3-4 节 / 周四 5-6 节',
  '知行楼 305 智慧教室',
  '2026-01-10 08:30:00'
);

INSERT INTO sc_course.class_students (class_id, student_id, created_at)
VALUES
  (@demo_class_id, @demo_student_1, '2026-01-10 08:35:00'),
  (@demo_class_id, @demo_student_2, '2026-01-10 08:36:00'),
  (@demo_class_id, @demo_student_3, '2026-01-10 08:37:00'),
  (@demo_class_id, @demo_student_4, '2026-01-10 08:38:00')
ON DUPLICATE KEY UPDATE created_at = VALUES(created_at);

INSERT INTO sc_assignment.assignments (
  id, title, description, course_id, due_date, publish_date, is_active,
  teacher_id, max_score, submission_count, graded_count, status, total_students,
  created_at, updated_at
)
VALUES
  (
    @demo_assignment_1,
    '分布式框架技术作业：服务注册与发现',
    '完成 Eureka/Nacos 服务注册与发现实践，提交服务启动截图、调用链路说明和核心配置文件。',
    @demo_course_id,
    '2026-04-12 23:59:00',
    '2026-04-01 09:00:00',
    1,
    @demo_teacher_id,
    100,
    4,
    4,
    '已发布',
    4,
    '2026-04-01 09:00:00',
    '2026-04-08 18:00:00'
  ),
  (
    @demo_assignment_2,
    '分布式框架技术作业：网关路由设计',
    '设计课程系统网关路由、鉴权过滤器和异常降级方案，并说明关键测试结果。',
    @demo_course_id,
    '2026-04-26 23:59:00',
    '2026-04-15 09:00:00',
    1,
    @demo_teacher_id,
    100,
    2,
    2,
    '批改中',
    4,
    '2026-04-15 09:00:00',
    '2026-04-20 18:00:00'
  )
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  description = VALUES(description),
  course_id = VALUES(course_id),
  due_date = VALUES(due_date),
  publish_date = VALUES(publish_date),
  is_active = VALUES(is_active),
  teacher_id = VALUES(teacher_id),
  max_score = VALUES(max_score),
  submission_count = VALUES(submission_count),
  graded_count = VALUES(graded_count),
  status = VALUES(status),
  total_students = VALUES(total_students),
  updated_at = VALUES(updated_at);

INSERT INTO sc_assignment.assignment_classes (assignment_id, class_id)
VALUES
  (@demo_assignment_1, @demo_class_id),
  (@demo_assignment_2, @demo_class_id)
ON DUPLICATE KEY UPDATE class_id = VALUES(class_id);

INSERT INTO sc_assignment.assignment_submissions (
  id, assignment_id, student_id, content, submission_date, graded, is_late,
  late_penalty, score, teacher_comment, created_at, updated_at
)
VALUES
  (98001, @demo_assignment_1, @demo_student_1, '已完成服务注册中心搭建，消费者可以通过服务名调用课程服务。', '2026-04-09 20:10:00', 1, 0, 0, 92, '配置清晰，调用链路说明完整。', '2026-04-09 20:10:00', '2026-04-10 09:00:00'),
  (98002, @demo_assignment_1, @demo_student_2, '已提交 Nacos 注册发现示例，包含课程服务、作业服务和网关服务。', '2026-04-10 19:40:00', 1, 0, 0, 88, '服务拆分思路正确，建议补充异常场景。', '2026-04-10 19:40:00', '2026-04-11 09:00:00'),
  (98003, @demo_assignment_1, @demo_student_3, '完成注册中心配置，但服务健康检查截图不够完整。', '2026-04-13 00:20:00', 1, 1, 5, 74, '有迟交，核心配置基本正确。', '2026-04-13 00:20:00', '2026-04-13 10:00:00'),
  (98004, @demo_assignment_1, @demo_student_4, '提交了服务注册流程图和核心配置说明。', '2026-04-11 21:30:00', 1, 0, 0, 81, '流程图表达清楚，代码截图略少。', '2026-04-11 21:30:00', '2026-04-12 09:00:00'),
  (98005, @demo_assignment_2, @demo_student_1, '完成网关路由与 JWT 过滤器配置，附带接口测试结果。', '2026-04-22 18:30:00', 1, 0, 0, 90, '测试覆盖较完整。', '2026-04-22 18:30:00', '2026-04-23 09:00:00'),
  (98006, @demo_assignment_2, @demo_student_3, '网关路由已实现，降级说明较简略。', '2026-04-27 01:15:00', 1, 1, 5, 68, '连续两次作业低于及格线附近，需要补充实践记录。', '2026-04-27 01:15:00', '2026-04-27 10:00:00')
ON DUPLICATE KEY UPDATE
  assignment_id = VALUES(assignment_id),
  student_id = VALUES(student_id),
  content = VALUES(content),
  submission_date = VALUES(submission_date),
  graded = VALUES(graded),
  is_late = VALUES(is_late),
  late_penalty = VALUES(late_penalty),
  score = VALUES(score),
  teacher_comment = VALUES(teacher_comment),
  updated_at = VALUES(updated_at);

INSERT INTO major_assignment.knowledge_points (id, point_name, description, difficulty, order_index, course_id, created_at, updated_at)
VALUES
  (@demo_kp_1, '服务注册与发现', '理解注册中心、服务实例注册、健康检查与服务发现调用流程。', '中等', 1, @demo_course_id, '2026-04-01 09:00:00', '2026-04-01 09:00:00'),
  (@demo_kp_2, 'API网关与路由', '掌握网关路由、鉴权过滤器、跨域配置和降级处理。', '较难', 2, @demo_course_id, '2026-04-01 09:00:00', '2026-04-01 09:00:00'),
  (@demo_kp_3, '配置中心与动态刷新', '掌握配置集中管理、环境隔离和运行时刷新机制。', '中等', 3, @demo_course_id, '2026-04-01 09:00:00', '2026-04-01 09:00:00')
ON DUPLICATE KEY UPDATE
  point_name = VALUES(point_name),
  description = VALUES(description),
  difficulty = VALUES(difficulty),
  order_index = VALUES(order_index),
  course_id = VALUES(course_id),
  updated_at = VALUES(updated_at);

INSERT INTO major_assignment.assignment_knowledge_points (assignment_id, knowledge_point_id)
VALUES
  (@demo_assignment_1, @demo_kp_1),
  (@demo_assignment_1, @demo_kp_3),
  (@demo_assignment_2, @demo_kp_2)
ON DUPLICATE KEY UPDATE knowledge_point_id = VALUES(knowledge_point_id);

INSERT INTO sc_assignment.assignment_knowledge_points (assignment_id, knowledge_point_id, created_at)
VALUES
  (@demo_assignment_1, @demo_kp_1, '2026-04-01 09:05:00'),
  (@demo_assignment_1, @demo_kp_3, '2026-04-01 09:05:00'),
  (@demo_assignment_2, @demo_kp_2, '2026-04-15 09:05:00')
ON DUPLICATE KEY UPDATE created_at = VALUES(created_at);

INSERT INTO major_assignment.exams (
  id, title, description, course_id, start_time, end_time, publish_date,
  is_active, is_online, location, duration, teacher_id
)
VALUES (
  @demo_exam_id,
  '分布式框架技术阶段测验',
  '覆盖服务注册与发现、网关路由、配置中心等核心知识点。',
  @demo_course_id,
  '2026-05-08 09:00:00',
  '2026-05-08 10:30:00',
  '2026-05-01 09:00:00',
  1,
  1,
  '线上考试平台',
  90,
  @demo_teacher_id
)
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  description = VALUES(description),
  course_id = VALUES(course_id),
  start_time = VALUES(start_time),
  end_time = VALUES(end_time),
  publish_date = VALUES(publish_date),
  is_active = VALUES(is_active),
  is_online = VALUES(is_online),
  location = VALUES(location),
  duration = VALUES(duration),
  teacher_id = VALUES(teacher_id);

INSERT INTO major_assignment.exam_classes (exam_id, class_id)
VALUES (@demo_exam_id, @demo_class_id)
ON DUPLICATE KEY UPDATE class_id = VALUES(class_id);

INSERT INTO major_assignment.exam_submissions (
  id, graded, score, submission_date, teacher_comment, content, time_taken, exam_id, student_id
)
VALUES
  (99001, 1, 86, '2026-05-08 10:12:00', '服务发现题回答准确，网关降级题还可更具体。', '阶段测验答题记录：李雨桐完成全部题目。', 72, @demo_exam_id, @demo_student_1),
  (99002, 1, 79, '2026-05-08 10:18:00', '配置中心题掌握较好，链路分析略欠完整。', '阶段测验答题记录：赵思涵完成全部题目。', 78, @demo_exam_id, @demo_student_2),
  (99003, 1, 58, '2026-05-08 10:29:00', '服务注册与发现基础概念需要复习。', '阶段测验答题记录：陈嘉宁完成大部分题目。', 89, @demo_exam_id, @demo_student_3)
ON DUPLICATE KEY UPDATE
  graded = VALUES(graded),
  score = VALUES(score),
  submission_date = VALUES(submission_date),
  teacher_comment = VALUES(teacher_comment),
  content = VALUES(content),
  time_taken = VALUES(time_taken),
  exam_id = VALUES(exam_id),
  student_id = VALUES(student_id);

INSERT INTO major_assignment.exam_knowledge_points (exam_id, knowledge_point_id)
VALUES
  (@demo_exam_id, @demo_kp_1),
  (@demo_exam_id, @demo_kp_2),
  (@demo_exam_id, @demo_kp_3)
ON DUPLICATE KEY UPDATE knowledge_point_id = VALUES(knowledge_point_id);

INSERT INTO major_assignment.knowledge_mastery (student_id, knowledge_point_id, mastery_level, last_assessed_date, update_time)
VALUES
  (@demo_student_1, @demo_kp_1, '优秀', '2026-05-08 11:00:00', '2026-05-08 11:00:00'),
  (@demo_student_1, @demo_kp_2, '良好', '2026-05-08 11:00:00', '2026-05-08 11:00:00'),
  (@demo_student_1, @demo_kp_3, '良好', '2026-05-08 11:00:00', '2026-05-08 11:00:00'),
  (@demo_student_2, @demo_kp_1, '良好', '2026-05-08 11:00:00', '2026-05-08 11:00:00'),
  (@demo_student_2, @demo_kp_2, '中等', '2026-05-08 11:00:00', '2026-05-08 11:00:00'),
  (@demo_student_2, @demo_kp_3, '良好', '2026-05-08 11:00:00', '2026-05-08 11:00:00'),
  (@demo_student_3, @demo_kp_1, '需加强', '2026-05-08 11:00:00', '2026-05-08 11:00:00'),
  (@demo_student_3, @demo_kp_2, '需加强', '2026-05-08 11:00:00', '2026-05-08 11:00:00'),
  (@demo_student_4, @demo_kp_1, '中等', '2026-05-08 11:00:00', '2026-05-08 11:00:00')
ON DUPLICATE KEY UPDATE
  mastery_level = VALUES(mastery_level),
  last_assessed_date = VALUES(last_assessed_date),
  update_time = VALUES(update_time);

INSERT INTO sc_exam.exams (
  id, title, description, course_id, teacher_id, start_time, end_time, publish_date,
  duration, total_score, is_active, is_online, location, created_at, updated_at
)
VALUES (
  @demo_exam_id,
  '分布式框架技术阶段测验',
  '覆盖服务注册与发现、网关路由、配置中心等核心知识点。',
  @demo_course_id,
  @demo_teacher_id,
  '2026-05-08 09:00:00',
  '2026-05-08 10:30:00',
  '2026-05-01 09:00:00',
  90,
  100,
  1,
  1,
  '线上考试平台',
  '2026-05-01 09:00:00',
  '2026-05-08 11:00:00'
)
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  description = VALUES(description),
  course_id = VALUES(course_id),
  teacher_id = VALUES(teacher_id),
  start_time = VALUES(start_time),
  end_time = VALUES(end_time),
  publish_date = VALUES(publish_date),
  duration = VALUES(duration),
  total_score = VALUES(total_score),
  is_active = VALUES(is_active),
  is_online = VALUES(is_online),
  location = VALUES(location),
  updated_at = VALUES(updated_at);

INSERT INTO sc_exam.exam_classes (exam_id, class_id)
VALUES (@demo_exam_id, @demo_class_id)
ON DUPLICATE KEY UPDATE class_id = VALUES(class_id);

INSERT INTO sc_exam.exam_submissions (
  id, exam_id, student_id, answers_json, content, submission_date,
  time_taken, graded, score, teacher_comment, created_at, updated_at
)
VALUES
  (99001, @demo_exam_id, @demo_student_1, JSON_OBJECT('student', '李雨桐', 'summary', '完成全部题目'), '阶段测验答题记录：李雨桐完成全部题目。', '2026-05-08 10:12:00', 72, 1, 86, '服务发现题回答准确，网关降级题还可更具体。', '2026-05-08 10:12:00', '2026-05-08 11:00:00'),
  (99002, @demo_exam_id, @demo_student_2, JSON_OBJECT('student', '赵思涵', 'summary', '完成全部题目'), '阶段测验答题记录：赵思涵完成全部题目。', '2026-05-08 10:18:00', 78, 1, 79, '配置中心题掌握较好，链路分析略欠完整。', '2026-05-08 10:18:00', '2026-05-08 11:00:00'),
  (99003, @demo_exam_id, @demo_student_3, JSON_OBJECT('student', '陈嘉宁', 'summary', '完成大部分题目'), '阶段测验答题记录：陈嘉宁完成大部分题目。', '2026-05-08 10:29:00', 89, 1, 58, '服务注册与发现基础概念需要复习。', '2026-05-08 10:29:00', '2026-05-08 11:00:00')
ON DUPLICATE KEY UPDATE
  exam_id = VALUES(exam_id),
  student_id = VALUES(student_id),
  answers_json = VALUES(answers_json),
  content = VALUES(content),
  submission_date = VALUES(submission_date),
  time_taken = VALUES(time_taken),
  graded = VALUES(graded),
  score = VALUES(score),
  teacher_comment = VALUES(teacher_comment),
  updated_at = VALUES(updated_at);

INSERT INTO sc_exam.exam_knowledge_points (exam_id, knowledge_point_id, created_at)
VALUES
  (@demo_exam_id, @demo_kp_1, '2026-05-01 09:05:00'),
  (@demo_exam_id, @demo_kp_2, '2026-05-01 09:05:00'),
  (@demo_exam_id, @demo_kp_3, '2026-05-01 09:05:00')
ON DUPLICATE KEY UPDATE created_at = VALUES(created_at);

INSERT INTO major_assignment.student_performance (
  id, student_id, average_score, pending_assignments, overall_progress, created_at, updated_at
)
VALUES
  (99101, @demo_student_1, 91, 0, 94, '2026-05-08 11:20:00', '2026-05-08 11:20:00'),
  (99102, @demo_student_2, 83, 0, 86, '2026-05-08 11:20:00', '2026-05-08 11:20:00'),
  (99103, @demo_student_3, 65, 1, 68, '2026-05-08 11:20:00', '2026-05-08 11:20:00'),
  (99104, @demo_student_4, 81, 1, 78, '2026-05-08 11:20:00', '2026-05-08 11:20:00')
ON DUPLICATE KEY UPDATE
  average_score = VALUES(average_score),
  pending_assignments = VALUES(pending_assignments),
  overall_progress = VALUES(overall_progress),
  updated_at = VALUES(updated_at);

INSERT INTO major_assignment.notifications (
  id, student_id, teacher_id, type, title, content, related_id, is_read, created_at
)
VALUES
  (99201, @demo_student_1, @demo_teacher_id, 'assignment', '新作业发布：服务注册与发现', '王明远老师发布了分布式框架技术作业：服务注册与发现，请按时提交。', @demo_assignment_1, 0, '2026-04-01 09:10:00'),
  (99202, @demo_student_2, @demo_teacher_id, 'exam', '阶段测验安排提醒', '分布式框架技术阶段测验将于 2026-05-08 09:00 开始，请提前进入系统。', @demo_exam_id, 0, '2026-05-01 09:15:00'),
  (99203, @demo_student_3, @demo_teacher_id, 'system', '系统通知：学习数据已更新', '系统已同步你的作业、测验与知识点掌握数据，请查看学习分析页面。', NULL, 1, '2026-05-08 11:30:00'),
  (99204, @demo_student_4, @demo_teacher_id, 'course', '课程资料更新', '分布式框架技术课程新增网关路由案例资料，请及时学习。', @demo_course_id, 0, '2026-04-18 10:00:00')
ON DUPLICATE KEY UPDATE
  student_id = VALUES(student_id),
  teacher_id = VALUES(teacher_id),
  type = VALUES(type),
  title = VALUES(title),
  content = VALUES(content),
  related_id = VALUES(related_id),
  is_read = VALUES(is_read),
  created_at = VALUES(created_at);

INSERT INTO sc_notification.notifications (
  id, student_id, teacher_id, type, title, content, related_id, is_read, created_at
)
VALUES
  (99201, @demo_student_1, @demo_teacher_id, 'assignment', '新作业发布：服务注册与发现', '王明远老师发布了分布式框架技术作业：服务注册与发现，请按时提交。', @demo_assignment_1, b'0', '2026-04-01 09:10:00.000000'),
  (99202, @demo_student_2, @demo_teacher_id, 'exam', '阶段测验安排提醒', '分布式框架技术阶段测验将于 2026-05-08 09:00 开始，请提前进入系统。', @demo_exam_id, b'0', '2026-05-01 09:15:00.000000'),
  (99203, @demo_student_3, @demo_teacher_id, 'system', '系统通知：学习数据已更新', '系统已同步你的作业、测验与知识点掌握数据，请查看学习分析页面。', NULL, b'1', '2026-05-08 11:30:00.000000'),
  (99204, @demo_student_4, @demo_teacher_id, 'course', '课程资料更新', '分布式框架技术课程新增网关路由案例资料，请及时学习。', @demo_course_id, b'0', '2026-04-18 10:00:00.000000')
ON DUPLICATE KEY UPDATE
  student_id = VALUES(student_id),
  teacher_id = VALUES(teacher_id),
  type = VALUES(type),
  title = VALUES(title),
  content = VALUES(content),
  related_id = VALUES(related_id),
  is_read = VALUES(is_read),
  created_at = VALUES(created_at);

INSERT INTO major_assignment.early_warnings (
  id, assessment_type, related_assessment_id, is_resolved, resolved_by, resolved_date,
  resolved_note, trigger_date, warning_level, warning_message, warning_type,
  course_id, student_id, teacher_id
)
VALUES
  (99301, 'assignment', @demo_assignment_2, b'0', NULL, NULL, NULL, '2026-04-27 10:15:00.000000', '高', '连续两次作业低于及格线附近，请尽快安排一对一辅导。', '作业成绩预警', @demo_course_id, @demo_student_3, @demo_teacher_id),
  (99302, 'exam', @demo_exam_id, b'1', @demo_teacher_id, '2026-05-09 15:00:00.000000', '已安排服务注册与发现专题补练。', '2026-05-08 11:10:00.000000', '中等', '阶段测验服务注册与发现题得分偏低。', '测验成绩预警', @demo_course_id, @demo_student_3, @demo_teacher_id)
ON DUPLICATE KEY UPDATE
  assessment_type = VALUES(assessment_type),
  related_assessment_id = VALUES(related_assessment_id),
  is_resolved = VALUES(is_resolved),
  resolved_by = VALUES(resolved_by),
  resolved_date = VALUES(resolved_date),
  resolved_note = VALUES(resolved_note),
  trigger_date = VALUES(trigger_date),
  warning_level = VALUES(warning_level),
  warning_message = VALUES(warning_message),
  warning_type = VALUES(warning_type),
  course_id = VALUES(course_id),
  student_id = VALUES(student_id),
  teacher_id = VALUES(teacher_id);

INSERT INTO sc_analysis.early_warnings (
  id, student_id, course_id, class_id, teacher_id, warning_type, warning_level,
  warning_message, trigger_date, is_resolved, resolved_by, resolved_date,
  resolved_note, assessment_type, related_assessment_id, student_name, course_name,
  created_at, updated_at
)
VALUES
  (99301, @demo_student_3, @demo_course_id, @demo_class_id, @demo_teacher_id, '作业成绩预警', '高', '连续两次作业低于及格线附近，请尽快安排一对一辅导。', '2026-04-27 10:15:00.000000', 0, NULL, NULL, NULL, 'assignment', @demo_assignment_2, '陈嘉宁', '分布式框架技术', '2026-04-27 10:15:00.000000', '2026-04-27 10:15:00.000000'),
  (99302, @demo_student_3, @demo_course_id, @demo_class_id, @demo_teacher_id, '测验成绩预警', '中等', '阶段测验服务注册与发现题得分偏低。', '2026-05-08 11:10:00.000000', 1, @demo_teacher_id, '2026-05-09 15:00:00.000000', '已安排服务注册与发现专题补练。', 'exam', @demo_exam_id, '陈嘉宁', '分布式框架技术', '2026-05-08 11:10:00.000000', '2026-05-09 15:00:00.000000')
ON DUPLICATE KEY UPDATE
  student_id = VALUES(student_id),
  course_id = VALUES(course_id),
  class_id = VALUES(class_id),
  teacher_id = VALUES(teacher_id),
  warning_type = VALUES(warning_type),
  warning_level = VALUES(warning_level),
  warning_message = VALUES(warning_message),
  trigger_date = VALUES(trigger_date),
  is_resolved = VALUES(is_resolved),
  resolved_by = VALUES(resolved_by),
  resolved_date = VALUES(resolved_date),
  resolved_note = VALUES(resolved_note),
  assessment_type = VALUES(assessment_type),
  related_assessment_id = VALUES(related_assessment_id),
  student_name = VALUES(student_name),
  course_name = VALUES(course_name),
  updated_at = VALUES(updated_at);

INSERT INTO sc_analysis.kp_mastery (
  id, student_id, course_id, class_id, knowledge_point_id, mastery_score,
  evidence_count, last_source_type, last_source_id, last_event_id, updated_at
)
VALUES
  (99501, @demo_student_1, @demo_course_id, @demo_class_id, @demo_kp_1, 0.9200, 3, 'assignment', @demo_assignment_1, 'seed-cn-99501', '2026-05-08 11:00:00.000000'),
  (99502, @demo_student_1, @demo_course_id, @demo_class_id, @demo_kp_2, 0.8600, 2, 'exam', @demo_exam_id, 'seed-cn-99502', '2026-05-08 11:00:00.000000'),
  (99503, @demo_student_2, @demo_course_id, @demo_class_id, @demo_kp_1, 0.8200, 2, 'assignment', @demo_assignment_1, 'seed-cn-99503', '2026-05-08 11:00:00.000000'),
  (99504, @demo_student_3, @demo_course_id, @demo_class_id, @demo_kp_1, 0.5600, 2, 'exam', @demo_exam_id, 'seed-cn-99504', '2026-05-08 11:00:00.000000')
ON DUPLICATE KEY UPDATE
  student_id = VALUES(student_id),
  course_id = VALUES(course_id),
  class_id = VALUES(class_id),
  knowledge_point_id = VALUES(knowledge_point_id),
  mastery_score = VALUES(mastery_score),
  evidence_count = VALUES(evidence_count),
  last_source_type = VALUES(last_source_type),
  last_source_id = VALUES(last_source_id),
  last_event_id = VALUES(last_event_id),
  updated_at = VALUES(updated_at);

INSERT INTO sc_analysis.score_trends (
  id, student_id, course_id, class_id, source_type, source_id, submission_id,
  score, max_score, score_rate, occurred_at, created_at
)
VALUES
  (99601, @demo_student_1, @demo_course_id, @demo_class_id, 'assignment', @demo_assignment_1, 98001, 92, 100, 0.9200, '2026-04-10 09:00:00.000000', '2026-04-10 09:00:00.000000'),
  (99602, @demo_student_1, @demo_course_id, @demo_class_id, 'exam', @demo_exam_id, 99001, 86, 100, 0.8600, '2026-05-08 11:00:00.000000', '2026-05-08 11:00:00.000000'),
  (99603, @demo_student_3, @demo_course_id, @demo_class_id, 'assignment', @demo_assignment_2, 98006, 68, 100, 0.6800, '2026-04-27 10:00:00.000000', '2026-04-27 10:00:00.000000'),
  (99604, @demo_student_3, @demo_course_id, @demo_class_id, 'exam', @demo_exam_id, 99003, 58, 100, 0.5800, '2026-05-08 11:00:00.000000', '2026-05-08 11:00:00.000000')
ON DUPLICATE KEY UPDATE
  student_id = VALUES(student_id),
  course_id = VALUES(course_id),
  class_id = VALUES(class_id),
  source_type = VALUES(source_type),
  source_id = VALUES(source_id),
  submission_id = VALUES(submission_id),
  score = VALUES(score),
  max_score = VALUES(max_score),
  score_rate = VALUES(score_rate),
  occurred_at = VALUES(occurred_at);

INSERT INTO major_assignment.audit_logs (
  id, operator_id, operation_type, table_name, record_id, old_data, new_data,
  ip_address, operation_time
)
VALUES
  (99401, @demo_teacher_id, '发布作业', 'assignments', @demo_assignment_1, NULL, JSON_OBJECT('title', '分布式框架技术作业：服务注册与发现', 'teacher', '王明远'), '127.0.0.1', '2026-04-01 09:10:00.000000'),
  (99402, @demo_teacher_id, '发布考试', 'exams', @demo_exam_id, NULL, JSON_OBJECT('title', '分布式框架技术阶段测验', 'teacher', '王明远'), '127.0.0.1', '2026-05-01 09:10:00.000000'),
  (99403, @demo_student_1, '提交作业', 'assignment_submissions', 98001, NULL, JSON_OBJECT('student', '李雨桐', 'assignment', '服务注册与发现'), '127.0.0.1', '2026-04-09 20:10:00.000000')
ON DUPLICATE KEY UPDATE
  operator_id = VALUES(operator_id),
  operation_type = VALUES(operation_type),
  table_name = VALUES(table_name),
  record_id = VALUES(record_id),
  old_data = VALUES(old_data),
  new_data = VALUES(new_data),
  ip_address = VALUES(ip_address),
  operation_time = VALUES(operation_time);

-- seed-more-chinese-data:end
