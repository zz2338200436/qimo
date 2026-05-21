# 迁移计划

> 版本：v0.6
> 最后更新：2026-05-20
> 作者：架构组

## 0. 文档边界

本文档只记录以下内容：

- 迁移阶段与总体路径
- 里程碑、风险、回滚与切流原则
- 当前处于哪个阶段、下一阶段应聚焦什么

以下内容不再在本文逐项展开：

- 具体接口级实施状态
- Gateway 已切换到新服务的细粒度路由清单
- 每个子任务的勾选进度

上述执行细节统一以 `.kiro/specs/spring-cloud-migration/tasks.md` 为唯一真相来源。

## 1. 迁移总体路径

项目按“三阶段迁移 + 双路由过渡”推进：

1. 阶段 1 在单体 `major_assignment` 内完成鉴权、异常、日志、配置、缓存、分页等治理，先让旧系统具备可拆分条件。
2. 阶段 2 搭建 Eureka、Gateway、common、公用契约模块与可观测性栈，并优先剥离 `auth-service`、`user-service`、`course-service` 以及 Assignment 领域的首批链路。
3. 阶段 3 完成 `assignment / exam / analysis / notification / ai` 的全量拆分，最终移除 `legacy-route` 与过渡期单体。

当前仓库已处于“阶段 3 服务剥离推进中，旧单体下线准备未完成”的状态。判断依据是：

- 多模块微服务骨架、注册中心、网关、`common` 与可观测性文档已落地。
- `auth-service`、`user-service`、`course-service`、`assignment-service`、`exam-service`、`analysis-service`、`notification-service` 与 `ai-service` 均已有独立模块或切片。
- Notification 与 AI 已完成 Gateway 真实运行时 smoke；Exam、Analysis、Assignment 仍存在增强项与历史兼容边界。
- Gateway catch-all `legacy-route` 已删除，当前仅剩少量显式 `legacy-*` 路由分组，因此旧单体尚未达到完整下线条件。

## 2. 阶段 1：现状治理

### 2.1 目标与退出条件

阶段 1 的目标是“单体继续可运行，但内部治理完成”。主要退出条件如下：

- 登录态读取统一收口到共享入口。
- `ResponseResult`、全局异常处理、MDC、TraceId、结构化日志完成规范化。
- 敏感配置外置并具备 `prod` 快速失败保护。
- 缓存命名空间、分页边界、分布式锁封装完成。
- 前端静态资源已从单体部署路径中分离规划，为网关 + Nginx 过渡打底。

### 2.2 现状

阶段 1 可以视为已基本完成，仓库现状已经包含：

- 鉴权、异常、日志、配置、缓存、Actuator 与部分架构测试的治理结果。
- `major_assignment` 保留运行能力，作为阶段 2 双路由过渡兜底。

## 3. 阶段 2：平台基础设施与首服务剥离

### 3.1 基础设施搭建

已完成或已落地的基础设施包括：

- `registry-server` 作为 Eureka 注册中心。
- `gateway` 作为统一入口，承载路由、熔断、CORS 与后续统一鉴权入口。
- `common` 与 `*-service-api` 作为共享异常、响应、MDC、Feign 契约承载层。
- `docker-compose.dev.yml`、`docker-compose.obs.yml` 与 `docs/observability.md` 支撑开发与观测基线。

### 3.2 首批服务剥离现状

当前首批服务剥离状态如下：

- `auth-service` / `user-service`：已完成模块化拆分与契约分离，属于阶段 2 已落地主链路。
- `course-service`：教师课程、班级、课程分配等核心读取/维护能力已具备独立路由，是当前最成熟的业务服务之一。
- `assignment-service`：已从“内部骨架”推进到“部分外部业务链路已切流”的状态，是当前最值得持续推进的服务。

### 3.3 Assignment_Service 当前进度

截至 2026-05-15，`assignment-service` 已从“内部骨架”推进到“关键业务链路已切流并完成联调验证”的状态。

当前可以确定的是：

- 教师侧读取/主写、教师查看/批改提交、学生侧读取、学生提交等关键链路已经具备新服务承载能力。
- Gateway 已对公开作业域路由完成切换，且本地已验证 `auth-service -> gateway -> assignment-service` 的教师主写实流。
- 本地开发环境已补多 schema 初始化与可重复执行的鉴权种子脚本，联调前置准备已基本收口。

