# 部署与运维手册

> 版本：v0.3
> 最后更新：2026-05-19
> 作者：架构组

## 1. 环境分层

| 环境 | 目标 | Profile | Namespace |
| --- | --- | --- | --- |
| `dev` | 本地联调、单机调试、前后端冒烟 | `dev` | `dev` |
| `test` | CI/CD 自动部署、回归验证、接口契约验证 | `test` | `test` |
| `prod` | 正式运行、灰度切流、旧单体下线前后的稳定性保障 | `prod` | `prod` |

环境映射遵循 R15.5：Spring Profile 与部署命名空间一一对应。Helm umbrella chart 已预留 `deploy/charts/target-platform/values-{dev,test,prod}.yaml`。

## 2. 基础设施

### 2.1 运行平台

当前交付同时支持两种运行方式：

- 本地 Docker Compose：使用根目录 `docker-compose.yml` 启动 MySQL、Redis、RabbitMQ、Prometheus、Grafana、Registry、Gateway、旧单体与 8 个业务服务。
- 观测栈独立启动：使用 `docker-compose.obs.yml` 单独拉起 Prometheus、Grafana、Loki、Promtail、Tempo。

### 2.2 中间件

本地联调阶段当前采用：

- MySQL 8.4
- Redis 7.4
- RabbitMQ 3.13 Management
- `docker-compose.dev.yml` 作为最小依赖编排入口
- `docker-compose.yml` 作为完整平台聚合入口

联调注意事项：

- 微服务默认连接的 schema 不是 `major_assignment`，而是各自 `sc_*`
- 本地 MySQL 首次启动时会执行 `data/mysql/init/01-create-microservice-schemas.sql`
- MySQL 数据目录与初始化脚本目录必须分离挂载：
  - `data/mysql/db -> /var/lib/mysql`
  - `data/mysql/init -> /docker-entrypoint-initdb.d`
- 该脚本会创建：
  - `major_assignment`
  - `sc_auth`
  - `sc_user`
  - `sc_course`
  - `sc_assignment`
  - `sc_exam`
  - `sc_analysis`
  - `sc_notification`
  - `sc_ai`
  - `sc_platform`
- 该脚本还会创建开发账号：
  - 用户名 `dev_user`
  - 密码 `dev_only_pwd`

如果本地 `data/mysql/db` 已包含旧数据卷，初始化脚本不会自动重复执行；此时需要手工补库或清空数据卷后重建容器。
如果本机已经有 MySQL 占用 `3306`，当前 `docker-compose.dev.yml` 或 `docker-compose.yml` 无法直接拉起 `qimo-mysql`，需要先释放端口或调整映射端口。
如果本机已经存在同名容器（例如 `qimo-redis`），需要先删除旧容器或复用既有容器。

## 3. 构建与制品

### 3.1 Dockerfile 模板

所有运行时服务统一使用多阶段构建：

- Builder：`maven:3.9-eclipse-temurin-17`
- Runtime：`eclipse-temurin:17-jre-jammy`
- 健康检查：`HEALTHCHECK ... /actuator/health`

当前已为以下模块提供运行时 Dockerfile：

- `registry-server`
- `gateway`
- `auth-service`
- `user-service`
- `course-service`
- `assignment-service`
- `exam-service`
- `analysis-service`
- `notification-service`
- `ai-service`
- `major_assignment`（仅过渡期兼容与 `legacy-route` 联调）

### 3.2 镜像命名

CI 中镜像标签采用 `{service}-{gitShortSha}-{date}`，例如 `gateway-a1b2c3d-20260519`。这满足 R15.3 对可追溯构建产物的要求。

## 4. 本地编排

### 4.1 最小依赖

仅需数据库与缓存时使用：

```bash
docker compose -f docker-compose.dev.yml up -d
```

### 4.2 完整平台

需要完整微服务、Gateway 与旧单体兼容路由时使用：

```bash
docker compose up -d --build
```

根 `docker-compose.yml` 包含：

- 基础设施：`mysql`、`redis`、`rabbitmq`
- 观测组件：`prometheus`、`grafana`
- 平台服务：`registry-server`、`gateway`
- 过渡服务：`legacy-monolith`
- 业务服务：`auth-service`、`user-service`、`course-service`、`assignment-service`、`exam-service`、`analysis-service`、`notification-service`、`ai-service`

### 4.3 过渡期兼容

