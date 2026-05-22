---
title: 服务边界与单体收缩清单
version: v0.1
last_updated: 2026-05-22
author: Codex
---

# 服务边界与单体收缩清单

## 1. 文档目的

本文档只解决两件事：

- 当前每一类业务能力应该归属哪个微服务
- `major_assignment` 在后续阶段应该保留什么、收缩什么、最终下线什么

它不是接口明细真相源。接口级实施细节仍以 Gateway 路由配置和任务清单为准。

## 2. 当前总体判断

当前仓库已经进入“微服务为主体、单体为过渡层”的阶段：

- 对外主入口应以 `gateway` 为准
- 新增或继续演进的业务能力应优先进入独立微服务
- `major_assignment` 不再适合作为长期业务主实现，只应承担过渡兼容、局部兜底和少量尚未完全迁移的页面承载职责

因此，后续演进原则是：

1. 业务能力优先收敛到独立服务
2. 单体 Controller 只保留兼容入口，不再扩张业务逻辑
3. Gateway 路由是“是否完成服务归属切换”的外部标志

## 3. 服务边界总表

| 业务域 | 目标服务 | 当前状态 | `major_assignment` 后续定位 |
| --- | --- | --- | --- |
| 认证、登录、角色切换、JWT | `auth-service` | 已独立 | 单体只保留兼容壳与过渡调用，不再新增认证主逻辑 |
| 用户资料、学生/教师档案 | `user-service` | 已独立 | 单体逐步收缩为兼容入口 |
| 课程、班级、专业、教师课程分配、知识点主数据 | `course-service` | 已独立且较成熟 | 单体同类接口逐步停用 |
| 作业、作业提交、教师批改 | `assignment-service` | 已独立并已切部分主链路 | 单体不再承担作业主业务实现 |
| 考试、考试提交、考试评分、成绩聚合 | `exam-service` | 已独立并已切关键主链路 | 单体仅保留过渡兼容 |
| 成绩趋势、知识点掌握、学情预警、分析任务 | `analysis-service` | 已独立并有事件链 | 单体分析类接口应持续迁出 |
| 通知 | `notification-service` | 已独立 | 单体通知接口逐步下线 |
| AI 题目生成、组卷、学习建议 | `ai-service` | 已独立 | 单体 `/api/ai/**` 仅返回迁移提示，不再提供假实现 |
| 历史兼容读接口 | `legacy-adapter` | 已存在最小切片 | 专门承接少量旧接口兼容，不承载新业务 |

## 4. 各模块推荐职责

### 4.1 Gateway

`gateway` 是统一外部入口，负责：

- 路由分发
- JWT 透传与白名单控制
- 限流
- 熔断 fallback
- CORS
- TraceId / 用户上下文传递

判断一条业务链“是否真正迁到微服务”，优先看 Gateway 是否已有稳定路由，而不是只看仓库里有没有 service 模块。

### 4.2 Registry Server

`registry-server` 只负责服务注册发现，不承载业务能力。

### 4.3 Common / *-api

`common` 负责公共基础设施：

- 异常与响应契约
- Feign 公共支持
- MDC / TraceId
- 事件基础设施
- 管理与自动配置

`*-service-api` 负责：

- DTO
- Feign 接口
- 服务间契约

原则上服务之间不应直接依赖别人的实现模块。

### 4.4 major_assignment

`major_assignment` 的推荐新定位：

- 过渡期单体兼容层
- 静态页面/本地集成测试承载者
- 少量尚未完全切流功能的兜底壳

不推荐继续把新的核心业务逻辑堆回 `major_assignment`。

### 4.5 legacy-adapter

`legacy-adapter` 的定位应比 `major_assignment` 更窄：

- 专门承接确实还需要保留的旧 URL / 旧读接口
- 作为旧系统到新服务的过渡适配层

它不应成为第二个单体。

## 5. 单体收缩原则

`major_assignment` 后续收缩时，按下面三类处理：

### 5.1 保留

短期可以保留的内容：

- 启动与测试基座
- 兼容性 Controller 壳
- 会话桥接、过滤器、迁移期安全适配
- 无法一次性外迁的少量历史页面承载

### 5.2 冻结

应冻结、不再继续扩张的内容：

- 与微服务已重复的业务 Controller
- 单体内新的 Repository/Service 主实现
- 单体内新的 AI 业务逻辑

冻结的意思不是立刻删除，而是“不再往里长新逻辑”。

### 5.3 迁出后删除

满足以下条件后可以删除对应单体逻辑：

- Gateway 已切到新服务
- 前端调用已验证走新链路
- 回滚步骤已明确
- 兼容接口已在 `legacy-adapter` 或新服务中有替代

## 6. 下一阶段建议动作

### 6.1 第一优先级

先完成“代码提交 + 路由链路确认”：

1. 提交当前会话入口统一与 AI 迁移提示修复
2. 以 Gateway 为入口做一轮前端联调冒烟
3. 确认前端主要页面不再依赖单体里的假实现

### 6.2 第二优先级

围绕 `major_assignment` 做减法：

