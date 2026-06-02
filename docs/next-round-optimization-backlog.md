# 前后端下一轮优化清单（按优先级 + 工作量 + 风险）

> 面向当前 `majorassignment` 项目的下一轮优化建议。  
> 目标不是“为了重构而重构”，而是优先做那些最能提升可维护性、继续开发效率、演示稳定性和长期项目感的改进。

## 使用方式

- `优先级`
  - `P0`：继续迭代前最值得先做
  - `P1`：完成 P0 后尽快做
  - `P2`：有时间再做，属于提质型优化
- `工作量`
  - `S`：0.5-1 天
  - `M`：1-3 天
  - `L`：3-7 天
- `风险`
  - `低`：局部改动，容易回归
  - `中`：会影响多个页面或多个接口
  - `高`：涉及系统边界、认证链路或大范围重构

## 已落地（2026-05-23）

这一轮已经先把一批“高收益、低到中风险”的基础优化做进仓库，后续继续拆分时可以直接在这层基础上推进：

- 已新增前端双目录同步脚本：
  [scripts/sync-frontend-to-static.ps1](D:/111/Distributed framework technology/JavaCode/majorassignment/scripts/sync-frontend-to-static.ps1:1)
- 已新增前端公共 UI 脚本：
  [frontend/dist/common-ui.js](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/common-ui.js:1)
  和
  [major_assignment/src/main/resources/static/common-ui.js](D:/111/Distributed framework technology/JavaCode/majorassignment/major_assignment/src/main/resources/static/common-ui.js:1)
- 已把学生端 3 个高频页面接入公共逻辑层：
  [frontend/dist/student-dashboard.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/student-dashboard.html:1)
  [frontend/dist/student-courses.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/student-courses.html:1)
  [frontend/dist/student-notifications.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/student-notifications.html:1)
- 已继续把学生端设置页和学习数据页接入公共逻辑层：
  [frontend/dist/student-settings.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/student-settings.html:1)
  [frontend/dist/student-stats.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/student-stats.html:1)
- 已把教师端 3 个高频页面接入公共逻辑层，统一教师会话检查、侧边栏加载和用户下拉逻辑：
  [frontend/dist/teacher-dashboard.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-dashboard.html:1)
  [frontend/dist/teacher-courses.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-courses.html:1)
  [frontend/dist/teacher-assignments.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-assignments.html:1)
- 已继续把教师端重复的 UI helper 收进公共层，统一 toast 通知、资源加载失败提示、按钮级加载态，并让仪表盘切回共享侧边栏加载入口：
  [frontend/dist/common-ui.js](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/common-ui.js:1)
  [frontend/dist/teacher-dashboard.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-dashboard.html:1)
  [frontend/dist/teacher-courses.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-courses.html:1)
- 已把 `teacher-courses` 的课程分配子模块拆成独立脚本，降低单页内联脚本体积，同时保留原有按钮、分页和模态框入口：
  [frontend/dist/teacher-courses-assignments.js](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-courses-assignments.js:1)
  [frontend/dist/teacher-courses.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-courses.html:1)
- 已继续把 `teacher-courses` 的学生弹窗子模块拆成独立脚本，承接“查看班级学生 / 添加学生到当前班级”逻辑，并补齐同步脚本白名单：
  [frontend/dist/teacher-courses-students.js](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-courses-students.js:1)
  [frontend/dist/teacher-courses.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-courses.html:1)
  [scripts/sync-frontend-to-static.ps1](D:/111/Distributed framework technology/JavaCode/majorassignment/scripts/sync-frontend-to-static.ps1:1)
- 已继续把 `teacher-courses` 的班级编辑子模块拆成独立脚本，承接“打开编辑班级模态框 / 表单实时校验 / 提交更新班级”逻辑，并补齐同步脚本白名单：
  [frontend/dist/teacher-courses-class-edit.js](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-courses-class-edit.js:1)
  [frontend/dist/teacher-courses.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-courses.html:1)
  [scripts/sync-frontend-to-static.ps1](D:/111/Distributed framework technology/JavaCode/majorassignment/scripts/sync-frontend-to-static.ps1:1)
- 已继续把 `teacher-courses` 的课程 CRUD 子模块拆成独立脚本，承接“新增课程 / 打开编辑课程模态框 / 提交编辑课程 / 删除课程”逻辑，并补齐同步脚本白名单：
  [frontend/dist/teacher-courses-course-crud.js](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-courses-course-crud.js:1)
  [frontend/dist/teacher-courses.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-courses.html:1)
  [scripts/sync-frontend-to-static.ps1](D:/111/Distributed framework technology/JavaCode/majorassignment/scripts/sync-frontend-to-static.ps1:1)
- 已开始拆分 `teacher-assignments`，首批将作业 CRUD 子模块外置，承接“查看作业 / 编辑作业 / 删除作业 / 提交编辑作业”逻辑，并补齐同步脚本白名单：
  [frontend/dist/teacher-assignments-assignment-crud.js](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-assignments-assignment-crud.js:1)
  [frontend/dist/teacher-assignments.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-assignments.html:1)
  [scripts/sync-frontend-to-static.ps1](D:/111/Distributed framework technology/JavaCode/majorassignment/scripts/sync-frontend-to-static.ps1:1)
- 已继续拆分 `teacher-assignments`，将考试 CRUD 子模块外置，承接“查看考试 / 编辑考试 / 删除考试 / 提交编辑考试”逻辑，并补齐同步脚本白名单：
  [frontend/dist/teacher-assignments-exam-crud.js](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-assignments-exam-crud.js:1)
  [frontend/dist/teacher-assignments.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-assignments.html:1)
  [scripts/sync-frontend-to-static.ps1](D:/111/Distributed framework technology/JavaCode/majorassignment/scripts/sync-frontend-to-static.ps1:1)
- 已继续拆分 `teacher-assignments`，将考试发布与课程/知识点选项加载子模块外置，承接“发布考试 / 加载课程下拉选项 / 加载知识点 / 绑定模态框与课程切换事件”逻辑，并补齐同步脚本白名单：
  [frontend/dist/teacher-assignments-exam-publish.js](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-assignments-exam-publish.js:1)
  [frontend/dist/teacher-assignments.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-assignments.html:1)
  [scripts/sync-frontend-to-static.ps1](D:/111/Distributed framework technology/JavaCode/majorassignment/scripts/sync-frontend-to-static.ps1:1)
- 已继续拆分 `teacher-assignments`，将提交记录筛选与作业/考试下拉缓存子模块外置，承接“缓存作业考试选项 / 填充提交记录筛选下拉 / 提交记录搜索按钮绑定”逻辑，并补齐同步脚本白名单：
  [frontend/dist/teacher-assignments-submission-filters.js](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-assignments-submission-filters.js:1)
  [frontend/dist/teacher-assignments.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-assignments.html:1)
  [scripts/sync-frontend-to-static.ps1](D:/111/Distributed framework technology/JavaCode/majorassignment/scripts/sync-frontend-to-static.ps1:1)
- 已把 `@RequireLogin` 扩展到支持类级别标注，并开始把预警控制器切回统一鉴权/异常出口：
  [AuthenticationAspect.java](D:/111/Distributed framework technology/JavaCode/majorassignment/major_assignment/src/main/java/com/_202510007517/major_assignment/aspect/AuthenticationAspect.java:1)
  [EarlyWarningController.java](D:/111/Distributed framework technology/JavaCode/majorassignment/major_assignment/src/main/java/com/_202510007517/major_assignment/controller/EarlyWarningController.java:1)
- 已新增第二个类级别统一鉴权样板，收口教师分析控制器中的重复 401 逻辑：
  [AnalysisController.java](D:/111/Distributed framework technology/JavaCode/majorassignment/major_assignment/src/main/java/com/_202510007517/major_assignment/controller/AnalysisController.java:1)
  [AnalysisControllerTest.java](D:/111/Distributed framework technology/JavaCode/majorassignment/major_assignment/src/test/java/com/_202510007517/major_assignment/controller/AnalysisControllerTest.java:1)
- 已把学生 JWT 页面 smoke 覆盖扩展到学习数据页，并校准 JWT-only 环境下的验证口径：
  [scripts/verify-student-jwt-pages.js](D:/111/Distributed framework technology/JavaCode/majorassignment/scripts/verify-student-jwt-pages.js:1)
- 已补齐前端能力矩阵接口在两条运行时链路上的一致性：
  [FrontendCapabilityController.java](D:/111/Distributed framework technology/JavaCode/majorassignment/major_assignment/src/main/java/com/_202510007517/major_assignment/controller/FrontendCapabilityController.java:1)
  [FrontendCapabilityEdgeController.java](D:/111/Distributed framework technology/JavaCode/majorassignment/gateway/src/main/java/com/_202510007517/platform/gateway/controller/FrontendCapabilityEdgeController.java:1)
- 已为教师端 smoke 增加 JWT 过期前置检查，避免把会话过期误判成页面或 CRUD 回退：
  [scripts/verify-teacher-jwt-pages.js](D:/111/Distributed framework technology/JavaCode/majorassignment/scripts/verify-teacher-jwt-pages.js:1)
  [scripts/verify-teacher-browser-crud.js](D:/111/Distributed framework technology/JavaCode/majorassignment/scripts/verify-teacher-browser-crud.js:1)

---

## P0

### 1. 拆分超大前端页面和公共脚本

- 优先级：`P0`
- 工作量：`L`
- 风险：`中`

**为什么先做**

当前前端已经能稳定交付，但几个核心文件体量明显过大，后续再叠功能会越来越难维护：

- [frontend/dist/api.js](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/api.js:1) 约 `4268` 行
- [frontend/dist/teacher-courses.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-courses.html:1) 约 `5309` 行
- [frontend/dist/teacher-assignments.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-assignments.html:1) 约 `2946` 行
- [frontend/dist/student-settings.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/student-settings.html:1) 约 `1416` 行

**建议拆分方向**

- `api.js`
  - 认证与会话恢复
  - 教师 API 封装
  - 学生 API 封装
  - 通用错误提示/能力降级
- 教师页面
  - 表格分页/筛选
  - 作业与考试 modal
  - 仪表盘统计卡片和图表
- 学生页面
  - 侧边栏状态
  - 设置页本地偏好保存
  - 统一消息提示组件

**收益**

- 后续加功能更快
- 问题定位更容易
- 自动化脚本更不容易被页面内部改动带崩

---

### 2. 统一后端接口返回契约

- 优先级：`P0`
- 工作量：`M`
- 风险：`中`

**为什么先做**

这轮前端之所以做了不少兜底，根因之一是接口返回风格并不完全统一。前端当前要兼容：

- `response.data`
- `response.success + response.data`
- 直接返回数据对象
- 认证失败时不同 message 文案

这会让前端页面里出现很多“如果是这种结构就这样取，否则那样取”的逻辑。

**建议统一内容**

- 成功响应格式统一
- 失败响应格式统一
- 分页接口统一使用同一字段结构
- 401 / 403 / 404 / 409 等错误语义统一
- “当前环境能力不可用”的返回结构统一

**优先涉及文件**

- [major_assignment/src/main/java/com/_202510007517/major_assignment/controller/GlobalExceptionHandler.java](D:/111/Distributed framework technology/JavaCode/majorassignment/major_assignment/src/main/java/com/_202510007517/major_assignment/controller/GlobalExceptionHandler.java:1)
- [major_assignment/src/main/java/com/_202510007517/major_assignment/controller/AuthController.java](D:/111/Distributed framework technology/JavaCode/majorassignment/major_assignment/src/main/java/com/_202510007517/major_assignment/controller/AuthController.java:1)
- 各业务 Controller 的分页和错误返回

**收益**

- 前端可以删掉一批兼容分支
- 接口文档更清晰
- 新页面接入成本更低

---

### 3. 建立前端产物同步流程，结束“双目录人工同步”

- 优先级：`P0`
- 工作量：`M`
- 风险：`低`

**为什么先做**

当前真实前端内容存在两套：

- `frontend/dist`
- `major_assignment/src/main/resources/static`

这次收尾我已经把两边同步到了同一版本，但长期靠人工复制，后面很容易再次漂移。

**建议做法**

- 明确 `frontend/dist` 为唯一源
- 增加一个同步脚本，例如：
  - `scripts/sync-frontend-to-static.ps1`
  - 或 Maven/Gradle 资源复制步骤
- 在文档里写明：
  - 修改前端改哪里
  - 启动后端前如何同步
  - 发布前如何校验两边一致

**收益**

- 避免“本地预览一版、后端跑出来另一版”
- 降低交付前人工操作风险

---

## P1

### 4. 拆分超大的后端 Controller 和 Service

- 优先级：`P1`
- 工作量：`L`
- 风险：`中`

**现状证据**

当前单体后端里已有多个明显过大的类：

- [AssignmentController.java](D:/111/Distributed framework technology/JavaCode/majorassignment/major_assignment/src/main/java/com/_202510007517/major_assignment/controller/AssignmentController.java:1) 约 `869` 行
- [TeacherDashboardController.java](D:/111/Distributed framework technology/JavaCode/majorassignment/major_assignment/src/main/java/com/_202510007517/major_assignment/controller/TeacherDashboardController.java:1) 约 `841` 行
- [ExamController.java](D:/111/Distributed framework technology/JavaCode/majorassignment/major_assignment/src/main/java/com/_202510007517/major_assignment/controller/ExamController.java:1) 约 `771` 行
- [TeacherDashboardServiceImpl.java](D:/111/Distributed framework technology/JavaCode/majorassignment/major_assignment/src/main/java/com/_202510007517/major_assignment/service/impl/TeacherDashboardServiceImpl.java:1) 约 `721` 行

**建议拆分方式**

- `AssignmentController`
  - 作业管理
  - 提交记录
  - 批改
- `TeacherDashboardController`
  - 仪表盘统计
  - 图表数据
  - 最近活动/预警数据
- `ExamController`
  - 考试 CRUD
  - 考试提交/成绩

**收益**

- 降低改动耦合
- 单元测试更容易补
- 后续迁移到微服务边界时更顺

---

### 5. 把“能力降级”变成显式配置，而不是页面各自猜

- 优先级：`P1`
- 工作量：`M`
- 风险：`中`

**现状**

前端现在已经做了很多环境降级处理，比如学生设置页会根据环境决定是否走本地保存、教师通知设置会降级为浏览器本地保存。这个方向是对的，但目前仍然分散在页面里自己判断。

**建议**

- 增加统一 capability/config 接口
- 或在登录态里下发能力矩阵
- 例如：
  - `teacherNotificationPersistence: false`
  - `studentAvatarUpload: false`
  - `knowledgeAnalysisExport: partial`

**收益**

- 页面逻辑更干净
- 不必每页重复写环境判断
- 演示环境和正式环境切换更自然

---

### 6. 增加关键接口的契约测试和回归测试矩阵

- 优先级：`P1`
- 工作量：`M`
- 风险：`低`

**为什么值得做**

现在浏览器 smoke 已经很有价值，但更多覆盖的是“主流程能不能走通”。下一步适合补一点更稳定的接口契约测试，专门看：

- 分页字段是否一致
- 认证失败返回是否一致
- 创建/编辑/删除的关键字段是否稳定
- 前端依赖的字段有没有被改名

**建议放置**

- 后端模块级接口测试
- `docs/api-test-coverage-matrix.md` 同步补 coverage

**收益**

- 后续重构 Controller/Service 时更有底
- 能更早发现“前端没坏，但契约悄悄漂了”

---

## P2

### 7. 清理仓库运行产物和调试残留

- 优先级：`P2`
- 工作量：`S`
- 风险：`低`

**现状**

仓库根目录下有较多运行残留，例如：

- `*.log`
- `*.pid`
- `hs_err_pid*.log`
- `replay_pid*.log`
- 临时 png / txt / json

**建议**

- 进一步补 `.gitignore`
- 增加 `scripts/clean-runtime-artifacts.ps1`
- 把交付截图、运行日志收拢到单独目录

**收益**

- 仓库更干净
- 交作业/答辩时更专业

---

### 8. 减少重复的前端本地状态逻辑

- 优先级：`P2`
- 工作量：`M`
- 风险：`低`

**现状**

很多页面都在重复写：

- `sidebarCollapsed` 的 `localStorage` 读写
- `showMessage` / `showNotification`
- 设置页本地偏好保存

从扫描结果看，这类逻辑已经散在多个学生页里。

**建议**

- 抽一个 `ui-state.js`
- 抽一个 `toast.js`
- 抽一个 `preferences.js`

**收益**

- 页面会明显变短
- 统一交互风格更容易

---

### 9. 优化静态资源体积和加载组织

- 优先级：`P2`
- 工作量：`S`
- 风险：`低`

**现状**

目前最大的静态资源主要是：

- `echarts.min.js`
- `xlsx.full.min.js`
- `chart.umd.min.js`

资源已经本地化，这是对的；下一步可以做的是更细一点的按页加载和目录整理，而不是先追求复杂构建。

**建议**

- 只在真正需要图表/导出的页面引入对应库
- 检查是否存在重复字体目录和重复静态资源
- 为本地库加一个简单的 vendor 说明文档

---

### 10. 继续收文案和演示路径

- 优先级：`P2`
- 工作量：`S`
- 风险：`低`

**现状**

