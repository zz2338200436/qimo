# 可观测性方案

> 版本：v0.2
> 最后更新：2026-05-12
> 作者：架构组

## 1. 可观测性目标

本方案覆盖 `gateway`、`auth-service`、`user-service`、过渡期 `major_assignment` 以及后续新增业务微服务，目标是把“请求发生了什么、为什么失败、影响到谁、该先看哪里”压缩为可操作的标准流程。

阶段 2 的观测目标如下：

- **统一链路定位**：任意外部请求都必须带有 `X-Trace-Id`，并在 Gateway、微服务、日志、统一响应体中保持一致，满足 R13.4、R13.6。
- **统一指标入口**：所有可部署服务统一暴露 `/actuator/health`、`/actuator/info`、`/actuator/prometheus`，由 Prometheus 抓取，满足 R13.1、R13.3。
- **统一错误语义**：日志、指标、告警对“4xx、5xx、限流、熔断、超时、下游故障”采用同一口径，避免面板正常但用户实际失败。
- **统一排障路径**：默认排障顺序固定为“Grafana 告警/仪表盘 → TraceId 检索日志 → Tempo 追踪 → 依赖健康检查”，把核心链路 MTTR 控制在 15 分钟内。
- **统一容量判断**：至少具备 JVM、HTTP、DataSource、Redis、Feign、CircuitBreaker 六类基础指标，后续服务在接入时不得降级这套基线。

说明：当前仓库已经具备 Actuator、Micrometer、Prometheus Registry、Micrometer Tracing、Resilience4j 和统一 MDC 透传的代码基础；观测基础设施的容器编排与告警文件将在任务 18.2、18.3 落地。

## 2. 总体方案与组件边界

### 2.1 组件选型

| 能力 | 组件 | 当前状态 | 说明 |
| --- | --- | --- | --- |
| 指标采集 | Spring Boot Actuator + Micrometer | 已接入依赖 | 服务统一通过 `/actuator/prometheus` 暴露 |
| 指标存储 | Prometheus | 待 18.2 落地 | 负责抓取、存储、规则计算 |
| 仪表盘 | Grafana | 待 18.2 落地 | 统一展示指标、日志、链路 |
| 日志聚合 | Loki + Promtail | 待 18.2 落地 | 收集 JSON 日志并按标签检索 |
| 链路追踪 | Micrometer Tracing + Tempo | 依赖已锁定，导出待 18.2 落地 | 统一承接 Trace / Span |
| 告警 | Prometheus Alerting Rules + Alertmanager（或兼容目标） | 待 18.3 落地 | 阶段 2 先覆盖高价值告警 |

### 2.2 代码侧基线

当前代码已经具备以下观测基线：

- `common` 模块的 [CommonAutoConfiguration.java](</D:/111/Distributed framework technology/JavaCode/qimo/common/src/main/java/com/_202510007517/platform/common/autoconfigure/CommonAutoConfiguration.java:24>) 为所有服务注册统一 `MeterRegistryCustomizer`，自动添加 `service=<spring.application.name>` 公共标签。
- Servlet 服务通过 [CommonServletMdcFilter.java](</D:/111/Distributed framework technology/JavaCode/qimo/common/src/main/java/com/_202510007517/platform/common/mdc/CommonServletMdcFilter.java:14>) 建立 `traceId / userId / roles / activeRole / uri / method` MDC 基线。
- Reactive 服务通过 [CommonReactiveMdcFilter.java](</D:/111/Distributed framework technology/JavaCode/qimo/common/src/main/java/com/_202510007517/platform/common/mdc/CommonReactiveMdcFilter.java:12>) 建立同口径 MDC，并在响应头回写 `X-Trace-Id`。
- Feign 调用通过 [FeignRequestInterceptor.java](</D:/111/Distributed framework technology/JavaCode/qimo/common/src/main/java/com/_202510007517/platform/common/feign/FeignRequestInterceptor.java:8>) 自动透传 `X-Trace-Id / X-User-Id / X-Roles / X-Active-Role`。
- 单体过渡期日志已经在 [logback-spring.xml](</D:/111/Distributed framework technology/JavaCode/qimo/major_assignment/src/main/resources/logback-spring.xml:1>) 落地 JSON 结构化输出与敏感字段打码。
- 单体过渡期控制器访问日志通过 [LoggingAspect.java](</D:/111/Distributed framework technology/JavaCode/qimo/major_assignment/src/main/java/com/_202510007517/major_assignment/aspect/LoggingAspect.java:29>) 写入 `status / elapsed_ms`，异常出口通过 [GlobalExceptionHandler.java](</D:/111/Distributed framework technology/JavaCode/qimo/major_assignment/src/main/java/com/_202510007517/major_assignment/controller/GlobalExceptionHandler.java:243>) 与统一 `ResponseResult` 保持一致。

