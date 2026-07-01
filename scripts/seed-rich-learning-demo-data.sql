-- Rich demo data for teacher student dashboard and student learning progress.
-- Safe to rerun. Targets the local development seed around teacher 7, class 90209.

SET @teacher_id := 7;
SET @class_id := 90209;
SET @base_time := NOW(6);

INSERT INTO sc_user.users (id, username, name, email, enabled)
VALUES
  (93021, 'student_demo_93021', _utf8mb4 0xE99988E6809DE8BF9C, 'student93021@example.test', 1),
  (93022, 'student_demo_93022', _utf8mb4 0xE69D8EE88BA5E6BAAA, 'student93022@example.test', 1),
  (93023, 'student_demo_93023', _utf8mb4 0xE78E8BE5AD90E6B6B5, 'student93023@example.test', 1),
  (93024, 'student_demo_93024', _utf8mb4 0xE8B5B5E6988EE8BDA9, 'student93024@example.test', 1),
  (93025, 'student_demo_93025', _utf8mb4 0xE58898E59889E5AE81, 'student93025@example.test', 1),
  (93026, 'student_demo_93026', _utf8mb4 0xE591A8E99BA8E6A190, 'student93026@example.test', 1),
  (93027, 'student_demo_93027', _utf8mb4 0xE590B4E6B5A9E784B6, 'student93027@example.test', 1),
  (93028, 'student_demo_93028', _utf8mb4 0xE5AD99E4B880E8AFBA, 'student93028@example.test', 1)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  email = VALUES(email),
  enabled = VALUES(enabled);

INSERT IGNORE INTO sc_user.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM sc_user.users u
JOIN sc_user.roles r ON r.name = 'STUDENT'
WHERE u.id IN (93021, 93022, 93023, 93024, 93025, 93026, 93027, 93028);

INSERT INTO sc_course.class_students (class_id, student_id)
VALUES
  (@class_id, 42),
  (@class_id, 93021), (@class_id, 93022), (@class_id, 93023), (@class_id, 93024),
  (@class_id, 93025), (@class_id, 93026), (@class_id, 93027), (@class_id, 93028)
ON DUPLICATE KEY UPDATE student_id = VALUES(student_id);

INSERT INTO sc_course.teacher_knowledge_points (id, course_id, point_name, order_index)
VALUES
  (9110101, 91001, _utf8mb4 0xE5BEAEE69C8DE58AA1E69EB6E69E84E8AEBEE8AEA1, 1),
  (9110102, 91001, _utf8mb4 0xE69C8DE58AA1E6B3A8E5868CE4B88EE58F91E78EB0, 2),
  (9110201, 91002, _utf8mb4 0xE9858DE7BDAEE4B8ADE5BF83E4B88EE78EAFE5A283E99A94E7A6BB, 1),
  (9110202, 91002, _utf8mb4 0xE7BD91E585B3E8B7AFE794B1E4B88EE989B4E69D83, 2),
  (9110301, 91003, _utf8mb4 0xE6B688E681AFE9989FE58897E58FAFE99DA0E68A95E98092, 1),
  (9110302, 91003, _utf8mb4 0xE5BC82E6ADA5E4BA8BE4BBB6E5A484E79086, 2),
  (9110401, 91004, _utf8mb4 0xE5AEB9E599A8E58C96E983A8E7BDB2E5AE9EE8B7B5, 1),
  (9110402, 91004, _utf8mb4 0xE69C8DE58AA1E79B91E68EA7E4B88EE68E92E99A9C, 2),
  (174, 91005, '云服务安全与治理', 1),
  (9110501, 91005, _utf8mb4 0xE4BA91E8AEA1E7AE97E8B584E6BA90E7AEA1E79086, 2),
  (9110502, 91005, _utf8mb4 0xE5BCB9E680A7E4BCB8E7BCA9E7AD96E795A5, 3)
ON DUPLICATE KEY UPDATE
  point_name = VALUES(point_name),
  order_index = VALUES(order_index);

-- demo-student42-course-descriptions: keep student42 course cards from rendering empty descriptions.
UPDATE sc_course.courses
SET description = '面向 Spring Cloud 微服务、配置中心、网关路由、服务治理与部署排障的综合实训课程。'
WHERE id = 91010
  AND (description IS NULL OR TRIM(description) = '');

