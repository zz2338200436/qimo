# 需求文档：Spring Cloud 微服务化改造与既有系统优化

## Introduction（概述）

本需求文档针对现有单体 Spring Boot 应用 `major_assignment`（智能学习辅助系统）进行两个层面的改造：

1. **现状治理**：在不改变业务行为的前提下，修复当前已识别的代码质量、安全、可维护性、可观测性等方面的问题。
2. **架构升级**：将后端由单体 Spring Boot 架构演进为 Spring Cloud 微服务架构，以 Spring Boot 3.x 作为每个微服务的运行时框架，建立网关、注册中心、配置中心、服务治理、链路追踪、熔断限流、集中式会话 / 令牌鉴权、容器化部署等完整的云原生能力。

改造目标是：在保留既有业务功能（学生/教师的登录、课程、作业、考试、知识点、学情预警、通知、AI 辅助、数据看板）可用的前提下，渐进式完成微服务拆分与平台化治理，并沉淀一套可供后续演进的完整文档。

说明：本文档定义目标需求与验收口径，不逐项记录当前已经切换的接口、任务勾选进度或 Gateway 细粒度路由范围；这些实施状态以 `.kiro/specs/spring-cloud-migration/tasks.md` 为准。

## Glossary（术语表）

- **System（本系统）**：当前的 `major_assignment` 单体应用，也是本次改造的输入。
- **Target_Platform（目标平台）**：改造完成后的 Spring Cloud 微服务平台总称，包含网关、注册中心、配置管理方式（阶段 2 可由本地配置 + 环境变量承接）与若干业务微服务。
- **Gateway（网关服务）**：基于 Spring Cloud Gateway 的统一入口服务，负责路由、鉴权、限流、CORS、日志。
- **Registry（注册中心）**：服务注册与发现组件（候选：Nacos、Eureka、Consul）。
- **Config_Center（配置中心）**：集中式配置管理组件（候选：Nacos Config、Spring Cloud Config、Apollo）；在阶段 2 过渡期允许暂由服务本地配置与环境变量承接。
- **Auth_Service（认证授权服务）**：提供用户登录、令牌颁发、令牌校验、角色查询的微服务。
- **User_Service（用户服务）**：管理用户基础信息与角色关系的微服务。
- **Course_Service（课程服务）**：管理课程、班级与选课关系的微服务。
- **Assignment_Service（作业服务）**：管理作业发布、提交、批改的微服务。
- **Exam_Service（考试服务）**：管理考试、试题、考试提交与阅卷的微服务。
- **Analysis_Service（分析服务）**：负责成绩分析、知识点掌握度分析、学情早期预警的微服务。
- **Notification_Service（通知服务）**：负责站内通知与消息推送的微服务。
- **AI_Service（AI 辅助服务）**：负责题目生成、试卷生成、学习建议等 AI 能力的微服务。
- **Monitoring_Stack（可观测性套件）**：由指标、日志聚合与链路追踪组件组成的可观测性系统（候选：Prometheus + Grafana + Loki/ELK + Tempo/Zipkin/SkyWalking）。
- **Access_Token（访问令牌）**：用户登录后由 Auth_Service 颁发、在网关与下游服务间传递的身份凭证（候选：JWT）。
- **Refresh_Token（刷新令牌）**：用于续期 Access_Token 的凭证。
- **Legacy_Session（遗留会话机制）**：当前基于 `MultiRoleSessionManager` + 多 Cookie（`JSESSIONID_TEACHER` / `JSESSIONID_STUDENT` / `JSESSIONID_ADMIN`）+ Redis 的自研多角色会话方案。
- **EARS**：Easy Approach to Requirements Syntax，需求文档使用的验收标准语法。
- **INCOSE_Rules**：本文档遵循的需求质量规则（清晰、可测、完整、积极表述）。
- **Diagnosis_Report（诊断报告）**：本次改造交付的“现状问题清单”文档。
- **Migration_Plan（迁移方案）**：本次改造交付的“迁移步骤与里程碑”文档。
- **Architecture_Document（架构文档）**：本次改造交付的目标架构设计文档。

## Requirements（需求）

---

### Requirement 1：现状诊断与问题清单交付

**User Story:** 作为项目负责人，我希望获得一份覆盖代码质量、架构、安全、性能、可观测性等维度的现状诊断报告，以便在启动微服务化改造前对齐问题范围与优先级。

#### Acceptance Criteria

