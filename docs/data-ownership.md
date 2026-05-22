---
title: 数据所有权矩阵
version: v0.2
last_updated: 2026-05-13
author: 架构组
---

# 数据所有权矩阵

## 1. 设计原则

数据拆分的核心目标是让每张业务表只有一个写入所有者。服务可以保存其他服务的业务 ID，也可以保存事件驱动的只读冗余，但不能直接改写其他服务的库表。

铁律：

- 一个目标表只能有一个归属服务负责写入。
- 禁止服务通过 JDBC、MyBatis、JPA 或手写 SQL 跨 Schema 读写业务表。
- 跨服务同步读必须走服务 API、Feign Client 或已定义的 read-model API。
- 跨服务写入必须通过领域事件、outbox relay 或拥有者服务提供的命令 API。
- 允许保存外部 ID，例如 `user_id`、`course_id`、`knowledge_point_id`，但目标拆库后不再创建跨 Schema 外键。
- 旧单体只作为迁移期适配层，不再夺回已拆服务的写入所有权。

## 2. 服务与数据库映射

| 服务 | Schema / 存储 | 写入范围 | 说明 |
| ---- | ------------- | -------- | ---- |
| Gateway | 无业务 Schema | 无 | 只负责路由、鉴权透传、限流与 TraceId，不落业务表。 |
| Auth_Service | `sc_auth` + Redis | 登录凭证、令牌状态、验证码状态 | 只保存认证事实，不保存用户画像和角色业务属性。 |
| User_Service | `sc_user` | 用户档案、角色、用户角色关系 | 其他服务通过用户 API 读取教师、学生、角色信息。 |
| Course_Service | `sc_course` | 课程、班级、专业、选课/授课关系、知识点目录主数据 | 教师和学生只保存 `user_id` 引用，不跨库外键。 |
| Assignment_Service | `sc_assignment` | 作业发布、作业提交、作业批改、作业知识点关联 | 学情分析通过事件消费提交与批改事实。 |
| Exam_Service | `sc_exam` | 考试、试题、考试提交、考试答案、考试知识点关联 | 自动阅卷在 Exam_Service 内完成，分析结果异步投递。 |
| Analysis_Service | `sc_analysis` | 掌握度、成绩趋势、学情预警、分析触发任务 | 由事件和定时只读拉取构建分析 read model。 |
| Notification_Service | `sc_notification` | 站内信、通知投递记录 | 不拥有业务事实，只保存通知事实和阅读状态。 |
| AI_Service | `sc_ai` | AI 请求、生成结果、提示词版本 | AI 不直接写业务库，业务落库由拥有者服务完成。 |
| Platform Audit | `sc_platform` | 审计日志 | 由公共审计组件写入，业务服务只提交审计事件。 |

## 3. 数据所有权矩阵

