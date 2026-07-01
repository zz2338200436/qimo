# Spring Cloud 迁移最终交付说明

> 版本：v0.1
> 最后更新：2026-05-23
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
| `agent-service` | 8092 | 自然语言 Agent 编排、动作预览、确认、审计、流式聊天 | `sc_agent` |
| `frontend/dist` | 5500 | 前端本地预览（静态资源 + `/api/**` 代理到 Gateway） | 无 |

本轮统一网关烟测使用默认 Gateway 地址 `http://localhost:8080`。`5500` 预览端口由本地轻量前端服务器承载，页面中的 `/api/**` 会转发到 Gateway，因此浏览器冒烟路径与生产部署保持一致。如果本地端口冲突，可通过 `SERVER_PORT=18080` 临时覆盖，并同步调整登录态脚本和烟测脚本的 BaseUrl。

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
| 2026-05-22 | `node .\scripts\verify-gateway-api-smoke.js` | `All smoke checks passed: 20`，统一网关链路再次验证通过 |
| 2026-05-22 | `node .\scripts\verify-student-jwt-pages.js .\.runtime-logs\student-session-full-smoke.json` | 学生高频页面 5/5 通过：`student-dashboard / courses / assignments / notifications / settings` |
| 2026-05-22 | `node .\scripts\verify-teacher-jwt-pages.js .\.runtime-logs\teacher-session-full-smoke.json` | 教师受保护页面 9/9 通过：`teacher-dashboard / courses / assignments / knowledge / warning / student-dashboard / notifications / settings / ai-tools` |
| 2026-05-22 | `mvn --% -pl major_assignment test -DfailIfNoTests=false` | `Tests run: 85, Failures: 0, Errors: 0, Skipped: 2` |
| 2026-05-23 | `node .\scripts\verify-student-jwt-pages.js .\.runtime-logs\student-session-polish-fresh.json` | 学生高频页面 6/6 通过：`student-dashboard / courses / assignments / notifications / settings / stats` |
| 2026-05-23 | `node .\scripts\verify-student-browser-crud.js .\.runtime-logs\teacher-session-polish-fresh.json .\.runtime-logs\student-session-polish-fresh.json` | 学生浏览器 CRUD smoke 5/5 通过 |
| 2026-05-23 | `mvn --% -pl gateway -Dtest=FrontendCapabilityEdgeControllerTest test` | `BUILD SUCCESS`，验证 Gateway 侧 `/api/frontend/capabilities` 能力矩阵接口 |
| 2026-05-23 | `node .\scripts\verify-teacher-jwt-pages.js .\.runtime-logs\teacher-session-polish-fresh.json` | 教师受保护页面 9/9 通过：`teacher-dashboard / courses / assignments / knowledge / warning / student-dashboard / notifications / settings / ai-tools` |
| 2026-05-23 | `node .\scripts\verify-teacher-browser-crud.js .\.runtime-logs\teacher-session-polish-fresh.json .\.runtime-logs\student-session-polish-fresh.json` | 教师浏览器 CRUD smoke 5/5 通过 |
| 2026-06-20 | `mvn --% -pl agent-service -am -Dtest=AgentControllerTest,AgentOrchestratorTest -Dsurefire.failIfNoSpecifiedTests=false test` | Agent 控制器与编排定向回归通过：`Tests run: 32, Failures: 0` |
| 2026-06-20 | `node .\scripts\verify-agent-service-frontend-contract.js` | Agent 前后端契约通过，覆盖 `/api/agent/chat/stream`、前端 SSE 消费和静态资源版本 |
| 2026-06-21 | `mvn --% -pl agent-service -am -Dtest=DefaultAgentChatStreamingServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` | Agent SSE 首包回归通过，验证 `orchestrator.chat(...)` 完成前已先发出流式起始事件 |
| 2026-06-21 | `node .\scripts\verify-agent-frontend-browser-smoke-contract.js` | Agent 浏览器 smoke 契约通过，验证脚本已按页面渲染结果取证，不再依赖 `response.text()` 读取 SSE 响应体 |
| 2026-06-21 | `node .\scripts\verify-agent-frontend-browser-smoke.js` | Agent 浏览器烟测 15/15 通过，覆盖教师/学生流式聊天、动作预览、确认执行、考试提交流程、作业提交流程、通知发送与已读更新 |

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

### 4.3 浏览器级页面烟测

2026-05-22 已完成面向真实 `5500 前端预览 -> Gateway(8080) -> 微服务` 链路的页面级烟测：

| 页面 | 结果 | 说明 |
| --- | --- | --- |
| `teacher-dashboard.html` | 通过 | 命中教师课程、作业、提交聚合数据 |
| `teacher-courses.html` | 通过 | 命中教师课程接口 |
| `teacher-assignments.html` | 通过 | 命中教师作业与课程接口 |
| `teacher-knowledge.html` | 通过 | 命中教师知识点与分析接口 |
| `teacher-warning.html` | 通过 | 命中 `analysis-service` 预警接口 |
| `teacher-student-dashboard.html` | 通过 | 命中教师学情分析接口 |
| `teacher-notifications.html` | 通过 | 页面首屏无鉴权错误、无 JS 异常 |
| `teacher-settings.html` | 通过 | 命中教师用户资料接口 |
| `teacher-ai-tools.html` | 通过 | 页面首屏无鉴权错误、无 JS 异常 |
| `student-dashboard.html` | 通过 | 学生综合表现接口未接通时，会自动回退到已接通课程/作业/考试数据源 |
| `student-courses.html` | 通过 | 命中学生课程接口 |
| `student-assignments.html` | 通过 | 命中学生作业接口 |
| `student-notifications.html` | 通过 | 命中学生通知接口 |
| `student-settings.html` | 通过 | 命中学生资料、通知设置、隐私设置接口 |
| `student-stats.html` | 通过 | JWT-only 环境下优先加载已接通的统计与作业数据，并可稳定回退 |

2026-05-23 追加说明：

- 教师端 `teacher-dashboard / teacher-courses / teacher-assignments` 已接入共享
  [common-ui.js](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/common-ui.js:1)
  中的教师会话检查、侧边栏加载和用户下拉逻辑。
- 教师端 `teacher-dashboard / teacher-courses` 已继续把重复的 toast 通知、资源加载失败提示、按钮级加载态收进
  [common-ui.js](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/common-ui.js:1)，
  进一步降低页面内联壳层脚本体积，同时保持页面函数入口不变。
- [teacher-courses-assignments.js](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-courses-assignments.js:1)
  已承接 `teacher-courses` 中的课程分配子模块，实现班级下拉加载、分配提交、分配列表分页、取消分配与筛选逻辑外置，
  页面仍保留原有 `onclick`/分页入口，因此现有 smoke 和 CRUD 自动化无需调整。
- [teacher-courses-students.js](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-courses-students.js:1)
  已继续承接 `teacher-courses` 中的学生弹窗子模块，实现“查看班级学生 / 添加学生到当前班级”逻辑外置，
  页面保留原有模态框和按钮入口；刷新开发会话后，教师 JWT 页面 smoke 与教师浏览器 CRUD smoke 重新通过。
- [teacher-courses-class-edit.js](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-courses-class-edit.js:1)
  已继续承接 `teacher-courses` 中的班级编辑子模块，实现“打开编辑模态框 / 表单实时校验 / 提交更新班级”逻辑外置，
  页面保留原有按钮和模态框入口；同步到 Spring 静态目录后，教师 JWT 页面 smoke 与教师浏览器 CRUD smoke 继续通过。
- [teacher-courses-course-crud.js](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-courses-course-crud.js:1)
  已继续承接 `teacher-courses` 中的课程 CRUD 子模块，实现“新增课程 / 打开编辑课程模态框 / 提交编辑课程 / 删除课程”逻辑外置，
  页面保留原有按钮与模态框入口；同步到 Spring 静态目录后，教师 JWT 页面 smoke 与教师浏览器 CRUD smoke 继续通过。
- [teacher-assignments-assignment-crud.js](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-assignments-assignment-crud.js:1)
  已开始承接 `teacher-assignments` 中的作业 CRUD 子模块，实现“查看作业 / 编辑作业 / 删除作业 / 提交编辑作业”逻辑外置，
  页面保留原有按钮与模态框入口；同步到 Spring 静态目录后，教师 JWT 页面 smoke 与教师浏览器 CRUD smoke 继续通过。
- [teacher-assignments-exam-crud.js](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-assignments-exam-crud.js:1)
  已继续承接 `teacher-assignments` 中的考试 CRUD 子模块，实现“查看考试 / 编辑考试 / 删除考试 / 提交编辑考试”逻辑外置，
  页面保留原有按钮与模态框入口；刷新开发会话后，教师 JWT 页面 smoke 与教师浏览器 CRUD smoke 重新通过。
- [teacher-assignments-exam-publish.js](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-assignments-exam-publish.js:1)
  已继续承接 `teacher-assignments` 中的考试发布与课程/知识点选项加载子模块，实现“发布考试 / 加载课程下拉选项 / 加载知识点 / 绑定模态框与课程切换事件”逻辑外置，
  页面保留原有发布入口与模态框交互；同步到 Spring 静态目录后，教师 JWT 页面 smoke 与教师浏览器 CRUD smoke 继续通过。
- [teacher-assignments-submission-filters.js](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-assignments-submission-filters.js:1)
  已继续承接 `teacher-assignments` 中的提交记录筛选与作业/考试下拉缓存子模块，实现“缓存作业考试选项 / 填充提交记录筛选下拉 / 提交记录搜索按钮绑定”逻辑外置，
  页面保留原有提交记录 tab 和筛选交互；同步到 Spring 静态目录后，教师 JWT 页面 smoke 与教师浏览器 CRUD smoke 继续通过。
- 前端静态同步脚本
  [scripts/sync-frontend-to-static.ps1](D:/111/Distributed framework technology/JavaCode/majorassignment/scripts/sync-frontend-to-static.ps1:1)
  已补充 `teacher-courses-assignments.js`、`teacher-courses-students.js`、`teacher-courses-class-edit.js`、`teacher-courses-course-crud.js`、`teacher-assignments-assignment-crud.js`、`teacher-assignments-exam-crud.js`、`teacher-assignments-exam-publish.js` 与 `teacher-assignments-submission-filters.js` 白名单覆盖，`-CheckOnly` 现可正确校验新增外链脚本是否同步进 Spring 静态目录。
- `5500` 前端预览链路依赖的 Gateway 现已提供
  `GET /api/frontend/capabilities`，
  对齐单体静态运行时的能力矩阵接口，避免页面在本地预览链路下因为 404 误判为功能回退。
- 教师端 smoke 脚本已增加 JWT 临期检查，若登录态过期会优先报“会话过期”，不再混淆成页面故障。

### 4.4 Agent 流式输出交付状态

Agent 聊天链路已新增流式接口：

| 接口 | 返回类型 | 说明 |
| --- | --- | --- |
| `POST /api/agent/chat` | `application/json` | 原同步聊天接口，继续作为兼容兜底 |
| `POST /api/agent/chat/stream` | `text/event-stream` | 新增 SSE 流式聊天接口，经 Gateway 统一暴露 |

SSE 事件约定：

| 事件 | 用途 |
| --- | --- |
| `start` | 流式请求已建立并开始处理，用于尽早下发首包 |
| `session` | 返回或刷新当前 Agent 会话 ID |
| `delta` | 推送当前回答文本片段 |
| `result` | 推送最终结构化 `AgentChatResponseDTO`，用于渲染 `TEXT`、`DATA` 或 `ACTION_PREVIEW` |
| `error` | 推送流式处理失败信息 |
| `done` | 标记本次流式响应结束 |

前端共享组件 `agent-chat-panel.js` 会优先调用 `/api/agent/chat/stream`。如果流式接口在尚未收到有效数据前不可用，会自动回退到原 `/api/agent/chat`，因此旧环境不会直接阻断教师端 `teacher-ai-tools.html` 或学生端 `student-ai-assistant.html` 的 Agent 使用。`frontend/dist` 与 `major_assignment/src/main/resources/static` 两份静态资源已同步，页面引用版本为 `20260620-agent-stream-1`。

2026-06-21 本轮流式链路最终闭环时，已完成两处关键修复：

- 前端本地代理 [scripts/frontend_dev_server.py](D:/111/Distributed framework technology/JavaCode/majorassignment/scripts/frontend_dev_server.py:1) 不再先 `read()` 完整上游响应再返回浏览器，而是对 `text/event-stream` 逐块转发并显式关闭缓冲。
- 后端 [DefaultAgentChatStreamingService.java](D:/111/Distributed framework technology/JavaCode/majorassignment/agent-service/src/main/java/com/_202510007517/platform/agent/service/DefaultAgentChatStreamingService.java:1) 在真正执行 `orchestrator.chat(...)` 前先发送 `start` 事件，避免长耗时 LLM 分类/编排阶段把浏览器首包拖到最终结果之后。

本机运行时验证链路为：

- `5500 frontend proxy -> 8080 gateway -> 8092 agent-service`
- 重新生成 fresh `teacher7` / `student42` JWT 会话
- 复跑 `node scripts/verify-agent-frontend-browser-smoke.js`

结果：教师和学生 Agent 面板都已按真实流式链路通过浏览器烟测，包含读操作、写预览、确认执行、考试提交、作业提交、通知发送、通知已读更新等关键交付路径。

当前流式实现是可交付的渐进式通道：后端先沿用现有 Agent 编排结果，再通过 SSE 发送 `delta` 与 `result`。这保证前端具备真实流式消费、取消、结果渲染和降级能力；后续如接入真正 token-by-token 模型流，只需要替换 `AgentChatStreamingService` 内部产生 `delta` 的方式，不需要重做前端协议。

本机低内存演示建议：

```powershell
$env:AGENT_RAG_ENABLED='false'
java -jar agent-service\target\agent-service-*.jar
```

原因：当前机器启用 Ollama/RAG 嵌入链路时曾出现 JVM native memory 分配失败。本交付保留 RAG 代码和开关，但低内存本地演示优先关闭 RAG，保证 Agent 工具编排、聊天、动作预览、确认和流式前端链路稳定展示。

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

## 7. 前端同步约定

当前静态前端仍同时存在于以下两个目录：

- `frontend/dist`
- `major_assignment/src/main/resources/static`

为避免“本地静态预览”和“Spring Boot 实际返回页面”再次漂移，本仓库已补充同步脚本：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\sync-frontend-to-static.ps1
```

仅检查是否一致：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\sync-frontend-to-static.ps1 -CheckOnly
```