## 3. 指标体系

### 3.1 指标清单

阶段 2 必须具备以下最小指标集：

| 类别 | 关键指标 | 关注维度 | 主要用途 |
| --- | --- | --- | --- |
| JVM | `jvm_memory_used_bytes`、`jvm_gc_pause_seconds`、`process_cpu_usage`、`system_cpu_usage` | `service`, `instance`, `area` | 判断内存压力、GC 抖动、CPU 饱和 |
| HTTP | `http_server_requests_seconds_count`、`http_server_requests_seconds_bucket`、`http_server_requests_seconds_sum` | `service`, `uri`, `method`, `status`, `outcome` | 计算吞吐、错误率、P95/P99 |
| DataSource | `hikaricp_connections_active`、`hikaricp_connections_pending`、`hikaricp_connections_timeout_total` | `service`, `pool` | 判断连接池是否耗尽 |
| Redis | `spring_data_redis_commands` 或 Lettuce/Jedis 指标、缓存命中/失败数 | `service`, `command`, `result` | 判断缓存依赖与限流令牌桶健康 |
| Feign | Feign client 请求耗时、异常数、4xx/5xx 分布 | `service`, `client`, `method`, `status` | 判断下游抖动与消费者受影响范围 |
| CircuitBreaker | `resilience4j_circuitbreaker_state`、`resilience4j_circuitbreaker_calls`、`resilience4j_circuitbreaker_failure_rate` | `service`, `name`, `kind` | 判断熔断状态与降级触发频次 |
| Gateway | 路由级请求量、限流拒绝数、Fallback 次数 | `service`, `routeId`, `status` | 判断入口容量与故障面 |
| 认证/安全 | 登录失败数、验证码失败数、令牌刷新失败数、限流命中数 | `service`, `routeId`, `ip`, `reason` | 支撑 R17.4 登录异常告警 |

额外约束：

- 所有 Micrometer 指标必须至少带 `service` 标签，来源于 `commonMeterRegistryCustomizer`。
- HTTP 指标保留 `uri` 时必须使用模板化路径，不允许把原始主键、token、手机号写入标签，避免高基数与敏感信息泄露。
- Gateway 的 `routeId`、CircuitBreaker 的 `name`、Feign 的客户端名必须与服务发现名一致，例如 `auth-service`、`user-service`、`legacy-monolith`。

### 3.2 视图与查询口径

Grafana 至少预置以下看板：

1. **平台总览**：各服务 QPS、5xx 比例、P95、实例健康数。
2. **Gateway 总览**：按 `routeId` 看请求量、限流命中、Fallback 次数、熔断状态。
3. **认证链路**：`gateway -> auth-service -> user-service` 的登录、刷新、改密关键路径指标。
4. **资源层看板**：JVM、HikariCP、Redis、MySQL、Eureka 注册状态。
5. **错误看板**：Top 5 异常类型、Top 5 失败接口、Top 5 下游失败来源。

阶段 2 的核心 SLO 计算口径：