这轮已经把大部分“开发中 / 未实现”感收掉了，但如果要冲“更像真实产品”，还可以继续做两件小事：

- 把演示环境说明文案继续统一口径
- 做一页正式的答辩演示顺序和避坑按钮清单

**收益**

- 演示体验更顺
- 交付表达更完整

---

## 建议执行顺序

如果按投入产出比来排，我建议下一轮这样做：

1. `P0-2` 统一后端接口返回契约
2. `P0-3` 建立前端产物同步流程
3. `P0-1` 拆分超大前端页面和公共脚本
4. `P1-4` 拆分超大的后端 Controller / Service
5. `P1-5` 建立统一 capability/config 机制
6. `P1-6` 补契约测试

---

## 如果只做三件事

如果时间有限，只做下面三件最值：

1. 统一后端接口返回契约
2. 建立 `frontend/dist -> static` 自动同步
3. 拆 `api.js + teacher-courses + teacher-assignments`

---

## 最新进展

- 已新增 `scripts/sync-frontend-to-static.ps1`，并持续扩充白名单，降低 `frontend/dist` 与 Spring Boot `static` 双目录漂移风险。
- `teacher-courses` 已先后拆出课程分配、学生弹窗、班级编辑、课程 CRUD 等独立脚本模块。
- `teacher-assignments` 已先后拆出作业 CRUD、考试 CRUD、考试发布/知识点加载、提交记录筛选缓存模块。
- 本次继续把评分/批改逻辑从 `api.js` 抽离到 `frontend/dist/teacher-assignments-grading.js`，包括作业批改、考试评分、提交详情查看与查看考试后跳转评分列表的交互绑定。
- 本次继续把 `teacher-assignments` 的页面壳层从页面内联脚本抽离到 `frontend/dist/teacher-assignments-shell.js`，承接初始化启动、tab 切换、入口参数处理、学生下拉与课程筛选下拉、以及作业/考试搜索按钮绑定。
- 本次继续清理 `api.js` 中 teacher-assignments 已替代的重复搜索/reset 旧兼容实现，收敛为单份最小兼容层，并改为优先复用 `teacher-assignments-shell.js` 提供的参数构造逻辑。
- 对应契约脚本 `verify-teacher-grade-refresh-contract.js` 与 `verify-teacher-exam-view-contract.js` 已改为校验新的页面模块文件，避免验证口径继续绑定旧的 `api.js` 大文件结构。
- 本次补齐 `teacher-dashboard.html` 的本地作业入口，新增仪表盘级 `deleteAssignment` 并复用作业工作区跳转，避免教师仪表盘继续依赖历史全局实现；同步脚本与教师端 JWT/CRUD smoke 已验证通过。
- 本次继续补齐 `teacher-assignments-shell.js` 对 `assignmentId` 入口参数的消费链路，确保教师仪表盘跳转到作业工作区时能够自动打开查看/编辑动作，并将该行为纳入 `verify-teacher-exam-view-contract.js` 的页面入口契约校验。
- 本次继续把 `teacher-assignments` 的列表加载/渲染层从 `api.js` 抽离到 `frontend/dist/teacher-assignments-lists.js`，承接作业/考试/提交记录列表与分页渲染；页面脚本、同步脚本白名单和新增 `verify-teacher-assignments-lists-contract.js` 均已补齐，并通过教师端 JWT/CRUD smoke 验证。
- 本次继续收缩 `api.js` 中 teacher-assignments 的旧 search/reset/initTabs 壳：重置逻辑和兼容搜索入口已迁入 `teacher-assignments-shell.js`，重复的全局 `initTabs` 已移除，并通过列表层契约、页面入口契约与教师端 JWT/CRUD smoke 验证。
- 本次继续收敛 teacher-assignments 的模块边界：提交记录搜索按钮正式由 `teacher-assignments-shell.js` 接管，`teacher-assignments-submission-filters.js` 仅保留下拉缓存职责，并移除 `api.js` 中已无消费者的 `buildTeacherAssignmentsLegacySearchParams` 与 `search*` 兼容函数。
- 本次继续补齐 teacher-assignments 的页面级统计刷新链：`teacher-assignments-shell.js` 新增 `refreshTeacherAssignmentsSummary()`，并在作业/考试发布、编辑、删除后触发刷新；同时移除 `api.js` 中无页面依赖的旧 `loadStats / updateStatCard / window DOMContentLoaded` 初始化链和重复 `exportSubmissions` 定义。
- 本次继续清理 `api.js` 中剩余的 teacher 死层：删除已无消费者的 `initAssignmentSearch / initExamSearch / initSubmissionSearch` 空函数，移除指向不存在页面的 `teacher-exams.html / teacher-submissions.html` 分支与不再使用的 `loadDashboardStats()` 旧入口，验证教师端 smoke 继续通过。
- 本次继续把 teacher-assignments 的作业发布流从 `api.js` 抽离到 `frontend/dist/teacher-assignments-assignment-publish.js`，页面脚本与同步白名单已补齐，并新增 `verify-teacher-assignment-publish-contract.js` 校验新模块接管发布作业与列表/统计刷新链。
- 本次开始把 `teacher-courses` 当前真实生效的课程搜索/列表/分页主链收口到 `frontend/dist/teacher-courses-courses.js`，页面脚本与同步白名单已补齐，并新增 `verify-teacher-courses-courses-contract.js` 作为课程页模块接入护栏。
- 本次继续收口 `teacher-courses` 课程页旧宿主代码：已从页面内联脚本中移除被 `teacher-courses-courses.js` 接管的课程搜索/列表/分页旧实现，并将契约脚本升级为同时校验“新模块导出存在 + 页面旧函数已移除”。 
- 本次继续清理 `teacher-courses` 课程分页遗留链：`initPagination()` 已改为只处理班级分页，课程页旧 `handleCoursePaginationClick / updateCoursePaginationUI / coursePagination click` 监听已移除，避免与新课程模块的分页入口重复接管。
- 本次继续去重 `teacher-courses` 页面遗留状态：移除重复的 `DOMContentLoaded -> initPagination()` 监听，以及已无消费者的课程页全局状态 `currentTab / currentPage / totalPages / currentSearchParams / searchHistory.courses`，仅保留班级搜索历史所需字段。
- 本次继续把 `teacher-courses` 班级搜索/列表/分页主链收口到 `frontend/dist/teacher-courses-classes.js`，页面内联班级主链旧实现与底部重复 `handleClassSearch / handleClassClear` 已移除；同步补充 `verify-teacher-courses-classes-contract.js`，并通过教师端 JWT/CRUD smoke 验证。 
- 本次继续把 `teacher-courses` 班级新增/删除 CRUD 从页面内联脚本抽离到 `frontend/dist/teacher-courses-class-crud.js`，同时移除页面中已无消费者的 `showFieldError / clearFieldValidation / clearValidation / resetFormValidation` 表单辅助函数；同步补充 `verify-teacher-courses-class-crud-contract.js`，并通过教师端 JWT/CRUD smoke 验证。 
- 本次继续把 `teacher-courses` 的课程/班级表单验证与唯一性校验壳整体抽离到 `frontend/dist/teacher-courses-validation.js`，页面内联 `checkCourseCodeExists / checkClassNameExists / validateField / validateCourseForm / validateClassForm / validateForm / initFormValidation` 已移除；同步补充 `verify-teacher-courses-validation-contract.js`，并通过教师端 JWT/CRUD smoke 验证。 
- 本次继续把 `teacher-courses` 的课程选项、年级/专业元数据与 modal 预加载壳整体抽离到 `frontend/dist/teacher-courses-metadata.js`，页面内联 `loadCourseOptions / loadGradeAndMajorData / ensure*OptionsLoaded / shown.bs.modal` 绑定已移除；同步补充 `verify-teacher-courses-metadata-contract.js`，并通过教师端 JWT/CRUD smoke 验证。 
- 本次继续把 `teacher-courses` 的页面启动/user/tabs/课程类别与状态下拉壳整体抽离到 `frontend/dist/teacher-courses-shell.js`，页面已改为通过 `bootstrapTeacherCoursesPage()` 启动；同步补充 `verify-teacher-courses-shell-contract.js`，并通过教师端 JWT/CRUD smoke 验证。 
- 本次继续收口 `teacher-courses` 页面尾部的通用 helper：`showNotification / reportResourceLoadFailure / handleApiError / showLoading / hideLoading / searchHistory / 班级管理侧边栏快捷切换` 已并入 `frontend/dist/teacher-courses-shell.js`，页面尾部内联 helper 壳已移除，并通过教师端 JWT/CRUD smoke 验证。 
- 本次继续把 `teacher-courses` 页内宿主壳并入 `frontend/dist/teacher-courses-shell.js`：`loadAssessmentMethods / loadUserDropdownForTeacher / 浏览器错误展示与全局 error/unhandledrejection 捕获 / toggleBtn 折叠绑定` 已从页面内联脚本移除，并通过教师端 JWT/CRUD smoke 验证。 
- 本次开始推进后端 Controller 收口：为 `major_assignment/src/main/java/com/_202510007517/major_assignment/controller/TeacherDashboardController.java` 增加类级 `@RequireLogin(roles = {RoleConstants.TEACHER})`，并把 dashboard/learning-summary/score-trend 三处重复的 `"all" -> null` 参数归一化抽成私有 helper；对应补充 `TeacherDashboardControllerTest` 类级鉴权契约并通过 Maven 定向测试验证。 
- 本次继续收口 `TeacherDashboardController`：在类级教师鉴权已经生效的前提下，移除 15 处手写 `isLoggedIn -> 401` 重复壳，改由 `AuthenticationAspect` 统一处理；对应 `TeacherDashboardControllerTest` 与鉴权相关 Maven 定向回归继续通过。 
- 本次将统一教师鉴权模式推进到 `major_assignment/src/main/java/com/_202510007517/major_assignment/controller/AssignmentController.java`：新增类级 `@RequireLogin(roles = {RoleConstants.TEACHER})`，移除 11 处手写 `isLoggedIn -> 401` 分支，并补充 `AssignmentControllerTest` 契约后通过 Maven 定向回归验证。 
- 本次将同样的统一教师鉴权模式推进到 `major_assignment/src/main/java/com/_202510007517/major_assignment/controller/ExamController.java`：新增类级 `@RequireLogin(roles = {RoleConstants.TEACHER})`，移除 11 处手写 `isLoggedIn -> 401` 分支，并补充 `ExamControllerTest` 契约后通过 Maven 定向回归验证。 
- 本次开始收第二层重复业务壳：在 `major_assignment/src/main/java/com/_202510007517/major_assignment/controller/BaseController.java` 新增 `resolveTeacherCourseId()`，并让 `AssignmentController / ExamController` 复用课程 ID / 课程代码解析逻辑；对应补充 `BaseControllerCourseResolutionTest` 并通过相关 Controller 的 Maven 定向回归验证。 
- 本次继续收第二层重复业务壳：在 `major_assignment/src/main/java/com/_202510007517/major_assignment/controller/BaseController.java` 新增 `resolveTeacherCourseIdFromPayload()`，统一兼容 `courseId / course_id` 与“数字课程 ID / 字符串课程代码”两类请求体输入；`AssignmentController / ExamController` 的创建与更新接口已切换复用该 helper，并通过 `BaseControllerCourseResolutionTest`、`AssignmentControllerTest`、`ExamControllerTest` 及相关鉴权 Maven 定向回归验证。 
- 本次继续收第二层重复业务壳：在 `major_assignment/src/main/java/com/_202510007517/major_assignment/controller/BaseController.java` 新增 `calculateTotalPages()` 与 `boundPageNum()`，统一分页条数夹紧与“页码超出总页数时回落到最后一页”的兜底规则；`AssignmentController / ExamController` 的列表与提交记录接口已切换复用该分页规则，并通过 `BaseControllerPaginationTest`、相关 Controller 契约测试与鉴权 Maven 定向回归验证。 
- 本次继续收第二层重复业务壳：在 `major_assignment/src/main/java/com/_202510007517/major_assignment/controller/BaseController.java` 新增 `buildSpringPageResponse()`，统一 Spring 风格分页 `Map` 的 `content/pageable/totalPages/first/last` 等响应拼装；`AssignmentController` 的作业列表与作业提交记录接口、`ExamController` 的考试列表接口已切换复用，并通过 `BaseControllerPaginationTest`、相关 Controller 契约测试与鉴权 Maven 定向回归验证。 
- 本次继续收第二层重复业务壳：在 `major_assignment/src/main/java/com/_202510007517/major_assignment/controller/BaseController.java` 新增作业/考试状态 token 归一化 helper，统一处理中英文别名、大小写与空白输入；`AssignmentController / ExamController` 的状态筛选入口已切换复用，并通过 `BaseControllerStatusNormalizationTest`、相关 Controller 契约测试与鉴权 Maven 定向回归验证。 
- 本次继续收 `TeacherDashboardController` 的分页实现：`/api/teacher/course-assignments` 已切换复用共享分页 helper，修复“请求页码越界后 offset 仍按旧页码裁切，导致最后一页空白”的 bug；`/api/teacher/submissions` 已统一返回 Spring 风格分页结构，并通过扩展后的 `TeacherDashboardControllerTest` 与 36 项 Maven 定向回归验证。 
- 本次继续收 `ExamController` 的教师考试提交记录列表分页实现：`/api/teacher/exams/submissions` 已从旧 `submissions/total/page/pages` 结构切换到共享的 Spring 风格分页响应，并通过扩展后的 `ExamControllerTest` 与 37 项 Maven 定向回归验证。 
- 本次继续收 `CourseController` 的教师课程列表契约：`/api/teacher/courses` 已从“返回全量列表 + 伪造分页字段”改为真正按请求页码和页大小裁切，并统一复用 `BaseController` 的分页夹紧规则；对应补充 `CourseControllerTest` 覆盖越界页码回最后一页。前端课程页 `teacher-courses-courses.js` 已切换为直接消费后端分页结果，不再本地二次切片；需要全量课程下拉/校验的教师端入口已显式改为请求 `page=1&size=100`，并通过教师端 JWT smoke 与 CRUD smoke 回归验证。 
- 本次继续收 `CourseController` 的教师鉴权入口：已从“每个方法分别挂 `@RequireLogin(TEACHER)`”切换为类级教师鉴权注解，课程列表/创建/更新/删除/详情/学生列表统一依赖 `AuthenticationAspect`；`CourseControllerTest` 已同步改为校验类级 guard 存在、方法级不重复挂注解，并通过 40 项 Maven 定向回归验证。 
- 本次继续收预警列表分页边界：`EarlyWarningServiceImpl#getWarningList` 已统一复用共享分页夹紧规则，修复“请求页码超出总页数时仍按旧页码计算 offset，导致末页可能空白”的问题；新增 `EarlyWarningServiceImplTest` 覆盖越界页码回最后一页与空结果回第一页场景，并通过 42 项 Maven 定向回归、教师端 JWT smoke 与 CRUD smoke 验证。 
- 本次继续整理共享分页层：升级 `major_assignment/src/main/java/com/_202510007517/major_assignment/utils/PageUtils.java`，补齐 `clampPageSize / clampPageNum / boundPageNum / buildPageResponse` 等共享能力；`BaseController` 的分页 helper 已改为转调 `PageUtils`，`EarlyWarningServiceImpl` 也不再继承 controller 基类获取分页逻辑，改为直接依赖中立工具类，避免 service -> controller 的层次倒挂。对应 `BaseControllerPaginationTest`、`EarlyWarningServiceImplTest`、`ExamControllerTest`、`TeacherDashboardControllerTest` 与教师端 smoke 已全部通过。
- 本次继续统一 `PageResult` 构建：在 `PageUtils` 新增 `buildPageResult(...)`，并让 `CourseController` 与 `EarlyWarningServiceImpl` 改为复用共享 helper 生成分页 DTO，去掉各自手搓 `PageResult` 字段的重复代码；相关分页测试、课程控制器测试与教师端 smoke 已全部通过。
- 本次继续收口浏览器错误分页契约：`BrowserErrorServiceImpl#getBrowserErrorList` 已改为复用 `PageUtils` 的页大小夹紧、越界页码回落与 `PageResult` 构建逻辑，`BrowserErrorController` 也已直接返回 `ResponseResult<PageResult<BrowserError>>`，替代原先手搓 `content/totalElements/pageNumber/pageSize/totalPages` 的 `Map`；对应补充 `BrowserErrorControllerTest` 并通过 43 项 Maven 定向回归与教师端 JWT/CRUD smoke 验证。
- 本次继续修补学生通知分页边界：`NotificationServiceImpl#getNotificationsWithPagination` 现已统一复用 `PageUtils` 的页大小夹紧、越界页码回落和内存分页逻辑，修复“页码越界时 `subList` 直接抛异常、空结果仍回传假页码”的问题，同时保持前端依赖的 `notifications/total/page/size/totalPages` 返回结构不变；新增 `NotificationServiceImplTest` 并通过 45 项 Maven 定向回归、学生端 CRUD smoke 与教师端 CRUD smoke 验证。
- 本次继续收学生课程分页的重复壳：`StudentServiceImpl#getStudentCoursesWithPagination` 已从手搓分页 `Map` 切换为直接复用 `PageUtils.buildPageResponse(...)`，统一页大小夹紧、越界页码回落、空结果 `totalPages=0` 与分页元数据结构，同时保持学生课程页当前依赖的 Spring 风格分页 shape 不变；新增 `StudentServiceImplTest` 并通过 50 项 Maven 定向回归、学生端 CRUD smoke 与教师端 CRUD smoke 验证。
- 本次继续收学生/教师提交流服务的分页底座：`AssignmentSubmissionServiceImpl#getSubmissionsWithPagination` 与 `ExamSubmissionServiceImpl#getSubmissionsWithPagination` 已统一改为复用 `PageUtils` 的页大小夹紧、越界页码回落与 offset 计算逻辑，替代原先裸 `(page - 1) * size` 的实现；新增 `AssignmentSubmissionServiceImplTest` 与 `ExamSubmissionPaginationTest`，并通过 53 项 Maven 定向回归、学生端 CRUD smoke 与教师端 CRUD smoke 验证。
- 本次继续收 teacher 侧内存列表分页壳：在 `BaseController` 新增 `buildSpringPageResponseFromInMemoryList(...)`，统一“内存列表再分页”的页码夹紧、越界回落与分页响应拼装；`AssignmentController` 的作业列表、`ExamController` 的考试列表、`TeacherDashboardController` 的课程分配列表已切换复用该 helper，替代原先手写 `subList/skip + buildSpringPageResponse` 逻辑。新增 `BaseControllerInMemoryPageTest`，并通过 55 项 Maven 定向回归、学生端 CRUD smoke 与教师端 CRUD smoke 验证。
- 本次继续收 teacher 侧内存列表分页壳的第二层落点：`BaseController.buildSpringPageResponseFromInMemoryList(...)` 已开始承接控制器内存列表分页的统一出口，`AssignmentController#getAssignments`、`ExamController#getExams` 与 `TeacherDashboardController#getClassAssignments` 现已移除各自手写的 `subList/skip/limit` 分页实现，统一复用共享 helper 完成页码夹紧、越界回落与 Spring 风格分页响应拼装；对应新增 `BaseControllerInMemoryPageTest` 并通过 55 项 Maven 定向回归、学生端 CRUD smoke 与教师端 CRUD smoke 验证。
- 本次继续补齐 teacher 侧内存列表分页 DTO 出口：`BaseController` 已新增 `buildPageResultFromInMemoryList(...)`，用于统一“内存列表 -> PageResult`”这类控制器分页壳；`CourseController#getCourses` 已移除手写 `subList + PageUtils.buildPageResult(...)` 逻辑，改为直接复用共享 helper。对应扩展 `BaseControllerInMemoryPageTest` 与 `CourseControllerTest`，并通过 57 项 Maven 定向回归验证。
- 本次继续收分页窗口重复壳：`PageUtils` 已新增 `PageWindow` 与 `resolvePageWindow(...)`，统一输出 `safePage / safeSize / totalPages / offset` 这一组高频分页元数据；`StudentServiceImpl#getStudentCoursesWithPagination`、`AssignmentSubmissionServiceImpl#getSubmissionsWithPagination`、`ExamSubmissionServiceImpl#getSubmissionsWithPagination` 已切换复用共享分页窗口，替代各自重复的 `clamp + bound + offset` 计算逻辑。新增 `PageUtilsTest` 并通过 59 项 Maven 定向回归验证。
- 本次继续把分页窗口推进到 controller/service 壳层：`BaseController` 已新增 `resolvePageWindow(...)` 代理，`AssignmentController#getAllSubmissions`、`ExamController#getAllSubmissions`、`TeacherDashboardController#getAllSubmissions`、`NotificationServiceImpl#getNotificationsWithPagination` 与 `BrowserErrorServiceImpl#getBrowserErrorList` 已切换复用共享分页窗口，进一步移除本地 `safePage/safeSize/offset/totalPages` 重复计算。扩展 `BaseControllerPaginationTest` 后，60 项 Maven 定向回归继续通过。
- 本次继续把分页窗口推进到学生列表与预警链路：`AssignmentServiceImpl#getAssignmentsWithPagination`、`ExamServiceImpl#getExamsWithPagination` 与 `EarlyWarningServiceImpl#getWarningList` 已切换复用 `PageUtils.resolvePageWindow(...)`，替代各自的分页窗口计算，同时保持现有分页返回 shape 不变。新增 `AssignmentServiceImplTest` 与 `ExamServiceImplTest`，并通过 62 项 Maven 定向回归验证。
- 本次继续收 teacher 提交记录链路的重复查询：`AssignmentSubmissionService#getSubmissionsWithPagination` 与 `ExamSubmissionService#getSubmissionsWithPagination` 现已改为接收 controller 已算出的 `total`，不再在 service 内重复执行一次 `countSubmissions(...)` 仅用于分页窗口计算；`AssignmentController`、`ExamController` 与 `TeacherDashboardController` 调用面已同步切换。更新 `AssignmentSubmissionServiceImplTest`、`ExamSubmissionPaginationTest` 及相关 controller 测试后，62 项 Maven 定向回归继续通过。
- 本次继续收学生作业/考试列表的同页重复查找：`AssignmentServiceImpl#getAssignmentsWithPagination` 与 `ExamServiceImpl#getExamsWithPagination` 已新增单页局部课程/教师缓存，避免同一页中对相同 `courseId` 和 `teacherId` 重复执行 `courseService.findById(...)` / `userService.findById(...)`；新增对应 N+1 防回退测试后，64 项 Maven 定向回归继续通过。
- 本次继续收教师学情汇总中的同学生重复班级名查询：`TeacherDashboardServiceImpl#getStudentLearningSummary` 已将 `userMapper.getStudentClassName(studentId)` 从课程内层循环移到学生粒度，避免同一学生关联多门课时重复查询同一班级名；新增 `TeacherDashboardServiceImplTest` 后，65 项 Maven 定向回归继续通过。
- 本次继续收教师仪表盘多课程场景的逐课程学生人数查询：`TeacherDashboardServiceImpl#getDashboardData` 在“无班级筛选 + 多门课程”路径下已切换为复用 `CourseMapper.batchGetStudentCountByCourseIds(...)`，避免逐课程重复执行 `getStudentCountByCourseId(...)`；扩展 `TeacherDashboardServiceImplTest` 后，66 项 Maven 定向回归继续通过。
- 本次修复教师仪表盘平均分被重复无筛选计算覆盖的问题：`TeacherDashboardServiceImpl#getDashboardData` 已移除后段重复的课程平均分重算逻辑，避免带 `classId` 时前面已得到的 `getCourseAverageScoreByClassId(...)` 结果被无筛选 `getCourseAverageScore(...)` 覆盖；扩展 `TeacherDashboardServiceImplTest` 后，67 项 Maven 定向回归继续通过。
- 本次继续收教师仪表盘多课程场景的逐课程平均分查询：已在 `CourseMapper` / `CourseMapper.xml` 新增 `batchGetCourseAverageScoresByCourseIds(...)`，并让 `TeacherDashboardServiceImpl#getDashboardData` 在“无班级筛选 + 多门课程”路径下复用批量平均分结果，避免逐课程重复执行 `getCourseAverageScore(...)`；扩展 `TeacherDashboardServiceImplTest` 后，67 项 Maven 定向回归继续通过。
- 本次继续收教师仪表盘最近预警活动的逐条学生姓名补全：`EarlyWarningMapper.findRecentByTeacherId(...)` 已直接联表返回 `studentName`，`TeacherDashboardServiceImpl#getDashboardData` 不再为每条预警单独执行 `userMapper.findById(...)`；同时修复了 `EarlyWarning.triggerDate` 为 `LocalDateTime` 时 recent activities 会因 `SimpleDateFormat` 直接格式化失败而退回“系统提示”的 bug。扩展 `TeacherDashboardServiceImplTest` 后，68 项 Maven 定向回归继续通过。