具体接口、任务勾选状态和已切换路由范围，以 `.kiro/specs/spring-cloud-migration/tasks.md` 中 Assignment 章节为准。

### 3.4 Assignment_Service 当前保守边界

当前不再保守保留教师端作业主写在单体，Assignment 域公开路由已切到新服务。当前剩余边界转为：

- `AssignmentSubmittedEvent` 已完成真实 `outbox -> RabbitMQ -> notification-service` 消费联调验证
- `assignment-service` 对 `user-service` / `course-service` 的 Feign Fallback 已补齐，仍需在更多运行时场景下继续观察
- 作业域切流后的回滚步骤仍需继续保持清晰、可执行，并逐步沉淀为演练脚本

因此，Assignment 下一步不再是“是否切教师主写”，而是“如何把事件、降级与回滚补齐到可长期维护的状态”。

## 4. 阶段 3：全量服务剥离与单体下线

### 4.1 剥离依赖顺序

后续仍按以下依赖顺序推进：

`Course -> Assignment -> Exam -> Analysis -> Notification -> AI`

其中当前真正处在实施中的，是 `Assignment_Service` 的“联调与最终切流准备”，而不是继续横向扩展到 `Exam_Service`。

### 4.2 单体下线策略

单体下线必须满足以下前置条件：

- Gateway 中不再存在 catch-all `legacy-route`，剩余显式 `legacy-*` 路由不再承接核心主链路。
- 所有 `api/**` 入口均有明确的新服务归属。
- 读写路径不再跨 `major_assignment` 与 `sc_*` 双写/双读混用。
- 至少完成一次按业务链路的灰度验证与回滚演练。

## 5. 风险与回滚

### 5.1 当前最高风险

当前最需要关注的风险已经从“能否切流”转向“切流后能否稳定维护”：

1. `notification-service` 的三类最小事件消费者、通知域读写接口、历史通知回填脚本、Gateway 配置级切流与真实路由 smoke 已补齐。
2. `assignment-service` 的 Fallback 已补齐，但仍需继续验证下游异常场景下的用户可感知行为与告警信息是否足够清晰。
3. 本地开发账号依赖脚本化种子准备，执行时机需要遵守“先 Flyway 建表、后灌入账号”的顺序。

### 5.2 回滚策略

当前阶段统一采用“窄路由回滚”：

- 只要新链路出现问题，优先把 Gateway 某一条业务路由切回对应显式 `legacy-*` 路由或上一版路由配置。
- 不对单体执行 destructive rollback，不回滚用户已有业务数据。
- 对已切流链路采用“先恢复旧路径，再排查新服务”的顺序。

## 6. 里程碑与验收

### 6.1 已达到的里程碑

- 多模块 Maven 聚合工程与基础设施骨架就绪。
- `course-service` 已具备独立业务承载能力。
- `assignment-service` 已完成教师读取、学生读取、学生提交、教师查看提交、教师批改提交这几条链路。

### 6.2 下一里程碑

下一阶段应聚焦以下验收项：