- **错误率**：`5xx / 全请求量`，由 `http_server_requests_seconds_count` 统计。
- **延迟**：核心接口使用 histogram 计算 P95，重点关注登录、首页看板、作业列表。
- **可用性**：对外以 Gateway 路由成功率为准，不以单服务自报健康直接替代用户感知。

### 3.3 采集与暴露约定

- 每个服务都必须引入 `spring-boot-starter-actuator`；当前 `gateway`、`auth-service`、`user-service`、`major_assignment` 已具备依赖基础。
- 统一暴露端点：`health,info,prometheus`。注册中心 [application.yml](</D:/111/Distributed framework technology/JavaCode/qimo/registry-server/src/main/resources/application.yml:11>) 已使用该口径，其余服务在任务 18.2/18.4 中补齐。
- 管理端点默认与业务端口同机部署，但在测试/生产环境必须通过内网绑定、Ingress 白名单或 Security `requestMatcher` 进行保护。
- 当前代码基线已在共享模块中启用 Actuator 来源收口：`platform.management.access.enabled=true` 时，仅允许环回地址、RFC1918 私网、链路本地地址与 IPv6 ULA 访问 `/actuator/**`；如需调整网段，可在服务配置中覆盖 `platform.management.access.allowed-cidrs`。
- Prometheus 抓取周期建议 15 秒，规则计算周期建议 15 秒；高频容量压测场景可临时降为 5 秒，但不得作为常态配置。

## 4. 日志方案

### 4.1 日志格式与字段

日志分为两层：

- **开发/本地控制台**：保留可读文本格式，便于本地调试。
- **文件/容器标准输出**：统一采用 JSON 结构化日志，供 Loki 聚合检索。

结构化日志的标准字段如下：

| 字段 | 含义 | 来源 |
| --- | --- | --- |
| `timestamp` | UTC 时间戳 | Logback JSON provider |
| `level` | 日志级别 | Logback |
| `logger` | Logger 名称 | Logback |
| `thread` | 线程名 | Logback |
| `app` | 应用名 | `spring.application.name` |
| `traceId` | 全链路请求 ID | MDC / `X-Trace-Id` |
| `userId` | 当前用户 ID | Gateway 身份头 / 会话上下文 |
| `roles` | 用户角色全集 | 微服务 `common` MDC |
| `activeRole` | 当前生效角色 | 微服务 `common` MDC |
| `role` | 过渡期单体角色字段 | 单体 MDC（仅迁移期间保留） |
| `uri` | 请求路径 | Filter 写入 MDC |
| `method` | HTTP 方法 | Filter 写入 MDC |
| `status` | 业务处理结果状态 | 访问日志/异常处理器写入 MDC |
| `elapsed_ms` | 请求耗时 | 访问日志切面写入 MDC |
| `exceptionType` | 异常类型 | 全局异常处理器 |
| `message` | 日志消息 | 应用日志 |
| `stacktrace` | 堆栈信息 | 仅错误日志输出 |

约束如下：

- `traceId`、`userId`、`uri` 为排障最小必备字段，缺一视为日志不合格。
- `message`、异常栈、请求参数中出现密码、验证码、token、手机号、邮箱时必须打码；单体已有 `MaskingConverter` / `MessageMaskingJsonProvider` 基线，微服务沿用同口径。
- 不允许把整份 JWT、完整密码哈希、数据库连接串、Authorization 头原文写入日志。

### 4.2 日志级别口径

统一日志级别约定：

- `DEBUG`：本地开发和测试环境排障信息，可包含方法进入/离开、关键分支命中。
- `INFO`：正常业务链路、服务启动、关键状态切换、访问摘要。
- `WARN`：可恢复异常，例如参数问题、资源不存在、限流命中、NoHandlerFound、下游 4xx。
- `ERROR`：真实故障，例如下游 5xx、超时、未捕获异常、熔断进入 OPEN 后导致的业务失败。

