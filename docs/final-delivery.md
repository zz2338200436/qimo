# Spring Cloud 迁移最终交付说明

> 版本：v0.1
> 最后更新：2026-05-21
> 作者：Codex

## 1. 交付范围

本次交付完成了教学辅助系统从单体过渡到 Spring Boot + Spring Cloud 多服务架构的主要迁移工作。Spring Cloud 不替代 Spring Boot：当前每个业务模块仍然是独立 Spring Boot 应用，Spring Cloud 负责网关、注册发现、服务间调用、熔断限流等分布式能力。

当前关键提交：

| 提交 | 内容 |
| --- | --- |
| `d7257dc` | 完成 Spring Cloud 微服务迁移主体，不包含 GitHub Actions workflow 变更 |
| `79260d4` | 修复 `analysis-service` Spring 构造注入，并补充回归测试 |
| `da3e29c` | 修复运行时烟测与教师 JWT 页面兼容问题 |
| `a36b90f` | 加固本地登录态与烟测辅助脚本 |
| `9ac38b7` | 新增统一网关 API 冒烟覆盖，并修复学生知识点 404 兼容响应 |

本轮最终运行时烟测使用新网关路由，不依赖旧单体兜底路由。

## 2. 服务与端口

| 模块 | 默认端口 | 职责 | 本地数据库 |
| --- | ---: | --- | --- |
| `registry-server` | 8761 | Eureka 注册中心 | 无 |
| `gateway` | 8080 | 统一 API 网关、鉴权透传、限流熔断 | Redis |
| `auth-service` | 8081 | 登录、验证码、Token、角色切换 | `sc_auth` |
| `user-service` | 8082 | 用户、教师、学生档案 | `sc_user` |
| `course-service` | 8083 | 课程、班级、知识点 | `sc_course` |
| `assignment-service` | 8084 | 作业、提交、批改 | `sc_assignment` |
| `exam-service` | 8085 | 考试、提交、成绩 | `sc_exam` |
| `analysis-service` | 8086 | 学情分析、趋势、预警、知识点掌握 | `sc_analysis` |
| `notification-service` | 8087 | 通知、未读数、已读状态 | `sc_notification` |
| `ai-service` | 8088 | AI 题目、试卷、学习建议生成 | `sc_ai` |
| `frontend/dist` | 5500 | 静态前端本地预览 | 无 |

本轮统一网关烟测使用默认 Gateway 地址 `http://localhost:8080`。如果本地端口冲突，可通过 `SERVER_PORT=18080` 临时覆盖，并同步调整登录态脚本和烟测脚本的 BaseUrl。

## 3. 本地启动

### 3.1 基础设施

本地最小依赖包括 MySQL、Redis、RabbitMQ：

```powershell
docker compose -f docker-compose.dev.yml up -d
```

如使用完整编排，可按部署手册执行：

```powershell
docker compose up -d --build
```

首次初始化会创建 `major_assignment` 与各 `sc_*` schema，并创建本地开发数据库账号：

| 用户名 | 密码 | 用途 |
| --- | --- | --- |
| `dev_user` | `dev_only_pwd` | 本地微服务连接 MySQL |

### 3.2 构建与验证

低内存环境推荐使用以下 Maven 参数：

```powershell
$env:MAVEN_OPTS='-Xms128m -Xmx768m -XX:CICompilerCount=2 -XX:TieredStopAtLevel=1'
mvn -T 1 "-DargLine=-Xms64m -Xmx384m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=64m -XX:CICompilerCount=2 -XX:TieredStopAtLevel=1 -Djdk.attach.allowAttachSelf=true -XX:+StartAttachListener -XX:+EnableDynamicAgentLoading" clean verify
```

本轮已完成的验证记录：

| 时间 | 命令/范围 | 结果 |
| --- | --- | --- |
| 2026-05-21 | 全仓 `mvn --% test -DfailIfNoTests=false` | `BUILD SUCCESS`，24 个模块成功 |
| 2026-05-21 | `mvn --% -pl analysis-service -am -Dtest=AnalysisQueryControllerTest -Dsurefire.failIfNoSpecifiedTests=false test` | `Tests run: 24, Failures: 0, Errors: 0, Skipped: 0` |
| 2026-05-21 | `mvn --% -pl major_assignment -Dtest=CiWorkflowStructureTest test` | `BUILD SUCCESS`；当前 checkout 无 workflow 文件时该结构检查跳过 |
| 2026-05-21 | `node .\scripts\verify-gateway-api-smoke.js` | `All smoke checks passed: 18`，测试数据已清理 |

说明：当前分支的常规验证使用 `mvn test` 与网关运行时烟测；正式封版前可再跑一次全仓 `mvn clean verify` 作为发布级证明。

### 3.3 开发登录账号

种子账号脚本：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\seed-dev-auth-users.ps1
```

本地演示账号：

| 账号 | 密码 | 角色 | userId |
| --- | --- | --- | ---: |
| `student42` | `Teach1234` | `STUDENT` | 42 |
| `teacher7` | `Teach1234` | `TEACHER` | 7 |

通过网关获取开发登录态：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\get-dev-auth-session.ps1 -Role teacher -OutFile .\.runtime-logs\teacher-session-full-smoke.json
powershell -ExecutionPolicy Bypass -File scripts\get-dev-auth-session.ps1 -Role student -OutFile .\.runtime-logs\student-session-full-smoke.json
```

脚本默认连接 `http://localhost:8080`。如果 Gateway 临时跑在 `18080`，追加 `-BaseUrl http://localhost:18080`。

## 4. 烟测结果

### 4.1 注册与健康检查

