# Assignment 联调检查清单

> 版本：v0.5
> 最后更新：2026-05-15
> 作者：Codex

## 1. 目标

本文档用于支撑 `assignment-service` 从“代码已具备”推进到“运行时可联调”的阶段，重点覆盖：

- 本地环境是否具备 `sc_assignment` / `sc_course` 等多 schema 运行基础
- 学生读取链路对 `class_students` 的依赖是否可达
- Gateway 已切作业域路由是否能闭环验证
- 教师端作业主写链路是否已在网关鉴权下闭环，以及剩余风险在哪里

## 2. 当前运行时状态

### 2.1 本地数据库与开发鉴权种子已可重复准备

当前微服务默认数据源分别连接：

- `assignment-service -> sc_assignment`
- `course-service -> sc_course`
- `auth-service -> sc_auth`
- `user-service -> sc_user`

但原始 `docker-compose.dev.yml` 只初始化 `major_assignment`，如果不补多 schema，本地启动微服务时会直接出现：

- schema 不存在
- `dev_user / dev_only_pwd` 账号不存在
- Flyway 无法落库

本轮已补：

- `data/mysql/init/01-create-microservice-schemas.sql`
- `docker-compose.dev.yml` 挂载 `/docker-entrypoint-initdb.d`
- `scripts/seed-dev-auth-users.sql`
- `scripts/seed-dev-auth-users.ps1`
- MySQL 数据目录与初始化脚本目录分离：
  - `data/mysql/db -> /var/lib/mysql`
  - `data/mysql/init -> /docker-entrypoint-initdb.d`

注意：

- 该初始化脚本只会在全新 MySQL 数据目录首次启动时自动执行。
- `auth_credentials` / `users` 表由 `auth-service` 与 `user-service` 的 Flyway 迁移创建，因此开发联调账号不能放进 `data/mysql/init` 目录里与 schema 脚本一起执行。
- `scripts/seed-dev-auth-users.ps1` 应在 `user-service` 与 `auth-service` 至少完成一次 Flyway 建表后再执行。
- 如果本地 `data/mysql/db` 已存在旧数据，需要手工补库或清空后重建容器。
- 如果本机 `3306` 已被其他 MySQL 占用，`docker compose up mysql` 会直接失败，需要先停掉占用者，或改 compose 端口映射。
- 如果本机已存在同名旧容器（例如 `qimo-redis`），compose 会因容器名冲突失败，需要先删除旧容器或直接复用它。
- 如果把 `data/mysql/init` 一并挂到 `/var/lib/mysql`，MySQL 8.4 会因“数据目录非空”反复初始化失败；当前 compose 已修正为分离挂载。

当前默认开发账号：

- 教师：`teacher7 / Teach1234`
- 学生：`student42 / Teach1234`

### 2.2 学生读取链路已切换为经 `course-service` 聚合

2026-05-15 已完成以下修正：

- `course-service` 新增内部接口：`GET /internal/courses/students/{studentId}/class-ids`
- `assignment-service` 学生读取链路改为：
  - 先通过 Feign 调 `course-service` 获取学生所属 `classIds`
  - 再仅基于 `assignment_classes` 查询 `sc_assignment`

这意味着：

- `assignment-service` 不再直接查询 `sc_assignment.class_students`
- 学生作业列表的跨 Schema 直连风险已消除

已验证结果：

- 2026-05-14 晚间的旧实现会在 `GET /api/student/assignments` 上报：
  - `Table 'sc_assignment.class_students' doesn't exist`
- 2026-05-15 修复后，本地直连 `http://localhost:8084/api/student/assignments` 在 `X-User-Id: 42` 下已返回 `200` 空页，不再报 SQL 语法错误

### 2.3 教师端作业主写已完成 Gateway 鉴权切流验证

当前已切到 `assignment-service` 的是：

- 教师作业读取
- 教师作业主写
- 教师提交记录读取/批改
- 学生作业读取
- 学生提交作业

2026-05-15 已额外完成以下修正与验证：

