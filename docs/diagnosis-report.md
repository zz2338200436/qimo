# 现状诊断报告

> 版本：v0.1
> 最后更新：2026-05-10
> 作者：架构组

## 1. 背景与目标

本报告针对单体 `major_assignment`（Spring Boot 3.5.3 + MyBatis + Redis + 自研多角色 Session）进行现状盘点，为后续 Spring Cloud 微服务化改造（见 `design.md`、`migration-plan.md`）建立"问题事实基线"。核心输出：

- 覆盖架构与分层、安全与认证、代码质量与可维护性、性能与数据访问、可观测性、前后端与部署共六个维度；
- 对 `.kiro/specs/spring-cloud-migration/requirements.md` R1.3 列出的 8 条已知问题逐条给出文件 / 行号级证据；
- 追加诊断过程中新发现的问题并标注"发现来源"（R1.4）；
- 为每条问题提供可执行的"验证手段"，与 `design.md` 的 Correctness Properties（P1~P32）对齐。

## 2. 诊断范围

| 范围 | 内容 |
| --- | --- |
| 代码树 | `major_assignment/src/main/java/**`、`major_assignment/src/main/resources/**` |
| 主要模块 | `config/`（Security、MultiRoleSession、Redis 缓存）、`aspect/`（认证 / 日志切面）、`controller/`（19 个 Controller）、`mapper/`（12 个 Mapper + 3 个 XML）、`resources/application.properties` |
| 外部依赖边界 | MySQL `major_assignment` 库、Redis（会话 / 缓存 / 验证码）、前端静态资源（随 jar 打包） |
| 不在范围 | `major_assignment/logs/**`、`.idea/**`、`.mvn/**`、前端构建产物更新历史 |

## 3. 问题清单

### 字段约定

每条记录固定六字段：**编号 / 描述 / 证据 / 影响等级 / 建议阶段 / 验证手段**；影响等级取值 `高 / 中 / 低`；建议阶段取值 `现状治理（阶段 1）` / `微服务化前置（阶段 2）` / `微服务化阶段（阶段 3）`。

### 3.1 架构与分层

#### D-06 `BaseController` 同时依赖 `HttpServletRequest` 与 `HttpSession`，登录态读取路径不一致

- **编号**：D-06
- **描述**：`BaseController` 提供了 `getCurrentUserId(HttpServletRequest)`、`getCurrentUserId(HttpSession)`、`isLoggedIn(HttpServletRequest)`、`isLoggedIn(HttpSession)` 等多条并存的读取入口；前者优先读 `request.getAttribute("SESSION_DATA")` 再回退 `HttpSession`，后者直接读 `session.getAttribute("userId")`，两条路径在会话同步失败时返回结果不一致，导致同一用户在不同 Controller 方法中被判定为"已登录 / 未登录"并存。
- **证据**：`major_assignment/src/main/java/com/_202510007517/major_assignment/controller/BaseController.java:L11-L97`（`getCurrentUserId(HttpServletRequest)` L13-L28、`getCurrentUserId(HttpSession)` L34-L43、`isLoggedIn(HttpServletRequest)` L65-L77、`isLoggedIn(HttpSession)` L82-L84）。下游例：`controller/AIController.java:L83-L92` 同时以 `HttpSession session` 形参调用 `isLoggedIn(session)` 与 `getCurrentUserId(session)`。
- **影响等级**：中
- **建议阶段**：现状治理（阶段 1）
- **验证手段**：属性测试 **P1 会话一致性不变量**（`design.md §Correctness Properties` P1）—— 对任意请求，`BaseController.getCurrentUserId(request)` / `getCurrentUserId(session)` / `SecurityContextHolder` 三处必须同时为空或同指一个 userId；并辅以 ArchUnit 规则：`BaseController` 的 `HttpSession` 入口方法必须标注 `@Deprecated`，非 `BaseController` 的类禁止调用 `session.getAttribute("userId")`。

### 3.2 安全与认证

#### D-01 `MultiRoleSessionManager` 与 Spring Security 双轨鉴权存在一致性风险

