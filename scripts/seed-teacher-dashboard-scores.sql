SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @teacher_id := 7;
SET @student_1 := 42;
SET @student_2 := 9211;
SET @student_3 := 9212;
SET @student_4 := 9213;
SET @student_5 := 9214;

INSERT INTO sc_user.users (id, username, name, email, phone, avatar, enabled, created_at, updated_at)
VALUES
  (@student_2, 'student_demo_01', '李雨桐', 'liyutong@example.com', '13992110001', 'student-avatar-demo-9211.png', 1, '2026-01-10 08:05:00', '2026-01-10 08:05:00'),
  (@student_3, 'student_demo_02', '赵思涵', 'zhaosihan@example.com', '13992110002', 'student-avatar-demo-9212.png', 1, '2026-01-10 08:06:00', '2026-01-10 08:06:00'),
  (@student_4, 'student_demo_03', '陈嘉宁', 'chenjianing@example.com', '13992110003', 'student-avatar-demo-9213.png', 1, '2026-01-10 08:07:00', '2026-01-10 08:07:00'),
  (@student_5, 'student_demo_04', '梁晓曼', 'liangxiaoman@example.com', '13992110004', 'student-avatar-demo-9214.png', 1, '2026-01-10 08:08:00', '2026-01-10 08:08:00')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  email = VALUES(email),
  phone = VALUES(phone),
  avatar = VALUES(avatar),
  enabled = VALUES(enabled),
  updated_at = VALUES(updated_at);

INSERT INTO sc_course.courses (
  id, course_name, course_code, description, credit, course_category, total_hours,
  teacher_id, course_director, assessment_method, course_status, semester,
  start_date, end_date, max_students, created_at, updated_at
)
VALUES
  (91001, '微服务架构设计', 'TDASH-MS-2026', '教师仪表盘平均分演示课程：微服务拆分、注册发现与网关治理。', 4, '专业课', 64, @teacher_id, @teacher_id, '项目实践', 'active', '第二学期', '2026-03-01', '2026-06-30', 45, '2026-06-08 09:00:00', '2026-06-08 09:00:00'),
  (91002, '云原生部署实践', 'TDASH-CN-2026', '教师仪表盘平均分演示课程：容器编排、配置管理与观测部署。', 3, '专业课', 48, @teacher_id, @teacher_id, '实验报告', 'active', '第二学期', '2026-03-01', '2026-06-30', 42, '2026-06-08 09:00:00', '2026-06-08 09:00:00'),
  (91003, '数据接口测试', 'TDASH-API-2026', '教师仪表盘平均分演示课程：接口契约、自动化测试与质量分析。', 3, '实践课', 48, @teacher_id, @teacher_id, '阶段测验', 'active', '第二学期', '2026-03-01', '2026-06-30', 40, '2026-06-08 09:00:00', '2026-06-08 09:00:00'),
  (91004, '智能学习分析', 'TDASH-LA-2026', '教师仪表盘平均分演示课程：学习行为、成绩趋势与预警分析。', 2, '选修课', 32, @teacher_id, @teacher_id, '课程论文', 'active', '第二学期', '2026-03-01', '2026-06-30', 38, '2026-06-08 09:00:00', '2026-06-08 09:00:00')
ON DUPLICATE KEY UPDATE
  course_name = VALUES(course_name),
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