约定如下：

1. 日常前端修改优先落在 `frontend/dist`
2. 提交前运行一次同步脚本
3. 若要校验交付状态，先跑 `-CheckOnly`，再跑浏览器 smoke

## 变更记录

| 日期 | 变更人 | 变更内容 |
| --- | --- | --- |
| 2026-06-21 | Codex | 补充 Agent 流式输出交付说明，记录 `/api/agent/chat/stream`、`start/session/delta/result/error/done` 事件约定、前端 SSE 优先与同步接口兜底、SSE 代理与首包修复、低内存本机关闭 RAG 演示口径，以及 Agent 定向 Maven / 前端契约 / 浏览器 smoke 最终通过结果 |
| 2026-05-21 | Codex | 新增最终交付说明，记录启动、账号、端口、烟测结果与 Flyway 本地注意事项 |
| 2026-05-23 | Codex | 补充学生学习数据页 smoke 覆盖、学生端公共逻辑抽离进展与最新验证记录 |
| 2026-05-23 | Codex | 补充 Gateway 前端能力矩阵接口、教师端公共逻辑接入与教师 smoke 最新验证记录 |
| 2026-05-23 | Codex | 继续记录教师端 UI helper 抽离进展，新增共享 toast / 资源失败提示 / 按钮加载态落地说明 |
| 2026-05-23 | Codex | 继续记录 teacher-courses 结构拆分进展，新增课程分配模块独立脚本与对应回归验证结果 |
| 2026-05-23 | Codex | 继续记录 teacher-courses 学生弹窗模块独立脚本化进展，并补充同步脚本白名单与刷新会话后的教师端回归验证结果 |
| 2026-05-23 | Codex | 继续记录 teacher-courses 班级编辑模块独立脚本化进展，并补充同步脚本白名单与最新教师端回归验证结果 |
| 2026-05-24 | Codex | 继续记录 teacher-courses 课程 CRUD 模块独立脚本化进展，并补充同步脚本白名单与最新教师端回归验证结果 |
| 2026-05-24 | Codex | 开始记录 teacher-assignments 作业 CRUD 模块独立脚本化进展，并补充同步脚本白名单与最新教师端回归验证结果 |
| 2026-05-24 | Codex | 继续记录 teacher-assignments 考试 CRUD 模块独立脚本化进展，并补充同步脚本白名单与刷新开发会话后的教师端回归验证结果 |
| 2026-05-24 | Codex | 继续记录 teacher-assignments 考试发布与课程/知识点选项加载模块独立脚本化进展，并补充同步脚本白名单与最新教师端回归验证结果 |
| 2026-05-24 | Codex | 继续记录 teacher-assignments 提交记录筛选与作业/考试下拉缓存模块独立脚本化进展，并补充同步脚本白名单与最新教师端回归验证结果 |
| 2026-05-24 | Codex | 继续记录 teacher-assignments 评分/批改模块独立脚本化进展，补充 `teacher-assignments-grading.js`、同步脚本白名单与对应契约验证脚本更新 |
| 2026-05-24 | Codex | 继续记录 teacher-assignments 页面壳层模块独立脚本化进展，补充 `teacher-assignments-shell.js`、同步脚本白名单与最新教师端回归验证结果 |
| 2026-05-24 | Codex | 继续记录 `api.js` 中 teacher-assignments 重复搜索兼容层去重进展，保留单份最小兼容实现并复用 shell 模块参数构造逻辑 |
| 2026-05-24 | Codex | 补齐教师仪表盘作业入口本地化收口，新增 dashboard 级 `deleteAssignment` 并验证同步脚本、教师端 JWT smoke 与 CRUD smoke 继续通过 |
| 2026-05-24 | Codex | 补齐 teacher-assignments 壳层对 `assignmentId` 入口参数的消费链路，并扩展页面入口契约脚本以覆盖教师仪表盘跳转到作业工作区的查看/编辑动作 |
| 2026-05-24 | Codex | 继续记录 teacher-assignments 列表加载/渲染层独立脚本化进展，新增 `teacher-assignments-lists.js`、同步脚本白名单与列表层契约脚本，并验证教师端 smoke 继续通过 |
| 2026-05-24 | Codex | 继续记录 teacher-assignments 旧 search/reset/initTabs 壳迁移进展，补充 shell 模块兼容入口并移除 api.js 中重复 tab 初始化，验证教师端 smoke 继续通过 |
| 2026-05-24 | Codex | 继续记录 teacher-assignments 提交记录搜索职责收敛进展，改由 shell 模块统一承接搜索入口，submission-filters 仅保留缓存职责，并移除 api.js 中已无消费者的 search 兼容函数 |
| 2026-05-24 | Codex | 继续记录 teacher-assignments 页面级统计刷新链补齐进展，新增 `refreshTeacherAssignmentsSummary()` 并移除 api.js 中旧 `loadStats / updateStatCard / window DOMContentLoaded` 初始化链与重复导出函数 |
| 2026-05-24 | Codex | 继续记录 api.js 中 teacher 死层清理进展，移除已无消费者的 `init*Search` 空函数、指向不存在页面的 `teacher-exams / teacher-submissions` 分支与不再使用的 `loadDashboardStats()` 旧入口 |
| 2026-05-24 | Codex | 继续记录 teacher-assignments 作业发布流独立脚本化进展，新增 `teacher-assignments-assignment-publish.js`、发布流契约脚本与最新教师端 smoke 验证结果 |
| 2026-05-24 | Codex | 开始记录 teacher-courses 课程搜索/列表/分页主链独立脚本化进展，新增 `teacher-courses-courses.js`、课程页模块契约脚本与最新教师端 smoke 验证结果 |
| 2026-05-24 | Codex | 继续记录 teacher-courses 课程页旧宿主代码收口进展，移除页面内联课程搜索/列表/分页旧实现，并升级课程页契约脚本校验新模块接管与旧函数移除 |
| 2026-05-24 | Codex | 继续记录 teacher-courses 课程分页遗留链清理进展，移除旧课程分页点击监听与 `handleCoursePaginationClick / updateCoursePaginationUI`，保留班级分页初始化并验证教师端 smoke 继续通过 |
| 2026-05-24 | Codex | 继续记录 teacher-courses 页面遗留状态去重进展，移除重复 `DOMContentLoaded -> initPagination()` 监听与无消费者的课程页全局状态，仅保留班级搜索历史字段并验证教师端 smoke 继续通过 |
| 2026-05-24 | Codex | 继续记录 teacher-courses 班级搜索/列表/分页主链独立脚本化进展，新增 `teacher-courses-classes.js`、班级页契约脚本与 `initPagination` 启动兼容别名，并验证教师端 smoke 继续通过 |
| 2026-05-24 | Codex | 继续记录 teacher-courses 班级新增/删除 CRUD 独立脚本化进展，新增 `teacher-courses-class-crud.js`、班级 CRUD 契约脚本，并移除页面中无消费者的班级表单辅助函数后验证教师端 smoke 继续通过 |
| 2026-05-24 | Codex | 继续记录 teacher-courses 课程/班级表单验证壳独立脚本化进展，新增 `teacher-courses-validation.js`、验证契约脚本，并移除页面内联唯一性校验与表单验证实现后验证教师端 smoke 继续通过 |
| 2026-05-24 | Codex | 继续记录 teacher-courses 元数据与 modal 预加载壳独立脚本化进展，新增 `teacher-courses-metadata.js`、元数据契约脚本，并移除页面内联课程选项/年级专业元数据/`shown.bs.modal` 绑定实现后验证教师端 smoke 继续通过 |
| 2026-05-24 | Codex | 继续记录 teacher-courses 页面壳层独立脚本化进展，新增 `teacher-courses-shell.js`、shell 契约脚本，并将页面启动切换为 `bootstrapTeacherCoursesPage()` 后验证教师端 smoke 继续通过 |
| 2026-05-24 | Codex | 继续记录 teacher-courses 页面尾部通用 helper 壳收口进展，将通知/API 错误/按钮 loading/searchHistory/侧边栏快捷切换并入 `teacher-courses-shell.js`，移除页面内联 helper 并验证教师端 smoke 继续通过 |
| 2026-05-24 | Codex | 继续记录 teacher-courses 页内宿主壳收口进展，将考核方式下拉加载、教师用户下拉代理、浏览器错误展示/全局错误捕获、侧边栏折叠绑定并入 `teacher-courses-shell.js`，移除页面内联宿主脚本并验证教师端 smoke 继续通过 |
| 2026-05-24 | Codex | 开始记录后端 Controller 收口进展，为 `TeacherDashboardController` 增加类级教师鉴权注解并抽离 `"all" -> null` 参数归一化 helper，补充 `TeacherDashboardControllerTest` 契约后通过 Maven 定向测试验证 |
| 2026-05-24 | Codex | 继续记录 `TeacherDashboardController` 重复鉴权壳清理进展，移除 15 处手写 `isLoggedIn -> 401` 分支并交由 `AuthenticationAspect` 统一处理，定向 Maven 回归继续通过 |
| 2026-05-24 | Codex | 继续记录 `AssignmentController` 统一教师鉴权接入进展，新增类级教师鉴权注解并移除 11 处手写 `isLoggedIn -> 401` 分支，补充 `AssignmentControllerTest` 后通过 Maven 定向回归 |
| 2026-05-24 | Codex | 继续记录 `ExamController` 统一教师鉴权接入进展，新增类级教师鉴权注解并移除 11 处手写 `isLoggedIn -> 401` 分支，补充 `ExamControllerTest` 后通过 Maven 定向回归 |
| 2026-05-24 | Codex | 开始记录第二层后端重复业务壳收口进展，在 `BaseController` 新增 `resolveTeacherCourseId()` 并让 `AssignmentController` / `ExamController` 复用课程 ID / 课程代码解析逻辑，补充共享 helper 测试后通过定向 Maven 回归 |
| 2026-05-24 | Codex | 继续记录第二层后端重复业务壳收口进展，在 `BaseController` 新增 `resolveTeacherCourseIdFromPayload()`，统一请求体 `courseId / course_id` 解析并让 `AssignmentController / ExamController` 的创建与更新接口复用，补充共享 helper 测试后通过定向 Maven 回归 |
| 2026-05-24 | Codex | 继续记录第二层后端重复业务壳收口进展，在 `BaseController` 新增 `calculateTotalPages()` 与 `boundPageNum()`，统一分页条数夹紧与页码越界兜底规则，并让 `AssignmentController / ExamController` 的列表与提交记录接口复用后通过定向 Maven 回归 |
| 2026-05-24 | Codex | 继续记录第二层后端重复业务壳收口进展，在 `BaseController` 新增 `buildSpringPageResponse()`，统一 Spring 风格分页响应拼装，并让 `AssignmentController` 的作业列表/提交记录接口与 `ExamController` 的考试列表接口复用后通过定向 Maven 回归 |
| 2026-05-24 | Codex | 继续记录第二层后端重复业务壳收口进展，在 `BaseController` 新增作业/考试状态 token 归一化 helper，统一处理中英文别名、大小写与空白输入，并让 `AssignmentController / ExamController` 的状态筛选入口复用后通过定向 Maven 回归 |
| 2026-05-24 | Codex | 继续记录 `TeacherDashboardController` 分页收口进展，`/api/teacher/course-assignments` 已复用共享分页 helper 并修复越界页码导致的末页空白 bug，`/api/teacher/submissions` 已统一返回 Spring 风格分页结构，并通过扩展后的 `TeacherDashboardControllerTest` 与 36 项 Maven 定向回归 |
| 2026-05-24 | Codex | 继续记录 `ExamController` 分页收口进展，`/api/teacher/exams/submissions` 已切换复用共享 Spring 风格分页响应，替代旧 `submissions/total/page/pages` 结构，并通过扩展后的 `ExamControllerTest` 与 37 项 Maven 定向回归 |
| 2026-05-24 | Codex | 继续记录 `CourseController` 契约收口进展，`/api/teacher/courses` 已从伪分页改为真实裁切并统一复用共享分页夹紧规则；课程页已改为直接消费后端分页结果，全量课程下拉/校验入口已显式请求 `page=1&size=100`，并通过 `CourseControllerTest`、教师端 JWT smoke 与 CRUD smoke 回归 |
| 2026-05-24 | Codex | 继续记录 `CourseController` 教师鉴权收口进展，课程相关接口已从方法级重复 `@RequireLogin(TEACHER)` 切换为类级教师鉴权注解统一承接，`CourseControllerTest` 同步改为校验类级 guard 与方法级去重，并通过 40 项 Maven 定向回归 |
| 2026-05-24 | Codex | 继续记录预警列表分页边界收口进展，`EarlyWarningServiceImpl#getWarningList` 已改为复用共享分页夹紧规则并修复越界页码导致的末页空白问题；新增 `EarlyWarningServiceImplTest` 后通过 42 项 Maven 定向回归、教师端 JWT smoke 与 CRUD smoke |
| 2026-05-24 | Codex | 继续记录共享分页层收口进展，`PageUtils` 已升级为中立分页核心，`BaseController` 改为转调它提供分页 helper，`EarlyWarningServiceImpl` 不再通过继承 controller 基类复用分页逻辑；相关分页测试与教师端 smoke 均通过 |
| 2026-05-24 | Codex | 继续记录 `PageResult` 构建统一进展，`PageUtils` 已新增 `buildPageResult(...)`，`CourseController` 与 `EarlyWarningServiceImpl` 改为复用共享 helper 生成分页 DTO，去除重复字段赋值代码；相关分页测试、课程控制器测试与教师端 smoke 均通过 |
| 2026-05-24 | Codex | 继续记录浏览器错误分页契约收口进展，`BrowserErrorServiceImpl` 已复用 `PageUtils` 统一页大小夹紧、越界页码回落与 `PageResult` 构建，`BrowserErrorController` 改为直接返回 `ResponseResult<PageResult<BrowserError>>`，并通过 `BrowserErrorControllerTest` 与 43 项 Maven 定向回归验证 |
| 2026-05-24 | Codex | 继续记录学生通知分页边界修复进展，`NotificationServiceImpl#getNotificationsWithPagination` 已复用 `PageUtils` 统一页大小夹紧、越界页码回落与内存分页，同时保持学生端依赖的 `notifications/total/page/size/totalPages` 结构不变；新增 `NotificationServiceImplTest` 并通过 45 项 Maven 定向回归验证 |
| 2026-05-24 | Codex | 继续记录学生课程分页重复壳收口进展，`StudentServiceImpl#getStudentCoursesWithPagination` 已改为直接复用 `PageUtils.buildPageResponse(...)`，统一页大小夹紧、越界页码回落与空结果 `totalPages=0` 口径，同时保持学生课程页依赖的 Spring 风格分页 shape 不变；新增 `StudentServiceImplTest` 并通过 50 项 Maven 定向回归、学生端 CRUD smoke 与教师端 CRUD smoke 验证 |
| 2026-05-24 | Codex | 继续记录学生/教师提交流服务分页底座收口进展，`AssignmentSubmissionServiceImpl#getSubmissionsWithPagination` 与 `ExamSubmissionServiceImpl#getSubmissionsWithPagination` 已统一复用 `PageUtils` 的页大小夹紧、越界页码回落与 offset 计算逻辑，替代原先裸 `(page - 1) * size` 实现；新增 `AssignmentSubmissionServiceImplTest` 与 `ExamSubmissionPaginationTest` 并通过 53 项 Maven 定向回归、学生端 CRUD smoke 与教师端 CRUD smoke 验证 |
| 2026-05-24 | Codex | 继续记录 teacher 侧内存列表分页共享出口收口进展，`BaseController` 已新增 `buildSpringPageResponseFromInMemoryList(...)`，`AssignmentController#getAssignments`、`ExamController#getExams` 与 `TeacherDashboardController#getClassAssignments` 已移除手写 `subList/skip/limit` 分页实现并统一复用共享 helper；新增 `BaseControllerInMemoryPageTest` 并通过 55 项 Maven 定向回归、学生端 CRUD smoke 与教师端 CRUD smoke 验证 |
| 2026-05-24 | Codex | 继续记录 teacher 侧内存列表 `PageResult` 出口收口进展，`BaseController` 已新增 `buildPageResultFromInMemoryList(...)`，`CourseController#getCourses` 已移除手写 `subList + PageUtils.buildPageResult(...)` 逻辑并统一复用共享 helper；扩展 `BaseControllerInMemoryPageTest` 与 `CourseControllerTest` 后，57 项 Maven 定向回归继续通过 |
| 2026-05-24 | Codex | 继续记录分页窗口重复壳收口进展，`PageUtils` 已新增 `PageWindow` 与 `resolvePageWindow(...)`，统一输出 `safePage / safeSize / totalPages / offset` 这组高频分页元数据；`StudentServiceImpl#getStudentCoursesWithPagination`、`AssignmentSubmissionServiceImpl#getSubmissionsWithPagination`、`ExamSubmissionServiceImpl#getSubmissionsWithPagination` 已切换复用共享分页窗口，并通过新增 `PageUtilsTest` 与 59 项 Maven 定向回归验证 |
| 2026-05-24 | Codex | 继续记录分页窗口向 controller/service 壳层推进进展，`BaseController` 已新增 `resolvePageWindow(...)` 代理，`AssignmentController#getAllSubmissions`、`ExamController#getAllSubmissions`、`TeacherDashboardController#getAllSubmissions`、`NotificationServiceImpl#getNotificationsWithPagination` 与 `BrowserErrorServiceImpl#getBrowserErrorList` 已统一复用共享分页窗口，进一步移除本地 `safePage/safeSize/offset/totalPages` 重复计算；扩展 `BaseControllerPaginationTest` 后，60 项 Maven 定向回归继续通过 |
| 2026-05-24 | Codex | 继续记录分页窗口向学生列表与预警链路推进进展，`AssignmentServiceImpl#getAssignmentsWithPagination`、`ExamServiceImpl#getExamsWithPagination` 与 `EarlyWarningServiceImpl#getWarningList` 已切换复用 `PageUtils.resolvePageWindow(...)`，替代本地分页窗口计算并保持原有返回 shape 不变；新增 `AssignmentServiceImplTest` 与 `ExamServiceImplTest` 后，62 项 Maven 定向回归继续通过 |
| 2026-05-24 | Codex | 继续记录 teacher 提交记录链路重复查询收口进展，`AssignmentSubmissionService#getSubmissionsWithPagination` 与 `ExamSubmissionService#getSubmissionsWithPagination` 已改为接收 controller 已计算的 `total`，避免在 service 内再次执行 `countSubmissions(...)`；`AssignmentController`、`ExamController` 与 `TeacherDashboardController` 调用面已同步切换，相关 service/controller 测试更新后 62 项 Maven 定向回归继续通过 |
| 2026-05-24 | Codex | 继续记录学生作业/考试列表同页重复查找收口进展，`AssignmentServiceImpl#getAssignmentsWithPagination` 与 `ExamServiceImpl#getExamsWithPagination` 已新增单页局部课程/教师缓存，避免同一页对相同 `courseId` 和 `teacherId` 重复执行 `courseService.findById(...)` / `userService.findById(...)`；新增 N+1 防回退测试后，64 项 Maven 定向回归继续通过 |
| 2026-05-24 | Codex | 继续记录教师学情汇总中同学生重复班级名查询收口进展，`TeacherDashboardServiceImpl#getStudentLearningSummary` 已将 `userMapper.getStudentClassName(studentId)` 从课程内层循环移到学生粒度，避免同一学生关联多门课时重复查询同一班级名；新增 `TeacherDashboardServiceImplTest` 后，65 项 Maven 定向回归继续通过 |
| 2026-05-24 | Codex | 继续记录教师仪表盘多课程场景逐课程学生人数查询收口进展，`TeacherDashboardServiceImpl#getDashboardData` 在“无班级筛选 + 多门课程”路径下已切换为复用 `CourseMapper.batchGetStudentCountByCourseIds(...)`，避免逐课程重复执行 `getStudentCountByCourseId(...)`；扩展 `TeacherDashboardServiceImplTest` 后，66 项 Maven 定向回归继续通过 |
| 2026-05-24 | Codex | 继续记录教师仪表盘平均分覆盖 bug 修复进展，`TeacherDashboardServiceImpl#getDashboardData` 已移除后段重复的无筛选课程平均分重算逻辑，避免带 `classId` 时前面通过 `getCourseAverageScoreByClassId(...)` 得到的结果被覆盖；扩展 `TeacherDashboardServiceImplTest` 后，67 项 Maven 定向回归继续通过 |
| 2026-05-24 | Codex | 继续记录教师仪表盘多课程场景逐课程平均分查询收口进展，`CourseMapper` / `CourseMapper.xml` 已新增 `batchGetCourseAverageScoresByCourseIds(...)`，`TeacherDashboardServiceImpl#getDashboardData` 现已在“无班级筛选 + 多门课程”路径下复用批量平均分结果，避免逐课程重复执行 `getCourseAverageScore(...)`；扩展 `TeacherDashboardServiceImplTest` 后，67 项 Maven 定向回归继续通过 |
| 2026-05-24 | Codex | 继续记录教师仪表盘最近预警活动链路收口进展，`EarlyWarningMapper.findRecentByTeacherId(...)` 已直接联表返回 `studentName`，`TeacherDashboardServiceImpl#getDashboardData` 不再逐条调用 `userMapper.findById(...)` 补学生姓名；同时修复 `EarlyWarning.triggerDate` 为 `LocalDateTime` 时 recent activities 格式化失败并回退成“系统提示”的 bug。扩展 `TeacherDashboardServiceImplTest` 后，68 项 Maven 定向回归继续通过 |
| 2026-05-24 | Codex | 同步教师仪表盘变化指标覆盖 bug 修复进展，记录 `TeacherDashboardServiceImpl#getDashboardData` 已移除尾段对 `pendingAssignmentsChange / pendingExamsChange / missingSubmissionsChange / upcomingDeadlinesChange / warningCountChange` 的“较上周”二次覆盖，确保前端“较昨日”文案与实际返回值一致；扩展 `TeacherDashboardServiceImplTest` 后，69 项 Maven 定向回归继续通过 |
| 2026-05-24 | Codex | 同步教师学习汇总统计口径修复进展，记录 `TeacherDashboardServiceImpl#getStudentLearningSummary` 已改为按学生聚合 summary 级统计，不再因同一学生挂多门课而重复累加 `averageScore / totalPendingAssignments / overallProgress`，同时保留 `studentPerformances` 按“学生 × 课程”展开的表格行语义；扩展 `TeacherDashboardServiceImplTest` 后，70 项 Maven 定向回归继续通过 |
| 2026-05-24 | Codex | 同步教师学情页前端统计口径修复进展，记录 `teacher-student-dashboard.html` 已将课程行分页与真实学生数展示拆开处理，标题与分数段统计改为基于去重 `studentId` 展示“x名学生 / y条课程记录”，避免把 `studentPerformances.length` 误当学生数；新增 `verify-teacher-student-dashboard-contract.js` 并通过定向验证 |
| 2026-05-24 | Codex | 同步教师学情页多课程行级语义修复进展，记录 `teacher-student-dashboard.html` 的导出 Excel 已补齐课程列，查看/编辑按钮也会携带 `courseName/className` 参与当前行定位，避免同一学生多门课时详情/编辑串行或导出成无课程上下文的重复记录；`verify-teacher-student-dashboard-contract.js` 已同步加严并通过 |
| 2026-05-24 | Codex | 同步教师学情页详情弹窗课程行语义修复进展，记录 `teacher-student-dashboard.html` 已将详情弹窗调整为“课程行字段优先、学生级详情接口补充通用资料”的合并顺序，并新增“当前课程”字段，避免同一学生多门课时详情弹窗被学生级接口数据覆盖回错误课程语境；`verify-teacher-student-dashboard-contract.js`、教师端 JWT smoke 与 CRUD smoke 已通过 |
| 2026-05-24 | Codex | 同步教师学情页详情弹窗趋势上下文修复进展，记录 `TeacherDashboardController#getScoreTrend` / `TeacherDashboardServiceImpl#getScoreTrend` / `AssignmentSubmissionMapper#getAssignmentScoreTrend` 已补齐可选 `studentId` 过滤，`teacher-student-dashboard.html` 也会把当前行 `courseId` 一并传入并正确解析 `ScoreTrendDTO[]` 绘图，避免把更大范围的趋势误当当前学生当前课程趋势；扩展相关测试、契约脚本并通过教师端 JWT smoke 与 CRUD smoke |
| 2026-05-24 | Codex | 同步教师学情页多课程行定位进一步收口进展，记录 `teacher-student-dashboard.html` 已将 `findStudentRow(...)`、详情弹窗与编辑弹窗的行匹配逻辑升级为 `courseId` 优先，再回退到 `courseName/className`，避免同名课程或同班重复时误命中错误课程行；契约脚本、教师端 JWT smoke 与 CRUD smoke 已通过 |
| 2026-05-24 | Codex | 同步教师学情页编辑链多课程上下文收口进展，记录 `teacher-student-dashboard.html` 已将编辑弹窗的 `courseId/courseName/className` 显式写入 `editStudentForm.dataset`，`saveStudentEdit()` 的本地回写与确认重试统一从表单上下文读取，避免依赖外层作用域隐式变量导致多课程场景串线或潜在运行时错误；契约脚本、教师端 JWT smoke 与 CRUD smoke 已通过 |
| 2026-05-24 | Codex | 同步教师学情页编辑链 `courseId` shadowing bug 修复进展，记录 `editStudent(...)` 已将筛选值统一改名为 `selectedCourseId`，避免覆盖行级 `courseId` 并误按筛选课程而非当前课程行定位学生数据；契约脚本、教师端 JWT smoke 与 CRUD smoke 已通过 |
| 2026-05-24 | Codex | 同步教师学情页编辑后本地一致性提升进展，记录 `saveStudentEdit()` 已将即时本地回写从单条课程行扩展为按 `studentId` 批量同步该学生在当前表格中的所有课程行，使姓名/班级等学生级字段在多课程场景下先于全量 reload 保持一致；契约脚本、教师端 JWT smoke 与 CRUD smoke 已通过 |
| 2026-05-24 | Codex | 同步教师学情页分数段下钻时间范围口径修复进展，记录 `showStudentsByScoreRange(...)` 已改为继承当前 `timeRangeSelect` 并把 `timeRange` 一并带入 `/api/teacher/learning-summary`，避免成绩分布图与点击下钻后的学生列表在“本周/本月/本学期”切换后使用不同时间范围；`verify-teacher-student-dashboard-contract.js`、教师端 JWT smoke 与 CRUD smoke 已通过 |
| 2026-05-24 | Codex | 同步教师学情页成绩分布图展示语义修复进展，记录当前成绩分布仍基于 `studentPerformances` 的课程记录分桶，因此图卡标题、图例、tooltip 与柱状图 y 轴已统一改为“课程记录数/条记录”，避免把课程行计数误表述为学生人数；`verify-teacher-student-dashboard-contract.js`、教师端 JWT smoke 与 CRUD smoke 已通过 |
| 2026-05-24 | Codex | 同步教师学情页趋势图重绘链 `timeRange` 传递 bug 修复进展，记录查询与重置筛选后已改为使用当前 `timeRangeSelect` 调用 `initScoreTrendChart(timeRange)`，不再误把 `scoreTrendChart.config.type` 这类图表类型值当成时间范围传入，避免趋势图在筛选后悄悄退回默认月度口径；`verify-teacher-student-dashboard-contract.js`、教师端 JWT smoke 与 CRUD smoke 已通过 |
| 2026-05-24 | Codex | 同步教师学情页趋势图时间范围状态一致性修复进展，记录趋势图卡片上的“周/月/学期”按钮现在会同步回写顶部 `timeRangeSelect`，且 `initCharts()` 已改为优先读取共享下拉值而非旧 active 按钮，避免按钮高亮、下拉值和实际趋势数据三者错位；`verify-teacher-student-dashboard-contract.js`、教师端 JWT smoke 与 CRUD smoke 已通过 |
| 2026-05-24 | Codex | 同步教师学情页趋势图控件联动修复进展，记录已新增 `syncScoreTrendTimeRangeControls(...)` 统一同步顶部 `timeRangeSelect` 与趋势图按钮 active 状态；现在趋势按钮点击、下拉变更、查询、重置、刷新以及趋势图初始化都会走同一套时间范围状态源，避免 UI 停在“周”而实际数据已切到“月/学期”的错位；`verify-teacher-student-dashboard-contract.js`、教师端 JWT smoke 与 CRUD smoke 已通过 |
| 2026-05-24 | Codex | 同步教师学情页图卡交互与趋势语义收口进展，记录图卡切换逻辑已从标题文本判断切到稳定 `data-chart-kind` 分流，避免标题改名牵连行为；同时趋势图标题已明确为“当前筛选平均成绩趋势”，dataset label 与 tooltip 也统一为“当前筛选平均成绩”，让标题、坐标轴和悬浮提示一致表达班级/课程筛选后的聚合趋势语义；`verify-teacher-student-dashboard-contract.js`、教师端 JWT smoke 与 CRUD smoke 已通过 |
| 2026-05-24 | Codex | 同步教师学情汇总 `courseId/timeRange` 真正下推到底层查询的修复进展，记录 `TeacherDashboardServiceImpl#getStudentLearningSummary` 已不再复用只按 `studentId` 聚合的 `studentMapper.getStudentPerformance(...)`，而是改用新增的 `StudentMapper#getTeacherStudentCoursePerformance(...)` 按“学生 × 课程 × 时间范围”返回 `averageScore / pendingAssignments / overallProgress`，从而修正 `/api/teacher/learning-summary` 的时间范围假筛选；扩展 `TeacherDashboardServiceImplTest` 后，73 项 Maven 定向回归、前端契约脚本、教师端 JWT smoke 与 CRUD smoke 已通过 |
| 2026-05-24 | Codex | 同步教师学情页 `quarter / 最近三个月` 趋势链收口进展，记录趋势图已在顶部筛选、卡片按钮、控件同步 helper、默认占位数据与日期标签格式上统一支持 `quarter`，后端 `TeacherDashboardServiceImpl#getScoreTrend(...)` 也已按最近 90 天窗口真实出数，不再默认回退到 `month`；扩展 `TeacherDashboardServiceImplTest`、`TeacherDashboardControllerTest`、前端契约脚本后，教师端 JWT smoke 与 CRUD smoke 继续通过 |
| 2026-05-24 | Codex | 同步教师学情汇总 `semester / 本学期` 时间范围语义修复进展，记录 `StudentMapper.xml` 中 `getTeacherStudentCoursePerformance(...)` 之前只对 `week/month/quarter` 做真实时间裁切，`semester` 分支没有按时间过滤，导致“本学期”实际更接近全量历史；现已补齐 `assignment_submissions` 与 `exam_submissions` 侧的 `semester -> 最近 4 个月` 过滤，并新增 `StudentMapperXmlContractTest` 防回退，相关 service/controller 测试、前端契约脚本与教师端 smoke 均继续通过 |
| 2026-05-24 | Codex | 同步教师学情页仪表盘卡片“学习进度”真值修复进展，记录 `TeacherDashboardDTO` 已新增 `overallProgress`，`TeacherDashboardServiceImpl#getDashboardData(...)` 现已复用 `getStudentLearningSummary(...)` 的真实聚合进度回填该字段，`teacher-student-dashboard.html` 与静态副本也已改为消费 `dashboardData.overallProgress`，不再拿 `averageScore` 冒充学习进度；扩展 `TeacherDashboardServiceImplTest`、前端契约脚本后，教师端 JWT smoke 与 CRUD smoke 继续通过 |
| 2026-05-24 | Codex | 同步教师学情页仪表盘卡片变化值文案修复进展，记录 `teacher-student-dashboard.html` 原先“平均成绩 / 作业完成率 / 学习进度”卡片下方仍保留硬编码箭头与假变化数字，现已将学生数变化值改为消费后端真实 `totalStudentsChange` 并通过 `formatStatChange(...)` 动态渲染，另外三张暂无稳定后端变化源的卡片统一改为中性说明文案，避免页面继续展示伪造增减值；`verify-teacher-student-dashboard-contract.js`、教师端 JWT smoke 与 CRUD smoke 已通过。后续复核确认：此前偶发的 `sync-frontend-to-static.ps1 -CheckOnly` 红灯主要来自把 `sync` 与 `checkOnly` 并行执行造成的竞态，顺序执行后可稳定恢复绿色 |
| 2026-05-24 | Codex | 同步 teacher 端总仪表盘伪变化语义收口进展，记录 `TeacherDashboardServiceImpl#getDashboardData(...)` 中多组 `*Change` 字段目前仍主要来自模拟值/启发式回填，因此 [frontend/dist/teacher-dashboard.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-dashboard.html) 已不再把它们渲染成“较上周 / 较昨日”，而是统一降级为中性说明文案（如“当前筛选课程统计”“基于当前待批改队列”）；新增 `verify-teacher-dashboard-contract.js` 护栏。过程中教师 JWT smoke 真实抓到 `updateStatistics()` 在 `forEach` 回调里误用 `continue` 导致的 `Illegal continue statement` 回归，现已修复为 `return` 并重新验证通过 |
| 2026-05-24 | Codex | 同步教师学情页学生数卡片伪时间对比进一步收口进展，记录前一轮 `teacher-student-dashboard.html` 对 `dashboardData.totalStudentsChange` 的消费虽然更动态，但进一步审计后确认该字段本身仍来自 `TeacherDashboardServiceImpl#getDashboardData(...)` 的模拟/启发式回填，因此学生数卡片变化行现已统一降级为中性说明“当前筛选学生统计”，并移除前端对 `totalStudentsChange` 的直接消费；`verify-teacher-student-dashboard-contract.js`、教师端 JWT smoke、教师 CRUD smoke 与前端同步链已重新通过 |
| 2026-05-24 | Codex | 同步学生端首页伪变化值链收口进展，记录 [frontend/dist/student-dashboard.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/student-dashboard.html) 之前直接用 `Math.random()` 兜底 `coursesChange / assignmentsChange / examsChange`，并通过“较上月/较上周”文案把占位值渲染成真实时间对比；同时后端 `StudentServiceImpl` 对相关 change 字段目前也基本只回 `0` 占位。现已将学生首页四张卡片统一降级为中性说明文案，并移除前端随机 delta 与趋势式渲染逻辑；新增 `verify-student-dashboard-contract.js` 护栏。学生 CRUD smoke、教师 JWT smoke 与同步链均重新通过 |
| 2026-05-24 | Codex | 同步 student dashboard 后端死契约清理进展，记录 `StudentDashboardDTO` 已移除 `courseCountChange / pendingAssignmentsChange / upcomingExamsChange / overallProgressChange` 四个仅由 `StudentServiceImpl#getStudentPerformance(...)` 固定回 `0` 的占位字段，学生首页也已停止读取这些后端死字段；`StudentServiceImplTest` 增加 DTO 反射护栏，`verify-student-dashboard-contract.js` 追加禁止 `data.courseCountChange / data.pendingAssignmentsChange / data.upcomingExamsChange / data.overallProgressChange` 回流的检查。`mvn --% -pl major_assignment -Dtest=...` 定向回归 77 项全绿，学生 CRUD smoke、教师 JWT smoke 与前端同步链均已通过 |
| 2026-05-24 | Codex | 同步 student-stats 伪变化值链收口进展，记录 `StudentServiceImpl#getLearningStats(...)` 已移除 `studyTimeChange / completedTasksChange / averageScoreChange / knowledgeMasteryChange` 四个固定回 `0` 的占位字段，学习数据页也已停止把这些字段渲染成“比上周提升/下降”或“与上周持平”，统一改为中性说明文案；新增 `verify-student-stats-contract.js`，并扩展 `StudentServiceImplTest` 覆盖 `getLearningStats()` 返回 shape。定向 Maven 回归 78 项、学生 CRUD smoke、教师 JWT smoke 与前端同步链均已通过 |
| 2026-05-24 | Codex | 同步 teacher-dashboard 前端死壳清理进展，记录 `teacher-dashboard.html` 已移除 `payload.totalCoursesChange / totalStudentsChange / pendingAssignmentsChange / pendingExamsChange / warningCountChange` 这层无展示职责的适配读取，以及 fallback 分支里对应的 `0` 占位值；`verify-teacher-dashboard-contract.js` 已加严为禁止这些前端死读取与 `0` fallback 回流。教师端 JWT smoke、教师 CRUD smoke、学生 CRUD smoke 与前端同步链均已通过 |
| 2026-05-24 | Codex | 同步 `teacher-student-dashboard` 前端统计壳进一步收口进展，记录页面已将 `studentCountChange / averageScoreChange / assignmentCompletionRateChange / learningProgressChange` 这组旧 delta 容器统一改为 `*Meta` 中性 metadata 行，并移除 `positive/negative` 样式语义与空数据/异常分支里的“暂无变化数据”伪变化文案，让四张卡片在成功、空数据和异常三条链上都稳定落到真实说明口径；`verify-teacher-student-dashboard-contract.js`、教师端 JWT smoke、教师 CRUD smoke 与前端同步链均已通过 |
| 2026-05-24 | Codex | 同步 `student-stats` 前端统计壳进一步收口进展，记录学习数据页已将 `studyTimeChange / completedTasksChange / averageScoreChange / knowledgeMasteryChange` 这组旧 delta 命名容器统一改为 `*Meta` 中性 metadata 行，并同步替换页面脚本中的对应 DOM 查询与回写逻辑；`verify-student-stats-contract.js` 已加严为禁止旧 `*Change` id 和脚本查询回流，学生 CRUD smoke 与前端同步链均已通过 |
| 2026-05-24 | Codex | 同步 `student-dashboard` 前端统计壳进一步收口进展，记录学生首页已将 `courseCountChangeLabel / assignmentCountChangeLabel / examCountChangeLabel / progressPercentageChangeLabel` 这组旧 delta label 命名统一改为 `*MetaLabel`，并将 `changeLabelOverrides` / `updateStatChange(...)` 这层历史兼容壳同步替换为 `metaLabelOverrides` / `updateStatMeta(...)`；`verify-student-dashboard-contract.js`、学生 CRUD smoke 与前端同步链均已通过 |
| 2026-05-24 | Codex | 同步 `student-dashboard` 空状态壳命名收口进展，记录学生首页在登录上下文缺失或仪表板请求失败时实际使用的是空状态兜底，但函数/注释/日志仍残留 `showMockData()`、`显示模拟数据`、`模拟数据加载` 这类误导性命名；现已统一改为 `showEmptyStateData()`、`显示空状态数据` 与“模拟异步数据加载”，并在刷新 teacher/student 开发会话后重新跑通学生 CRUD smoke。`verify-student-dashboard-contract.js` 与前端同步链均已通过 |
| 2026-05-24 | Codex | 同步 `student-stats` 默认值兜底话术收口进展，记录学习数据页虽然早已在错误/空返回场景下统一回落到默认值和空图表，但注释中仍残留“`不使用模拟数据` / `使用默认值而不是模拟数据`”这类历史过渡表述；现已统一改为“`使用默认值（0）` / `使用默认值`”，让注释口径与真实行为对齐。`verify-student-stats-contract.js`、学生 CRUD smoke 与前端同步链均已通过 |
| 2026-05-24 | Codex | 同步 `teacher-dashboard` 前端统计壳进一步收口进展，记录教师首页已将 `totalCoursesChangeLabel / totalStudentsChangeLabel / pendingAssignmentsChangeLabel / pendingExamsChangeLabel / warningCountChangeLabel` 这组旧 delta label 命名统一改为 `*MetaLabel`，并将 `changeLabelOverrides` 同步替换为 `metaLabelOverrides`；`verify-teacher-dashboard-contract.js`、教师端 JWT smoke、教师 CRUD smoke 与前端同步链均已通过 |
| 2026-05-24 | Codex | 同步 `teacher-student-dashboard` 默认值重复壳收口进展，记录学情页统计卡原本在成功、空数据和异常三条链里重复逐项回写同一组 metadata 文案，现已抽成共享 `dashboardStatMetaText` 与 `applyDashboardStatMetaText()` helper，减少默认值/元信息回写的分叉点；`verify-teacher-student-dashboard-contract.js`、教师端 JWT smoke、教师 CRUD smoke 与前端同步链均已通过 |
| 2026-05-24 | Codex | 同步 `teacher-dashboard` 前端统计壳继续收口进展，记录教师首页 `updateStatistics()` 中原本还残留一段永远不会命中的旧 `Change` 分支，且中性 metadata 文案仅散落挂在局部 `metaLabelOverrides` 上；现已统一抽成共享 `dashboardStatMetaLabelText` 与 `applyDashboardStatMetaLabels()` helper，并移除这段失效旧分支，让首页统计卡的 metadata 文案出口与 `teacher-student-dashboard` 保持一致。`verify-teacher-dashboard-contract.js`、教师端 JWT smoke、教师 CRUD smoke 与前端同步链均已通过；后续复核确认：此前偶发的 `sync-frontend-to-static.ps1 -CheckOnly` 红灯主要来自把 `sync` 与 `checkOnly` 并行执行造成的竞态，顺序执行后可稳定恢复绿色 |
| 2026-05-24 | Codex | 同步 `teacher-dashboard` 图表与最近活动空状态壳收口进展，记录教师首页 `initCharts()` 中平均分图和提交率图原本各自手写 `暂无数据 / [0]` 兜底，`updateRecentActivities()` 也单独内联构造“暂无活动记录”项；现已统一抽成 `buildChartCategoryAxisData()`、`buildChartSeriesData()`、`createEmptyRecentActivityItem()` 与 `revealActivityItem()` 这组共享 helper，减少空状态构造重复点，并让图表/活动区默认表现更一致。`verify-teacher-dashboard-contract.js`、教师端 JWT smoke、教师 CRUD smoke 与前端同步链均已通过，且本轮 `sync-frontend-to-static.ps1 -CheckOnly` 也已恢复绿色 |
| 2026-05-24 | Codex | 同步 `teacher-dashboard` 加载态重复定义收口进展，记录教师首页原本同时存在两套 `showLoadingState()/hideLoadingState()` 实现，后定义版本会直接覆盖前面的统计卡片 overlay 逻辑；现已统一保留一套加载态出口，并抽成 `buildChartLoadingStateMarkup()`、`appendStatCardLoadingOverlays()`、`replaceChartContainersWithLoadingState()`、`removeStatCardLoadingOverlays()`、`removeChartContainerLoadingStates()` 这组 helper，减少行为漂移面，并通过收紧后的 `verify-teacher-dashboard-contract.js`（要求 `showLoadingState/hideLoadingState` 仅定义一次）、教师端 JWT smoke、教师 CRUD smoke 与前端同步链验证。后续复核确认：此前偶发的 `sync-frontend-to-static.ps1 -CheckOnly` 红灯主要来自把 `sync` 与 `checkOnly` 并行执行造成的竞态，顺序执行后可稳定恢复绿色 |
| 2026-05-24 | Codex | 同步 `teacher-dashboard` 前置依赖 guard 收口进展，记录课程、班级、作业、考试、预警等 loader 原本各自重复写 `contentContainer` 初始化检查与 `teacherAPI` 可用性检查；现已统一抽成 `ensureContentContainerReady()` 与 `ensureTeacherApiReady()` 两个共享 helper，收敛同类错误提示与初始化判断的分叉点。`verify-teacher-dashboard-contract.js`、教师端 JWT smoke、教师 CRUD smoke 与前端同步链均已通过；后续复核确认：此前偶发的 `sync-frontend-to-static.ps1 -CheckOnly` 红灯主要来自把 `sync` 与 `checkOnly` 并行执行造成的竞态，顺序执行后可稳定恢复绿色 |
| 2026-05-24 | Codex | 同步 `teacher-dashboard` 内容区空列表/错误态壳收口进展，记录课程、班级、作业、预警几条 loader 原本都在各自手写 `empty-state` 和 `error-state` HTML，考试空态与筛选空态也单独内联；现已统一抽成 `buildTeacherEmptyStateMarkup()`、`buildTeacherTableEmptyRow()` 与 `buildTeacherSectionErrorMarkup()` 这组共享 helper，收敛内容区空列表与错误态渲染逻辑。`verify-teacher-dashboard-contract.js`、教师端 JWT smoke、教师 CRUD smoke 与前端同步链均已通过；后续复核确认：此前偶发的 `sync-frontend-to-static.ps1 -CheckOnly` 红灯主要来自把 `sync` 与 `checkOnly` 并行执行造成的竞态，顺序执行后可稳定恢复绿色 |
| 2026-05-24 | Codex | 补充修正文档中的同步校验口径，确认最近几轮把 `sync-frontend-to-static.ps1 -CheckOnly` 记成“脚本口径噪音”的说法并不准确；重新按顺序执行 `sync-frontend-to-static.ps1` 后再执行 `-CheckOnly` 已稳定绿色，先前零星 `[DIFF] teacher-dashboard.html` 主要来自把同步与检查并行执行造成的竞态 |
| 2026-05-24 | Codex | 同步 `teacher-dashboard` 说明型 section 壳收口进展，记录 `学习数据 / AI辅助工具 / 系统设置` 三段原本各自手写 `fade-in + teacher-panel + title + description + info-state` 结构，现已统一抽成 `buildTeacherInfoSectionMarkup()` 并切换三条 loader 复用；`verify-teacher-dashboard-contract.js`、顺序执行的 `sync-frontend-to-static.ps1 -> -CheckOnly`、教师端 JWT smoke 与教师 CRUD smoke 均已通过 |
| 2026-05-24 | Codex | 同步 `teacher-dashboard` section helper 骨架继续下沉进展，记录新增 `buildTeacherSectionShell()`，并让 `buildTeacherSectionErrorMarkup()`、`buildTeacherDatedSectionMarkup()` 与 `buildTeacherInfoSectionMarkup()` 统一从共享底座生成，去掉三类 section helper 各自重复的外层骨架；`verify-teacher-dashboard-contract.js`、顺序执行的 `sync-frontend-to-static.ps1 -> -CheckOnly`、教师端 JWT smoke 与教师 CRUD smoke 均已通过 |
| 2026-05-24 | Codex | 同步 `teacher-dashboard` 页面分发壳收口进展，记录 `loadPageContent()` 已从手写 `switch (page)` 切换为 `teacherPageLoaders` 分发表，并通过 `pageLoader || teacherPageLoaders.dashboard` 兜底默认路径，减少分发层分叉与新增页面时的改动面；`verify-teacher-dashboard-contract.js`、顺序执行的 `sync-frontend-to-static.ps1 -> -CheckOnly`、教师端 JWT smoke 与教师 CRUD smoke 均已通过 |
| 2026-05-24 | Codex | 同步 `teacher-dashboard` dashboard 加载态职责收口进展，记录导航分发层 `loadPageContent()` 与 `loadDashboardContent()` 原本会在 dashboard 路径上双重执行 `showLoadingState()/hideLoadingState()`；现已为 `loadDashboardContent(options)` 新增 `manageLoading` 选项，并让 `teacherPageLoaders.dashboard` 切换为 `loadDashboardContent({ manageLoading: false })`，把导航切换场景下的 loading 控制统一留在分发层，而 `window.onload` 与定时刷新仍保留默认自管 loading 语义。`verify-teacher-dashboard-contract.js`、顺序执行的 `sync-frontend-to-static.ps1 -> -CheckOnly`、教师端 JWT smoke 与教师 CRUD smoke 均已通过 |
| 2026-05-24 | Codex | 同步 `teacher-dashboard` 初始化链收口进展，记录 `window.onload` 已从同时承担 header 初始化、dashboard 首屏加载、最近活动刷新按钮绑定和 30 秒自动刷新注册，拆成 `initializeTeacherDashboardPage()` 与 `startTeacherDashboardAutoRefresh()` 两个 helper，让 onload 只保留会话检查与启动调度；`verify-teacher-dashboard-contract.js`、顺序执行的 `sync-frontend-to-static.ps1 -> -CheckOnly`、教师端 JWT smoke 与教师 CRUD smoke 均已通过 |
| 2026-05-24 | Codex | 同步 `teacher-dashboard` 内容区 loader 控制壳收口进展，记录课程、班级、学情预警以及 `学习数据 / AI辅助工具 / 系统设置` 几条 loader 原本各自重复写 `contentContainer` guard、`teacherAPI` guard、`try/catch` 与 section error 回退；现已统一抽成 `ensureTeacherLoaderPrerequisites(requireApi)` 与 `renderTeacherSectionContent(title, renderContent, options)`，收敛这些内容区 loader 的共同前置检查与错误回退路径，同时暂不把作业/考试两条更重的搜索与事件绑定链硬塞进同一抽象。`verify-teacher-dashboard-contract.js` 已先红后绿，顺序执行的 `sync-frontend-to-static.ps1 -> -CheckOnly`、教师端 JWT smoke 与教师 CRUD smoke 均已通过；中途 fresh session 过期后已用 `get-dev-auth-session.ps1` 刷新 teacher/student 会话再补跑验证 |
| 2026-05-24 | Codex | 同步 `teacher-dashboard` 内容区共享 loader 继续下沉进展，记录在上一轮统一了 section loader 的前置检查、成功回填和错误回退后，又为 `renderTeacherSectionContent(title, renderContent, options)` 补充了最薄的 `afterRender` 能力，专门承接渲染后的 DOM 事件绑定；`loadAssignmentsContent()` 已把作业搜索按钮绑定移入 `afterRender`，`loadExamsContent()` 已把 `setupExamsSearchAndFilter()` 移入 `afterRender`，从而让 assignment/exam 两条重 loader 也纳入共享 section 控制路径，同时保留各自不同的搜索参数与筛选语义，不强行抽成同一种搜索模型。`verify-teacher-dashboard-contract.js` 已先红后绿，顺序执行的 `sync-frontend-to-static.ps1 -> -CheckOnly`、教师端 JWT smoke 与教师 CRUD smoke 均已通过 |
| 2026-05-24 | Codex | 同步 `teacher-dashboard` teacher API 入口统一进展，记录页面内大多数 loader 已走 `window.teacherAPI`，但 `buildDashboardFromAvailableApis()` 与 `loadAssignmentsContent()` 仍各自 `new APIService()` / `new TeacherAPI(...)` 走本地 client 路径；现已新增 `getTeacherDashboardApi()` 统一页面级 teacher API 入口，优先复用 `window.teacherAPI`，必要时再回退创建本地 `TeacherAPI`，并让 dashboard 聚合链与作业 loader 都切到这条共享入口。`verify-teacher-dashboard-contract.js` 已先红后绿，顺序执行的 `sync-frontend-to-static.ps1 -> -CheckOnly`、教师端 JWT smoke 与教师 CRUD smoke 均已通过 |
| 2026-05-24 | Codex | 同步 `teacher-dashboard` assignment 搜索绑定局部收口进展，记录在不强行统一 exam 搜索模型的前提下，已新增 `readAssignmentSearchParams(previousSearchParams)` 与 `bindAssignmentSearchControls(previousSearchParams)`，把 assignment 搜索框/课程下拉/状态下拉读取及重新加载绑定从 `afterRender` 内联匿名函数中抽出，继续压薄 `loadAssignmentsContent()` 的局部控制壳，同时保留 exam 侧 `setupExamsSearchAndFilter()` 独立不动。`verify-teacher-dashboard-contract.js` 已先红后绿，顺序执行的 `sync-frontend-to-static.ps1 -> -CheckOnly`、教师端 JWT smoke 与教师 CRUD smoke 均已通过 |
| 2026-05-24 | Codex | 同步 `teacher-dashboard` exam 搜索区局部收口进展，记录在不强行统一 assignment/exam 筛选模型的前提下，已新增 `buildExamSearchPanelMarkup(courses = [])` 与 `readExamFilterValues()`，把考试页有数据/空数据分支里重复的搜索面板 HTML 以及 `filterExams()` 开头那组 DOM 读取统一收成 exam 自己的局部 helper。`verify-teacher-dashboard-contract.js` 已先红后绿，顺序执行的 `sync-frontend-to-static.ps1 -> -CheckOnly`、教师端 JWT smoke 与教师 CRUD smoke 均已通过 |
| 2026-05-24 | Codex | 同步 `teacher-dashboard` exam 过滤链去重进展，记录 `loadExamsContent()` 已在成功取数后回写页内 `examListState`，`filterExams()` 不再为每次本地筛选重新 `getExams()`，而是直接过滤当前页已持有的考试列表；删除考试后仍通过重新执行 `loadExamsContent()` 刷新这份状态。`verify-teacher-dashboard-contract.js` 已先红后绿，顺序执行的 `sync-frontend-to-static.ps1 -> -CheckOnly`、教师端 JWT smoke 与教师 CRUD smoke 均已通过；中途 fresh session 过期后已用 `get-dev-auth-session.ps1` 刷新 teacher/student 会话再补跑验证 |
- 2026-05-24：修正教师学情预警页 `teacher-warning.html` 统计卡片的伪 delta 展示。页面原先在拿到真实预警总数后，又以前端固定比例推导出 `+x 条 (y%)` 和上涨/下降箭头，容易被误读为真实环比。现已改为仅展示真实数量和中性 metadata 文案，并新增 `scripts/verify-teacher-warning-contract.js` 作为回归护栏；配套 `sync-frontend-to-static.ps1`、教师 JWT 页面验证与教师 CRUD smoke 均已通过。
- 2026-05-24：继续修正教师学情预警页 `teacher-warning.html` 图表语义。页面此前在获取预警列表失败时会注入一组默认 warning 数据继续绘图，造成“看起来有图但其实是假的”。现已改为真实空态/失败态渲染，并将图表按钮切换从依赖标题文本改为 `data-*` 标记驱动；配套 `scripts/verify-teacher-warning-contract.js`、前端静态同步、教师 JWT 页面验证与教师 CRUD smoke 均已通过。
- 2026-05-24：继续收敛教师学情预警页 `teacher-warning.html` 的数据流重复。页面现已补充共享 warning analytics helper（`warningAnalyticsListState` / `buildWarningAnalyticsListParams(...)` / `fetchWarningAnalyticsList(...)`），用于统一图表初始化、图表刷新与统计 fallback 对全量预警数据的取数逻辑，同时保留分页列表链独立，避免跨语义强耦合；相关契约脚本、前端静态同步、教师 JWT 页面验证与教师 CRUD smoke 均已通过。
- 2026-05-24：继续修正教师学情预警页 `teacher-warning.html` 的详情处理链。退场的 `generateWarnings(...)` 已删除；列表状态渲染改为优先基于 `status` 归一化，不再把“未解决”一律看成“未处理”；预警详情弹窗中的“标记为处理中 / 标记为已解决”按钮也已真正接入处理函数，并在操作成功后同步更新详情 badge 与按钮状态。相关契约校验、前端静态同步、教师 JWT 页面验证与教师 CRUD smoke 均已通过（中途 401 来自 fresh session 过期，续新后已恢复绿色）。
- 2026-05-24：继续收正教师学情预警页 `teacher-warning.html` 的详情弹窗语义。由于后端 detail 当前只稳定返回 `EarlyWarning` 本体及 `studentName / courseName / reason / suggestion / status` 等字段，前端中原有的班级、联系方式、家长联系方式、辅导员、学习数据和成绩图均属于无数据来源的占位内容。现已改为仅展示真实 warning detail 字段，避免页面继续“看起来很全但其实在说假话”；相关契约脚本、前端静态同步、教师 JWT 页面验证与教师 CRUD smoke 均已通过。
- 2026-05-24：继续收正教师知识点分析页 `teacher-knowledge.html` 的详情弹窗语义。前端审计确认：分析接口 `/api/knowledge-points/analysis/teacher/course` 当前稳定返回 `courseName / knowledgePointDistribution / atRiskStudents / weakTopics`，而详情接口 `/api/teacher/knowledge-points/{id}` 仅稳定返回 `KnowledgePoint(id / pointName / description / difficulty / orderIndex / courseId)`。因此页面中原有的 `masteryLevel / difficultyLevel / importanceLevel / poorStudentsPercentage / topErrorQuestions / teachingSuggestions / weakStudentsList` 等内容属于无数据来源的占位详情，现已统一降级为只展示知识点基础信息，并通过说明文案引导用户以主分析区为准；新增 `scripts/verify-teacher-knowledge-contract.js` 护栏，配套 `sync-frontend-to-static.ps1`、`-CheckOnly`、教师 JWT 页面验证与教师 CRUD smoke 均已通过（中途 fresh session 过期后已续新 teacher/student 会话再补跑通过）。
- 2026-05-24：继续收正教师知识点分析页 `teacher-knowledge.html` 的旧 legacy 降级链。基于 fresh teacher 会话实测，`/api/teacher/knowledge-points` 与 `/api/knowledge-points/analysis/teacher/course` 当前都稳定返回 `200`，因此旧“仍依赖旧会话认证、当前微服务联调环境暂显示空结果”文案已经不再符合真实状态。现已将相关 401/403 分支统一改成中性的“知识点列表暂不可用，请稍后重试 / 知识点分析服务暂不可用，请稍后重试”，同时保留空数据场景继续显示真实的暂无数据状态；`scripts/verify-teacher-knowledge-contract.js` 已同步加严为禁止旧 legacy helper 与旧文案回流，配套 `sync-frontend-to-static.ps1`、`-CheckOnly`、教师 JWT 页面验证与教师 CRUD smoke 均已通过。
- 2026-05-24：继续收正教师知识点分析页 `teacher-knowledge.html` 的图表切换结构。页面原本通过读取 `chart-card-title` 文本来判断切换的是“知识点掌握程度分布”还是“知识点掌握程度雷达图”，标题一旦变更就有行为回退风险。现已为两张图卡补充 `data-chart-kind`，为按钮补充 `data-chart-view`，切换逻辑也改为只读 `data-*` 分流，不再依赖标题文本；`scripts/verify-teacher-knowledge-contract.js` 已同步加严为禁止旧 `chartTitle === ...` 逻辑回流。配套 `sync-frontend-to-static.ps1`、顺序执行的 `-CheckOnly`、教师 JWT 页面验证与教师 CRUD smoke 均已通过。
- 2026-05-24：继续收正教师知识点分析页 `teacher-knowledge.html` 的重复取数链。页面原本会先在 `loadFilterOptions()` 中单独请求一次知识点列表用于填充筛选下拉，随后每次 `handleQuery()` 又会在 `mergeKnowledgeAnalysisWithPointList()` 里再请求一次同样的知识点列表以补全分析结果。现已新增页内 `knowledgePointListState` 与统一 `fetchKnowledgePointList({ forceRefresh })` helper，让筛选初始化和分析 merge 共享同一份知识点列表状态，同时在创建/编辑/删除知识点成功后显式刷新并回填筛选下拉，避免本地状态陈旧；`scripts/verify-teacher-knowledge-contract.js` 已同步加严为禁止旧 `getKnowledgePointList()` 直连回流。配套 `sync-frontend-to-static.ps1`、顺序执行的 `-CheckOnly`、教师 JWT 页面验证与教师 CRUD smoke 均已通过。
- 2026-05-24：继续收正教师知识点分析页 `teacher-knowledge.html` 的页面提示 helper 命名。原 `showKnowledgeMicroserviceNotice(...) / hideKnowledgeMicroserviceNotice()` 早已不只承载“微服务联调”场景，导出报告成功等普通页面提示也在复用它，名称与真实职责已经不对齐。现已统一改为 `showKnowledgePageNotice(...) / hideKnowledgePageNotice()`，并把对应 DOM id 同步切到 `knowledge-page-notice`，让命名、用途和调用点一致；`scripts/verify-teacher-knowledge-contract.js` 已同步加严为禁止旧 helper 和旧 DOM id 回流。配套 `sync-frontend-to-static.ps1`、顺序执行的 `-CheckOnly`、教师 JWT 页面验证与教师 CRUD smoke 均已通过。
- 2026-05-24：继续收正教师知识点分析页 `teacher-knowledge.html` 的初始化控制流。页面原本同时存在三段 `DOMContentLoaded`（权限/chrome、图表与首屏加载、筛选与查询事件）以及两段顶层即刻绑定的表格动作，启动职责分散且不便后续维护。现已统一改为单一 `DOMContentLoaded -> initializeTeacherKnowledgePage()` 入口，并拆出 `ensureTeacherKnowledgeAccess()`、`initializeTeacherKnowledgeChrome()`、`initializeKnowledgeManagementControls()`、`initializeKnowledgeAnalysisCharts()`、`initializeKnowledgeFilterControls()`、`initializeKnowledgeTableActions()` 等薄 helper，再顺序初始化课程下拉、图片错误处理、分页和首屏 `loadInitialData()`，把页面启动链收成一条更清晰的控制流；`scripts/verify-teacher-knowledge-contract.js` 已同步加严为要求仅保留一个 `DOMContentLoaded` 初始化入口。配套 `sync-frontend-to-static.ps1`、顺序执行的 `-CheckOnly`、教师 JWT 页面验证与教师 CRUD smoke 均已通过。

