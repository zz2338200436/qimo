# Agent Web Search Markdown Format Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make internet-search replies render as compact Markdown with a direct answer and readable source list.

**Architecture:** Keep the existing chat renderer unchanged and emit Markdown-friendly text from `ResponseComposerService`. Lock the new shape with focused reply-composer tests so formatting regressions are caught without involving the browser.

**Tech Stack:** Java 17, Spring Boot service layer, JUnit 5, AssertJ, existing Markdown-capable chat UI

---

### Task 1: Reformat Internet Search Replies

**Files:**
- Modify: `agent-service/src/test/java/com/_202510007517/platform/agent/service/ResponseComposerServiceTest.java`
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/service/ResponseComposerService.java`

- [ ] **Step 1: Write the failing tests**

Add assertions that internet-search replies render as Markdown-style sections:
- direct answer on its own line
- blank line before sources
- `**来源（查询词）**` heading
- numbered list items using Markdown links
- source snippets rendered as indented bullets

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl agent-service -Dtest=ResponseComposerServiceTest -DforkCount=0 test`
Expected: FAIL because the current reply still emits plain text source blocks.

- [ ] **Step 3: Write minimal implementation**

Update `ResponseComposerService` so `composeInternetSearchReply(...)` returns Markdown-friendly text while keeping the current answer-selection logic intact.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl agent-service -Dtest=ResponseComposerServiceTest -DforkCount=0 test`
Expected: PASS with `Tests run` matching the focused suite and `Failures: 0, Errors: 0`.

