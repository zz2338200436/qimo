# Teacher Frontend Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Polish the scoped teacher-side static pages so they feel consistent, safer to demo, and still preserve current teacher/student behavior and automation hooks.

**Architecture:** Keep the existing static multi-page architecture, but pull repeated teacher-shell presentation into shared CSS and make localized markup/style adjustments per page. Preserve existing script entry points such as `toggleBtn`, `mainContent`, `initSidebar`, page-specific function names, and table/form selectors so current browser smoke and CRUD automation keep working.

**Tech Stack:** Static HTML, shared CSS, vanilla JavaScript, Bootstrap 5, Font Awesome, local `frontend/dist/lib` assets, Node-based smoke scripts, Playwright-based browser checks

---

## File Structure

**Modify**
- `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\styles.css`
  - Add shared teacher-shell primitives, panel/table/toolbar/state styles, and notification presentation helpers.
- `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\components\teacher-sidebar-nav.html`
  - Keep the current sidebar IDs/functions, but align shared shell styling expectations and avoid style conflicts with page-local CSS.
- `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-dashboard.html`
  - Switch shared shell/resource usage toward local assets and shared CSS, soften unfinished exam actions, preserve dashboard API flow and existing functions like `viewExam`, `editExam`, `startExam`, `showNotification`.
- `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-courses.html`
  - Align shell/resources, tighten toolbar/table hierarchy, remove alert-style resource failure handling, preserve modal/form/table selectors and functions such as `openAssignCourseModal`, `loadCourses`, `showNotification`.
- `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-assignments.html`
  - Align shell/resources, tighten tabs/action hierarchy, standardize visible state blocks, preserve assignment/exam CRUD functions and existing IDs.
- `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-settings.html`
  - Rework shell and settings panel presentation to match the teacher console, keep save flows intact, and turn the unsupported notification persistence branch into a calmer informational experience.
- `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\api.js`
  - Limit changes to teacher-facing feedback copy/presentation where the scoped pages surface submission/export interactions.

**Verify**
- `D:\111\Distributed framework technology\JavaCode\majorassignment\scripts\verify-teacher-jwt-pages.js`
- `D:\111\Distributed framework technology\JavaCode\majorassignment\scripts\verify-teacher-browser-crud.js`
- `D:\111\Distributed framework technology\JavaCode\majorassignment\scripts\verify-student-browser-crud.js`

## Task 1: Establish Shared Teacher Shell Styles

**Files:**
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\styles.css`
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\components\teacher-sidebar-nav.html`
- Test: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-dashboard.html`
- Test: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-courses.html`
- Test: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-assignments.html`
- Test: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-settings.html`

- [ ] **Step 1: Add the shared teacher-shell CSS primitives to `styles.css`**

```css
/* teacher shell primitives */
.teacher-shell-body {
    font-family: 'Inter', 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    background: #f8fafc;
    color: #334155;
    min-height: 100vh;
}

.teacher-shell-main {
    margin-left: 280px;
    min-height: 100vh;
    transition: margin-left 0.3s ease;
}

.teacher-shell-main.collapsed {
    margin-left: 80px;
}

.teacher-topbar {
    background: rgba(255, 255, 255, 0.96);
    border-bottom: 1px solid #e2e8f0;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
    padding: 16px 24px;
}

.teacher-content {
    padding: 24px;
}

.teacher-page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 16px;
    margin-bottom: 24px;
}

.teacher-panel {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 14px;
    box-shadow: 0 10px 30px rgba(15, 23, 42, 0.05);
}

.teacher-toolbar {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    align-items: center;
}

.teacher-empty-state,
.teacher-info-state,
.teacher-error-state {
    border-radius: 12px;
    padding: 16px 18px;
    border: 1px solid #e2e8f0;
}

.teacher-info-state {
    background: #eff6ff;
    border-color: #bfdbfe;
    color: #1d4ed8;
}

.teacher-empty-state {
    background: #f8fafc;
    color: #64748b;
}

.teacher-error-state {
    background: #fef2f2;
    border-color: #fecaca;
    color: #b91c1c;
}

.teacher-muted-action[disabled],
.teacher-muted-action.is-disabled {
    opacity: 0.6;
    cursor: not-allowed;
    box-shadow: none;
    transform: none;
}
```

- [ ] **Step 2: Add shared sidebar compatibility styles without breaking existing IDs**