1. THE Diagnosis_Report SHALL 覆盖以下六个维度：架构与分层、安全与认证、代码质量与可维护性、性能与数据访问、可观测性、前后端与部署。
2. THE Diagnosis_Report SHALL 为每个识别出的问题记录以下字段：问题编号、问题描述、证据（涉及的文件或配置）、影响等级（高 / 中 / 低）、建议的改造阶段（现状治理 / 微服务化前置 / 微服务化阶段）。
3. THE Diagnosis_Report SHALL 至少列出以下已知问题：`MultiRoleSessionManager` 与 Spring Security 双轨鉴权造成的一致性风险、`MajorAssignmentApplication` 中硬编码的 CORS 白名单、`application.properties` 中明文的数据库账号口令、`SecurityConfig` 对 `/api/teacher/**`、`/api/student/**` 等路径全量放行 CSRF、`AIController` 中未实现的 AI 能力占位数据、`BaseController` 同时依赖 `HttpServletRequest` 与 `HttpSession` 导致登录态读取路径不一致、日志切面与认证切面对 session 的重复读取、Mapper XML 仅覆盖 3 个表而其它 Mapper 使用注解式 SQL。
4. IF 在诊断过程中发现未在上述清单列出的新问题，THEN THE Diagnosis_Report SHALL 将该问题追加到问题清单并标注发现来源。
5. THE Diagnosis_Report SHALL 以 Markdown 文档形式交付，路径为 `docs/diagnosis-report.md`。

#### Correctness Properties

- **Invariant（完整性不变量）**：Diagnosis_Report 中每一条问题记录的六个字段都必须非空，缺失字段视为报告不完整。
- **Metamorphic（度量关系）**：Diagnosis_Report 中“高影响”问题的数量不得少于在 `docs/migration-plan.md` 中被标记为“必须在阶段 1 解决”的问题数量，确保二者口径一致。

---

### Requirement 2：现状治理 —— 鉴权与会话机制统一

**User Story:** 作为安全负责人，我希望在微服务化之前先将当前双轨（`MultiRoleSessionManager` + Spring Security）鉴权收敛为单一、可审计的机制，以消除不同入口得到不同登录态判断的风险。

#### Acceptance Criteria

1. THE System SHALL 在单体阶段保留一条统一的登录态读取路径，所有 Controller 通过 `BaseController` 暴露的方法获取 `userId` 与 `roles`，不再直接读取 `HttpSession` 属性。
2. WHEN 请求进入 `MultiRoleSessionFilter`，THE System SHALL 将会话信息同步写入 `SecurityContextHolder`、请求属性与标准 `HttpSession` 中的同一组键，并在同一请求生命周期内保证三者一致。
3. IF `MultiRoleSessionFilter` 未能从 Redis 解析到有效会话数据，THEN THE System SHALL 使 `SecurityContextHolder` 对本次请求保持匿名状态，不得回退写入任何已认证主体。
4. THE System SHALL 在 `SecurityConfig` 中停止对 `/api/teacher/**`、`/api/student/**`、`/api/knowledge-points/**`、`/api/early-warnings/**`、`/api/notifications/**` 的 CSRF 全量忽略，改为由网关或令牌鉴权负责同源校验。
5. WHERE 后端仍处于单体部署阶段，THE System SHALL 保留 Kaptcha 验证码校验逻辑，且验证码键值不得与业务会话键值共用同一 Redis key 前缀。

#### Correctness Properties

- **Invariant（会话一致性不变量）**：对同一请求，`BaseController.getCurrentUserId(request)`、`BaseController.getCurrentUserId(session)` 与 `SecurityContextHolder.getContext().getAuthentication()` 三者返回的用户身份必须同时为空或同时指向同一 `userId`。
- **Idempotence（幂等性）**：对同一个有效请求多次调用 `MultiRoleSessionFilter.doFilter` 不得产生不同的 `SecurityContext` 内容；第二次调用的上下文必须与第一次相同。

---

### Requirement 3：现状治理 —— 统一异常、响应与日志

**User Story:** 作为研发成员，我希望后端的错误返回、成功返回与日志格式稳定统一，以便前端、网关和运维采用一致的处理规则。

#### Acceptance Criteria