---

## 结论

当前版本已经达到“可交付且主流程稳定”的状态。  
下一轮优化最应该追求的，不再是修零散 bug，而是：

- 降低前端大文件复杂度
- 降低后端接口契约漂移
- 降低双目录和多环境带来的维护成本

这三件事做好，项目会从“交付完成”明显进入“更像长期维护项目”的阶段。

已落地补充：
- `TeacherDashboardServiceImpl#getDashboardData` 已移除尾段对 `pendingAssignmentsChange / pendingExamsChange / missingSubmissionsChange / upcomingDeadlinesChange / warningCountChange` 的“较上周”二次覆盖，确保前端文案“较昨日”和实际返回值一致；对应新增 `TeacherDashboardServiceImplTest`，当前定向 Maven 回归已到 `69` 项全绿。
- `TeacherDashboardServiceImpl#getStudentLearningSummary` 已将 summary 级统计改为按学生聚合，不再因同一学生挂多门课而重复累加 `averageScore / totalPendingAssignments / overallProgress`；同时保留 `studentPerformances` 按“学生 × 课程”展开的表格行语义。对应新增 `TeacherDashboardServiceImplTest`，当前定向 Maven 回归已到 `70` 项全绿。
- `teacher-student-dashboard.html` 已将列表分页用的课程行数与对外展示的真实学生数拆开处理：表格仍按 `studentPerformances` 课程行分页，但标题和分数段统计已改为基于去重 `studentId` 展示“x名学生 / y条课程记录”；新增 `verify-teacher-student-dashboard-contract.js`，当前定向验证已通过。
- `teacher-student-dashboard.html` 已继续补齐多课程行级语义：导出 Excel 新增课程列，查看/编辑按钮会把 `courseName/className` 一起带入详情与编辑定位，避免同一学生多门课时落到错误行或导出成看似重复的无课程记录；`verify-teacher-student-dashboard-contract.js` 已同步加严并通过。
- 本次继续收教师学情页详情弹窗的课程行语义：`teacher-student-dashboard.html` 已将详情弹窗改为“课程行字段优先、学生级详情接口补充通用资料”的合并顺序，并新增“当前课程”展示字段，避免同一学生多门课时详情弹窗被学生级接口数据覆盖回错误课程语境；`verify-teacher-student-dashboard-contract.js` 已同步加严，并通过教师端 JWT smoke 与 CRUD smoke 验证。
- 本次继续修正教师学情页详情弹窗的趋势上下文：`TeacherDashboardController#getScoreTrend` / `TeacherDashboardServiceImpl#getScoreTrend` / `AssignmentSubmissionMapper#getAssignmentScoreTrend` 已补齐可选 `studentId` 过滤，`teacher-student-dashboard.html` 也会把当前行的 `courseId` 一并传入，并正确解析 `ScoreTrendDTO[]` 作为折线图数据，避免详情弹窗把全班/全课程趋势误当成当前学生当前课程的趋势。对应扩展 `TeacherDashboardControllerTest`、`TeacherDashboardServiceImplTest` 与 `verify-teacher-student-dashboard-contract.js`，并通过教师端 JWT smoke 与 CRUD smoke 验证。
- 本次继续收教师学情页多课程行定位语义：`teacher-student-dashboard.html` 已将 `findStudentRow(...)`、详情弹窗与编辑弹窗的行匹配逻辑升级为 `courseId` 优先，再回退到 `courseName/className`，避免同名课程或同班重复时误命中错误课程行；`verify-teacher-student-dashboard-contract.js`、教师端 JWT smoke 与 CRUD smoke 已通过。
- 本次继续收教师学情页编辑链的多课程上下文：`teacher-student-dashboard.html` 已将编辑弹窗的 `courseId/courseName/className` 显式写入 `editStudentForm.dataset`，`saveStudentEdit()` 的本地回写与确认重试统一从表单上下文读取，避免依赖外层作用域中的隐式变量导致多课程场景下保存链串线或潜在运行时错误；契约脚本、教师端 JWT smoke 与 CRUD smoke 已通过。
- 本次继续修正教师学情页编辑链中的 `courseId` shadowing bug：`editStudent(...)` 内原本把行级 `courseId` 形参与当前筛选下拉的 `courseId` 局部变量混用，导致多课程场景下可能按筛选课程而不是当前行课程定位学生数据。现已统一改为使用 `selectedCourseId` 表示筛选值，并保留行级 `courseId` 贯穿详情/编辑上下文、表单 dataset 与本地回写链路；契约脚本、教师端 JWT smoke 与 CRUD smoke 已通过。
- 本次继续提升教师学情页编辑后的本地一致性：`teacher-student-dashboard.html` 已将 `saveStudentEdit()` 的即时本地回写从“只更新命中的单条课程行”扩展为按 `studentId` 批量同步该学生当前表格中的所有课程行，让姓名/班级这类学生级字段在多课程场景下先于全量 reload 保持一致；契约脚本、教师端 JWT smoke 与 CRUD smoke 已通过。
- 本次继续修正教师学情页分数段下钻的时间范围口径：`showStudentsByScoreRange(...)` 已改为沿用当前 `timeRangeSelect` 并把 `timeRange` 一并带入 `/api/teacher/learning-summary`，避免“成绩分布图按本周/本月筛选、点击后学生列表却退回默认时间范围”的图表与表格口径错位；`verify-teacher-student-dashboard-contract.js` 已补充护栏并通过。
- 本次继续修正教师学情页成绩分布图的展示语义：当前成绩分布仍基于 `studentPerformances` 的课程记录分桶，因此图卡标题、图例、tooltip 与柱状图 y 轴已统一改为“课程记录数/条记录”，避免把课程行计数误表述为学生人数；`verify-teacher-student-dashboard-contract.js`、教师端 JWT smoke 与 CRUD smoke 已通过。
- 本次继续修正教师学情页趋势图重绘链的 `timeRange` 传递 bug：查询与重置筛选后，`teacher-student-dashboard.html` 已改为使用当前 `timeRangeSelect` 重绘 `initScoreTrendChart(timeRange)`，不再误把 `scoreTrendChart.config.type` 这类图表类型值当成时间范围传入，避免趋势图在查询后悄悄退回默认月度口径；契约脚本、教师端 JWT smoke 与 CRUD smoke 已通过。
- 本次继续收教师学情页趋势图的时间范围状态一致性：趋势图卡片上的“周/月/学期”按钮现在会同步回写顶部 `timeRangeSelect`，而 `initCharts()` 也改为优先读取共享下拉值，不再从旧 active 按钮反推时间范围，避免按钮高亮、下拉值和实际趋势数据三者发生错位；契约脚本、教师端 JWT smoke 与 CRUD smoke 已通过。
- 本次继续补齐教师学情页趋势图控件联动：新增 `syncScoreTrendTimeRangeControls(...)`，统一同步顶部 `timeRangeSelect` 与趋势图按钮 active 状态；现在趋势按钮点击、下拉变更、查询、重置、刷新以及趋势图初始化都会走同一套时间范围状态源，避免 UI 看起来停在“周”但实际数据已经切到“月/学期”的错位；契约脚本、教师端 JWT smoke 与 CRUD smoke 已通过。
- 本次继续收教师学情页图卡交互与趋势语义：图卡切换逻辑已从标题文本判断改为基于稳定 `data-chart-kind` 分流，避免未来改标题时再牵连行为；同时趋势图标题已明确为“当前筛选平均成绩趋势”，dataset label 与 tooltip 也同步改成“当前筛选平均成绩”，让标题、坐标轴和悬浮提示统一指向班级/课程筛选后的聚合趋势语义；契约脚本、教师端 JWT smoke 与 CRUD smoke 已通过。
- 本次继续修正教师学情汇总的时间范围假筛选问题：`TeacherDashboardServiceImpl#getStudentLearningSummary` 之前仍在复用只按 `studentId` 聚合的 `studentMapper.getStudentPerformance(...)`，导致 `/api/teacher/learning-summary` 虽然接收了 `courseId/timeRange`，但课程行指标实际上没有真正吃进这些筛选。现已新增 `StudentMapper#getTeacherStudentCoursePerformance(...)` 与对应 SQL，按“学生 × 课程 × 时间范围”返回 `averageScore / pendingAssignments / overallProgress`，并由 service 按课程行构建 `studentPerformances`、按学生聚合 summary 级统计；扩展 `TeacherDashboardServiceImplTest` 后，73 项 Maven 定向回归、`verify-teacher-student-dashboard-contract.js`、教师端 JWT smoke 与 CRUD smoke 已通过。
- 本次继续补齐教师学情页 `quarter / 最近三个月` 趋势链：`teacher-student-dashboard.html` 已在顶部筛选、趋势按钮、`syncScoreTrendTimeRangeControls(...)`、按钮文案映射、默认占位数据与日期标签格式上统一支持 `quarter`，后端 `TeacherDashboardServiceImpl#getScoreTrend(...)` 也已按最近 90 天窗口真实计算，不再把 `quarter` 悄悄退回 `month`。扩展 `TeacherDashboardServiceImplTest`、`TeacherDashboardControllerTest` 与 `verify-teacher-student-dashboard-contract.js` 后，教师端 JWT smoke 与 CRUD smoke 继续通过。
- 本次继续修正教师学情汇总 `semester / 本学期` 的底层时间范围语义：`StudentMapper.xml` 中 `getTeacherStudentCoursePerformance(...)` 之前仅对 `week/month/quarter` 做了真实时间过滤，`semester` 分支实际上没有按时间裁切，导致“本学期”更像全量历史。现已补齐 `assignment_submissions` / `exam_submissions` 侧的 `semester -> 最近 4 个月` 过滤，并新增 `StudentMapperXmlContractTest` 防回退；相关 `TeacherDashboardServiceImplTest`、`TeacherDashboardControllerTest`、前端契约脚本、教师端 JWT smoke 与 CRUD smoke 已通过。
- 本次继续修正教师学情页仪表盘卡片“学习进度”假值问题：`teacher-student-dashboard.html` 之前直接拿 `averageScore` 冒充 `learningProgress`，后端 `TeacherDashboardDTO` 也没有承载真实整体进度字段。现已为 `TeacherDashboardDTO` 新增 `overallProgress`，并让 `TeacherDashboardServiceImpl#getDashboardData(...)` 复用 `getStudentLearningSummary(...)` 的真实聚合进度回填该字段，前端和静态副本也已改为消费 `dashboardData.overallProgress`，不再用平均分顶替。扩展 `TeacherDashboardServiceImplTest`、前端契约脚本后，相关教师端 JWT smoke 与 CRUD smoke 继续通过。
- 本次继续收教师学情页仪表盘卡片变化值文案的真实性：`teacher-student-dashboard.html` 原先“平均成绩 / 作业完成率 / 学习进度”卡片下方仍保留硬编码箭头与假变化数字，学生数卡片也只是静态占位。现已将学生数变化值改为吃后端真实 `totalStudentsChange` 并通过 `formatStatChange(...)` 动态渲染，另外三张暂无稳定后端变化源的卡片统一改为中性说明文案（如“基于当前筛选结果”），避免页面继续展示伪造增减值；`verify-teacher-student-dashboard-contract.js` 已加严并通过。后续复核确认：此前偶发的 `sync-frontend-to-static.ps1 -CheckOnly` 红灯主要来自把 `sync` 与 `checkOnly` 并行执行造成的竞态，顺序执行后可稳定恢复绿色。
- 本次继续收 teacher 端总仪表盘的伪变化语义：审计发现 `TeacherDashboardServiceImpl#getDashboardData(...)` 中 `totalCoursesChange / totalStudentsChange / pendingAssignmentsChange / pendingExamsChange / warningCountChange` 仍主要来自模拟值或启发式回填，但 [frontend/dist/teacher-dashboard.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-dashboard.html) 之前仍把它们渲染成“较上周 / 较昨日”。现已将这五张卡片的变化行统一降级为中性说明文案（如“当前筛选课程统计”“基于当前待批改队列”），不再把模拟字段包装成真实时间对比；新增 `verify-teacher-dashboard-contract.js` 护栏。过程中教师 JWT smoke 真实抓到一处回归：`updateStatistics()` 在 `forEach` 回调内误用了 `continue`，导致 `teacher-dashboard` 页面报 `Illegal continue statement` 并阻断 `/api/teacher/courses` 检查；现已修正为 `return`，重新验证后教师 JWT smoke 与教师 CRUD smoke 全部恢复为绿色。
- 本次继续收教师学情页学生数卡片的剩余伪时间对比：进一步审计后确认 `teacher-student-dashboard.html` 之前消费的 `dashboardData.totalStudentsChange` 目前同样来自 `TeacherDashboardServiceImpl#getDashboardData(...)` 的模拟/启发式字段，因此学生数卡片虽然比前一轮更“动态”，语义上仍是假 delta。现已将该卡片变化行从“较昨日持平/动态人数变化”统一降级为中性说明“当前筛选学生统计”，并移除前端对 `totalStudentsChange` 的直接消费；`verify-teacher-student-dashboard-contract.js`、教师端 JWT smoke、教师 CRUD smoke 与前端同步链已重新通过。
- 本次继续收学生端首页的伪变化值链：审计发现 [frontend/dist/student-dashboard.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/student-dashboard.html) 之前直接用 `Math.random()` 兜底 `coursesChange / assignmentsChange / examsChange`，并通过 `updateStatChange(..., '较上月/较上周')` 把这些占位值渲染成真实时间对比；同时后端 `StudentServiceImpl` 目前对相关 change 字段也基本只回 `0` 占位。现已为学生首页四张卡片统一降级为中性说明文案（如“当前课程总览”“当前待完成作业”“当前考试安排”“当前学习进度估算”），并移除前端随机 delta 与趋势式渲染逻辑；新增 `verify-student-dashboard-contract.js` 护栏。学生 CRUD smoke、教师 JWT smoke 与同步链均重新通过。
- 本次继续反向清 student dashboard 的后端死契约：`StudentDashboardDTO` 已移除 `courseCountChange / pendingAssignmentsChange / upcomingExamsChange / overallProgressChange` 四个只会由 `StudentServiceImpl#getStudentPerformance(...)` 固定回 `0` 的占位字段，学生首页 [frontend/dist/student-dashboard.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/student-dashboard.html) 也不再读取这些字段；`StudentServiceImplTest` 现已增加 DTO 反射护栏，`verify-student-dashboard-contract.js` 也会明确卡住 `data.courseCountChange / data.pendingAssignmentsChange / data.upcomingExamsChange / data.overallProgressChange` 不得回流。定向 Maven 回归 `77` 项、学生 CRUD smoke、教师 JWT smoke 与前端同步链均已通过。
- 本次继续收 student-stats 的伪变化值链：`StudentServiceImpl#getLearningStats(...)` 已移除 `studyTimeChange / completedTasksChange / averageScoreChange / knowledgeMasteryChange` 四个固定回 `0` 的占位字段，学习数据页 [frontend/dist/student-stats.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/student-stats.html) 也已不再把这些字段渲染成“比上周提升/下降”或“与上周持平”，统一改为中性说明文案（如“当前筛选学习时长”“当前筛选平均成绩”）；新增 `verify-student-stats-contract.js`，`StudentServiceImplTest` 也已补充 `getLearningStats()` 的反射/返回 shape 护栏。定向 Maven 回归 `78` 项、学生 CRUD smoke、教师 JWT smoke 与前端同步链均已通过。
- 本次继续清 `teacher-dashboard` 的前端死壳：页面卡片早已只显示中性说明文案，但 [frontend/dist/teacher-dashboard.html](D:/111/Distributed framework technology/JavaCode/majorassignment/frontend/dist/teacher-dashboard.html) 里还残留着 `payload.totalCoursesChange / totalStudentsChange / pendingAssignmentsChange / pendingExamsChange / warningCountChange` 的适配读取，以及 fallback 分支里对应的 `0` 占位值。现已移除这层无消费者的 change-field 搬运壳，只保留真实统计值与文案 label；`verify-teacher-dashboard-contract.js` 也已加严到禁止这些前端死读取和 `0` fallback 回流。教师端 JWT smoke、教师 CRUD smoke、学生 CRUD smoke 与前端同步链均已通过。
- 本次继续收 `teacher-student-dashboard` 的前端统计壳：虽然页面前几轮已经把四张卡片的文案收成中性说明，但卡片 DOM 与脚本里仍残留 `studentCountChange / averageScoreChange / assignmentCompletionRateChange / learningProgressChange` 这组旧 delta 壳、`positive/negative` 样式语义，以及空数据/异常分支里的“暂无变化数据”伪变化文案。现已统一改为 `studentCountMeta / averageScoreMeta / assignmentCompletionRateMeta / learningProgressMeta` 这组中性 metadata 行，并让成功/空数据/异常三条分支都稳定回写相同的真实说明文案，不再暗示存在时间对比；`verify-teacher-student-dashboard-contract.js` 已同步加严，教师端 JWT smoke、教师 CRUD smoke 与前端同步链均已通过。
- 本次继续收 `student-stats` 的前端统计壳：虽然学习数据页前几轮已经把四张卡片的内容收成中性说明，但 DOM 与脚本里仍残留 `studyTimeChange / completedTasksChange / averageScoreChange / knowledgeMasteryChange` 这组旧 delta 命名壳。现已统一改为 `studyTimeMeta / completedTasksMeta / averageScoreMeta / knowledgeMasteryMeta` 这组中性 metadata 行，并同步替换页面脚本中的对应查询/回写逻辑；`verify-student-stats-contract.js` 已加严为禁止旧 `*Change` id 和脚本查询回流，学生 CRUD smoke 与前端同步链均已通过。
- 本次继续收 `student-dashboard` 的前端统计壳：虽然学生首页前几轮已经把四张卡片的内容降级为中性说明，但 DOM id 与脚本里仍残留 `courseCountChangeLabel / assignmentCountChangeLabel / examCountChangeLabel / progressPercentageChangeLabel` 这组旧 delta label 命名，以及 `changeLabelOverrides` / `updateStatChange(...)` 这一层历史兼容壳。现已统一改为 `*MetaLabel` 与 `metaLabelOverrides` / `updateStatMeta(...)`，让页面命名、结构和语义三层对齐；`verify-student-dashboard-contract.js` 已同步加严，学生 CRUD smoke 与前端同步链均已通过。
- 本次继续收 `student-dashboard` 的空状态壳命名：学生首页在登录上下文缺失或仪表板请求失败时，实际走的是“空状态兜底”，但函数/注释/日志仍残留 `showMockData()`、`显示模拟数据`、`模拟数据加载` 这类误导性命名。现已统一改为 `showEmptyStateData()`、`显示空状态数据` 与“模拟异步数据加载”这组更贴近真实行为的表述，避免后续维护者误判页面还在展示假数据；`verify-student-dashboard-contract.js` 已同步加严，学生 CRUD smoke 与前端同步链均已通过。
- 本次继续收 `student-stats` 的默认值兜底话术：学习数据页虽然早已改为在错误/空返回场景下统一回落到 `0` 和空图表，但注释里仍残留“`不使用模拟数据` / `使用默认值而不是模拟数据`”这种历史过渡表述。现已统一改成更直接的“`使用默认值（0）` / `使用默认值`”口径，避免页面实现明明已经不走 mock 分支，注释却还在和“模拟数据”做对照；`verify-student-stats-contract.js`、学生 CRUD smoke 与前端同步链均已通过。
- 本次继续收 `teacher-dashboard` 的前端统计壳：教师首页卡片文案前几轮已经收成中性说明，但 DOM id 与脚本里仍残留 `totalCoursesChangeLabel / totalStudentsChangeLabel / pendingAssignmentsChangeLabel / pendingExamsChangeLabel / warningCountChangeLabel` 这组旧 delta label 命名，以及 `changeLabelOverrides` 这层历史兼容壳。现已统一改为 `*MetaLabel` 与 `metaLabelOverrides`，让教师首页与 student dashboard / student stats / teacher-student-dashboard 的前端统计壳命名保持一套中性语义；`verify-teacher-dashboard-contract.js`、教师端 JWT smoke、教师 CRUD smoke 与前端同步链均已通过。
- 本次继续收 `teacher-student-dashboard` 的默认值重复壳：学情页统计卡成功分支、空数据分支和异常分支，之前都在逐项重复回写同一组 `studentCountMeta / averageScoreMeta / assignmentCompletionRateMeta / learningProgressMeta` 文案。现已抽成共享 `dashboardStatMetaText` + `applyDashboardStatMetaText()` helper，让默认值文案和元信息只维护一处，减少后续回退面；`verify-teacher-student-dashboard-contract.js`、教师端 JWT smoke、教师 CRUD smoke 与前端同步链均已通过。
- 本次继续收 `teacher-dashboard` 的前端统计壳：教师首页 `updateStatistics()` 里原本还残留一段永远不会命中的旧 `Change` 分支，并把中性 metadata 文案分散挂在局部 `metaLabelOverrides` 上。现已统一抽成共享 `dashboardStatMetaLabelText` + `applyDashboardStatMetaLabels()` helper，并移除这段失效旧分支，让首页统计卡的 metadata 文案出口与 `teacher-student-dashboard` 保持一致；`verify-teacher-dashboard-contract.js`、教师端 JWT smoke、教师 CRUD smoke 与前端同步链均已通过。后续复核确认：此前偶发的 `sync-frontend-to-static.ps1 -CheckOnly` 红灯主要来自把 `sync` 与 `checkOnly` 并行执行造成的竞态，顺序执行后可稳定恢复绿色。
- 本次继续收 `teacher-dashboard` 的图表与最近活动空状态壳：教师首页 `initCharts()` 原本在平均分图和提交率图里各自手写 `暂无数据 / [0]` 兜底，`updateRecentActivities()` 也单独内联构造“暂无活动记录”项。现已统一抽成 `buildChartCategoryAxisData()`、`buildChartSeriesData()`、`createEmptyRecentActivityItem()` 与 `revealActivityItem()` 这组共享 helper，减少空状态构造重复点，并让图表/活动区的默认表现更一致；`verify-teacher-dashboard-contract.js`、教师端 JWT smoke、教师 CRUD smoke 与前端同步链均已通过，且顺序执行 `sync-frontend-to-static.ps1 -> -CheckOnly` 已稳定恢复绿色。
- 本次继续收 `teacher-dashboard` 的加载态重复定义：教师首页原本同时存在两套 `showLoadingState()/hideLoadingState()` 实现，后定义的版本会直接覆盖前面的统计卡片 overlay 逻辑，属于真实重复实现而不是单纯命名残留。现已统一保留一套加载态出口，并抽成 `buildChartLoadingStateMarkup()`、`appendStatCardLoadingOverlays()`、`replaceChartContainersWithLoadingState()`、`removeStatCardLoadingOverlays()`、`removeChartContainerLoadingStates()` 这组 helper，减少行为漂移面；`verify-teacher-dashboard-contract.js` 已加严为要求 `showLoadingState/hideLoadingState` 仅定义一次。教师端 JWT smoke、教师 CRUD smoke 与前端同步链均已通过；此前偶发的 `sync-frontend-to-static.ps1 -CheckOnly` 红灯已确认为并行执行竞态，顺序执行后可稳定恢复绿色。
- 本次继续收 `teacher-dashboard` 的前置依赖 guard：课程、班级、作业、考试、预警等 loader 原本各自重复写 `contentContainer 未初始化` 与 `teacherAPI 未定义 / 系统服务正在加载中，请稍后重试` 这两层 guard。现已统一抽成 `ensureContentContainerReady()` 与 `ensureTeacherApiReady()`，把高频前置检查收成共享出口，减少同类错误提示和初始化检查的分叉点；`verify-teacher-dashboard-contract.js`、教师端 JWT smoke、教师 CRUD smoke 与前端同步链均已通过；此前偶发的 `sync-frontend-to-static.ps1 -CheckOnly` 红灯已确认为并行执行竞态，顺序执行后可稳定恢复绿色。
- 本次继续收 `teacher-dashboard` 内容区的空列表/错误态重复壳：课程、班级、作业、预警几条 loader 原本都在各自手写 `empty-state` 和 `error-state` HTML，考试空态与筛选空态也单独内联。现已统一抽成 `buildTeacherEmptyStateMarkup()`、`buildTeacherTableEmptyRow()` 与 `buildTeacherSectionErrorMarkup()`，让内容区空列表和错误态有了共享出口；`verify-teacher-dashboard-contract.js`、教师端 JWT smoke、教师 CRUD smoke 与前端同步链均已通过；此前偶发的 `sync-frontend-to-static.ps1 -CheckOnly` 红灯已确认为并行执行竞态，顺序执行后可稳定恢复绿色。
- 本次补充修正文档验证口径：针对最近几轮多次把 `sync-frontend-to-static.ps1 -CheckOnly` 记成“脚本检查口径噪音”的描述，已重新按顺序执行 `sync-frontend-to-static.ps1` 后再执行 `-CheckOnly` 复核，结果稳定绿色；此前零星 `[DIFF] teacher-dashboard.html` 主要来自把同步与检查并行执行导致的竞态，不应继续在交付文档中表述为脚本本身存在噪音。
- 本次继续收 `teacher-dashboard` 内容区的标题/时间戳骨架：课程、班级、作业、预警四条最对称的 loader 原本都在各自重复写 `fade-in + h2 + 当前时间` 这层 dated section 外壳。现已统一抽成 `buildTeacherDatedSectionMarkup()`，并切换这四条 loader 复用共享 section renderer，在不碰业务表格内容的前提下进一步压薄页面壳层；`verify-teacher-dashboard-contract.js`、顺序执行的 `sync-frontend-to-static.ps1 -> -CheckOnly`、教师端 JWT smoke 与教师 CRUD smoke 均已通过。
- 本次继续收 `teacher-dashboard` 的说明型 section 壳：`学习数据 / AI辅助工具 / 系统设置` 三段原本都在各自手写 `fade-in + teacher-panel + title + description + info-state` 结构，现已统一抽成 `buildTeacherInfoSectionMarkup()` 并切换三条 loader 复用，在不改业务语义的前提下继续压薄页面骨架；`verify-teacher-dashboard-contract.js`、顺序执行的 `sync-frontend-to-static.ps1 -> -CheckOnly`、教师端 JWT smoke 与教师 CRUD smoke 均已通过。
- 本次继续下沉 `teacher-dashboard` 的 section helper 骨架：新增 `buildTeacherSectionShell()`，让 `buildTeacherSectionErrorMarkup()`、`buildTeacherDatedSectionMarkup()` 与 `buildTeacherInfoSectionMarkup()` 统一从共享底座长出，去掉三类 section helper 各自重复的 `fade-in + section title + subtitle/body 容器` 骨架；`verify-teacher-dashboard-contract.js`、顺序执行的 `sync-frontend-to-static.ps1 -> -CheckOnly`、教师端 JWT smoke 与教师 CRUD smoke 均已通过。
- 本次继续收 `teacher-dashboard` 的页面分发壳：`loadPageContent()` 原本依赖一整段 `switch (page)` 手工分发 `dashboard/courses/classes/assignments/exams/learning-data/warnings/ai-tools/settings` 各类 loader，现已统一改为 `teacherPageLoaders` 分发表，并通过 `pageLoader || teacherPageLoaders.dashboard` 兜底默认路径，减少分发层分叉与后续新增页面时的改动面；`verify-teacher-dashboard-contract.js`、顺序执行的 `sync-frontend-to-static.ps1 -> -CheckOnly`、教师端 JWT smoke 与教师 CRUD smoke 均已通过。
- 本次继续收 `teacher-dashboard` 的 dashboard 加载态职责：导航分发层 `loadPageContent()` 与 `loadDashboardContent()` 之前都会在 dashboard 路径上各自执行一次 `showLoadingState()/hideLoadingState()`，形成双重 loading 包裹。现已为 `loadDashboardContent(options)` 新增 `manageLoading` 选项，并让 `teacherPageLoaders.dashboard` 显式走 `loadDashboardContent({ manageLoading: false })`，把导航切换场景下的 loading 控制统一留在分发层，而 `window.onload` 与定时刷新仍沿用默认自管 loading 语义；`verify-teacher-dashboard-contract.js`、顺序执行的 `sync-frontend-to-static.ps1 -> -CheckOnly`、教师端 JWT smoke 与教师 CRUD smoke 均已通过。
- 本次继续收 `teacher-dashboard` 的初始化链：`window.onload` 原本同时承担教师会话检查后的 header 初始化、首屏 dashboard 加载、最近活动刷新按钮绑定与 30 秒自动刷新注册。现已拆成 `initializeTeacherDashboardPage()` 与 `startTeacherDashboardAutoRefresh()` 两个 helper，让 `window.onload` 只保留会话检查和启动调度，初始化职责边界更清楚，同时保留现有首屏加载和定时刷新行为不变；`verify-teacher-dashboard-contract.js`、顺序执行的 `sync-frontend-to-static.ps1 -> -CheckOnly`、教师端 JWT smoke 与教师 CRUD smoke 均已通过。
- 本次继续收 `teacher-dashboard` 内容区 loader 的重复控制壳：课程、班级、学情预警以及 `学习数据 / AI辅助工具 / 系统设置` 这几条 loader 原本各自重复写 `contentContainer` guard、`teacherAPI` guard、`try/catch` 与 section error 回退。现已统一抽成 `ensureTeacherLoaderPrerequisites(requireApi)` 与 `renderTeacherSectionContent(title, renderContent, options)`，让这些内容区 loader 共享同一条“前置检查 + 成功回填 + 错误 section”路径，而把作业/考试两条更重的搜索与事件绑定链先保留在原地，避免过度抽象；`verify-teacher-dashboard-contract.js` 已先红后绿，顺序执行的 `sync-frontend-to-static.ps1 -> -CheckOnly`、教师端 JWT smoke 与教师 CRUD smoke 均已通过。中途 fresh session 过期后已用 `get-dev-auth-session.ps1` 刷新 teacher/student 会话再补跑验证。
- 本次继续下沉 `teacher-dashboard` 内容区共享 loader：在上一轮统一了“前置检查 + 成功回填 + 错误回退”之后，又为 `renderTeacherSectionContent(title, renderContent, options)` 补了最薄的一层 `afterRender` 能力，用来承接渲染后的 DOM 事件绑定。这样 `loadAssignmentsContent()` 与 `loadExamsContent()` 也已切到共享入口上，分别把“作业搜索按钮绑定”与 `setupExamsSearchAndFilter()` 放进 `afterRender`，从而把 assignment/exam 两条重 loader 纳入同一条 section 控制路径，同时保留各自不同的搜索参数和筛选语义，不去强行抽成同一种搜索模型；`verify-teacher-dashboard-contract.js` 已先红后绿，顺序执行的 `sync-frontend-to-static.ps1 -> -CheckOnly`、教师端 JWT smoke 与教师 CRUD smoke 均已通过。
- 本次继续统一 `teacher-dashboard` 的 teacher API 入口：页面内大多数 loader 早已通过 `window.teacherAPI` 取数，但 `buildDashboardFromAvailableApis()` 与 `loadAssignmentsContent()` 仍各自 `new APIService()` / `new TeacherAPI(...)` 走本地 client 路径，导致同页同时存在全局 bridge 和局部 client 两套 teacher API 入口。现已新增 `getTeacherDashboardApi()`，优先复用 `window.teacherAPI`，必要时再回退创建本地 `TeacherAPI`，并让 dashboard 聚合链与作业 loader 都切到这条统一入口上；`verify-teacher-dashboard-contract.js` 已先红后绿，顺序执行的 `sync-frontend-to-static.ps1 -> -CheckOnly`、教师端 JWT smoke 与教师 CRUD smoke 均已通过。
- 本次继续收 `teacher-dashboard` 中 assignment 搜索绑定的局部重复壳：在不强行统一 exam 搜索模型的前提下，已新增 `readAssignmentSearchParams(previousSearchParams)` 与 `bindAssignmentSearchControls(previousSearchParams)`，把 assignment 自己的搜索框/课程下拉/状态下拉读取与重新加载绑定从 `afterRender` 的内联匿名函数里抽出，继续压薄 `loadAssignmentsContent()` 的局部控制壳，同时保持 exam 侧 `setupExamsSearchAndFilter()` 独立不动，避免制造一层发虚的“统一筛选模型”；`verify-teacher-dashboard-contract.js` 已先红后绿，顺序执行的 `sync-frontend-to-static.ps1 -> -CheckOnly`、教师端 JWT smoke 与教师 CRUD smoke 均已通过。
- 本次继续收 `teacher-dashboard` 中 exam 搜索区的局部重复：在延续“不硬造 assignment/exam 统一筛选模型”的前提下，已新增 `buildExamSearchPanelMarkup(courses = [])` 与 `readExamFilterValues()`，把考试页两段几乎相同的搜索面板 HTML 和 `filterExams()` 开头那组筛选值读取收成 exam 自己的局部 helper。这样 `loadExamsContent()` 里的有数据/空数据分支不再各自手写同一套搜索区，`filterExams()` 也不再直接碰三处 DOM 读取；`verify-teacher-dashboard-contract.js` 已先红后绿，顺序执行的 `sync-frontend-to-static.ps1 -> -CheckOnly`、教师端 JWT smoke 与教师 CRUD smoke 均已通过。
- 本次继续收 `teacher-dashboard` 中 exam 过滤链的重复取数：`loadExamsContent()` 已经拿到一次考试列表，但 `filterExams()` 之前每次筛选还会重新 `getExams()` 一次。现已新增页内 `examListState`，由 `loadExamsContent()` 在成功取数后回写，`filterExams()` 则直接复用这份本地考试列表做筛选，不再为每次本地过滤重复拉取远端数据；删除考试后仍通过 `loadExamsContent()` 重新加载这份状态。`verify-teacher-dashboard-contract.js` 已先红后绿，顺序执行的 `sync-frontend-to-static.ps1 -> -CheckOnly`、教师端 JWT smoke 与教师 CRUD smoke 均已通过；中途 teacher/student fresh session 过期后已重新续期再补跑验证。
- 2026-05-24: `teacher-warning.html` 统计卡片已移除前端推导的伪变化值和涨跌箭头，`loadStats(...)` 现仅展示真实预警数量，并统一收成中性 metadata 文案（`当前筛选预警统计 / 当前未处理预警统计 / 当前处理中预警统计 / 当前已解决预警统计`）。新增契约脚本 `scripts/verify-teacher-warning-contract.js` 防止旧 `+x 条 (y%)` 与 `fa-arrow-up/down` 语义回流；已顺序通过 `node .\scripts\verify-teacher-warning-contract.js`、`powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1`、`powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1 -CheckOnly`、`node .\scripts\verify-teacher-jwt-pages.js .\.runtime-logs\teacher-session-polish-fresh.json`、`node .\scripts\verify-teacher-browser-crud.js .\.runtime-logs\teacher-session-polish-fresh.json .\.runtime-logs\student-session-polish-fresh.json`。
- 2026-05-24: `teacher-warning.html` 图表初始化已移除“API失败时塞默认预警数据”的兜底。`initCharts()` / `updateCharts()` 现在在接口失败或筛选后无数据时统一渲染真实空态，不再用假 warning 对象把图表画满；同时图表按钮切换从读标题文本改成 `data-chart-kind / data-chart-view / data-time-range` 驱动，并补充 `destroyWarningCharts()`、`renderWarningChartsCanvasState()`、`renderWarningChartsEmptyState()`、`bindWarningChartControls()` 等 helper。`scripts/verify-teacher-warning-contract.js` 已加严并通过，顺序验证 `sync -> -CheckOnly -> teacher JWT smoke -> teacher browser CRUD smoke` 也已全绿。
- 2026-05-24: `teacher-warning.html` 已新增共享 warning analytics 取数链，用于复用图表初始化、图表更新和统计 fallback 的全量预警数据。新增 `warningAnalyticsListState`、`buildWarningAnalyticsListParams(...)`、`fetchWarningAnalyticsList(...)`，并让 `loadStats(...)` 的 stats API 回退、`initCharts()`、`updateCharts()` 统一走这条 helper；分页列表 `loadWarnings(...)` 仍保持独立，避免把分页语义和图表/统计分析硬绑死。`scripts/verify-teacher-warning-contract.js` 已更新并通过，顺序验证 `sync -> -CheckOnly -> teacher JWT smoke -> teacher browser CRUD smoke` 全绿。
- 2026-05-24: `teacher-warning.html` 已清理退场 helper `generateWarnings(...)`，并补通预警详情弹窗状态链。新增 `normalizeWarningStatus(...)`、`updateWarningDetailStatusBadge(...)`、`updateWarningDetailActionState(...)`，列表卡片状态不再只依赖 `isResolved`，而是优先按 `status` 归一化；详情弹窗中的“标记为处理中 / 标记为已解决”按钮已真正接线到 `markAsProcessing(...)` / `resolveWarning(...)`，处理后会同步刷新 badge 和按钮可见性。`scripts/verify-teacher-warning-contract.js` 已更新并通过；中途出现的 401 仅为 teacher/student fresh session 过期，续新会话后 `verify-teacher-jwt-pages.js` 与 `verify-teacher-browser-crud.js` 已重新全绿。
- 2026-05-24: `teacher-warning.html` 预警详情弹窗已移除没有后端来源的占位信息，改为只展示 `/api/early-warnings/teacher/detail/{warningId}` 当前真实返回能证明的字段。详情页现在使用 `warningDetail.studentName / courseName / warningLevel / reason / suggestion / triggerDate / resolvedNote` 回填，固定班级、联系方式、家长联系方式、辅导员、学习数据和成绩图已撤下；教师 JWT 页面验证、教师 CRUD smoke、前端同步与 `scripts/verify-teacher-warning-contract.js` 全部通过。
- 2026-05-24: `teacher-knowledge.html` 知识点详情弹窗已按当前后端真实能力范围诚实降级。前端审计确认：分析接口 `/api/knowledge-points/analysis/teacher/course` 稳定返回 `courseName / knowledgePointDistribution / atRiskStudents / weakTopics`，详情接口 `/api/teacher/knowledge-points/{id}` 仅稳定返回 `KnowledgePoint(id / pointName / description / difficulty / orderIndex / courseId)`；因此页面已移除 `masteryLevel / difficultyLevel / importanceLevel / poorStudentsPercentage / topErrorQuestions / teachingSuggestions / weakStudentsList` 等无后端来源的假详情，只保留基础字段与“主分析区为准”的说明。新增 `scripts/verify-teacher-knowledge-contract.js` 护栏，并已顺序通过 `node .\scripts\verify-teacher-knowledge-contract.js`、`sync-frontend-to-static.ps1`、`sync-frontend-to-static.ps1 -CheckOnly`、`verify-teacher-jwt-pages.js`、`verify-teacher-browser-crud.js` 验证；中途 fresh session 过期后已续新 teacher/student 会话并补跑通过。
- 2026-05-24: `teacher-knowledge.html` 旧“仍依赖旧会话认证、当前微服务联调环境暂显示空结果”降级链已收正。实测当前 teacher 会话下 `/api/teacher/knowledge-points` 与 `/api/knowledge-points/analysis/teacher/course` 均稳定返回 `200`，旧文案已不再符合当前真实状态；现已将 401/403 分支统一改为中性 unavailable 语义（如“知识点分析服务暂不可用，请稍后重试”“知识点列表暂不可用，请稍后重试”），保留空数据场景继续诚实显示“暂无知识点数据”。`scripts/verify-teacher-knowledge-contract.js` 已加严为禁止旧 legacy helper 与旧文案回流，并已顺序通过 `node .\scripts\verify-teacher-knowledge-contract.js`、`sync-frontend-to-static.ps1`、`sync-frontend-to-static.ps1 -CheckOnly`、`verify-teacher-jwt-pages.js`、`verify-teacher-browser-crud.js` 验证。
- 2026-05-24: `teacher-knowledge.html` 图表切换链已移除对标题文本的行为耦合。页面原本依赖 `chartCard.querySelector('.chart-card-title').textContent` 判断当前操作的是“知识点掌握程度分布”还是“知识点掌握程度雷达图”，标题改名就可能带出图表行为回退。现已为两张图卡补充稳定的 `data-chart-kind`，为切换按钮补充 `data-chart-view`，并让切换逻辑改为完全基于 `data-*` 分流；`scripts/verify-teacher-knowledge-contract.js` 已加严为禁止旧 `chartTitle === ...` 判断回流，并已顺序通过 `node .\scripts\verify-teacher-knowledge-contract.js`、`sync-frontend-to-static.ps1`、顺序执行的 `sync-frontend-to-static.ps1 -CheckOnly`、`verify-teacher-jwt-pages.js`、`verify-teacher-browser-crud.js` 验证。
- 2026-05-24: `teacher-knowledge.html` 已开始收知识点列表与分析 merge 的重复取数链。页面原本会先在 `loadFilterOptions()` 里单独 `getKnowledgePointList()` 一次填充筛选下拉，随后每次 `handleQuery()` 又会在 `mergeKnowledgeAnalysisWithPointList()` 里再打一次同样的列表接口补分析结果。现已新增页内 `knowledgePointListState` 与统一 `fetchKnowledgePointList({ forceRefresh })` helper，让筛选初始化和分析 merge 共享同一份知识点列表状态；创建/编辑/删除知识点成功后也会显式 `forceRefresh` 并回填下拉，避免本地状态陈旧。`scripts/verify-teacher-knowledge-contract.js` 已加严为禁止旧 `getKnowledgePointList()` 直接回流，并已顺序通过 `node .\scripts\verify-teacher-knowledge-contract.js`、`sync-frontend-to-static.ps1`、顺序执行的 `sync-frontend-to-static.ps1 -CheckOnly`、`verify-teacher-jwt-pages.js`、`verify-teacher-browser-crud.js` 验证。
- 2026-05-24: `teacher-knowledge.html` 的页面提示 helper 已按真实职责收口。原 `showKnowledgeMicroserviceNotice(...) / hideKnowledgeMicroserviceNotice()` 现在既承载分析不可用提示，又承载“分析报告已导出到本地文本文件”这类普通页面信息，`microservice` 命名已经和真实职责不匹配。现已统一改为 `showKnowledgePageNotice(...) / hideKnowledgePageNotice()`，DOM id 也同步切到 `knowledge-page-notice`，让命名、职责和调用点重新对齐；`scripts/verify-teacher-knowledge-contract.js` 已加严为禁止旧 notice helper 与旧 DOM id 回流，并已顺序通过 `node .\scripts\verify-teacher-knowledge-contract.js`、`sync-frontend-to-static.ps1`、顺序执行的 `sync-frontend-to-static.ps1 -CheckOnly`、`verify-teacher-jwt-pages.js`、`verify-teacher-browser-crud.js` 验证。
- 2026-05-24: `teacher-knowledge.html` 的页面初始化链已统一入口。页面原本同时存在三段 `DOMContentLoaded`（教师会话/chrome、图表与首屏加载、查询/重置/筛选事件）以及两段顶层即刻执行的表格按钮绑定，初始化职责分散在多个入口里。现已统一改为单一 `document.addEventListener('DOMContentLoaded', initializeTeacherKnowledgePage);`，并拆成 `ensureTeacherKnowledgeAccess()`、`initializeTeacherKnowledgeChrome()`、`initializeKnowledgeManagementControls()`、`initializeKnowledgeAnalysisCharts()`、`initializeKnowledgeFilterControls()`、`initializeKnowledgeTableActions()` 这组薄 helper，再顺序初始化课程下拉、图片错误处理、分页与首屏 `loadInitialData()`，把控制流收成一条清晰的页面启动链。`scripts/verify-teacher-knowledge-contract.js` 已加严为要求仅保留一个 `DOMContentLoaded` 入口，并已顺序通过 `node .\scripts\verify-teacher-knowledge-contract.js`、`sync-frontend-to-static.ps1`、顺序执行的 `sync-frontend-to-static.ps1 -CheckOnly`、`verify-teacher-jwt-pages.js`、`verify-teacher-browser-crud.js` 验证。