1. 将 Assignment 域的 relay 开关、检查命令与回滚步骤进一步脚本化或固化为演练清单。
2. 继续补强 Fallback 运行时验证与可观测性，确认下游抖动时错误表现与告警符合预期。
3. Notification 域接口、历史通知回填脚本与 `notification-route` 真实运行时 smoke 已补齐，AI_Service 剥离已进入 26.1 模块骨架阶段。
4. `exam-service` 已进入最小正式拆分阶段：`student exams` 列表 / 详情 / 提交链路已由正式服务承接并完成 gateway 窄切流。
5. `GET /api/student/scores` 已完成从单体兼容层迁出，现由 `exam-service` 聚合作业成绩 + 考试成绩统一承接，并已通过 `student-assignments.html` 成绩查询 tab 的真实页面回归。
6. 教师考试管理最小正式 CRUD 也已由 `exam-service` 承接，并通过 `teacher-assignments.html` 的考试管理 tab 完成真实页面回归。
7. 教师考试评分最小正式链也已由 `exam-service` 承接，并通过经 `gateway:8080` 的真实 API smoke 验证。
8. `ExamFinishedEvent` 已完成真实 `grade -> outbox -> relay -> notification-service` 联调验证：教师评分后 `sc_exam.outbox_event` 会落入 `event_id = exam-finished-9003` 并发布，`notification-service` 已成功消费并生成学生考试完成通知；重复评分已收敛为同分同评语不重复发事件、同分但评语变化生成带评语指纹的新事件。
9. Analysis 域已启动最小事件消费切片：`analysis-service` 可消费 `AssignmentSubmittedEvent / ExamFinishedEvent`，并写入 `score_trends / kp_mastery` 基础表；真实运行时 smoke 已验证 `exam-service grade -> outbox relay -> RabbitMQ -> analysis-service` 链路可落库。
10. Analysis 域已开放最小只读查询切片：`GET /api/teacher/score-trend`、`GET /api/teacher/knowledge-points/mastery/student/{studentId}/course/{courseId}` 与旧 URL 兼容的 `GET /api/knowledge-points/analysis/teacher/**` 可从 `analysis-service` 返回结构稳定的分析数据，并已通过 Gateway 路由配置测试。
11. Analysis 域已补齐最小 EarlyWarning 事件切片：低分 `ExamFinishedEvent` 会写入 `EarlyWarningRaisedEvent` outbox，目标 binding 为 `early.warning.raised`，可衔接 Notification 域现有消费者；真实 smoke 已验证 `exam.finished -> analysis-service -> early.warning.raised -> notification-service` 可生成 warning 通知。
12. Analysis 域已补可重复的本地 JDBC 集成测试：使用真实 `processed_event / score_trends / kp_mastery / outbox_event` 仓储组合验证低分 `ExamFinishedEvent` 会更新分析投影、生成预警 outbox，并对重复 `eventId` 幂等忽略。
13. Analysis 域已补历史回填脚本：可从 `sc_exam` 与 `sc_assignment` 历史表回填 `sc_analysis.score_trends / kp_mastery`，并通过 H2 MySQL mode 测试验证脚本可重复执行。
14. Notification 域已补契约集成测试：使用真实 JDBC 仓储、命令/查询服务、Controller 与事件 Handler 覆盖学生通知 HTTP 契约、教师发送/批量发送通知契约、分页 `totalPages` 兼容字段、事件落库契约与重复事件幂等。
15. Gateway 已新增统一 `notification-route -> lb://notification-service`，覆盖 `/api/notifications/**` 的 `GET/POST/PUT/DELETE`，并通过路由配置测试锁定其位于 `legacy-route` 前。
16. Notification 域已补历史通知回填脚本：从旧单体 `major_assignment.notifications` 回填到 `sc_notification.notifications`，保留阅读状态、教师 ID、关联 ID 与创建时间，并通过 H2 MySQL mode 验证脚本可重复执行。
17. Notification 域已完成真实运行时 smoke：`registry-server + notification-service + gateway` 下，使用测试 JWT 经 `gateway:8080 -> lb://notification-service` 覆盖创建、列表读取、未读数、标记已读与删除。
18. 当前 Exam、Analysis 与 Notification 域剩余差距不再是基础事件链本身，而是统一提交记录、自动阅卷增强、班级排名、完整 KnowledgePoint Controller / EarlyWarning Controller 迁移与后续 AI_Service 业务接口继续推进。
19. AI_Service 已完成最小模块骨架：新增 `ai-service-api` 与 `ai-service` 聚合模块，`ai-service` 使用 `ai-service` 注册名、8088 端口，并通过最小 Spring Boot 上下文测试验证配置可加载。
20. AI_Service 已完成 `sc_ai` 初始 Schema：`ai_prompts` 保存 prompt 模板，`ai_generations` 保存 AI 调用历史，请求/响应 JSON、状态、错误与耗时均可落库，并通过 H2 MySQL mode Flyway 迁移测试验证。
21. AI_Service 已迁入旧单体 `/api/ai` 三个兼容接口：生成题目、生成试卷、学习建议；当前通过 `AiModelClient` 模型代理边界调用默认本地 mock 实现，并将调用历史写入 `sc_ai.ai_generations`，不写课程 / 考试 / 学情业务库。
22. Gateway 已为 AI 调用配置用户维度 QPS 限流：`ai-route` 匹配 `/api/ai/**` 的 POST 请求，限流 key 包含 `clientIp:userId:ai-route`，AI route 专属令牌桶阈值由 `gateway.rate-limit.routes.ai-route` 管理。
23. AI_Service 已补 Mock 模型集成测试：`@SpringBootTest + MockMvc + H2 MySQL mode + Flyway` 覆盖 `/api/ai/generate-questions` 真实控制器链路、默认 `LocalMockAiModelClient` 输出与 `ai_generations` 调用历史落库。
24. AI_Service 已完成 Gateway 真实运行时切流验证：`registry-server + ai-service + gateway` 下，`gateway:8080 -> lb://ai-service` 覆盖三个 `/api/ai/**` POST 接口、Eureka 注册、`sc_ai.ai_generations` 落库与 AI route 专属限流 `429 / Retry-After=3`。