- 2026-05-24 teacher-knowledge: 修复学生筛选联合语义。此前班级和课程同时选中时，学生下拉实际只按班级接口取数，课程条件被静默忽略；现改为课程学生集合与班级学生集合按 studentId 取交集，保证 teacher-knowledge 页面筛选语义与 UI 一致。已通过 contract verifier、前端静态同步检查、teacher JWT 页面验证与 teacher CRUD smoke。

- 2026-05-24 teacher-knowledge: 完成班级/课程列表状态复用。此前筛选区、知识点编辑课程下拉以及全部学生扇出链会重复请求班级/课程列表；现已通过 classListState / courseListState 与 fetchTeacherClassList/fetchTeacherCourseList 统一复用，降低重复取数并保持现有 CRUD/筛选行为不变。

- 2026-05-24 teacher-knowledge: 完成筛选初始化职责收口。学生下拉联动监听已从 loadFilterOptions() 挪回 initializeKnowledgeFilterControls()，确保筛选数据加载与事件绑定解耦，后续即使重复刷新筛选项也不会重复绑定 throttledUpdateStudentOptions。

- 2026-05-24 teacher-knowledge: 修复重置筛选后的学生候选回填。此前点击重置会把班级/课程/学生/知识点值设回 all，但学生下拉不会跟着刷新到新的全集候选；现已在 reset 流程中先刷新学生选项，再把 student-select 复位为 all，确保 UI 与实际筛选范围一致。
- 2026-05-24 teacher-knowledge: 修复重置筛选后的学生候选回填。此前点击重置会把班级/课程/学生/知识点值设回 all，但学生下拉不会跟着刷新到新的全集候选；现已在 reset 流程中先刷新学生选项，再把 student-select 复位为 all，确保 UI 与实际筛选范围一致。
- 2026-05-24 teacher-knowledge: 修复学生候选刷新链的异步语义。此前 updateStudentOptions() 实际 await 不到节流包装器内部的异步取数，reset 和筛选初始化只能“看起来等待”；现已拆出真正可 await 的 refreshStudentOptions()，并把节流限定在 change 事件路径上。
- 2026-05-24 teacher-knowledge: 完成学生候选刷新控制流二次收口。现已将主动刷新与事件刷新分流，主动链支持真实 await 和 forceRefresh，事件链使用 debounce，并通过 filter key + request token 避免旧请求覆盖新筛选结果。
- 2026-05-24 teacher-knowledge: 补入统一筛选动作入口 applyKnowledgeFilters()，让 query/reset/removeFilter 至少共享一套筛选状态落地和学生候选刷新语义；此前 removeFilter 在 class/course 维度上会留下旧 student-select 候选，现已修正。
- 2026-05-24 teacher-courses: 修正班级学生弹窗“添加学生到当前班级”链。此前前端将输入描述成“学生ID/学号”，但实际调用的是 `PUT /api/teacher/students/{studentId}`，后端 `updateStudent(...)` 又会先按“教师已可见学生”做权限门槛，导致这条入口无法真实承诺“把系统里已有学生加入我管理的班级”。现已新增专用接口 `POST /api/teacher/classes/{classId}/students`，支持按学生ID或用户名解析学生，仅为“加入当前教师可管理班级”这条纯班级分配链放开未分班学生入口；若学生已在其他班级但该班级不属于当前教师管理范围，则返回 `403` 阻止跨权限移动。前端 `teacher-courses-students.js` 已切到新接口，并将 UI 文案与表头统一为“学生ID / 用户名（学号）”；新增 `scripts/verify-teacher-courses-students-contract.js` 护栏，并已顺序通过 contract verifier、静态同步检查、`TeacherDashboardControllerTest`、教师 JWT 页面验证与教师 CRUD smoke。
| 2026-05-24 | Codex | 补齐 `teacher-courses` 班级学生链在微服务 runtime 中的真实落点。浏览器 smoke 追查确认 gateway 实际命中 course-service 的 TeacherCourseAdminController，原先缺少 `POST /api/teacher/classes/{classId}/students`，因此 teacher CRUD smoke 在“把已有学生加入班级”步骤上返回 `405 请求方法不支持`。现已在 course-service 新增该 POST 路由与 `CourseApplicationService.addStudentToClass(...)`，支持 `studentIdentifier` 按数字 ID 或用户名解析、未分班学生直接加入、已在教师可管理班级中的学生先返回 `needConfirm=true`、携带 `forceReplace=true` 再执行换班、已在教师不可管理班级中的学生返回 `403` 无权移动该学生所在班级。对应 `CourseControllerTest` 与 `CourseApplicationServiceTest` 已补上控制器/服务红绿灯，并通过 `mvn --% -pl course-service -am -Dtest=CourseControllerTest,CourseApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` 验证；重新打包 course-service 并重启 runtime stack 后，teacher-courses 这段 browser smoke 已从 `405` 变为真实通过。 |
| 2026-05-24 | Codex | 修正 `scripts/verify-teacher-browser-crud.js` 的状态污染。teacher-courses 步骤原先在验证“把已有学生移动到新建班级”后，只在最终 cleanup 阶段才恢复学生原班级；由于当前环境下唯一可移动学生是 `student42`，后续 “teacher assignments page CRUD: assignment/exam/grade” 会因为学生失去课程可见性而报“考试不存在或无权访问”。现已将 smoke 调整为：在 teacher-courses 步骤确认移动成功后立刻把学生恢复回原班级，并清空 `created.movedStudent`，避免污染后续 assignment/exam/notification/settings 验证链。续新 teacher/student 会话后，`node .\scripts\verify-teacher-browser-crud.js .\.runtime-logs\teacher-session-polish-fresh.json .\.runtime-logs\student-session-polish-fresh.json` 已重新达到 `Teacher browser CRUD smoke passed: 5`。 |
| 2026-05-24 | Codex | 为 `teacher-courses` 新增班级学生 runtime 能力补充独立 targeted verifier。新增 `scripts/seed-isolated-teacher-course-student-assign.sql` / `.ps1` 与 `scripts/verify-teacher-courses-student-add-runtime.js`，在隔离 teacher/class/student fixture 下直接验证“未分班学生按用户名加入”“教师可管理原班级学生先 `needConfirm` 再 `forceReplace`”“教师不可管理原班级学生返回 `403`”四条语义；该 verifier 已与 `verify-teacher-browser-crud.js`、`verify-teacher-jwt-pages.js`、`verify-teacher-courses-students-contract.js` 及顺序执行的 `sync-frontend-to-static.ps1 -> -CheckOnly` 一起 fresh 通过。 |
- 2026-05-24：`teacher-ai-tools.html` 已收正为真实 AI 能力页。当前只保留并接通三条后端已稳定提供的能力：题目生成（`/api/ai/generate-questions`）、试卷生成（`/api/ai/generate-exam`）、学习建议（`/api/ai/learning-suggestions`）；其中原“学情分析”入口已按真实语义改为“AI学习建议”。知识点讲解、作业批改、教学计划三条仍无后端来源的工具已统一降级为不可用说明，不再通过 `setTimeout + mockResults` 展示本地伪 AI 结果。新增 `scripts/verify-teacher-ai-tools-contract.js` 护栏，并已通过契约检查、前端静态同步、teacher JWT 页面链与 teacher browser CRUD smoke 验证。
- 2026-05-24：新增 `scripts/verify-teacher-ai-tools-runtime.js`，对 `teacher-ai-tools.html` 的三条真实 AI 能力（题目生成、试卷生成、学习建议）和三条诚实降级能力（知识点讲解、作业批改、教学计划）做页面级运行时验证。该脚本已使用 fresh teacher JWT 会话验证通过，补齐了此前只有 contract、没有按钮级 runtime 证据的缺口。
| 2026-05-24 | Codex | 收正 `student-ai-assistant.html`：页面已从纯前端 `setTimeout + getAIMockResponse(...)` 演示聊天改为“真实学习建议 + 诚实降级自由问答”。真实能力现通过 `POST /api/ai/learning-suggestions` 按 student JWT 身份生成学习建议；quick actions 已改为“生成我的学习建议 / 给我本周复习建议 / 帮我梳理当前学习重点”；任意自由问答则明确提示当前运行时仅支持学习建议、不再假装有通用聊天能力。新增 `scripts/verify-student-ai-assistant-contract.js` 与 `scripts/verify-student-ai-assistant-runtime.js`，并将 `student-ai-assistant` 纳入 `scripts/verify-student-jwt-pages.js` 页面链。已顺序通过 contract、runtime、静态同步检查、student JWT 页面链、student browser CRUD smoke、teacher browser CRUD smoke 与 teacher JWT 页面链；中途已续新 fresh teacher/student 会话文件避免 token 过期噪音。 |
| 2026-05-24 | Codex | 收正 `student-settings.html` 数据导出链：页面原先会在 `/api/student/export-data` 失败时自动导出“当前页面 + 本地偏好”的拼装 JSON，语义上容易被误读成真实学习数据导出。现已改为只在真实 Gateway 导出成功时生成 `student-data-export-YYYY-MM-DD.json` 并提示“学生学习数据导出成功”，失败时明确报错，不再伪装本地兜底导出。新增 `scripts/verify-student-settings-export-contract.js` 与 `scripts/verify-student-settings-export-runtime.js`，并已通过静态同步检查、student JWT 页面链、student browser CRUD smoke、teacher JWT 页面链与 teacher browser CRUD smoke。 |
| 2026-05-24 | Codex | 收正 `student-dashboard.html` 最近活动 fallback：在当前 JWT 微服务环境下，`/api/student/activities` 仍未接通，但页面已能稳定读取作业/考试列表。原实现会在 activities unsupported 时直接清空活动区并落到“暂无活动记录”；现已新增 `populateDashboardActivitiesFromConnectedSources(studentAPI, statsData)` 统一用最近作业/考试生成活动流，并在需要时顺手用最近考试列表修正 `statsData.exams`。`scripts/verify-student-dashboard-contract.js` 与 `scripts/verify-student-jwt-pages.js` 已加严，相关静态同步检查、student JWT 页面链、student browser CRUD smoke 与 teacher browser CRUD smoke 全部通过。 |
| 2026-05-24 | Codex | 收正 `student-dashboard.html` 学习时间图 connected-sources 路径：当前 JWT 微服务环境下 `/api/student/study-time-distribution` 已能稳定返回真实学习时间数组，但首页 connected-sources 路径此前仍会把 `chartData.studyTime` 直接写成空数组转换结果，导致页面容易把真数据画成空图。现已新增共享 helper `populateDashboardStudyTimeFromConnectedSources(studentAPI, chartData)`，统一服务于 connected-sources 路径、综合表现成功分支与 fallback 分支，并新增 `scripts/verify-student-dashboard-runtime.js` 直接验证 study-time API 返回非零数据且 ECharts `studyTimeChart` 实际渲染出非零 series；配套 contract、静态同步、student/teacher JWT 页面链与 browser smoke 均已 fresh 通过。 |
- 2026-05-24：`student-stats.html` 已把两条 JWT 误降级链接回真实能力。其一，知识点区域不再在 JWT 微服务环境下硬编码为“暂未接通”，而是直接调用已真实接通的 `/api/student/knowledge-points`，初始加载、刷新按钮与 student JWT 页面链现在都会命中该接口并渲染真实知识点卡片；其二，学习时长图不再在 JWT 路径下通过 `applyJwtStatsOnlyFallback()` / `changeTimeChartType()` 强制回退为空图，而是改为真实调用 `/api/student/study-time-distribution`，初始加载与“每日/每周”切换都会重新取数并渲染包含非零值的 Chart.js series。配套新增并扩展了 `scripts/verify-student-stats-runtime.js`、收紧了 `scripts/verify-student-jwt-pages.js`，并已顺序通过 `node .\scripts\verify-student-stats-contract.js`、`node .\scripts\verify-student-stats-runtime.js .\.runtime-logs\student-session-polish-fresh.json`、`powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1`、顺序执行的 `powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1 -CheckOnly`、`node .\scripts\verify-student-jwt-pages.js .\.runtime-logs\student-session-polish-fresh.json`、`node .\scripts\verify-student-browser-crud.js .\.runtime-logs\teacher-session-polish-fresh.json .\.runtime-logs\student-session-polish-fresh.json`、`node .\scripts\verify-teacher-jwt-pages.js .\.runtime-logs\teacher-session-polish-fresh.json`、`node .\scripts\verify-teacher-browser-crud.js .\.runtime-logs\teacher-session-polish-fresh.json .\.runtime-logs\student-session-polish-fresh.json`。
- 2026-05-24：`student-stats.html` 已进一步把 JWT 路径上的真实统计链接回后端。继续核实后确认：当前 `/api/student/stats` 与 `/api/student/scores` 在 fresh student JWT 会话下也都稳定返回 `200`，但页面此前仍在 `isJwtStatsOnlyMode()` 分支里主动绕开这两条接口，只保留空统计卡与空成绩图。现已将 JWT 初始加载与“应用筛选”路径改为真实调用 `fetchStudentStats(filters)` 与 `fetchScoreTrend(filters)`，仅保留学习计划仍通过 `generateStudyPlanFromCourses(filters)` 做诚实 fallback。配套扩展了 `scripts/verify-student-stats-runtime.js`，新增对 `/api/student/stats`、`/api/student/scores` 调用以及卡片/成绩图真实渲染的运行时检查，并收紧 `scripts/verify-student-jwt-pages.js` 要求 `student-stats` 页面在 JWT 环境下必须命中 `/api/student/stats`、`/api/student/scores`、`/api/student/study-time-distribution`、`/api/student/knowledge-points`。已顺序通过 `node .\scripts\verify-student-stats-contract.js`、`node .\scripts\verify-student-stats-runtime.js .\.runtime-logs\student-session-polish-fresh.json`、`powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1`、顺序执行的 `powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1 -CheckOnly`、`node .\scripts\verify-student-jwt-pages.js .\.runtime-logs\student-session-polish-fresh.json`、`node .\scripts\verify-student-browser-crud.js .\.runtime-logs\teacher-session-polish-fresh.json .\.runtime-logs\student-session-polish-fresh.json`、`node .\scripts\verify-teacher-jwt-pages.js .\.runtime-logs\teacher-session-polish-fresh.json`、`node .\scripts\verify-teacher-browser-crud.js .\.runtime-logs\teacher-session-polish-fresh.json .\.runtime-logs\student-session-polish-fresh.json`。
- 2026-05-24：`student-stats.html` 已进一步把课程筛选在 JWT 环境下接回真实课程链。继续核实后确认：student 微服务 capability 中 `courses` 已为 `true`，`/api/student/courses` 在 fresh student JWT 会话下也稳定返回真实课程列表，且 `/api/student/stats`、`/api/student/scores`、`/api/student/study-time-distribution`、`/api/student/knowledge-points` 全部支持 `courseId` 过滤；但页面此前在 `loadCourses()` 里遇到 `isJwtStatsOnlyMode()` 会直接 `return`，导致 JWT 学习数据页课程下拉永远只有“全部课程”，筛选 UI 与底层真实过滤能力脱节。现已移除这层前端自锁，让 `student-stats` 在 JWT 环境下也真实拉取学生课程列表，并通过课程下拉把 `courseId` 一并传到底层四条已接通接口。配套扩展 `scripts/verify-student-stats-runtime.js`，新增“初始加载命中 `/api/student/courses` 并填充课程选项”“选择具体课程后，`/api/student/stats`、`/api/student/scores`、`/api/student/study-time-distribution`、`/api/student/knowledge-points` 都会携带 `courseId` 重新取数”的运行时检查，并收紧 `scripts/verify-student-jwt-pages.js` 要求 `student-stats` 在 JWT 页面链中也必须命中 `/api/student/courses`。已顺序通过 `node .\scripts\verify-student-stats-contract.js`、`node .\scripts\verify-student-stats-runtime.js .\.runtime-logs\student-session-polish-fresh.json`、`powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1`、`powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1 -CheckOnly`、`node .\scripts\verify-student-jwt-pages.js .\.runtime-logs\student-session-polish-fresh.json`、`node .\scripts\verify-student-browser-crud.js .\.runtime-logs\teacher-session-polish-fresh.json .\.runtime-logs\student-session-polish-fresh.json`、`node .\scripts\verify-teacher-jwt-pages.js .\.runtime-logs\teacher-session-polish-fresh.json`、`node .\scripts\verify-teacher-browser-crud.js .\.runtime-logs\teacher-session-polish-fresh.json .\.runtime-logs\student-session-polish-fresh.json`。
- 2026-05-24：`student-settings.html` 头像上传能力已完全接回真实 JWT 运行时链路。继续追查后确认：后端 `/api/student/upload-avatar` 实际早已稳定支持 JSON `{ avatar: dataUrl }`，但学生前端仍把它当成 `multipart/form-data` 文件上传来调，且 `/api/frontend/capabilities` 在 gateway edge 上还把 `student.avatarUpload` 下发成 `false`，导致页面即使拿到了图片文件也会在本地 capability guard 中直接短路成“暂未提供头像上传接口”。现已一并收正：`frontend/dist/api.js` 与 `student-settings.html` 改为 data URL -> JSON 上传链并移除旧 multipart/unsupported 分支；`gateway` 的 `FrontendCapabilityEdgeController` 与 `major_assignment` 的 `FrontendCapabilityController` 均已将 `avatarUpload` 设为 `true`；新增 `scripts/verify-student-settings-avatar-contract.js` 与 `scripts/verify-student-settings-avatar-runtime.js`，runtime verifier 现已改用 Playwright 原生 `setInputFiles` 和真实 `#messageContainer .message-text` 成功提示检查，直接验证 `/api/student/upload-avatar` 返回 `200` 且页面不再展示“当前 JWT 微服务环境暂未提供头像上传接口”。本轮已顺序通过 `mvn --% -pl major_assignment -Dtest=FrontendCapabilityControllerTest test`、`mvn --% -pl gateway -am -Dtest=FrontendCapabilityEdgeControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`、`mvn --% -pl gateway -am -DskipTests package`、`powershell -ExecutionPolicy Bypass -File .\scripts\start-runtime-smoke-stack.ps1`、`node .\scripts\verify-student-settings-avatar-contract.js`、`node .\scripts\verify-student-settings-avatar-runtime.js .\.runtime-logs\student-session-polish-fresh.json`、`node .\scripts\verify-student-jwt-pages.js .\.runtime-logs\student-session-polish-fresh.json`、`powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1`、`powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1 -CheckOnly`、`node .\scripts\verify-student-browser-crud.js .\.runtime-logs\teacher-session-polish-fresh.json .\.runtime-logs\student-session-polish-fresh.json`、`node .\scripts\verify-teacher-jwt-pages.js .\.runtime-logs\teacher-session-polish-fresh.json`、`node .\scripts\verify-teacher-browser-crud.js .\.runtime-logs\teacher-session-polish-fresh.json .\.runtime-logs\student-session-polish-fresh.json`。
- 2026-05-24：继续补强 `student-settings.html` 头像上传的 runtime 口径记录。复盘后确认，这条链真正把请求挡在页面内的，是 JWT 运行时 `/api/frontend/capabilities` 仍错误下发 `student.avatarUpload=false`；所以即使前端协议已改对，页面也会先在 capability guard 中短路。现已补齐 gateway 测试与运行构件，重启后再次实测 `/api/frontend/capabilities` 已真实返回 `avatarUpload: true`，页面则统一走“文件选择 -> FileReader -> JSON `{ avatar: dataUrl }` 上传”单一路径。相关专项 contract/runtime verifier、student JWT 页面链、student browser CRUD smoke、teacher JWT 页面链与 teacher browser CRUD smoke 均已 fresh 通过。
- 2026-05-24：`student-settings.html` 的通知设置与隐私设置已收正为“本浏览器本地优先”的诚实语义。继续横向审计后确认：user-service 当前 `getNotificationSettings/getPrivacySettings` 仍返回固定默认值，`updateNotificationSettings/updatePrivacySettings` 仅回 `true` 占位；但页面此前会先把设置写入 localStorage，再调用这两条占位 API，并在成功时提示“通知设置保存成功 / 隐私设置保存成功”，刷新后又会被服务端默认值覆盖，形成“像真持久化、实际没持久化”的错位。现已为通知/隐私 section 增加“当前环境下…以本浏览器保存为准”的静态说明，保存动作仅写入当前用户 localStorage 并提示“已保存在当前浏览器”，加载动作也只读取本地偏好，不再调用占位的通知/隐私设置 API。新增 `scripts/verify-student-settings-preferences-contract.js` 与 `scripts/verify-student-settings-preferences-runtime.js`，并同步更新 `scripts/verify-student-jwt-pages.js` 对 `student-settings` 的页面期望；相关 contract/runtime、student/teacher JWT 页面链、student/teacher browser CRUD smoke 与前端静态同步均已 fresh 通过。补充说明：本轮 `sync-frontend-to-static.ps1 -CheckOnly` 对 `student-settings.html` 的 `[DIFF]` 经 SHA256 复核为并行执行竞态误报，源文件与静态副本内容一致，并非真实不同步。
- 2026-05-24：`student-settings.html` 个人资料区已收正为“真实账号资料 + 本地补充信息 + 只读班级”的诚实语义。后端 `UpdateStudentProfileDTO` 当前只稳定支持 `realName / email / phone / avatar`，但页面此前会把 `major / grade / className` 一并提交，并统一提示“个人信息保存成功”，容易让用户误以为专业、年级、班级都已同步到账号资料。现已明确拆分：姓名/邮箱/手机号继续走真实 `/api/student/profile` 持久化；专业/年级改为当前浏览器补充信息并按 `studentAcademicProfile:{userId}` 本地保存；班级改为只读展示，并补充“班级由系统班级关系自动生成，当前页不可手动修改”的说明。新增 `scripts/verify-student-settings-profile-contract.js` 与 `scripts/verify-student-settings-profile-runtime.js`，runtime verifier 会检查 profile API 请求体不再包含 `major / grade / className`，并验证重载后姓名通过真实接口保留、专业/年级通过本地信息保留、班级保持服务端派生值不变。配套 `scripts/verify-student-jwt-pages.js`、静态同步检查、student/teacher JWT 页面链与 student/teacher browser CRUD smoke 均已 fresh 通过。
- 2026-05-24：`teacher-student-dashboard.html` 成绩趋势图已移除默认占位曲线兜底。页面此前在 `/api/teacher/score-trend` 为空或失败时会回退一组周/月/三个月/学期默认分数线，并提示“已显示占位数据”，语义上会把无数据或失败态误导成真实趋势。现已改为诚实空态：空结果显示“当前筛选下暂无成绩趋势数据”，失败显示“成绩趋势数据暂不可用，请稍后重试”，并通过 `clearScoreTrendEmptyState(...)`、`renderScoreTrendEmptyState(...)`、`buildScoreTrendEmptyChart(...)` 统一渲染空趋势图，不再伪造默认分数序列。新增 `scripts/verify-teacher-student-dashboard-runtime.js` 直接模拟空结果与异常两条路径，配套 contract、静态同步、teacher JWT 页面链与 teacher browser CRUD smoke 均已 fresh 通过。
- 2026-05-24：`teacher-settings.html` 改密码链已修正为与当前兼容后端真实要求对齐。继续审计确认：`POST /api/auth/change-password` 当前要求请求体中同时存在 `currentPassword / newPassword / confirmPassword` 三项密码字段，否则会直接返回“密码不能为空”或“两次输入的密码不一致”；但教师设置页此前虽然读取了确认密码输入框，却只提交了 `currentPassword / newPassword`，导致页面本地校验通过后 runtime 仍会失败。现已把 `confirmPassword` 正式补回请求体，并新增 `scripts/verify-teacher-settings-contract.js` 与 `scripts/verify-teacher-settings-runtime.js` 两层护栏：runtime verifier 会在浏览器里真实填写三项密码、拦截 `/api/auth/change-password`，确认请求体包含三项字段且页面显示“密码更新成功，请重新登录”。配套 `sync-frontend-to-static.ps1`、`-CheckOnly`、teacher JWT 页面链与 teacher browser CRUD smoke 均已 fresh 通过。
- 2026-05-24：`student-notifications.html` 的通知操作链已改为“写后回读”语义。页面此前在标记已读、全部已读、删除通知、清空已读后，会直接在当前 DOM 上修改 `read` 类、移除按钮并重新统计本地 `.notification-item:not(.read)` 数量；在分页或“未读/已读”过滤视图下，这会让页面状态与服务端真实通知状态脱节。现已引入 `notificationListState` 与统一的 `refreshUnreadNotificationCount()` / `refreshNotificationListView()`，四类变更操作成功后都会重新请求服务器未读数量和当前筛选列表，不再靠当前 DOM 猜测结果。新增 `scripts/verify-student-notifications-contract.js` 与 `scripts/verify-student-notifications-runtime.js`，并已顺序通过前端静态同步检查、student JWT 页面链与 student browser CRUD smoke。
- 2026-05-24 student-settings 能力口径纠偏：user-service 的学生通知偏好/隐私设置仍为兼容占位实现，尚未提供真实账号级持久化，因此将 gateway、monolith 与前端默认 capability 的 `notificationSettings`、`privacySettings` 全部从 `true` 收正为 `false`，保持与页面“当前环境下以本浏览器保存为准”的实际行为一致；同时保留头像上传 capability 为 `true`。相关 contract、架构测试、JWT 页面链与 browser CRUD smoke 均已 fresh 跑绿。
- 2026-05-24：`teacher-courses` 的班级 metadata 链已接回真实专业数据并补齐初始化时序。继续审计确认：`/api/teacher/majors` 在 course-service 的 `TeacherCourseAdminController` 已真实接通，但 `frontend/dist/teacher-courses-metadata.js` 之前既保留硬编码五专业名单，又让 `loadGradeAndMajorData()` 这条本该负责真实 metadata 的链几乎不参与页面启动流程，导致搜索区与“新增班级/编辑班级”里的专业下拉在很多路径下要么靠后续班级搜索顺手补齐，要么直接退回固定五专业。现已新增 `loadTeacherMajors()`、`teacherMajorMetadata`、`ensureClassMetadataLoaded()`，页面启动、切到班级标签、打开新增/编辑班级弹窗时都会优先拉取真实 `/api/teacher/majors` 并填充专业下拉；班级数据继续负责补充年级与班级上已有专业，但接口失败时不再回退硬编码专业目录。新增 `scripts/verify-teacher-courses-metadata-runtime.js`，直接验证页面会真实命中 `/api/teacher/majors` 且新增班级弹窗中的专业/年级下拉已完成填充。已顺序通过 `node .\\scripts\\verify-teacher-courses-metadata-contract.js`、`node .\\scripts\\verify-teacher-courses-metadata-runtime.js .\\.runtime-logs\\teacher-session-polish-fresh.json`、`powershell -ExecutionPolicy Bypass -File .\\scripts\\sync-frontend-to-static.ps1`、`powershell -ExecutionPolicy Bypass -File .\\scripts\\sync-frontend-to-static.ps1 -CheckOnly`、`node .\\scripts\\verify-teacher-jwt-pages.js .\\.runtime-logs\\teacher-session-polish-fresh.json`、`node .\\scripts\\verify-teacher-browser-crud.js .\\.runtime-logs\\teacher-session-polish-fresh.json .\\.runtime-logs\\student-session-polish-fresh.json`。
- 2026-05-24：`teacher-courses` 的课程分配列表已移除过时的“联调环境暂不可用”空态分支。课程分配能力当前已由 runtime CRUD 验证为真实可用，因此此前 `teacher-courses-assignments.js` 中在接口失败时回退“当前联调环境的课程分配列表暂不可用，不影响课程新增与课程列表使用。”的分支会误导真实错误语义。现已统一改为使用 `assignments-error` 呈现真实失败信息，并补充 `scripts/verify-teacher-courses-assignments-contract.js` 与 `scripts/verify-teacher-courses-assignments-runtime.js`，分别钉住源码中旧降级话术不得回流、以及接口返回 `503` 时页面必须显示真实错误态而非旧空态；静态同步检查、teacher JWT 页面链与 teacher browser CRUD smoke 已顺序 fresh 通过。
- 2026-05-24：`teacher-assignments` 已移除启动时先灌 `mockCourses` 的旧初始化壳。当前教师作业与考试页已稳定通过 `/api/teacher/courses` 获取真实课程列表，因此此前 `bootstrapTeacherAssignmentsPage()` 里先 `populateAllCourseDropdowns(mockCourses)`、再异步等待 `loadTeacherAssignmentCourses()` 的做法，会让课程筛选与发布弹窗短暂展示一组本地伪课程。现已删除 `mockCourses` 预灌路径，改为在页面 bootstrap 时直接等待真实课程列表后再进入初始 tab；并新增 `scripts/verify-teacher-assignments-shell-contract.js` 与 `scripts/verify-teacher-assignments-runtime.js`，分别约束源码中 `mockCourses` 初始化不得回流，以及页面运行时课程下拉必须来自真实 `/api/teacher/courses`。静态同步检查、teacher JWT 页面链与 teacher browser CRUD smoke 已顺序 fresh 通过。
- 2026-05-24：`teacher-courses` 的课程类别/课程状态枚举链已收正为兼容历史中文值与当前代码值。runtime 现状确认：课程列表中同时存在 `practice/active` 与 `必修/进行中` 两套历史值，而 course-service 列表筛选仍按字符串精确匹配；页面下拉此前只提供中文值，因此会带出“代码值课程搜索不到 / 编辑回填丢值”的真行为缺口。现已引入统一枚举配置，以“代码值 value + 中文 label”填充筛选与新增/编辑表单；搜索链新增历史别名兼容，让 `practice/实践课`、`active/进行中` 等历史值都能被同一页面筛选覆盖；编辑链新增 `ensureSelectHasOption(...)` 兜底，保证代码值课程打开编辑弹窗时对应 select 不会掉空。配套新增 `scripts/verify-teacher-courses-enum-contract.js` 与 `scripts/verify-teacher-courses-enum-runtime.js`，并已顺序通过前端静态同步检查、teacher JWT 页面链与 teacher browser CRUD smoke。
- 2026-05-24：`student-courses.html` 的课程类别/课程状态筛选链已收正为兼容当前代码值与历史中文值。fresh student JWT runtime 已确认 `/api/student/courses` 真正返回 `practice/active` 这类代码值课程，但页面筛选下拉此前仍只提供中文值，并直接把 `courseCategory/courseStatus` 透传给接口，因此代码值课程无法通过页面筛选稳定命中。现已将 student 课程页下拉统一为“代码值 value + 中文 label”，并在前端查询链中加入历史别名兼容，让 `practice/实践课`、`active/进行中` 等历史值都能通过同一筛选覆盖；HTML 模板中的旧中文静态 option 也已移除。配套新增 `scripts/verify-student-courses-contract.js` 与 `scripts/verify-student-courses-runtime.js`，并已顺序通过前端静态同步检查、student JWT 页面链与 student browser CRUD smoke。
- 2026-05-24：收正 `teacher-settings.html` 的通知偏好语义。页面现已不再把通知偏好描述成“只保存在当前浏览器”，而是改为真实调用 `PUT /api/auth/notification-settings` 做会话级同步，并保留本地浏览器副本。由于兼容后端当前返回 `data.persisted=false`、`data.mode=compatibility-placeholder`，页面文案与成功提示已明确说明这只是当前登录会话同步，尚未接通账号级持久化。配套扩展 `scripts/verify-teacher-settings-contract.js` 与 `scripts/verify-teacher-settings-runtime.js`，并已通过 contract、runtime、前端静态同步、teacher JWT 页面链与 teacher browser CRUD smoke 验证。
- 2026-05-24：`student-assignments.html` 的考试/成绩 tab 已正式纳入专项 runtime 与 JWT 页面链回归。继续核实后确认：这页的考试列表、成绩查询接口当前在 fresh student JWT 会话下都已真实可用，但此前系统级 `scripts/verify-student-jwt-pages.js` 只检查默认首屏作业 tab，因此考试/成绩两条链虽然能用，却没有被页面链正式钉住；页面底部注释也还保留“避免未切通的考试/成绩链路制造噪音”的旧阶段表述。现已收正注释语义，并新增 `scripts/verify-student-assignments-contract.js` 与 `scripts/verify-student-assignments-runtime.js`：runtime verifier 会主动切到考试、成绩两个 tab，验证页面真实命中 `/api/student/exams`、`/api/student/scores` 并渲染考试项、成绩行和课程筛选；同时 `scripts/verify-student-jwt-pages.js` 也已补上对 `student-assignments` 这两条接口的页面级期望，并修正为会主动点开懒加载 tab 后再判定命中结果。相关 contract、runtime、student JWT 页面链、student browser CRUD smoke、teacher JWT 页面链与 teacher browser CRUD smoke 均已 fresh 通过。
- 2026-05-25：`frontend/dist/api.js` 中 student 侧考试/成绩能力的旧 unsupported 文案已按当前 runtime 真相收正。继续核实后确认：fresh student JWT 会话下 `/api/frontend/capabilities` 已稳定返回 `exams=true`、`examDetail=true`、`examSubmit=true`、`scores=true`，且 `/api/student/exams`、`/api/student/scores` 的专项 runtime 与 JWT 页面链也都真实通过；但 `api.js` 里仍保留“当前 JWT 微服务环境暂未提供考试列表/考试详情/成绩查询/考试提交接口”的旧 capability message 和 fallback 参数，容易让后续页面在意外 fallback 时说旧话。现已仅保留当前仍真实 unsupported 的 `dashboardPerformance` 与 `activities` 文案，并移除 `getExams/getExamDetail/getScores/getExamSubmissions/submitExam/submitExamJson` 中过时的考试/成绩 fallback 提示参数；新增 `scripts/verify-student-api-capability-messages-contract.js` 防止旧文案回流。相关 contract、`student-assignments` runtime、student JWT 页面链、student browser CRUD smoke、teacher JWT 页面链、顺序执行的静态同步与 `-CheckOnly` 均已 fresh 通过。
- 2026-05-25：`teacher-courses` 中残留的 mock 语义壳已进一步收正。继续核实后确认：这条链当前真正的问题不在“页面仍在用假数据”，而在命名本身会误导维护者。`teacher-courses-metadata.js` 中的 `useMockCourseData()` 实际只负责在课程接口失败时展示错误状态，并不回填任何伪课程；`teacher-courses-shell.js` 中的 `mockMethods` 也只是本地考核方式枚举，并非试图伪装成后端元数据。现已将其分别收正为 `showCourseLoadFailureState()` 与 `localAssessmentMethods`，同时把相关日志文案改成“课程数据加载失败状态 / 本地考核方式选项”，避免后续继续把本地枚举和旧 mock 壳混为一谈。配套扩展 `scripts/verify-teacher-courses-metadata-contract.js` 与 `scripts/verify-teacher-courses-shell-contract.js`，并已顺序通过 contract、`verify-teacher-courses-metadata-runtime.js`、teacher browser CRUD smoke 验证。
- 2026-05-25：`teacher-student-dashboard.html` 的学生表格 access-denied fallback 已按真实错误语义收正。此前 `/api/teacher/learning-summary` 在 `401/403` 场景下会被页面统一描述成“当前环境未开放该页面所需数据，已显示空结果”，把登录失效或权限不足伪装成空结果。现已新增 `getStudentTableFallbackMessage(error)`，将 `401` 收正为“登录已过期，正在跳转到登录页”，`403` 收正为“当前账号暂无权限查看学生学习数据”，其余失败仍保留通用加载失败提示。配套扩展 `scripts/verify-teacher-student-dashboard-contract.js` 与 `scripts/verify-teacher-student-dashboard-runtime.js`，并已顺序通过 contract、`powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1`、`powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1 -CheckOnly` 以及 `node .\scripts\verify-teacher-student-dashboard-runtime.js .\.runtime-logs\teacher-session-polish-fresh.json`，运行时已明确验证页面在 learning-summary 返回 `403` 时展示真实 access-denied 文案、不再回流“已显示空结果”旧提示。
- 2026-05-25：`student-settings / teacher-settings` 的 capability 与页面说明已完成最终对齐复核。复核确认：student 侧仍应保持 `notificationSettings=false / privacySettings=false / avatarUpload=true`，其页面语义继续分别对应“通知/隐私以当前浏览器保存为准”和“头像上传接口已真实接通”；teacher 侧则继续保持 `notificationPersistence=false`，并把通知偏好统一描述为“同步到当前登录会话，并保留当前浏览器副本”，不再保留“已同步到当前账号”的备用成功文案，以免超出当前 `persisted=false` 兼容接口的真实能力范围。相关 `verify-student-settings-preferences-contract.js`、`verify-teacher-settings-contract.js`、`verify-student-settings-preferences-runtime.js .\.runtime-logs\student-session-polish-fresh.json`、`verify-teacher-settings-runtime.js .\.runtime-logs\teacher-session-polish-fresh.json`、`sync-frontend-to-static.ps1` 与 `-CheckOnly` 均已 fresh 通过。

