# Teacher Frontend Polish Design

Date: 2026-05-23
Scope: `frontend/dist` teacher-side static pages and shared shell

## Goal

Polish the teacher-side frontend so the final delivery feels like one coherent product instead of several partially aligned static pages. The work should improve presentation quality, consistency, and demo safety without changing backend contracts or introducing new APIs.

## Problem Summary

The current teacher-side frontend is functional, but the user experience still shows several delivery-stage rough edges:

- Shared shell styles are duplicated across pages and are not fully aligned.
- Teacher pages mix different resource-loading styles and visual conventions.
- Some teacher-side interactions still expose "正在开发中" messaging directly in primary flows.
- The running dashboard currently reports missing frontend assets, including a `font-awesome-full.css` 404.
- The settings page has a noticeably different visual structure from dashboard, courses, and assignments.
- Feedback states such as loading, empty, success, and error are implemented inconsistently.

These issues reduce confidence during demo flows even when the underlying data and CRUD behavior work.

## Design Principles

1. Keep the existing light teacher-console look instead of switching to a new dark theme.
2. Optimize for final delivery and presentation stability before broader redesign.
3. Preserve existing backend and API behavior.
4. Prefer focused improvements to shared shell and high-frequency teacher pages over global refactors.
5. Remove "half-finished" signals from visible primary workflows where possible.

## In Scope

Primary pages:

- `frontend/dist/teacher-dashboard.html`
- `frontend/dist/teacher-courses.html`
- `frontend/dist/teacher-assignments.html`
- `frontend/dist/teacher-settings.html`

Shared shell and shared presentation behavior:

- `frontend/dist/components/teacher-sidebar-nav.html`
- `frontend/dist/styles.css`
- Limited teacher-facing feedback and status behavior in `frontend/dist/api.js`

## Out of Scope

- Student-side page redesign
- Backend API changes
- New endpoints or data models
- Large-scale componentization or framework migration
- Full visual rewrite of all teacher pages
- Completing feature areas that are genuinely not implemented server-side

## User Experience Targets

### 1. Unified Teacher Shell

All scoped teacher pages should present the same shell language:

- same sidebar width, spacing, icon treatment, and active-state styling
- same top navbar height, padding, page-title placement, and user action area
- same content width behavior and section spacing

The teacher shell should feel stable when moving between dashboard, courses, assignments, and settings.

### 2. Stronger Visual Hierarchy

The scoped pages should use a cleaner admin-console hierarchy:

- page heading and supporting actions clearly separated from content blocks
- cards and panels with consistent border radius, shadows, and surface color
- tables with better spacing, row separation, and filter/action grouping
- muted secondary text and stronger primary text contrast

### 3. Safer Demo Flows

Primary user actions should avoid abrupt "开发中" messages where possible.

Within scope:

- downgrade unfinished actions into less prominent affordances
- use disabled or softer secondary treatments for actions that are not ready
- prefer inline informational hints, tooltip-style explanations, or disabled labels over alert-style interruptions for known unsupported actions
- replace blunt error-style messaging for known unsupported teacher-side settings with clearer informational messaging

Out-of-scope unfinished features do not need to be built, but they should no longer read like accidental breakage in the middle of a demo.

### 4. Consistent States

The scoped pages should converge on one teacher-console pattern for:

- loading states
- empty states
- success feedback
- recoverable error feedback
- destructive-action confirmation

These states do not need a new abstraction layer, but they should read consistently in copy and presentation.

## Page-by-Page Plan

### Teacher Dashboard

Goals:

- keep it as a true overview page
- improve KPI card consistency
- improve chart/card section spacing
- reduce the sense that extra in-page modules are mixed into one oversized file

Specific direction:

- normalize top summary cards
- normalize card hover and border/shadow styling
- ensure chart titles and recent-activity area share the same panel treatment
- soften or demote unfinished exam/detail actions so they do not look like broken main actions

### Teacher Courses

Goals:

- make the page read as a management console
- tighten filters, table spacing, and action grouping
- improve table scanability and section boundaries

Specific direction:

- bring filter/search blocks closer to a consistent toolbar layout
- align card and table containers with dashboard shell
- improve spacing around list/create/edit sections and modal or form surfaces if present

### Teacher Assignments

Goals:

- make assignment and exam operations feel like one coherent task-management page
- improve density and readability without turning the page into a cluttered wall

Specific direction:

- align action bars, tabs/sections, and table controls
- visually separate primary actions from secondary row actions
- standardize empty, loading, and error blocks with the rest of the teacher console

### Teacher Settings

Goals:

- make settings feel like part of the same product rather than a separate page
- improve form readability and feedback quality

Specific direction:

- bring navbar and shell styling in line with the other teacher pages
- restyle settings sections as polished console panels
- improve form spacing, labels, helper text, and button hierarchy
- convert the current teacher notification persistence limitation from a harsh stop into a clear informational state

## Shared Technical Approach

### CSS Strategy

Use the existing static-page architecture, but shift more page-shell styling into shared CSS instead of repeating large inline blocks in each page.

Expected direction:

- move common teacher-shell primitives into `styles.css`
- keep page-specific CSS only where the page layout truly differs
- reduce visual drift by using shared class names for shell, section, toolbar, panel, and state blocks
- preserve current page script entry points and major automation-facing selectors while refactoring presentation markup

### Asset and Dependency Cleanup

Correct frontend resource inconsistencies that visibly affect runtime quality.

Known example from runtime evidence:

- dashboard currently emits a missing `font-awesome-full.css` resource request

Any equivalent scoped teacher-shell resource mismatch found during implementation should be cleaned up as part of this pass.

### Feedback Copy

Teacher-facing feedback in scoped pages should shift toward:

- concise operational language
- informational tone for unsupported-but-known conditions
- fewer blunt "功能正在开发中" interruptions in user-facing primary flows

## Risks and Constraints

1. Teacher pages are large static HTML files with embedded scripts, so edits should stay localized and careful.
2. Shared shell changes can unintentionally affect pages outside the four primary targets; verification must include targeted page checks.
3. Some unsupported actions are backed by missing server capability rather than UI gaps; this pass should present them better, not fake full support.
4. Existing browser smoke scripts and embedded page scripts may depend on current IDs, function names, and DOM structure; implementation should preserve those hooks unless a matching script update is intentionally included.

## Verification Plan

Implementation is not complete until these are checked against current runtime behavior:

1. Manual visual verification of:
   - `teacher-dashboard.html`
   - `teacher-courses.html`
   - `teacher-assignments.html`
   - `teacher-settings.html`
2. Confirm the teacher dashboard no longer shows the observed shared-shell asset 404 caused by the scoped resource issue.
3. Re-run the existing teacher-side browser smoke coverage relevant to the scoped pages.
4. Re-run teacher/student CRUD smoke scripts to ensure presentation-layer changes did not break behavior.
5. Verify responsive layout still avoids obvious text overflow and layout collisions on desktop and mobile-sized viewports.

## Recommended Implementation Order

1. Shared teacher shell and shared CSS primitives
2. Teacher settings alignment
3. Teacher dashboard polish and unsupported-action demotion
4. Teacher courses polish
5. Teacher assignments polish
6. Verification and cleanup

## Success Criteria

This work is successful when:

- the four scoped teacher pages look like one consistent product
- the most visible demo-time rough edges are softened or removed
- shared shell styling is more unified and less duplicated
- teacher-facing states are clearer and more polished
- existing smoke-tested teacher/student flows still pass after the UI changes
