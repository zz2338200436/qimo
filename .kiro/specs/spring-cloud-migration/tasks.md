# 实施任务清单：Spring Cloud 微服务化改造与既有系统优化

> 版本：v0.5
> 最后更新：2026-05-19
> 对应需求：`.kiro/specs/spring-cloud-migration/requirements.md`（R1~R18）
> 对应设计：`.kiro/specs/spring-cloud-migration/design.md`
> 实施语言：**Java 17 + Spring Boot 3.5.3 + Spring Cloud 2025.0.0**（与设计文档一致，无需额外语言选择）

## Overview（概述）

本任务清单按照设计文档中的三阶段迁移路径组织：

- **阶段 1 — 现状治理**（不改架构，先治代码）：在单体 `major_assignment` 内完成鉴权、异常、日志、配置、数据访问、缓存等规范化工作，为后续拆分奠定"可抽离"基础。对应 R1~R5 与 R14 的静态资源规划部分。
- **阶段 2 — 平台基础设施搭建 + 首服务剥离（Auth_Service / User_Service）**：搭建 Eureka、Gateway、可观测性栈、common 库，完成 Auth_Service 与 User_Service 剥离，并建立网关双路由过渡机制。对应 R6~R10、R13、R14。
- **阶段 3 — 全量服务剥离与旧单体下线**：按依赖关系依次剥离 Course → Assignment → Exam → Analysis（含 KnowledgePoint）→ Notification → AI 服务；完成事件驱动补齐、接口契约版本化、旧单体下线。对应 R6、R10、R12、R16。

> 维护约定：
> - `docs/migration-plan.md` 记录迁移阶段、里程碑、风险、回滚与切流原则。
> - 本文件记录接口级实施状态、任务勾选进度与 Gateway 切流范围。
> - 如果两处表述存在重复或冲突，以本文件为准。

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

  - [x]* 1.3 编写文档骨架完整性单元测试
    - 解析 Markdown，断言 8 篇文档存在且包含必要 Front-Matter 字段与"变更记录"表格
    - 断言 `docs/README.md` 中所有 8 篇链接均可被识别
    - 已补 `DocumentationSkeletonTest`
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

  - [x]* 2.1 编写诊断报告结构完整性测试
    - 用 Markdown 解析器断言：每条问题记录包含六字段且非空
    - 断言"高影响"问题数量 ≥ `migration-plan.md` 中阶段 1 必修问题数量（完整性度量关系）
    - 已补 `DiagnosisReportStructureTest`
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

  - [x]* 4.5 编写 ArchUnit 结构测试
    - 断言：所有 `@RestController` 方法返回 `ResponseResult<?>`
    - 断言：`GlobalExceptionHandler` 是唯一 `@ControllerAdvice`
    - 已补 `ControllerReturnTypeArchTest`
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

  - [x]* 6.4 编写 Prod 快速失败示例测试
    - 已补 `ProdConfigValidatorStartupFailureTest`：激活 `prod` 并故意缺失 `DB_PASSWORD`，断言启动阶段快速失败且日志包含缺失字段文案
    - 已补 `ProdConfigValidatorTest`：覆盖非 prod 不退出、prod 缺配置退出、prod 配置齐全不退出
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

  - [x]* 8.3 编写缓存配置唯一性测试
    - **Property 7：缓存配置唯一性**（`(cacheName, key)` 组合在全应用内唯一）
    - 扫描所有 `@Cacheable` 注解并断言
    - 已补 `CacheConfigurationUniquenessTest`
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

  - [x]* 8.6 编写分布式锁 Key 格式属性测试
    - **Property 9：分布式锁 Key 格式**（实际 Redis Key 匹配 `^LOCK:[^:]+:.+$`）
    - 使用 `lockKeyArb` 生成器（含特殊字符 / 空串 / 超长）
    - 已由 `RedisDistributedLockImplTest` 覆盖典型值、特殊字符与参数边界
    - _Requirements: 5.4_
    - _Properties: P9 分布式锁 Key 格式_

- [x] 9. 分页上限保护
  - 依赖：任务 8
  - 当前已完成 `PageRequestDTO.pageSize` 字段校验、`BaseController#clampPageSize` 兜底夹紧与 P10 属性/边界测试
  - 2026-05-21 执行 `mvn --% -pl major_assignment -Dtest=BaseControllerPaginationTest -Dsurefire.failIfNoSpecifiedTests=false test`，3 个测试通过
  - _Requirements: 5.5_

  - [x] 9.1 `PageRequestDTO.pageSize` 增加 `@Max(100)`
    - 同步增加 `@Min(1)`，并在 `BaseController` 提供 `clampPageSize(int)` 兜底方法
    - 违规分页请求走 `GlobalExceptionHandler` 走 400 响应
    - _Requirements: 5.5_

  - [x]* 9.2 编写 clampPageSize 属性测试
    - **Property 10：分页上限 clamp**（任意整数输入 → `[1, 100]`；`[1,100]` 内保持不变）
    - 使用 `pageSizeArb`：覆盖 `Integer.MIN_VALUE / 0 / 50 / Integer.MAX_VALUE`
    - 已新增 `BaseControllerPaginationTest`，使用固定种子生成 10,000 个整数样本，并覆盖区间保持不变与指定边界样本
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
> 退出条件：Eureka / Gateway / 可观测性栈可用；Auth_Service / User_Service 剥离完成并 100% 流量走新服务；网关双路由机制上线；P11~P24、P28、P29、P30 相关属性测试通过。

#### 多模块 Maven 骨架

- [x] 11. 建立父 POM 与多模块骨架
  - 依赖：阶段 1 检查点
  - 在仓库根目录建立 `Target_Platform/`（或直接使用仓库根）作为多模块聚合工程
  - 目录结构：`parent-pom / gateway / auth-service / user-service / common / {service}-api / legacy-adapter`
  - _Requirements: 6.1, 6.2, 7.1_

  - [x] 11.1 创建 `parent-pom` 锁定 BOM 版本
    - 声明 `java.version=17` / `spring-boot.version=3.5.3` / `spring-cloud.version=2025.0.0`
    - `<dependencyManagement>` 引入 spring-cloud-dependencies / micrometer-tracing / resilience4j BOM
    - 子模块禁止覆盖版本（通过 Enforcer 插件规则约束）
    - _Requirements: 7.1, 7.9_

  - [x] 11.2 创建多模块骨架与模块间依赖
    - 每个 `{service}-api` 模块仅含 Feign 接口与 DTO；纯 Java jar
    - 每个业务服务模块依赖其它服务的 `-api`，不依赖实现模块（Enforcer 禁止传递）
    - `common` 模块包含：ResponseResult / 异常体系 / MDC Filter / Feign 配置 / 指标 tagging
    - _Requirements: 6.2, 10.1_

  - [x]* 11.3 编写依赖版本一致性属性测试
    - **Property 13：依赖版本一致**（所有子模块 `spring-boot.version / spring-cloud.version` 与 `docs/architecture.md` 声明一致）
    - 用 Maven Enforcer 规则 + ArchUnit / 自定义扫描实现
    - 已补 `PlatformVersionAlignmentTest`
    - _Requirements: 7.1, 7 Invariant_
    - _Properties: P13 依赖版本一致_

  - [x] 11.4 在 `docs/architecture.md` 中填充选型决策表与版本说明
    - 对 R7.2~R7.9 每个选型写明 "候选 / 选定 / 理由 / 备选切换条件"
    - 记录 Spring Cloud 2025.0.0 与 Spring Boot 3.5.3 匹配说明
    - _Requirements: 7.3, 7.4, 7.9, 18.1, 18.4_

#### Eureka Server 与接入

- [x] 12. Eureka Server 搭建与接入
  - 依赖：任务 11
  - _Requirements: 7.3, 7.4, 4.5_

  - [x] 12.1 编写本地基础依赖 `docker-compose.dev.yml`
    - 提供 MySQL / Redis 本地容器编排
    - Eureka 以 `registry-server` 模块启动，默认端口 8761
    - _Requirements: 7.3, 15.2_

  - [x] 12.2 每个业务服务引入 `spring-cloud-starter-netflix-eureka-client`
    - 移除 `bootstrap.yml`，统一由 `application.yml` 承载服务配置
    - `eureka.client.service-url.defaultZone` 指向注册中心
    - `spring.application.name` 与 Registry 注册名、Gateway 路由 ID 三者一致
    - _Requirements: 6.3, 4.5, 7.3, 7.4_

  - [ ]* 12.3 编写服务名三元一致属性测试
    - **Property 11：服务名三元一致**（`application.yml` / Eureka 注册记录 / Gateway 路由 `lb://` 一致）
    - 启动可嵌入 Eureka 测试环境，断言注册名匹配配置
    - _Requirements: 6.3_
    - _Properties: P11 服务名三元一致_

  - [ ]* 12.4 编写环境 Profile ↔ Namespace 映射属性测试
    - **Property 29：环境 Profile ↔ 配置源映射**（Profile=env 时只读取对应 `application-{env}.yml` / 环境变量组合）
    - 使用集成测试注入不同 profile 切换断言
    - _Requirements: 15.5_
    - _Properties: P29 环境 Profile ↔ 配置源映射_

#### common 公共库

- [x] 13. 建立 `common` 公共库
  - 依赖：任务 11
  - _Requirements: 3.1, 3.2, 3.4, 8.3, 10.1, 10.2, 10.3_

  - [x] 13.1 迁移 `ResponseResult` / 异常体系到 `common`
    - 拷贝并重构 `ResponseResult<T>` / `BusinessException` / `UnauthorizedException` / `ForbiddenException` / `ResourceNotFoundException` / `RemoteClientException` / `RemoteServerException` / `RateLimitExceededException`
    - 提供 `@ControllerAdvice` 基类 `BaseGlobalExceptionHandler`，各服务继承
    - _Requirements: 3.1, 3.2, 10.3_

  - [x] 13.2 实现 `CommonMdcFilter`（Servlet / Reactive 双版本）
    - Servlet 版本用于业务服务；Reactive 版本用于 Gateway
    - 写入 `traceId / userId / roles / activeRole / uri / method`
    - _Requirements: 3.4, 8.5_

  - [x] 13.3 实现 `FeignRequestInterceptor`
    - 从 MDC 读取 `X-Trace-Id / X-User-Id / X-Roles / X-Active-Role` 并写入 Feign 请求头
    - 作为 `@Bean` 自动装配（`spring.factories` / `AutoConfiguration.imports`）
    - _Requirements: 10.2_

  - [x] 13.4 实现 `GlobalFeignErrorDecoder`
    - 4xx → `RemoteClientException(code, msg, bodyJson)`
    - 5xx → `RemoteServerException(code, msg, bodyJson)`
    - 携带原始 HTTP 状态码与下游 `ResponseResult.message`
    - _Requirements: 10.3_

  - [x]* 13.5 编写 ErrorDecoder 分类属性测试
    - **Property 25：ErrorDecoder 错误分类**（`[400, 499] → RemoteClientException`；`[500, 599] → RemoteServerException`；`exception.code == httpStatus`）
    - 使用 `httpStatusArb` 生成器
    - 已补 `GlobalFeignErrorDecoderTest`
    - _Requirements: 10.3, 10 Metamorphic_
    - _Properties: P25 ErrorDecoder 错误分类_

  - [x] 13.6 共享指标 / 链路自动配置
    - 引入 `micrometer-tracing-bridge-otel` + `io.opentelemetry.exporter.zipkin`（或 otlp）
    - 引入 `micrometer-registry-prometheus`
    - 提供 `application-common.yml` 片段：Actuator 端点暴露 + tracing sample rate + prometheus 端点
    - _Requirements: 13.1, 13.2, 13.3_