```html
<style>
    .sidebar,
    #sidebar {
        background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
        color: white;
        box-shadow: 0 18px 40px rgba(15, 23, 42, 0.18);
    }

    .sidebar-header {
        padding: 24px 20px 18px;
    }

    .menu-item {
        min-height: 48px;
        border-radius: 0 12px 12px 0;
        margin: 0 10px 6px 0;
    }

    .menu-item.active {
        background: rgba(255, 255, 255, 0.18);
    }
</style>
```

- [ ] **Step 3: Update each scoped page to opt into the shared teacher-shell classes**

```html
<body class="teacher-shell-body">
    <div id="sidebar-container"></div>
    <main class="main-content teacher-shell-main" id="mainContent">
        <nav class="top-navbar teacher-topbar">
            <div class="navbar-left">
                <button class="toggle-btn" id="toggleBtn">
                    <i class="fa fa-bars"></i>
                </button>
                <h1 class="page-title">页面标题保持原页面文案</h1>
            </div>
        </nav>
        <div class="content-container teacher-content">
            <div id="messageContainer"></div>
            <section class="teacher-panel">保留原页面主内容容器与现有脚本绑定元素</section>
        </div>
    </main>
</body>
```

- [ ] **Step 4: Verify the shared-shell classes are referenced on all four pages**

Run:

```powershell
Select-String -Path 'D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-dashboard.html','D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-courses.html','D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-assignments.html','D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-settings.html' -Pattern 'teacher-shell-body|teacher-shell-main|teacher-topbar|teacher-content'
```

Expected: each page shows matches for the shared shell classes while still retaining `mainContent`.

- [ ] **Step 5: Commit**

```bash
git add frontend/dist/styles.css frontend/dist/components/teacher-sidebar-nav.html frontend/dist/teacher-dashboard.html frontend/dist/teacher-courses.html frontend/dist/teacher-assignments.html frontend/dist/teacher-settings.html
git commit -m "feat: unify teacher shell presentation"
```

## Task 2: Normalize Teacher Page Assets and Resource Loading

**Files:**
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-dashboard.html`
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-courses.html`
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-assignments.html`
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-settings.html`

- [ ] **Step 1: Replace mixed CDN/local shell assets with the same local asset set where available**

```html
<link rel="icon" href="data:,">
<link href="lib/bootstrap.min.css" rel="stylesheet">
<link href="lib/font-awesome.min.css" rel="stylesheet">
<script src="lib/bootstrap.bundle.min.js"></script>
```

Apply the same pattern to:
- `teacher-courses.html`
- `teacher-assignments.html`
- `teacher-settings.html`

Keep page-specific scripts like `lib/echarts.min.js` only where the page already needs them.

- [ ] **Step 2: Remove the dashboard reference that triggers the missing stylesheet request**

```powershell
Select-String -Path 'D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-dashboard.html' -Pattern 'font-awesome-full'
```

Expected before change: a match referencing `font-awesome-full.css` or equivalent stale asset path.

After editing, rerun the same command.

Expected after change: no matches.

- [ ] **Step 3: Replace alert-based resource-load fallbacks in courses with notification-friendly handling**

```javascript
function reportResourceLoadFailure(message) {
    console.error(message);
    if (typeof showNotification === 'function') {
        showNotification('页面资源加载异常，请刷新后重试', 'danger');
        return;
    }
    const fallback = document.getElementById('browser-error-container');
    if (fallback) {
        fallback.style.display = 'block';
        fallback.textContent = '页面资源加载异常，请刷新后重试';
    }
}
```

Use this instead of:

```javascript
alert('页面资源加载失败，请刷新页面');
```

- [ ] **Step 4: Verify no scoped page still contains the targeted resource-alert pattern**

Run:

```powershell
Select-String -Path 'D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-courses.html','D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-dashboard.html','D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-assignments.html','D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-settings.html' -Pattern 'font-awesome-full|页面资源加载失败，请刷新页面'
```

Expected: no matches.

- [ ] **Step 5: Commit**

```bash
git add frontend/dist/teacher-dashboard.html frontend/dist/teacher-courses.html frontend/dist/teacher-assignments.html frontend/dist/teacher-settings.html
git commit -m "fix: normalize teacher page assets"
```

## Task 3: Polish the Dashboard and Soften Unsupported Exam Actions

