# Role Color Theme Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply scheme 1: teacher pages use a professional blue identity, student pages use a friendly teal identity, while both roles keep the same static Bootstrap console structure.

**Architecture:** The implementation keeps the existing static frontend and adds role-specific theme variables/classes in shared CSS. Student and teacher sidebar/login components consume the role palette, and the deployable `frontend/dist` files are synced to the legacy Spring Boot static mirror.

**Tech Stack:** Static HTML, CSS custom properties, Bootstrap 5 assets, Font Awesome, Node.js verification script.

---

### Checklist

- [x] **Step 1: Confirm scope**

  Only modify the static frontend surface:

  - `frontend/dist/styles.css`
  - `frontend/dist/components/sidebar-nav.html`
  - `frontend/dist/components/teacher-sidebar-nav.html`
  - `frontend/dist/student-login.html`
  - `frontend/dist/teacher-login.html`
  - matching files under `major_assignment/src/main/resources/static/`
  - `scripts/verify-role-color-theme.js`

- [x] **Step 2: Write the failing verifier**

  Create `scripts/verify-role-color-theme.js` to prove:

  - shared role variables exist in `frontend/dist/styles.css`
  - teacher palette includes `#2563eb`, `#1d4ed8`, `#0f172a`
  - student palette includes `#0f766e`, `#0d9488`, `#14b8a6`
  - student sidebar uses teal, teacher sidebar uses blue
  - student login uses teal, teacher login uses blue
  - old purple gradient `#667eea` / `#764ba2` is not used in the role-owned theme files
  - deployable and legacy static copies match for the touched files

- [x] **Step 3: Run verifier for RED**

  Run:

  ```powershell
  node scripts/verify-role-color-theme.js
  ```

  Expected: FAIL because the current teacher sidebar still uses the old purple gradient and the student login/sidebar are not teal.

- [x] **Step 4: Implement scheme 1 in shared CSS**

  Add role variables and role-aware rules:

  - teacher primary: `#2563eb`
  - teacher strong: `#1d4ed8`
  - teacher shell: `#0f172a`
  - student primary: `#0d9488`
  - student strong: `#0f766e`
  - student accent: `#14b8a6`

  Keep layout, spacing, component names, and API-facing JavaScript unchanged.

- [x] **Step 5: Implement scheme 1 in role components**

  Update:

  - student sidebar to use teal gradient and `data-role-theme="student"`
  - teacher sidebar to use blue shell gradient and `data-role-theme="teacher"`
  - student login to use teal page/header/button/focus colors
  - teacher login to use blue page/header/button/focus colors

- [x] **Step 6: Sync legacy static mirror**

  Copy the touched `frontend/dist` files to:

  - `major_assignment/src/main/resources/static/styles.css`
  - `major_assignment/src/main/resources/static/components/sidebar-nav.html`
  - `major_assignment/src/main/resources/static/components/teacher-sidebar-nav.html`
  - `major_assignment/src/main/resources/static/student-login.html`
  - `major_assignment/src/main/resources/static/teacher-login.html`

- [x] **Step 7: Run verifier for GREEN**

  Run:

  ```powershell
  node scripts/verify-role-color-theme.js
  ```

  Expected: PASS with every scheme 1 requirement verified.

- [x] **Step 8: Browser smoke check**

  Serve `frontend/dist` locally and inspect:

  - `student-login.html`
  - `teacher-login.html`
  - `student-dashboard.html`
  - `teacher-dashboard.html`

  Expected: teacher surfaces read as blue/professional, student surfaces read as teal/friendly, with no obvious horizontal overflow on desktop or mobile width.