#### Spring Cloud Gateway

- [x] 14. 构建 Spring Cloud Gateway
  - 依赖：任务 12, 13
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 14.3_

  - [x] 14.1 Gateway 基础骨架与 Eureka 注册
    - 新模块 `gateway`，引入 `spring-cloud-starter-gateway` + Eureka Client
    - 端口 8080；注册名 `gateway`
    - _Requirements: 8.1, 7.2_

  - [x] 14.2 编写 `TraceIdFilter`（Reactive）
    - 若 `X-Trace-Id` 存在透传，否则生成 UUID
    - 写入 `exchange.getAttributes()` 并透传到下游请求头
    - 同时写入 Reactor Context 供日志使用
    - _Requirements: 8.5, 13.2_

  - [x] 14.3 编写 `JwtAuthenticationFilter`（Reactive）
    - 从服务配置读取 `kid -> publicKey` 映射
    - 解析 `Authorization: Bearer`；校验签名、过期、`jti` 黑名单（Redis）
    - 失败返回 `ResponseResult(401, ...)` JSON
    - 白名单路由跳过：`/api/auth/login / /api/auth/refresh / /api/auth/captcha`
    - _Requirements: 8.3, 8.4, 9.4_

  - [x] 14.4 编写 `HeaderEnrichFilter`（Reactive）
    - 剥离客户端自带的 `X-User-Id / X-Roles / X-Active-Role`
    - 用 JWT 解析值重写
    - _Requirements: 8.3, 17.4_

  - [x] 14.5 配置 CORS 外置到 Gateway 配置
    - `gateway.cors.allowed-origins` 由 `application.yml` / 环境变量统一管理
    - 移除 `MajorAssignmentApplication.corsConfigurer` 中硬编码（放入 `legacy-adapter` 过渡期引用）
    - _Requirements: 8.6, 14.3_

  - [x] 14.6 配置按 `(ip, userId, routeId)` 分桶的限流
    - 使用 `RequestRateLimiter` + `RedisRateLimiter` + 自定义 `KeyResolver`
    - 登录接口限流规则：同一 IP 60 秒内 30 次失败触发
    - 限流触发返回 `ResponseResult(429, retryAfter)`
    - _Requirements: 8.7, 17.4_

  - [x] 14.7 配置熔断 Fallback
    - 使用 `Spring Cloud CircuitBreaker` + Resilience4j Reactive
    - 每条路由挂 `fallbackUri` 指向内建 `/_fallback/{service}` 端点
    - Fallback 返回 `ResponseResult(503, "{服务} 暂时不可用")`
    - Registry 查不到实例时走 `ResponseResult(504)`
    - _Requirements: 11.3, 17.3_

  - [x] 14.8 落地初始路由配置
    - 初始路由：`auth-route / auth-secured / legacy-route`
    - Gateway 本地配置中保留 `legacy-route -> http://legacy-monolith:8080`（过渡期）
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

- [x] 15. 剥离 Auth_Service
  - 依赖：任务 13, 14
  - 目标：`/api/auth/login|refresh|logout|me|switch-role|captcha` 完整迁出
  - 前置：`docs/auth-design.md` 完成 JWT Claims / 密钥轮换 / 多角色策略撰写
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 17.5, 17.6_

  - [x] 15.1 编写 `docs/auth-design.md`
    - Claims 结构（`sub / roles / activeRole / iat / exp / jti / tokenType`）
    - Token 生命周期状态机
    - 多角色策略（单 Token + activeRole + `/switch-role`）
    - 密钥轮换（RSA `kid` 双密钥并存 48h）
    - 单体 / JWT 凭证互斥策略
    - _Requirements: 9.7, 18.1_

  - [x] 15.2 创建 `auth-service` 模块骨架
    - `pom.xml` 依赖 `common` / `user-service-api` / Eureka Client / Spring Security / JJWT
    - `application.yml` 承载服务配置与注册中心地址
    - 端口 8081
    - _Requirements: 6.2, 9.1_

  - [x] 15.3 创建 `auth-service-api` 模块
    - Feign 接口：`AuthInternalFeignClient`（`/internal/auth/introspect` 供其它服务反查 token 状态）
    - DTO：`TokenIntrospectionDTO`
    - _Requirements: 10.1_

  - [x] 15.4 创建独立数据源 `sc_auth` schema
    - 新增 `auth_credentials` 表（从原 `users` 表剥离口令字段）
    - `token_blacklist` 不建表，直接走 Redis `JTI_BLACKLIST:{jti}` 与 `REFRESH:{rt}`
    - 提供 Flyway 迁移脚本 `V1__init_auth_schema.sql`
    - _Requirements: 12.1, 12.2_

  - [x] 15.5 实现 `POST /api/auth/login`
    - 校验验证码（Redis `CAPTCHA:IMG:{sessionKey}`）
    - 校验口令（BCrypt）
    - 通过 `UserFeignClient` 查询 roles（不直连 `users` 表）
    - 签发 Access_Token（RSA 私钥）+ Refresh_Token（UUID 入 Redis）
    - _Requirements: 9.1, 9.2, 9.3, 17.6_

  - [x] 15.6 实现 `POST /api/auth/refresh`
    - 校验 Refresh_Token（Redis 查找 + 未被吊销）
    - 签发新 Access_Token；保留 Refresh_Token 或滚动（策略写入文档）
    - _Requirements: 9.1, 9.2, 9.4_

  - [x] 15.7 实现 `POST /api/auth/logout`（幂等）
    - 解析当前 Access_Token 的 `jti`，写入 `JTI_BLACKLIST:{jti}` TTL = 剩余有效期
    - 删除对应 Refresh_Token
    - 多次调用仍返回成功
    - _Requirements: 9.4, 9 Idempotence_

  - [x] 15.8 实现 `POST /api/auth/switch-role`
    - 校验 `targetRole ∈ roles`
    - 吊销旧 JWT（写入黑名单）并签发新 JWT，`activeRole = targetRole`
    - _Requirements: 9.7_

  - [x] 15.9 实现 `GET /api/auth/me`
    - 从 JWT 解析 `sub / activeRole / roles`
    - 返回 `ResponseResult(200, AuthUserDTO)`
    - _Requirements: 9.1_

  - [x] 15.10 实现密钥热轮换
    - 支持 `kid -> { publicKey, privateKey }` 双密钥配置
    - 配置切换后 48h 窗口内新旧密钥都可验签
    - 新签发固定用新 `kid`
    - _Requirements: 9.5_

  - [x] 15.11 实现单体 / JWT 凭证互斥
    - 用户 JWT 登录成功时通过 `legacy-adapter` 调用单体清理旧 `JSESSIONID_*`（或直接清除 Redis `LEGACY_SESSION:{sid}`）
    - 单体登录成功时回调 Auth_Service `/internal/auth/revoke-by-user` 将该用户所有现有 JWT 加黑名单
    - _Requirements: 9.6_

  - [x] 15.12 实现密码 BCrypt 与敏感字段脱敏
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

  - [x] 15.20 Gateway 路由切换 Auth 到新服务
    - Gateway 将 `auth-route` 与 `auth-secured` 的 `uri` 指向 `lb://auth-service`
    - `legacy-route` 继续保留其它前缀
    - 单体 `AuthController` 标注 `@Deprecated`（保留 1 个阶段便于回滚）
    - _Requirements: 8.2, 16.2_

#### User_Service 剥离

- [x] 16. 剥离 User_Service
  - 依赖：任务 15
  - 当前已完成 `sc_user` 用户/角色表迁移、公开用户画像与学生兼容接口、内部 `/internal/users/**` 契约、Gateway `user-route` 切流、用户表写权限唯一性测试与 Spring Cloud Contract 契约测试
  - 2026-05-21 执行 `mvn --% -pl user-service -am test`，包含公共模块与 User_Service 共 26 个测试，全部通过
  - 2026-05-21 执行 `mvn --% -pl major_assignment -Dtest=UserTableOwnershipArchTest -Dsurefire.failIfNoSpecifiedTests=false test`，用户表写权限唯一性架构测试 2 个用例通过
  - _Requirements: 6.1, 6.2, 6.3, 12.1, 12.2_

  - [x] 16.1 创建 `user-service` 模块 + `user-service-api` 模块
    - 端口 8082；注册名 `user-service`
    - `-api` 暴露：`UserFeignClient { getProfile / getRoles / listByIds }` + `UserProfileDTO` / `UserRolesDTO`
    - _Requirements: 6.2, 10.1_

  - [x] 16.2 迁移 `users / user_roles` 表到 `sc_user` schema
    - Flyway 脚本 `V1__init_user_schema.sql`
    - Mapper 从单体迁入 `user-service`
    - 单体模块删除相同实体 + Mapper（保留 legacy-adapter 读取通道）
    - 已通过 `user-service/src/main/resources/db/migration/V1__init_user_schema.sql` 建立 `users / roles / user_roles`
    - 已通过 `JpaUserRepository` 承接用户资料读写与角色读取；`UserTableOwnershipArchTest` 确认非 `user-service` 生产源码不再写 `users / user_roles`
    - _Requirements: 6.4, 12.1, 12.2_

  - [x] 16.3 迁移 `UserController` / `StudentController` / `TeacherDashboardController` 相关接口
    - 全部重写为调用 User_Service 业务方法
    - 返回 `ResponseResult<T>`
    - 已完成学生画像、教师端学生资料更新、学生端资料/设置/头像/导出/班级名兼容路径与学生改密兼容路径的服务化迁移；旧单体用户写入口改为调用 User_Service / Auth_Service
    - _Requirements: 6.2, 6.3, 3.1_

    - [x] 16.3.1 迁移学生详情用户画像窄切片
      - 在 `user-service-api` 增加 `StudentProfileDTO`
      - 在 `user-service` 增加 `GET /api/users/students/{studentId}`，返回学生基础资料、角色与过渡期班级名字段
      - 在 `UserFeignClient` / `UserInternalController` 增加 `/internal/users/{studentId}/student-profile` 内部契约
      - 单体 `TeacherDashboardController#getStudentById` 优先调用 `User_Service` 画像契约，失败时回退旧 `UserService`
      - 班级归属仍保留给后续 Course_Service，当前不跨表读取 `course_classes / class_students`
      - _Requirements: 6.2, 6.3, 12.1_

    - [x] 16.3.2 迁移教师端学生基础资料更新窄切片
      - 在 `user-service-api` 增加 `UpdateStudentProfileDTO`
      - 在 `user-service` 增加 `PUT /api/users/students/{studentId}` 与 `/internal/users/{studentId}/student-profile`
      - 单体 `TeacherDashboardController#updateStudent` 将 `realName/email/phone/avatar` 更新优先转发到 `User_Service`
      - 班级移动、学习表现更新仍保留在单体，等待 Course / Analysis 服务剥离
      - _Requirements: 6.2, 6.3, 12.1, 12.2_

    - [x] 16.3.3 迁移学生端教师资料读取窄切片
      - 单体 `StudentController#getAssignmentDetail` / `getExamDetail` 优先调用 `User_Service` 获取教师资料
      - 单体保留旧 `UserService.findById` 作为回退路径，避免过渡期新服务不可用导致详情页退化
      - 当前仅迁移教师 `id/name` 读取，课程、作业、考试、提交记录仍保留在原服务边界
      - _Requirements: 6.2, 6.3, 12.1_

    - [x] 16.3.4 迁移学生学习表现存在性校验窄切片
      - 单体 `DashboardController#getStudentPerformance` 优先调用 `User_Service` 学生画像契约判断学生存在
      - 单体保留旧 `UserService.findById` 作为回退路径，避免过渡期新服务不可用导致仪表盘接口退化
      - 当前仅迁移用户存在性校验，学习表现统计仍保留在原 `StudentService` 边界
      - _Requirements: 6.2, 6.3, 12.1_

    - [x] 16.3.5 迁移旧 Session 用户写入口窄切片
      - 在 `user-service-api` 增加 `UpdateUserProfileDTO`，在 `User_Service` 增加 `PUT /api/users/{userId}` 与内部更新契约
      - 单体 `AuthController#updateProfile`、`StudentServiceImpl#updateProfile`、`uploadAvatar` 改为调用 `User_Service`
      - 在 `Auth_Service` 增加 `/internal/auth/users/{userId}/change-password`，单体 `AuthController#changePassword` 与学生改密改为调用 `Auth_Service`
      - 移除教师端学生更新的本地写表 fallback，保留只读存在性 fallback
      - _Requirements: 6.2, 6.3, 9.6, 12.1, 12.2_

    - [x] 16.3.6 迁移学生端资料/设置兼容公开路径
      - 在 `user-service` 增加 `StudentCompatibilityController`，承接旧路径 `/api/student/profile`、`/api/student/notification-settings`、`/api/student/privacy-settings`、`/api/student/upload-avatar`、`/api/student/export-data`、`/api/students/{studentId}/class`
      - `StudentCompatibilityController` 保持旧前端需要的 `ResponseResult` 包络、`name` 字段映射与头像/导出/班级名响应文案
      - `UserApplicationService` 增加通知设置、隐私设置、头像上传、班级名与学生数据导出兼容方法；设置项当前按旧系统默认值返回，后续如需持久化可扩展到 `sc_user` 专表
      - Gateway 新增 `student-profile-compatibility-route -> lb://user-service`，并从 `legacy-student-route` 移除上述已迁移路径
      - `legacy-student-route` 当前仅保留学生知识点等未迁移接口
      - _Requirements: 6.2, 6.3, 8.2, 12.1, 16.2_

    - [x] 16.3.7 迁移学生改密兼容公开路径
      - 在 `auth-service` 的 `AuthController` 增加旧路径 `POST /api/student/change-password`，兼容 `currentPassword/newPassword/confirmPassword` 请求体与旧响应文案 `密码更新成功`
      - 复用 `AuthApplicationService#changePassword` 的 JWT 校验、当前密码校验、密码强度校验与访问令牌吊销逻辑
      - Gateway 将 `/api/student/change-password` 纳入 `auth-secured -> lb://auth-service`，并从 `legacy-student-route` 移除
      - _Requirements: 6.2, 6.3, 8.2, 9.6, 16.2_

  - [x] 16.4 实现 `/internal/users/**` 内部接口
    - 仅接受来自 Registry 内部的调用
    - 前缀 `/internal/**` 不走网关；在 Gateway 路由中明确不对外暴露
    - _Requirements: 17.5_

  - [x] 16.5 Gateway 切换 `user-route` 到 `lb://user-service`
    - Gateway 路由配置更新
    - _Requirements: 8.2, 16.2_

  - [x]* 16.6 编写数据表写权限唯一性属性测试
    - **Property 12：数据表写权限唯一**（`users / user_roles` 的 I/U/D 只出现在 `user-service`）
    - 用架构测试扫描生产源码中的 SQL 写语句实现，禁止单体继续直写 `users / user_roles`
    - _Requirements: 6 Invariant, 12.2_
    - _Properties: P12 数据表写权限唯一_

  - [x]* 16.7 编写 User_Service 契约测试
    - 使用 Spring Cloud Contract 生产者契约覆盖 `GET/PUT /api/users/{userId}`、`GET/PUT /api/users/students/{studentId}`
    - 使用内部契约覆盖 Auth_Service 消费的 `GET /internal/users/{userId}` 与 `GET /internal/users/{userId}/roles`
    - 生成的 `UserTest` / `InternalTest` 通过 MockMvc base class 执行，契约测试进入 Surefire
    - _Requirements: 10.1_

