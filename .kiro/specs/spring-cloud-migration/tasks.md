# 实施任务清单：Spring Cloud 微服务化改造与既有系统优化

> 版本：v0.1
> 最后更新：2025-xx-xx
> 对应需求：`.kiro/specs/spring-cloud-migration/requirements.md`（R1~R18）
> 对应设计：`.kiro/specs/spring-cloud-migration/design.md`
> 实施语言：**Java 17 + Spring Boot 3.5.3 + Spring Cloud 2023.0.x**（与设计文档一致，无需额外语言选择）

## Overview（概述）

本任务清单按照设计文档中的三阶段迁移路径组织：

- **阶段 1 — 现状治理**（不改架构，先治代码）：在单体 `major_assignment` 内完成鉴权、异常、日志、配置、数据访问、缓存等规范化工作，为后续拆分奠定"可抽离"基础。对应 R1~R5 与 R14 的静态资源规划部分。
- **阶段 2 — 平台基础设施搭建 + 首服务剥离（Auth_Service / User_Service）**：搭建 Nacos、Gateway、可观测性栈、common 库，完成 Auth_Service 与 User_Service 剥离，并建立网关双路由过渡机制。对应 R6~R10、R13、R14。
- **阶段 3 — 全量服务剥离与旧单体下线**：按依赖关系依次剥离 Course → Assignment → Exam → Analysis（含 KnowledgePoint）→ Notification → AI 服务；完成事件驱动补齐、接口契约版本化、旧单体下线。对应 R6、R10、R12、R16。

**任务格式约定**：

- 叶子任务控制在 1~3 小时可完成粒度；复杂服务剥离按 "模块骨架 → Feign API → 业务迁移 → 测试 → 网关切流" 细分。
- 测试相关叶子任务以 `- [ ]*` 标记为可选（遵循工作流规则：顶层任务不得带 `*`）。
- 每个任务末尾标注 `_Requirements: N[, M...]_`；带有设计文档正确性属性的任务额外标注 `_Properties: P{n} <属性名>_`。
- 文档交付类任务聚焦"文件产出"，不含用户培训 / 审批 / 上线等非编码工作。
- 父任务的说明中简要给出前置依赖。

---

## Tasks（任务）

### 阶段 0：Spec 交付物（独立于三阶段，先产出总览文档）

- [x] 1. 建立文档基线与导航索引
  - 依赖：无
  - 在仓库 `docs/` 目录下建立 8 篇核心文档的空骨架（标题 / 版本号 / 最后更新日期 / 作者 / 变更记录表格），后续阶段逐步填充
  - 以 Mermaid / PlantUML 文本化方式承载所有结构图
  - _Requirements: 18_

  - [x] 1.1 创建 `docs/README.md` 文档导航索引
    - 列出 8 篇文档的链接：`diagnosis-report.md` / `architecture.md` / `auth-design.md` / `data-ownership.md` / `migration-plan.md` / `coding-guidelines.md` / `observability.md` / `deployment.md`
    - 每个链接写明一句话用途描述
    - _Requirements: 18.1, 18.3_

  - [x] 1.2 创建 8 篇文档的骨架文件
    - 每篇文档顶部含：标题、版本号（v0.1）、最后更新日期、作者（架构组）
    - 每篇文档末尾含"变更记录"表格（列：日期 / 变更人 / 变更内容）
    - `architecture.md` 中结构图占位使用 Mermaid/PlantUML 代码块
    - _Requirements: 18.1, 18.2, 18.4, 18.5_

  - [ ]* 1.3 编写文档骨架完整性单元测试
    - 解析 Markdown，断言 8 篇文档存在且包含必要 Front-Matter 字段与"变更记录"表格
    - 断言 `docs/README.md` 中所有 8 篇链接均可被识别
    - _Requirements: 18.1, 18.2, 18.3_

---

### 阶段 1：现状治理（不改架构，先治代码）

> 前置依赖：阶段 0 完成（文档骨架到位）。
> 退出条件：诊断报告中所有"高影响"问题修复完毕；P1~P10 属性测试全部通过；CSRF 配置收敛；敏感配置外置；静态资源迁出规划落文档。

#### 诊断报告与文档输出

- [x] 2. 产出现状诊断报告 `docs/diagnosis-report.md`
  - 依赖：任务 1.2 的文档骨架
  - 覆盖六个维度：架构与分层、安全与认证、代码质量与可维护性、性能与数据访问、可观测性、前后端与部署
  - 列出需求 R1.3 中指定的 8 条已知问题证据（引用具体文件路径与行号）
  - 每条问题包含六字段：编号 / 描述 / 证据 / 影响等级 / 建议阶段 / 验证手段
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [ ]* 2.1 编写诊断报告结构完整性测试
    - 用 Markdown 解析器断言：每条问题记录包含六字段且非空
    - 断言"高影响"问题数量 ≥ `migration-plan.md` 中阶段 1 必修问题数量（完整性度量关系）
    - _Requirements: 1.1, 1.2, Invariant(完整性), Metamorphic(度量关系)_

#### 现状治理 —— 鉴权与会话（R2）

- [x] 3. 统一 `BaseController` 登录态读取入口
  - 依赖：任务 2
  - 前置：诊断报告中已登记"BaseController 双路径读取 session"问题
  - 改造 `major_assignment/.../controller/BaseController.java`，新增 `getCurrentUser(HttpServletRequest)` / `getCurrentUserId(HttpServletRequest)` / `getCurrentRoles(HttpServletRequest)` 作为唯一入口
  - 将现有读取 `HttpSession` 的方法标记为 `@Deprecated`，并添加编译期告警注释
  - 全仓扫描所有 Controller，将 `session.getAttribute("currentUser")` 等调用替换为 `BaseController` 新入口
  - _Requirements: 2.1_

  - [x] 3.1 改造 `MultiRoleSessionFilter` 同步写入三处上下文
    - Filter 从 Redis 解析会话后，同时写入 `SecurityContextHolder`、`request.setAttribute("CURRENT_USER", ...)`、`HttpSession` 同一键名
    - 无效会话输入时保持匿名，不得写入 `Authentication` 对象
    - 在 `finally` 中 `MDC.clear()` 防止线程复用污染
    - _Requirements: 2.2, 2.3_

  - [ ]* 3.2 编写会话一致性属性测试
    - **Property 1：会话一致性不变量**（`BaseController.getCurrentUserId` / `SecurityContextHolder` / `request attribute` 三处同时为空或同指一个 userId）
    - 使用 `authUserArb` 生成器：随机 userId + roles 子集 + activeRole
    - _Requirements: 2.1, 2.2_
    - _Properties: P1 会话一致性不变量_

  - [ ]* 3.3 编写会话 Filter 幂等属性测试
    - **Property 2：会话 Filter 幂等**（多次 doFilter 结果一致）
    - _Requirements: 2.2_
    - _Properties: P2 会话 Filter 幂等_

  - [ ]* 3.4 编写无效会话不回退属性测试
    - **Property 3：无效会话不回退**（null / 畸形 JSON / 过期 / 错误类型 → 匿名）
    - 使用 `invalidSessionArb` 生成器
    - _Requirements: 2.3_
    - _Properties: P3 无效会话不回退_

  - [x] 3.5 `AuthenticationAspect` / `LoggingAspect` 改造为只读 MDC
    - 移除切面内部自行查 Session / Redis 的代码
    - 所有 `userId` / `role` 从 `MDC` 或 `SecurityContextHolder` 读取
    - _Requirements: 2.1, 2.2_

  - [x] 3.6 收敛 `SecurityConfig` CSRF 策略
    - 停止对 `/api/teacher/**`、`/api/student/**`、`/api/knowledge-points/**`、`/api/early-warnings/**`、`/api/notifications/**` 的全量 CSRF 忽略
    - 保留登录 / 验证码接口的必要白名单，其余统一启用 CSRF 或明确在注释中说明由网关/令牌负责
    - _Requirements: 2.4_

  - [x] 3.7 验证码 Redis key 前缀与业务会话隔离
    - `CAPTCHA:IMG:{sessionKey}` 与业务会话前缀 `SESSION:*` 互不交叉
    - 在 `CacheConstants` 中显式声明验证码命名空间
    - _Requirements: 2.5_

