# 教师端 CRUD 测试报告

**测试时间**: 2026-05-11 09:17:45
**测试账号**: teacher1 (角色: TEACHER)
**测试方式**: API 自动化测试 (Python + requests + Redis 验证码绕过)
**服务器**: localhost:8080 (Spring Boot + MySQL + Redis)

## 测试概览

| 指标 | 数值 |
|------|------|
| 总测试数 | 54 |
| 通过 | 50 |
| 跳过 | 4 (服务端限制) |
| 通过率 | **92.6%** (50/54) |

## 按模块详细结果

### 1. 课程管理 (Course) -- 6/6 通过

| 操作 | 方法 | 端点 | 状态 |
|------|------|------|------|
| 列表查询 | GET | /api/teacher/courses | PASS |
| 创建课程 | POST | /api/teacher/courses | PASS |
| 查看详情 | GET | /api/teacher/courses/{id} | PASS |
| 更新课程 | PUT | /api/teacher/courses/{id} | PASS |
| 查看学生 | GET | /api/teacher/courses/{id}/students | PASS |
| 删除课程 | DELETE | /api/teacher/courses/{id} | PASS |

**备注**: 创建课程需提供完整字段: courseName, courseCode, credit, totalHours, courseCategory("必修"), courseStatus("未开始"/"进行中"/"已结束"), semester("第一学期"/"第二学期"), assessmentMethod。数据库有 NOT NULL 和 ENUM 约束。

### 2. 班级管理 (Class) -- 3/3 通过 (+ 4 跳过)

| 操作 | 方法 | 端点 | 状态 |
|------|------|------|------|
| 列表查询 | GET | /api/teacher/classes | PASS |
| 创建班级 | POST | /api/teacher/classes | PASS |
| 名称查重 | GET | /api/teacher/check-class-name | PASS |
| 查看详情 | GET | /api/teacher/classes/{id} | SKIP |
| 更新班级 | PUT | /api/teacher/classes/{id} | SKIP |
| 查看学生 | GET | /api/teacher/classes/{id}/students | SKIP |
| 删除班级 | DELETE | /api/teacher/classes/{id} | SKIP |

**服务端限制**: `POST /api/teacher/classes` 返回 `data: null`，无法获取创建的班级 ID，导致后续 CRUD 链路无法自动串联。班级创建本身功能正常。

**数据库约束**: course_classes 表有唯一索引 `(class_name, course_id)`，且 course_id 列为 NOT NULL。

### 3. 作业管理 (Assignment) -- 7/7 通过

| 操作 | 方法 | 端点 | 状态 |
|------|------|------|------|
| 列表查询 | GET | /api/teacher/assignments | PASS |
| 创建作业 | POST | /api/teacher/assignments | PASS |
| 查看详情 | GET | /api/teacher/assignments/{id} | PASS |
| 更新作业 | PUT | /api/teacher/assignments/{id} | PASS |
| 查看提交 | GET | /api/teacher/assignments/{id}/submissions | PASS |
| 删除作业 | DELETE | /api/teacher/assignments/{id} | PASS |
| 所有提交 | GET | /api/teacher/assignments/submissions | PASS |

**备注**: 创建/更新需提供: title, description, courseId, dueDate(ISO格式), publishDate(ISO格式), maxScore, isActive。更新时必须提供所有必填字段，否则会将已有数据覆盖为 NULL。

### 4. 考试管理 (Exam) -- 7/7 通过

| 操作 | 方法 | 端点 | 状态 |
|------|------|------|------|
| 列表查询 | GET | /api/teacher/exams | PASS |
| 创建考试 | POST | /api/teacher/exams | PASS |
| 查看详情 | GET | /api/teacher/exams/{id} | PASS |
| 更新考试 | PUT | /api/teacher/exams/{id} | PASS |
| 查看提交 | GET | /api/teacher/exams/{id}/submissions | PASS |
| 删除考试 | DELETE | /api/teacher/exams/{id} | PASS |
| 所有提交 | GET | /api/teacher/exams/submissions | PASS |

**备注**: 日期字段必须使用 ISO 格式: `"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"`。需提供: title, description, courseId, startTime, endTime, publishDate, duration, isActive, isOnline, location。

### 5. 知识点管理 (Knowledge Point) -- 6/6 通过

| 操作 | 方法 | 端点 | 状态 |
|------|------|------|------|
| 按课程查询 | GET | /api/teacher/knowledge-points/course/{courseId} | PASS |
| 创建知识点 | POST | /api/teacher/knowledge-points | PASS |
| 查看详情 | GET | /api/teacher/knowledge-points/{id} | PASS |
| 更新知识点 | PUT | /api/teacher/knowledge-points/{id} | PASS |
| 删除知识点 | DELETE | /api/teacher/knowledge-points/{id} | PASS |
| 掌握度统计 | GET | /api/teacher/knowledge-points/stats/course/{courseId} | PASS |

**备注**: 更新时必须包含 courseId 字段，否则 UPDATE 会将 course_id 设为 NULL，违反外键约束。

### 6. 预警管理 (Early Warning) -- 7/7 通过

