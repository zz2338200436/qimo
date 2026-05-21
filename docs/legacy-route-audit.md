# Legacy Route Audit

> 版本：v0.2
> 最后更新：2026-05-20
> 作者：Codex

## 1. 目的

本文档记录 Gateway 中仍显式转发到旧单体 `major_assignment` 的 `legacy-*` 路由分组，用于替代已经删除的 catch-all `legacy-route`，并帮助后续逐组迁移到正式微服务。

## 2. 当前状态

当前 Gateway 已删除 `legacy-route` 这一条兜底所有 `/api/**` 的 catch-all 路由。

仍保留的显式 legacy 路由如下：

| Gateway Route ID | 当前路径范围 | 旧单体 Controller / 职责 |
| --- | --- | --- |
| 无 | 无 | Gateway 当前不再显式转发到旧单体 |

## 3. 已正式切出的路径

以下路径当前已优先由正式服务路由承接，不再依赖旧单体 catch-all：

- `auth-service`：登录、刷新、`/me`、登出、切换角色、改密
- `user-service`：`/api/users/**`，以及学生资料兼容接口 `/api/student/profile`、`/api/student/notification-settings`、`/api/student/privacy-settings`、`/api/student/upload-avatar`、`/api/student/export-data`、`/api/students/*/class`
- `course-service`：教师课程、学生课程、班级、课程分配、专业，以及 `/api/teacher/check-class-name`、`/api/teacher/students/**` 教师侧学生资料兼容接口
- `assignment-service`：教师作业 CRUD、提交列表、学生作业读取/提交、`/api/teacher/knowledge-points/assignment/**` 作业知识点关联旧 URL 兼容
- `exam-service`：学生考试、成绩、教师考试 CRUD/评分、`/api/teacher/knowledge-points/exam/**` 考试知识点关联旧 URL 兼容
- `notification-service`：`/api/notifications/**`
- `analysis-service`：教师分数趋势、`/api/teacher/dashboard`、`/api/teacher/learning-summary` 教师看板兼容读接口、`/api/teacher/analysis/**` 教师手动分析触发兼容入口、学生课程知识点掌握度、`/api/teacher/knowledge-points/mastery/student/{studentId}/course/{courseId}`、`/api/teacher/knowledge-points/stats/course/{courseId}`、`POST /api/teacher/knowledge-points/analyze/student/{studentId}/course/{courseId}`、`/api/knowledge-points/analysis/teacher/**` 旧 URL 兼容分析接口、`/api/early-warnings/**`、`/api/teacher/early-warnings/**`、`/api/student/early-warnings` 学情预警 JSON/导出兼容接口，以及 `/api/student/study-time-distribution`、`/api/student/stats`、`/api/student/knowledge-points`、`/api/student/knowledge-points/{knowledgePointId}` 学生学习统计/知识点兼容读接口
- `ai-service`：`/api/ai/**`
- `auth-service` 兼容扩展：`/api/public/captcha`、`PUT /api/auth/notification-settings` 与 `POST /api/student/change-password`
- `course-service` 兼容扩展：`/api/system/semesters`、`/api/system/student/courses`、`/api/system/teacher/courses`、`/api/system/time-ranges`、`/api/dashboard/student-performance`、`/api/dashboard/student-performance/{studentId}`、`/api/teacher/knowledge-points`、`/api/teacher/knowledge-points/{id}`、`/api/teacher/knowledge-points/course/{courseId}`
- `gateway` 边缘兼容：`/api/errors/browser`、`/api/errors/browser/batch`、`/api/errors/browser/{id}`、`/api/errors/browser`

## 4. 下一步建议

当前 Gateway 已无显式 `legacy-*` 路由。后续重点转为：

1. 将 `analysis-service` 的触发任务进一步异步化，例如引入任务执行器或 Outbox 事件，避免手动触发接口承担长耗时同步扫描。
2. 扩展预警规则，补齐缺交、截止日期、出勤等当前读模型尚未覆盖的旧单体行为。
3. 移除前端中对旧单体行为的隐式依赖，并补齐端到端冒烟测试。