2026-05-21 的运行时烟测中，Eureka 注册表内 9 个应用均为 `UP`：

| 应用 | 端口 | 状态 |
| --- | ---: | --- |
| `AI-SERVICE` | 8088 | `UP` |
| `ANALYSIS-SERVICE` | 8086 | `UP` |
| `ASSIGNMENT-SERVICE` | 8084 | `UP` |
| `AUTH-SERVICE` | 8081 | `UP` |
| `COURSE-SERVICE` | 8083 | `UP` |
| `EXAM-SERVICE` | 8085 | `UP` |
| `GATEWAY` | 8080 | `UP` |
| `NOTIFICATION-SERVICE` | 8087 | `UP` |
| `USER-SERVICE` | 8082 | `UP` |

同一轮检查中，`8761`、`8080` 到 `8088` 的端口均处于监听状态，Gateway 统一 API 烟测通过。

### 4.2 网关接口烟测

本次通过 Gateway 发起学生和教师链路烟测，覆盖认证、系统兼容接口、课程、班级、知识点、作业、考试、分析、通知、AI。统一脚本共 18 组检查通过，结果概览如下：

| 操作 | 接口 | 结果 | 说明 |
| --- | --- | --- | --- |
| 学生身份 | `GET /api/auth/me` | 200 | Token 鉴权与用户上下文透传成功 |
| 学生课程 | `GET /api/student/courses?page=1&size=5` | 200 | `course-service` 路由成功 |
| 学生作业 | `GET /api/student/assignments?page=1&size=5` | 200 | `assignment-service` 路由成功 |
| 学生考试 | `GET /api/student/exams?page=1&size=5` | 200 | `exam-service` 路由成功 |
| 学生成绩 | `GET /api/student/scores` | 200 | 考试成绩读取成功 |
| 学习统计 | `GET /api/student/stats?courseId=2` | 200 | `analysis-service` 学生分析读取成功 |
| 学生通知 | `GET /api/notifications/student?page=1&size=5` | 200 | `notification-service` 路由成功 |
| 学习建议 | `POST /api/ai/learning-suggestions` | 200 | `ai-service` 学习建议生成成功 |
| 教师身份 | `GET /api/auth/me` | 200 | 教师 Token 鉴权成功 |
| 教师课程 | `GET /api/teacher/courses?page=1&size=5` | 200 | 教师课程读取成功 |
| 教师作业 | `GET /api/teacher/assignments?page=1&size=5` | 200 | 教师作业读取成功 |
| 教师考试 | `GET /api/teacher/exams?page=1&size=5` | 200 | 教师考试读取成功 |
| 教师仪表盘 | `GET /api/teacher/dashboard?courseId=2` | 200 | 分析仪表盘读取成功 |
| 成绩趋势 | `GET /api/teacher/score-trend?courseId=2` | 200 | 成绩趋势读取成功 |
| 知识点统计 | `GET /api/teacher/knowledge-points/stats/course/2` | 200 | 知识点掌握统计读取成功 |
| AI 生成题目 | `POST /api/ai/generate-questions` | 200 | AI 题目生成成功 |

统一脚本还覆盖了教师端课程/班级创建与删除、知识点创建与删除、作业发布与批改、考试发布与批改、通知创建与已读、学生资料更新、预警创建/导出/触发、学生知识点详情、教师学生详情兼容读取等写入链路。脚本结束时已删除本轮创建的 warning、notification、assignment、exam、class、course 测试数据。

## 5. 本地 Flyway 注意事项

如果本地 MySQL 数据卷来自旧迁移过程，部分 `sc_*` schema 可能已应用过旧版本迁移，启动时会出现 Flyway 校验和不一致或版本顺序不一致。2026-05-21 烟测遇到过以下本地历史状态：

| Schema | 现象 | 本地烟测处理 |
| --- | --- | --- |
| `sc_course` | `V1` checksum mismatch | 为保留既有数据，临时设置 `SPRING_FLYWAY_VALIDATE_ON_MIGRATE=false` |
| `sc_assignment` | `V1` checksum mismatch | 为保留既有数据，临时设置 `SPRING_FLYWAY_VALIDATE_ON_MIGRATE=false` |
| `sc_analysis` | 旧库已存在 `V20.2`，但当前代码需要补跑 `V2/V3` | 临时设置 `SPRING_FLYWAY_VALIDATE_ON_MIGRATE=false` 与 `SPRING_FLYWAY_OUT_OF_ORDER=true`，补齐 `V2/V3` |

这些设置只用于本地烟测保留历史数据，不能作为生产配置。干净环境应优先从当前初始化脚本和迁移脚本重新创建 schema；生产环境如遇校验和问题，应通过备份、审计和正式 Flyway repair 流程处理。

## 6. 交付前检查清单

| 检查项 | 当前状态 | 备注 |
| --- | --- | --- |
| 任务清单 | 已完成 | `.kiro/specs/spring-cloud-migration/tasks.md` 顶层任务已勾选 |
| 代码提交 | 已完成 | 当前远端分支最新提交为 `9ac38b7` |
| 全仓测试 | 已通过一次 | 最新提交前已跑全仓 `mvn test`，封版前可选跑 `clean verify` |
| 运行时烟测 | 已通过 | 9 个服务注册，18 组网关接口烟测通过 |
| 文档交付 | 本文档已补充 | 作为最终交付入口 |
| 标签封版 | 未执行 | 可选：`git tag spring-cloud-migration-final` |

## 变更记录

| 日期 | 变更人 | 变更内容 |
| --- | --- | --- |
| 2026-05-21 | Codex | 新增最终交付说明，记录启动、账号、端口、烟测结果与 Flyway 本地注意事项 |