- `gateway` 中 `JwtAuthenticationFilter` 对同步抛出的 `JwtAuthenticationException` 已改为返回 `401`，不再冒泡成 `500`
- `auth-service` 与 `gateway` 已统一本地 `dev` kid 对应的 RSA 密钥
- 教师登录拿到 Bearer Token 后，`POST /api/teacher/assignments` 已能经 `gateway` 成功落库到 `sc_assignment`

当前对 Assignment 域而言，`legacy-route` 已不再承接公开作业接口；剩余关注点转为：

- 回滚时是否保留清晰的路由快照
- `user-service` / `course-service` 不可用时的降级表现是否足够清晰
- RabbitMQ 消费侧是否已接好并完成下游消费验证

### 2.4 `AssignmentSubmittedEvent` 已完成 RabbitMQ relay 验证

2026-05-15 已完成一次真实 `outbox -> RabbitMQ` 联调验证：

1. 以 `platform.outbox.relay.enabled=true` 启动 `assignment-service`
2. 向 `sc_assignment.outbox_event` 准备 `relay-smoke-evt-1`
3. 确认 `assignment-service` 运行时 `relayJobPresent=true`
4. 观察到该事件由 `pending` 自动变为 `published`
5. 在 RabbitMQ 中确认生成：
   - `assignment.submitted.assignment-submitted-inspector`
   - `assignment.submitted.assignment-submitted-inspector.dlq`

验证证据：

- `outbox_event.event_id = relay-smoke-evt-1`
- `status = 1`
- `published_at = 2026-05-15 15:56:18`
- RabbitMQ 检查到 inspector 队列中已有 `1` 条待消费消息

这说明当前 Assignment 域已经不是“只会写本地 outbox”，而是具备了真实消息发布能力。当前剩余工作转为：

- 消费侧是否订阅并正确处理 `AssignmentSubmittedEvent`
- 是否需要把 relay 开关、检查命令、回滚步骤进一步脚本化
- 是否要补充端到端消费验收用例

2026-05-15 已补出第一个消费侧落点：

- `notification-service` 最小切片已建立
- 已接入 `AssignmentSubmittedEvent` 幂等消费
- 已提供学生通知最小读接口用于后续联调

当前仍未完成的部分是：

- 通知域剩余接口（已读、删除、教师手工发送）
- `notification-route` 的 Gateway 切换

2026-05-15 已完成一次真实运行时通知消费验证：

1. 启动 `registry-server`、`course-service`、`assignment-service`（开启 relay）、`notification-service`
2. 学生 `POST http://localhost:8084/api/student/assignments/1/submit`，带 `X-User-Id: 42`
3. `sc_assignment.outbox_event` 生成并发布：
   - `event_id = assignment-submitted-3`
   - `status = 1`
4. `notification-service` 消费后写入：
   - `sc_notification.notifications.id = 1`
   - `student_id = 42`
   - `type = assignment`
   - `title = 作业提交成功`
5. 读接口验证：
   - `GET http://localhost:8087/api/notifications/student`
   - `GET http://localhost:8087/api/notifications/student/unread-count`

这说明当前最小通知切片已经不是“只具备代码和单测”，而是完成了真实 `submit -> outbox -> RabbitMQ -> notification-service -> sc_notification` 闭环。

2026-05-15 已额外完成一次 `ExamFinishedEvent` 的真实运行时 smoke：

1. 使用最新 `notification-service` 重启后确认三个 RabbitMQ 消费队列均已在线：
   - `assignment.submitted.notification-service`
   - `exam.finished.notification-service`
   - `early.warning.raised.notification-service`
2. 向 `exam.finished` 发布 `exam-finished-smoke-4`
3. `notification-service` 成功消费后写入：
   - `sc_notification.notifications.id = 2`
   - `student_id = 42`
   - `type = exam`
   - `title = 考试已完成`
   - `content = 您已完成本次考试，当前成绩：93/100`
   - `related_id = 79`
4. `processed_event` 记录：
   - `event_id = exam-finished-smoke-4`
   - `event_type = ExamFinishedEvent`
   - `consumer_name = notification-service.exam-finished`
5. `GET http://localhost:8087/api/notifications/student` 已返回 exam 通知

2026-05-15 已额外完成一次 `EarlyWarningRaisedEvent` 的真实运行时 smoke：