## 7. 阶段验收与回滚手册

### 7.1 阶段 1「现状治理」

| 项目 | 内容 |
| --- | --- |
| 进入条件 | 需求 R1~R18 已冻结；诊断范围、目标架构和测试策略已确认。 |
| 负责人 | 架构组 + 单体维护负责人。 |
| 交付物 | `docs/diagnosis-report.md`、统一鉴权入口、统一响应/异常、MDC 日志、敏感配置外置、缓存/分页治理。 |
| 退出条件 | R1.1~R1.5、R2.1~R2.5、R3.1~R3.5、R4.1~R4.5、R5.1~R5.5 的整改证据落入任务清单或测试。 |
| 验证用例清单 | BaseController 登录态一致性测试、ResponseResult/GlobalExceptionHandler 结构测试、配置 profile 快速失败测试、缓存命名与分页上限测试、诊断报告字段完整性抽检。 |
| 回滚步骤 | 回退到治理前 Git tag；恢复旧 `application.properties` 与旧 Security 配置；停止新网关入口，只保留单体 Tomcat 入口。 |
| RTO 目标 | 30 分钟内恢复单体旧版本，符合 R16.3。 |

### 7.2 阶段 2「平台搭建与首服务剥离」

| 项目 | 内容 |
| --- | --- |
| 进入条件 | 阶段 1 全部退出条件达成；Eureka、Gateway、Redis、MySQL、RabbitMQ 本地环境可启动。 |
| 负责人 | 平台负责人 + Auth/User/Course/Assignment 领域负责人。 |
| 交付物 | `registry-server`、`gateway`、`common`、`*-service-api` 契约模块、首批服务模块、Gateway 双路由机制、基础可观测性。 |
| 退出条件 | R6.1~R6.6、R7.1~R7.9、R8.1~R8.7、R9.1~R9.7、R10.1~R10.5、R11.1~R11.5 的核心能力具备测试或 smoke 证据。 |
| 验证用例清单 | Eureka 注册查询、Gateway 鉴权/透传/限流测试、路由顺序测试、Feign 契约编译测试、熔断 fallback 测试、已切流接口与旧 URL 兼容 smoke。 |
| 回滚步骤 | 将单条已切流路由从 `lb://{service}` 切回 `legacy-route`；停止对应新服务；保留数据库不做 destructive rollback；如涉及 JWT 则吊销新 token 并恢复旧登录路径。 |
| RTO 目标 | 30 分钟内完成单业务域窄路由回滚，符合 R16.3。 |

阶段 2 的关键兼容要求是 R16.2 与 R16.6：已剥离接口经 Gateway 转发到新服务，未剥离方法/路径继续由 `legacy-route` 或等价旧路径兜底，对前端保持 URL 与核心 JSON 契约稳定。

### 7.3 阶段 3「全量服务剥离与旧单体下线」

| 项目 | 内容 |
| --- | --- |
| 进入条件 | 阶段 2 验收通过；Course、Assignment、Exam、Analysis、Notification、AI 的数据所有权与路由归属已登记。 |
| 负责人 | 架构组 + 各领域服务负责人 + 运维负责人。 |
| 交付物 | 全量业务服务、历史数据回填脚本、事件驱动链路、单体下线清单、部署与回滚演练记录。 |
| 退出条件 | 所有 `/api/**` 路由均有明确新服务归属；Gateway 不再需要 `legacy-route`；`major_assignment` 不再承接核心业务读写；R12.1~R12.5、R13.1~R13.6、R14.1~R14.4、R15.1~R15.5、R16.1~R16.6、R17.1~R17.6、R18.1~R18.5 均有文档或测试证据。 |
| 验证用例清单 | 全链路登录、课程、作业、考试、分析、通知、AI smoke；事件 outbox relay 重放；`legacy-route` 删除后的 Gateway 路由测试；Jar 静态资源排除测试；Prometheus/Loki/TraceId 检索。 |
| 回滚步骤 | 按业务域恢复上一版 Gateway 路由配置；停用异常新服务；保留 outbox 消息并依靠幂等消费重放；必要时从只读备份分支 `archive/legacy-final` 重新发布单体。 |
| RTO 目标 | 单业务域回滚 ≤ 30 分钟；多业务域联动回滚需拆分为多个窄路由回滚窗口，每个窗口仍按 R16.3 控制。 |