- 2026-05-24: 修正 teacher-knowledge 学生筛选链。学生下拉在同时选择班级与课程时，原实现会因为 TeacherAPI.getStudents 优先走 classId 分支而忽略 courseId。现已将联合筛选改为按课程学生集与班级学生集取交集，并把学生列表成功返回、去重、分页全量聚合分别收成 helper；verify-teacher-knowledge-contract.js 已加严，teacher JWT 页面链与 teacher CRUD smoke fresh 通过。

- 2026-05-24: teacher-knowledge 班级/课程列表改为页内状态复用。新增 classListState / courseListState、normalizeTeacherCollection、fetchTeacherClassList({ forceRefresh })、fetchTeacherCourseList({ forceRefresh })，统一服务于筛选区班级下拉、筛选区课程下拉、知识点编辑课程下拉，以及 fetchAllTeacherStudents 的全部学生扇出链，减少重复 getClasses/getCourses 请求。contract verifier、静态同步检查、teacher JWT 页面链与 teacher CRUD smoke fresh 通过。

- 2026-05-24: teacher-knowledge 学生联动筛选监听已从 loadFilterOptions() 收回 initializeKnowledgeFilterControls()。这样筛选选项加载只负责取数和回填，事件绑定统一留在初始化控制流里，避免将来因重复加载筛选项导致 class-select/course-select 上的 throttledUpdateStudentOptions 叠绑。contract verifier、静态同步检查、teacher JWT 页面链与 teacher CRUD smoke fresh 通过。
- 2026-05-24: 修复 teacher-knowledge 重置筛选后的学生下拉刷新缺口。此前 reset 只把 class/course/student/knowledge-point 的 select 值改回 all 并重新查询，但不会重建学生候选项，导致 student-select 可能仍停留在上一次班级/课程子集。现已将 reset 回调改为 async，在清理 localStorage 后先 await updateStudentOptions()，再将 student-select 复位为 all，保证“全部班级/全部课程”场景下学生候选与筛选状态一致。contract verifier、静态同步检查、teacher JWT 页面链与 teacher CRUD smoke fresh 通过。
- 2026-05-24: 修复 teacher-knowledge 学生下拉刷新链中的假 await。此前 updateStudentOptions() 内部 await 的是 throttle 包装器返回值，而该包装器不会返回内部异步函数的 promise，导致 loadFilterOptions()/reset 里的 await updateStudentOptions() 实际不会等待学生候选刷新完成。现已拆出 async refreshStudentOptions() 作为主动刷新链，throttledUpdateStudentOptions 只保留给 class/course change 事件，内部转调 refreshStudentOptions().catch(...)；updateStudentOptions() 现在直接 await refreshStudentOptions()。contract verifier、静态同步检查、teacher JWT 页面链与 teacher CRUD smoke fresh 通过。
- 2026-05-24: 继续收顺 teacher-knowledge 学生候选刷新控制流。新增 getStudentListParamsFromFilters()、buildStudentOptionsFilterKey()、studentOptionsFilterKey、studentOptionsRequestToken，并将 refreshStudentOptions(options) 改造成真正的主动刷新链：支持 forceRefresh、相同 class/course 组合直接短路、只让最新请求结果落地。与此同时，将 class/course 事件侧从 throttle 改为 debouncedUpdateStudentOptions，内部 refreshStudentOptions().then(() => saveFilterConditions(); updateFilterBadges())；query/reset 则继续显式 await updateStudentOptions()。这使学生候选刷新在行为上更接近“主动路径可等待，事件路径可合并，旧请求不覆盖新筛选”。contract verifier、静态同步检查、teacher JWT 页面链与 teacher CRUD smoke fresh 通过。
- 2026-05-24: teacher-knowledge 新增统一筛选动作入口 applyKnowledgeFilters(options)。当前至少已将 queryBtn、resetBtn、removeFilter 三条链拉回同一语义：统一保存筛选条件、更新 badge、按需刷新学生候选、再触发 handleQuery。尤其修复了 removeFilter(class/course) 之前只改 select 值就查询、却不刷新 student-select 候选的行为缺口。contract verifier、静态同步检查、teacher JWT 页面链与 teacher CRUD smoke fresh 通过。
- 2026-05-24: teacher-courses 班级学生弹窗“添加学生”链已收正为真实可用能力。此前前端把输入描述成“学生ID/学号”，但实际调用的是 `PUT /api/teacher/students/{studentId}`，后端又先按“教师已可见学生”做权限门槛，导致“把系统里已存在的学生加入我管理的班级”能力既不诚实也不完整。现已新增专用接口 `POST /api/teacher/classes/{classId}/students`，支持按学生ID或用户名解析目标学生，只为“加入当前教师可管理班级”这条路放开未分班学生入口，同时保留资料编辑/学情修改原有的可见性边界；若学生已在其他班级且该班级并非当前教师可管理范围，接口会拒绝移动。前端 `teacher-courses-students.js` 已切到新接口，并把提示文案与表头统一为“学生ID / 用户名（学号）”；新增 `scripts/verify-teacher-courses-students-contract.js` 护栏，并已顺序通过 `node .\\scripts\\verify-teacher-courses-students-contract.js`、`powershell -ExecutionPolicy Bypass -File .\\scripts\\sync-frontend-to-static.ps1`、`powershell -ExecutionPolicy Bypass -File .\\scripts\\sync-frontend-to-static.ps1 -CheckOnly`、`mvn --% -pl major_assignment -Dtest=TeacherDashboardControllerTest test`、`node .\\scripts\\verify-teacher-jwt-pages.js .\\.runtime-logs\\teacher-session-polish-fresh.json`、`node .\\scripts\\verify-teacher-browser-crud.js .\\.runtime-logs\\teacher-session-polish-fresh.json .\\.runtime-logs\\student-session-polish-fresh.json`。
- 2026-05-24：继续补齐 `teacher-courses` 班级学生链在微服务 runtime 中的真实落点。浏览器 smoke 追查确认：gateway 实际命中的是 course-service 的 `TeacherCourseAdminController`，此前只有 `GET /api/teacher/classes/{classId}/students`，因此 teacher CRUD smoke 在“把已有学生加入班级”步骤上返回 `405 请求方法不支持`。现已在 course-service 新增 `POST /api/teacher/classes/{classId}/students` 以及 `CourseApplicationService.addStudentToClass(...)`，支持 `studentIdentifier` 按数字 ID 或用户名解析、未分班学生直接加入、已在教师可管理班级中的学生先返回 `needConfirm=true`、携带 `forceReplace=true` 再执行换班、已在教师不可管理班级中的学生返回 `403 无权移动该学生所在班级`。对应 `CourseControllerTest` 与 `CourseApplicationServiceTest` 已补上控制器/服务红绿灯，并通过 `mvn --% -pl course-service -am -Dtest=CourseControllerTest,CourseApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` 验证；随后重新打包 course-service、重启 runtime stack，再跑 `node .\scripts\verify-teacher-browser-crud.js .\.runtime-logs\teacher-session-polish-fresh.json .\.runtime-logs\student-session-polish-fresh.json`，teacher-courses 这段已由原先的 `405` 前进为真实通过。
- 2026-05-24：修正 `scripts/verify-teacher-browser-crud.js` 的状态污染。teacher-courses 步骤在验证“可将已有学生移动到新建班级”后，原本只在最终 cleanup 阶段才恢复学生原班级；当当前环境下唯一可移动学生恰好是 `student42` 时，会让后续 “teacher assignments page CRUD: assignment/exam/grade” 中的学生考试提交因失去课程可见性而报“考试不存在或无权访问”。现已将 smoke 调整为：在 teacher-courses 步骤确认移动成功后立即把学生恢复回原班级，并清空 `created.movedStudent`，避免污染后续 assignment/exam/notification/settings 验证链。续新 teacher/student 会话后，`verify-teacher-browser-crud.js` 已重新达到 `Teacher browser CRUD smoke passed: 5`。
- 2026-05-24: 为 `teacher-courses` 新增班级学生接口补充独立 runtime verifier。现已新增 `scripts/seed-isolated-teacher-course-student-assign.sql`、`scripts/seed-isolated-teacher-course-student-assign.ps1` 与 `scripts/verify-teacher-courses-student-add-runtime.js`，使用隔离 fixture 直接验证四条核心语义：未分班学生按用户名直接加入、教师可管理原班级学生先返回 `needConfirm`、携带 `forceReplace` 后完成换班并可恢复、教师不可管理原班级学生返回 `403 无权移动该学生所在班级`。该 targeted verifier 已与 `verify-teacher-browser-crud.js`、`verify-teacher-jwt-pages.js`、`verify-teacher-courses-students-contract.js` 以及顺序执行的 `sync-frontend-to-static.ps1 -> -CheckOnly` 一起 fresh 通过。
- 2026-05-24: `teacher-ai-tools.html` 已从本地伪 AI 结果页收正为“3 条真实能力 + 3 条诚实降级”。前端审计确认当前后端 AI 服务仅稳定暴露 `POST /api/ai/generate-questions`、`POST /api/ai/generate-exam`、`POST /api/ai/learning-suggestions` 三条能力；页面原先六个工具函数都通过 `setTimeout + mockResults` 直接构造本地结果并展示。现已新增 `requestTeacherAi(endpoint, payload)`，并将题目生成、试卷生成、学习建议三条链分别切到真实接口；同时将“学情分析”口径收成“AI学习建议”，按 `studentId` 真实请求学习建议。知识点讲解、作业批改、教学计划三条当前无后端来源的工具已降级为明确的不可用状态说明，不再展示本地捏造结果。页面还新增 `teacher-ai-tools-availability-banner`，明确声明“当前已接通：题目生成、试卷生成、学习建议。知识点讲解、作业批改、教学计划暂不可用。” 已新增 `scripts/verify-teacher-ai-tools-contract.js` 护栏，禁止 `mockResults`、`// 模拟AI生成过程` 与旧成功文案回流；并顺序通过 `node .\\scripts\\verify-teacher-ai-tools-contract.js`、`powershell -ExecutionPolicy Bypass -File .\\scripts\\sync-frontend-to-static.ps1`、`powershell -ExecutionPolicy Bypass -File .\\scripts\\sync-frontend-to-static.ps1 -CheckOnly`、`powershell -ExecutionPolicy Bypass -File .\\scripts\\get-dev-auth-session.ps1 -Role teacher -OutFile .\\.runtime-logs\\teacher-session-polish-fresh.json`、`powershell -ExecutionPolicy Bypass -File .\\scripts\\get-dev-auth-session.ps1 -Role student -OutFile .\\.runtime-logs\\student-session-polish-fresh.json`、`node .\\scripts\\verify-teacher-jwt-pages.js .\\.runtime-logs\\teacher-session-polish-fresh.json`、`node .\\scripts\\verify-teacher-browser-crud.js .\\.runtime-logs\\teacher-session-polish-fresh.json .\\.runtime-logs\\student-session-polish-fresh.json`。
- 2026-05-24: `teacher-ai-tools.html` 已新增独立运行时验证脚本 `scripts/verify-teacher-ai-tools-runtime.js`。该 verifier 使用 fresh teacher JWT 会话直连页面，覆盖 7 条关键语义：availability banner 真实声明、题目生成命中 `/api/ai/generate-questions` 并渲染结果、试卷生成命中 `/api/ai/generate-exam` 并渲染结果、学习建议命中 `/api/ai/learning-suggestions` 并渲染结果，以及知识点讲解/作业批改/教学计划三条未接通能力保持“无 `/api/ai/*` 请求 + 明确不可用说明”的诚实降级。已通过 `node .\\scripts\\verify-teacher-ai-tools-runtime.js .\\.runtime-logs\\teacher-session-polish-fresh.json` 验证，并与 contract、teacher JWT 页面链、teacher browser CRUD smoke 共同组成这页的新闭环证据。
- 2026-05-24：`student-ai-assistant.html` 已从纯前端演示聊天收正为“真实学习建议 + 诚实降级自由问答”。页面原先通过 `setTimeout + getAIMockResponse(...)` 本地生成 AI 回复，并用“当前页面为演示模式：会展示本地模拟回复，不会调用真实 AI 服务。”文案暗示整页仍是 demo。现已接通真实 `POST /api/ai/learning-suggestions`，由 student 角色直接按当前登录学生身份生成学习建议；quick actions 也已改为“生成我的学习建议 / 给我本周复习建议 / 帮我梳理当前学习重点”。与此同时，通用自由问答已改为明确的不可用说明：当前运行时仅支持学习建议，不再假装有通用聊天能力。新增 `scripts/verify-student-ai-assistant-contract.js` 与 `scripts/verify-student-ai-assistant-runtime.js` 两层护栏，并将 `student-ai-assistant` 纳入 `scripts/verify-student-jwt-pages.js` 页面链。已顺序通过 `node .\\scripts\\verify-student-ai-assistant-contract.js`、`node .\\scripts\\verify-student-ai-assistant-runtime.js .\\.runtime-logs\\student-session-polish-fresh.json`、`powershell -ExecutionPolicy Bypass -File .\\scripts\\sync-frontend-to-static.ps1`、`powershell -ExecutionPolicy Bypass -File .\\scripts\\sync-frontend-to-static.ps1 -CheckOnly`、`node .\\scripts\\verify-student-jwt-pages.js .\\.runtime-logs\\student-session-polish-fresh.json`、`node .\\scripts\\verify-student-browser-crud.js .\\.runtime-logs\\teacher-session-polish-fresh.json .\\.runtime-logs\\student-session-polish-fresh.json`、`node .\\scripts\\verify-teacher-browser-crud.js .\\.runtime-logs\\teacher-session-polish-fresh.json .\\.runtime-logs\\student-session-polish-fresh.json`、`node .\\scripts\\verify-teacher-jwt-pages.js .\\.runtime-logs\\teacher-session-polish-fresh.json`；中途为避免会话过期噪音，已续新 `teacher-session-polish-fresh.json` 与 `student-session-polish-fresh.json`。 
- 2026-05-24：`student-settings.html` 的“数据导出”链已从“真实接口优先 + 本地拼装 JSON 兜底”收成更诚实的真实导出语义。前端审计确认 `/api/student/export-data` 在 monolith 与 user-service compatibility 路由中都已接通，并已有既有架构测试要求页面必须通过该 Gateway 路由导出；因此页面现在会在成功时直接解析服务端 JSON 响应、生成 `student-data-export-YYYY-MM-DD.json`，并显示“学生学习数据导出成功”，而不再在失败时悄悄导出“当前页面 + 本地偏好”的拼装文件来伪装成完整学习数据导出。新增 `scripts/verify-student-settings-export-contract.js` 与 `scripts/verify-student-settings-export-runtime.js` 两层护栏；已顺序通过 `node .\\scripts\\verify-student-settings-export-contract.js`、`node .\\scripts\\verify-student-settings-export-runtime.js .\\.runtime-logs\\student-session-polish-fresh.json`、`powershell -ExecutionPolicy Bypass -File .\\scripts\\sync-frontend-to-static.ps1`、`powershell -ExecutionPolicy Bypass -File .\\scripts\\sync-frontend-to-static.ps1 -CheckOnly`、`node .\\scripts\\verify-student-jwt-pages.js .\\.runtime-logs\\student-session-polish-fresh.json`、`node .\\scripts\\verify-student-browser-crud.js .\\.runtime-logs\\teacher-session-polish-fresh.json .\\.runtime-logs\\student-session-polish-fresh.json`、`node .\\scripts\\verify-teacher-jwt-pages.js .\\.runtime-logs\\teacher-session-polish-fresh.json`、`node .\\scripts\\verify-teacher-browser-crud.js .\\.runtime-logs\\teacher-session-polish-fresh.json .\\.runtime-logs\\student-session-polish-fresh.json`。 
- 2026-05-24：`student-dashboard.html` 的“最近活动”链已从“activities 能力未接通就直接空列表”收正为真实 fallback。当前 JWT 微服务环境下，`/api/student/activities` 仍通过 capability guard 返回 unsupported 说明，但页面其实已经能稳定读取作业列表和考试列表；原实现却在 `activitiesResponse?.unsupported` 分支中直接 `updateRecentActivities([])`，导致首页明明有作业/考试也常常显示“暂无活动记录”。现已新增 `populateDashboardActivitiesFromConnectedSources(studentAPI, statsData)`，统一用最近作业/考试生成活动流，并在必要时顺手用最近考试列表修正 `statsData.exams`；`getDashboardData()` 在 activities unsupported、综合表现 fallback 以及 connected-sources 直连三条路径上都已切到这条共享 helper，不再因为 activities capability 未接通而把活动区直接清空。`scripts/verify-student-dashboard-contract.js` 已加严，`scripts/verify-student-jwt-pages.js` 也已新增对 `student-dashboard` 禁止出现“暂无活动记录”的运行时检查。已顺序通过 `node .\\scripts\\verify-student-dashboard-contract.js`、`powershell -ExecutionPolicy Bypass -File .\\scripts\\sync-frontend-to-static.ps1`、`powershell -ExecutionPolicy Bypass -File .\\scripts\\sync-frontend-to-static.ps1 -CheckOnly`、`node .\\scripts\\verify-student-jwt-pages.js .\\.runtime-logs\\student-session-polish-fresh.json`、`node .\\scripts\\verify-student-browser-crud.js .\\.runtime-logs\\teacher-session-polish-fresh.json .\\.runtime-logs\\student-session-polish-fresh.json`、`node .\\scripts\\verify-teacher-browser-crud.js .\\.runtime-logs\\teacher-session-polish-fresh.json .\\.runtime-logs\\student-session-polish-fresh.json`。 
- 2026-05-24：学生端 `student-dashboard.html` 的“本周学习时间分布”已接回真实 connected source 数据。当前 JWT 微服务环境下，`/api/student/study-time-distribution` 已能稳定返回真实学习时间数组，但首页 connected-sources 路径此前仍会直接把 `chartData.studyTime` 写成空数组转换结果，导致页面容易把真数据画成空图。现已新增共享 helper `populateDashboardStudyTimeFromConnectedSources(studentAPI, chartData)`，统一服务于 connected-sources 路径、综合表现链中的成功分支以及 fallback 分支，不再在 connected-sources 路径里直接清空学习时间图。新增 `scripts/verify-student-dashboard-runtime.js`，直接校验 `/api/student/study-time-distribution` 返回非零数据且 ECharts 的 `studyTimeChart` 实际渲染出非零 series；配套 `verify-student-dashboard-contract.js`、静态同步检查、student/teacher JWT 页面链与 browser smoke 均已 fresh 通过。
- 2026-05-24：`student-stats.html` 已把两条 JWT 误降级链接回真实能力。其一，知识点区域不再在 JWT 微服务环境下硬编码为“暂未接通”，而是直接调用已真实接通的 `/api/student/knowledge-points`，初始加载、刷新按钮与 student JWT 页面链现在都会命中该接口并渲染真实知识点卡片；其二，学习时长图不再在 JWT 路径下通过 `applyJwtStatsOnlyFallback()` / `changeTimeChartType()` 强制回退为空图，而是改为真实调用 `/api/student/study-time-distribution`，初始加载与“每日/每周”切换都会重新取数并渲染包含非零值的 Chart.js series。配套新增并扩展了 `scripts/verify-student-stats-runtime.js`、收紧了 `scripts/verify-student-jwt-pages.js`，并已顺序通过 `node .\scripts\verify-student-stats-contract.js`、`node .\scripts\verify-student-stats-runtime.js .\.runtime-logs\student-session-polish-fresh.json`、`powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1`、顺序执行的 `powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1 -CheckOnly`、`node .\scripts\verify-student-jwt-pages.js .\.runtime-logs\student-session-polish-fresh.json`、`node .\scripts\verify-student-browser-crud.js .\.runtime-logs\teacher-session-polish-fresh.json .\.runtime-logs\student-session-polish-fresh.json`、`node .\scripts\verify-teacher-jwt-pages.js .\.runtime-logs\teacher-session-polish-fresh.json`、`node .\scripts\verify-teacher-browser-crud.js .\.runtime-logs\teacher-session-polish-fresh.json .\.runtime-logs\student-session-polish-fresh.json`。
- 2026-05-24：`student-stats.html` 已进一步把 JWT 路径上的真实统计链接回后端。继续核实后确认：当前 `/api/student/stats` 与 `/api/student/scores` 在 fresh student JWT 会话下也都稳定返回 `200`，但页面此前仍在 `isJwtStatsOnlyMode()` 分支里主动绕开这两条接口，只保留空统计卡与空成绩图。现已将 JWT 初始加载与“应用筛选”路径改为真实调用 `fetchStudentStats(filters)` 与 `fetchScoreTrend(filters)`，仅保留学习计划仍通过 `generateStudyPlanFromCourses(filters)` 做诚实 fallback。配套扩展了 `scripts/verify-student-stats-runtime.js`，新增对 `/api/student/stats`、`/api/student/scores` 调用以及卡片/成绩图真实渲染的运行时检查，并收紧 `scripts/verify-student-jwt-pages.js` 要求 `student-stats` 页面在 JWT 环境下必须命中 `/api/student/stats`、`/api/student/scores`、`/api/student/study-time-distribution`、`/api/student/knowledge-points`。已顺序通过 `node .\scripts\verify-student-stats-contract.js`、`node .\scripts\verify-student-stats-runtime.js .\.runtime-logs\student-session-polish-fresh.json`、`powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1`、顺序执行的 `powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1 -CheckOnly`、`node .\scripts\verify-student-jwt-pages.js .\.runtime-logs\student-session-polish-fresh.json`、`node .\scripts\verify-student-browser-crud.js .\.runtime-logs\teacher-session-polish-fresh.json .\.runtime-logs\student-session-polish-fresh.json`、`node .\scripts\verify-teacher-jwt-pages.js .\.runtime-logs\teacher-session-polish-fresh.json`、`node .\scripts\verify-teacher-browser-crud.js .\.runtime-logs\teacher-session-polish-fresh.json .\.runtime-logs\student-session-polish-fresh.json`。
- 2026-05-24：`student-stats.html` 已进一步把课程筛选在 JWT 环境下接回真实课程链。继续核实后确认：student 微服务 capability 中 `courses` 已为 `true`，`/api/student/courses` 在 fresh student JWT 会话下也稳定返回真实课程列表，且 `/api/student/stats`、`/api/student/scores`、`/api/student/study-time-distribution`、`/api/student/knowledge-points` 全部支持 `courseId` 过滤；但页面此前在 `loadCourses()` 里遇到 `isJwtStatsOnlyMode()` 会直接 `return`，导致 JWT 学习数据页课程下拉永远只有“全部课程”，筛选 UI 与底层真实过滤能力脱节。现已移除这层前端自锁，让 `student-stats` 在 JWT 环境下也真实拉取学生课程列表，并通过课程下拉把 `courseId` 一并传到底层四条已接通接口。配套扩展 `scripts/verify-student-stats-runtime.js`，新增“初始加载命中 `/api/student/courses` 并填充课程选项”“选择具体课程后，`/api/student/stats`、`/api/student/scores`、`/api/student/study-time-distribution`、`/api/student/knowledge-points` 都会携带 `courseId` 重新取数”的运行时检查，并收紧 `scripts/verify-student-jwt-pages.js` 要求 `student-stats` 在 JWT 页面链中也必须命中 `/api/student/courses`。已顺序通过 `node .\scripts\verify-student-stats-contract.js`、`node .\scripts\verify-student-stats-runtime.js .\.runtime-logs\student-session-polish-fresh.json`、`powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1`、`powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1 -CheckOnly`、`node .\scripts\verify-student-jwt-pages.js .\.runtime-logs\student-session-polish-fresh.json`、`node .\scripts\verify-student-browser-crud.js .\.runtime-logs\teacher-session-polish-fresh.json .\.runtime-logs\student-session-polish-fresh.json`、`node .\scripts\verify-teacher-jwt-pages.js .\.runtime-logs\teacher-session-polish-fresh.json`、`node .\scripts\verify-teacher-browser-crud.js .\.runtime-logs\teacher-session-polish-fresh.json .\.runtime-logs\student-session-polish-fresh.json`。
- 2026-05-24：`student-settings.html` 头像上传链已从“前端走错协议 + JWT capability 本地短路”收正为真实可用能力。继续追查后确认：后端 `/api/student/upload-avatar` 实际已稳定支持 JSON `{ avatar: dataUrl }`，但学生前端此前仍把它当成 `multipart/form-data` 文件上传来调，且 JWT 运行时 `/api/frontend/capabilities` 还把 `student.avatarUpload` 下发为 `false`，导致页面在本地 guard 中被短路成“暂未提供头像上传接口”。现已完成三层修正：1）`frontend/dist/api.js` 与 `student-settings.html` 已统一改为 data URL -> JSON 提交链，并移除旧 multipart/unsupported 分支；2）`gateway` 的 `FrontendCapabilityEdgeController` 与 `major_assignment` 的 `FrontendCapabilityController` 均已把 `avatarUpload` 收正为 `true`，避免 JWT 页面被旧 capability 错误短路；3）新增 `scripts/verify-student-settings-avatar-contract.js` 与 `scripts/verify-student-settings-avatar-runtime.js`，其中 runtime verifier 已改用 Playwright 原生 `setInputFiles` 和真实 `#messageContainer .message-text` 成功提示检查，直接验证 `/api/student/upload-avatar` 返回 `200` 且页面不再展示“当前 JWT 微服务环境暂未提供头像上传接口”。本轮已顺序通过 `mvn --% -pl major_assignment -Dtest=FrontendCapabilityControllerTest test`、`mvn --% -pl gateway -am -Dtest=FrontendCapabilityEdgeControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`、`mvn --% -pl gateway -am -DskipTests package`、`powershell -ExecutionPolicy Bypass -File .\scripts\start-runtime-smoke-stack.ps1`、`node .\scripts\verify-student-settings-avatar-contract.js`、`node .\scripts\verify-student-settings-avatar-runtime.js .\.runtime-logs\student-session-polish-fresh.json`、`node .\scripts\verify-student-jwt-pages.js .\.runtime-logs\student-session-polish-fresh.json`、`powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1`、`powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1 -CheckOnly`、`node .\scripts\verify-student-browser-crud.js .\.runtime-logs\teacher-session-polish-fresh.json .\.runtime-logs\student-session-polish-fresh.json`、`node .\scripts\verify-teacher-jwt-pages.js .\.runtime-logs\teacher-session-polish-fresh.json`、`node .\scripts\verify-teacher-browser-crud.js .\.runtime-logs\teacher-session-polish-fresh.json .\.runtime-logs\student-session-polish-fresh.json`。
- 2026-05-24：继续把 `student-settings.html` 头像上传的 runtime 证据链钉实。进一步排查确认，这条链前面真正的短路点不在 FileReader，而在 JWT 运行时 `/api/frontend/capabilities` 仍把 `student.avatarUpload` 下发成 `false`，导致页面在 `guardStudentCapability('avatarUpload', ...)` 里直接本地拦截。现已补齐 gateway 侧测试期望与实际运行构件，重启 smoke stack 后确认 `/api/frontend/capabilities` 已真实返回 `avatarUpload: true`；同时页面上传入口已收成“文件选择 -> FileReader -> uploadAvatar(avatarDataUrl)”的单一路径，不再在 `uploadAvatar()` 内重复读文件。专项 contract verifier、runtime verifier、student JWT 页面链、student browser CRUD smoke、teacher JWT 页面链与 teacher browser CRUD smoke 已 fresh 通过。
- 2026-05-24：`student-settings.html` 的通知设置与隐私设置已从“看起来像服务器持久化成功”收正为“当前浏览器本地优先”的诚实语义。继续横向审计后确认：user-service 当前 `getNotificationSettings/getPrivacySettings` 仍返回固定默认值，`updateNotificationSettings/updatePrivacySettings` 仅回 `true` 占位；但页面此前会先把设置写入 localStorage，再调用这两条占位 API，并在成功时提示“通知设置保存成功 / 隐私设置保存成功”，刷新后又会被服务端默认值覆盖，形成“像真持久化、实际没持久化”的错位。现已为通知/隐私 section 增加“当前环境下…以本浏览器保存为准”的静态说明，保存动作仅写入当前用户 localStorage 并提示“已保存在当前浏览器”，加载动作也只读取本地偏好，不再调用占位的通知/隐私设置 API。新增 `scripts/verify-student-settings-preferences-contract.js` 与 `scripts/verify-student-settings-preferences-runtime.js` 两层护栏，并同步更新 `scripts/verify-student-jwt-pages.js` 对 `student-settings` 的页面期望。已顺序通过 `node .\scripts\verify-student-settings-preferences-contract.js`、`powershell -ExecutionPolicy Bypass -File .\scripts\get-dev-auth-session.ps1 -Role student -OutFile .\.runtime-logs\student-session-polish-fresh.json`、`powershell -ExecutionPolicy Bypass -File .\scripts\get-dev-auth-session.ps1 -Role teacher -OutFile .\.runtime-logs\teacher-session-polish-fresh.json`、`node .\scripts\verify-student-settings-preferences-runtime.js .\.runtime-logs\student-session-polish-fresh.json`、`node .\scripts\verify-student-jwt-pages.js .\.runtime-logs\student-session-polish-fresh.json`、`node .\scripts\verify-student-browser-crud.js .\.runtime-logs\teacher-session-polish-fresh.json .\.runtime-logs\student-session-polish-fresh.json`、`node .\scripts\verify-teacher-jwt-pages.js .\.runtime-logs\teacher-session-polish-fresh.json`、`node .\scripts\verify-teacher-browser-crud.js .\.runtime-logs\teacher-session-polish-fresh.json .\.runtime-logs\student-session-polish-fresh.json` 以及 `powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1`。补充复核：本轮 `sync-frontend-to-static.ps1 -CheckOnly` 对 `student-settings.html` 的 `[DIFF]` 经 SHA256 证明仍是把 `sync` 与 `checkOnly` 并行执行造成的竞态误报，源文件与静态副本内容一致。
- 2026-05-24：`student-settings.html` 的个人资料保存语义已按后端真实能力范围收正。继续审计确认：`/api/student/profile` 当前只稳定持久化 `姓名 / 邮箱 / 手机号 / 头像`，页面此前却会把 `专业 / 年级 / 班级` 一并提交并统一提示“个人信息保存成功”。现已拆成三层：姓名/邮箱/手机号继续走真实账号资料保存；专业/年级改为当前浏览器补充信息，通过 `studentAcademicProfile:{userId}` 本地持久化；班级改成只读展示，并明确说明其来自系统班级关系、当前页不可手动修改。新增 `scripts/verify-student-settings-profile-contract.js` 与 `scripts/verify-student-settings-profile-runtime.js`，runtime verifier 会校验 `/api/student/profile` 请求体不再包含 `major / grade / className`，并验证重载后姓名通过真实接口保留、专业/年级通过本地补充信息保留、班级保持服务端派生值不变。配套 `scripts/verify-student-jwt-pages.js`、静态同步检查、student/teacher JWT 页面链与 student/teacher browser CRUD smoke 均已 fresh 通过；本轮 `sync-frontend-to-static.ps1 -CheckOnly` 若与 `sync` 并行执行会对 `student-settings.html` 报 `[DIFF]`，顺序重跑即可恢复绿色。
- 2026-05-24：`teacher-student-dashboard.html` 的成绩趋势图已移除“默认占位曲线”兜底。此前页面在 `/api/teacher/score-trend` 返回空数组或抛错时，会退回一组看起来很真的周/月/三个月/学期默认分数曲线，并提示“已显示占位数据”，这会把无数据/失败态误包装成真实趋势。现已将这条链收正为诚实空态：空结果返回 `当前筛选下暂无成绩趋势数据`，接口失败返回 `成绩趋势数据暂不可用，请稍后重试`，并通过 `clearScoreTrendEmptyState(...)`、`renderScoreTrendEmptyState(...)`、`buildScoreTrendEmptyChart(...)` 统一渲染空趋势图，不再伪造默认分数序列。新增 `scripts/verify-teacher-student-dashboard-runtime.js`，直接模拟空结果与异常两条路径，校验页面显示真实空态/不可用态且数据集不再包含伪造分数。配套 `scripts/verify-teacher-student-dashboard-contract.js`、静态同步检查、teacher JWT 页面链与 teacher browser CRUD smoke 均已 fresh 通过。
- 2026-05-24：`student-notifications.html` 已从“操作后靠当前 DOM 自己猜状态”收正为“操作后回到服务器真状态”。此前这页在标记已读、全部已读、删除通知、清空已读后，会直接在当前 DOM 上加 `read` 类、删按钮、算 `.notification-item:not(.read)` 数量，甚至在“未读”过滤视图下继续沿用当前 DOM 快照；这在分页和过滤切换下容易出现已读数、列表内容和服务端实际状态错位。现已新增 `notificationListState`、`getActiveNotificationState()`、`refreshUnreadNotificationCount()`、`refreshNotificationListView()`，四类变更操作都会在成功后重新请求未读数量和当前筛选列表，不再靠当前 DOM 推断服务端状态。新增 `scripts/verify-student-notifications-contract.js` 与 `scripts/verify-student-notifications-runtime.js` 两层护栏，runtime verifier 会模拟“未读过滤 -> 标记已读”，要求页面重新请求 unread-count 和当前 unread 列表，并最终显示真实空态“暂无通知”。配套 `sync-frontend-to-static.ps1`、顺序执行的 `-CheckOnly`、student JWT 页面链与 student browser CRUD smoke 均已 fresh 通过。
- 2026-05-24 继续 student-settings 收口：确认 `/api/student/notification-settings` 与 `/api/student/privacy-settings` 在 user-service 仍是占位实现（GET 固定默认值、PUT 仅返回 success），因此将 gateway/monolith/frontend 默认 capability 的 `notificationSettings` 与 `privacySettings` 从 `true` 收正为 `false`，与页面“当前环境下以本浏览器保存为准”的真实语义对齐；同步更新 capability contract 与架构对齐测试，避免系统继续把占位接口包装成已接通账号级能力。验证通过：`node .\\scripts\\verify-student-settings-preferences-contract.js`、`node .\\scripts\\verify-student-settings-preferences-runtime.js .\\.runtime-logs\\student-session-polish-fresh.json`、`node .\\scripts\\verify-student-jwt-pages.js .\\.runtime-logs\\student-session-polish-fresh.json`、`node .\\scripts\\verify-student-browser-crud.js .\\.runtime-logs\\teacher-session-polish-fresh.json .\\.runtime-logs\\student-session-polish-fresh.json`、`mvn --% -pl major_assignment -Dtest=FrontendCapabilityControllerTest,StudentSettingsCapabilityAlignmentTest test`、`mvn --% -pl gateway -Dtest=FrontendCapabilityEdgeControllerTest -DargLine="-Djdk.attach.allowAttachSelf=true" test`、`powershell -ExecutionPolicy Bypass -File .\\scripts\\sync-frontend-to-static.ps1 -CheckOnly`。
- 2026-05-24：`teacher-courses` 的班级 metadata 链已接回真实专业数据并补齐初始化时序。继续审计确认：`/api/teacher/majors` 在 course-service 的 `TeacherCourseAdminController` 已真实接通，但 `frontend/dist/teacher-courses-metadata.js` 之前既保留硬编码五专业名单，又让 `loadGradeAndMajorData()` 这条本该负责真实 metadata 的链几乎不参与页面启动流程，导致搜索区与“新增班级/编辑班级”里的专业下拉在很多路径下要么靠后续班级搜索顺手补齐，要么直接退回固定五专业。现已新增 `loadTeacherMajors()`、`teacherMajorMetadata`、`ensureClassMetadataLoaded()`，页面启动、切到班级标签、打开新增/编辑班级弹窗时都会优先拉取真实 `/api/teacher/majors` 并填充专业下拉；班级数据继续负责补充年级与班级上已有专业，但接口失败时不再回退硬编码专业目录。新增 `scripts/verify-teacher-courses-metadata-runtime.js`，直接验证页面会真实命中 `/api/teacher/majors` 且新增班级弹窗中的专业/年级下拉已完成填充。已顺序通过 `node .\\scripts\\verify-teacher-courses-metadata-contract.js`、`node .\\scripts\\verify-teacher-courses-metadata-runtime.js .\\.runtime-logs\\teacher-session-polish-fresh.json`、`powershell -ExecutionPolicy Bypass -File .\\scripts\\sync-frontend-to-static.ps1`、`powershell -ExecutionPolicy Bypass -File .\\scripts\\sync-frontend-to-static.ps1 -CheckOnly`、`node .\\scripts\\verify-teacher-jwt-pages.js .\\.runtime-logs\\teacher-session-polish-fresh.json`、`node .\\scripts\\verify-teacher-browser-crud.js .\\.runtime-logs\\teacher-session-polish-fresh.json .\\.runtime-logs\\student-session-polish-fresh.json`。
- 2026-05-24：`teacher-courses-assignments.js` 已移除过时的“当前联调环境的课程分配列表暂不可用，不影响课程新增与课程列表使用。”降级分支。继续核实后确认，课程分配 CRUD 现在在 runtime 中已真实接通并由 teacher browser CRUD smoke 覆盖，这段旧话术只会把真实错误态伪装成“联调环境不可用”的空结果。现已统一改为失败时走 `assignments-error` 真实错误态，并新增 `scripts/verify-teacher-courses-assignments-contract.js` 与 `scripts/verify-teacher-courses-assignments-runtime.js` 两层护栏；配套顺序通过 `node .\\scripts\\verify-teacher-courses-assignments-contract.js`、`node .\\scripts\\verify-teacher-courses-assignments-runtime.js .\\.runtime-logs\\teacher-session-polish-fresh.json`、`powershell -ExecutionPolicy Bypass -File .\\scripts\\sync-frontend-to-static.ps1 -CheckOnly`、`node .\\scripts\\verify-teacher-jwt-pages.js .\\.runtime-logs\\teacher-session-polish-fresh.json`、`node .\\scripts\\verify-teacher-browser-crud.js .\\.runtime-logs\\teacher-session-polish-fresh.json .\\.runtime-logs\\student-session-polish-fresh.json`。
- 2026-05-24：`teacher-assignments-shell.js` 已移除启动时先灌 `mockCourses` 再等待真实课程接口覆盖的旧初始化壳。继续核实后确认，教师作业与考试页面当前已稳定通过 `/api/teacher/courses` 拉取真实课程列表，且 teacher JWT 页面链早已要求这页命中真实课程接口；因此启动链中先 `populateAllCourseDropdowns(mockCourses)`、再 `loadTeacherAssignmentCourses()` 的做法会让课程筛选和发布弹窗短暂展示假课程选项。现已删除 `mockCourses` 预灌路径，改为在 `bootstrapTeacherAssignmentsPage()` 中直接 `await loadTeacherAssignmentCourses()` 后再进入首个 tab 加载。新增 `scripts/verify-teacher-assignments-shell-contract.js` 与 `scripts/verify-teacher-assignments-runtime.js` 两层护栏，分别约束源码中不得回流 `mockCourses` 初始化，以及页面运行时必须通过真实 `/api/teacher/courses` 填充课程下拉。已顺序通过 `node .\\scripts\\verify-teacher-assignments-shell-contract.js`、`node .\\scripts\\verify-teacher-assignments-runtime.js .\\.runtime-logs\\teacher-session-polish-fresh.json`、`powershell -ExecutionPolicy Bypass -File .\\scripts\\sync-frontend-to-static.ps1`、顺序执行的 `powershell -ExecutionPolicy Bypass -File .\\scripts\\sync-frontend-to-static.ps1 -CheckOnly`、`node .\\scripts\\verify-teacher-jwt-pages.js .\\.runtime-logs\\teacher-session-polish-fresh.json`、`node .\\scripts\\verify-teacher-browser-crud.js .\\.runtime-logs\\teacher-session-polish-fresh.json .\\.runtime-logs\\student-session-polish-fresh.json`。
- 2026-05-24：`teacher-courses` 的课程类别/课程状态枚举链已收正为兼容历史中文值与当前代码值。继续核实后确认：课程 runtime 数据当前混存 `practice/active` 与 `必修/进行中` 两套值，而 course-service 的 teacher/student 列表筛选仍按字符串精确匹配；页面下拉此前只提供中文值，导致搜索无法覆盖代码值课程，编辑代码值课程时也容易因 select 中不存在对应 option 而回填丢值。现已在 `teacher-courses-shell.js` 中引入统一 `COURSE_CATEGORY_OPTIONS` / `COURSE_STATUS_OPTIONS`，以“代码值 + 中文标签”填充下拉；在 `teacher-courses-courses.js` 中新增历史别名兼容搜索，让 `practice/实践课`、`active/进行中` 这类历史值都能被同一页面筛选覆盖；在 `teacher-courses-course-crud.js` 中新增 `ensureSelectHasOption(...)` 兜底，保证编辑代码值课程时 select 不会掉空。新增 `scripts/verify-teacher-courses-enum-contract.js` 与 `scripts/verify-teacher-courses-enum-runtime.js` 两层护栏，并已顺序通过 contract、runtime、前端静态同步、teacher JWT 页面链与 teacher browser CRUD smoke 验证。
- 2026-05-24：`student-courses.html` 的课程类别/课程状态筛选链已收正为兼容当前代码值与历史中文值。继续核实后确认：当前 `/api/student/courses` 在 fresh student JWT 会话下已真实返回 `courseCategory: practice`、`courseStatus: active` 这类代码值课程，但页面筛选下拉仍硬编码中文值，并直接把 `courseCategory/courseStatus` 透传给接口，导致代码值课程无法用页面筛选稳定命中。现已将这页筛选下拉收成统一“代码值 value + 中文 label”，并在前端请求链中加入历史别名兼容，让 `practice/实践课`、`active/进行中` 等历史值都能被同一筛选覆盖，同时移除了 HTML 中旧的中文静态 option 壳。新增 `scripts/verify-student-courses-contract.js` 与 `scripts/verify-student-courses-runtime.js` 两层护栏，并已顺序通过 contract、runtime、前端静态同步、student JWT 页面链与 student browser CRUD smoke 验证。
- 2026-05-24：`teacher-settings.html` 的通知偏好链已从“纯本地浏览器保存”收正为“真实会话级同步 + 浏览器副本”的诚实语义。继续核实后确认：`PUT /api/auth/notification-settings` 当前在兼容后端中已真实可用，但返回 `data.mode=compatibility-placeholder`、`data.persisted=false`，说明它只代表当前登录会话可读写，并非账号级持久化。页面此前仍只写 `localStorage`，并把文案描述成“会先保存在当前浏览器”。现已改为真实调用 `/api/auth/notification-settings` 同步 checkbox 状态，再保留 `teacherNotificationSettings` 本地副本；页面说明与成功提示统一收成“已同步到当前登录会话，并保留当前浏览器副本；当前环境下尚未接通账号级持久化”。配套扩展 `scripts/verify-teacher-settings-contract.js` 与 `scripts/verify-teacher-settings-runtime.js`，分别钉住源码中必须命中真实接口且不得回到“纯本地”文案，以及浏览器运行时必须发出 `/api/auth/notification-settings` 请求并展示新的会话级提示。已顺序通过 `node .\scripts\verify-teacher-settings-contract.js`、`node .\scripts\verify-teacher-settings-runtime.js .\.runtime-logs\teacher-session-polish-fresh.json`、`powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1`、顺序执行 `powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1 -CheckOnly`、`node .\scripts\verify-teacher-jwt-pages.js .\.runtime-logs\teacher-session-polish-fresh.json`、`node .\scripts\verify-teacher-browser-crud.js .\.runtime-logs\teacher-session-polish-fresh.json .\.runtime-logs\student-session-polish-fresh.json`。
- 2026-05-24：`student-assignments.html` 的考试/成绩 tab 已正式纳入专项 runtime 与 student JWT 页面链回归。继续核实后确认：这页当前 `/api/student/exams`、`/api/student/scores` 在 fresh student JWT 会话下都已真实可用，但系统级 `scripts/verify-student-jwt-pages.js` 之前只检查默认首屏作业 tab，没有覆盖到考试/成绩两条懒加载链；页面底部注释也还保留“避免未切通的考试/成绩链路制造噪音”的旧阶段表述。现已新增 `scripts/verify-student-assignments-contract.js` 与 `scripts/verify-student-assignments-runtime.js`，并扩展 `scripts/verify-student-jwt-pages.js` 为主动点开考试、成绩 tab 后再验证 `/api/student/exams`、`/api/student/scores` 的真实命中与渲染结果；页面注释同步收正为“首屏先加载作业，考试和成绩按需取数”。相关 contract、runtime、student JWT 页面链、student browser CRUD smoke、teacher JWT 页面链与 teacher browser CRUD smoke 已顺序 fresh 通过。
- 2026-05-25：`frontend/dist/api.js` 中 student 侧考试/成绩能力的旧 unsupported 文案已与当前 runtime 重新对齐。继续核实后确认：fresh student JWT 会话下 `/api/frontend/capabilities` 已稳定返回 `exams / examDetail / examSubmit / scores = true`，且 `/api/student/exams`、`/api/student/scores` 已通过专项 runtime 与 student JWT 页面链；但 `api.js` 里仍保留“当前 JWT 微服务环境暂未提供考试列表/考试详情/成绩查询/考试提交接口”的旧提示及 fallback 参数。现已仅保留当前仍真实 unsupported 的 `dashboardPerformance` 与 `activities` 口径，并移除 `getExams/getExamDetail/getScores/getExamSubmissions/submitExam/submitExamJson` 中过时的考试/成绩 fallback 提示参数；新增 `scripts/verify-student-api-capability-messages-contract.js` 防止旧文案回流。相关 contract、student-assignments runtime、student JWT 页面链、student browser CRUD smoke、teacher JWT 页面链、顺序执行的静态同步与 `-CheckOnly` 已顺序 fresh 通过。
- 2026-05-25：`teacher-courses` 中残留的 mock 语义壳已进一步收正。继续核实后确认：`teacher-courses-metadata.js` 里的 `useMockCourseData()` 实际不再回填任何伪课程，而只是课程接口失败时的错误态展示；`teacher-courses-shell.js` 里的 `mockMethods` 也只是本地考核方式枚举，并不对应某条后端 metadata 接口。现已分别收正为 `showCourseLoadFailureState()` 与 `localAssessmentMethods`，并同步调整日志语义，避免后续维护者把“本地枚举/错误态 helper”继续误读成“仍在用 mock 数据”。配套扩展 `scripts/verify-teacher-courses-metadata-contract.js` 与 `scripts/verify-teacher-courses-shell-contract.js`，并已顺序通过 contract、metadata runtime verifier 与 teacher browser CRUD smoke。
- 2026-05-25：`teacher-student-dashboard.html` 的学生表格 access-denied fallback 已按真实运行时语义收正。此前 `/api/teacher/learning-summary` 返回 `401/403` 时，页面会把登录失效或权限不足统一包装成“当前环境未开放该页面所需数据，已显示空结果”，属于典型的旧联调空态话术回流。现已补入 `getStudentTableFallbackMessage(error)`，将 `401` 分支改为“登录已过期，正在跳转到登录页”，将 `403` 分支改为“当前账号暂无权限查看学生学习数据”，并保持其他失败分支继续使用通用加载失败提示。配套扩展 `scripts/verify-teacher-student-dashboard-contract.js` 与 `scripts/verify-teacher-student-dashboard-runtime.js`，相关 contract、静态同步检查与 targeted runtime verifier 已顺序通过，`403` 场景下不再允许回流“已显示空结果”旧文案。
- 2026-05-25：`student-settings / teacher-settings` 的最终对齐复核已完成。继续核实后确认：student 设置页当前“通知/隐私本地优先、头像真实上传”的 capability 与页面说明已经一致，无需再扩接口语义；teacher 设置页则仍应保持 `notificationPersistence=false`，因为当前 `/api/auth/notification-settings` 仅代表会话级同步且返回 `persisted=false`。页面此前虽然主说明已是“当前登录会话 + 当前浏览器副本”，但仍保留一条“通知偏好已同步到当前账号”的备用成功分支，口径比真实能力更满。现已将其收正为始终使用“当前登录会话，并保留当前浏览器副本”的成功语义，并顺序通过 `verify-student-settings-preferences-contract.js`、`verify-teacher-settings-contract.js`、`verify-student-settings-preferences-runtime.js .\.runtime-logs\student-session-polish-fresh.json`、`verify-teacher-settings-runtime.js .\.runtime-logs\teacher-session-polish-fresh.json`、`sync-frontend-to-static.ps1` 与 `-CheckOnly`。