INSERT INTO sc_assignment.assignments (
  id, title, description, course_id, due_date, publish_date, is_active,
  teacher_id, max_score, submission_count, graded_count, status, total_students,
  created_at, updated_at
)
VALUES
  (99101, '微服务架构设计：服务拆分实践', '根据学习辅助系统业务域完成服务拆分与接口契约说明。', 91001, '2026-06-10 23:59:00', '2026-06-03 09:00:00', 1, @teacher_id, 100, 5, 5, 'graded', 5, '2026-06-03 09:00:00', '2026-06-08 09:00:00'),
  (99102, '云原生部署实践：Docker Compose 编排', '完成多服务本地编排、健康检查和配置说明。', 91002, '2026-06-11 23:59:00', '2026-06-04 09:00:00', 1, @teacher_id, 100, 5, 5, 'graded', 5, '2026-06-04 09:00:00', '2026-06-08 09:00:00'),
  (99103, '数据接口测试：Gateway 冒烟脚本', '设计并运行统一网关接口冒烟测试，记录结果。', 91003, '2026-06-12 23:59:00', '2026-06-05 09:00:00', 1, @teacher_id, 100, 5, 5, 'graded', 5, '2026-06-05 09:00:00', '2026-06-08 09:00:00'),
  (99104, '智能学习分析：成绩趋势解读', '基于成绩趋势和预警记录撰写学习分析报告。', 91004, '2026-06-13 23:59:00', '2026-06-06 09:00:00', 1, @teacher_id, 100, 5, 5, 'graded', 5, '2026-06-06 09:00:00', '2026-06-08 09:00:00')
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

INSERT INTO sc_assignment.assignment_submissions (
  id, assignment_id, student_id, content, submission_date, graded, is_late,
  late_penalty, score, teacher_comment, created_at, updated_at
)
VALUES
  (9910101, 99101, @student_1, '完成服务边界拆分和接口清单。', '2026-06-04 19:20:00', 1, 0, 0, 92, '服务边界清晰，接口说明完整。', '2026-06-04 19:20:00', '2026-06-08 09:00:00'),
  (9910102, 99101, @student_2, '提交服务拆分图与调用链路。', '2026-06-04 20:10:00', 1, 0, 0, 86, '链路表达清楚。', '2026-06-04 20:10:00', '2026-06-08 09:00:00'),
  (9910103, 99101, @student_3, '完成注册中心与服务接口说明。', '2026-06-05 08:30:00', 1, 0, 0, 88, '配置说明较完整。', '2026-06-05 08:30:00', '2026-06-08 09:00:00'),
  (9910104, 99101, @student_4, '补充服务拆分风险分析。', '2026-06-05 09:45:00', 1, 0, 0, 80, '风险分析可以更深入。', '2026-06-05 09:45:00', '2026-06-08 09:00:00'),
  (9910105, 99101, @student_5, '提交服务治理改造说明。', '2026-06-05 11:00:00', 1, 0, 0, 90, '治理思路完整。', '2026-06-05 11:00:00', '2026-06-08 09:00:00'),
  (9910201, 99102, @student_1, '完成 Compose 编排和健康检查截图。', '2026-06-05 18:20:00', 1, 0, 0, 78, '健康检查截图清晰。', '2026-06-05 18:20:00', '2026-06-08 09:00:00'),
  (9910202, 99102, @student_2, '提交容器编排说明与日志。', '2026-06-05 19:10:00', 1, 0, 0, 84, '日志记录完整。', '2026-06-05 19:10:00', '2026-06-08 09:00:00'),
  (9910203, 99102, @student_3, '完成配置中心与容器环境变量说明。', '2026-06-06 08:30:00', 1, 0, 0, 81, '环境变量说明准确。', '2026-06-06 08:30:00', '2026-06-08 09:00:00'),
  (9910204, 99102, @student_4, '补充部署故障排查步骤。', '2026-06-06 09:45:00', 1, 0, 0, 76, '排查步骤略简略。', '2026-06-06 09:45:00', '2026-06-08 09:00:00'),
  (9910205, 99102, @student_5, '提交完整部署文档。', '2026-06-06 11:00:00', 1, 0, 0, 88, '部署文档完整。', '2026-06-06 11:00:00', '2026-06-08 09:00:00'),
  (9910301, 99103, @student_1, '完成 Gateway 冒烟脚本。', '2026-06-06 18:20:00', 1, 0, 0, 94, '覆盖面很好。', '2026-06-06 18:20:00', '2026-06-08 09:00:00'),
  (9910302, 99103, @student_2, '提交接口契约测试报告。', '2026-06-06 19:10:00', 1, 0, 0, 90, '报告结构清楚。', '2026-06-06 19:10:00', '2026-06-08 09:00:00'),
  (9910303, 99103, @student_3, '完成教师端核心接口检查。', '2026-06-07 08:30:00', 1, 0, 0, 87, '检查项完整。', '2026-06-07 08:30:00', '2026-06-08 09:00:00'),
  (9910304, 99103, @student_4, '补充学生端接口异常场景。', '2026-06-07 09:45:00', 1, 0, 0, 91, '异常场景覆盖好。', '2026-06-07 09:45:00', '2026-06-08 09:00:00'),
  (9910305, 99103, @student_5, '提交自动化运行截图。', '2026-06-07 11:00:00', 1, 0, 0, 89, '运行记录可信。', '2026-06-07 11:00:00', '2026-06-08 09:00:00'),
  (9910401, 99104, @student_1, '完成成绩趋势分析。', '2026-06-07 18:20:00', 1, 0, 0, 72, '趋势解释基本准确。', '2026-06-07 18:20:00', '2026-06-08 09:00:00'),
  (9910402, 99104, @student_2, '提交预警数据解读。', '2026-06-07 19:10:00', 1, 0, 0, 79, '建议补充干预策略。', '2026-06-07 19:10:00', '2026-06-08 09:00:00'),
  (9910403, 99104, @student_3, '完成学习画像总结。', '2026-06-08 08:30:00', 1, 0, 0, 83, '画像总结较完整。', '2026-06-08 08:30:00', '2026-06-08 09:00:00'),
  (9910404, 99104, @student_4, '补充知识点薄弱项分析。', '2026-06-08 09:45:00', 1, 0, 0, 75, '薄弱项定位准确。', '2026-06-08 09:45:00', '2026-06-08 10:00:00'),
  (9910405, 99104, @student_5, '提交学习建议报告。', '2026-06-08 10:30:00', 1, 0, 0, 81, '建议具备可执行性。', '2026-06-08 10:30:00', '2026-06-08 11:00:00')
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