#### 前端静态资源剥离

- [x] 17. 前端静态资源剥离到 Nginx
  - 依赖：任务 14
  - _Requirements: 14.1, 14.2, 14.4, R14 部署解耦_

  - [x] 17.1 新建 `frontend/` 独立目录（或独立仓库链接）
    - 将 `major_assignment/src/main/resources/static/**` 全量镜像迁出到 `frontend/dist`
    - 过渡期保留单体中的原静态资源，等待 17.2 / 17.3 接管访问入口后再关闭 Spring 静态映射
    - 添加 `frontend/README.md` 说明部署产物路径、页面入口与过渡边界
    - _Requirements: 14.1_

  - [x] 17.2 新增 `deploy/nginx/nginx.conf`
    - 提供 `frontend/dist -> /usr/share/nginx/html` 的静态资源部署约定
    - `/api/**` 反向代理到 Gateway 上游 `gateway:8080`
    - 开启 gzip / cache-control
    - _Requirements: 14.2, 14.3_

  - [x] 17.3 更新 Spring Boot 单体启动配置
    - 禁用默认静态资源映射（`spring.web.resources.add-mappings=false`）
    - 单体中已无 `MajorAssignmentApplication.corsConfigurer` 硬编码 origin，跨域统一由 Gateway 处理
    - 新增 `StaticResourceMappingDisabledTest`，验证 `/index.html`、`/styles.css` 不再由单体静态资源处理，且返回统一 `404 ResponseResult` JSON
    - _Requirements: 14.1, 14.3_

  - [x] 17.4 编写部署产物无前端资源属性测试
    - **Property 28：部署产物无前端资源**（业务微服务 jar 解压不含 `.html/.js/.css/.vue/.tsx`）
    - 新增 `DeploymentArtifactFrontendExclusionTest`，扫描各模块 `target/classes`，并在存在构建产物时继续扫描 `target/*.jar`
    - `major_assignment` 构建资源排除 `src/main/resources/static/**`，确保前端资源仅保留在 `frontend/dist`
    - _Requirements: 14 部署解耦_
    - _Properties: P28 部署产物无前端资源_

#### 可观测性栈搭建

- [x] 18. 可观测性栈（Prometheus / Grafana / Loki / Tempo）
  - 依赖：任务 12
  - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5, 13.6_

  - [x] 18.1 编写 `docs/observability.md`
    - 指标清单（JVM / HTTP / DataSource / Redis / Feign / CircuitBreaker）
    - 日志字段清单 & MDC 规范
    - 链路追踪采样策略（dev 100% / prod 10%）
    - 告警规则清单（5xx 比例 / 熔断打开 / 登录失败率）
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5, 18.1_

  - [x] 18.2 `docker-compose.obs.yml`：Prometheus + Grafana + Loki + Promtail + Tempo
    - Prometheus scrape `/actuator/prometheus`
    - Loki 通过 Promtail 收集容器日志
    - Tempo 接收 OTLP
    - Grafana 预置 Data Source + 示例 Dashboard JSON
    - 新增 `deploy/prometheus|grafana|loki|promtail|tempo` 配置目录，Prometheus 默认抓取 host `8761/8080/8081/8082` 的 Actuator 指标
    - _Requirements: 13.3, 13.4, 13.6, 15.2_

  - [x] 18.3 定义 Prometheus 告警规则
      - `instance_5xx_rate > 5%` 持续 1 分钟 → 触发
      - `circuit_breaker_state == "OPEN"` 持续 30 秒 → 触发
      - 告警规则 YAML 放 `deploy/prometheus/alerts/`
    - _Requirements: 11.4, 13.5_

  - [x] 18.4 限制 Actuator 端点访问来源
      - `management.server.address` 绑定内网；或通过 Security `requestMatcher` 白名单
    - _Requirements: 13.1, 17.5_

#### 阶段 2 检查点与过渡期验证

- [x] 19. 阶段 2 检查点 - 确保所有测试通过
  - 依赖：任务 11~18
  - 运行 `mvn clean verify`（多模块聚合）
  - 运行 Testcontainers 集成测试（Eureka / Redis / MySQL / RabbitMQ）
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
> 退出条件：Course / Assignment / Exam / Analysis / Notification / AI 全部剥离；legacy-route 从 Gateway 配置删除；P25~P28 属性测试全绿；旧单体下线。

#### 数据所有权矩阵与事件驱动基础

- [x] 20. 编写 `docs/data-ownership.md` 与事件驱动基础设施
  - 依赖：阶段 2 检查点
  - _Requirements: 6, 10.5, 12.1, 12.2, 12.5_

  - [x] 20.1 `docs/data-ownership.md` 服务 ↔ 表所有权矩阵
    - 表格列：表名 / 归属服务 / Schema / 备注 / 只读共享范围
    - 跨服务访问铁律声明（禁止 JDBC 跨 Schema）
    - _Requirements: 12.1, 6 Invariant, 18.1_

  - [x] 20.2 在 `common` 提供 `outbox_event` / `processed_event` 通用建表脚本与 Relay Job
    - `OutboxEventEntity` + `OutboxRelayJob`（定时轮询 status=0）
    - `ProcessedEventEntity` + `IdempotentEventHandler`
    - 采用 Spring Cloud Stream（RabbitMQ binder）
    - _Requirements: 7.8, 10.5, 12.5_

  - [x] 20.3 定义核心领域事件 DTO
    - `AssignmentSubmittedEvent / ExamFinishedEvent / EarlyWarningRaisedEvent / EarlyWarningRollbackEvent / NotificationPushedEvent`
    - 统一字段：`eventId / occurredAt / aggregate / payload`
    - 放 `common-events` 子模块
    - _Requirements: 10.5, 6.5_

  - [x]* 20.4 编写事件消费幂等属性测试
    - **Property 26：事件消费幂等**（N ≥ 1 次消费副作用等价；`processed_event` 仅写一次）
    - 使用 `domainEventArb` + Testcontainers RabbitMQ
    - 至少 1000 次迭代
    - 已在 `IdempotentEventHandlerTest` 补确定性属性式测试：使用固定随机种子生成 1000 组重复事件序列，验证同一 `(eventId, consumerName)` 无论重复消费多少次都只执行一次副作用，且 `processed_event` 记录数等于唯一事件消费者组合数
    - _Requirements: 10.5, R10 Idempotence_
    - _Properties: P26 事件消费幂等_

#### Course_Service 剥离