- 2026-05-24：`teacher-assignments-exam-publish.js` 的课程知识点下拉已从旧裸 `fetch` + 伪“环境未提供接口”降级链收正为真实 teacher API 取数。继续核实后确认：`GET /api/teacher/knowledge-points/course/{courseId}` 在 fresh teacher JWT 会话下已稳定返回 `200` 且可用于作业/考试发布弹窗；但页面此前仍在 `loadKnowledgePoints(...)` 中直接裸调 `fetch(...)`，并把 `401/403` 一律渲染成“当前环境暂未提供知识点接口”，会把真实鉴权或请求失败误包装成旧联调降级。现已为 `TeacherAPI` 新增 `getKnowledgePointsByCourse(courseId)`，并让 `teacher-assignments-exam-publish.js` 统一改走这条 API helper；接口失败时改为诚实的“知识点暂不可用，请稍后重试 / 加载知识点失败，请稍后重试”，不再回流旧环境文案。配套新增 `scripts/verify-teacher-assignments-exam-publish-contract.js` 与 `scripts/verify-teacher-assignments-knowledge-points-runtime.js` 两层护栏，runtime verifier 会直接在 `teacher-assignments.html` 中选取一个真实存在知识点的课程，并验证作业/考试发布弹窗的知识点下拉真实命中 `/api/teacher/knowledge-points/course/{courseId}` 且完成填充。相关 contract、runtime、静态同步、teacher JWT 页面链与 teacher browser CRUD smoke 已顺序 fresh 通过。

- 2026-05-25：完成本轮剩余问题复盘，当前 backlog 状态已切换为“待命令执行”，不再默认继续实现。Task 1 至 Task 4 已全部完成；当前尚未处理的事项仅保留为文档级清理建议：一是压缩历史进展中的重复记录；二是清理少量遗留乱码控制字符；三是如需继续，只按新的明确单项命令推进。最后一轮 fresh 通过的收口 verifier 清单为：`verify-student-assignments-contract.js`、`verify-student-assignments-runtime.js`、`verify-student-api-capability-messages-contract.js`、`verify-teacher-student-dashboard-contract.js`、`verify-teacher-student-dashboard-runtime.js`、`verify-student-settings-preferences-contract.js`、`verify-student-settings-preferences-runtime.js`、`verify-teacher-settings-contract.js`、`verify-teacher-settings-runtime.js`、`sync-frontend-to-static.ps1` 与 `sync-frontend-to-static.ps1 -CheckOnly`。