- **编号**：D-01
- **描述**：`MultiRoleSessionFilter` 在同一请求生命周期内，同时向 `request` 属性（`SESSION_DATA`）、`HttpSession`（`userId` / `roles` / `SPRING_SECURITY_CONTEXT`）、以及 `SecurityContextHolder` 写入三套鉴权数据；但读侧（`BaseController`、`AuthenticationAspect`、`LoggingAspect`、各 Controller）各自选取不同来源，且过滤器在"未解析到有效会话"分支下未清空 `SecurityContextHolder` / 请求属性，会出现被上一个请求线程遗留的 `Authentication` 污染，形成典型的双轨鉴权一致性缺陷。
- **证据**：
  - `major_assignment/src/main/java/com/_202510007517/major_assignment/config/MultiRoleSessionFilter.java:L48-L100`（同时写 request 属性 L58-L60、HttpSession L63-L66、Authorities/SecurityContext L69-L87、把 Security Context 再写入 Session L90，并在 `sessionId == null` 分支仅调用 `getSession(false)` 未清理上下文 L93-L97）；
  - `major_assignment/src/main/java/com/_202510007517/major_assignment/config/MultiRoleSessionManager.java:L21-L25`（三条独立 Cookie `JSESSIONID_TEACHER / _STUDENT / _ADMIN` 分别承载同一用户的会话）；
  - `major_assignment/src/main/java/com/_202510007517/major_assignment/aspect/AuthenticationAspect.java:L78-L106`（读侧优先 `SESSION_DATA`，失败回退 `HttpSession`，与 Filter 写入口径存在错配风险）。
- **影响等级**：高
- **建议阶段**：现状治理（阶段 1）
- **验证手段**：属性测试 **P1 会话一致性不变量** + **P2 会话 Filter 幂等** + **P3 无效会话不回退**（`design.md §Correctness Properties` P1/P2/P3）；集成测试断言：`MultiRoleSessionFilter` 在无效会话输入下 `SecurityContextHolder.getContext().getAuthentication()` 必须为 `null` 或匿名主体；并在 `finally` 中通过 MDC 断言无跨线程残留。

#### D-03 `application.properties` 明文存放数据库账号口令等敏感配置

- **编号**：D-03
- **描述**：`spring.datasource.username` / `spring.datasource.password` 以明文 `root / root` 直接提交至代码仓库；`spring.data.redis.password` 虽为空串，但未采用 `${ENV}` 占位符机制；所有 profile 共用同一份 `application.properties`，一旦该文件误入公开仓库即构成凭据泄露。
- **证据**：`major_assignment/src/main/resources/application.properties:L7-L10`（`spring.datasource.username=root` / `spring.datasource.password=root` / JDBC URL 指向 `localhost:3306/major_assignment`）、`L36-L39`（`spring.data.redis.password=` 明文空值未走环境变量）。仓库内未见 `application-dev.properties` / `-prod.properties`，无 profile 隔离。
- **影响等级**：高
- **建议阶段**：现状治理（阶段 1）
- **验证手段**：
  - gitleaks / detect-secrets 在 CI 中阻断明文密码与密钥模式（`Requirement 4` 安全不变量）；
  - 集成测试 `ProdConfigValidatorTests#failFastWhenMissingSecrets`：`@ActiveProfiles("prod")` 且不提供 `DB_PASSWORD` 环境变量时应用必须 fail-fast（对应 `requirements.md` R4.4、`tasks.md` 任务 6.4）；
  - 静态检查：`application*.properties` 不得直接出现 `password=` 非空字面量（仅允许 `${ENV:default}` 形式）。

#### D-04 `SecurityConfig` 对 `/api/teacher/**`、`/api/student/**` 等路径全量放行 CSRF

- **编号**：D-04
- **描述**：`SecurityConfig#securityFilterChain` 在 `csrf().ignoringRequestMatchers(...)` 中一次性放行 `/api/auth/**`、`/api/public/**`、`/api/teacher/**`、`/api/student/**`、`/api/knowledge-points/**`、`/api/early-warnings/**`、`/api/notifications/**` 共 7 条前缀，覆盖教师、学生、知识点分析、学情预警、通知几乎所有业务写接口；仅登录 / 公共接口需要放行，其余应由同源校验或令牌鉴权负责。当前配置使 CSRF 保护形同虚设。
- **证据**：`major_assignment/src/main/java/com/_202510007517/major_assignment/config/SecurityConfig.java:L31-L50`（`.csrf(...).ignoringRequestMatchers(...)` 列表，明确放行教师 / 学生 / 知识点 / 预警 / 通知 5 组业务前缀）。
- **影响等级**：高
- **建议阶段**：现状治理（阶段 1）
- **验证手段**：
  - ArchUnit / 配置扫描规则：`ignoringRequestMatchers(...)` 白名单必须是受控集合，最多包含 `/api/auth/**`、`/api/public/**`、`/api/errors/**`、`/error` 与静态资源；
  - 集成测试：对 `/api/teacher/**`、`/api/student/**`、`/api/knowledge-points/**`、`/api/early-warnings/**`、`/api/notifications/**` 的写请求在未携带 CSRF Token / 同源校验失败时必须返回 403；
  - 回归属性测试 **P18 CORS 白名单行为与配置一致**（CSRF 白名单同理，`design.md §Correctness Properties` P18 参考）。