INSERT INTO sc_assignment.assignments (
  id, title, description, course_id, due_date, publish_date, is_active,
  teacher_id, max_score, submission_count, graded_count, status, total_students
)
VALUES
  (9930101, '微服务架构建模作业', '完成服务拆分与接口建模。', 91001, DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 20 DAY), '%Y-%m-%d 23:59:59'), DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 28 DAY), '%Y-%m-%d 08:00:00'), 1, @teacher_id, 100, 9, 9, 'graded', 9),
  (9930102, '注册中心实验报告', '提交服务注册与发现实验记录。', 91001, DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 10 DAY), '%Y-%m-%d 23:59:59'), DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 18 DAY), '%Y-%m-%d 08:00:00'), 1, @teacher_id, 100, 8, 8, 'graded', 9),
  (9930201, '配置中心分环境实践', '完成 dev/test/prod 配置拆分。', 91002, DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 18 DAY), '%Y-%m-%d 23:59:59'), DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 24 DAY), '%Y-%m-%d 08:00:00'), 1, @teacher_id, 100, 8, 8, 'graded', 9),
  (9930202, '网关鉴权规则设计', '完成网关路由和权限控制设计。', 91002, DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY), '%Y-%m-%d 23:59:59'), DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 14 DAY), '%Y-%m-%d 08:00:00'), 1, @teacher_id, 100, 7, 7, 'graded', 9),
  (9930301, '消息队列可靠投递实验', '完成消息确认与重试机制。', 91003, DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 16 DAY), '%Y-%m-%d 23:59:59'), DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 22 DAY), '%Y-%m-%d 08:00:00'), 1, @teacher_id, 100, 8, 8, 'graded', 9),
  (9930302, '异步事件业务设计', '设计作业批改后的事件链路。', 91003, DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 5 DAY), '%Y-%m-%d 23:59:59'), DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 12 DAY), '%Y-%m-%d 08:00:00'), 1, @teacher_id, 100, 6, 6, 'graded', 9),
  (9930401, 'Docker Compose 部署作业', '完成本地基础设施编排。', 91004, DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 12 DAY), '%Y-%m-%d 23:59:59'), DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 19 DAY), '%Y-%m-%d 08:00:00'), 1, @teacher_id, 100, 8, 8, 'graded', 9),
  (9930402, '服务监控排障记录', '记录一次端口和服务排障过程。', 91004, DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 3 DAY), '%Y-%m-%d 23:59:59'), DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 9 DAY), '%Y-%m-%d 08:00:00'), 1, @teacher_id, 100, 5, 5, 'graded', 9),
  (9930501, '云计算资源管理案例', '分析课程项目的资源分配。', 91005, DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 14 DAY), '%Y-%m-%d 23:59:59'), DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 21 DAY), '%Y-%m-%d 08:00:00'), 1, @teacher_id, 100, 9, 9, 'graded', 9),
  (9930502, '弹性伸缩策略设计', '设计云服务伸缩策略。', 91005, DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 2 DAY), '%Y-%m-%d 23:59:59'), DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 8 DAY), '%Y-%m-%d 08:00:00'), 1, @teacher_id, 100, 6, 6, 'graded', 9)
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  description = VALUES(description),
  course_id = VALUES(course_id),
  due_date = VALUES(due_date),
  publish_date = VALUES(publish_date),
  teacher_id = VALUES(teacher_id),
  max_score = VALUES(max_score),
  submission_count = VALUES(submission_count),
  graded_count = VALUES(graded_count),
  status = VALUES(status),
  total_students = VALUES(total_students);

INSERT INTO sc_assignment.assignment_classes (assignment_id, class_id)
SELECT id, @class_id
FROM sc_assignment.assignments
WHERE id BETWEEN 9930101 AND 9930502
ON DUPLICATE KEY UPDATE class_id = VALUES(class_id);

INSERT INTO sc_assignment.assignment_knowledge_points (assignment_id, knowledge_point_id)
VALUES
  (9930101, 9110101), (9930102, 9110102),
  (9930201, 9110201), (9930202, 9110202),
  (9930301, 9110301), (9930302, 9110302),
  (9930401, 9110401), (9930402, 9110402),
  (9930501, 174), (9930501, 9110501), (9930502, 9110502)
ON DUPLICATE KEY UPDATE knowledge_point_id = VALUES(knowledge_point_id);

INSERT INTO sc_assignment.assignment_submissions (
  id, assignment_id, student_id, content, submission_date, graded, is_late, late_penalty, score, teacher_comment
)
SELECT
  assignment_id * 100 + student_id % 100,
  assignment_id,
  student_id,
  CONCAT('演示提交：', assignment_id, '-', student_id),
  DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL day_offset DAY), '%Y-%m-%d 20:30:00'),
  1,
  0,
  0,
  score,
  '已批改，演示数据'