#### 现状治理 —— 统一异常、响应与日志（R3）

- [x] 4. 统一 `ResponseResult` 与 `GlobalExceptionHandler` 契约
  - 依赖：任务 3
  - 前置：所有 Controller 必须返回 `ResponseResult<T>`；禁止裸 `Map`/`String`/实体
  - _Requirements: 3.1, 3.2_

  - [x] 4.1 为 `ResponseResult` 增加 `traceId` 与 `timestamp` 字段
    - 不破坏旧字段（`success` / `code` / `message` / `data`）
    - 新增字段初始化：`traceId` 从 MDC 读取；`timestamp = System.currentTimeMillis()`
    - 静态工厂 `ResponseResult.ok(...) / error(...)` 同步更新
    - _Requirements: 3.1_

  - [x] 4.2 完善 `GlobalExceptionHandler` 覆盖异常映射表
    - 实现设计文档 §Error Handling §3 中完整异常—状态码映射（17 行）
    - `prod` Profile 下未知 `Exception` 隐藏堆栈与内部消息
    - 每个处理器内先写 MDC 再构造 `ResponseResult`
    - _Requirements: 3.2, 3.3_

  - [ ]* 4.3 编写响应契约属性测试
    - **Property 4：响应契约一致性**（`success == (200 ≤ code < 300)`、`message` 非空、Fallback code ∈ {429, 503, 504}）
    - 使用 `responseErrorSourceArb`：业务异常 / 校验异常 / 熔断 / 限流
    - _Requirements: 3.1, 3.2, 3.3_
    - _Properties: P4 响应契约一致性_

  - [x] 4.4 扫描并整改违规 Controller
    - 扫描所有 `@RestController`；对返回 `Map` / `String` / 裸实体的方法，统一改为 `ResponseResult<T>`
    - 建立 ArchUnit 规则：`@RestController` 方法返回类型必须是 `ResponseResult<?>` 或其派生
    - _Requirements: 3.1_

  - [ ]* 4.5 编写 ArchUnit 结构测试
    - 断言：所有 `@RestController` 方法返回 `ResponseResult<?>`
    - 断言：`GlobalExceptionHandler` 是唯一 `@ControllerAdvice`
    - _Requirements: 3.1, 3.2_

#### 现状治理 —— MDC 与 JSON 结构化日志（R3.4、R3.5）

- [x] 5. 建立 MDC 上下文与 JSON 结构化日志
  - 依赖：任务 4
  - _Requirements: 3.4, 3.5_

  - [x] 5.1 新增 `TraceIdFilter` 写入 MDC
    - 若请求头 `X-Trace-Id` 存在则透传，否则生成 `UUID.randomUUID()`
    - 同时写入 `traceId` / `userId` / `role` / `uri` / `method`
    - 注册在 Filter 链首位
    - _Requirements: 3.4_

  - [x] 5.2 配置 Logback JSON 输出
    - `logback-spring.xml` 使用 `net.logstash.logback.encoder.LogstashEncoder` 或 `logback-json`
    - 输出字段：`timestamp / level / logger / thread / traceId / userId / role / uri / status / elapsed_ms / message / stacktrace`
    - 控制台保留可读文本格式，文件输出 JSON
    - _Requirements: 3.5_

  - [x] 5.3 编写 `MaskingConverter` 敏感字段打码
    - 正则匹配 `password | pwd | apiKey | secret | token | credential` 字段名
    - 输出替换为 `***`
    - 在 `logback-spring.xml` 中注册为 `<conversionRule>`
    - _Requirements: 4.2_

  - [ ]* 5.4 编写 MDC 完整性属性测试
    - **Property 5：日志与 MDC 字段完整性**（同一请求在 `MultiRoleSessionFilter` / `LoggingAspect` / `GlobalExceptionHandler` 记录的 MDC 值相等；JSON 字段齐全）
    - 使用 `logEntryArb` 生成器
    - _Requirements: 3.4, 3.5_
    - _Properties: P5 日志与 MDC 字段完整性_

  - [ ]* 5.5 编写敏感字段打码属性测试
    - **Property 6：敏感字段打码不变量**（含敏感键的日志输出必定不包含原值且替换为 `***`）
    - 使用 `maskingInputArb` 生成器
    - _Requirements: 4.2_
    - _Properties: P6 敏感字段打码不变量_

#### 现状治理 —— 敏感配置外置（R4）

- [x] 6. 敏感配置外置与多 Profile 拆分
  - 依赖：任务 5
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

  - [x] 6.1 将敏感字段改为环境变量占位
    - 修改 `application.properties`：`spring.datasource.username=${DB_USERNAME:dev_user}` / `password=${DB_PASSWORD:dev_only_pwd}` / `spring.data.redis.password=${REDIS_PASSWORD:}`
    - 所有邮件 / AI API Key 同步改为 `${ENV_NAME}` 格式
    - _Requirements: 4.1_

  - [x] 6.2 拆分 `application-{dev,test,prod}.properties`
    - `dev` 允许默认回退值
    - `test` 全部走环境变量
    - `prod` 全部走环境变量且默认值为空
    - _Requirements: 4.3_

  - [x] 6.3 实现 Prod 快速失败启动校验
    - 新增 `@Component` `ProdConfigValidator implements ApplicationRunner`
    - 当 `spring.profiles.active` 包含 `prod` 时，检测关键敏感字段为空则 `System.exit(1)` 并输出明确原因
    - _Requirements: 4.4_

  - [ ]* 6.4 编写 Prod 快速失败示例测试
    - 使用 `@ActiveProfiles("prod")` + 故意不提供 `DB_PASSWORD` 环境变量
    - 断言 ApplicationContext 启动失败并包含预期错误文案
    - _Requirements: 4.4_

  - [x] 6.5 引入 gitleaks / detect-secrets 扫描
    - 新增 `.gitleaks.toml` 规则文件，覆盖密码 / API Key / 私钥正则
    - 在 `Makefile` 或 `scripts/check-secrets.sh` 提供本地扫描命令
    - _Requirements: 4 安全不变量_

#### 现状治理 —— 数据访问与缓存（R5）

- [x] 7. Mapper SQL 风格规范化
  - 依赖：任务 6
  - 产出 `docs/coding-guidelines.md` 中的"复杂 SQL 判定表格"
  - 将使用注解写复杂 JOIN 的 Mapper 迁移到 XML
  - _Requirements: 5.1_

  - [x] 7.1 编写 `docs/coding-guidelines.md` SQL 风格规范
    - 表格列出：多表 JOIN / 动态 where / 超过 10 行 / 存储过程 → XML；简单 CRUD → 注解
    - 附明确判定示例（引用现有 Mapper 文件）
    - _Requirements: 5.1, 18.1_

  - [x] 7.2 迁移违规 Mapper 到 XML
    - 扫描现有所有 `@Mapper` 接口，识别复杂 JOIN / 动态 SQL 情形
    - 将对应 SQL 迁移到 `src/main/resources/mapper/` 目录下新增/更新的 XML 文件
    - _Requirements: 5.1_