**Files:**
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-dashboard.html`
- Test: `D:\111\Distributed framework technology\JavaCode\majorassignment\scripts\verify-teacher-jwt-pages.js`

- [ ] **Step 1: Convert dashboard summary/layout blocks to shared panel classes without renaming existing script hooks**

```html
<section class="stats-cards">
    <article class="stat-card teacher-panel">
        <div class="stat-header">
            <span class="stat-title">教授课程</span>
            <div class="stat-icon"><i class="fa fa-book"></i></div>
        </div>
        <div class="stat-value" data-stat="courses">0</div>
    </article>
</section>

<section class="teacher-panel dashboard-section">
    <div class="section-header">
        <div>
            <h2>最近活动</h2>
            <p class="section-description">跟踪近期课程、作业与考试动态</p>
        </div>
    </div>
    <div id="recentActivitiesList"></div>
</section>
```

- [ ] **Step 2: Keep `viewExam`, `editExam`, and `startExam` functions, but change their UX from error-style to informational**

```javascript
function showInfoNotification(message) {
    showNotification(message, 'info');
}

function viewExam(examId) {
    console.log('查看考试详情:', examId);
    showInfoNotification('考试详情入口保留中，当前交付环境暂未开放该页面。');
}

function editExam(examId) {
    console.log('编辑考试:', examId);
    showInfoNotification('考试编辑入口暂未开放，请在考试管理页维护考试内容。');
}

function startExam(examId) {
    console.log('开始考试:', examId);
    showInfoNotification('当前交付环境未开放手动开考入口，请按既定发布时间组织考试。');
}
```

- [ ] **Step 3: Demote unsupported exam action buttons visually while preserving `onclick` hooks**

```html
<button
    class="btn btn-sm btn-outline-secondary teacher-muted-action"
    onclick="viewExam(${exam.id})"
    title="当前交付环境暂未开放该操作">
    <i class="fa fa-eye"></i> 查看
</button>
```

Apply the same idea to `editExam` and `startExam`, but keep the existing function names.

- [ ] **Step 4: Verify the dashboard still exposes the page text and no longer contains the old unsupported-copy**

Run:

```powershell
Select-String -Path 'D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-dashboard.html' -Pattern '仪表盘|查看考试功能正在开发中|编辑考试功能正在开发中|开始考试功能正在开发中|function viewExam|function editExam|function startExam'
```

Expected:
- `仪表盘` remains present
- `function viewExam`, `function editExam`, `function startExam` remain present
- the three `正在开发中` messages are gone

- [ ] **Step 5: Commit**

```bash
git add frontend/dist/teacher-dashboard.html
git commit -m "feat: polish teacher dashboard actions"
```

## Task 4: Polish the Courses Page Toolbar, Table, and Notifications

**Files:**
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-courses.html`

- [ ] **Step 1: Reframe the top page header and filter regions as shared toolbars/panels**

```html
<div class="page-header teacher-page-header">
    <div>
        <h1 class="page-title">课程与班级管理</h1>
        <p class="text-muted mb-0">统一管理课程、班级与课程分配。</p>
    </div>
    <div class="btn-group teacher-toolbar">
        <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#addCourseModal">
            <i class="fa fa-plus"></i> 新增课程
        </button>
        <button class="btn btn-secondary" data-bs-toggle="modal" data-bs-target="#addClassModal">
            <i class="fa fa-plus"></i> 新增班级
        </button>
    </div>
</div>

<section class="teacher-panel filter-panel">
    <div class="teacher-toolbar">
        <input id="course-search-input" class="form-control" placeholder="搜索课程名称">
        <button id="course-search-btn" class="btn btn-secondary" type="button">查询</button>
    </div>
</section>
```

- [ ] **Step 2: Improve table readability while preserving selectors like `#courses-content .table tbody`**

```html
<div class="data-table teacher-panel">
    <table class="table align-middle">
        <thead>
            <tr>
                <th>课程名称</th>
                <th>课程编码</th>
                <th>状态</th>
                <th>操作</th>
            </tr>
        </thead>
        <tbody></tbody>
    </table>
</div>
```

```css
.data-table.teacher-panel .table th {
    font-size: 13px;
    color: #64748b;
    letter-spacing: 0;
}

.data-table.teacher-panel .table td {
    vertical-align: middle;
}
```