#### D-09 会话 Cookie 未设置 `Secure` / `SameSite`，生产部署易受会话劫持（发现来源：代码审查）

- **编号**：D-09
- **描述**：`MultiRoleSessionManager#createSession` 在签发 `JSESSIONID_TEACHER / _STUDENT / _ADMIN` Cookie 时仅设置 `Path="/"`、`HttpOnly=true`、`MaxAge`，未设置 `Secure` 与 `SameSite`，在跨站或 HTTP 明文传输场景下存在会话劫持 / CSRF 交叉风险；与 D-04 的 CSRF 放行组合后风险面进一步放大。
- **证据**：`major_assignment/src/main/java/com/_202510007517/major_assignment/config/MultiRoleSessionManager.java:L98-L102`（`cookie.setPath / setHttpOnly / setMaxAge`，缺 `setSecure(true)` 与 `SameSite` 属性）。
- **影响等级**：中
- **建议阶段**：现状治理（阶段 1）
- **验证手段**：集成测试断言登录接口响应 `Set-Cookie` 报头包含 `Secure` 且 `SameSite=Lax|Strict`（`prod` profile 下强制 `Secure=true`）；属性测试 P2 会话 Filter 幂等 作为辅助验证。
- **发现来源**：诊断过程补充（静态代码审查）。

### 3.3 代码质量与可维护性

#### D-05 `AIController` 三个接口全部返回占位 / 模拟数据

- **编号**：D-05
- **描述**：`AIController#generateQuestions / generateExam / getLearningSuggestions` 均以循环拼接假数据返回，保留 `// TODO: 接入真实AI模型` 与"模拟生成试卷"、"模拟生成学习建议"等注释，答案字段固定为 `"A"` / `"正确答案"`，无法作为真实业务能力上线，但接口仍以 200 状态码返回 `ResponseResult.success`，在前端 / 数据看板侧易被误当作真实结果。
- **证据**：`major_assignment/src/main/java/com/_202510007517/major_assignment/controller/AIController.java:L97-L114`（生成题目循环、`// TODO: 接入真实AI模型`、固定答案 `"A"`）、`L141-L183`（生成试卷循环、固定答案 `"正确答案" / "详细的正确答案"`）、`L208-L220`（学习建议硬编码三条"建议加强函数概念的理解"等文案）。
- **影响等级**：中
- **建议阶段**：微服务化阶段（阶段 3） —— 对应 AI_Service 剥离（见 `tasks.md` 阶段 3 AI_Service 章节），在剥离前需在单体侧将接口统一降级为 `501 Not Implemented` 或加上"mock"特征标记并纳入 OpenAPI 文档。
- **验证手段**：
  - OpenAPI 契约测试：`/api/ai/**` 响应 Schema 必须包含 `"mock": true` 标识，或在服务未就绪时返回 `ResponseResult(501, "AI 能力尚未就绪")`；
  - 静态扫描：`AIController.java` 中不允许出现 `// TODO: 接入真实AI模型` 一类注释 + 同时返回 `success(200)` 的组合（作为 PR 检查规则）；
  - 阶段 3 剥离后以 AI_Service 自带的契约测试（Spring Cloud Contract）替代。

### 3.4 性能与数据访问

#### D-08 Mapper XML 仅覆盖 3 个表，其余 Mapper 使用注解式 SQL（含复杂 JOIN / 动态 where）