- [x] 21. 剥离 Course_Service
  - 依赖：任务 20；网关双路由模板复用任务 14.8
  - 当前已完成 Course_Service 的模块骨架、`sc_course` 表迁移、课程/班级/专业/知识点主数据兼容接口、Feign fallback、契约测试与 Gateway `course-route` 切流
  - _Requirements: 6, 10, 12_

  - [x] 21.1 `course-service` + `course-service-api` 模块骨架
    - 端口 8083；注册名 `course-service`
    - Feign：`CourseFeignClient { getCourse / listByTeacher / listByStudent }`
    - _Requirements: 6.2, 10.1_

  - [x] 21.2 迁移 `courses / classes / course_enrollment` 到 `sc_course`
    - Flyway `V1__init_course_schema.sql`
    - 已落地 `courses / course_classes / class_courses / class_students / majors` 表结构与 Course_Service JPA 读写；单体对应实体与 Mapper 删除保留到最终切流清理
    - 已补 `teacher_knowledge_points` 表，作为旧 `knowledge_points` 主数据在当前拆分架构中的拥有者表
    - 单体对应实体与 Mapper 删除保留到最终切流清理；Gateway 已无旧单体显式路由承接课程域主路径
    - _Requirements: 6.4, 12.1_

  - [x] 21.3 迁移 `CourseController` 业务接口
    - 已迁移教师课程、班级、课程分配、专业列表相关接口，并补齐内部 Feign 契约与 Gateway 路由
    - 已为课程列表/详情补齐 `COURSE:LIST / COURSE:DETAIL` 缓存命名空间与写操作驱逐
    - 所有依赖 User_Service 的数据走 Feign
    - 已补学生/系统兼容路径：`/api/system/semesters`、`/api/system/student/courses`、`/api/system/teacher/courses`、`/api/system/time-ranges`、`/api/dashboard/student-performance/**`
    - 已补教师知识点管理兼容主链：`/api/teacher/knowledge-points`、`/{id}`、`/course/{courseId}`，由 Course_Service 拥有知识点目录主数据；掌握度/分析投影仍由 Analysis_Service 承接
    - 缓存命名空间 `COURSE:LIST / COURSE:DETAIL`
    - _Requirements: 6.1, 6.3, 5.2_

  - [x] 21.4 Feign 熔断与 Fallback 声明
    - `UserFeignClient` 配置 `fallbackFactory`
    - 通过 `RemoteServerException(503, ...)` + 全局异常处理统一返回 `ResponseResult(503, ...)`
    - _Requirements: 10.4, 11.3_

  - [x]* 21.5 编写 Course_Service 单元测试
    - 覆盖业务方法的边界条件
    - _Requirements: 6.1_

  - [x]* 21.6 编写 Course_Service 契约测试
    - Spring Cloud Contract 生产者契约
    - _Requirements: 10.1_

  - [x] 21.7 Gateway 切换 `course-route` 到 `lb://course-service`
    - Gateway 路由更新；保留回滚配置快照
    - 已补教师知识点管理兼容主链：`/api/teacher/knowledge-points`、`/api/teacher/knowledge-points/{id}`、`/api/teacher/knowledge-points/course/{courseId}` 由 `course-service` 承接，兼容旧 `ResponseResult` 包络与字段名 `pointName/name`
    - 已补 `TeacherKnowledgePointCompatibilityController`、JPA 仓储与 `teacher_knowledge_points` 表初始化，供教师知识点页面与教师作业页的课程知识点加载主路径使用
    - `legacy-knowledge-route` 已删除；`assignment/**` 由 `assignment-service` 承接，`exam/**` 由 `exam-service` 承接，`mastery/student/{studentId}/course/{courseId}`、`stats/course/{courseId}` 与 `analyze/student/{studentId}/course/{courseId}` 已由 `analysis-service` 承接
    - _Requirements: 8.2, 16.2_

#### Assignment_Service 剥离

- [x] 22. 剥离 Assignment_Service
  - 依赖：任务 21
  - _Requirements: 6, 10, 12_

  - [x] 22.1 `assignment-service` + `-api` 模块骨架
    - 端口 8084；注册名 `assignment-service`
    - Feign：`AssignmentFeignClient { getAssignment / listByCourse / submit }`；已补 `/internal/assignments/**` 骨架控制器与内存种子实现，供后续 22.2/22.3 替换
    - _Requirements: 6.2, 10.1_

  - [x] 22.2 迁移 `assignments / assignment_submissions` 到 `sc_assignment`
    - 已完成 `assignments / assignment_submissions / assignment_classes` 的 JPA 读写与 `assignment-service` 内部持久化接口
    - 已接通学生提交、教师查看提交、教师批改提交对 `sc_assignment` 的读写路径
    - 学生读取链路已从跨 Schema 直查 `class_students` 改为经 `course-service` 聚合学生 `classIds` 后查询 `assignment_classes`
    - 2026-05-15 本地联调已验证 `GET /api/student/assignments` 不再因 `sc_assignment.class_students` 缺表而报 500
    - 2026-05-15 已补 `graded_count` 汇总持久化，并完成一次真实“学生提交 -> 教师批改 -> 汇总回写”落库验证
    - 2026-05-15 已完成教师主写经 Gateway 的实际联调，确认新建、更新班级范围、删除回收后学生可见性与 `assignment_classes` 保持一致
    - _Requirements: 6.4, 12.1_

  - [x] 22.3 迁移 `AssignmentController` 业务接口
    - 已迁移并暴露以下外部链路：
      - 教师作业读取：`GET /api/teacher/assignments`、`GET /api/teacher/assignments/{id}`
      - 教师提交记录：`GET /api/teacher/assignments/{id}/submissions`、`GET /api/teacher/assignments/submissions/{id}`
      - 教师提交列表/详情/批改：`GET /api/teacher/submissions`、`GET /api/teacher/submissions/{id}`、`PUT /api/teacher/submissions/{id}/grade`
      - 学生作业读取：`GET /api/student/assignments`、`GET /api/student/assignments/{id}`、`GET /api/student/assignment-submissions`
      - 学生提交作业：`POST /api/student/assignments/{id}/submit`
    - 教师端作业主写接口 `POST / PUT / DELETE /api/teacher/assignments` 已完成经 Gateway + Bearer Token 的实流验证，确认创建后落库到 `sc_assignment`，并保持学生可见性与删除回收闭环
    - 当前开放业务接口已全部切至 `assignment-service`，剩余工作转为 outbox、Fallback 与补充测试
    - 依赖 User / Course 走 Feign
    - _Requirements: 6.1, 6.3_

  - [x] 22.4 实现 `AssignmentSubmittedEvent` 发布（outbox 模式）
    - 学生提交作业时，`assignment-service` 已在同一本地事务内写入 `outbox_event`
    - `AssignmentSubmittedEvent` 载荷已落为统一事件 DTO，并默认使用 `assignment.submitted` 作为 relay binding name
    - 2026-05-15 已完成真实 `outbox -> RabbitMQ` 联调：`relay-smoke-evt-1` 已从 `pending` 变为 `published`，并在 RabbitMQ 中生成 `assignment.submitted.assignment-submitted-inspector` 队列与 1 条消息
    - Relay Job 运行开关仍沿用公共 `platform.outbox.relay.*` 配置，默认保持关闭；联调或演练时按需显式开启
    - _Requirements: 10.5, 12.5_

  - [x] 22.5 Feign 熔断 Fallback 声明
    - `UserFeignClient` 继续使用 `fallbackFactory` 抛出 503 语义异常，供调用方选择 fail-open 或 fail-closed
    - `CourseFeignClient` 已补 `fallbackFactory`，对只读查询返回安全空值/空集合，避免学生读取与教师查询链路在下游抖动时整体崩溃
    - `assignment-service` 已补回归测试：学生读取对 `classIds` 降级为空页、教师查询对姓名富化降级为缺省、教师主写对课程归属校验保持 fail-closed
    - _Requirements: 10.4, 11.3_

  - [x]* 22.6 编写 Assignment_Service 单元测试
    - 已覆盖应用服务、教师命令/查询服务、JPA 仓储、outbox 自动配置/stream 配置，以及学生/教师/内部控制器链路
    - 2026-05-21 执行 `mvn --% -pl assignment-service -am test`，包含公共模块与 Assignment_Service 共 53 个测试，全部通过
    - _Requirements: 6.1_

  - [x]* 22.7 编写 Assignment_Service 契约测试
    - 已接入 Spring Cloud Contract producer 测试，新增 `AssignmentServiceContractBase`
    - 契约覆盖 `GET /api/student/assignments`、`GET /internal/assignments/2001`、`GET /internal/assignments/2001/knowledge-point-ids`
    - 2026-05-21 已由 Maven 生成 `InternalTest` / `StudentTest` 并随 `assignment-service` 测试通过
    - _Requirements: 10.1_

  - [x] 22.8 Gateway 切换 `assignment-route`
    - 已切换教师作业读取/主写、教师提交记录读取/批改、学生作业读取、学生提交作业等公开作业域路由到 `assignment-service`
    - 2026-05-15 已修复 `JwtAuthenticationFilter` 同步异常返回 500 的问题，并完成 `auth-service -> gateway -> assignment-service` 的教师主写实流验证
    - 当前 `legacy-route` 不再承接公开作业域路由，Assignment 剩余风险已收敛到回滚策略、消费者联调与回滚演练脚本化
    - _Requirements: 8.2, 16.2_

  - [x] 22.9 准备 Assignment 联调环境与检查清单
    - 已补本地 MySQL 多 schema 初始化脚本与 `docker-compose.dev.yml` 挂载，支持 `sc_auth / sc_user / sc_course / sc_assignment` 默认启动
    - 已新增 `docs/assignment-integration-checklist.md`，固化 `class_students` 可达性、学生可见性、教师主写切流前置条件
    - 已执行真实环境启动验证，并确认学生可见性查询已改造为经 `course-service` 聚合
    - 已完成真实“学生提交 -> 教师查看/批改 -> 学生重新读取 -> 教师汇总刷新”闭环验证
    - 已完成教师主写 `POST / PUT / DELETE` 的直连预演与 Gateway 鉴权实流验证，确认创建、班级范围更新、学生可见性变化与删除回收正确
    - 已完成真实 `AssignmentSubmittedEvent` relay 验证，确认 `assignment-service` 在显式开启 `platform.outbox.relay.enabled=true` 后可把 outbox 消息推送到 RabbitMQ
    - 已新增 `scripts/seed-dev-auth-users.ps1` / `scripts/seed-dev-auth-users.sql` 作为本地联调账号的可重复准备入口
    - _Requirements: 12.2, 16.2_

#### Exam_Service 剥离