## 8. 接口版本化策略

当接口请求或响应契约发生不兼容变更时，必须遵守以下策略：

- v1 接口保持原 URL 或原 Feign 方法签名不变，至少保留 14 天或一个完整发布周期，以满足 R16.5。
- v2 接口可以采用 `/api/v2/**` 路径前缀，也可以采用 `X-Api-Version: 2` 请求头；同一业务域内只能固定一种区分方式。
- `*-service-api` 模块中不得修改 v1 DTO 字段语义；新增字段必须保持可选；破坏性变更必须新增 v2 DTO 与 v2 Feign 方法。
- 服务端在并存期内同时实现 v1/v2；删除 v1 前必须完成前端灰度、调用量归零确认、回滚演练和变更记录。
- Gateway 路由允许对同一微服务配置多条窄路由逐步切流，未切流方法继续由显式 `legacy-*` 路由兜底，对齐 R16.6。

## 9. 旧单体下线 Check-list

旧单体下线前必须逐项确认：

- Gateway 中所有 `/api/**` 路径均已由新服务路由覆盖，`legacy-route` 已无生产流量。
- `legacy-route` 从 Gateway 配置删除，并补充路由配置测试防止重新出现。
- `legacy-adapter` 子模块只保留兼容说明，归档并冻结，不再新增业务逻辑。
- 创建只读备份分支 `archive/legacy-final`，保留最后一个可启动单体版本。
- `major_assignment` 中已迁移领域的 Controller / Mapper / Entity 不再作为生产路径使用；删除前需确认没有前端或脚本直接访问。
- 前端静态资源不再由单体 Jar 提供，过渡期最晚截止时间为阶段 3 退出评审日；该项对应 R14.4。
- 数据库所有权矩阵显示无在线跨库直连，报表或离线分析均通过只读副本、事件或回填脚本处理，覆盖 R12.3 与 R12.4。
- 可观测性中已有 Gateway、核心服务、数据库、Redis、RabbitMQ 的告警规则；1 分钟 5xx 比例超过阈值应触发告警，覆盖 R13.5。
- `docs/README.md` 已能一跳导航到 8 篇核心文档；所有架构或选型变更均写入对应文档变更记录，覆盖 R18.3 与 R18.4。

## 10. 需求可溯源矩阵

