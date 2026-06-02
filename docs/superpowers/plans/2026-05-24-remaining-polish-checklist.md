# Remaining Polish Checklist Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 收口当前 majorassignment 仓库中剩余的页面级真口径问题，避免继续无边界扩展，并把后续工作拆成可单独下达的清单。

**Architecture:** 继续沿“先核实 runtime 真相，再补 contract/runtime verifier，最后做最小修复”的路径推进，但不再并行扩多条线。剩余工作以页面为单位切分，每次只处理一个页面或一类语义错位，做完即停，避免再次出现目标不断外扩。

**Tech Stack:** Spring Boot / gateway / course-service / user-service / static HTML / Playwright runtime verifiers / PowerShell sync scripts

---

### Task 1: 收正 Student API 中已经过时的考试/成绩能力降级文案

**Files:**
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\api.js`
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\major_assignment\src\main\resources\static\api.js`
- Test: `D:\111\Distributed framework technology\JavaCode\majorassignment\scripts\verify-student-jwt-pages.js`
- Test: `D:\111\Distributed framework technology\JavaCode\majorassignment\scripts\verify-student-assignments-runtime.js`

- [ ] 核实 `/api/frontend/capabilities` 在 fresh student JWT 下对 `exams / examDetail / examSubmit / scores` 的真实返回值。
- [ ] 清理 `frontend/dist/api.js` 中仍写着“暂未提供考试列表/考试详情/成绩查询/考试提交接口”的旧 capability message，只保留当前仍真实 unsupported 的能力。
- [ ] 如有必要，调整 `guardStudentCapability(...)` 的 fallback 文案，避免后续页面继续弹出与 runtime 不符的旧提示。
- [ ] 运行：
  - `node .\scripts\verify-student-assignments-runtime.js .\.runtime-logs\student-session-polish-fresh.json`
  - `node .\scripts\verify-student-jwt-pages.js .\.runtime-logs\student-session-polish-fresh.json`
  - `powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1`
  - `powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1 -CheckOnly`

### Task 2: 审计 teacher-courses 中仍残留的 mock metadata 壳

**Files:**
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-courses-metadata.js`
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-courses-shell.js`
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\major_assignment\src\main\resources\static\teacher-courses-metadata.js`
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\major_assignment\src\main\resources\static\teacher-courses-shell.js`
- Test: `D:\111\Distributed framework technology\JavaCode\majorassignment\scripts\verify-teacher-courses-metadata-runtime.js`
- Test: `D:\111\Distributed framework technology\JavaCode\majorassignment\scripts\verify-teacher-browser-crud.js`

- [ ] 核实 `useMockCourseData()`、`mockMethods` 是否仍为真实必须保留的本地兜底，还是已经可以接回某条真实 metadata 源。
- [ ] 如果真实接口已存在：移除或降级这些 mock 初始化壳，并补 verifier。
- [ ] 如果真实接口不存在：保留本地兜底，但把命名/注释/提示语收成“本地枚举”而不是“模拟真实数据”。
- [ ] 运行：
  - `node .\scripts\verify-teacher-courses-metadata-runtime.js .\.runtime-logs\teacher-session-polish-fresh.json`
  - `node .\scripts\verify-teacher-browser-crud.js .\.runtime-logs\teacher-session-polish-fresh.json .\.runtime-logs\student-session-polish-fresh.json`

### Task 3: 处理 teacher-student-dashboard 仍偏旧的“环境未开放数据”提示

**Files:**
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-student-dashboard.html`
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\major_assignment\src\main\resources\static\teacher-student-dashboard.html`
- Test: `D:\111\Distributed framework technology\JavaCode\majorassignment\scripts\verify-teacher-student-dashboard-contract.js`
- Test: `D:\111\Distributed framework technology\JavaCode\majorassignment\scripts\verify-teacher-student-dashboard-runtime.js`