1. THE System SHALL 使所有 `@RestController` 的返回值统一封装为 `ResponseResult<T>` 或其派生结构，禁止直接返回未包装的 `Map`、`String`、实体对象。
2. WHEN 控制器方法抛出 `BusinessException`、`UnauthorizedException`、`ForbiddenException`、`ResourceNotFoundException`、`MethodArgumentNotValidException`、`ConstraintViolationException`、`BindException`、`IllegalArgumentException` 之一，THE System SHALL 由 `GlobalExceptionHandler` 统一转换为 `ResponseResult`，HTTP 语义化状态码与 `code` 字段保持一致。
3. IF 环境变量中激活的 Spring Profile 包含 `prod`，THEN THE System SHALL 在未知异常响应中隐藏异常堆栈与内部消息，仅返回通用错误文案。
4. THE System SHALL 使用统一的 MDC 上下文记录 `requestId`、`userId`、`role`，`LoggingAspect` 与 `MultiRoleSessionFilter` 均从该 MDC 上下文读取与写入，避免各自重复查询会话。
5. WHERE 日志需要输出到文件，THE System SHALL 采用 JSON 结构化日志格式，字段至少包含 `timestamp`、`level`、`logger`、`thread`、`requestId`、`userId`、`message`、`stacktrace`。

#### Correctness Properties

- **Invariant（响应契约不变量）**：任意 REST 响应体 `r` 必须满足 `r.success == (200 <= r.code < 300)`，且 `r.message` 非空。
- **Metamorphic（日志一致性）**：对同一次 HTTP 请求，`LoggingAspect` 记录的 `userId` 与 `GlobalExceptionHandler` 记录的 `userId`（若存在）必须相同。

---

### Requirement 4：现状治理 —— 配置与敏感信息外置

**User Story:** 作为运维，我希望数据库、Redis、邮件、AI API Key 等敏感配置不再以明文方式写死在代码仓库中，以便安全合规并为配置中心接入做准备。

#### Acceptance Criteria

1. THE System SHALL 把 `application.properties` 中的 `spring.datasource.username`、`spring.datasource.password`、`spring.data.redis.password` 等敏感字段迁移为 `${ENV_NAME}` 占位符，默认值仅用于本地开发。
2. WHEN 应用启动时读取敏感配置，THE System SHALL 允许通过环境变量或外部配置文件注入真实值，且在日志中对敏感字段打码输出。
3. THE System SHALL 为 `dev`、`test`、`prod` 三种 Profile 分别提供独立的配置文件（或配置片段），避免跨环境泄露。
4. IF 环境变量缺失必需的敏感字段并且当前 Profile 为 `prod`，THEN THE System SHALL 在启动阶段快速失败并输出明确的启动失败原因。
5. WHERE Target_Platform 已完成部署，THE System SHALL 改由 Config_Center 统一下发非本地配置，应用本地仅保留最小引导配置（bootstrap）。

#### Correctness Properties

- **Invariant（安全不变量）**：对任意提交到 Git 的配置文件 `f`，扫描 `f` 不得命中明文密码、API Key、私钥模式（正则可枚举）。

---

### Requirement 5：现状治理 —— 数据访问与缓存规范

**User Story:** 作为研发成员，我希望数据访问与缓存策略有统一的规范，以避免出现注解式 SQL 与 XML SQL 混用、缓存 key 冲突、缓存穿透等问题。

#### Acceptance Criteria

1. THE System SHALL 在单体阶段统一 Mapper 的 SQL 组织方式：对复杂 SQL 使用 XML，对简单 CRUD 使用注解，并在 `docs/coding-guidelines.md` 中以表格形式说明“复杂 SQL”的判定条件。
2. WHEN `Service` 层方法被 `@Cacheable` 标注，THE System SHALL 在 `CacheConstants` 中显式声明其所使用的缓存名、key 表达式与 TTL，禁止使用默认 TTL。
3. IF 同一缓存 key 既被多次读取又被多次失效，THEN THE System SHALL 使用 `@Caching` 组合注解显式列出所有 `@CacheEvict` 策略，避免遗漏。
4. WHERE 需要分布式锁，THE System SHALL 统一使用基于 Redis 的 `setIfAbsent` 封装（由 `RedisCacheService` 提供），锁 key 前缀固定为 `LOCK:{业务域}:{资源ID}`。
5. THE System SHALL 为所有查询型接口提供分页参数上限（单页最大条数 100），防止意外全表扫描。

#### Correctness Properties

- **Invariant（缓存 key 唯一性）**：对任意 `@Cacheable` 注解 `a`，其 `(cacheName, key)` 组合在整个应用中必须唯一可推导。
- **Round-Trip（缓存读写一致性）**：对任意被 `@Cacheable` 覆盖的查询方法 `f` 与对应的 `@CacheEvict` 写方法 `g`，先调用 `f(x)` 后调用 `g(x)` 再次调用 `f(x)` 的结果必须反映 `g` 引起的变化，不得继续返回旧值。