阶段 2 不做日志采样；生产环境默认保留全量 `WARN/ERROR` 与全量访问摘要 `INFO`。只有在高并发压测时，才允许对低价值 `DEBUG` 做降噪。

### 4.3 采集、标签与保留周期

- 应用输出到容器标准输出或滚动日志文件，由 Promtail 收集。
- Loki 标签至少包含 `service`、`instance`、`level`、`env`；`traceId` 不作为标签，仅作为正文 JSON 字段检索，避免高基数。
- 在线日志保留周期：默认 30 天；故障演练和审计相关日志按运维归档策略延长。
- 典型查询入口：
  - 按 `traceId` 查询一次完整调用链；
  - 按 `service=user-service AND level=ERROR` 看服务异常；
  - 按 `uri=/api/auth/login` 看登录失败聚集。

## 5. 链路追踪

### 5.1 TraceId 贯穿策略

当前仓库采用“入口复用、缺失生成、头部透传、响应回写”的 TraceId 策略：

1. 入站请求若携带 `X-Trace-Id`，则直接复用；否则生成去连字符 UUID。
2. Servlet 链路使用 [CommonServletMdcFilter.java](</D:/111/Distributed framework technology/JavaCode/qimo/common/src/main/java/com/_202510007517/platform/common/mdc/CommonServletMdcFilter.java:14>) 写入 MDC、请求属性与响应头。
3. Reactive 链路使用 [CommonReactiveMdcFilter.java](</D:/111/Distributed framework technology/JavaCode/qimo/common/src/main/java/com/_202510007517/platform/common/mdc/CommonReactiveMdcFilter.java:12>) 对 Gateway 等 WebFlux 服务做同样处理。
4. 服务间同步调用通过 [FeignRequestInterceptor.java](</D:/111/Distributed framework technology/JavaCode/qimo/common/src/main/java/com/_202510007517/platform/common/feign/FeignRequestInterceptor.java:8>) 透传 `X-Trace-Id` 及身份头。
5. 统一响应体 `ResponseResult` 会把当前 `traceId` 带回前端，便于用户报障时直接携带请求号。
6. Gateway CORS 已暴露 `X-Trace-Id` 响应头，前端可以在浏览器网络面板直接读取。

后续扩展约束：

- 异步线程池任务、消息消费、定时任务必须显式复制 `traceId` 到任务上下文；若无上游请求，则以“作业触发事件”生成新的根 TraceId。
- 日志、指标、追踪三者必须以同一个 `traceId` 互相跳转，不允许三套 ID 并行。

### 5.2 Sampling 策略

链路采样策略统一如下：

| 环境 | 采样率 | 说明 |
| --- | --- | --- |
| `dev` | 100% | 便于本地与联调完整排障 |
| `test` | 100% | 便于验收和回归时复盘 |
| `prod` | 10% | 兼顾成本与可见性；错误链路、限流、熔断、登录失败事件需强制保留 |

生产环境补充规则：

- 对 5xx、熔断打开、下游超时、登录失败过频等异常路径做“基于事件的保留”，即使常规采样命中率较低，也必须让对应 Span 可检索。
- 当进行灰度、迁移演练、性能压测时，可临时把特定服务或路由采样调高到 100%，结束后恢复默认值。

### 5.3 Tracing 后端与导出协议

- 统一使用 Micrometer Tracing 桥接 OpenTelemetry。
- 统一导出到 Tempo，阶段 2 主协议采用 **OTLP/HTTP**，目标地址约定为 `http://tempo:4318/v1/traces`。
- 本地开发默认不导出 OTLP traces，避免未启动 Tempo / Collector 时反复出现 `127.0.0.1:4318` 连接错误；需要启用导出时设置 `MANAGEMENT_OTLP_TRACING_EXPORT_ENABLED=true`，并通过 `OTEL_EXPORTER_OTLP_ENDPOINT` 指向实际 Collector。
- Tempo 只作为追踪后端，不承担日志存储职责；日志检索仍走 Loki。
- Grafana 中从日志按 `traceId` 跳转 Tempo，从 Tempo 反查对应日志与指标。