- 2026-05-24：`teacher-assignments-exam-publish.js` 的课程知识点下拉已从旧裸 `fetch` + 伪“环境未提供接口”降级链收正为真实 teacher API 取数。继续核实后确认：`GET /api/teacher/knowledge-points/course/{courseId}` 在 fresh teacher JWT 会话下已稳定返回 `200` 且可用于作业/考试发布弹窗；但页面此前仍在 `loadKnowledgePoints(...)` 中直接裸调 `fetch(...)`，并把 `401/403` 一律渲染成“当前环境暂未提供知识点接口”，会把真实鉴权或请求失败误包装成旧联调降级。现已为 `TeacherAPI` 新增 `getKnowledgePointsByCourse(courseId)`，并让 `teacher-assignments-exam-publish.js` 统一改走这条 API helper；接口失败时改为诚实的“知识点暂不可用，请稍后重试 / 加载知识点失败，请稍后重试”，不再回流旧环境文案。配套新增 `scripts/verify-teacher-assignments-exam-publish-contract.js` 与 `scripts/verify-teacher-assignments-knowledge-points-runtime.js` 两层护栏，runtime verifier 会直接在 `teacher-assignments.html` 中选取一个真实存在知识点的课程，并验证作业/考试发布弹窗的知识点下拉真实命中 `/api/teacher/knowledge-points/course/{courseId}` 且完成填充。相关 contract、runtime、静态同步、teacher JWT 页面链与 teacher browser CRUD smoke 已顺序 fresh 通过。