---

### Requirement 6：微服务拆分与边界

**User Story:** 作为架构师，我希望按业务领域将单体拆分为独立部署的微服务，使每个服务只承担一个明确职责，从而降低耦合并支持按需扩展。

#### Acceptance Criteria

1. THE Target_Platform SHALL 至少包含以下业务微服务：Auth_Service、User_Service、Course_Service、Assignment_Service、Exam_Service、Analysis_Service、Notification_Service、AI_Service，以及平台级服务 Gateway、Registry、Config_Center。
2. THE Target_Platform SHALL 为每个业务微服务定义独立的 Maven 子模块、独立的启动类、独立的端口与独立的 `application.yml`。
3. THE Target_Platform SHALL 使每个微服务的 `spring.application.name` 与其在 Registry 中的注册名一致；Gateway 路由 ID 可以按业务链路拆分为多个细粒度路由，但这些路由必须稳定指向唯一的目标微服务。
4. WHEN 领域内的实体或 Mapper 被迁移到对应微服务，THE Target_Platform SHALL 在原单体模块中移除该实体与 Mapper，不得保留重复定义。
5. IF 两个微服务之间存在强一致性需求并且无法通过事件解耦，THEN THE Target_Platform SHALL 将涉及的聚合归并回同一个微服务，不得以分布式事务跨服务强绑定。
6. WHERE 某个微服务承担报表或批处理职能（例如 Analysis_Service 的成绩分析），THE Target_Platform SHALL 允许该服务以异步或定时任务方式运行，与在线请求链路解耦。

#### Correctness Properties

- **Invariant（服务边界不变量）**：对任意业务实体 `E`，在 Target_Platform 的所有微服务源码中，`E` 的权威定义（含 Mapper 与数据库表映射）必须且仅存在于一个微服务内。
- **Metamorphic（一致性映射）**：对任意业务功能点 `F`，其在单体中对应的 URL、方法与角色组合经网关路由映射后，必须对应到且仅对应到一个目标微服务。

---

### Requirement 7：Spring Cloud 技术组件选型

**User Story:** 作为架构师，我希望明确 Spring Cloud 生态中各能力点的选型，以便后续实施有统一依据并避免组件冲突。

#### Acceptance Criteria

1. THE Target_Platform SHALL 以 Spring Boot 3.x（≥ 3.5.3）作为每个微服务的基础框架，并锁定与之匹配的 Spring Cloud 版本（当前基线：2025.0.x）。
2. THE Target_Platform SHALL 使用 Spring Cloud Gateway 作为统一网关，不使用 Zuul。
3. THE Target_Platform SHALL 使用 Nacos 或 Eureka 中的一项作为 Registry，并在 `docs/architecture.md` 中说明选择理由与备选方案。
4. THE Target_Platform SHALL 在目标态使用 Nacos Config、Spring Cloud Config 或 Apollo 中的一项作为 Config_Center；在阶段 2 过渡期允许先使用 `application.yml` + 环境变量承接配置管理。
5. THE Target_Platform SHALL 使用 Spring Cloud OpenFeign 作为同步 RPC 客户端，并搭配 Spring Cloud LoadBalancer 做客户端负载均衡。
6. THE Target_Platform SHALL 使用 Resilience4j 或 Sentinel 中的一项作为熔断限流组件，并在所有跨服务的 Feign 调用上启用。
7. THE Target_Platform SHALL 使用 Micrometer + Prometheus 采集指标，使用 SkyWalking 或 OpenTelemetry + Zipkin 采集链路，使用 ELK 或 Loki 聚合日志。
8. WHERE 业务需要异步消息驱动，THE Target_Platform SHALL 使用 Spring Cloud Stream 封装底层 MQ（候选：RabbitMQ、RocketMQ、Kafka），或提供与之等价的统一事件发布/消费抽象，应用代码不得在业务层散落对具体 MQ 客户端的直接依赖。
9. IF 组件选型决策被修改，THEN THE Architecture_Document SHALL 更新对应章节并记录变更日期与决策者。

#### Correctness Properties

- **Invariant（依赖一致性）**：对任意微服务的 `pom.xml`，其 `spring-boot.version` 与 `spring-cloud.version` 必须与 `docs/architecture.md` 中声明的版本一致；不一致视为选型偏离。

---

### Requirement 8：API 网关

**User Story:** 作为前端与外部调用方，我希望所有 API 通过一个稳定的入口访问，无需感知后端服务的部署拓扑。

#### Acceptance Criteria