- [x] 8. CacheConstants 梳理与 RedisCacheService 分布式锁封装
  - 依赖：任务 7
  - _Requirements: 5.2, 5.3, 5.4_

  - [x] 8.1 重构 `CacheConstants` 显式声明 TTL
    - 按设计 §Data Models §3.1 缓存命名空间与 TTL 矩阵填充常量
    - 每项包含：cacheName / key 表达式 / TTL 注释
    - 验证码前缀 `CAPTCHA:IMG` 与业务会话隔离
    - _Requirements: 5.2, 2.5_

  - [x] 8.2 扫描 `@Cacheable` / `@CacheEvict` 违规项
    - 所有 `@Cacheable` 必须引用 `CacheConstants` 且显式指定 TTL（禁止默认）
    - 写方法使用 `@Caching` 显式枚举所有 `@CacheEvict`
    - _Requirements: 5.2, 5.3_

  - [ ]* 8.3 编写缓存配置唯一性测试
    - **Property 7：缓存配置唯一性**（`(cacheName, key)` 组合在全应用内唯一）
    - 扫描所有 `@Cacheable` 注解并断言
    - _Requirements: 5.2_
    - _Properties: P7 缓存配置唯一性_

  - [ ]* 8.4 编写缓存读-写-读一致性属性测试
    - **Property 8：缓存读-写-读一致性**（`f(x) → g(x, v) → f(x)` 返回新值）
    - 使用 `cacheOpSequenceArb`：随机读写序列
    - _Requirements: 5.3, Round-Trip_
    - _Properties: P8 缓存读-写-读一致性_

  - [x] 8.5 在 `RedisCacheService` 封装分布式锁
    - 新增接口 `DistributedLock { tryLock / unlock / withLock }`
    - 基于 `SET NX PX` + Lua 解锁脚本实现（防误删）
    - 锁 Key 固定格式 `LOCK:{domain}:{resourceId}`
    - _Requirements: 5.4_

  - [ ]* 8.6 编写分布式锁 Key 格式属性测试
    - **Property 9：分布式锁 Key 格式**（实际 Redis Key 匹配 `^LOCK:[^:]+:.+$`）
    - 使用 `lockKeyArb` 生成器（含特殊字符 / 空串 / 超长）
    - _Requirements: 5.4_
    - _Properties: P9 分布式锁 Key 格式_

- [-] 9. 分页上限保护
  - 依赖：任务 8
  - _Requirements: 5.5_

  - [x] 9.1 `PageRequestDTO.pageSize` 增加 `@Max(100)`
    - 同步增加 `@Min(1)`，并在 `BaseController` 提供 `clampPageSize(int)` 兜底方法
    - 违规分页请求走 `GlobalExceptionHandler` 走 400 响应
    - _Requirements: 5.5_

  - [ ]* 9.2 编写 clampPageSize 属性测试
    - **Property 10：分页上限 clamp**（任意整数输入 → `[1, 100]`；`[1,100]` 内保持不变）
    - 使用 `pageSizeArb`：覆盖 `Integer.MIN_VALUE / 0 / 50 / Integer.MAX_VALUE`
    - _Requirements: 5.5_
    - _Properties: P10 分页上限 clamp_

#### 阶段 1 检查点

- [x] 10. 阶段 1 检查点 - 确保所有测试通过
  - 依赖：任务 2~9
  - 运行 `mvn clean verify`
  - 确认 P1~P10 属性测试全绿；ArchUnit 规则通过；Secrets 扫描无命中
  - 确保所有测试通过，如有问题询问用户
  - _Requirements: 1, 2, 3, 4, 5_

---

### 阶段 2：平台基础设施搭建 + 首服务剥离

> 前置依赖：阶段 1 全部验收通过。
> 退出条件：Nacos / Gateway / 可观测性栈可用；Auth_Service / User_Service 剥离完成并 100% 流量走新服务；网关双路由机制上线；P11~P24、P28、P29、P30 相关属性测试通过。

#### 多模块 Maven 骨架

- [ ] 11. 建立父 POM 与多模块骨架
  - 依赖：阶段 1 检查点
  - 在仓库根目录建立 `Target_Platform/`（或直接使用仓库根）作为多模块聚合工程
  - 目录结构：`parent-pom / gateway / auth-service / user-service / common / {service}-api / legacy-adapter`
  - _Requirements: 6.1, 6.2, 7.1_

  - [ ] 11.1 创建 `parent-pom` 锁定 BOM 版本
    - 声明 `java.version=17` / `spring-boot.version=3.5.3` / `spring-cloud.version=2023.0.x` / `spring-cloud-alibaba.version=2023.0.x`
    - `<dependencyManagement>` 引入 spring-cloud-dependencies / spring-cloud-alibaba-dependencies / micrometer-tracing / resilience4j BOM
    - 子模块禁止覆盖版本（通过 Enforcer 插件规则约束）
    - _Requirements: 7.1, 7.9_

  - [ ] 11.2 创建多模块骨架与模块间依赖
    - 每个 `{service}-api` 模块仅含 Feign 接口与 DTO；纯 Java jar
    - 每个业务服务模块依赖其它服务的 `-api`，不依赖实现模块（Enforcer 禁止传递）
    - `common` 模块包含：ResponseResult / 异常体系 / MDC Filter / Feign 配置 / 指标 tagging
    - _Requirements: 6.2, 10.1_

  - [ ]* 11.3 编写依赖版本一致性属性测试
    - **Property 13：依赖版本一致**（所有子模块 `spring-boot.version / spring-cloud.version` 与 `docs/architecture.md` 声明一致）
    - 用 Maven Enforcer 规则 + ArchUnit / 自定义扫描实现
    - _Requirements: 7.1, 7 Invariant_
    - _Properties: P13 依赖版本一致_

  - [ ] 11.4 在 `docs/architecture.md` 中填充选型决策表与版本说明
    - 对 R7.2~R7.9 每个选型写明 "候选 / 选定 / 理由 / 备选切换条件"
    - 记录 Spring Cloud 2023.0.x 与 Spring Boot 3.5.3 匹配说明
    - _Requirements: 7.3, 7.4, 7.9, 18.1, 18.4_

#### Nacos Server 与接入

- [-] 12. Nacos Server 搭建与接入
  - 依赖：任务 11
  - _Requirements: 7.3, 7.4, 4.5_

  - [ ] 12.1 编写本地 Nacos 启动 `docker-compose.dev.yml`
    - 使用 `nacos/nacos-server:v2.3.0` 单机模式
    - 暴露 8848 端口；持久化到 `./data/nacos`
    - _Requirements: 7.3, 15.2_

  - [ ] 12.2 每个业务服务引入 `spring-cloud-starter-alibaba-nacos-discovery` + `-config`
    - `bootstrap.yml` 仅含 Nacos 地址与 namespace / group
    - `application.yml` 主体由 Nacos 下发
    - `spring.application.name` 与 Registry 注册名、Gateway 路由 ID 三者一致
    - _Requirements: 6.3, 4.5, 7.3, 7.4_

  - [ ]* 12.3 编写服务名三元一致属性测试
    - **Property 11：服务名三元一致**（`application.yml` / Nacos 注册记录 / Gateway 路由 `lb://` 一致）
    - 启动 Testcontainers Nacos，断言注册名匹配配置
    - _Requirements: 6.3_
    - _Properties: P11 服务名三元一致_

  - [ ]* 12.4 编写环境 Profile ↔ Namespace 映射属性测试
    - **Property 29：环境 Profile ↔ 配置命名空间映射**（Profile=env 时只从 env 命名空间读取）
    - 使用 Testcontainers + 注入不同 profile 切换断言
    - _Requirements: 15.5_
    - _Properties: P29 环境 Profile ↔ 配置命名空间映射_

