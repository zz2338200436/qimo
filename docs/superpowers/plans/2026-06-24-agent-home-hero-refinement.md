# Agent Home Hero Refinement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refine the student and teacher AI entry screens so their empty state feels closer to the lighter ChatGPT-style homepage while keeping the existing business-page shell and conversation transition behavior.

**Architecture:** Keep the refinement CSS-first inside the two page HTML files. Reuse the existing shared conversation-state contract in `agent-chat-panel.js`, and add one targeted verifier that checks the selected lighter visual contract on both pages. Sync `frontend/dist` into Spring static after the changes land.

**Tech Stack:** Static HTML/CSS/JS, Node verification scripts, PowerShell asset sync.

---

### Task 1: Add a visual contract test for the chosen lighter homepage variant

**Files:**
- Create: `scripts/verify-agent-home-hero-refinement-contract.js`

- [ ] **Step 1: Write the failing contract script**

Create a verifier that asserts both pages contain the selected empty-state contract:
- lower-empty-state header treatment markers
- hero-upward positioning markers
- centered example prompt chips
- premium empty-state composer treatment

- [ ] **Step 2: Run the contract script to verify it fails**

Run: `node .\scripts\verify-agent-home-hero-refinement-contract.js`
Expected: FAIL because the current pages do not yet include the new lighter-homepage selectors and values.

### Task 2: Refine the student empty-state layout

**Files:**
- Modify: `frontend/dist/student-ai-assistant.html`

- [ ] **Step 1: Apply the lighter homepage-style empty-state CSS**

Update the empty-state header, hero, prompt chips, composer, and note treatment so the initial screen feels lighter and more centered without changing conversation-state behavior.

- [ ] **Step 2: Verify the student-specific contract still holds**

Run: `node .\scripts\verify-student-ai-assistant-contract.js`
Expected: PASS

### Task 3: Refine the teacher empty-state layout

**Files:**
- Modify: `frontend/dist/teacher-ai-tools.html`

- [ ] **Step 1: Apply the same lighter homepage-style empty-state CSS in the teacher theme**

Mirror the student refinement while preserving the existing teacher color accents and layout shell.

- [ ] **Step 2: Verify the teacher-specific contract still holds**

Run: `node .\scripts\verify-teacher-ai-tools-contract.js`
Expected: PASS

### Task 4: Run the new visual contract and sync assets

**Files:**
- Modify: `major_assignment/src/main/resources/static/student-ai-assistant.html`
- Modify: `major_assignment/src/main/resources/static/teacher-ai-tools.html`

- [ ] **Step 1: Run the new visual contract**

Run: `node .\scripts\verify-agent-home-hero-refinement-contract.js`
Expected: PASS

- [ ] **Step 2: Sync frontend assets**

Run: `powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1`
Expected: relevant files sync from `frontend/dist` to Spring static.

- [ ] **Step 3: Verify sync is clean**

Run: `powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1 -CheckOnly`
Expected: `Frontend assets and Spring static assets are already in sync.`

### Task 5: Final regression verification

**Files:**
- Verify only

- [ ] **Step 1: Re-run all relevant targeted checks**

Run:
- `node .\scripts\verify-agent-home-hero-refinement-contract.js`
- `node .\scripts\verify-agent-entry-transition-contract.js`
- `node .\scripts\verify-student-ai-assistant-contract.js`
- `node .\scripts\verify-teacher-ai-tools-contract.js`
- `node .\scripts\verify-agent-chat-thinking-placeholder.js`
- `node .\scripts\verify-agent-chat-enter-submit.js`

Expected: all PASS