1. THE Gateway SHALL 基于 Spring Cloud Gateway 实现，暴露 HTTP/HTTPS 端口并注册到 Registry。
2. THE Gateway SHALL 按路径前缀或更细粒度的“路径 + HTTP 方法”组合将请求路由到对应微服务；在迁移过渡期允许同时存在窄路由切流与 `legacy-route` 兜底。
3. THE Gateway SHALL 校验 Access_Token 的签名与有效期，校验通过后将 `userId`、`roles`、`traceId` 以约定的请求头透传到下游服务。
4. IF Access_Token 缺失或无效，THEN THE Gateway SHALL 对需要鉴权的路由返回 401，并在响应体中使用与 `ResponseResult` 一致的 JSON 结构。
5. THE Gateway SHALL 对所有路由开启请求日志与链路追踪 TraceId，TraceId 由网关生成并透传到下游。
6. THE Gateway SHALL 在跨域处理上统一配置 CORS，允许来源列表通过 Config_Center 或网关本地配置热更新，移除单体阶段 `MajorAssignmentApplication` 中硬编码的 `localhost:8080` 配置。
7. WHERE 某个路由启用了限流策略，THE Gateway SHALL 按“客户端 IP + 用户 ID + 路由 ID”作为限流键，触发限流时返回 429 与可读的错误文案。

#### Correctness Properties

- **Invariant（鉴权不变量）**：对任意需要鉴权的下游接口 `I`，下游服务从请求头读取到的 `userId` 必须等于 Gateway 校验 Access_Token 时解析出的 `userId`。
- **Round-Trip（请求追踪不变量）**：对任意一次经过 Gateway 的请求，Gateway 日志与下游服务日志中的 TraceId 必须一致，可通过 TraceId 还原完整调用链。

---

### Requirement 9：统一认证与令牌鉴权

**User Story:** 作为用户，我希望登录一次后即可访问多个微服务的受保护接口，而无需在每个服务之间单独维护会话。

#### Acceptance Criteria

1. THE Auth_Service SHALL 提供 `POST /api/auth/login`、`POST /api/auth/refresh`、`POST /api/auth/logout`、`GET /api/auth/me` 四个基础接口。
2. WHEN 登录凭证与验证码校验通过，THE Auth_Service SHALL 颁发一对 Access_Token 与 Refresh_Token，其中 Access_Token 的默认有效期不超过 30 分钟，Refresh_Token 的默认有效期不超过 7 天，有效期可通过 Config_Center 调整。
3. THE Access_Token SHALL 使用 JWT 格式，Claims 至少包含 `sub`（用户 ID）、`roles`、`iat`、`exp`、`jti`。
4. IF 用户主动登出或管理员强制下线，THEN THE Auth_Service SHALL 将对应 Refresh_Token 与 `jti` 写入 Redis 黑名单，并使其在剩余有效期内均无法续期。
5. THE Gateway 与下游服务 SHALL 仅信任由 Auth_Service 签名的 Access_Token，签名密钥通过 Config_Center 下发并支持热轮换。
6. THE Target_Platform SHALL 在单体向微服务过渡期间，使 Legacy_Session 机制与 JWT 机制同时可用，但对同一用户不得同时签发两套凭证。
7. WHERE 多角色（学生、教师、管理员）用户同时在同一浏览器下登录，THE Auth_Service SHALL 明确并固定一种角色令牌策略，并在 `docs/auth-design.md` 中说明；该策略可以是“单 Token + activeRole + 切换接口”，也可以是“每角色独立 Token”，但在同一阶段不得两种策略并存。

#### Correctness Properties

- **Round-Trip（令牌编解码不变量）**：对任意合法 JWT `t`，`verify(sign(decode(t))) == true` 且解析出的 Claims 与原始 Claims 等价。
- **Invariant（黑名单不变量）**：任意被写入黑名单的 `jti`，在其原始有效期内被网关解析时必须被判定为无效 Token。
- **Idempotence（登出幂等性）**：对同一 Refresh_Token 多次调用 `/api/auth/logout` 仅会将其加入黑名单一次，后续调用返回幂等成功响应，不得抛出异常。

---

### Requirement 10：服务间通信

**User Story:** 作为服务开发者，我希望在微服务之间调用时使用类型安全、具备容错能力的统一方式，而不是自行拼 URL。

#### Acceptance Criteria