Gateway 仍保留 `legacy-route` 时，`docker-compose.yml` 中会同时启动 `legacy-monolith` 并通过 `LEGACY_BASE_URL=http://legacy-monolith:8080` 兜底。等任务 27 的单体下线检查全部满足后，再删除该服务与对应环境变量。

## 5. Helm 部署

当前 Helm 交付采用“umbrella + per-service chart”骨架：

- Umbrella chart：`deploy/charts/target-platform`
- 基础设施 chart：`deploy/charts/platform-infra`
- 服务 chart：
  - `deploy/charts/registry-server`
  - `deploy/charts/gateway`
  - `deploy/charts/auth-service`
  - `deploy/charts/user-service`
  - `deploy/charts/course-service`
  - `deploy/charts/assignment-service`
  - `deploy/charts/exam-service`
  - `deploy/charts/analysis-service`
  - `deploy/charts/notification-service`
  - `deploy/charts/ai-service`

当前阶段这些 chart 主要承担目录约定、环境 values 与后续模板扩展的入口，适合在任务 28 后续迭代中继续补全 Deployment/Service/ConfigMap 模板。

## 6. CI/CD 流水线

`.github/workflows/ci.yml` 当前固定 10 个阶段，顺序如下：

1. `lint`
2. `unit`
3. `pbt`
4. `integration`
5. `contract`
6. `scan`
7. `docker-build`
8. `docker-push`
9. `deploy-test`
10. `e2e-smoke`

结构断言测试会校验这些 job 是否存在，以及 `needs` 链是否保持串联顺序。任一阶段失败都必须立即阻断后续阶段，符合 R15.4。

## 7. 配置管理

当前阶段 2/3 过渡期仍以服务本地 `application.yml` + 环境变量承接配置，符合 R7.4 与 R4.5。

关键环境变量约定：

- Eureka：`EUREKA_SERVER_URL`
- MySQL：`{DOMAIN}_DB_URL`、`{DOMAIN}_DB_USERNAME`、`{DOMAIN}_DB_PASSWORD`
- Redis：`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`
- RabbitMQ：`RABBITMQ_HOST`、`RABBITMQ_PORT`、`RABBITMQ_USERNAME`、`RABBITMQ_PASSWORD`
- OTLP Trace：`MANAGEMENT_OTLP_TRACING_EXPORT_ENABLED`、`OTEL_EXPORTER_OTLP_ENDPOINT`

## 8. 运维手册

### 8.1 日常巡检

每日巡检至少覆盖：

- `registry-server`、`gateway` 与核心业务服务 `/actuator/health`
- MySQL、Redis、RabbitMQ 容器状态
- Prometheus targets 是否全绿
- Grafana 是否能读取 Prometheus 数据源
- 关键路由 smoke：登录、课程读取、通知读取、AI 题目生成

### 8.2 故障处置 SOP

推荐顺序：

1. 先看 Gateway `/actuator/health` 与 Eureka 注册状态。
2. 再看目标服务容器日志与 `/actuator/health`。
3. 如果是切流问题，优先恢复 `legacy-route` 或旧路由，不对数据库做 destructive rollback。
4. 如果是消息链问题，优先检查 RabbitMQ、outbox 表、消费幂等记录。

### 8.3 扩缩容

当前 Docker Compose 仅用于本地和测试验证，不做自动扩缩容。生产或准生产环境建议通过 Helm/Kubernetes 按服务维度扩容，优先扩 `gateway`、`auth-service`、`assignment-service`、`exam-service` 等高频入口。

## 9. 备份与恢复

当前最低要求：

- MySQL 数据目录定期备份
- 关键回填脚本保留在 `deploy/scripts/backfill`
- 单体最终可启动状态保留只读备份分支 `archive/legacy-final`
- 发生切流事故时，先恢复 Gateway 路由，再根据需要恢复数据或重放事件

## 变更记录

| 日期       | 变更人 | 变更内容 |
| ---------- | ------ | -------- |
| 2026-05-10 | 架构组 | 初版骨架 |
| 2026-05-14 | Codex | 补充本地联调阶段的 MySQL 多 schema 初始化约定与 docker-compose 开发环境说明 |
| 2026-05-19 | Codex | 补充多阶段 Dockerfile 模板、完整 `docker-compose.yml`、Helm 目录约定、CI/CD 阶段链与环境 Profile/Namespace 映射说明 |