FROM (
  SELECT 9930101 assignment_id, 42 student_id, 88 score, 20 day_offset UNION ALL
  SELECT 9930101, 93021, 94, 20 UNION ALL SELECT 9930101, 93022, 86, 20 UNION ALL SELECT 9930101, 93023, 76, 20 UNION ALL SELECT 9930101, 93024, 68, 20 UNION ALL SELECT 9930101, 93025, 91, 20 UNION ALL SELECT 9930101, 93026, 83, 20 UNION ALL SELECT 9930101, 93027, 72, 20 UNION ALL SELECT 9930101, 93028, 58, 20 UNION ALL
  SELECT 9930102, 42, 84, 10 UNION ALL SELECT 9930102, 93021, 92, 10 UNION ALL SELECT 9930102, 93022, 81, 10 UNION ALL SELECT 9930102, 93023, 79, 10 UNION ALL SELECT 9930102, 93024, 64, 10 UNION ALL SELECT 9930102, 93025, 88, 10 UNION ALL SELECT 9930102, 93026, 85, 10 UNION ALL SELECT 9930102, 93027, 74, 10 UNION ALL
  SELECT 9930201, 42, 91, 18 UNION ALL SELECT 9930201, 93021, 96, 18 UNION ALL SELECT 9930201, 93022, 87, 18 UNION ALL SELECT 9930201, 93023, 73, 18 UNION ALL SELECT 9930201, 93024, 66, 18 UNION ALL SELECT 9930201, 93025, 90, 18 UNION ALL SELECT 9930201, 93026, 80, 18 UNION ALL SELECT 9930201, 93028, 62, 18 UNION ALL
  SELECT 9930202, 42, 78, 7 UNION ALL SELECT 9930202, 93021, 89, 7 UNION ALL SELECT 9930202, 93022, 84, 7 UNION ALL SELECT 9930202, 93023, 75, 7 UNION ALL SELECT 9930202, 93025, 86, 7 UNION ALL SELECT 9930202, 93026, 82, 7 UNION ALL SELECT 9930202, 93027, 70, 7 UNION ALL
  SELECT 9930301, 42, 86, 16 UNION ALL SELECT 9930301, 93021, 93, 16 UNION ALL SELECT 9930301, 93022, 88, 16 UNION ALL SELECT 9930301, 93023, 71, 16 UNION ALL SELECT 9930301, 93024, 61, 16 UNION ALL SELECT 9930301, 93025, 85, 16 UNION ALL SELECT 9930301, 93026, 79, 16 UNION ALL SELECT 9930301, 93028, 55, 16 UNION ALL
  SELECT 9930302, 42, 82, 5 UNION ALL SELECT 9930302, 93021, 90, 5 UNION ALL SELECT 9930302, 93022, 80, 5 UNION ALL SELECT 9930302, 93023, 69, 5 UNION ALL SELECT 9930302, 93025, 87, 5 UNION ALL SELECT 9930302, 93027, 73, 5 UNION ALL
  SELECT 9930401, 42, 89, 12 UNION ALL SELECT 9930401, 93021, 95, 12 UNION ALL SELECT 9930401, 93022, 83, 12 UNION ALL SELECT 9930401, 93023, 77, 12 UNION ALL SELECT 9930401, 93024, 65, 12 UNION ALL SELECT 9930401, 93025, 88, 12 UNION ALL SELECT 9930401, 93026, 81, 12 UNION ALL SELECT 9930401, 93028, 60, 12 UNION ALL
  SELECT 9930402, 42, 80, 3 UNION ALL SELECT 9930402, 93021, 91, 3 UNION ALL SELECT 9930402, 93022, 82, 3 UNION ALL SELECT 9930402, 93023, 74, 3 UNION ALL SELECT 9930402, 93025, 84, 3 UNION ALL
  SELECT 9930501, 42, 90, 14 UNION ALL SELECT 9930501, 93021, 97, 14 UNION ALL SELECT 9930501, 93022, 85, 14 UNION ALL SELECT 9930501, 93023, 78, 14 UNION ALL SELECT 9930501, 93024, 63, 14 UNION ALL SELECT 9930501, 93025, 92, 14 UNION ALL SELECT 9930501, 93026, 84, 14 UNION ALL SELECT 9930501, 93027, 75, 14 UNION ALL SELECT 9930501, 93028, 59, 14 UNION ALL
  SELECT 9930502, 42, 76, 2 UNION ALL SELECT 9930502, 93021, 88, 2 UNION ALL SELECT 9930502, 93022, 79, 2 UNION ALL SELECT 9930502, 93023, 70, 2 UNION ALL SELECT 9930502, 93025, 86, 2 UNION ALL SELECT 9930502, 93027, 67, 2
) seeded
ON DUPLICATE KEY UPDATE
  content = VALUES(content),
  submission_date = VALUES(submission_date),
  graded = VALUES(graded),
  score = VALUES(score),
  teacher_comment = VALUES(teacher_comment);