1. THE Target_Platform SHALL 使用 OpenFeign 作为同步调用方式，每个被调用服务 SHALL 对外暴露独立的 `{service}-api` 模块，集中维护 Feign 接口与 DTO。
2. WHEN Feign 客户端发起调用，THE Target_Platform SHALL 自动透传 TraceId、`userId`、`roles` 请求头，无需业务代码手工设置。
3. IF 目标服务返回 4xx/5xx，THEN THE Target_Platform SHALL 由统一的 `ErrorDecoder` 将其转换为对应的业务异常（例如 `RemoteServiceException`），避免调用方直接感知 HTTP 状态码。
4. THE Target_Platform SHALL 对所有 Feign 调用启用熔断、超时与重试策略，默认超时 3 秒、重试 0 次，具体值可通过 Config_Center 覆盖。
5. WHERE 调用链路中存在异步事件（例如考试结束后触发学情分析），THE Target_Platform SHALL 通过统一事件发布机制发布领域事件，消费端保证至少一次消费与幂等处理；在目标态优先采用 Spring Cloud Stream + MQ Binder，在过渡期允许先以本地消息表 / outbox 打底后再接入统一消息抽象。

#### Correctness Properties

- **Idempotence（事件消费幂等性）**：对任意领域事件 `e`，消费端对 `e` 处理两次与处理一次的业务副作用必须等价。
- **Metamorphic（错误映射一致性）**：对任意下游返回的 `HttpStatus == 4xx` 响应，调用方捕获到的异常必须属于“客户端错误”分类；对 `5xx` 响应必须属于“服务端错误”分类，不得混淆。

---

### Requirement 11：熔断、限流与降级

**User Story:** 作为运维，我希望系统在单个服务故障、流量突增时不被整体拖垮，并可在故障时返回可控的降级响应。

#### Acceptance Criteria

1. THE Target_Platform SHALL 在 Gateway 与每个微服务的对外关键接口上启用限流规则，默认单实例 QPS 上限在 `docs/architecture.md` 中显式声明。
2. THE Target_Platform SHALL 为每个 Feign 调用配置熔断器，熔断阈值（错误率、慢调用比例、最小样本数）在 Config_Center 中可配。
3. WHEN 熔断器进入打开状态，THE Target_Platform SHALL 调用预定义的 Fallback 方法返回降级响应，Fallback 方法必须显式声明，禁止使用返回 `null` 的隐式降级。
4. IF 某个服务连续 N 秒（默认 30）的错误率高于阈值，THEN THE Monitoring_Stack SHALL 触发告警并通知预定义的接收人。
5. WHERE 某个接口被标注为“核心接口”（如登录、考试提交），THE Target_Platform SHALL 为其配置比非核心接口更宽松的限流阈值与更严格的熔断阈值，并在文档中列出核心接口清单。

#### Correctness Properties

- **Invariant（降级响应格式不变量）**：任意 Fallback 响应体必须符合 `ResponseResult` 结构，且 `success == false`、`code` 属于 `[503, 504, 429]` 之一。

---

### Requirement 12：数据库拆分策略

**User Story:** 作为数据负责人，我希望明确每个微服务的数据所有权，避免多个服务直接读写同一张业务表导致数据不一致。

#### Acceptance Criteria

1. THE Target_Platform SHALL 为每个业务微服务分配独立的数据库 Schema（物理或逻辑均可），并在 `docs/data-ownership.md` 中列出“服务—表”所有权矩阵。
2. WHEN 微服务 A 需要访问属于微服务 B 的数据，THE Target_Platform SHALL 通过 B 的 OpenFeign 接口访问，不得跨库直连。
3. IF 出于报表或离线分析需要跨库数据，THEN THE Target_Platform SHALL 通过数据同步管道（CDC 或定时同步）构建只读副本，而非允许在线服务跨库查询。
4. THE Target_Platform SHALL 在过渡期允许多个微服务共享同一物理数据库但使用不同 Schema，过渡期结束时间在 Migration_Plan 中明确。
5. WHERE 涉及跨服务事务，THE Target_Platform SHALL 优先采用本地消息表或 Saga 模式，不强制引入 XA 分布式事务。

#### Correctness Properties

- **Invariant（所有权唯一性）**：对任意业务表 `T`，其写权限所有者在“服务—表”所有权矩阵中必须且仅有一个。

---

### Requirement 13：可观测性

**User Story:** 作为运维与开发，我希望能通过日志、指标、链路三位一体地观察系统运行状态，快速定位问题。

#### Acceptance Criteria