#### common 公共库

- [ ] 13. 建立 `common` 公共库
  - 依赖：任务 11
  - _Requirements: 3.1, 3.2, 3.4, 8.3, 10.1, 10.2, 10.3_

  - [ ] 13.1 迁移 `ResponseResult` / 异常体系到 `common`
    - 拷贝并重构 `ResponseResult<T>` / `BusinessException` / `UnauthorizedException` / `ForbiddenException` / `ResourceNotFoundException` / `RemoteClientException` / `RemoteServerException` / `RateLimitExceededException`
    - 提供 `@ControllerAdvice` 基类 `BaseGlobalExceptionHandler`，各服务继承
    - _Requirements: 3.1, 3.2, 10.3_

  - [ ] 13.2 实现 `CommonMdcFilter`（Servlet / Reactive 双版本）
    - Servlet 版本用于业务服务；Reactive 版本用于 Gateway
    - 写入 `traceId / userId / roles / activeRole / uri / method`
    - _Requirements: 3.4, 8.5_

  - [ ] 13.3 实现 `FeignRequestInterceptor`
    - 从 MDC 读取 `X-Trace-Id / X-User-Id / X-Roles / X-Active-Role` 并写入 Feign 请求头
    - 作为 `@Bean` 自动装配（`spring.factories` / `AutoConfiguration.imports`）
    - _Requirements: 10.2_

  - [ ] 13.4 实现 `GlobalFeignErrorDecoder`
    - 4xx → `RemoteClientException(code, msg, bodyJson)`
    - 5xx → `RemoteServerException(code, msg, bodyJson)`
    - 携带原始 HTTP 状态码与下游 `ResponseResult.message`
    - _Requirements: 10.3_

  - [ ]* 13.5 编写 ErrorDecoder 分类属性测试
    - **Property 25：ErrorDecoder 错误分类**（`[400, 499] → RemoteClientException`；`[500, 599] → RemoteServerException`；`exception.code == httpStatus`）
    - 使用 `httpStatusArb` 生成器
    - _Requirements: 10.3, 10 Metamorphic_
    - _Properties: P25 ErrorDecoder 错误分类_

  - [ ] 13.6 共享指标 / 链路自动配置
    - 引入 `micrometer-tracing-bridge-otel` + `io.opentelemetry.exporter.zipkin`（或 otlp）
    - 引入 `micrometer-registry-prometheus`
    - 提供 `application-common.yml` 片段：Actuator 端点暴露 + tracing sample rate + prometheus 端点
    - _Requirements: 13.1, 13.2, 13.3_

#### Spring Cloud Gateway

- [ ] 14. 构建 Spring Cloud Gateway
  - 依赖：任务 12, 13
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 14.3_

  - [ ] 14.1 Gateway 基础骨架与 Nacos 注册
    - 新模块 `gateway`，引入 `spring-cloud-starter-gateway` + Nacos discovery/config
    - 端口 8080；注册名 `gateway`
    - _Requirements: 8.1, 7.2_

  - [ ] 14.2 编写 `TraceIdFilter`（Reactive）
    - 若 `X-Trace-Id` 存在透传，否则生成 UUID
    - 写入 `exchange.getAttributes()` 并透传到下游请求头
    - 同时写入 Reactor Context 供日志使用
    - _Requirements: 8.5, 13.2_

  - [ ] 14.3 编写 `JwtAuthenticationFilter`（Reactive）
    - 从 Config_Center 拉取 `kid -> publicKey` 映射
    - 解析 `Authorization: Bearer`；校验签名、过期、`jti` 黑名单（Redis）
    - 失败返回 `ResponseResult(401, ...)` JSON
    - 白名单路由跳过：`/api/auth/login / /api/auth/refresh / /api/auth/captcha`
    - _Requirements: 8.3, 8.4, 9.4_

  - [ ] 14.4 编写 `HeaderEnrichFilter`（Reactive）
    - 剥离客户端自带的 `X-User-Id / X-Roles / X-Active-Role`
    - 用 JWT 解析值重写
    - _Requirements: 8.3, 17.4_

  - [ ] 14.5 配置 CORS 由 Nacos 热下发
    - `gateway-cors.yml` 中的 `allowedOrigins` 通过 `@RefreshScope` 支持热更新
    - 移除 `MajorAssignmentApplication.corsConfigurer` 中硬编码（放入 `legacy-adapter` 过渡期引用）
    - _Requirements: 8.6, 14.3_

  - [ ] 14.6 配置按 `(ip, userId, routeId)` 分桶的限流
    - 使用 `RequestRateLimiter` + `RedisRateLimiter` + 自定义 `KeyResolver`
    - 登录接口限流规则：同一 IP 60 秒内 30 次失败触发
    - 限流触发返回 `ResponseResult(429, retryAfter)`
    - _Requirements: 8.7, 17.4_

  - [ ] 14.7 配置熔断 Fallback
    - 使用 `Spring Cloud CircuitBreaker` + Resilience4j Reactive
    - 每条路由挂 `fallbackUri` 指向内建 `/_fallback/{service}` 端点
    - Fallback 返回 `ResponseResult(503, "{服务} 暂时不可用")`
    - Registry 查不到实例时走 `ResponseResult(504)`
    - _Requirements: 11.3, 17.3_

  - [ ] 14.8 Nacos 下发路由配置
    - 初始路由：`auth-route / auth-secured / legacy-route`
    - `legacy-route` 指向 `http://legacy-monolith:8080`（过渡期）
    - _Requirements: 8.2, 16.2_

  - [ ]* 14.9 编写网关与 Feign 链路透传属性测试
    - **Property 14：网关与 Feign 链路透传**（JWT claims 与下游请求头一致；MDC 与 Feign 出站请求头一致）
    - 使用 `validClaims` 生成器 + MockServer 下游
    - _Requirements: 8.3, 10.2_
    - _Properties: P14 网关与 Feign 链路透传_

  - [ ]* 14.10 编写无效 JWT 响应属性测试
    - **Property 15：无效 JWT 返回 401 + ResponseResult**（缺失 / 畸形 / 过期 / 签名错 / 黑名单）
    - 使用 `invalidClaims` 生成器
    - _Requirements: 8.4, 9.4_
    - _Properties: P15 无效 JWT 返回 401 + ResponseResult_

  - [ ]* 14.11 编写 TraceId 全链路一致属性测试
    - **Property 16：TraceId 全链路一致**（Gateway 写入值 = 下游 MDC = Span 树根一致）
    - 使用 Testcontainers Tempo / Zipkin
    - _Requirements: 8.5, 13.2, 13 链路完整性_
    - _Properties: P16 TraceId 全链路一致_

  - [ ]* 14.12 编写限流按键分桶属性测试
    - **Property 17：网关限流按键分桶**（`(ip, userId, routeId)` 三元组分桶）
    - 构造并发请求序列，断言计数器隔离
    - _Requirements: 8.7, 17.4_
    - _Properties: P17 网关限流按键分桶_

  - [ ]* 14.13 编写 CORS 白名单属性测试
    - **Property 18：CORS 白名单行为与配置一致**（`o ∈ L` 放行；`o ∉ L` 拒绝；热更新 TTL 内生效）
    - 使用 `originArb` 生成器
    - _Requirements: 8.6, 14.3_
    - _Properties: P18 CORS 白名单行为与配置一致_

  - [ ]* 14.14 编写网关降级响应格式属性测试
    - **Property 4（复用）**：Fallback 响应体必须是合法 `ResponseResult`、`code ∈ {429, 503, 504}`
    - _Requirements: 11.3, 17.3, R11 Invariant_
    - _Properties: P4 响应契约一致性_


#### Auth_Service 剥离