1. 修复 `EarlyWarningRaisedNotificationHandler` 未写入 `studentId`，导致 `notifications.student_id` 非空约束报错的问题
2. 使用最新 `notification-service` 重启后，向 `early.warning.raised` 发布 `warning-smoke-2`
3. `notification-service` 成功消费后写入：
   - `sc_notification.notifications.id = 3`
   - `student_id = 42`
   - `type = warning`
   - `title = 学情预警`
   - `content = 学生近期成绩低于及格线`
   - `related_id = 7002`
4. `processed_event` 记录：
   - `event_id = warning-smoke-2`
   - `event_type = EarlyWarningRaisedEvent`
   - `consumer_name = notification-service.early-warning-raised`
5. `GET http://localhost:8087/api/notifications/student` 已返回 warning 通知

这说明 Notification 域当前已经完成三条真实事件消费链路：

- `AssignmentSubmittedEvent`
- `ExamFinishedEvent`
- `EarlyWarningRaisedEvent`

## 3. 联调前环境检查

### 3.1 MySQL

确认以下 schema 存在：

- `major_assignment`
- `sc_auth`
- `sc_user`
- `sc_course`
- `sc_assignment`

确认以下账号可登录：

- 用户名：`dev_user`
- 密码：`dev_only_pwd`

建议检查 SQL：

```sql
SHOW DATABASES;
SELECT user, host FROM mysql.user WHERE user = 'dev_user';
```

额外检查：