- **编号**：D-08
- **描述**：`src/main/resources/mapper/` 仅有 `CourseMapper.xml`、`EarlyWarningMapper.xml`、`StudentMapper.xml` 3 份；而 `mapper/` 包下共 12 个 `@Mapper` 接口，其中 `KnowledgePointMapper` / `StudentMapper` / `UserMapper` 等已在注解中写了多表 JOIN、`<script>` 动态 SQL、甚至 `INSERT ... ON DUPLICATE KEY UPDATE`，与"复杂 SQL 一律走 XML、简单 CRUD 走注解"的行业惯例不一致，后续维护成本高，且不利于 SQL 审计与慢查询治理。
- **证据**：
  - XML 清单（共 3 份）：`major_assignment/src/main/resources/mapper/CourseMapper.xml`、`.../EarlyWarningMapper.xml`、`.../StudentMapper.xml`；
  - Mapper 接口清单（共 12 个）：`major_assignment/src/main/java/com/_202510007517/major_assignment/mapper/{Assignment,AssignmentSubmission,BrowserError,Course,EarlyWarning,Exam,ExamSubmission,KnowledgeMastery,KnowledgePoint,Notification,Student,User}Mapper.java`；
  - 注解式复杂 SQL 示例：
    - `mapper/UserMapper.java:L15-L17`（`SELECT r.name FROM roles r JOIN user_roles ur ON r.id = ur.role_id WHERE ur.user_id = #{userId}`，跨两表 JOIN）；
    - `mapper/StudentMapper.java:L51-L53`（三表 JOIN + 角色过滤 `ur.role_id = 3`）、`L77-L80`（拼接 `class_students / course_classes / majors` 的多表查询）；
    - `mapper/KnowledgePointMapper.java:L21-L33`、`L35-L60`、`L61-L79`、`L154-L185`、`L186-L197`、`L198-L205`、`L206-L217`、`L218-L233`（`<script>` 动态 where、多表 JOIN、`INSERT ... ON DUPLICATE KEY UPDATE`）。
- **影响等级**：中
- **建议阶段**：现状治理（阶段 1）
- **验证手段**：
  - 静态检查脚本 `scripts/check-mapper-sql.sh`：扫描所有 `@Mapper` 接口，若注解 SQL 含 `JOIN` / `<script>` / `ON DUPLICATE` 或行数 > 10，则判定违规，必须迁移到 XML（对应 `tasks.md` 7.2 + `docs/coding-guidelines.md` SQL 风格规范）；
  - ArchUnit 规则：`@Mapper` 接口内的 `@Select/@Insert/@Update/@Delete` 字符串长度不得超过 200 字符（简单 CRUD 上限）；
  - MyBatis 单元测试：每个 Mapper 关键方法附带 H2 / Testcontainers MySQL 的集成测试用例。

### 3.5 可观测性

#### D-07 日志切面与认证切面对 Session 重复读取，MDC 缺位

- **编号**：D-07
- **描述**：`LoggingAspect#logAround` 与 `AuthenticationAspect#checkLogin` 分别再次从 `RequestContextHolder` 取出 `HttpServletRequest`，并独立再读一次 `request.getAttribute("SESSION_DATA")` / `HttpSession`；`MultiRoleSessionFilter` 本应是唯一写入入口。三处重复读取导致：(1) 运行成本：同一请求多次反射 `HttpServletRequest`；(2) 一致性风险：若 Filter 未写入但 Session 有遗留，日志切面仍会记录一个"幽灵 userId"；(3) 缺少 MDC 贯穿，JSON 结构化日志无 `traceId / userId / role` 字段，故障定位困难。
- **证据**：
  - `major_assignment/src/main/java/com/_202510007517/major_assignment/aspect/LoggingAspect.java:L49-L66`（切面自行再取 `ServletRequestAttributes` 并调用本地 `getCurrentUserId`）、`L94-L112`（本地实现：优先 `request.getAttribute("SESSION_DATA")`，回退 `request.getSession(false).getAttribute("userId")`）；
  - `major_assignment/src/main/java/com/_202510007517/major_assignment/aspect/AuthenticationAspect.java:L46-L68`（切面再次取 `ServletRequestAttributes`）、`L78-L106`（`isLoggedIn` / `getCurrentUserRoles` 再次双路径读取）；
  - 读入口缺一处统一：`BaseController.java:L11-L97` 与以上切面的读取逻辑重复。
- **影响等级**：中
- **建议阶段**：现状治理（阶段 1）
- **验证手段**：属性测试 **P5 日志与 MDC 字段完整性**（`design.md §Correctness Properties` P5）—— `MultiRoleSessionFilter` / `LoggingAspect` / `GlobalExceptionHandler` 记录的 MDC 值必须完全相等；ArchUnit 规则：`aspect.*` 包内禁止直接调用 `HttpSession#getAttribute` 或 `HttpServletRequest#getAttribute("SESSION_DATA")`，必须走 `MDC` / `SecurityContextHolder`（对应 `tasks.md` 任务 3.5）。