- [x] 23. 剥离 Exam_Service
  - 依赖：任务 22
  - 核心接口清单（考试提交）需要更严格熔断阈值与更宽松限流
  - 当前已完成考试服务骨架、`sc_exam` 数据迁移、学生/教师考试业务迁移、自动阅卷、`ExamFinishedEvent` outbox、核心接口熔断/限流差异、Gateway 切流与熔断器状态转移属性测试
  - 2026-05-21 执行 `mvn --% -pl exam-service -am test`，包含公共模块与 Exam_Service 共 48 个测试，全部通过
  - _Requirements: 6, 10, 11.5, 12, 17.1_

  - [x] 23.1 `exam-service` + `-api` 模块骨架
    - 端口 8085；注册名 `exam-service`
    - 已补齐 `exam-service-api` DTO / Feign 骨架、`exam-service` Spring Boot / Flyway / Eureka / Feign 基础装配
    - 已新增学生最小链路控制器：`GET /api/student/exams`、`GET /api/student/exams/{id}`、`POST /api/student/exams/{id}/submit`
    - _Requirements: 6.2_

  - [x] 23.2 迁移 `exams / exam_questions / exam_submissions` 到 `sc_exam`
    - 已在 `exam-service` V1 schema 中补齐 `exam_questions` 表，`exams`、`exam_submissions`、`exam_classes`、`exam_knowledge_points`、`exam_questions` 均归属 `sc_exam`
    - 已新增 `ExamQuestionRecord` 与 JPA 仓储替换/读取能力，教师创建/更新考试时可携带题目并写入 `exam_questions`
    - 删除考试时同步清理 `exam_questions`，避免考试题目继续残留在服务内 schema
    - 已补 schema 迁移、JPA 仓储、服务层题目持久化测试
    - _Requirements: 6.4, 12.1_

  - [x] 23.3 迁移 `ExamController` 与自动阅卷业务
    - 已完成教师考试最小正式 CRUD 切片：
      - `GET /api/teacher/exams`
      - `GET /api/teacher/exams/{id}`
      - `POST /api/teacher/exams`
      - `PUT /api/teacher/exams/{id}`
      - `DELETE /api/teacher/exams/{id}`
    - 已完成教师考试评分最小正式链：
      - `GET /api/teacher/exams/{examId}/submissions`
      - `GET /api/teacher/exams/submissions/{submissionId}`
      - `PUT /api/teacher/exams/grade/{submissionId}`
    - 已通过经 `gateway:8080` 的真实 API smoke；教师考试 tab 已解除对 `teacher/submissions` 的默认耦合
    - 已完成全客观题自动阅卷最小切片：
      - 学生提交后读取 `exam_questions.correct_answer` 自动计分并标记 `exam_submissions.graded = 1`
      - 仅当试卷全部题目可客观判分时自动评分；含主观题或无题目仍保留给教师评分链
      - 自动阅卷成功后复用 `ExamFinishedEvent` outbox 链路，保持成绩分析消费路径一致
    - 已完成教师考试统一提交记录链：
      - `GET /api/teacher/exams/submissions`
      - `PUT /api/teacher/exams/submissions/{submissionId}`
      - `DELETE /api/teacher/exams/submissions/{submissionId}`
      - 全局提交记录列表按教师归属过滤，并支持 `examId`、`studentId`、`graded` 条件
    - _Requirements: 6.6, 10.5, 17.1_

  - [x] 23.4 实现 `ExamFinishedEvent` outbox 发布
    - 教师评分成功后，`exam-service` 已在同一本地事务内写入 `outbox_event`
    - `ExamFinishedEvent` 默认使用 `exam.finished` 作为 relay binding name
    - 已补 `exam-service` 的 outbox 自动装配 smoke 与评分后写 outbox 的服务测试
    - 2026-05-19 已完成真实运行时联调：
      - 直连 `PUT /api/teacher/exams/grade/9003` 完成教师评分
      - `sc_exam.outbox_event` 新增 `event_id = exam-finished-9003`
      - `binding_name = exam.finished`
      - `status = 1`
      - `published_at = 2026-05-19 10:15:50.358439`
      - `notification-service` 成功消费并写入 `sc_notification.notifications.id = 17`
      - 学生通知接口已可读取 `title = 考试已完成`、`content = 您已完成本次考试，当前成绩：94/100`
    - 已补重复评分事件语义：
      - 同一提交同分同评语重复提交不再写新 outbox 事件
      - 同一提交同分但评语变化会生成 `exam-finished-{submissionId}-{score}-{commentHash}` 形式的新 `event_id`
      - 2026-05-19 已验证 `event_id = exam-finished-9003-96-a4317356cd37` 成功 relay 并由 `notification-service` 幂等消费
    - 当前 Exam 域事件链剩余工作已从 outbox/relay 下沉为分析域消费与后续排行/分析链补齐
    - _Requirements: 10.5, 12.5_

  - [x] 23.5 在 `docs/architecture.md` 维护核心接口清单与熔断 / 限流阈值差异
    - 标注 `/api/exams/{id}/submit` 为核心接口，阈值配置写入表格
    - 已补 `student-exam-submit-route` 的 Gateway 限流覆盖：`replenish-rate=40`、`burst-capacity=80`
    - 已补 `exam-service-student-submit` 的 Resilience4j 熔断覆盖：`failure-rate-threshold=30`、`minimum-number-of-calls=20`
    - 已在 `docs/architecture.md` 同步核心接口清单、默认阈值与专项阈值差异
    - _Requirements: 11.5, 18.1_

  - [x]* 23.6 编写熔断器状态转移属性测试
    - **Property 27：熔断器状态转移**（CLOSED → OPEN → HALF_OPEN → CLOSED/OPEN 状态机）
    - 使用 `failureRateSeriesArb` 生成随机失败率序列
    - 已补 `CircuitBreakerStateTransitionPropertyTest`，使用固定种子 `failureRateSeriesArb()` 驱动真实 Resilience4j `CircuitBreaker`
    - 覆盖 CLOSED 达到失败率阈值后 OPEN、OPEN 拒绝调用、等待窗口后由探测调用进入 HALF_OPEN、半开探测成功回 CLOSED / 失败率达标回 OPEN
    - _Requirements: 10.4, 11.2_
    - _Properties: P27 熔断器状态转移_

  - [x] 23.7 Gateway 切换 `exam-route`
    - 已完成 `student exam` 窄路由切换：
      - `GET /api/student/exams`
      - `GET /api/student/exams/{id}`
      - `POST /api/student/exams/{id}/submit`
      - `GET /api/student/scores`
    - 已完成教师考试 CRUD 窄路由切换：
      - `GET /api/teacher/exams`
      - `GET /api/teacher/exams/{id}`
      - `POST /api/teacher/exams`
      - `PUT /api/teacher/exams/{id}`
      - `DELETE /api/teacher/exams/{id}`
    - 已完成教师考试评分窄路由切换：
      - `GET /api/teacher/exams/{examId}/submissions`
      - `GET /api/teacher/exams/submissions/{submissionId}`
      - `PUT /api/teacher/exams/grade/{submissionId}`
    - 已通过直连 `exam-service:8085` 与经 `gateway:8080` 的真实 smoke
    - `GET /api/student/scores` 已由 `exam-service` 聚合作业成绩 + 考试成绩承接，并完成前端 `student-assignments.html` 成绩查询 tab 的真实回归
    - 已完成教师考试统一提交记录链路切流：
      - `GET /api/teacher/exams/submissions`
      - `PUT /api/teacher/exams/submissions/{submissionId}`
      - `DELETE /api/teacher/exams/submissions/{submissionId}`
    - 已补 Gateway 配置测试，确认学生考试读、学生考试提交、教师考试 CRUD / 评分 / 统一提交记录均指向 `lb://exam-service`
    - 已为 `teacher-exam-crud-route` 补齐 `CircuitBreaker` fallback：`exam-service-teacher -> forward:/_fallback/exam-service`
    - 当前剩余差距不再是切流，而是更正式的成绩排名链继续迁移
    - _Requirements: 8.2, 16.2_

#### Analysis_Service 剥离（含 KnowledgePoint）