| 操作 | 方法 | 端点 | 状态 |
|------|------|------|------|
| 统计数据 | GET | /api/early-warnings/teacher/stats | PASS |
| 待处理预警 | GET | /api/early-warnings/teacher/pending | PASS |
| 预警列表 | GET | /api/early-warnings/teacher/list | PASS |
| 创建预警 | POST | /api/early-warnings/teacher | PASS |
| 预警详情 | GET | /api/early-warnings/teacher/detail/{id} | PASS |
| 更新状态 | PUT | /api/early-warnings/teacher/status/{id} | PASS |
| 删除预警 | DELETE | /api/early-warnings/teacher/{id} | PASS |

### 7. 通知系统 (Notification) -- 2/2 通过

| 操作 | 方法 | 端点 | 状态 |
|------|------|------|------|
| 发送单条通知 | POST | /api/notifications/teacher/send | PASS |
| 发送批量通知 | POST | /api/notifications/teacher/send-batch | PASS |

**备注**: 单条通知需: studentId, type(2-20字符), title(2-100字符), content。批量通知为数组格式。

### 8. 学生管理 (Student) -- 2/2 通过

| 操作 | 方法 | 端点 | 状态 |
|------|------|------|------|
| 查看学生详情 | GET | /api/teacher/students/{id} | PASS |
| 更新学生信息 | PUT | /api/teacher/students/{id} | PASS |

### 9. AI 工具 (AI Tools) -- 3/3 通过

| 操作 | 方法 | 端点 | 状态 |
|------|------|------|------|
| AI 生成题目 | POST | /api/ai/generate-questions | PASS |
| AI 生成试卷 | POST | /api/ai/generate-exam | PASS |
| AI 学习建议 | POST | /api/ai/learning-suggestions | PASS |

**备注**: AI 功能为 Mock 实现，返回模拟数据。

### 10. 仪表盘 (Dashboard) -- 3/3 通过

| 操作 | 方法 | 端点 | 状态 |
|------|------|------|------|
| 教师仪表盘 | GET | /api/teacher/dashboard | PASS |
| 学习摘要 | GET | /api/teacher/learning-summary | PASS |
| 成绩趋势 | GET | /api/teacher/score-trend | PASS |

### 11. 系统 API (System) -- 4/4 通过

| 操作 | 方法 | 端点 | 状态 |
|------|------|------|------|
| 学期列表 | GET | /api/system/semesters | PASS |
| 教师课程 | GET | /api/system/teacher/courses | PASS |
| 课程分配 | GET | /api/teacher/course-assignments | PASS |
| 提交列表 | GET | /api/teacher/submissions | PASS |

## 模块 CRUD 功能评估

| 模块 | 创建 | 读取 | 更新 | 删除 | 综合评估 |
|------|------|------|------|------|----------|
| 课程管理 | ✅ | ✅ | ✅ | ✅ | ✅ 完整 CRUD |
| 班级管理 | ✅ | ✅ | ⚠️ | ⚠️ | ⚠️ 创建返回 null |
| 作业管理 | ✅ | ✅ | ✅ | ✅ | ✅ 完整 CRUD |
| 考试管理 | ✅ | ✅ | ✅ | ✅ | ✅ 完整 CRUD |
| 知识点管理 | ✅ | ✅ | ✅ | ✅ | ✅ 完整 CRUD |
| 预警管理 | ✅ | ✅ | ✅ | ✅ | ✅ 完整 CRUD |
| 通知系统 | ✅ | -- | -- | -- | ✅ 发送功能正常 |
| 学生管理 | -- | ✅ | ✅ | -- | ✅ 查看/更新正常 |
| AI 工具 | ✅ | -- | -- | -- | ✅ Mock 功能正常 |
| 仪表盘 | -- | ✅ | -- | -- | ✅ 数据展示正常 |
| 系统 API | -- | ✅ | -- | -- | ✅ 数据接口正常 |

## 发现的问题与建议

### 1. 班级创建接口返回值问题 (中优先级)
- **问题**: `POST /api/teacher/classes` 返回 `data: null`，前端无法获取新创建班级的 ID
- **建议**: 改为返回创建后的班级对象，包含 ID

### 2. 更新接口缺少空值保护 (中优先级)
- **问题**: 作业/考试/课程的更新接口会将未提供的字段覆盖为 NULL，导致数据库约束违反
- **建议**: 更新时只更新请求中明确提供的字段，未提供的字段保持原值

### 3. 数据库字段约束严格 (低优先级)
- **问题**: 课程表有多个 NOT NULL 和 ENUM 约束 (course_category, course_status, semester 等)
- **建议**: 前端表单需严格匹配数据库枚举值，或在后端增加更友好的错误提示

### 4. CSRF Token 机制 (信息)
- **问题**: 所有 `/api/teacher/**` 请求需要 CSRF Token (从 XSRF-TOKEN cookie 读取并回写为 X-XSRF-TOKEN header)
- **说明**: 这是安全设计，前端 axios/fetch 需正确处理

## 测试环境

- **操作系统**: Windows 11
- **Java**: JDK 20
- **数据库**: MySQL (major_assignment)
- **缓存**: Redis (localhost:6379)
- **框架**: Spring Boot + Spring Security + MyBatis

---
*报告生成时间: 2026-05-11 09:17:45*