- [ ] 15. 剥离 Auth_Service
  - 依赖：任务 13, 14
  - 目标：`/api/auth/login|refresh|logout|me|switch-role|captcha` 完整迁出
  - 前置：`docs/auth-design.md` 完成 JWT Claims / 密钥轮换 / 多角色策略撰写
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 17.5, 17.6_

  - [ ] 15.1 编写 `docs/auth-design.md`
    - Claims 结构（`sub / roles / activeRole / iat / exp / jti / tokenType`）
    - Token 生命周期状态机
    - 多角色策略（单 Token + activeRole + `/switch-role`）
    - 密钥轮换（RSA `kid` 双密钥并存 48h）
    - 单体 / JWT 凭证互斥策略
    - _Requirements: 9.7, 18.1_

  - [ ] 15.2 创建 `auth-service` 模块骨架
    - `pom.xml` 依赖 `common` / `user-service-api` / Nacos discovery+config / Spring Security / JJWT
    - `bootstrap.yml` 配置 Nacos；`application.yml` 主体由 Nacos 下发
    - 端口 8081
    - _Requirements: 6.2, 9.1_

  - [ ] 15.3 创建 `auth-service-api` 模块
    - Feign 接口：`AuthInternalFeignClient`（`/internal/auth/introspect` 供其它服务反查 token 状态）
    - DTO：`TokenIntrospectionDTO`
    - _Requirements: 10.1_

  - [ ] 15.4 创建独立数据源 `sc_auth` schema
    - 新增 `auth_credentials` 表（从原 `users` 表剥离口令字段）
    - `token_blacklist` 不建表，直接走 Redis `JTI_BLACKLIST:{jti}` 与 `REFRESH:{rt}`
    - 提供 Flyway 迁移脚本 `V1__init_auth_schema.sql`
    - _Requirements: 12.1, 12.2_

  - [ ] 15.5 实现 `POST /api/auth/login`
    - 校验验证码（Redis `CAPTCHA:IMG:{sessionKey}`）
    - 校验口令（BCrypt）
    - 通过 `UserFeignClient` 查询 roles（不直连 `users` 表）
    - 签发 Access_Token（RSA 私钥）+ Refresh_Token（UUID 入 Redis）
    - _Requirements: 9.1, 9.2, 9.3, 17.6_

  - [ ] 15.6 实现 `POST /api/auth/refresh`
    - 校验 Refresh_Token（Redis 查找 + 未被吊销）
    - 签发新 Access_Token；保留 Refresh_Token 或滚动（策略写入文档）
    - _Requirements: 9.1, 9.2, 9.4_

  - [ ] 15.7 实现 `POST /api/auth/logout`（幂等）
    - 解析当前 Access_Token 的 `jti`，写入 `JTI_BLACKLIST:{jti}` TTL = 剩余有效期
    - 删除对应 Refresh_Token
    - 多次调用仍返回成功
    - _Requirements: 9.4, 9 Idempotence_

  - [ ] 15.8 实现 `POST /api/auth/switch-role`
    - 校验 `targetRole ∈ roles`
    - 吊销旧 JWT（写入黑名单）并签发新 JWT，`activeRole = targetRole`
    - _Requirements: 9.7_

  - [ ] 15.9 实现 `GET /api/auth/me`
    - 从 JWT 解析 `sub / activeRole / roles`
    - 返回 `ResponseResult(200, AuthUserDTO)`
    - _Requirements: 9.1_

  - [ ] 15.10 实现密钥热轮换
    - 支持 `kid -> { publicKey, privateKey }` 双密钥配置
    - Nacos 热更新后 48h 窗口内新旧密钥都可验签
    - 新签发固定用新 `kid`
    - _Requirements: 9.5_

  - [ ] 15.11 实现单体 / JWT 凭证互斥
    - 用户 JWT 登录成功时通过 `legacy-adapter` 调用单体清理旧 `JSESSIONID_*`（或直接清除 Redis `LEGACY_SESSION:{sid}`）
    - 单体登录成功时回调 Auth_Service `/internal/auth/revoke-by-user` 将该用户所有现有 JWT 加黑名单
    - _Requirements: 9.6_

  - [ ] 15.12 实现密码 BCrypt 与敏感字段脱敏
    - 新建 / 修改密码接口使用 `BCryptPasswordEncoder`
    - `AuthUserDTO` 中手机号 / 邮箱按规则脱敏
    - _Requirements: 17.6_

  - [ ]* 15.13 编写 JWT 签发-解码 Round-Trip 属性测试
    - **Property 19：JWT 签发-解码 Round-Trip**（`verify(sign(c)) == true` 且 claims 等价）
    - 使用 `validClaims` 生成器；至少 1000 次迭代
    - _Requirements: 9.3, R9 Round-Trip_
    - _Properties: P19 JWT 签发-解码 Round-Trip_

  - [ ]* 15.14 编写 Token 黑名单生命周期属性测试
    - **Property 20：Token 黑名单生命周期**（黑名单命中 → 失败；TTL ≤ 剩余有效期；jti 不复用）
    - 使用 Testcontainers Redis
    - _Requirements: 9.4, R9 黑名单不变量_
    - _Properties: P20 Token 黑名单生命周期_

  - [ ]* 15.15 编写登出幂等属性测试
    - **Property 21：登出幂等**（N 次调用同结果；Redis 仅写入一次；不抛异常）
    - _Requirements: 9 Idempotence_
    - _Properties: P21 登出幂等_

  - [ ]* 15.16 编写密钥热轮换属性测试
    - **Property 22：密钥热轮换双密钥并存**（轮换窗口 48h 内 K1/K2 都可验签；窗口外 K1 失败）
    - _Requirements: 9.5_
    - _Properties: P22 密钥热轮换双密钥并存_

  - [ ]* 15.17 编写多角色切换一致性属性测试
    - **Property 23：多角色切换一致性**（`target ∈ roles` 则成功；`t_new.activeRole == target`；旧 `jti` 入黑名单）
    - _Requirements: 9.7_
    - _Properties: P23 多角色切换一致性_

  - [ ]* 15.18 编写单体 / JWT 凭证互斥属性测试
    - **Property 24：单体 / JWT 凭证互斥**（交错登录 / 登出序列下任意中间状态只存在一套有效凭证）
    - 使用 `concurrentLoginOpsArb` 生成器
    - _Requirements: 9.6_
    - _Properties: P24 单体 / JWT 凭证互斥_

  - [ ]* 15.19 编写密码 BCrypt 属性测试
    - **Property 31：密码 BCrypt 与敏感字段脱敏**（`password` 字段匹配 `^\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}$`；手机号 / 邮箱脱敏）
    - _Requirements: 17.6, R17 加密不变量_
    - _Properties: P31 密码 BCrypt 与敏感字段脱敏_

  - [ ] 15.20 Gateway 路由切换 Auth 到新服务
    - Nacos 将 `auth-route` 与 `auth-secured` 的 `uri` 改为 `lb://auth-service`
    - `legacy-route` 继续保留其它前缀
    - 单体 `AuthController` 标注 `@Deprecated`（保留 1 个阶段便于回滚）
    - _Requirements: 8.2, 16.2_

#### User_Service 剥离