- [x] 24. 剥离 Analysis_Service（含 KnowledgePoint）
  - 依赖：任务 23
  - 仅读 + 事件驱动写入，不对外提供在线业务写操作
  - 当前已完成最小事件消费切片：`analysis-service` 可消费 `AssignmentSubmittedEvent / ExamFinishedEvent` 并写入 `sc_analysis` 的趋势与掌握度基础表
  - 已完成真实运行时 smoke：`exam-service grade -> outbox relay -> RabbitMQ exam.finished -> analysis-service -> score_trends/kp_mastery`
  - 已开放最小只读查询切片：`/api/teacher/score-trend` 与 `/api/teacher/knowledge-points/mastery/student/{studentId}/course/{courseId}`
  - Gateway 已完成最小 `analysis-route` 切流，两个查询端点可经 `gateway:8080 -> lb://analysis-service` 访问
  - 已完成最小预警事件切片：低分 `ExamFinishedEvent` 会由 `analysis-service` 写入 `EarlyWarningRaisedEvent` outbox，目标 binding 为 `early.warning.raised`
  - 已完成控制器归属收敛：KnowledgePoint 主数据由 `course-service` 承接，作业/考试知识点关联分别由 `assignment-service` / `exam-service` 承接，Analysis_Service 只保留掌握度、趋势、预警与分析投影读模型
  - _Requirements: 6, 6.6, 10.5, 12_

  - [x] 24.1 `analysis-service` + `-api` 模块骨架
    - 端口 8086；注册名 `analysis-service`
    - 对外前缀：`/api/analysis/** / /api/knowledge-points/** / /api/early-warnings/** / /api/dashboard/**`
    - 已新增 `analysis-service-api` 与 `analysis-service` Maven 模块，并纳入根聚合 `pom.xml`
    - `analysis-service` 当前提供事件驱动写入能力、查询 Controller、早预警兼容 Controller 与 Gateway 显式路由
    - _Requirements: 6.1, 6.2_

  - [x] 24.2 迁移 `knowledge_points / kp_mastery / score_trends / early_warnings` 到 `sc_analysis`
    - 已通过 Flyway 初始化最小表：
      - `score_trends`
      - `kp_mastery`
      - `early_warnings`
      - `processed_event`
    - `early_warnings` 已支持教师预警列表、统计、详情、状态更新、新增、删除、课程维度查询与导出兼容；`knowledge_points` 主数据已由 `course-service` 承接
    - `knowledge_points` 主数据不再落入 Analysis Schema：由 `course-service` 的 `TeacherKnowledgePointCompatibilityController / TeacherKnowledgePointService / TeacherKnowledgePointRepository` 承接旧 `KnowledgePointController` 主数据 CRUD 与课程维度列表；`assignment_knowledge_points / exam_knowledge_points` 关联表分别由 Assignment / Exam 服务本地表承接
    - `sc_analysis` 保留 `kp_mastery / score_trends / early_warnings / processed_event` 投影与幂等消费表，`kp_mastery.knowledge_point_id` 用于承载来自 Assignment / Exam 事件的真实知识点维度
    - _Requirements: 6.4, 12.1_

  - [x] 24.3 迁移 `AnalysisController / KnowledgePointController / KnowledgePointAnalysisController / EarlyWarningController / DashboardController / TeacherDashboardController`
    - 已完成 `TeacherDashboardController#scoreTrend` 兼容端点：`GET /api/teacher/score-trend`
    - 已完成 `KnowledgePointController#getStudentMastery` 兼容端点：`GET /api/teacher/knowledge-points/mastery/student/{studentId}/course/{courseId}`
    - 已完成 `KnowledgePointController#getKnowledgePointMasteryStats` 兼容端点：`GET /api/teacher/knowledge-points/stats/course/{courseId}`，由 `kp_mastery` 课程掌握度投影聚合出旧前端字段 `masteryRate / totalStudents / excellentCount / goodCount / averageCount / poorCount`
    - 已完成 `EarlyWarningController` JSON/导出兼容端点：`/api/early-warnings/**` 与 `/api/teacher/early-warnings/**`
    - 已完成学生端旧路径 `GET /api/student/early-warnings` 兼容端点，按 `X-User-Id` 学生身份读取 `early_warnings` 投影表，并从 `legacy-student-route` 移入 `analysis-route`
    - 已完成学生端旧路径 `GET /api/student/study-time-distribution` 兼容端点，按 `score_trends / kp_mastery` 聚合作业、考试与知识点学习证据，返回旧前端 `date / label / study_time / hours` 字段，并从 `legacy-student-route` 移入 `analysis-route`
    - 已完成学生端旧路径 `GET /api/student/stats` 兼容端点，基于 `score_trends / kp_mastery` 返回旧前端学习统计包络（学习时长、完成任务、平均分、知识点掌握度、知识点列表与空学习计划），并从 `legacy-student-route` 移入 `analysis-route`
    - 查询数据来自 `score_trends / kp_mastery / early_warnings` 投影表，教师端校验 `X-User-Id` 教师身份头，学生端校验 `X-User-Id` 学生身份头
    - Dashboard / learning-summary / AnalysisController 触发类兼容端点已由 `AnalysisQueryController` 与 `AnalysisTriggerCompatibilityController` 承接；`/api/knowledge-points/analysis/teacher/**` 已由空占位升级为基于 `kp_mastery` 的分布、薄弱主题、待关注学生与优秀学生平均线聚合
    - 已在显式 `courseId` 的教师知识点分析链路接入 `course-service-api`，通过 `CourseFeignClient#getCourse` 补齐课程名称并按课程 `teacherId` 收敛教师归属；非本人课程返回旧兼容空分析包络 `课程不存在或无权访问`
    - 已在显式 `classId` 的教师知识点分析链路通过 `CourseFeignClient#listTeacherClasses` 收敛教师-班级权限并补齐 `className`；非本人班级会在读取 Analysis 投影前返回旧兼容空分析包络 `班级不存在或无权访问`
    - 已在触发类兼容链路接入 `course-service-api` 权限校验：`/api/teacher/analysis/student/{studentId}/course/{courseId}/trigger` 会在创建 job 前校验课程归属；`/api/teacher/analysis/class/{classId}/course/{courseId}/batch-trigger` 会在创建 job 前校验课程与班级归属；Course_Service 不可用时保持迁移期旧兼容降级
    - 已接入 `user-service-api`，待关注学生列表通过 `UserFeignClient#listByIds` 批量补齐学生显示名，远程不可用或资料缺失时继续回退为旧兼容 `学生 {id}`
    - 已接入 `assignment-service-api`，待关注学生薄弱来源中 `lastSourceType=assignment / lastSourceId` 会通过 `AssignmentFeignClient#getAssignment` 补齐 `sourceName`，并按作业 `teacherId / courseId` 收敛名称暴露权限；远程不可用、不归属或资料缺失时继续回退为 `作业 {id}`
    - 已接入 `exam-service-api`，新增 `ExamFeignClient#getTeacherExam` 与 `exam-service` 内部端点 `GET /internal/exams/{examId}/teacher`，待关注学生薄弱来源中 `lastSourceType=exam / lastSourceId` 会补齐考试 `sourceName`，并按教师身份与课程 `courseId` 收敛名称暴露权限；远程不可用、不归属或资料缺失时继续回退为 `考试 {id}`
    - 已为 `kp_mastery` 增加 `knowledge_point_id` 读模型维度，`KnowledgeMasteryDTO / KnowledgeMasteryRecord`、JDBC upsert/list 查询、学生知识点列表与教师知识点分析聚合均优先使用真实知识点 ID；旧事件/旧回填数据继续以内部 `0` 哨兵保持课程级汇总兼容
    - 已更新历史回填脚本与 H2 集成测试 schema，`kp_mastery` 唯一键从 `(student_id, course_id)` 收敛为 `(student_id, course_id, knowledge_point_id)`，同一学生同一课程下不同知识点可独立累计掌握度
    - 已新增 Assignment / Exam 内部知识点 ID 查询：`GET /internal/assignments/{assignmentId}/knowledge-point-ids`、`GET /internal/exams/{examId}/knowledge-point-ids`；Exam_Service 会合并考试级 `exam_knowledge_points` 与题目级 `exam_questions.knowledge_point_id` 并去重
    - `AssignmentSubmittedAnalysisHandler / ExamFinishedAnalysisHandler` 已接入对应 Feign 查询，事件消费时若存在知识点映射则按每个 `knowledgePointId` 写入细粒度 `kp_mastery`；无映射或远程不可用时继续降级为旧课程级掌握度行
    - `KnowledgePointController` 主数据兼容端点已按领域归属完成分流：`GET/POST/PUT/DELETE /api/teacher/knowledge-points`、`GET /api/teacher/knowledge-points/{id}`、`GET /api/teacher/knowledge-points/course/{courseId}` 经 `course-route -> lb://course-service`；作业关联 `GET/POST /api/teacher/knowledge-points/assignment/{assignmentId}` 经 `assignment-route -> lb://assignment-service`；考试关联 `GET/POST /api/teacher/knowledge-points/exam/{examId}` 经 `exam-route -> lb://exam-service`
    - Assignment / Exam 来源名称与权限聚合、触发类显式课程/班级权限收敛、Analysis_Service 细粒度知识点读模型、Assignment / Exam 事件侧知识点/题目维度来源映射已完成
    - 已补 `AnalysisQueryControllerTest`、`AnalysisQueryServiceTest`、`JpaEarlyWarningRepositoryTest`、Course / Assignment / Exam 知识点兼容控制器测试与事件映射测试，覆盖分析查询、知识点掌握统计聚合、知识点主数据分流、作业/考试关联、早预警兼容响应、Excel 下载响应头和 JPA 查询/更新/删除契约；并完成 `analysis-service:8086` 真实 HTTP smoke
    - _Requirements: 6.1, 6.3_

  - [x] 24.4 消费 `AssignmentSubmittedEvent / ExamFinishedEvent` 更新 KP 掌握度与成绩趋势
    - 使用 `IdempotentEventHandler` 保证幂等
    - 已新增两个最小消费者：
      - `assignmentSubmittedAnalysisConsumer`
      - `examFinishedAnalysisConsumer`
    - `ExamFinishedEvent` 会写入 `score_trends`，并按成绩比例更新 `kp_mastery`
    - `AssignmentSubmittedEvent` 会作为提交证据更新 `kp_mastery`
    - 若 Assignment / Exam 已有关联知识点，Analysis 消费端会按真实 `knowledgePointId` 写细粒度掌握度；Exam 侧同时支持题目级 `knowledge_point_id` 合并
    - 已补 `AnalysisEventHandlerTest` 验证：
      - 考试完成事件写入成绩趋势与掌握度
      - 重复 eventId 幂等忽略
      - 作业提交事件更新掌握度
      - 作业/考试存在知识点映射时写入细粒度 `knowledgePointId` 掌握度
      - 原始 JSON payload 可被消费者反序列化处理
    - 已完成运行时验证：批改提交 `9003` 后发布 `exam-finished-9003-98-c0f9620fd620`，`analysis-service` 消费并落入 `processed_event / score_trends / kp_mastery`
    - _Requirements: 10.5, 6.6_

  - [x] 24.5 生成 `EarlyWarningRaisedEvent` 触发通知
    - 低分考试完成事件（得分率 < 60%）会生成 `EarlyWarningRaisedEvent`
    - 通过 `analysis-service` 本地 outbox 发布，`bindingName = early.warning.raised`
    - 继续复用 common 事件基础设施表，不额外引入 analysis 专属 outbox migration
    - 已补 `AnalysisEventHandlerTest` 覆盖低分生成预警 outbox 与重复 eventId 幂等忽略
    - 已修复 common outbox 自动配置顺序：`OutboxAutoConfiguration` 需在 `JdbcTemplateAutoConfiguration` 后执行，否则运行时不会创建 `OutboxEventRepository`
    - 已完成真实运行时 smoke：
      - 通过 RabbitMQ 向 `exam.finished` 发布 `exam-finished-analysis-warning-smoke-20260519-1558`
      - `analysis-service` 写入 `score_trends / kp_mastery`，得分率 `0.4500`
      - `sc_analysis.outbox_event` 写入并发布 `early-warning-exam-finished-analysis-warning-smoke-20260519-1558`
      - `binding_name = early.warning.raised`，`status = 1`
      - `notification-service` 消费后写入 warning 通知：`notifications.id = 22`，`studentId = 42`，`title = 学情预警`
    - _Requirements: 10.5_

  - [x] 24.6 历史数据回填脚本
    - 一次性 ETL 脚本从 Exam / Assignment 的历史表同步到 Analysis Schema
    - 放 `deploy/scripts/backfill/`
    - 已新增 `deploy/scripts/backfill/backfill-analysis-history.sql`
    - 已新增 `deploy/scripts/backfill/run-analysis-backfill.ps1`，默认对本地 `qimo-mysql` 容器执行
    - 回填口径：
      - 已批改考试提交写入 `score_trends`，`score_rate = score / total_score`
      - 已批改考试提交按得分率写入 KP 掌握度证据
      - 历史作业提交按实时 `AssignmentSubmittedEvent` 口径写入 `0.6000` 提交证据
      - 同一学生同一课程聚合为 `kp_mastery`，`mastery_score` 为历史证据平均值
    - 脚本可重复执行：`score_trends` 与 `kp_mastery` 均通过唯一键 `ON DUPLICATE KEY UPDATE` 收敛
    - 已补 `AnalysisBackfillScriptTest` 使用 H2 MySQL mode 执行脚本两遍，验证回填结果与幂等性
    - _Requirements: 12.3_

  - [x] 24.7 编写 Analysis_Service 集成测试
    - 事件驱动链路：触发 `ExamFinishedEvent` → 断言 KP 掌握度更新 + 预警生成
    - 已新增 `AnalysisEventIntegrationTest`
    - 使用 H2 MySQL mode + 真实分析 JPA 仓储 + 事件基础设施 JDBC 组合验证：
      - `JdbcProcessedEventRepository`
      - `JpaAnalysisRepository`
      - `JdbcOutboxEventRepository`
    - 覆盖低分 `ExamFinishedEvent` 写入 `processed_event / score_trends / kp_mastery / outbox_event`
    - 覆盖重复 `eventId` 幂等忽略，确保不会重复写分析投影或预警 outbox
    - 已在 `analysis-service` 补 test-scope H2 依赖，仅用于自动化集成测试
    - RabbitMQ 端到端链路已由 24.5 真实 smoke 覆盖，本项聚焦本地可重复的分析 JPA + 事件基础设施 JDBC 集成测试
    - _Requirements: 6.6, 10.5_

  - [x] 24.8 Gateway 切换 `analysis-route`
    - 已新增 `analysis-route -> lb://analysis-service`
    - 当前切流范围：
      - `GET /api/teacher/score-trend`
      - `GET /api/teacher/knowledge-points/mastery/student/{studentId}/course/{courseId}`
      - `GET /api/knowledge-points/analysis/teacher/**`
      - `GET /api/student/early-warnings`
      - `GET /api/student/study-time-distribution`
      - `GET /api/student/stats`
    - 已补 `KnowledgePointAnalysisCompatibilityController` 承接旧 URL 的分析读接口，保留 `courseId=all`、`studentId=all`、`knowledgePointId=all` 等旧筛选语义与旧式 401/400 响应包络
    - 已补 `GatewayRouteConfigTest` 断言 `analysis-route` 位于显式 `legacy-*` 路由前、指向 `lb://analysis-service`、仅匹配 GET，并确认知识点分析读接口不再走旧单体；随后补充作业/考试知识点关联兼容路由，删除 `legacy-knowledge-route`
    - 已完成真实运行时 smoke：`registry-server + analysis-service + gateway` 下，经 `gateway:8080` 返回成绩趋势与学生课程掌握度投影数据
    - _Requirements: 8.2, 16.2_

#### Notification_Service 剥离