## 变更记录

| 日期       | 变更人 | 变更内容 |
| ---------- | ------ | -------- |
| 2026-05-19 | Codex | 初版 legacy 显式路由审计，记录 catch-all `legacy-route` 删除后的剩余旧单体接口分组 |
| 2026-05-20 | Codex | 将 `/api/knowledge-points/analysis/teacher/**` 从 `legacy-knowledge-route` 迁到 `analysis-service` 兼容读接口，`legacy-knowledge-route` 首次收窄 |
| 2026-05-20 | Codex | 将知识点管理主链 `/api/teacher/knowledge-points`、`/{id}`、`/course/{courseId}` 迁到 `course-service`，`legacy-knowledge-route` 继续收窄为关联作业/考试/统计/分析深接口 |
| 2026-05-20 | Codex | 将 `/api/teacher/knowledge-points/mastery/student/{studentId}/course/{courseId}` 确认为 `analysis-service` 承接，并从 `legacy-knowledge-route` 移除 `mastery/**` |
| 2026-05-20 | Codex | 将 `/api/teacher/knowledge-points/stats/course/{courseId}` 与 `POST /api/teacher/knowledge-points/analyze/student/{studentId}/course/{courseId}` 迁到 `analysis-service`，`legacy-knowledge-route` 收窄为作业/考试关联接口 |
| 2026-05-20 | Codex | 将 `/api/teacher/knowledge-points/assignment/**` 迁到 `assignment-service`、`/api/teacher/knowledge-points/exam/**` 迁到 `exam-service`，删除 `legacy-knowledge-route` |
| 2026-05-20 | Codex | 将 `/api/early-warnings/**` 与 `/api/teacher/early-warnings/**` 迁到 `analysis-service`，删除 `legacy-early-warning-route` |
| 2026-05-20 | Codex | 将学生资料/设置/头像/导出/班级名兼容接口迁到 `user-service`，`legacy-student-route` 收窄为统计、知识点、学习时长、学生预警与改密 |
| 2026-05-20 | Codex | 将 `POST /api/student/change-password` 迁到 `auth-service` 兼容入口，`legacy-student-route` 继续收窄为统计、知识点、学习时长与学生预警 |
| 2026-05-20 | Codex | 将 `GET /api/student/early-warnings` 迁到 `analysis-service` 兼容入口，`legacy-student-route` 继续收窄为统计、知识点与学习时长 |
| 2026-05-20 | Codex | 将 `GET /api/student/study-time-distribution` 迁到 `analysis-service` 兼容入口，`legacy-student-route` 继续收窄为统计与知识点 |
| 2026-05-20 | Codex | 将 `GET /api/student/stats` 迁到 `analysis-service` 兼容入口，`legacy-student-route` 继续收窄为学生知识点 |
| 2026-05-20 | Codex | 将 `GET /api/student/knowledge-points` 与 `GET /api/student/knowledge-points/{knowledgePointId}` 迁到 `analysis-service` 兼容入口，删除 `legacy-student-route` |
| 2026-05-20 | Codex | 将 `GET /api/teacher/dashboard`、`GET /api/teacher/learning-summary` 迁到 `analysis-service`，将 `/api/teacher/check-class-name` 与 `/api/teacher/students/**` 迁到 `course-service`，删除 `legacy-teacher-dashboard-route` |
| 2026-05-20 | Codex | 将 `POST /api/teacher/analysis/**` 迁到 `analysis-service` 教师手动分析触发兼容入口，删除最后一条 `legacy-analysis-trigger-route` |
| 2026-05-20 | Codex | 为 `/api/teacher/analysis/**` 触发入口补充 `analysis_trigger_jobs` 任务记录，并基于 `score_trends`、`kp_mastery` 读模型生成低分/进度基础预警 |
