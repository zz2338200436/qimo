# Teacher Student AI Entry Unification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring the teacher and student AI entry pages onto one consistent visual rhythm without changing their Agent behavior or backend contracts.

**Architecture:** Treat `frontend/dist` as the source of truth, first align it with the already-landed Spring static pages, then apply a narrow visual unification pass to the shared entry sections only. Preserve all existing `data-*` hooks, initialization helpers, request flows, and history drawer bindings.

**Tech Stack:** Static HTML, inline CSS, vanilla JavaScript, Bootstrap 5, Font Awesome, PowerShell sync script, Node-based contract verification

---

## File Structure

**Modify**
- `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\student-ai-assistant.html`
- `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-ai-tools.html`
- `D:\111\Distributed framework technology\JavaCode\majorassignment\scripts\verify-student-ai-assistant-contract.js`
- `D:\111\Distributed framework technology\JavaCode\majorassignment\scripts\verify-teacher-ai-tools-contract.js`

**Sync**
- `D:\111\Distributed framework technology\JavaCode\majorassignment\major_assignment\src\main\resources\static\student-ai-assistant.html`
- `D:\111\Distributed framework technology\JavaCode\majorassignment\major_assignment\src\main\resources\static\teacher-ai-tools.html`

**Verify**
- `node .\scripts\verify-student-ai-assistant-contract.js`
- `node .\scripts\verify-teacher-ai-tools-contract.js`
- `powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1`
- `powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1 -CheckOnly`

## Task 1: Tighten the Contract Guardrails

**Files:**
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\scripts\verify-student-ai-assistant-contract.js`
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\scripts\verify-teacher-ai-tools-contract.js`

- [ ] Require the new empty-state role label, unified composer placeholder, updated teacher title, and bottom-note strings.
- [ ] Run both contract scripts and confirm they fail against the stale `frontend/dist` pages before implementation.

## Task 2: Align `frontend/dist` With the Current Formal Pages

**Files:**
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\student-ai-assistant.html`
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-ai-tools.html`

- [ ] Replace the stale student page shell in `frontend/dist` with the current minimal Agent-entry version already present in Spring static.
- [ ] Replace the stale teacher page shell in `frontend/dist` with the current minimal Agent-entry version already present in Spring static.
- [ ] Preserve all current `data-agent-*` hooks and `initialize*EmptyState()` helpers.

## Task 3: Apply the Unification Pass

**Files:**
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\student-ai-assistant.html`
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\frontend\dist\teacher-ai-tools.html`

- [ ] Add role labels above the empty-state headlines.
- [ ] Normalize prompt-chip rhythm, textarea placeholder specificity, and composer notes.
- [ ] Rename the teacher page panel title to `AI教学助手` while keeping student as `AI学习助手`.

## Task 4: Sync and Verify

**Files:**
- Sync: `frontend/dist/*` to `major_assignment/src/main/resources/static/*`

- [ ] Run `node .\scripts\verify-student-ai-assistant-contract.js`
- [ ] Run `node .\scripts\verify-teacher-ai-tools-contract.js`
- [ ] Run `powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1`
- [ ] Run `powershell -ExecutionPolicy Bypass -File .\scripts\sync-frontend-to-static.ps1 -CheckOnly`
- [ ] Report any runtime verification gaps if local services or sessions are not available.