- [ ] 16. 剥离 User_Service
  - 依赖：任务 15
  - _Requirements: 6.1, 6.2, 6.3, 12.1, 12.2_

  - [ ] 16.1 创建 `user-service` 模块 + `user-service-api` 模块
    - 端口 8082；注册名 `user-service`
    - `-api` 暴露：`UserFeignClient { getProfile / getRoles / listByIds }` + `UserProfileDTO` / `UserRolesDTO`
    - _Requirements: 6.2, 10.1_

  - [ ] 16.2 迁移 `users / user_roles` 表到 `sc_user` schema
    - Flyway 脚本 `V1__init_user_schema.sql`
    - Mapper 从单体迁入 `user-service`
    - 单体模块删除相同实体 + Mapper（保留 legacy-adapter 读取通道）
    - _Requirements: 6.4, 12.1, 12.2_

  - [ ] 16.3 迁移 `UserController` / `StudentController` / `TeacherDashboardController` 相关接口
    - 全部重写为调用 User_Service 业务方法
    - 返回 `ResponseResult<T>`
    - _Requirements: 6.2, 6.3, 3.1_

  - [ ] 16.4 实现 `/internal/users/**` 内部接口
    - 仅接受来自 Registry 内部的调用
    - 前缀 `/internal/**` 不走网关；在 Nacos 路由中明确不对外暴露
    - _Requirements: 17.5_

  - [ ] 16.5 Gateway 切换 `user-route` 到 `lb://user-service`
    - Nacos 路由表更新
    - _Requirements: 8.2, 16.2_

  - [ ]* 16.6 编写数据表写权限唯一性属性测试
    - **Property 12：数据表写权限唯一**（`users / user_roles` 的 I/U/D 只出现在 `user-service`）
    - 用 ArchUnit + SQL AST 扫描源码实现
    - _Requirements: 6 Invariant, 12.2_
    - _Properties: P12 数据表写权限唯一_

  - [ ]* 16.7 编写 User_Service 契约测试
    - Spring Cloud Contract 生产者契约；Auth_Service 作为消费者验证
    - _Requirements: 10.1_

#### 前端静态资源剥离

- [ ] 17. 前端静态资源剥离到 Nginx
  - 依赖：任务 14
  - _Requirements: 14.1, 14.2, 14.4, R14 部署解耦_

  - [ ] 17.1 新建 `frontend/` 独立目录（或独立仓库链接）
    - 迁出 `major_assignment/src/main/resources/static/**` 全部资源
    - 添加 `frontend/README.md` 说明构建产物路径
    - _Requirements: 14.1_

  - [ ] 17.2 新增 `deploy/nginx/nginx.conf`
    - 静态资源根目录指向 `frontend/dist`
    - `/api/**` 反向代理到 Gateway
    - 开启 gzip / cache-control
    - _Requirements: 14.2, 14.3_

  - [ ] 17.3 更新 Spring Boot 单体启动配置
    - 禁用默认静态资源映射（`spring.web.resources.add-mappings=false`）
    - 移除 `MajorAssignmentApplication.corsConfigurer` 的硬编码 origin
    - _Requirements: 14.1, 14.3_

  - [ ]* 17.4 编写部署产物无前端资源属性测试
    - **Property 28：部署产物无前端资源**（业务微服务 jar 解压不含 `.html/.js/.css/.vue/.tsx`）
    - 构建后扫描 `target/*.jar` 内容
    - _Requirements: 14 部署解耦_
    - _Properties: P28 部署产物无前端资源_

#### 可观测性栈搭建

- [ ] 18. 可观测性栈（Prometheus / Grafana / Loki / Tempo）
  - 依赖：任务 12
  - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5, 13.6_

  - [ ] 18.1 编写 `docs/observability.md`
    - 指标清单（JVM / HTTP / DataSource / Redis / Feign / CircuitBreaker）
    - 日志字段清单 & MDC 规范
    - 链路追踪采样策略（dev 100% / prod 10%）
    - 告警规则清单（5xx 比例 / 熔断打开 / 登录失败率）
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5, 18.1_

  - [ ] 18.2 `docker-compose.obs.yml`：Prometheus + Grafana + Loki + Promtail + Tempo
    - Prometheus scrape `/actuator/prometheus`
    - Loki 通过 Promtail 收集容器日志
    - Tempo 接收 OTLP
    - Grafana 预置 Data Source + 示例 Dashboard JSON
    - _Requirements: 13.3, 13.4, 13.6, 15.2_

  - [ ] 18.3 定义 Prometheus 告警规则
    - `instance_5xx_rate > 5%` 持续 1 分钟 → 触发
    - `circuit_breaker_state == "OPEN"` 持续 30 秒 → 触发
    - 告警规则 YAML 放 `deploy/prometheus/alerts/`
    - _Requirements: 11.4, 13.5_

  - [ ] 18.4 限制 Actuator 端点访问来源
    - `management.server.address` 绑定内网；或通过 Security `requestMatcher` 白名单
    - _Requirements: 13.1, 17.5_

#### 阶段 2 检查点与过渡期验证

- [ ] 19. 阶段 2 检查点 - 确保所有测试通过
  - 依赖：任务 11~18
  - 运行 `mvn clean verify`（多模块聚合）
  - 运行 Testcontainers 集成测试（Nacos / Redis / MySQL / RabbitMQ）
  - 确认 P11~P24、P28、P29 全绿；`legacy-route` 对未剥离接口响应与直连单体等价
  - 确保所有测试通过，如有问题询问用户
  - _Requirements: 6, 7, 8, 9, 10, 13, 14, 15, 16_

  - [ ]* 19.1 编写阶段 2 接口兼容性属性测试
    - **Property 30：阶段 2 接口兼容性**（未剥离接口经网关响应与直连单体响应等价；接口版本并存）
    - 对同一请求向 Gateway 与 legacy-monolith 分别发起，diff 响应结构
    - _Requirements: 16.2, 16.5, R16 接口兼容性_
    - _Properties: P30 阶段 2 接口兼容性_

---

### 阶段 3：全量服务剥离与旧单体下线

> 前置依赖：阶段 2 检查点通过。
> 退出条件：Course / Assignment / Exam / Analysis / Notification / AI 全部剥离；legacy-route 从 Nacos 删除；P25~P28 属性测试全绿；旧单体下线。

#### 数据所有权矩阵与事件驱动基础

- [ ] 20. 编写 `docs/data-ownership.md` 与事件驱动基础设施
  - 依赖：阶段 2 检查点
  - _Requirements: 6, 10.5, 12.1, 12.2, 12.5_

  - [ ] 20.1 `docs/data-ownership.md` 服务 ↔ 表所有权矩阵
    - 表格列：表名 / 归属服务 / Schema / 备注 / 只读共享范围
    - 跨服务访问铁律声明（禁止 JDBC 跨 Schema）
    - _Requirements: 12.1, 6 Invariant, 18.1_

  - [ ] 20.2 在 `common` 提供 `outbox_event` / `processed_event` 通用建表脚本与 Relay Job
    - `OutboxEventEntity` + `OutboxRelayJob`（定时轮询 status=0）
    - `ProcessedEventEntity` + `IdempotentEventHandler`
    - 采用 Spring Cloud Stream（RabbitMQ binder）
    - _Requirements: 7.8, 10.5, 12.5_

  - [ ] 20.3 定义核心领域事件 DTO
    - `AssignmentSubmittedEvent / ExamFinishedEvent / EarlyWarningRaisedEvent / EarlyWarningRollbackEvent / NotificationPushedEvent`
    - 统一字段：`eventId / occurredAt / aggregate / payload`
    - 放 `common-events` 子模块
    - _Requirements: 10.5, 6.5_

  - [ ]* 20.4 编写事件消费幂等属性测试
    - **Property 26：事件消费幂等**（N ≥ 1 次消费副作用等价；`processed_event` 仅写一次）
    - 使用 `domainEventArb` + Testcontainers RabbitMQ
    - 至少 1000 次迭代
    - _Requirements: 10.5, R10 Idempotence_
    - _Properties: P26 事件消费幂等_

#### Course_Service 剥离