1. THE Target_Platform SHALL 为每个微服务暴露 `/actuator/health`、`/actuator/info`、`/actuator/prometheus` 端点，并在生产环境中限制访问来源。
2. WHEN 请求经过 Gateway 或任意微服务，THE Target_Platform SHALL 在日志中输出 `traceId`、`spanId`、`service.name`、`userId`、`uri`、`status`、`elapsed_ms`。
3. THE Target_Platform SHALL 向 Prometheus 暴露的核心指标至少包含：JVM 指标、HTTP 请求量与耗时分布、数据库连接池占用、Redis 命中率、Feign 调用成功率与延迟、熔断器状态。
4. THE Target_Platform SHALL 将所有服务日志汇聚到集中式日志系统，保留时间不少于 30 天。
5. IF 单个服务实例在 1 分钟内的 5xx 比例高于阈值（默认 5%），THEN THE Monitoring_Stack SHALL 触发告警。
6. WHERE 启用了链路追踪，THE Target_Platform SHALL 支持基于 TraceId 在日志系统中检索到整条调用链的全部日志。

#### Correctness Properties

- **Invariant（链路完整性）**：对任意 TraceId，链路追踪系统中的 Span 必须形成连通的树；不得出现孤立 Span 或缺失根 Span。

---

### Requirement 14：前后端分离与静态资源托管

**User Story:** 作为前端开发，我希望前端与后端可以独立构建与部署，不再由 Spring Boot 托管 HTML 与 JS 资源。

#### Acceptance Criteria

1. THE Target_Platform SHALL 将当前 `src/main/resources/static` 下的前端资源迁出为独立项目，构建产物通过 Nginx 或对象存储 + CDN 托管。
2. WHEN 前端访问后端 API，THE Target_Platform SHALL 经由 Gateway 统一路由，不再依赖 Spring Boot 内嵌 Tomcat 的静态资源路径。
3. THE Target_Platform SHALL 在网关或 Nginx 层配置 CORS 白名单，移除 `MajorAssignmentApplication.corsConfigurer` 中硬编码的来源。
4. WHERE 过渡阶段需要保留单体对静态资源的兼容访问，THE Target_Platform SHALL 在 Migration_Plan 中标明过渡期最晚截止时间。

#### Correctness Properties

- **Invariant（部署解耦不变量）**：完成本需求后，任意微服务的构建产物必须不包含前端 HTML/JS/CSS 文件。

---

### Requirement 15：部署与 DevOps

**User Story:** 作为运维，我希望每个微服务都有可复用的镜像构建与部署方式，并能在同一套 CI/CD 流水线中统一管理。

#### Acceptance Criteria

1. THE Target_Platform SHALL 为每个微服务提供 `Dockerfile` 与多阶段构建配置，镜像基础层使用 OpenJDK 17 精简发行版。
2. THE Target_Platform SHALL 提供一个 `docker-compose.yml`（用于本地开发）和一套 Kubernetes 清单或 Helm Chart（用于测试/生产）。
3. WHEN CI 流水线执行时，THE Target_Platform SHALL 依次执行：单元测试、集成测试、代码质量扫描、镜像构建、镜像推送、部署到测试环境。
4. IF 任意阶段失败，THEN THE CI 流水线 SHALL 立即终止并将失败原因写入流水线制品，不得部署不完整的版本。
5. THE Target_Platform SHALL 使部署环境的 Spring Profile 与 Config_Center 命名空间一一对应（例如 `dev` ↔ `dev` 命名空间）。

#### Correctness Properties

- **Invariant（可重复构建不变量）**：对同一 Git 提交哈希 `c` 构建两次，得到的镜像层校验和（除时间戳外）必须一致。

---

### Requirement 16：迁移路径与兼容性

**User Story:** 作为项目负责人，我希望迁移过程对业务零中断，并允许在任一里程碑回滚到上一个稳定状态。

#### Acceptance Criteria

1. THE Migration_Plan SHALL 将改造划分为至少三个阶段：阶段 1「现状治理」、阶段 2「平台搭建与首个服务剥离」、阶段 3「全量服务剥离与旧单体下线」。
2. WHEN 阶段 2 进行中，THE Target_Platform SHALL 通过网关的路由规则把尚未剥离的接口继续转发到旧单体，把已剥离的接口转发到新微服务，对前端保持 URL 不变。
3. IF 任一阶段验收失败，THEN THE Migration_Plan SHALL 允许通过“切回旧路由 + 停用新服务”的方式回滚，单次回滚的 RTO 目标不超过 30 分钟。
4. THE Migration_Plan SHALL 为每个阶段定义进入条件、退出条件、负责人、验证用例清单、回滚步骤。
5. WHERE 前端依赖的接口契约发生变更，THE Target_Platform SHALL 在至少一个发布周期内保持新旧版本并存，并通过请求头或路径前缀区分版本。
6. WHERE 某个业务域处于迁移过渡期，THE Target_Platform SHALL 允许对同一微服务使用多条窄路由逐步切流，但尚未切流的方法或路径必须继续由 `legacy-route` 或等价旧路径兜底，对前端保持 URL 与核心 JSON 契约稳定。