- [ ] **Step 3: Standardize course-page notifications without changing the existing `showNotification` signature**

```javascript
function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    const classMap = {
        success: 'success-notification',
        danger: 'error-notification',
        warning: 'error-notification',
        info: 'notification'
    };
    notification.className = `notification ${classMap[type] || 'notification'}`;
    notification.innerHTML = `
        <div class="notification-content">
            <i class="fa ${type === 'success' ? 'fa-check-circle' : type === 'danger' || type === 'warning' ? 'fa-exclamation-circle' : 'fa-info-circle'}"></i>
            <span>${message}</span>
        </div>
        <button type="button" class="notification-close" onclick="this.parentElement.remove()">
            <i class="fa fa-times"></i>
        </button>
    `;
    document.body.appendChild(notification);
    setTimeout(() => notification.remove(), 3000);
}
```

- [ ] **Step 4: Verify the page still preserves modal and table hooks after markup polish**

Run:

```powershell
Select-String -Path 'D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-courses.html' -Pattern 'openAssignCourseModal|openAssignCourseModalForClass|loadCourses|addCourseModal|addClassModal|assignCourseModal|#courses-content|showNotification'
```

Expected: all named hooks still exist in the file.

- [ ] **Step 5: Commit**

```bash
git add frontend/dist/teacher-courses.html
git commit -m "feat: polish teacher courses workspace"
```

## Task 5: Polish the Assignments Page Tabs, Tables, and State Presentation

**Files:**
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-assignments.html`
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\api.js`

- [ ] **Step 1: Align the assignments page header, stats, and tabs with the shared shell**

```html
<div class="page-header teacher-page-header">
    <div>
        <h1 class="page-title">作业与考试发布</h1>
        <p class="text-muted mb-0">集中维护作业、考试与提交记录。</p>
    </div>
    <div class="btn-group teacher-toolbar">
        <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#addAssignmentModal">
            <i class="fa fa-plus"></i> 发布作业
        </button>
        <button class="btn btn-secondary" data-bs-toggle="modal" data-bs-target="#addExamModal">
            <i class="fa fa-plus"></i> 发布考试
        </button>
    </div>
</div>

<div class="tabs teacher-panel">
    <div class="tabs-nav">
        <button class="tab-item active" data-tab="assignments">作业管理</button>
        <button class="tab-item" data-tab="exams">考试管理</button>
        <button class="tab-item" data-tab="submissions">提交记录</button>
    </div>
</div>
```

- [ ] **Step 2: Use consistent state containers for empty/loading/error table messages**

```javascript
assignmentsTableBody.innerHTML = `
    <tr>
        <td colspan="7">
            <div class="teacher-empty-state text-center">暂无作业数据</div>
        </td>
    </tr>
`;
```

```javascript
submissionsTableBody.innerHTML = `
    <tr>
        <td colspan="8">
            <div class="teacher-error-state text-center">加载失败：${error.message || '网络异常，请稍后重试'}</div>
        </td>
    </tr>
`;
```

- [ ] **Step 3: Soften the scoped export message in `api.js`**

```javascript
function exportSubmissions() {
    console.log('导出提交记录');
    showMessage('当前交付环境暂未开放导出，请优先使用页面内批改与查询流程。', 'info');
}
```

- [ ] **Step 4: Verify assignment/exam CRUD hooks still exist after the presentation pass**

Run:

```powershell
Select-String -Path 'D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-assignments.html','D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\api.js' -Pattern 'loadAssignments|loadExams|submitAddAssignment|submitAddExam|editAssignment|editExam|deleteAssignment|deleteExam|exportSubmissions|showMessage'
```

Expected: all named hooks still exist.

- [ ] **Step 5: Commit**

```bash
git add frontend/dist/teacher-assignments.html frontend/dist/api.js
git commit -m "feat: polish teacher assignments experience"
```

## Task 6: Rework the Settings Page Into the Shared Teacher Console