- 本机 `3306` 是否已被其他进程或容器占用
- 如果是复用已有 MySQL，而不是新建容器，需要手工执行 `data/mysql/init/01-create-microservice-schemas.sql`
- 在 `user-service` 与 `auth-service` 完成建表后，再执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\seed-dev-auth-users.ps1
```

### 3.2 Redis

确认本地 Redis 可连，默认：

- host: `localhost`
- port: `6379`

### 3.3 注册中心与网关

联调前至少需要：

- `registry-server`
- `gateway`
- `course-service`
- `assignment-service`

否则无法验证：

- 已切窄路由是否实际命中新服务
- 课程/作业链路是否都能在注册中心发现

## 4. 业务联调检查项

### 4.1 学生读取可见性

目标：

- 学生调用 `GET /api/student/assignments`
- 能基于自己所在班级看到作业

必须确认：

- `assignment_classes` 中存在该作业绑定的 `class_id`
- `course-service` 内部接口能正确返回学生所属 `classIds`
- `assignment-service` 能基于这些 `classIds` 查到作业

失败时优先判断：

1. `course-service` 是否返回了空的 `classIds`
2. `assignment_classes` 是否缺少对应班级绑定
3. Feign 调用 `course-service` 是否超时或熔断

### 4.2 学生提交 -> 教师查看 -> 教师批改闭环

目标链路：

1. 学生 `POST /api/student/assignments/{id}/submit`
2. 教师 `GET /api/teacher/submissions`
3. 教师 `PUT /api/teacher/submissions/{id}/grade`
4. 学生重新读取提交记录看到批改结果

重点确认：

- Gateway 路由命中新服务
- `assignment_submissions` 写入 `sc_assignment`
- 批改后 `graded_count / submission_count / status` 已刷新

2026-05-15 本地已完成一次真实闭环验证：

1. 学生以 `X-User-Id: 42` 调用 `POST /api/student/assignments/1/submit`
2. 教师以 `X-User-Id: 7` 调用 `GET /api/teacher/submissions`
3. 教师调用 `PUT /api/teacher/submissions/1/grade`
4. 学生重新读取：
   - `GET /api/student/assignment-submissions`
   - `GET /api/student/assignments/1`
5. 教师重新读取：
   - `GET /api/teacher/assignments`

验证结果：

- 学生提交记录已返回最新 `score / teacherComment / graded / status`
- 教师作业列表已返回 `submissionCount = 1`、`gradedCount = 1`、`status = graded`
- 数据库 `sc_assignment.assignments` 中对应汇总字段已刷新，不再停留在旧值

本轮额外修复：

- `JdbcAssignmentRepository` 行映射从直接强转 `Integer` 改为兼容 `Long` 数值，消除 MySQL/JDBC 返回 `Long` 时的 `ClassCastException`
- `JdbcAssignmentRepository.updateAssignment(...)` 已补 `graded_count = :gradedCount` 持久化，避免批改后汇总未落库

### 4.3 教师主写切流验证

2026-05-15 前后已完成以下两轮验证：

1. 直连 `assignment-service` 的教师主写预演
2. 经 `auth-service -> gateway -> assignment-service` 的教师主写实流验证

2026-05-15 本地已完成直接命中 `assignment-service` 的教师主写预演：

1. 教师 `POST /api/teacher/assignments` 新建作业到 `courseId = 2`
2. 学生 `GET /api/student/assignments` 可立即看到新作业
3. 教师将同一作业 `PUT` 更新到 `courseId = 4`
   - `courseId = 4` 仅绑定 `classId = 3`
   - `classId = 3` 下没有 `studentId = 42`
4. 学生再次读取作业列表时，该作业已从可见范围中消失
5. 教师 `DELETE /api/teacher/assignments/{id}` 后，教师列表、学生列表、`assignment_classes` 与 `assignments` 表均完成回收

本轮验证说明：

- 教师主写在新服务内部已具备“创建 -> 更新班级范围 -> 学生可见性变化 -> 删除回收”的闭环能力
- `assignment_classes` 会随教师更新课程/班级范围而同步更新
- 学生可见性当前能正确跟随 `course-service` 聚合出的班级关系变化
- 通过 `teacher7 / Teach1234` 登录取得 Bearer Token 后，`POST /api/teacher/assignments` 已可经 `gateway` 成功写入 `sc_assignment`

当前仍未验证的点：

- 生产口径下是否需要保留对 `user-service` 不可用时的更明确降级表现（当前列表读取可退化为缺少姓名字段）
- 切流后的显式回滚演练是否要做成脚本化步骤
- `AssignmentSubmittedEvent` 的消费侧是否已经完成真正的业务处理联调

## 5. 建议的联调顺序

1. 起 MySQL / Redis，并确认多 schema 与 `dev_user` 已创建
2. 启动 `registry-server`
3. 启动 `gateway`
4. 启动 `course-service`
5. 启动 `assignment-service`
6. 验证学生读取是否能通过 `course-service -> assignment_classes` 聚合正常返回
7. 再验证学生提交、教师查看、教师批改闭环
8. 验证 `AssignmentSubmittedEvent` 是否从 `outbox_event` 成功 relay 到 RabbitMQ
9. 最后验证教师主写经 Gateway 的登录、建单、更新、删除闭环

## 变更记录

| 日期 | 变更人 | 变更内容 |
| ---- | ------ | -------- |
| 2026-05-14 | Codex | 新增 Assignment 联调检查清单，固化本地多 schema 依赖、`class_students` 风险与切流前检查项。 |
| 2026-05-14 | Codex | 补充本机 `3306` 端口占用与 `qimo-redis` 容器名冲突这两类真实启动阻塞。 |
| 2026-05-15 | Codex | 将学生读取链路更新为经 `course-service` 聚合班级 ID，并记录本地 `GET /api/student/assignments` 已从 500 修复为 200。 |
| 2026-05-15 | Codex | 补充学生提交 -> 教师查看/批改 -> 学生重新读取的真实闭环验证结果，并记录 `graded_count` 与数值类型兼容性修复。 |
| 2026-05-15 | Codex | 补充教师主写 `POST / PUT / DELETE` 的本地预演结果，确认创建、班级范围更新与删除回收在新服务内部已能闭环。 |
| 2026-05-15 | Codex | 补充 Gateway 鉴权链路下的教师主写实流验证，并新增可重复执行的本地开发鉴权种子脚本。 |
| 2026-05-15 | Codex | 补充 `AssignmentSubmittedEvent` 的真实 RabbitMQ relay 验证结果，确认 `relay-smoke-evt-1` 已发布并生成 inspector 队列。 |
| 2026-05-15 | Codex | 补充 `EarlyWarningRaisedEvent` 的真实通知消费验证，记录 `warning-smoke-2` 已落库并可通过学生通知接口读取。 |