| 需求编号 | 在本文中的落点 |
| --- | --- |
| R1.1 | §2 阶段 1 目标与退出条件；§7.1 阶段 1 验收。 |
| R1.2 | §7.1 要求诊断报告字段完整性抽检。 |
| R1.3 | §2.2 现状治理结果；任务清单维护具体问题证据。 |
| R1.4 | §7.1 诊断报告字段完整性抽检与问题追加流程。 |
| R1.5 | §7.1 交付物包含 `docs/diagnosis-report.md`。 |
| R2.1 | §2 阶段 1 目标与退出条件。 |
| R2.2 | §7.1 BaseController 登录态一致性测试。 |
| R2.3 | §7.1 登录态一致性与匿名状态验证。 |
| R2.4 | §7.1 Security 配置整改验证。 |
| R2.5 | §7.1 Kaptcha 与会话 key 前缀隔离验证。 |
| R3.1 | §2 阶段 1 目标与退出条件。 |
| R3.2 | §7.1 ResponseResult/GlobalExceptionHandler 结构测试。 |
| R3.3 | §7.1 prod 未知异常隐藏验证。 |
| R3.4 | §7.1 MDC 日志治理。 |
| R3.5 | §7.1 JSON 结构化日志验证。 |
| R4.1 | §2 阶段 1 目标与退出条件。 |
| R4.2 | §7.1 敏感配置外置验证。 |
| R4.3 | §7.1 profile 配置验证。 |
| R4.4 | §7.1 prod 快速失败测试。 |
| R4.5 | §7.2 平台配置能力进入条件。 |
| R5.1 | §2 阶段 1 目标与退出条件。 |
| R5.2 | §7.1 缓存命名验证。 |
| R5.3 | §7.1 缓存失效策略验证。 |
| R5.4 | §7.1 分布式锁封装验证。 |
| R5.5 | §7.1 分页上限测试。 |
| R6.1 | §1 三阶段迁移路径；§7.2 阶段 2 退出条件。 |
| R6.2 | §3 平台基础设施与服务剥离现状。 |
| R6.3 | §7.2 Eureka 注册与 Gateway 路由验证。 |
| R6.4 | §9 旧单体下线 Check-list。 |
| R6.5 | §4.2 单体下线策略。 |
| R6.6 | §4.1 剥离依赖顺序与 Analysis 批处理定位。 |
| R7.1 | §7.2 阶段 2 退出条件。 |
| R7.2 | §3.1 Gateway 作为统一入口。 |
| R7.3 | §3.1 `registry-server` 作为 Eureka 注册中心。 |
| R7.4 | §7.2 过渡期配置由本地配置与环境变量承接。 |
| R7.5 | §3.1 `*-service-api` 与 Feign 契约承载层。 |
| R7.6 | §7.2 熔断 fallback 测试与 Resilience4j/Sentinel 约束。 |
| R7.7 | §9 Prometheus/Loki/TraceId 检索与告警要求。 |
| R7.8 | §7.3 事件 outbox relay 与 RabbitMQ 重放。 |
| R7.9 | §10 本矩阵要求选型变更进入文档变更记录。 |
| R8.1 | §3.1 Gateway 作为统一入口。 |
| R8.2 | §7.2 阶段 2 双路由机制与窄路由切流。 |
| R8.3 | §7.2 Gateway 鉴权/透传测试。 |
| R8.4 | §7.2 401 响应测试。 |
| R8.5 | §7.2 TraceId 透传与请求日志验证。 |
| R8.6 | §7.2 CORS 配置测试。 |
| R8.7 | §6.2 AI route 专属限流里程碑。 |
| R9.1 | §7.2 Auth_Service 首服务剥离。 |
| R9.2 | §7.2 JWT 登录/刷新验证。 |
| R9.3 | §7.2 Access_Token claims 验证。 |
| R9.4 | §7.2 登出与黑名单回滚步骤。 |
| R9.5 | §7.2 Gateway JWT 鉴权与密钥配置。 |
| R9.6 | §7.2 双凭证互斥约束。 |
| R9.7 | §7.2 多角色令牌策略验证。 |
| R10.1 | §3.1 `*-service-api` 作为 Feign 契约承载层。 |
| R10.2 | §7.2 Gateway/Feign 透传测试。 |
| R10.3 | §7.2 Feign 错误解码和异常转换验证。 |
| R10.4 | §7.2 熔断 fallback 与超时策略验证。 |
| R10.5 | §7.3 事件 outbox relay 与幂等重放。 |
| R11.1 | §6.2 Gateway AI 用户维度 QPS 限流。 |
| R11.2 | §7.2 Feign 熔断配置。 |
| R11.3 | §7.2 fallback 显式返回验证。 |
| R11.4 | §9 5xx 告警规则。 |
| R11.5 | §7.2 核心接口限流与熔断阈值验证。 |
| R12.1 | §7.3 数据所有权与路由归属登记。 |
| R12.2 | §9 无在线跨库直连。 |
| R12.3 | §9 报表/离线分析走只读副本、事件或回填脚本。 |
| R12.4 | §9 过渡期使用独立 schema，退出阶段 3 时收敛。 |
| R12.5 | §7.3 outbox 与 Saga/幂等消费回滚策略。 |
| R13.1 | §9 可观测性端点与访问限制。 |
| R13.2 | §9 TraceId 日志检索。 |
| R13.3 | §9 Prometheus 核心指标。 |
| R13.4 | §9 集中式日志保留。 |
| R13.5 | §9 1 分钟 5xx 告警规则。 |
| R13.6 | §9 TraceId 全链路检索。 |
| R14.1 | §9 前端静态资源不再由单体 Jar 提供。 |
| R14.2 | §7.3 Gateway 统一 API 入口。 |
| R14.3 | §7.2 CORS 配置测试。 |
| R14.4 | §9 静态资源过渡期最晚截止时间为阶段 3 退出评审日。 |
| R15.1 | §7.3 部署与回滚演练。 |
| R15.2 | §7.3 全量服务 smoke 与部署验证。 |
| R15.3 | §7.3 验证用例清单与 CI 阶段。 |
| R15.4 | §7.3 任一阶段失败停止部署。 |
| R15.5 | §7.3 Spring Profile 与环境命名空间。 |
| R16.1 | §1 三阶段迁移路径。 |
| R16.2 | §7.2 双路由机制。 |
| R16.3 | §7.1~§7.3 RTO 目标。 |
| R16.4 | §7.1~§7.3 阶段表。 |
| R16.5 | §8 接口版本化策略。 |
| R16.6 | §8 未切流方法继续由 `legacy-route` 兜底。 |
| R17.1 | §7.3 全链路性能与 smoke 验证。 |
| R17.2 | §7.3 核心链路可用性目标。 |
| R17.3 | §7.3 Gateway fallback 验证。 |
| R17.4 | §7.2 登录限流与审计。 |
| R17.5 | §7.3 HTTPS/mTLS 部署验证。 |
| R17.6 | §7.1 BCrypt 与敏感字段脱敏验证。 |
| R18.1 | §0 文档边界与 §10 可溯源矩阵。 |
| R18.2 | 文档头部包含标题、版本号、最后更新日期、作者。 |
| R18.3 | §9 要求 `docs/README.md` 一跳导航到 8 篇核心文档。 |
| R18.4 | §9 要求架构或选型变更写入变更记录。 |
| R18.5 | §10 要求结构图使用可版本化文本格式，由 design.md 维护。 |

