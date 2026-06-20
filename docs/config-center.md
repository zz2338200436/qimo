# Spring Cloud Config 配置中心

## 组件定位

本项目新增 `config-server` 作为 Spring Cloud Config Server，端口为 `8888`。它使用 `native` 模式从类路径下的 `config-repo` 读取配置，适合课程项目和本地演示；后续可以平滑切换为 Git 后端。

## 配置仓库

配置仓库位于：

```text
config-server/src/main/resources/config-repo/
```

主要文件：

```text
application.yml              # 所有 Config Client 共享的 Eureka、Actuator、RabbitMQ、Outbox 基线配置
application-dev.yml          # IDEA / 本机联调环境，依赖 localhost
application-docker.yml       # Docker Compose 环境，依赖 mysql / redis / rabbitmq / registry-server 容器名
application-prod.yml         # 生产环境模板，敏感值必须由环境变量注入
gateway.yml                  # 网关端口、CORS、安全白名单、路由、限流与 Resilience4j 熔断配置
{service}.yml                # 服务端口、数据源、Flyway、JPA、消息绑定和业务开关
{service}-docker.yml         # Docker 环境覆盖项，数据库默认 dev_user / dev_only_pwd
{service}-prod.yml           # 生产环境覆盖项，不提供明文密码默认值
```

Config Server 现在统一管理 `dev`、`docker`、`prod` 三类环境配置。`dev` 环境用于 IDEA 本地开发，数据库密码默认 `root`；`docker` 环境继续沿用 Compose 初始化账号 `dev_user / dev_only_pwd`；`prod` 环境只保留环境变量占位，不提供明文密码默认值。

运行时配置不再散落在各业务服务本地 `application.yml`。数据库、RabbitMQ、Redis、JWT 正式密钥等生产敏感值应通过环境变量、受保护的配置仓库或后续的 Vault/加密配置注入。

本地开发未配置 `auth.keys.*` 或 `gateway.security.jwt.public-keys.*` 时，认证服务和网关会使用代码中按固定开发 seed 生成的开发 RSA 密钥对兜底，方便课堂演示和本机联调。生产环境应显式配置真实密钥，并避免使用开发兜底。

`gateway.yml` 已集中管理网关路由表、路由级限流和 Resilience4j 熔断参数。Spring Cloud Gateway 4.3.0 的 WebFlux 路由使用：

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
```

旧的 `spring.cloud.gateway.routes` 前缀不再用于本项目配置，避免启动时出现配置迁移提示。网关本地 `application.yml` 只保留应用名和 Config Client 启动基线；端口、CORS、安全白名单和路由均由配置中心管理。

## 客户端接入

以下模块已接入 `spring-cloud-starter-config`：

```text
gateway
auth-service
user-service
course-service
assignment-service
exam-service
analysis-service
notification-service
ai-service
agent-service
legacy-adapter
```

每个客户端本地 `application.yml` 只保留应用名、默认 profile 和配置中心导入：

```yaml
spring:
  application:
    name: exam-service
  profiles:
    default: dev
  config:
    import:
      - configserver:${CONFIG_SERVER_URL:http://localhost:8888}
```

这意味着本地 IDEA 联调也应先启动 Config Server（可以通过 `scripts/start-dev-infra.ps1` 拉起 Docker 中的 `config-server`，也可以直接运行 `ConfigServerApplication`），再启动各业务服务。配置中心未启动时，业务服务会快速失败，避免误用过期的本地配置。

测试环境通过 `test` profile 和 Maven Surefire 系统属性关闭 Config Client：

```yaml
spring:
  cloud:
    config:
      enabled: false
```

Docker/生产环境启用 `docker | prod` profile，并开启 fail-fast 与 retry：

```yaml
spring:
  cloud:
    config:
      fail-fast: true
      retry:
        max-attempts: 6
```

## Docker 启动顺序

`docker-compose.yml` 中新增 `config-server`、`agent-service` 和 `legacy-adapter`，所有 Config Client 容器均设置 `SPRING_PROFILES_ACTIVE=docker`。MySQL、Redis、RabbitMQ 在 Compose 中声明健康检查；各 Spring Boot 服务的 Dockerfile 通过 `/actuator/health` 声明健康检查。服务依赖使用 `condition: service_healthy`，避免只等容器创建完成就启动客户端。

启动顺序为：

```text
mysql / redis / rabbitmq -> registry-server -> config-server -> business services / agent-service / legacy-adapter -> gateway
```

容器环境中客户端通过：

```yaml
CONFIG_SERVER_URL: http://config-server:8888
```

访问配置中心。

可以用下面的命令先校验 Compose 语法与依赖展开：

```bash
docker compose -f docker-compose.yml config --quiet
```

如果只验证配置中心链路，可以先启动注册中心和配置中心：

```bash
docker compose -f docker-compose.yml up -d --build registry-server config-server
```

Prometheus 已补全 registry、config、gateway、业务服务和 legacy-adapter 的 `/actuator/prometheus` 抓取目标，便于统一观察配置中心接入后的服务健康状态。

## LoadBalancer 缓存

项目中注册中心、配置中心、网关和各业务微服务均补充了 `com.github.ben-manes.caffeine:caffeine`。Spring Cloud LoadBalancer 会在检测到 Caffeine 后使用它作为服务实例缓存实现，避免默认缓存提示，并让基于 Eureka 的服务发现和 `lb://` 路由在高频调用时拥有更稳定的本地缓存表现。

## 验证接口

启动配置中心后可以访问：

```text
http://localhost:8888/actuator/health
http://localhost:8888/application/default
http://localhost:8888/application/dev
http://localhost:8888/application/docker
http://localhost:8888/gateway/default
http://localhost:8888/exam-service/dev
http://localhost:8888/exam-service/docker
http://localhost:8888/exam-service/prod
http://localhost:8888/agent-service/docker
```

`gateway/default` 应返回 `gateway.yml` 与 `application.yml` 两类配置源，并包含 `spring.cloud.gateway.server.webflux.routes`、限流和熔断配置。`exam-service/dev` 应返回 localhost 数据库连接和 `root` 默认密码，`exam-service/docker` 应返回 mysql 容器连接和 `dev_only_pwd` 默认密码，`exam-service/prod` 应只返回环境变量占位。`agent-service/docker` 应返回 `sc_agent` 数据源，并默认关闭 `agent.llm.enabled`，避免本地 Compose 在未提供模型 API key 时启动失败。

## 报告表述

本项目引入 Spring Cloud Config 配置中心，采用 Config Server + Config Client 架构集中管理多微服务配置。Config Server 使用 native 配置仓库提供公共配置和服务专属配置，各微服务启动时通过 `spring.config.import=configserver:` 拉取配置。该设计减少重复配置，提高了多环境切换、服务治理参数调整和配置统一维护能力。