- 2026-05-25：完成本轮剩余问题复盘收口。本轮 Task 1 至 Task 4 已全部落地，当前状态已切换为“待命令执行”，不再默认继续实现新的页面修正。现阶段仅保留三类后续建议：一是清理历史进展文档中的重复记录；二是修整少量遗留乱码控制字符；三是如需继续推进，仅按新的明确单项命令执行。最后一轮实际 fresh 通过的收口 verifier 清单为：`node .\scripts\verify-student-assignments-contract.js`、`node .\scripts\verify-student-assignments-runtime.js .\.runtime-logs\student-session-polish-fresh.json`、`node .\scripts\verify-student-api-capability-messages-contract.js`、`node .\scripts\verify-teacher-student-dashboard-contract.js`、`node .\scripts\verify-teacher-student-dashboard-runtime.js .\.runtime-logs\teacher-session-polish-fresh.json`、`node .\scripts\verify-student-settings-preferences-contract.js`、`node .\scripts\verify-student-settings-preferences-runtime.js .\.runtime-logs\student-session-polish-fresh.json`、`node .\scripts\verify-teacher-settings-contract.js`、`node .\scripts\verify-teacher-settings-runtime.js .\.runtime-logs\teacher-session-polish-fresh.json`、`powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1`、`powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1 -CheckOnly`。