| 表名 | 归属服务 | Schema | 备注 | 只读共享范围 |
| ---- | -------- | ------ | ---- | ------------ |
| `auth_credentials` | Auth_Service | `sc_auth` | 从原 `users` 剥离用户名、密码哈希、启停状态等认证字段。 | Gateway 可通过 Auth API 校验；User_Service 不直连。 |
| `users` | User_Service | `sc_user` | 用户档案主表，保留姓名、学号/工号、联系方式、院系等业务属性。 | Auth、Course、Assignment、Exam、Analysis、Notification 通过 User API 或事件冗余读取。 |
| `roles` | User_Service | `sc_user` | 角色字典和业务角色描述由 User_Service 维护。 | Auth 通过 Feign 查询登录用户角色；Gateway 只消费 JWT 声明。 |
| `user_roles` | User_Service | `sc_user` | 用户角色关系唯一写入口。 | Auth 可同步查询；其他服务只读取 JWT 或 User API。 |
| `courses` | Course_Service | `sc_course` | 课程主数据，教师只保存 `teacher_id`。 | Assignment、Exam、Analysis 可通过 Course API 读取课程摘要。 |
| `course_classes` | Course_Service | `sc_course` | 班级主表，沿用当前单体表名，后续可在迁移脚本中评估是否重命名为 `classes`。 | Assignment、Exam、Notification 可读取班级摘要和成员范围。 |
| `class_courses` | Course_Service | `sc_course` | 班级与课程、任课教师关系。 | Assignment、Exam、Analysis 通过 Course API 校验课程班级关系。 |
| `class_students` | Course_Service | `sc_course` | 班级学生关系，学生只保存 `user_id`。 | Assignment、Exam、Notification 通过 Course API 获取收件人或提交范围。 |
| `majors` | Course_Service | `sc_course` | 专业/院系基础字典。 | User、Course、Analysis 可通过 Course API 或字典事件读取。 |
| `teacher_knowledge_points` | Course_Service | `sc_course` | 教师课程知识点目录主数据，承接旧 `KnowledgePointController` 的主数据 CRUD 与课程维度列表。 | Assignment、Exam 保存其 ID；Analysis 通过事件消费 ID 并构建掌握度读模型。 |
| `assignments` | Assignment_Service | `sc_assignment` | 作业主表，只保存课程、班级、教师等外部 ID。 | Course、Analysis、Notification 可通过 Assignment API 或事件读取。 |
| `assignment_classes` | Assignment_Service | `sc_assignment` | 作业发布到班级的关系。 | Notification、Analysis 可消费 `AssignmentPublished` read model。 |
| `assignment_submissions` | Assignment_Service | `sc_assignment` | 作业提交、批改结果和迟交状态。 | Analysis 通过 `AssignmentSubmittedEvent` / `AssignmentGradedEvent` 消费；Course 不直连。 |
| `assignment_knowledge_points` | Assignment_Service | `sc_assignment` | 作业覆盖知识点关系，只保存 Course_Service 的 `teacher_knowledge_points.id`。 | Analysis 可消费作业事件；Assignment 不跨库查询知识点表。 |
| `assignment_answers`（目标） | Assignment_Service | `sc_assignment` | 由原 `student_answers` 中存在 `assignment_submission_id` 的记录拆分而来。 | Analysis 通过事件读取答案统计，不直连表。 |
| `exams` | Exam_Service | `sc_exam` | 考试主表，只保存课程、班级、教师等外部 ID。 | Course、Analysis、Notification 可通过 Exam API 或事件读取。 |
| `exam_classes` | Exam_Service | `sc_exam` | 考试发布到班级的关系。 | Notification、Analysis 可消费 `ExamPublished` read model。 |
| `questions` | Exam_Service | `sc_exam` | 题库/题目主数据；单体中的 `assignment_id` 是过渡字段，迁移目标态由 Assignment 通过题目 API 或题目快照引用。 | Assignment、AI、Analysis 通过 Exam API 或事件读取题目摘要。 |
| `exam_submissions` | Exam_Service | `sc_exam` | 考试提交、得分、阅卷状态。 | Analysis 通过 `ExamFinishedEvent` 消费；Notification 消费结果通知事件。 |
| `exam_knowledge_points` | Exam_Service | `sc_exam` | 考试覆盖知识点关系，只保存 Course_Service 的 `teacher_knowledge_points.id`。 | Analysis 可消费考试事件；Exam 不跨库查询知识点表。 |
| `exam_answers`（目标） | Exam_Service | `sc_exam` | 由原 `student_answers` 中存在 `exam_submission_id` 的记录拆分而来。 | Analysis 通过事件读取答案统计，不直连表。 |
| `student_answers`（Legacy） | 迁移拆分项 | `legacy` | 目标态不保留共享表；按父提交外键拆为 `assignment_answers` 与 `exam_answers`。 | 禁止新服务直接读写，仅迁移脚本读取一次。 |
| `kp_mastery` | Analysis_Service | `sc_analysis` | 学生知识点掌握度 read model，`knowledge_point_id` 来自 Course_Service 知识点目录 ID，旧课程级汇总使用内部哨兵兼容。 | Dashboard、User 前端视图通过 Analysis API 读取。 |
| `score_trends` | Analysis_Service | `sc_analysis` | 学生成绩趋势 read model，由作业、考试事件聚合。 | Dashboard、User 前端视图通过 Analysis API 读取。 |
| `analysis_trigger_jobs` | Analysis_Service | `sc_analysis` | 教师手动分析触发兼容任务记录。 | 仅 Analysis_Service 写入和读取。 |
| `early_warnings` | Analysis_Service | `sc_analysis` | 学情预警主数据和处理状态。 | Notification 消费预警事件；User/Course 通过 Analysis API 读取。 |
| `notifications` | Notification_Service | `sc_notification` | 通知内容、接收人、已读状态。 | Gateway/User 只通过 Notification API 查询当前用户通知。 |
| `ai_prompts` | AI_Service | `sc_ai` | 提示词模板、版本、启停状态。 | Exam、Assignment 可通过 AI API 选择模板，不直连。 |
| `ai_generations` | AI_Service | `sc_ai` | AI 调用记录、输入摘要、输出摘要、模型元数据。 | 业务服务可读取生成任务结果；业务落库仍由业务拥有者完成。 |
| `audit_logs` | Platform Audit | `sc_platform` | 审计日志统一写入，记录 actor、动作、资源和快照。 | 管理端只读查询；业务服务不跨库查询。 |
| `outbox_event` | 生产事件的本地服务 | 各服务本地 Schema | 每个生产者服务各建一张本地 outbox 表，例如 `sc_assignment.outbox_event`。 | 仅本服务 relay job 读取并发布，不给其他业务服务查询。 |
| `processed_event` | 消费事件的本地服务 | 各服务本地 Schema | 每个消费者服务各建一张本地去重表，例如 `sc_analysis.processed_event`。 | 仅本服务幂等处理器读写。 |

