# 反馈层统一设计（学生列表页切片）

日期：2026-06-29

## 目标

先在学生列表页落地统一反馈层，覆盖：

- 页面级加载态
- 列表级空态
- 列表级错误态
- 操作成功/失败轻提示

目标页面：

- `student-courses.html`
- `student-assignments.html`

## 方案

在 `common-ui.js` 中补充统一反馈接口，而不是为学生页单独再写一套：

- `setElementLoadingState(container, options)`
- `renderEmptyState(container, options)`
- `renderErrorState(container, options)`
- `clearState(container)`

其中：

- `loading` 用于列表区域或分页区域的局部取数
- `empty` 表示请求成功但没有内容
- `error` 表示请求失败，并允许显示“重试”动作

## 页面接入策略

### student-courses

- 保留原有页面级 `loadingOverlay`
- 课程网格 `#coursesGrid` 接统一 empty/error/loading
- 分页区在 empty/error 时清空

### student-assignments

- 作业列表 `#assignment-list`
- 考试列表 `#exam-list`
- 成绩表 `#score-list`

统一改为：

- 无数据时使用一致的空态块
- 请求失败时使用一致的错误态块
- 顶部零散 `alert` 保留给轻提示，不再承担主体状态反馈

## 验证

- 静态搜索确认学生页不再手写重复 empty/error DOM
- 浏览器脚本验证学生课程页和作业页能显示统一空态/错误态容器