- [ ] 21. 剥离 Course_Service
  - 依赖：任务 20；网关双路由模板复用任务 14.8
  - _Requirements: 6, 10, 12_

  - [ ] 21.1 `course-service` + `course-service-api` 模块骨架
    - 端口 8083；注册名 `course-service`
    - Feign：`CourseFeignClient { getCourse / listByTeacher / listByStudent }`
    - _Requirements: 6.2, 10.1_

  - [ ] 21.2 迁移 `courses / classes / course_enrollment` 到 `sc_course`
    - Flyway `V1__init_course_schema.sql`
    - 单体同步删除对应实体与 Mapper
    - _Requirements: 6.4, 12.1_

  - [ ] 21.3 迁移 `CourseController` 业务接口
    - 所有依赖 User_Service 的数据走 Feign
    - 缓存命名空间 `COURSE:LIST / COURSE:DETAIL`
    - _Requirements: 6.1, 6.3, 5.2_

  - [ ] 21.4 Feign 熔断与 Fallback 声明
    - `UserFeignClient` 配置 `fallbackFactory`
    - 所有 Fallback 返回 `ResponseResult(503, ...)`
    - _Requirements: 10.4, 11.3_

  - [ ]* 21.5 编写 Course_Service 单元测试
    - 覆盖业务方法的边界条件
    - _Requirements: 6.1_

  - [ ]* 21.6 编写 Course_Service 契约测试
    - Spring Cloud Contract 生产者契约
    - _Requirements: 10.1_

  - [ ] 21.7 Gateway 切换 `course-route` 到 `lb://course-service`
    - Nacos 路由更新；保留回滚配置快照
    - _Requirements: 8.2, 16.2_

#### Assignment_Service 剥离

- [ ] 22. 剥离 Assignment_Service
  - 依赖：任务 21
  - _Requirements: 6, 10, 12_

  - [ ] 22.1 `assignment-service` + `-api` 模块骨架
    - 端口 8084；注册名 `assignment-service`
    - Feign：`AssignmentFeignClient { getAssignment / listByCourse / submit }`（内部 /internal 仅限统计查询）
    - _Requirements: 6.2, 10.1_

  - [ ] 22.2 迁移 `assignments / assignment_submissions` 到 `sc_assignment`
    - _Requirements: 6.4, 12.1_

  - [ ] 22.3 迁移 `AssignmentController` 业务接口
    - 依赖 User / Course 走 Feign
    - _Requirements: 6.1, 6.3_

  - [ ] 22.4 实现 `AssignmentSubmittedEvent` 发布（outbox 模式）
    - 业务事务内写 `outbox_event`
    - Relay Job 投递 RabbitMQ `assignment.submitted`
    - _Requirements: 10.5, 12.5_

  - [ ] 22.5 Feign 熔断 Fallback 声明
    - 对 User_Service / Course_Service 调用
    - _Requirements: 10.4, 11.3_

  - [ ]* 22.6 编写 Assignment_Service 单元测试
    - _Requirements: 6.1_

  - [ ]* 22.7 编写 Assignment_Service 契约测试
    - _Requirements: 10.1_

  - [ ] 22.8 Gateway 切换 `assignment-route`
    - _Requirements: 8.2, 16.2_

#### Exam_Service 剥离

- [ ] 23. 剥离 Exam_Service
  - 依赖：任务 22
  - 核心接口清单（考试提交）需要更严格熔断阈值与更宽松限流
  - _Requirements: 6, 10, 11.5, 12, 17.1_

  - [ ] 23.1 `exam-service` + `-api` 模块骨架
    - 端口 8085；注册名 `exam-service`
    - _Requirements: 6.2_

  - [ ] 23.2 迁移 `exams / exam_questions / exam_submissions` 到 `sc_exam`
    - _Requirements: 6.4, 12.1_

  - [ ] 23.3 迁移 `ExamController` 与自动阅卷业务
    - 自动阅卷同步执行（保证 P95 ≤ 800ms）
    - 统计推送到 Analysis 走 `ExamFinishedEvent`（异步）
    - _Requirements: 6.6, 10.5, 17.1_

  - [ ] 23.4 实现 `ExamFinishedEvent` outbox 发布
    - _Requirements: 10.5, 12.5_

  - [ ] 23.5 在 `docs/architecture.md` 维护核心接口清单与熔断 / 限流阈值差异
    - 标注 `/api/exams/{id}/submit` 为核心接口，阈值配置写入表格
    - _Requirements: 11.5, 18.1_

  - [ ]* 23.6 编写熔断器状态转移属性测试
    - **Property 27：熔断器状态转移**（CLOSED → OPEN → HALF_OPEN → CLOSED/OPEN 状态机）
    - 使用 `failureRateSeriesArb` 生成随机失败率序列
    - _Requirements: 10.4, 11.2_
    - _Properties: P27 熔断器状态转移_

  - [ ] 23.7 Gateway 切换 `exam-route`
    - _Requirements: 8.2, 16.2_

#### Analysis_Service 剥离（含 KnowledgePoint）

- [ ] 24. 剥离 Analysis_Service（含 KnowledgePoint）
  - 依赖：任务 23
  - 仅读 + 事件驱动写入，不对外提供在线业务写操作
  - _Requirements: 6, 6.6, 10.5, 12_

  - [ ] 24.1 `analysis-service` + `-api` 模块骨架
    - 端口 8086；注册名 `analysis-service`
    - 对外前缀：`/api/analysis/** / /api/knowledge-points/** / /api/early-warnings/** / /api/dashboard/**`
    - _Requirements: 6.1, 6.2_

  - [ ] 24.2 迁移 `knowledge_points / kp_mastery / score_trends / early_warnings` 到 `sc_analysis`
    - _Requirements: 6.4, 12.1_

  - [ ] 24.3 迁移 `AnalysisController / KnowledgePointController / KnowledgePointAnalysisController / EarlyWarningController / DashboardController / TeacherDashboardController`
    - 聚合数据通过 Feign 调 User / Course / Assignment / Exam
    - _Requirements: 6.1, 6.3_

  - [ ] 24.4 消费 `AssignmentSubmittedEvent / ExamFinishedEvent` 更新 KP 掌握度与成绩趋势
    - 使用 `IdempotentEventHandler` 保证幂等
    - _Requirements: 10.5, 6.6_

  - [ ] 24.5 生成 `EarlyWarningRaisedEvent` 触发通知
    - 通过 outbox 发布
    - _Requirements: 10.5_

  - [ ] 24.6 历史数据回填脚本
    - 一次性 ETL 脚本从 Exam / Assignment 的历史表同步到 Analysis Schema
    - 放 `deploy/scripts/backfill/`
    - _Requirements: 12.3_

  - [ ]* 24.7 编写 Analysis_Service 集成测试
    - 事件驱动链路：触发 `ExamFinishedEvent` → 断言 KP 掌握度更新 + 预警生成
    - _Requirements: 6.6, 10.5_

  - [ ] 24.8 Gateway 切换 `analysis-route`
    - _Requirements: 8.2, 16.2_

#### Notification_Service 剥离

- [ ] 25. 剥离 Notification_Service
  - 依赖：任务 24
  - _Requirements: 6, 10.5, 12_

  - [ ] 25.1 `notification-service` + `-api` 模块骨架
    - 端口 8087；注册名 `notification-service`
    - _Requirements: 6.2_

  - [ ] 25.2 迁移 `notifications` 到 `sc_notification`
    - _Requirements: 6.4, 12.1_

  - [ ] 25.3 迁移 `NotificationController` 与业务
    - 读走 DB；写走事件消费
    - _Requirements: 6.1, 6.3_

  - [ ] 25.4 消费 `AssignmentSubmittedEvent / ExamFinishedEvent / EarlyWarningRaisedEvent`
    - 生成对应通知（幂等）
    - _Requirements: 10.5_

  - [ ]* 25.5 编写 Notification_Service 契约测试
    - _Requirements: 10.1_

  - [ ] 25.6 Gateway 切换 `notification-route`
    - _Requirements: 8.2, 16.2_