INSERT INTO sc_analysis.score_trends (
  student_id, course_id, class_id, source_type, source_id, submission_id,
  score, max_score, score_rate, occurred_at
)
SELECT
  s.student_id,
  a.course_id,
  @class_id,
  'assignment',
  a.id,
  s.id,
  s.score,
  COALESCE(a.max_score, 100),
  ROUND(s.score / COALESCE(a.max_score, 100), 4),
  STR_TO_DATE(s.submission_date, '%Y-%m-%d %H:%i:%s')
FROM sc_assignment.assignment_submissions s
JOIN sc_assignment.assignments a ON a.id = s.assignment_id
WHERE a.id BETWEEN 9930101 AND 9930502
  AND s.graded = 1
  AND s.score IS NOT NULL
ON DUPLICATE KEY UPDATE
  course_id = VALUES(course_id),
  class_id = VALUES(class_id),
  score = VALUES(score),
  max_score = VALUES(max_score),
  score_rate = VALUES(score_rate),
  occurred_at = VALUES(occurred_at);

-- demo-student42-recent-study-time: keep the dashboard's 7-day study-time chart nonzero.
INSERT INTO sc_analysis.score_trends (
  student_id, course_id, class_id, source_type, source_id, submission_id,
  score, max_score, score_rate, occurred_at
)
SELECT
  42,
  course_id,
  @class_id,
  source_type,
  source_id,
  submission_id,
  score,
  100,
  ROUND(score / 100, 4),
  DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL day_offset DAY), '%Y-%m-%d 19:30:00')
FROM (
  SELECT 9974201 submission_id, 9930101 source_id, 91001 course_id, 'assignment' source_type, 88 score, 6 day_offset UNION ALL
  SELECT 9974202, 90271, 91002, 'exam', 82, 5 UNION ALL
  SELECT 9974203, 9930301, 91003, 'assignment', 86, 4 UNION ALL
  SELECT 9974204, 90273, 91004, 'exam', 91, 3 UNION ALL
  SELECT 9974205, 9930501, 91005, 'assignment', 90, 2 UNION ALL
  SELECT 9974206, 9930202, 91002, 'assignment', 78, 1 UNION ALL
  SELECT 9974207, 90278, 91005, 'exam', 84, 0
) demo_student42_recent_study_time
ON DUPLICATE KEY UPDATE
  course_id = VALUES(course_id),
  class_id = VALUES(class_id),
  source_type = VALUES(source_type),
  source_id = VALUES(source_id),
  score = VALUES(score),
  max_score = VALUES(max_score),
  score_rate = VALUES(score_rate),
  occurred_at = VALUES(occurred_at);

INSERT INTO sc_analysis.kp_mastery (
  student_id, course_id, class_id, knowledge_point_id, mastery_score,
  evidence_count, last_source_type, last_source_id, last_event_id, updated_at
)
SELECT
  agg.student_id,
  agg.course_id,
  @class_id,
  kp.knowledge_point_id,
  ROUND(agg.avg_score / 100, 4),
  agg.evidence_count,
  'assignment',
  agg.last_assignment_id,
  CONCAT('demo-rich-learning-', agg.student_id, '-', agg.course_id, '-', kp.knowledge_point_id),
  DATE_SUB(@base_time, INTERVAL agg.course_rank DAY)