## 6. 告警方案

### 6.1 告警分级

| 级别 | 触发条件示例 | 处理要求 |
| --- | --- | --- |
| P1 | Gateway 全站 5xx 激增、登录链路不可用、注册中心不可达 | 立即响应，优先止血/回滚 |
| P2 | 单服务熔断持续打开、Redis/数据库连接池耗尽风险、限流异常升高 | 15 分钟内介入，确认是否影响用户 |
| P3 | JVM 内存抖动、错误率轻微升高、单实例 scrape 丢失 | 值班时段内处理，持续观察 |

### 6.2 阶段 2 必须落地的规则

任务 18.3 至少实现以下 Prometheus 规则：

| 规则 | 建议表达意图 | 严重级别 | 目的 |
| --- | --- | --- | --- |
| `instance_5xx_rate > 5%` 持续 1 分钟 | Gateway 或服务 5xx 比例超过阈值 | P1 | 及时发现用户可见故障 |
| `circuit_breaker_state == OPEN` 持续 30 秒 | 任一核心熔断器保持 OPEN | P2 | 发现下游持续故障或错误阈值过低 |
| `login_failures_per_ip > 30 / 60s` | 单 IP 登录失败爆发 | P1/P2 | 对齐 R17.4 安全防护 |

建议同步补充但可次阶段实施的规则：

- `up == 0` 持续 1 分钟：服务实例掉线。
- `hikaricp_connections_pending > 0` 持续 3 分钟：数据库连接池拥堵。
- `redis_command_failures` 持续升高：限流、验证码、Token 黑名单可能失效。
- Prometheus 抓取失败或 Tempo/Loki 不可用：观测系统自身故障。

### 6.3 告警处置流程

标准处置顺序：

1. 在 Grafana / Alertmanager 查看告警对象、服务、持续时间、最近趋势。
2. 进入对应服务日志，按 `traceId`、`routeId`、`exceptionType` 检索。
3. 若为链路问题，跳转 Tempo 查看跨服务 Span。
4. 结合健康检查和资源指标判断是流量、代码、依赖还是配置问题。
5. 需要回滚时按 `docs/migration-plan.md` 的阶段回滚策略执行。

## 7. 健康检查与探针

健康检查分三层：

- **Liveness**：进程是否存活，避免死锁或线程池卡死后继续接流量。
- **Readiness**：实例是否具备接流量能力，例如数据库、Redis、注册中心依赖是否准备好。
- **Business Readiness**：核心路由是否可用，例如 Gateway 到 Auth/User 的关键依赖是否已注册。

阶段 2 约定：

- Kubernetes 或容器编排统一使用 `/actuator/health/liveness` 与 `/actuator/health/readiness`。
- `readiness` 默认包含数据库、Redis、Eureka 注册状态；若服务无对应依赖，则不强行暴露空探针。
- `prometheus`、`health` 端点只允许内网或受控来源访问，18.4 负责把暴露面真正收紧。

## 8. 实施顺序

与任务清单对应的实施顺序如下：

1. **18.1**：完成本方案文档，统一指标/日志/追踪/告警口径。
2. **18.2**：落地 `docker-compose.obs.yml`、Prometheus、Grafana、Loki、Promtail、Tempo。
3. **18.3**：把高价值告警规则写入 `deploy/prometheus/alerts/`。
4. **18.4**：收紧 Actuator 暴露面，避免观测端点被公网直接访问。

## 变更记录

| 日期 | 变更人 | 变更内容 |
| --- | --- | --- |
| 2026-05-10 | 架构组 | 初版骨架 |
| 2026-05-12 | Codex | 补全指标、日志、TraceId、采样策略、告警与健康检查方案 |
| 2026-05-19 | Codex | 明确本地默认关闭 OTLP trace 导出，观测栈启动后通过环境变量显式开启，避免无 Collector 时刷 4318 连接错误日志。 |