## 4. Redis 与缓存资源归属

| 资源 / Key 前缀 | 归属服务 | 用途 | 共享规则 |
| --------------- | -------- | ---- | -------- |
| `AUTH:JTI_BLACKLIST:*` | Auth_Service | JWT 注销、封禁、强制下线。 | Gateway 只通过 Auth API 或统一鉴权组件判断，不直接写。 |
| `AUTH:REFRESH:*` | Auth_Service | Refresh token 状态。 | 不对业务服务开放。 |
| `CAPTCHA:IMG:*` | Auth_Service | 图形验证码。 | 不对业务服务开放。 |
| `RATE_LIMIT:*` | Gateway | 网关限流计数。 | 业务服务不可依赖该 Key 做业务判断。 |
| `READMODEL:*` | 对应 read model 所属服务 | 查询缓存和投影缓存。 | 由拥有者服务写入，消费者通过 API 读取。 |

## 5. 跨服务数据访问策略

### 5.1 同步查询

同步查询只用于用户可见的读路径、轻量校验和列表页补全。调用方只能依赖拥有者服务公开的 API 契约，不能依赖对方表结构。

约定：

- Feign Client 放在调用方自己的 adapter 层，避免领域层直接依赖远端协议。
- API 响应使用业务稳定字段，例如 `userId`、`displayName`、`roleNames`、`courseName`，不暴露表字段全集。
- 调用失败时使用缓存、降级文案或阻断当前命令，不允许临时跨库查询绕过失败。

### 5.2 异步事件

跨服务写入采用事件驱动和最终一致性。生产者在本地事务内同时写业务表和 `outbox_event`，relay job 负责发布到消息系统；消费者先写 `processed_event` 去重，再更新本地 read model。

首批核心事件：

- `AssignmentSubmittedEvent`
- `AssignmentGradedEvent`
- `ExamFinishedEvent`
- `EarlyWarningRaisedEvent`
- `EarlyWarningRollbackEvent`
- `NotificationPushedEvent`

事件载荷必须携带稳定业务 ID、发生时间、聚合类型和最小必要快照。消费者不得回查生产者数据库。

### 5.3 数据冗余与缓存

允许的冗余只服务查询性能和历史快照，不能成为事实写入口。

- Assignment_Service、Exam_Service 可以保存 `course_id`、`class_id`、`teacher_id`、`student_id`。
- Notification_Service 可以保存通知发送当时的标题、摘要和接收人 ID。
- Analysis_Service 可以保存作业、考试、用户、课程的统计快照。
- 快照过期或修正必须由拥有者事件驱动，不能由消费者主动改写源表。

## 6. 数据迁移与一致性

迁移顺序：

1. 先完成 Auth/User 已拆表的写入收口，确保旧单体不再写密码和用户角色。
2. 再拆 Course，切断班级、课程、学生关系的跨库外键，改成 API 校验。
3. 再拆 Assignment 与 Exam，处理 `student_answers` 和 `questions` 的过渡字段。
4. 最后拆 Analysis、Notification、AI，把统计和通知改为事件消费。

一致性检查：

- 每个服务的数据库账号只授予本 Schema 的 DML 权限。
- 迁移脚本必须删除目标态跨 Schema 外键，只保留本 Schema 内约束。
- ArchUnit 或集成测试扫描数据源配置，禁止服务模块配置其他业务 Schema。
- 审计和 outbox 表是平台能力，但仍按“本地 Schema、本地写入”治理。

## 变更记录

| 日期 | 变更人 | 变更内容 |
| ---- | ------ | -------- |
| 2026-05-13 | Codex | 完成服务与表所有权矩阵、Redis 归属、跨 Schema 访问铁律和 outbox/processed 事件表归属规则。 |
| 2026-05-10 | 架构组 | 初版骨架。 |