#### D-10 日志配置缺失 MDC 字段与 JSON 结构化输出（发现来源：代码审查）

- **编号**：D-10
- **描述**：`application.properties:L23-L29` 定义的 `logging.pattern.file` / `logging.pattern.console` 为 `%d [%thread] %-5level %logger - %msg`，未包含 `traceId / userId / role / uri / status / elapsed_ms` 字段；整个 `resources/` 未见 `logback-spring.xml`，未注册 `LogstashEncoder`，与 R3.4 / R3.5 要求的"JSON 结构化日志 + MDC 贯穿"不符。
- **证据**：`major_assignment/src/main/resources/application.properties:L23-L29`（纯文本 pattern，无 `%X{traceId}` 等 MDC 占位符；`resources/` 目录内无 `logback-spring.xml`）。
- **影响等级**：中
- **建议阶段**：现状治理（阶段 1）
- **验证手段**：属性测试 **P5 日志与 MDC 字段完整性** + **P6 敏感字段打码不变量**；集成测试：对一次带 `X-Trace-Id` 的请求，日志文件每行 JSON 必须包含 `{timestamp, level, logger, thread, traceId, userId, uri, status, elapsed_ms, message}`（对应 `tasks.md` 任务 5.2、5.3）。
- **发现来源**：诊断过程补充（配置扫描）。

### 3.6 前后端与部署

#### D-02 `MajorAssignmentApplication` 中硬编码 CORS 白名单

- **编号**：D-02
- **描述**：`MajorAssignmentApplication#corsConfigurer` 以 `@Bean WebMvcConfigurer` 形式直接把 `allowedOrigins("http://localhost:8080")` 写死在 `main` 入口类中，未外置到配置中心 / 环境变量，也没有开发 / 测试 / 生产多 Origin 支持；一旦需要调整白名单须重新打包，且与未来 Gateway 统一 CORS 策略冲突。
- **证据**：`major_assignment/src/main/java/com/_202510007517/major_assignment/MajorAssignmentApplication.java:L22-L33`（`@Bean corsConfigurer`，`registry.addMapping("/api/**").allowedOrigins("http://localhost:8080")` 字面量硬编码）。
- **影响等级**：中
- **建议阶段**：微服务化前置（阶段 2） —— 通过网关 + Nacos 热下发接管 CORS；过渡期在单体可先改为 `${CORS_ALLOWED_ORIGINS}` 环境变量（对应 `tasks.md` 任务 14.5、17.3）。
- **验证手段**：属性测试 **P18 CORS 白名单行为与配置一致**（`design.md §Correctness Properties` P18）—— Origin ∈ L 放行 / ∉ L 拒绝 / 热更新 TTL 内生效；静态检查：`MajorAssignmentApplication.java` 不得再出现 `allowedOrigins("http://...")` 字面量。

#### D-11 前端静态资源打包进单体 jar，部署耦合（发现来源：结构扫描）

- **编号**：D-11
- **描述**：`major_assignment/src/main/resources/static/**` 与 `SecurityConfig` 中放行的 `/static/** /webjars/** /*.html /*.js /*.css /components/** /lib/** /fonts/**` 说明当前前端资源直接随单体 jar 发布，导致后端服务与前端版本绑定、CORS / CSRF 策略纠缠、后续网关前置时需要二次迁移（违反 R14 "部署解耦"）。
- **证据**：`major_assignment/src/main/java/com/_202510007517/major_assignment/config/SecurityConfig.java:L61-L75`（`permitAll` 列表显式枚举 `/static/**`、`/*.html` 等静态资源路径，证明 jar 内含前端资源）；单体启动类未设置 `spring.web.resources.add-mappings=false`。
- **影响等级**：中
- **建议阶段**：微服务化前置（阶段 2）
- **验证手段**：属性测试 **P28 部署产物无前端资源**（`design.md §Correctness Properties` P28）—— 构建后扫描 `target/*.jar`，其中不得出现 `.html / .js / .css / .vue / .tsx` 条目；对应 `tasks.md` 任务 17.4。
- **发现来源**：诊断过程补充（目录结构 + SecurityConfig 配置交叉比对）。

## 4. 风险与影响评估

按影响等级汇总（同一问题仅按主维度记一次）：

| 等级 | 计数 | 清单 |
| --- | --- | --- |
| 高 | 3 | D-01（双轨鉴权一致性）、D-03（明文敏感配置）、D-04（CSRF 全量放行） |
| 中 | 8 | D-02、D-05、D-06、D-07、D-08、D-09、D-10、D-11 |
| 低 | 0 | — |

