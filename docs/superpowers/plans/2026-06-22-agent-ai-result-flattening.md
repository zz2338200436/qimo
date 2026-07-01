# Agent AI Result Flattening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make teacher AI chat responses expose question-bank-backed question/exam data directly so the frontend renders real题库结果 instead of only a success message.

**Architecture:** Keep `ai-service` question-bank lookup unchanged. Fix the `agent-service` AI tool response shape so `aiResult` is flattened into the top-level data payload while preserving compatibility fields like `status` and `message`.

**Tech Stack:** Spring Boot, JUnit 5, Mockito, static frontend renderer expectations

---

### Task 1: Lock the regression with backend tests

**Files:**
- Modify: `agent-service/src/test/java/com/_202510007517/platform/agent/tool/AgentEdgeToolTest.java`

- [ ] **Step 1: Write the failing tests**

- [ ] **Step 2: Run the targeted tests to verify they fail**

Run: `mvn -pl agent-service -Dtest=AgentEdgeToolTest test`
Expected: FAIL because AI tool results still hide question/exam payload under `aiResult`.

- [ ] **Step 3: Write the minimal implementation**

- [ ] **Step 4: Run the targeted tests to verify they pass**

Run: `mvn -pl agent-service -Dtest=AgentEdgeToolTest test`
Expected: PASS

### Task 2: Flatten AI tool payloads without changing ai-service lookup logic

**Files:**
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/tool/GenerateQuestionsTool.java`

- [ ] **Step 1: Preserve `status` and top-level `message`**

- [ ] **Step 2: Merge `response.getData()` into the top-level result map when present**

- [ ] **Step 3: Keep `aiResult` for compatibility only if existing tests or callers still depend on it**

- [ ] **Step 4: Re-run targeted tests**

Run: `mvn -pl agent-service -Dtest=AgentEdgeToolTest test`
Expected: PASS

### Task 3: Verify ai-service regression coverage still holds

**Files:**
- Reuse existing tests: `ai-service/src/test/java/com/_202510007517/platform/ai/AiServiceIntegrationTest.java`

- [ ] **Step 1: Run targeted ai-service tests**

Run: `mvn -pl ai-service -Dtest=AiServiceIntegrationTest test`
Expected: PASS and prove题库查询逻辑未回退。