- [ ] 核实当前“当前环境未开放该页面所需数据，已显示空结果”到底对应哪条真实失败路径。
- [ ] 如果只是旧联调遗留话术，改成更具体的“当前筛选下暂无数据”或“数据暂不可用，请稍后重试”。
- [ ] 确认这条提示不会把真实错误态和空结果态混在一起。
- [ ] 运行：
  - `node .\scripts\verify-teacher-student-dashboard-contract.js`
  - `node .\scripts\verify-teacher-student-dashboard-runtime.js .\.runtime-logs\teacher-session-polish-fresh.json`

### Task 4: 横向复核 student-settings / teacher-settings 的 capability 与页面说明是否完全对齐

**Files:**
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\student-settings.html`
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-settings.html`
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\api.js`
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\gateway\src\main\java\com\_202510007517\platform\gateway\controller\FrontendCapabilityEdgeController.java`
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\major_assignment\src\main\java\com\_202510007517\major_assignment\controller\FrontendCapabilityController.java`
- Test: `D:\111\Distributed framework technology\JavaCode\majorassignment\scripts\verify-student-settings-preferences-runtime.js`
- Test: `D:\111\Distributed framework technology\JavaCode\majorassignment\scripts\verify-teacher-settings-runtime.js`

- [ ] 再次核实 `/api/frontend/capabilities` 与页面说明是否完全一致，尤其是：
  - student `notificationSettings / privacySettings / avatarUpload`
  - teacher 通知偏好会话同步能力
- [ ] 若 capability 或文案仍有出入，只修正一侧并补对齐测试，不再同时扩别的页面。
- [ ] 运行：
  - `node .\scripts\verify-student-settings-preferences-runtime.js .\.runtime-logs\student-session-polish-fresh.json`
  - `node .\scripts\verify-teacher-settings-runtime.js .\.runtime-logs\teacher-session-polish-fresh.json`

### Task 5: 做一次“仅剩问题清单”复盘，不再新增实现

**Files:**
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\docs\final-delivery.md`
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\docs\README.md`
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\docs\next-round-optimization-backlog.md`

- [ ] 汇总前四个任务完成后仍未处理的页面级问题，只写“问题、位置、建议下一步”，不继续实现。
- [ ] 删除所有“继续目标”“下一刀继续做”式文案，改成“待命令执行”的清单表述。
- [ ] 记录最后一轮实际通过的 verifier 清单。

---

## 建议执行顺序

1. Task 1：`student-assignments / api.js` 旧能力降级文案  
2. Task 2：`teacher-courses` metadata/mock 壳  
3. Task 3：`teacher-student-dashboard` 空态/错误态旧话术  
4. Task 4：settings capability 与说明对齐复核  
5. Task 5：只做文档复盘，停止实现

## 预计剩余时间

- Task 1：30-45 分钟
- Task 2：45-90 分钟
- Task 3：30-60 分钟
- Task 4：30-45 分钟
- Task 5：20-30 分钟

**合计：约 2.5 到 4.5 小时**

## 当前已完成、无需再做的块

- `student-dashboard` 最近活动与学习时间图真实 fallback
- `student-stats` JWT 路径下课程/统计/成绩/知识点/学习时间链
- `student-settings` 头像、资料、导出、通知偏好/隐私
- `student-notifications` 写后回读
- `teacher-ai-tools` 与 `student-ai-assistant` 真能力/诚实降级
- `teacher-warning` 伪 delta、假图表、重复取数、占位详情
- `teacher-knowledge` 详情假数据、初始化链、学生筛选链
- `teacher-dashboard` / `teacher-student-dashboard` 大部分统计壳与重复控制流
- `teacher-courses` 新增学生入班 runtime、metadata 初始化、枚举兼容
- `teacher-assignments` mockCourses 壳、知识点下拉真实 teacher API
- `student-assignments` 考试/成绩 tab runtime 与 JWT 页面链验证

## 停止规则

- 完成当前被点名的单个 Task 后立即停下并回报，不主动进入下一个 Task。
- 如果某个 Task 过程中发现新问题，只记录到文档，不顺手扩做。
- 所有后续动作以用户明确点名的 Task 编号为准。