迁移阻塞点判定：

- **D-01 / D-03 / D-04** 属于"高影响 + 安全领域"，必须在阶段 1 结清，否则拆分 Auth_Service 时会携带一致性缺陷与口令泄露风险入云。
- **D-06 / D-07 / D-10** 直接影响阶段 2 的 Gateway / MDC / JSON 日志基线，必须同步治理。
- **D-02 / D-11** 在阶段 2 由 Gateway 与 Nginx 统一接管即可解决，单体侧只需保留"过渡期外置"方案。
- **D-05** 绑定阶段 3 AI_Service 剥离，单体阶段只做"能力真伪标识"降级处理。
- **D-08** 属于跨阶段持续整改项，在阶段 1 完成 XML 迁移规范后，阶段 2/3 服务剥离时按服务边界顺带迁出。

**高影响问题与 `docs/migration-plan.md` 口径一致性校验（R1 度量关系）**：当前 `docs/migration-plan.md` §2 阶段 1 清单为占位骨架（待任务 24 填充），其"必须在阶段 1 解决的高影响问题数量"不得超过本报告"高影响"计数（= 3）；本报告建议在阶段 1 必修清单中固定 `D-01 / D-03 / D-04` 三条。

## 5. 改造建议与优先级

| 顺序 | 问题 | 治理阶段 | 关联 tasks | 预期收益 | 回退策略 |
| --- | --- | --- | --- | --- | --- |
| 1 | D-03 明文敏感配置 | 阶段 1 | 任务 6.1~6.5 | 消除凭据泄露、接入 gitleaks 护栏 | 保留本地 dev 默认值，prod 环境变量缺失 fail-fast 可立即回滚配置 |
| 2 | D-04 CSRF 全量放行 | 阶段 1 | 任务 3.6 | 恢复 CSRF 保护、为网关同源策略铺路 | 保留登录 / 公共接口白名单，异常时回退至上一版 `ignoringRequestMatchers` 列表 |
| 3 | D-01 双轨鉴权一致性 | 阶段 1 | 任务 3、3.1、3.5 + P1/P2/P3 | 为 Auth_Service 剥离提供确定性语义 | 保留 `MultiRoleSessionFilter` 旧版本 1 个迭代，遇问题切回旧 Filter |
| 4 | D-06 BaseController 双读 | 阶段 1 | 任务 3 | 统一登录态读取入口，减少 Bug 面 | 旧 `HttpSession` 入口 `@Deprecated` 而非删除 |
| 5 | D-07 / D-10 切面重复读 + MDC/JSON 日志缺位 | 阶段 1 | 任务 5.1~5.5 | 贯通全链路排错能力 | Logback 回退至文本 pattern（配置项切换） |
| 6 | D-08 Mapper XML 规范 | 阶段 1 | 任务 7、7.1、7.2 | 统一 SQL 审计与慢查询治理 | 每 Mapper 迁移独立 PR，可按 Mapper 维度回滚 |
| 7 | D-09 Cookie 安全属性 | 阶段 1 | 任务 3、3.1 | 降低会话劫持风险 | `prod` 启用 `Secure=true`，dev 可暂保留 `false` |
| 8 | D-02 硬编码 CORS | 阶段 2 | 任务 14.5、17.3 | 由 Gateway + Nacos 热下发接管 | 过渡期保留 `${CORS_ALLOWED_ORIGINS}` 环境变量开关 |
| 9 | D-11 前端资源耦合部署 | 阶段 2 | 任务 17.1~17.4 | 前后端独立发布，解耦 Release 节奏 | 保留 1 个迭代 `spring.web.resources.add-mappings=true` 兜底 |
| 10 | D-05 AI 占位数据 | 阶段 3 | AI_Service 剥离任务组 | 接入真实 AI 能力 | 剥离前以 `ResponseResult(501, ...)` 降级 |

## 变更记录

| 日期       | 变更人 | 变更内容 |
| ---------- | ------ | -------- |
| 2026-05-10 | 架构组 | 初版骨架 |
| 2026-05-11 | 架构组 | 首次填充诊断内容：覆盖六维度、登记 R1.3 的 8 条已知问题（D-01~D-08）与 3 条新增问题（D-09 / D-10 / D-11 均标注发现来源），补齐六字段（编号 / 描述 / 证据 / 影响等级 / 建议阶段 / 验证手段） |