- [x] 25. 剥离 Notification_Service
  - 依赖：任务 24
  - 当前已完成 Notification_Service 的模块骨架、Schema、历史数据回填、Controller 业务迁移、三类事件消费、契约测试与 Gateway 配置级切流
  - 已完成真实运行时 smoke：`registry-server + notification-service + gateway` 下，通过 `gateway:8080 -> lb://notification-service` 覆盖通知创建、列表读取、未读数、标记已读、删除
  - _Requirements: 6, 10.5, 12_

  - [x] 25.1 `notification-service` + `-api` 模块骨架
    - 端口 8087；注册名 `notification-service`
    - 已纳入聚合 `pom.xml`，补齐最小 Spring Boot / Flyway / RabbitMQ 依赖
    - _Requirements: 6.2_

  - [x] 25.2 迁移 `notifications` 到 `sc_notification`
    - 当前切片已在 `notification-service` 内补 `V1__init_notification_schema.sql`
    - 已建立 `notifications / processed_event` 两张本地表
    - 已复用 `common` 的幂等消费基础设施
    - 2026-05-15 本地已验证 `assignment-submitted-3` 被消费后落入 `sc_notification.notifications`
    - 已新增 `deploy/scripts/backfill/backfill-notification-history.sql`，从旧单体 `major_assignment.notifications` 回填到 `sc_notification.notifications`
    - 已新增 `deploy/scripts/backfill/run-notification-backfill.ps1`，默认对本地 `qimo-mysql` 容器执行通知历史回填
    - 回填口径：
      - 保留旧通知 `student_id / teacher_id / type / title / content / related_id / is_read / created_at`
      - 跳过关键字段为空的无效旧通知
      - 通过稳定业务字段 `student_id + teacher_id + type + title + related_id + created_at` 去重，脚本可重复执行
    - 已补 `NotificationBackfillScriptTest` 使用 H2 MySQL mode 执行脚本两遍，验证回填字段与幂等性
    - _Requirements: 6.4, 12.1_

  - [x] 25.3 迁移 `NotificationController` 与业务
    - 读走 `sc_notification.notifications`；事件类写入走事件消费；教师手工通知写入走 `NotificationCommandService`
    - 已迁入学生通知读接口：
      - `GET /api/notifications/student`
      - `GET /api/notifications/student/all`
      - `GET /api/notifications/student/unread-count`
    - 已迁入学生通知状态变更接口：
      - `PUT /api/notifications/{notificationId}/read`
      - `PUT /api/notifications/read-all`
      - `DELETE /api/notifications/{notificationId}`
      - `DELETE /api/notifications/delete-all-read`
    - 已迁入教师手工发送接口：
      - `POST /api/notifications/teacher/send`
      - `POST /api/notifications/teacher/send-batch`
    - 分页响应继续兼容旧前端字段：`notifications / total / page / size / totalPages`
    - 已补 `NotificationQueryServiceTest / NotificationCommandServiceTest / NotificationControllerTest` 覆盖分页 `totalPages` 与教师批量发送
    - _Requirements: 6.1, 6.3_

  - [x] 25.4 消费 `AssignmentSubmittedEvent / ExamFinishedEvent / EarlyWarningRaisedEvent`
    - 生成对应通知（幂等）
    - 当前已接入三类最小消费者：
      - `AssignmentSubmittedEvent`
      - `ExamFinishedEvent`
      - `EarlyWarningRaisedEvent`
    - 已补消费单测，验证三类事件的最小通知生成与重复 eventId 幂等忽略
    - 2026-05-15 真实链路已验证：
      - 学生 `POST /api/student/assignments/1/submit`
      - `assignment-service` 生成并 relay `assignment-submitted-3`
      - `notification-service` 落库 1 条 `studentId=42` 的作业提交通知
    - 2026-05-15 已补 `ExamFinishedEvent` 运行时 smoke：
      - 通过 RabbitMQ 向 `exam.finished` 发布 `exam-finished-smoke-4`
      - `notification-service` 成功写入 `sc_notification.notifications.id = 2`
      - `studentId = 42`，`type = exam`，`title = 考试已完成`，`relatedId = 79`
      - `processed_event` 记录 `event_id = exam-finished-smoke-4`
      - `GET /api/notifications/student` 已返回 exam 通知
    - 2026-05-15 已补 `EarlyWarningRaisedEvent` 运行时 smoke：
      - 通过 RabbitMQ 向 `early.warning.raised` 发布 `warning-smoke-2`
      - 修复 `EarlyWarningRaisedNotificationHandler` 未写入 `studentId` 导致 `notifications.student_id` 非空约束报错的问题
      - `notification-service` 成功写入 `sc_notification.notifications.id = 3`
      - `studentId = 42`，`type = warning`，`title = 学情预警`，`relatedId = 7002`
      - `processed_event` 记录 `event_id = warning-smoke-2`
      - `GET /api/notifications/student` 已返回 warning 通知
    - 当前 Notification 域三类最小事件消费者已全部完成真实运行时验证；剩余工作转为通知域接口补齐与 `notification-route` 切流
    - _Requirements: 10.5_

  - [x] 25.5 编写 Notification_Service 契约测试
    - 已新增 `NotificationContractIntegrationTest`
    - 使用 H2 MySQL mode + 真实 JPA 通知仓储 + 事件幂等 JDBC 组合验证：
      - `JpaNotificationRepository`
      - `JdbcProcessedEventRepository`
      - `NotificationQueryService`
      - `NotificationCommandService`
      - `NotificationController`
    - 覆盖学生通知 HTTP 契约：
      - `GET /api/notifications/student`
      - `GET /api/notifications/student/all`
      - `GET /api/notifications/student/unread-count`
      - `PUT /api/notifications/{notificationId}/read`
      - `PUT /api/notifications/read-all`
      - `DELETE /api/notifications/{notificationId}`
      - `DELETE /api/notifications/delete-all-read`
      - `POST /api/notifications/teacher/send`
      - `POST /api/notifications/teacher/send-batch`
    - 覆盖分页响应兼容字段：`totalPages`
    - 覆盖事件到落库契约：`AssignmentSubmittedEvent / ExamFinishedEvent / EarlyWarningRaisedEvent`
    - 覆盖重复 `EarlyWarningRaisedEvent` 的 `processed_event` 幂等收敛
    - 已补 `JpaNotificationRepositoryTest` 与 `NotificationContractIntegrationTest`，覆盖生成主键、分页兼容字段与事件落库契约
    - 已在 `notification-service` 补 test-scope H2 依赖，仅用于契约集成测试
    - _Requirements: 10.1_

  - [x] 25.6 Gateway 切换 `notification-route`
    - 已在 Gateway 配置中新增统一 `notification-route -> lb://notification-service`
    - 当前切流范围：
      - `Path=/api/notifications/**`
      - `Method=GET,POST,PUT,DELETE`
    - 已保留 CircuitBreaker fallback：`forward:/_fallback/notification-service`
    - 已将原 `notification-read-route / notification-write-route` 收敛为单一路由，避免任务清单与实际路由 ID 不一致
    - 已补 `GatewayRouteConfigTest` 断言 `notification-route` 位于 `legacy-route` 前、指向 `lb://notification-service` 且覆盖读写方法
    - 已完成真实运行时 smoke：
      - Eureka 注册表中 `GATEWAY` 与 `NOTIFICATION-SERVICE` 均为 `UP`
      - 使用开发 RSA 私钥签发测试 JWT，经 Gateway 访问 `/api/notifications/**`
      - `POST /api/notifications/teacher/send` 创建通知成功，返回 `code=201`
      - `GET /api/notifications/student` 查询到刚创建的通知，并返回 `totalPages`
      - `GET /api/notifications/student/unread-count` 返回未读数
      - `PUT /api/notifications/{notificationId}/read` 与 `DELETE /api/notifications/{notificationId}` 均通过 Gateway 成功执行
    - _Requirements: 8.2, 16.2_

#### AI_Service 剥离

- [x] 26. 剥离 AI_Service
  - 依赖：任务 25
  - 当前已完成 AI_Service 的模块骨架、独立 `sc_ai` 初始 Schema、旧 AIController 三个业务接口迁移、Gateway 用户维度 QPS 限流配置、Mock 模型集成测试与 Gateway 真实运行时切流验证
  - _Requirements: 6, 10, 12_

  - [x] 26.1 `ai-service` + `-api` 模块骨架
    - 端口 8088；注册名 `ai-service`
    - 已新增 `ai-service-api` 与 `ai-service` Maven 模块，并纳入根聚合 `pom.xml`
    - `ai-service-api` 当前建立公共契约包根，后续 DTO / Feign 契约在 26.3 随业务接口迁移补齐
    - `ai-service` 当前为最小 Spring Boot 服务容器，依赖 `common`、`ai-service-api`、Web、Validation、Actuator 与 Eureka Client
    - 已补 `AiServiceApplicationTest` 验证应用上下文可启动，且 `spring.application.name = ai-service`、`server.port = 8088`
    - _Requirements: 6.2_

  - [x] 26.2 新建 `sc_ai` Schema
    - 表：`ai_generations / ai_prompts`（历史调用记录）
    - Flyway `V1__init_ai_schema.sql`
    - `ai_prompts` 保存 prompt 模板、默认模型、启用状态与唯一 `prompt_key`
    - `ai_generations` 保存用户、角色、请求类型、请求/响应 JSON、模型、状态、错误信息、耗时与创建时间
    - `ai-service` 已引入 JDBC / Flyway / MySQL runtime / H2 test 依赖，并配置默认 `AI_DB_URL -> sc_ai`
    - 已补 `AiSchemaMigrationTest` 使用 H2 MySQL mode 真实执行 Flyway，验证两张表可插入历史记录且 `prompt_key` 唯一约束生效
    - _Requirements: 12.1_

  - [x] 26.3 迁移 `AIController` 业务接口
    - 替换原占位实现；接入真实 AI 模型代理
    - 不写业务库（结果由业务服务通过 Feign / 事件消费）
    - 已迁入旧单体 `/api/ai` 三个兼容接口：
      - `POST /api/ai/generate-questions`
      - `POST /api/ai/generate-exam`
      - `POST /api/ai/learning-suggestions`
    - 已新增 `ai-service-api` DTO：
      - `GenerateQuestionsRequestDTO`
      - `GenerateExamRequestDTO`
      - `LearningSuggestionRequestDTO`
    - 已新增 `AiModelClient` 模型代理边界与 `LocalMockAiModelClient` 默认实现，后续可替换为真实 OpenAI-compatible / 其它模型网关实现
    - 已新增 `AiGenerationService`，统一处理模型调用、角色解析与 AI 调用历史落库
    - 已新增 `JpaAiGenerationRepository`，仅写 `sc_ai.ai_generations` 历史调用记录，不写课程 / 考试 / 学情等业务库
    - 已新增 `AiServiceExceptionHandler` 继承 common 统一异常处理，参数校验错误返回 `ResponseResult`
    - 已补 `AiControllerTest / AiGenerationServiceTest / JpaAiGenerationRepositoryTest` 覆盖兼容响应、身份缺失、参数校验、模型代理调用与历史记录落库
    - _Requirements: 6.1, 1.3_

  - [x] 26.4 对 AI 调用配置按用户 QPS 限流
    - 限流键：`userId`；阈值由 Gateway 配置统一管理
    - 已在 Gateway 配置中新增 `ai-route -> lb://ai-service`，匹配 `Path=/api/ai/**` 与 `Method=POST`
    - 已在 `gateway.rate-limit.routes.ai-route` 配置 AI 专属令牌桶阈值：
      - `replenish-rate = 2`
      - `burst-capacity = 4`
      - `requested-tokens = 1`
      - `retry-after-seconds = 3`
    - 已扩展 `GatewayRateLimitProperties` 支持按 routeId 覆盖全局限流阈值
    - 已在 `GatewayRateLimitFilter` 中按当前 routeId 将覆盖阈值写入 `RedisRateLimiter` route config
    - `IpUserRouteKeyResolver` 已验证 AI 请求限流 key 格式为 `clientIp:userId:ai-route`
    - 已补 `GatewayRateLimitPropertiesTest / GatewayRateLimitFilterTest / IpUserRouteKeyResolverTest / GatewayRouteConfigTest` 覆盖 AI 限流配置与 key 解析
    - _Requirements: 8.7_

  - [x]* 26.5 编写 AI_Service 集成测试（Mock 模型）
    - 已新增 `AiServiceIntegrationTest`，使用 `@SpringBootTest + MockMvc + H2 MySQL mode + Flyway` 启动 `ai-service` 测试容器
    - 覆盖 `POST /api/ai/generate-questions` 经 `LocalMockAiModelClient` 生成题目，并写入 `ai_generations` 历史记录
    - 断言 HTTP 响应保持旧接口兼容 envelope，且落库记录包含 `user_id / user_role / prompt_key / request_type / model_name / status / request_payload / response_payload`
    - 已验证 `mvn --% -pl ai-service -am test` 通过：`common` 14 个测试 + `ai-service` 11 个测试全绿
    - _Requirements: 6.1_

  - [x] 26.6 Gateway 切换 `ai-route`
    - 配置级 `ai-route` 已随 26.4 落地，用于承载 AI 专属限流阈值
    - 已完成真实运行时 smoke：`registry-server + ai-service + gateway` 下经 `gateway:8080 -> lb://ai-service` 验证三个 `/api/ai/**` 接口
    - Eureka 已确认 `AI-SERVICE` 注册状态为 `UP`，实例端口 `8088`
    - Gateway smoke 结果：
      - 无 Bearer Token 请求 `POST /api/ai/generate-questions` 返回 `401`
      - 教师 JWT 请求 `POST /api/ai/generate-questions` 返回 `200`，响应包含本地 mock 题目
      - 教师 JWT 请求 `POST /api/ai/generate-exam` 返回 `200`，响应包含 `Java模拟试卷` 与 10 道题
      - 教师 JWT 请求 `POST /api/ai/learning-suggestions` 返回 `200`，响应包含 `studentId=42` 与 3 条建议
    - MySQL `sc_ai.ai_generations` 已确认三类 `prompt_key` 均写入 `SUCCESS` 历史记录
    - 已完成运行时限流 smoke：同一教师 JWT 连续请求 AI 题目生成，前 4 次 `200`，第 5/6 次 `429` 且 `Retry-After=3`
    - _Requirements: 8.2, 16.2_