## 变更记录

| 日期       | 变更人 | 变更内容 |
| ---------- | ------ | -------- |
| 2026-05-10 | 架构组 | 初版骨架 |
| 2026-05-12 | Codex | 阶段 2 基础设施描述从 Nacos 调整为 Eureka |
| 2026-05-14 | Codex | 补全迁移总体路径，更新 Assignment_Service 当前已切链路、保守边界与下一阶段重点 |
| 2026-05-14 | Codex | 收敛文档职责，改为由 tasks.md 统一维护接口级进度与细粒度切流状态 |
| 2026-05-15 | Codex | 同步 Assignment 域已完成 Gateway 教师主写实流验证的状态，并将下一阶段重点调整为事件、Fallback 与回滚补齐。 |
| 2026-05-15 | Codex | 同步 `AssignmentSubmittedEvent` 已完成真实 RabbitMQ relay 验证的状态，并将下一阶段重点收敛到消费侧联调、回滚演练与脚本化。 |
| 2026-05-15 | Codex | 补充 Notification_Service 最小切片已启动：模块骨架、`AssignmentSubmittedEvent` 消费与学生通知最小读接口已落地，尚未切 Gateway 路由。 |
| 2026-05-15 | Codex | 补充 `AssignmentSubmittedEvent -> notification-service -> sc_notification` 真实联调已通过，通知域下一重点转为补全其余事件消费者与 Gateway 切流准备。 |
| 2026-05-15 | Codex | 补充 `ExamFinishedEvent -> notification-service -> sc_notification` 真实 smoke 已通过，并将通知域下一重点更新为 `EarlyWarningRaisedEvent`、剩余接口与 Gateway 切流准备。 |
| 2026-05-15 | Codex | 补充 `EarlyWarningRaisedEvent -> notification-service -> sc_notification` 真实 smoke 已通过，并将通知域下一重点更新为剩余接口与 `notification-route` 切流准备。 |
| 2026-05-19 | Codex | 同步 `ExamFinishedEvent` 已完成真实 `grade -> outbox -> relay -> notification-service` 联调验证，并将 Exam 域剩余重点更新为统一提交记录、自动阅卷增强与 Analysis 消费链。 |
| 2026-05-19 | Codex | 补充 `ExamFinishedEvent` 重复评分语义：同分同评语不重复写事件，同分但评语变化会生成带评语指纹的新 `event_id` 并完成运行时消费验证。 |
| 2026-05-19 | Codex | 启动 `analysis-service` 最小切片，完成 `AssignmentSubmittedEvent / ExamFinishedEvent` 消费到 `score_trends / kp_mastery` 的代码、测试与打包验证。 |
| 2026-05-19 | Codex | 补充 Analysis 最小切片运行时 smoke：`exam.finished` 经 RabbitMQ 被 `analysis-service` 消费并写入分析投影表，同时本地默认关闭 OTLP trace 导出以避免无 Collector 时刷 4318 错误日志。 |
| 2026-05-19 | Codex | 补充 Analysis 最小只读查询切片：`score-trend` 与学生课程知识点掌握度端点已由 `analysis-service` 承接，并完成 Controller 测试、打包与真实 HTTP smoke。 |
| 2026-05-19 | Codex | 完成 Analysis 最小查询端点的 Gateway 窄切流：新增 `analysis-route -> lb://analysis-service`，并通过 Eureka + Gateway 的真实路由 smoke。 |
| 2026-05-19 | Codex | 完成 Analysis 最小 EarlyWarning 事件切片：低分 `ExamFinishedEvent` 会生成 `EarlyWarningRaisedEvent` 并写入 `early.warning.raised` outbox；已完成 RabbitMQ 真实链路 smoke 并生成 `studentId=42` 的 warning 通知。 |
| 2026-05-19 | Codex | 完成 Analysis 事件链路 JDBC 集成测试：使用 H2 MySQL mode 覆盖低分考试完成事件写入分析投影、预警 outbox 与重复 eventId 幂等。 |
| 2026-05-19 | Codex | 完成 Analysis 历史数据回填脚本：从 Exam / Assignment 历史表回填成绩趋势与 KP 掌握度，并补脚本幂等测试。 |
| 2026-05-19 | Codex | 完成 Notification_Service 契约集成测试：覆盖学生通知 HTTP 契约、教师发送通知、事件落库与重复事件幂等，并修复 JDBC generated key 提取兼容性。 |
| 2026-05-19 | Codex | 完成 Gateway `notification-route` 配置级切流：`/api/notifications/**` 的读写方法指向 `lb://notification-service`，并补路由顺序测试。 |
| 2026-05-19 | Codex | 收口 Notification_Controller 业务迁移：补齐教师批量发送通知接口与分页 `totalPages` 兼容字段，并用单元测试与 JDBC 契约测试覆盖。 |
| 2026-05-19 | Codex | 完成 Notification 历史数据回填脚本：从旧单体通知表回填到 `sc_notification.notifications`，并补脚本幂等测试。 |
| 2026-05-19 | Codex | 完成 `notification-route` 真实运行时 smoke：经 Gateway 覆盖通知创建、读取、未读数、已读与删除。 |
| 2026-05-19 | Codex | 启动 AI_Service 剥离：新增 `ai-service-api` 与 `ai-service` 模块骨架，配置注册名 `ai-service` 与端口 8088，并补最小上下文测试。 |
| 2026-05-19 | Codex | 完成 AI_Service `sc_ai` 初始 Schema：新增 `ai_prompts / ai_generations` Flyway 迁移，并补 H2 MySQL mode 迁移测试。 |
| 2026-05-19 | Codex | 完成 AIController 业务接口迁移：`/api/ai/generate-questions`、`/api/ai/generate-exam`、`/api/ai/learning-suggestions` 由 `ai-service` 承接，并通过模型代理边界与历史落库测试覆盖。 |
| 2026-05-19 | Codex | 完成 AI 调用用户维度 QPS 限流配置：Gateway 新增 `ai-route` 专属令牌桶阈值，限流 key 包含 `userId` 与 routeId，并补配置/过滤器/key 解析测试。 |
| 2026-05-19 | Codex | 完成 AI_Service Mock 模型集成测试：通过 Spring Boot 测试容器验证 `/api/ai/generate-questions`、本地 mock 模型与 AI 调用历史落库链路。 |
| 2026-05-19 | Codex | 完成 AI_Service Gateway 真实运行时切流 smoke：经 `gateway:8080 -> lb://ai-service` 验证三类 AI 接口、Eureka 注册、历史落库与专属限流。 |
| 2026-05-20 | Codex | 收窄 `legacy-knowledge-route`：`/api/knowledge-points/analysis/teacher/**` 已迁入 `analysis-service` 兼容读接口，Gateway catch-all `legacy-route` 状态同步为已删除。 |