FROM (
  SELECT
    s.student_id,
    a.course_id,
    AVG(s.score) AS avg_score,
    COUNT(*) AS evidence_count,
    MAX(a.id) AS last_assignment_id,
    ROW_NUMBER() OVER (PARTITION BY s.student_id ORDER BY a.course_id) AS course_rank
  FROM sc_assignment.assignment_submissions s
  JOIN sc_assignment.assignments a ON a.id = s.assignment_id
  WHERE a.id BETWEEN 9930101 AND 9930502
    AND s.graded = 1
    AND s.score IS NOT NULL
  GROUP BY s.student_id, a.course_id
) agg
JOIN (
  SELECT 91001 AS course_id, 9110101 AS knowledge_point_id UNION ALL
  SELECT 91001, 9110102 UNION ALL
  SELECT 91002, 9110201 UNION ALL
  SELECT 91002, 9110202 UNION ALL
  SELECT 91003, 9110301 UNION ALL
  SELECT 91003, 9110302 UNION ALL
  SELECT 91004, 9110401 UNION ALL
  SELECT 91004, 9110402 UNION ALL
  SELECT 91005, 174 UNION ALL
  SELECT 91005, 9110501 UNION ALL
  SELECT 91005, 9110502
) kp ON kp.course_id = agg.course_id
ON DUPLICATE KEY UPDATE
  class_id = VALUES(class_id),
  mastery_score = VALUES(mastery_score),
  evidence_count = VALUES(evidence_count),
  last_source_type = VALUES(last_source_type),
  last_source_id = VALUES(last_source_id),
  last_event_id = VALUES(last_event_id),
  updated_at = VALUES(updated_at);

UPDATE sc_analysis.kp_mastery
SET updated_at = DATE_SUB(@base_time, INTERVAL 1 DAY),
    evidence_count = GREATEST(evidence_count, 2)
WHERE student_id = 42
  AND class_id = @class_id;

INSERT INTO major_assignment.knowledge_mastery (student_id, knowledge_point_id, mastery_level, last_assessed_date)
SELECT
  student_id,
  knowledge_point_id,
  CASE
    WHEN mastery_score >= 0.85 THEN '优秀'
    WHEN mastery_score >= 0.70 THEN '良好'
    WHEN mastery_score >= 0.60 THEN '一般'
    ELSE '较差'
  END,
  NOW()
FROM sc_analysis.kp_mastery
WHERE class_id = @class_id
  AND knowledge_point_id IS NOT NULL
ON DUPLICATE KEY UPDATE
  mastery_level = VALUES(mastery_level),
  last_assessed_date = VALUES(last_assessed_date);

-- Extra pending tasks for student 42 so the student dashboard course progress chart is not flat.
INSERT INTO sc_assignment.assignments (
  id, title, description, course_id, due_date, publish_date, is_active,
  teacher_id, max_score, submission_count, graded_count, status, total_students
)
SELECT
  id,
  title,
  '用于学生端课程学习进度演示的未完成任务。',
  course_id,
  DATE_FORMAT(DATE_ADD(CURRENT_DATE(), INTERVAL due_offset DAY), '%Y-%m-%d 23:59:59'),
  DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 2 DAY), '%Y-%m-%d 08:00:00'),
  1,
  @teacher_id,
  100,
  0,
  0,
  'published',
  9
FROM (
  SELECT 9940101 id, '微服务架构扩展阅读' title, 91001 course_id, 5 due_offset UNION ALL
  SELECT 9940201, '云原生部署补充实验一', 91002, 6 UNION ALL
  SELECT 9940202, '云原生部署补充实验二', 91002, 9 UNION ALL
  SELECT 9940301, '接口测试补充任务一', 91003, 4 UNION ALL
  SELECT 9940302, '接口测试补充任务二', 91003, 8 UNION ALL
  SELECT 9940303, '接口测试补充任务三', 91003, 12 UNION ALL
  SELECT 9940401, '学习分析补充任务一', 91004, 5 UNION ALL
  SELECT 9940402, '学习分析补充任务二', 91004, 7 UNION ALL
  SELECT 9940403, '学习分析补充任务三', 91004, 10 UNION ALL
  SELECT 9940404, '学习分析补充任务四', 91004, 14 UNION ALL
  SELECT 9940501, '云计算技术补充任务一', 91005, 3 UNION ALL
  SELECT 9940502, '云计算技术补充任务二', 91005, 5 UNION ALL
  SELECT 9940503, '云计算技术补充任务三', 91005, 7 UNION ALL
  SELECT 9940504, '云计算技术补充任务四', 91005, 10 UNION ALL
  SELECT 9940505, '云计算技术补充任务五', 91005, 15
) pending_tasks
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  description = VALUES(description),
  course_id = VALUES(course_id),
  due_date = VALUES(due_date),
  publish_date = VALUES(publish_date),
  status = VALUES(status),
  total_students = VALUES(total_students);

INSERT INTO sc_assignment.assignment_classes (assignment_id, class_id)
SELECT id, @class_id
FROM sc_assignment.assignments
WHERE id BETWEEN 9940101 AND 9940505
ON DUPLICATE KEY UPDATE class_id = VALUES(class_id);