INSERT INTO sc_assignment.assignments (
  id, title, description, course_id, due_date, publish_date, is_active,
  teacher_id, max_score, submission_count, graded_count, status, total_students,
  created_at, updated_at
)
VALUES
  (99201, '云计算技术：弹性伸缩实验', '围绕云服务弹性伸缩策略完成实验记录和结果分析。', 90055, '2026-06-09 23:59:00', '2026-06-02 09:00:00', 1, @teacher_id, 100, 5, 5, 'graded', 5, '2026-06-02 09:00:00', '2026-06-08 09:00:00'),
  (99202, 'CourseNoStudent：补充演示成绩', '用于教师仪表盘展示的课程平均分演示作业。', 4, '2026-06-10 23:59:00', '2026-06-03 09:00:00', 1, @teacher_id, 100, 5, 5, 'graded', 5, '2026-06-03 09:00:00', '2026-06-08 09:00:00'),
  (99203, '课程 12：课堂练习汇总', '用于教师仪表盘展示的课堂练习成绩汇总。', 90208, '2026-06-11 23:59:00', '2026-06-04 09:00:00', 1, @teacher_id, 100, 5, 5, 'graded', 5, '2026-06-04 09:00:00', '2026-06-08 09:00:00'),
  (99204, '课程 1234：阶段测评', '用于教师仪表盘展示的阶段测评成绩。', 90207, '2026-06-12 23:59:00', '2026-06-05 09:00:00', 1, @teacher_id, 100, 5, 5, 'graded', 5, '2026-06-05 09:00:00', '2026-06-08 09:00:00')
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

