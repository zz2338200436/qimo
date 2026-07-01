# Random Question Assignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let teachers randomly generate question-bank questions and publish the generated set as an assignment in a later Agent message.

**Architecture:** Keep `ai-service` as the question-bank source and randomize matching query results. Reuse `agent_sessions.pending_slots_json` in `agent-service` to carry the generated question content into the next `PUBLISH_ASSIGNMENT` request.

**Tech Stack:** Spring Boot, JUnit 5, Mockito, H2 integration tests

---

### Task 1: Add failing Agent workflow test

**Files:**
- Modify: `agent-service/src/test/java/com/_202510007517/platform/agent/tool/AgentEdgeToolTest.java`

- [ ] Write a test where `GENERATE_QUESTIONS` returns two question maps, then the next message publishes an assignment in the same session.
- [ ] Assert the publish preview contains `content` with both generated questions.
- [ ] Confirm the action and capture `TeacherAssignmentUpsertRequestDTO.description`.

Run: `mvn -pl agent-service -Dtest=AgentEdgeToolTest test`
Expected: FAIL because generated question content is not carried into the publish assignment request.

### Task 2: Save generated questions into pending assignment context

**Files:**
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java`

- [ ] After non-confirmed `GENERATE_QUESTIONS` executes, if result has non-empty `questions`, save pending intent `PUBLISH_ASSIGNMENT`.
- [ ] Store `content` as formatted question text, plus optional generated title and max score if available.
- [ ] Preserve normal pending-context behavior for missing slots.

Run: `mvn -pl agent-service -Dtest=AgentEdgeToolTest test`
Expected: PASS for the new workflow.

### Task 3: Randomize question-bank selection

**Files:**
- Modify: `ai-service/src/main/java/com/_202510007517/platform/ai/model/LocalMockAiModelClient.java`
- Modify: `ai-service/src/test/java/com/_202510007517/platform/ai/AiServiceIntegrationTest.java`

- [ ] Add a test that inserts more matching questions than requested and asserts the requested count is honored.
- [ ] Change the matching question query order from deterministic `q.id` to random order.

Run: `mvn -pl ai-service -Dtest=AiServiceIntegrationTest test`
Expected: PASS.