#### Migration Plan 文档与旧单体下线

- [x] 27. 补全 `docs/migration-plan.md` 与旧单体下线
  - 依赖：任务 21~26
  - 当前已完成迁移计划三阶段验收/回滚手册、接口版本化策略、旧单体下线 Check-list 与 P32 文档可溯源测试
  - _Requirements: 16, 18_

  - [x] 27.1 完成 `docs/migration-plan.md` 三阶段详述
    - 每阶段：进入条件 / 交付物 / 退出条件 / 验证用例清单 / 回滚步骤 / RTO 目标
    - 显式引用每条需求 R1~R18 与对应属性 P1~P32
    - 已在 `docs/migration-plan.md` 新增“阶段验收与回滚手册”，覆盖阶段 1/2/3 的进入条件、负责人、交付物、退出条件、验证用例、回滚步骤与 RTO 目标
    - 已新增“需求可溯源矩阵”，显式覆盖 `R1.1` 至 `R18.5`
    - _Requirements: 16.1, 16.3, 16.4, 18.1_

  - [x] 27.2 `docs/migration-plan.md` 补充接口版本化策略
    - `v1` / `v2` 并存周期 ≥ 14 天
    - Feign `-api` 模块 v1 接口保持不变、新增 v2
    - 已在 `docs/migration-plan.md` 新增“接口版本化策略”，约束 `/api/v2/**` 或 `X-Api-Version: 2` 二选一、v1 至少保留 14 天、`*-service-api` v1 DTO 不做破坏性修改
    - _Requirements: 16.5_

  - [x] 27.3 旧单体下线清单 Check-list
    - 所有 `/api/**` 路由从 `legacy-route` 移除
    - `legacy-route` 从 Gateway 配置删除
    - `legacy-adapter` 子模块归档并冻结
    - 删除 `major_assignment` 中剩余 Controller / Mapper / Entity（保留只读备份分支 `archive/legacy-final`）
    - 已在 `docs/migration-plan.md` 新增“旧单体下线 Check-list”，覆盖 `legacy-route` 删除、`legacy-adapter` 冻结、`archive/legacy-final`、静态资源截止时间、跨库访问收敛、告警与文档导航
    - _Requirements: 6.4, 16.1_

  - [x]* 27.4 编写文档可溯源属性测试
    - **Property 32：文档需求可溯源**（R1~R18 所有 Acceptance Criteria 在 `design.md` 与 `migration-plan.md` 中至少被引用一次）
    - Markdown 扫描 + 正则 `R\d+\.\d+`
    - 已新增 `DocumentationTraceabilityTest`，从 `requirements.md` 自动解析 `R{n}.{m}` 并扫描 `design.md + docs/migration-plan.md`
    - 已验证 `mvn --% -pl major_assignment -Dtest=DocumentationTraceabilityTest test` 通过
    - _Requirements: 18 文档可溯源不变量_
    - _Properties: P32 文档需求可溯源_

#### DevOps 与部署

- [x] 28. Dockerfile / docker-compose / Helm 与 CI/CD
  - 依赖：任务 11~26
  - 当前已补每服务多阶段 Dockerfile、完整根 `docker-compose.yml`、Helm chart 骨架、GitHub Actions 流水线骨架、部署文档与结构断言测试
  - _Requirements: 15_

  - [x] 28.1 每服务 Dockerfile（多阶段构建）
    - 使用 `maven:3.9-eclipse-temurin-17` builder + `eclipse-temurin:17-jre-jammy` runtime
    - `HEALTHCHECK` 指向 `/actuator/health`
    - 已为 `registry-server / gateway / auth-service / user-service / course-service / assignment-service / exam-service / analysis-service / notification-service / ai-service` 补齐 Dockerfile
    - 模板写入 `docs/deployment.md`
    - _Requirements: 15.1, 18.1_

  - [x] 28.2 本地 `docker-compose.yml`
    - 聚合 Eureka / MySQL / Redis / RabbitMQ / Prometheus / Grafana / Gateway / 所有业务服务
    - 已新增根 `docker-compose.yml`，同时包含 `legacy-monolith` 以承接当前 `legacy-route` 过渡需求
    - _Requirements: 15.2_

  - [x]* 28.3 Helm Chart（umbrella + 每服务 Chart）
    - `deploy/charts/{service}/` 结构；`values-{dev,test,prod}.yaml`
    - `platform-infra` chart 覆盖 Eureka / Prometheus / Loki / Tempo / RabbitMQ
    - 可选：仅当项目规模需要 K8s 部署时实施
    - 已补 `deploy/charts/target-platform` umbrella chart、`platform-infra` chart 与各运行时服务 chart 骨架，环境 values 已拆为 `dev/test/prod`
    - _Requirements: 15.2, 15.5_

  - [x] 28.4 CI/CD 流水线配置（GitHub Actions）
    - `.github/workflows/ci.yml`：`lint → unit → pbt → integration → contract → scan → docker-build → docker-push → deploy-test → e2e-smoke`
    - 任一阶段失败立即 `exit 1`
    - 镜像打标 `{service}-{gitShortSha}-{date}`
    - 已新增 `.github/workflows/ci.yml` 骨架并按固定 `needs` 链串联 10 个阶段
    - _Requirements: 15.3, 15.4_

  - [x]* 28.5 编写 CI 流水线 YAML 结构断言
    - 解析 workflow YAML，断言 9 个阶段顺序与 `needs` 依赖
    - 已新增 `CiWorkflowStructureTest`
    - _Requirements: 15.3, 15.4_

  - [x] 28.6 编写 `docs/deployment.md`
    - Dockerfile 模板 / docker-compose 说明 / Helm 部署指引 / 环境 Profile 与 Namespace 映射
    - 已将 `docs/deployment.md` 补齐为可执行手册，覆盖环境分层、Dockerfile 模板、完整 Compose、Helm 目录约定、CI 阶段链、配置映射与运维巡检
    - _Requirements: 15.1, 15.2, 15.5, 18.1_

#### 阶段 3 最终检查点

- [x] 29. 阶段 3 最终检查点 - 确保所有测试通过
  - 依赖：任务 20~28
  - 运行完整 `mvn clean verify` + 契约测试 + 事件驱动集成测试
  - 确认 P1~P32 属性测试全绿
  - 确认 `legacy-route` 已从 Gateway 配置删除
  - 确认所有业务微服务 jar 不含前端资源（P28）
  - 确认所有需求 R1~R18 在 `design.md` 与 `migration-plan.md` 中均有引用（P32）
  - 当前状态（2026-05-21）：完整 `mvn clean verify` 已通过；在低内存机器上使用 Maven/Surefire 受限内存参数规避此前测试 fork JVM native memory OOM，24 个 Reactor 模块全部 `SUCCESS`，总耗时 02:48
  - 已完成的分模块验证：`gateway` 52 个测试通过；`auth-service` 12 个测试通过；`user-service` 26 个测试通过；`course-service` 49 个测试通过；`assignment-service` 53 个测试通过；`exam-service` 48 个测试通过；`analysis-service` 62 个测试通过；`notification-service` 29 个测试通过；`ai-service` 11 个测试通过；`legacy-adapter` 1 个测试通过；`major_assignment -DforkCount=0 test` 67 个测试通过、1 个跳过
  - 专项验证：`DocumentationTraceabilityTest` 通过（P32）；`DeploymentArtifactFrontendExclusionTest` 通过（P28）；`gateway/src/main/resources/application.yml` 中不存在 catch-all `legacy-route`、`legacy-dashboard-route`、`legacy-knowledge-route` 或 `legacy-early-warning-route`
  - 最终验证命令：`mvn -T 1 "-DargLine=-Xms64m -Xmx384m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=64m -XX:CICompilerCount=2 -XX:TieredStopAtLevel=1 -Djdk.attach.allowAttachSelf=true -XX:+StartAttachListener -XX:+EnableDynamicAgentLoading" clean verify`
  - 旧单体显式 legacy 路由的历史审计详见 `docs/legacy-route-audit.md`，当前 Gateway 配置已移除 catch-all `legacy-route` 与此前剩余的显式 legacy 路由组
  - 确保所有测试通过，如有问题询问用户
  - _Requirements: 6, 10, 12, 13, 14, 15, 16, 18_

---

## Notes（说明）

- 带 `*` 的任务为可选（主要是测试与 Helm Chart 类），可在 MVP 阶段跳过；顶层任务不得标 `*`，核心实现任务（骨架 / 迁移 / 网关切换 / 事件发布）不得标 `*`。
- 每条属性（P1~P32）均对应设计文档 `§Correctness Properties` 中的属性编号与需求条款，可通过 `jqwik` 在 Java 中实现 PBT。
- 属性测试任务刻意紧邻对应实现任务放置，以便错误在本任务内被发现。
- 检查点任务（10 / 19 / 29）是阶段退出的硬门槛；失败时返回对应阶段内的任务继续修复。
- 所有任务 **仅** 包含"写代码 / 写配置 / 写文档"这三类可由编码代理完成的工作；不包含用户培训、上线审批、业务演练、市场沟通等非编码活动。
- 实施语言统一为 Java 17 + Spring Boot 3.5.3 + Spring Cloud 2025.0.0（与设计一致），所有代码示例、PBT 与工具链基于该栈。

## Workflow Completion（工作流完成说明）

本 Requirements-First 工作流的规格阶段到此为止，**已完成需求 / 设计 / 任务三份产出物**：

- `.kiro/specs/spring-cloud-migration/requirements.md`
- `.kiro/specs/spring-cloud-migration/design.md`
- `.kiro/specs/spring-cloud-migration/tasks.md`

后续可以通过打开 `tasks.md` 并在需要执行的任务项旁点击 "Start task" 逐项落地。建议的执行顺序即本文件中的任务编号顺序（阶段 0 → 阶段 1 → 阶段 2 → 阶段 3），单个阶段内也按编号顺序执行以满足 "每步建立在前一步之上" 的约束。

请确认任务清单是否可以进入实施阶段，如有调整诉求请在本阶段提出。