**Files:**
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-settings.html`

- [ ] **Step 1: Move the settings page onto the shared shell classes and Inter-based look**

```html
<body class="teacher-shell-body">
    <div id="sidebar-container"></div>
    <main class="main-content teacher-shell-main" id="mainContent">
        <nav class="top-navbar teacher-topbar">
            <div class="navbar-left">
                <button class="toggle-btn" id="toggleBtn">
                    <i class="fa fa-bars"></i>
                </button>
                <h1 class="page-title">系统设置</h1>
            </div>
        </nav>
        <div class="content-container teacher-content">
            <div class="settings-container">
                <div id="messageContainer" class="mb-3"></div>
                <section class="settings-section teacher-panel">保留原有个人信息、密码、通知设置表单</section>
            </div>
        </div>
    </main>
</body>
```

- [ ] **Step 2: Restyle each settings section as a shared teacher panel**

```html
<section class="settings-section teacher-panel">
    <div class="section-heading">
        <h2>通知设置</h2>
        <p class="text-muted mb-0">管理教师端提醒方式与可用能力。</p>
    </div>
    <div id="notificationSettingsInfo" class="teacher-info-state">
        当前环境下可查看通知选项，持久化接口暂未开放。
    </div>
</section>
```

- [ ] **Step 3: Change the unsupported notification persistence path from hard stop copy to a calm informational state**

```javascript
async function saveNotificationSettings() {
    try {
        showMessage('当前联调环境暂未接通教师通知设置持久化接口，本次修改不会写入服务器。', 'info');
        const infoState = document.getElementById('notificationSettingsInfo');
        if (infoState) {
            infoState.textContent = '通知偏好界面可预览，服务器持久化将在后续服务接通后启用。';
            infoState.className = 'teacher-info-state';
        }
        return;
    } catch (error) {
        console.error('保存通知设置失败:', error);
        showMessage('通知设置提示渲染失败：' + error.message, 'error');
    }
}
```

With matching markup:

```html
<div id="notificationSettingsInfo" class="teacher-info-state">
    当前环境下可查看通知选项，持久化接口暂未开放。
</div>
```

- [ ] **Step 4: Verify existing settings save hooks remain intact**

Run:

```powershell
Select-String -Path 'D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-settings.html' -Pattern 'saveProfile|savePassword|saveNotificationSettings|showMessage|messageContainer|users/me|teacher-shell-body|notificationSettingsInfo'
```

Expected: all named hooks still exist.

- [ ] **Step 5: Commit**

```bash
git add frontend/dist/teacher-settings.html
git commit -m "feat: align teacher settings with console shell"
```

## Task 7: Run Visual and Smoke Verification

**Files:**
- Test: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-dashboard.html`
- Test: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-courses.html`
- Test: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-assignments.html`
- Test: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-settings.html`
- Test: `D:\111\Distributed framework technology\JavaCode\majorassignment\scripts\verify-teacher-jwt-pages.js`
- Test: `D:\111\Distributed framework technology\JavaCode\majorassignment\scripts\verify-teacher-browser-crud.js`
- Test: `D:\111\Distributed framework technology\JavaCode\majorassignment\scripts\verify-student-browser-crud.js`

- [ ] **Step 1: Manually open each scoped teacher page in the local browser session**

Check:
- dashboard shell spacing, cards, and softened unsupported exam actions
- courses toolbar/table readability and modal triggers
- assignments tabs/table state layout
- settings shell consistency and notification info state

- [ ] **Step 2: Verify the dashboard no longer reports the known shared-shell asset 404**

Run the existing page and inspect browser console. A passing result means there is no missing `font-awesome-full.css` request and no scoped shared-shell CSS 404.

- [ ] **Step 3: Re-run teacher page smoke**

Run:

```bash
node scripts/verify-teacher-jwt-pages.js .runtime-logs/teacher-session-final-verify.json
```

Expected: all scoped teacher pages pass, or only the known pre-existing dashboard aggregate false-positive remains if the script expectation is still unchanged.

- [ ] **Step 4: Re-run CRUD smoke coverage**

Run:

```bash
node scripts/verify-teacher-browser-crud.js .runtime-logs/teacher-session-final-verify.json
node scripts/verify-student-browser-crud.js .runtime-logs/student-session-final-verify.json
```

Expected: PASS for both scripts.

- [ ] **Step 5: Commit any final cleanup**

```bash
git add frontend/dist/styles.css frontend/dist/components/teacher-sidebar-nav.html frontend/dist/teacher-dashboard.html frontend/dist/teacher-courses.html frontend/dist/teacher-assignments.html frontend/dist/teacher-settings.html frontend/dist/api.js
git commit -m "chore: finalize teacher frontend polish"
```