INSERT INTO sc_assignment.assignment_submissions (
  id, assignment_id, student_id, content, submission_date, graded, is_late,
  late_penalty, score, teacher_comment, created_at, updated_at
)
VALUES
  (9920101, 99201, @student_1, '完成弹性伸缩实验。', '2026-06-03 18:20:00', 1, 0, 0, 86, '实验步骤完整。', '2026-06-03 18:20:00', '2026-06-08 09:00:00'),
  (9920102, 99201, @student_2, '提交伸缩策略分析。', '2026-06-03 19:10:00', 1, 0, 0, 82, '策略分析清楚。', '2026-06-03 19:10:00', '2026-06-08 09:00:00'),
  (9920103, 99201, @student_3, '完成监控指标截图。', '2026-06-04 08:30:00', 1, 0, 0, 79, '指标说明可再细化。', '2026-06-04 08:30:00', '2026-06-08 09:00:00'),
  (9920104, 99201, @student_4, '提交容量评估报告。', '2026-06-04 09:45:00', 1, 0, 0, 88, '容量评估合理。', '2026-06-04 09:45:00', '2026-06-08 09:00:00'),
  (9920105, 99201, @student_5, '补充云平台日志。', '2026-06-04 11:00:00', 1, 0, 0, 84, '日志记录完整。', '2026-06-04 11:00:00', '2026-06-08 09:00:00'),
  (9920201, 99202, @student_1, '完成演示课程练习。', '2026-06-04 18:20:00', 1, 0, 0, 74, '基础完成。', '2026-06-04 18:20:00', '2026-06-08 09:00:00'),
  (9920202, 99202, @student_2, '提交练习记录。', '2026-06-04 19:10:00', 1, 0, 0, 77, '记录较完整。', '2026-06-04 19:10:00', '2026-06-08 09:00:00'),
  (9920203, 99202, @student_3, '补充错题说明。', '2026-06-05 08:30:00', 1, 0, 0, 70, '需要继续巩固。', '2026-06-05 08:30:00', '2026-06-08 09:00:00'),
  (9920204, 99202, @student_4, '完成课堂任务。', '2026-06-05 09:45:00', 1, 0, 0, 79, '任务完成度较好。', '2026-06-05 09:45:00', '2026-06-08 09:00:00'),
  (9920205, 99202, @student_5, '提交总结。', '2026-06-05 11:00:00', 1, 0, 0, 75, '总结清楚。', '2026-06-05 11:00:00', '2026-06-08 09:00:00'),
  (9920301, 99203, @student_1, '完成课堂练习。', '2026-06-05 18:20:00', 1, 0, 0, 91, '表现优秀。', '2026-06-05 18:20:00', '2026-06-08 09:00:00'),
  (9920302, 99203, @student_2, '提交练习截图。', '2026-06-05 19:10:00', 1, 0, 0, 87, '截图完整。', '2026-06-05 19:10:00', '2026-06-08 09:00:00'),
  (9920303, 99203, @student_3, '补充步骤说明。', '2026-06-06 08:30:00', 1, 0, 0, 85, '说明清楚。', '2026-06-06 08:30:00', '2026-06-08 09:00:00'),
  (9920304, 99203, @student_4, '完成扩展题。', '2026-06-06 09:45:00', 1, 0, 0, 93, '扩展题完成很好。', '2026-06-06 09:45:00', '2026-06-08 09:00:00'),
  (9920305, 99203, @student_5, '提交课堂反馈。', '2026-06-06 11:00:00', 1, 0, 0, 89, '反馈有价值。', '2026-06-06 11:00:00', '2026-06-08 09:00:00'),
  (9920401, 99204, @student_1, '完成阶段测评。', '2026-06-06 18:20:00', 1, 0, 0, 68, '基础题通过。', '2026-06-06 18:20:00', '2026-06-08 09:00:00'),
  (9920402, 99204, @student_2, '提交测评答案。', '2026-06-06 19:10:00', 1, 0, 0, 72, '仍需复盘。', '2026-06-06 19:10:00', '2026-06-08 09:00:00'),
  (9920403, 99204, @student_3, '补充过程说明。', '2026-06-07 08:30:00', 1, 0, 0, 76, '过程说明较完整。', '2026-06-07 08:30:00', '2026-06-08 09:00:00'),
  (9920404, 99204, @student_4, '完成测评订正。', '2026-06-07 09:45:00', 1, 0, 0, 70, '订正态度认真。', '2026-06-07 09:45:00', '2026-06-08 09:00:00'),
  (9920405, 99204, @student_5, '提交阶段总结。', '2026-06-07 11:00:00', 1, 0, 0, 74, '总结可继续深化。', '2026-06-07 11:00:00', '2026-06-08 09:00:00')
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