#### AI_Service 剥离

- [ ] 26. 剥离 AI_Service
  - 依赖：任务 25
  - _Requirements: 6, 10, 12_

  - [ ] 26.1 `ai-service` + `-api` 模块骨架
    - 端口 8088；注册名 `ai-service`
    - _Requirements: 6.2_

  - [ ] 26.2 新建 `sc_ai` Schema
    - 表：`ai_generations / ai_prompts`（历史调用记录）
    - Flyway `V1__init_ai_schema.sql`
    - _Requirements: 12.1_

  - [ ] 26.3 迁移 `AIController` 业务接口
    - 替换原占位实现；接入真实 AI 模型代理
    - 不写业务库（结果由业务服务通过 Feign / 事件消费）
    - _Requirements: 6.1, 1.3_

  - [ ] 26.4 对 AI 调用配置按用户 QPS 限流
    - 限流键：`userId`；阈值 Nacos 可配
    - _Requirements: 8.7_

  - [ ]* 26.5 编写 AI_Service 集成测试（Mock 模型）
    - _Requirements: 6.1_

  - [ ] 26.6 Gateway 切换 `ai-route`
    - _Requirements: 8.2, 16.2_

#### Migration Plan 文档与旧单体下线

- [ ] 27. 补全 `docs/migration-plan.md` 与旧单体下线
  - 依赖：任务 21~26
  - _Requirements: 16, 18_

  - [ ] 27.1 完成 `docs/migration-plan.md` 三阶段详述
    - 每阶段：进入条件 / 交付物 / 退出条件 / 验证用例清单 / 回滚步骤 / RTO 目标
    - 显式引用每条需求 R1~R18 与对应属性 P1~P32
    - _Requirements: 16.1, 16.3, 16.4, 18.1_

  - [ ] 27.2 `docs/migration-plan.md` 补充接口版本化策略
    - `v1` / `v2` 并存周期 ≥ 14 天
    - Feign `-api` 模块 v1 接口保持不变、新增 v2
    - _Requirements: 16.5_

  - [ ] 27.3 旧单体下线清单 Check-list
    - 所有 `/api/**` 路由从 `legacy-route` 移除
    - `legacy-route` 从 Nacos 删除
    - `legacy-adapter` 子模块归档并冻结
    - 删除 `major_assignment` 中剩余 Controller / Mapper / Entity（保留只读备份分支 `archive/legacy-final`）
    - _Requirements: 6.4, 16.1_

  - [ ]* 27.4 编写文档可溯源属性测试
    - **Property 32：文档需求可溯源**（R1~R18 所有 Acceptance Criteria 在 `design.md` 与 `migration-plan.md` 中至少被引用一次）
    - Markdown 扫描 + 正则 `R\d+\.\d+`
    - _Requirements: 18 文档可溯源不变量_
    - _Properties: P32 文档需求可溯源_

#### DevOps 与部署

- [ ] 28. Dockerfile / docker-compose / Helm 与 CI/CD
  - 依赖：任务 11~26
  - _Requirements: 15_

  - [ ] 28.1 每服务 Dockerfile（多阶段构建）
    - 使用 `maven:3.9-eclipse-temurin-17` builder + `eclipse-temurin:17-jre-jammy` runtime
    - `HEALTHCHECK` 指向 `/actuator/health`
    - 模板写入 `docs/deployment.md`
    - _Requirements: 15.1, 18.1_

  - [ ] 28.2 本地 `docker-compose.yml`
    - 聚合 Nacos / MySQL / Redis / RabbitMQ / Prometheus / Grafana / Gateway / 所有业务服务
    - _Requirements: 15.2_

  - [ ]* 28.3 Helm Chart（umbrella + 每服务 Chart）
    - `deploy/charts/{service}/` 结构；`values-{dev,test,prod}.yaml`
    - `platform-infra` chart 覆盖 Nacos / Prometheus / Loki / Tempo / RabbitMQ
    - 可选：仅当项目规模需要 K8s 部署时实施
    - _Requirements: 15.2, 15.5_

  - [ ] 28.4 CI/CD 流水线配置（GitHub Actions）
    - `.github/workflows/ci.yml`：`lint → unit → pbt → integration → contract → scan → docker-build → docker-push → deploy-test → e2e-smoke`
    - 任一阶段失败立即 `exit 1`
    - 镜像打标 `{service}-{gitShortSha}-{date}`
    - _Requirements: 15.3, 15.4_

  - [ ]* 28.5 编写 CI 流水线 YAML 结构断言
    - 解析 workflow YAML，断言 9 个阶段顺序与 `needs` 依赖
    - _Requirements: 15.3, 15.4_

  - [ ] 28.6 编写 `docs/deployment.md`
    - Dockerfile 模板 / docker-compose 说明 / Helm 部署指引 / 环境 Profile 与 Namespace 映射
    - _Requirements: 15.1, 15.2, 15.5, 18.1_

#### 阶段 3 最终检查点

- [ ] 29. 阶段 3 最终检查点 - 确保所有测试通过
  - 依赖：任务 20~28
  - 运行完整 `mvn clean verify` + 契约测试 + 事件驱动集成测试
  - 确认 P1~P32 属性测试全绿
  - 确认 `legacy-route` 已从 Nacos 删除
  - 确认所有业务微服务 jar 不含前端资源（P28）
  - 确认所有需求 R1~R18 在 `design.md` 与 `migration-plan.md` 中均有引用（P32）
  - 确保所有测试通过，如有问题询问用户
  - _Requirements: 6, 10, 12, 13, 14, 15, 16, 18_

---

## Notes（说明）

- 带 `*` 的任务为可选（主要是测试与 Helm Chart 类），可在 MVP 阶段跳过；顶层任务不得标 `*`，核心实现任务（骨架 / 迁移 / 网关切换 / 事件发布）不得标 `*`。
- 每条属性（P1~P32）均对应设计文档 `§Correctness Properties` 中的属性编号与需求条款，可通过 `jqwik` 在 Java 中实现 PBT。
- 属性测试任务刻意紧邻对应实现任务放置，以便错误在本任务内被发现。
- 检查点任务（10 / 19 / 29）是阶段退出的硬门槛；失败时返回对应阶段内的任务继续修复。
- 所有任务 **仅** 包含"写代码 / 写配置 / 写文档"这三类可由编码代理完成的工作；不包含用户培训、上线审批、业务演练、市场沟通等非编码活动。
- 实施语言统一为 Java 17 + Spring Boot 3.5.3 + Spring Cloud 2023.0.x（与设计一致），所有代码示例、PBT 与工具链基于该栈。

## Workflow Completion（工作流完成说明）

本 Requirements-First 工作流的规格阶段到此为止，**已完成需求 / 设计 / 任务三份产出物**：

- `.kiro/specs/spring-cloud-migration/requirements.md`
- `.kiro/specs/spring-cloud-migration/design.md`
- `.kiro/specs/spring-cloud-migration/tasks.md`

后续可以通过打开 `tasks.md` 并在需要执行的任务项旁点击 "Start task" 逐项落地。建议的执行顺序即本文件中的任务编号顺序（阶段 0 → 阶段 1 → 阶段 2 → 阶段 3），单个阶段内也按编号顺序执行以满足 "每步建立在前一步之上" 的约束。

请确认任务清单是否可以进入实施阶段，如有调整诉求请在本阶段提出。
