# Agent Entry To Workspace Transition Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a smooth first-message transition on both student and teacher AI pages so the UI starts as a centered entry surface and expands into a full conversation workspace after chat begins.

**Architecture:** Keep page-specific motion and layout styles inside the two static HTML files, and drive the shared conversation state from `frontend/dist/agent-chat-panel.js`. Use one shell class for the transition instead of custom per-page JavaScript branches.

**Tech Stack:** Static HTML/CSS/JS, shared agent panel script, Node verification scripts, PowerShell asset sync.

---

### Task 1: Add a shared transition-state contract

**Files:**
- Create: `scripts/verify-agent-entry-transition-contract.js`
- Modify: `frontend/dist/student-ai-assistant.html`
- Modify: `frontend/dist/teacher-ai-tools.html`
- Modify: `frontend/dist/agent-chat-panel.js`

- [ ] **Step 1: Write the failing contract script**

Create a contract check that asserts:
- both pages contain a shell element marked with `data-agent-shell`
- both pages reference the `agent-shell-has-conversation` class in CSS
- shared JS contains shell state helpers and applies the class

- [ ] **Step 2: Run contract script to verify it fails**

Run: `node .\scripts\verify-agent-entry-transition-contract.js`
Expected: FAIL because the current files do not yet contain the new shell state hooks.

- [ ] **Step 3: Add the minimum structural hooks**

Add `data-agent-shell` to the student and teacher shell containers, and add the state helper names in shared JS.

- [ ] **Step 4: Run contract script again**

Run: `node .\scripts\verify-agent-entry-transition-contract.js`
Expected: still FAIL or partially fail until CSS/JS/state wiring is complete.

### Task 2: Add student-page entry-to-workspace motion

**Files:**
- Modify: `frontend/dist/student-ai-assistant.html`

- [ ] **Step 1: Update student shell CSS for entry state**

Add transition-aware styles for:
- shell vertical rhythm
- empty-state opacity/translate
- message area alignment/padding
- composer max-width/shadow/position

- [ ] **Step 2: Add student workspace-state overrides**

When `.agent-shell-has-conversation` is present:
- reduce empty-state prominence
- move message area to top-aligned workspace
- tighten composer into bottom workspace mode

- [ ] **Step 3: Add reduced-motion handling**

Add a short `prefers-reduced-motion` block so the transition stays accessible.

- [ ] **Step 4: Run student contract**

Run: `node .\scripts\verify-student-ai-assistant-contract.js`
Expected: PASS

### Task 3: Add teacher-page entry-to-workspace motion

**Files:**
- Modify: `frontend/dist/teacher-ai-tools.html`

- [ ] **Step 1: Update teacher shell CSS for entry state**

Mirror the student choreography while preserving blue/indigo styling.

- [ ] **Step 2: Add teacher workspace-state overrides**

When `.agent-shell-has-conversation` is present:
- compress hero section
- hide entry emphasis
- let message area and composer read as a working canvas

- [ ] **Step 3: Add reduced-motion handling**

Match the student page behavior.

- [ ] **Step 4: Run teacher contract**

Run: `node .\scripts\verify-teacher-ai-tools-contract.js`
Expected: PASS

### Task 4: Wire conversation state in shared agent script

**Files:**
- Modify: `frontend/dist/agent-chat-panel.js`

- [ ] **Step 1: Add shell state helpers**

Add minimal helpers to:
- find the current shell root
- mark workspace mode on first conversation activity
- clear workspace mode only when there are no real messages

- [ ] **Step 2: Apply state on send / append / session switch**

Use the helpers when:
- a user message is appended
- an agent message is rendered
- a restored or switched session is loaded

- [ ] **Step 3: Preserve current logic**

Do not change:
- request flow
- history drawer behavior
- pause / thinking handling

- [ ] **Step 4: Run shared script checks**

Run:
- `node .\scripts\verify-agent-entry-transition-contract.js`
- `node .\scripts\verify-agent-chat-thinking-placeholder.js`
- `node .\scripts\verify-agent-chat-enter-submit.js`

Expected: all PASS

### Task 5: Sync and final verification

**Files:**
- Modify: `major_assignment/src/main/resources/static/agent-chat-panel.js`
- Modify: `major_assignment/src/main/resources/static/student-ai-assistant.html`
- Modify: `major_assignment/src/main/resources/static/teacher-ai-tools.html`

- [ ] **Step 1: Sync frontend assets**

Run: `powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1`
Expected: relevant files sync from `frontend/dist` to Spring static.

- [ ] **Step 2: Verify sync is clean**

Run: `powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1 -CheckOnly`
Expected: `Frontend assets and Spring static assets are already in sync.`

- [ ] **Step 3: Re-run all relevant checks**

Run:
- `node .\scripts\verify-agent-entry-transition-contract.js`
- `node .\scripts\verify-student-ai-assistant-contract.js`
- `node .\scripts\verify-teacher-ai-tools-contract.js`
- `node .\scripts\verify-agent-chat-thinking-placeholder.js`
- `node .\scripts\verify-agent-chat-enter-submit.js`

Expected: all PASS