#### Correctness Properties

- **Metamorphic（接口兼容性）**：对任意在阶段 2 未被剥离或尚未切流的方法级接口 `I`，前端调用 `I` 经过 Gateway 后得到的响应必须与旧单体直接响应等价（状态码、JSON 字段、字段类型同构）。

---

### Requirement 17：非功能性需求

**User Story:** 作为产品负责人，我希望改造后的系统在性能、可用性、安全等方面达到可度量的目标。

#### Acceptance Criteria

1. THE Target_Platform SHALL 在压测环境下使登录、首页看板、作业列表三个核心接口的 P95 响应时间不超过 800 毫秒，单服务实例并发不低于 200 QPS。
2. THE Target_Platform SHALL 使核心链路（Gateway + Auth_Service + User_Service）的可用性目标不低于 99.5%（月度）。
3. WHEN 任一业务微服务不可用，THE Target_Platform SHALL 在网关层以 Fallback 方式返回可解释的降级响应，不得向前端返回 5xx 与空响应体。
4. IF 检测到对登录接口的异常高频请求（默认 60 秒内同一 IP 超过 30 次失败），THEN THE Target_Platform SHALL 触发限流或临时封禁并记录审计日志。
5. THE Target_Platform SHALL 使所有对外接口仅通过 HTTPS 对外暴露，内部服务间通信 SHALL 支持 mTLS（可配置）。
6. WHERE 存储敏感数据（密码、手机号、邮箱），THE Target_Platform SHALL 对密码使用 BCrypt 存储，对手机号与邮箱按业务需要脱敏展示。

#### Correctness Properties

- **Invariant（加密不变量）**：对任意新创建或修改密码的用户，数据库中存储的 `password` 字段必须匹配 BCrypt 格式（`$2a$`、`$2b$` 或 `$2y$` 前缀）；禁止明文或可逆加密。

---

### Requirement 18：文档交付物

**User Story:** 作为项目成员，我希望本次改造沉淀一套完整的文档，使新成员能够在 1 天内建立对系统的整体认知。

#### Acceptance Criteria

1. THE System SHALL 在本次改造过程中交付以下文档：`docs/diagnosis-report.md`（现状诊断）、`docs/architecture.md`（目标架构与组件选型）、`docs/auth-design.md`（认证授权方案）、`docs/data-ownership.md`（数据所有权矩阵）、`docs/migration-plan.md`（迁移步骤）、`docs/coding-guidelines.md`（编码规范）、`docs/observability.md`（可观测性方案）、`docs/deployment.md`（部署与运维手册）。
2. THE System SHALL 使用 Markdown 格式撰写上述文档，并在每篇文档顶部包含标题、版本号、最后更新日期、作者。
3. THE System SHALL 在 `docs/README.md` 中提供文档导航索引，使每篇文档均可一跳直达。
4. WHEN 架构或组件选型发生变更，THE System SHALL 在相关文档末尾维护“变更记录”表格，记录变更日期、变更人、变更内容。
5. WHERE 架构文档包含结构图，THE System SHALL 使用可版本化的文本型格式（例如 Mermaid 或 PlantUML）编写，而非仅嵌入图片。

#### Correctness Properties

- **Invariant（文档可溯源不变量）**：对本文档中的每一条需求 `R`，设计文档与迁移计划文档中至少存在一处显式引用 `R` 的编号，确保从需求到落地可追溯。

---

## Iteration and Feedback（迭代与反馈）

- 需求的任何新增、删除与修改都应在本文件中完成，并在“变更记录”表格中追加一行。
- 如果在设计阶段发现某条需求不可实现或存在歧义，应返回到本文档修订后再进入设计。
- 如果用户希望跳过评审直接进入设计阶段，请在回复中明确“Skip to Implementation Plan”。

## 变更记录

| 日期 | 变更人 | 变更内容 |
| --- | --- | --- |
| 初版 | - | 基于 `major_assignment` 当前代码库与用户诉求生成初稿 |
| 2026-05-14 | Codex | 同步过渡期配置管理、Gateway 窄路由切流、traceId 命名与角色令牌策略口径，使需求文档与当前设计一致 |