1. 梳理哪些 Controller 已经只是兼容壳
2. 标记哪些接口已不应作为生产主入口
3. 将少量必须保留的旧读接口向 `legacy-adapter` 收拢

### 6.3 第三优先级

为最终下线单体做准备：

1. 建立“单体剩余职责清单”
2. 建立“已切流接口清单”
3. 建立“仍依赖单体的页面或脚本清单”

## 6.4 当前已验证的前端主链路

2026-05-22 本地冒烟已经确认，下面这些页面在 `frontend/dist` 预览模式下可以通过
`5500 前端预览 -> Gateway(8080) -> 微服务` 的链路工作，不依赖单体静态资源托管：

| 页面 / 能力 | 验证方式 | 当前结论 |
| --- | --- | --- |
| `teacher-warning.html` | Playwright 页面冒烟 + `/api/early-warnings/**` 响应检查 | 已走 Gateway + `analysis-service` |
| `teacher-student-dashboard.html` | Playwright 页面冒烟 + `/api/teacher/dashboard` / `/api/teacher/learning-summary` 响应检查 | 已走 Gateway + `analysis-service` |
| 登录页验证码 | `verify-login-page-contract.js` | 已走 `/api/auth/captcha`，不再依赖 legacy captcha 路径 |
| 教师 / 学生统一 API 链路 | `verify-gateway-api-smoke.js` | 20 组统一网关烟测通过 |

补充说明：

- 本地 `5500` 预览端口现在不再只是裸 `http.server`，而是轻量前端代理服务：
  - 静态资源来自 `frontend/dist`
  - `/api/**` 自动代理到 `gateway`
- 这样浏览器联调路径与生产部署保持一致，不需要在页面里重新写死 `localhost:8080`

## 6.5 当前仍需继续收缩的单体兼容面

以下内容仍然属于“过渡兼容面”，后续应继续减少对 `major_assignment` 的依赖：

### A. 单体 Controller 兼容壳

仍存在于 `major_assignment` 的主要控制器包括：

- `AuthController.java`
- `CourseController.java`
- `AssignmentController.java`
- `ExamController.java`
- `AnalysisController.java`
- `NotificationController.java`
- `StudentController.java`
- `TeacherDashboardController.java`
- `DashboardController.java`
- `KnowledgePointController.java`
- `KnowledgePointAnalysisController.java`
- `SystemDataController.java`
- `UserController.java`
- `EarlyWarningController.java`

其中多数已经不应再作为长期主实现，只适合：

- 兼容旧 URL
- 兼容旧前端调用
- 过渡期本地对照验证

### B. 仍需继续补页面合同 / 浏览器冒烟的前端页面

当前教师高频页面里，已经做过更强联调验证的是：

- `teacher-warning.html`
- `teacher-student-dashboard.html`

而下面这些页面虽然多数接口在统一 API 烟测里已经覆盖，但还值得继续补“页面级合同”或 Playwright 冒烟：

- `teacher-courses.html`
- `teacher-assignments.html`
- `teacher-knowledge.html`
- `teacher-notifications.html`
- `teacher-settings.html`
- `student-dashboard.html`
- `student-courses.html`
- `student-assignments.html`
- `student-notifications.html`
- `student-settings.html`

### C. 本地烟测脚本与单体基座

以下脚本 / 资产短期内仍应保留，因为它们是迁移期验证工具，不是生产主实现：

- `scripts/start-runtime-smoke-stack.ps1`
- `scripts/frontend_dev_server.py`
- `scripts/verify-gateway-api-smoke.js`
- `scripts/verify-teacher-jwt-pages.js`
- `scripts/verify-login-page-contract.js`
- 以及一组学生/教师页面合同脚本

这些属于“保留的迁移辅助基座”，不代表 `major_assignment` 仍应继续承载业务主逻辑。

## 7. 识别是否该继续放在单体里的判断标准

遇到一个功能时，可以按这个判断：

1. Gateway 是否已有对应业务服务？
2. 对应领域服务中是否已有稳定实体、Service、Repository？
3. 该功能是否属于某个明确业务域，而不是纯兼容逻辑？
4. 如果把它继续放在单体，会不会形成双实现？

只要第 2、3、4 条答案偏向“是”，那它就不应该继续长在 `major_assignment`。

## 8. 当前结论

当前项目的正确发展方向不是“继续强化单体”，而是：

- 用 Gateway 固化外部入口
- 用独立服务承接业务主体
- 用 `major_assignment` 承接过渡期兼容
- 用 `legacy-adapter` 收纳少量历史遗留接口

也就是说，未来目标架构应当是：

```text
前端 / 客户端
    -> Gateway
        -> Auth / User / Course / Assignment / Exam / Analysis / Notification / AI

major_assignment
    -> 仅作为过渡兼容层，逐步收缩
```

## 变更记录

| 日期 | 变更人 | 变更内容 |
| --- | --- | --- |
| 2026-05-22 | Codex | 新增服务边界与单体收缩清单，明确 `major_assignment` 的过渡层定位 |
