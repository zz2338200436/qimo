# Agent Course Detail Follow-Up Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix teacher-side Agent follow-up handling so `查看课程详情` can resolve the current course context naturally instead of always requiring a manually repeated `courseId`.

**Architecture:** Add a focused backend follow-up resolution path around `QUERY_COURSE_DETAIL`, using the current teacher course context when exactly one course candidate is available. Keep ambiguity handling conservative. If the Agent UI exposes a course-detail action, make it send an explicit course-ID-based command as a deterministic path.

**Tech Stack:** Spring Boot, JUnit 5, Mockito, static frontend JavaScript

---

### Task 1: Add failing backend regression tests for course-detail follow-up

**Files:**
- Modify: `agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentOrchestratorTest.java`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentOrchestratorTest.java`

- [ ] **Step 1: Write a failing test for the single-course follow-up case**

Add a test shaped like this:

```java
@Test
void reusesSingleCourseContextForCourseDetailFollowUp() {
    CourseDTO course = new CourseDTO();
    course.setId(12L);
    course.setCourseName("Java 分布式框架");
    when(courseFeignClient.listTeacherCourses(7L, null, null, null, null)).thenReturn(List.of(course));
    when(courseFeignClient.getCourse(12L)).thenReturn(course);

    AgentChatResponseDTO first = orchestrator.chat(7L, "TEACHER", null, "查看我的课程");
    AgentChatResponseDTO second = orchestrator.chat(7L, "TEACHER", first.getSessionId(), "查看课程详情");

    assertThat(second.getResponseType()).isEqualTo("DATA");
    assertThat(second.getData()).isInstanceOf(Map.class);
    assertThat(((Map<?, ?>) second.getData())).containsEntry("status", "EXECUTED");
    assertThat(((Map<?, ?>) second.getData())).containsKey("course");
}
```

- [ ] **Step 2: Write a failing test for the ambiguous follow-up case**

Add a second test:

```java
@Test
void keepsPromptingForCourseIdWhenFollowUpContextIsAmbiguous() {
    CourseDTO firstCourse = new CourseDTO();
    firstCourse.setId(12L);
    firstCourse.setCourseName("Java 分布式框架");
    CourseDTO secondCourse = new CourseDTO();
    secondCourse.setId(13L);
    secondCourse.setCourseName("Java 企业开发");
    when(courseFeignClient.listTeacherCourses(7L, null, null, null, null))
            .thenReturn(List.of(firstCourse, secondCourse));

    AgentChatResponseDTO first = orchestrator.chat(7L, "TEACHER", null, "查看我的课程");
    AgentChatResponseDTO second = orchestrator.chat(7L, "TEACHER", first.getSessionId(), "查看课程详情");

    assertThat(second.getResponseType()).isEqualTo("TEXT");
    assertThat(second.getMessage()).contains("课程ID");
}
```

- [ ] **Step 3: Run the targeted tests and confirm the new one fails**

Run:

```powershell
mvn -pl agent-service -am "-Dtest=AgentOrchestratorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: the new single-course follow-up test fails because `查看课程详情` still asks for `课程ID`.

### Task 2: Implement minimal backend follow-up resolution

**Files:**
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java`
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentContextEnrichmentService.java`
- Modify: `agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentContextEnrichmentServiceTest.java`

- [ ] **Step 1: Add a focused helper for resolving course detail from recent teacher context**

Implement one backend path only for `QUERY_COURSE_DETAIL`, using:

```java
if (recognizedIntent.intent() == AgentIntent.QUERY_COURSE_DETAIL
        && "TEACHER".equalsIgnoreCase(userRole)
        && !hasValue(slots.get("courseId"))) {
    // derive from current teacher course candidates only when exactly one match exists
}
```

Use the same `listTeacherCourses` source already used by course lookups. If exactly one course exists, fill:

```java
enrichedSlots.put("courseId", course.getId());
enrichedSlots.put("resolvedCourseName", course.getCourseName());
```

If multiple courses exist, leave the request unresolved so the existing prompt remains in place.

- [ ] **Step 2: Add a direct unit test for the enrichment helper**

Add a test in `AgentContextEnrichmentServiceTest.java`:

```java
@Test
void resolvesTeacherCourseDetailFromSingleTeacherCourseContext() {
    when(courseClient.listTeacherCourses(7L, null, null, null, null))
            .thenReturn(List.of(course(12L, "Java 分布式框架", "JAVA-001", "2026-Spring")));
    RecognizedIntent intent = new RecognizedIntent(
            AgentIntent.QUERY_COURSE_DETAIL,
            0.9,
            Map.of(),
            List.of()
    );

    RecognizedIntent enriched = service.enrich(7L, "TEACHER", intent);

    assertThat(enriched.slots())
            .containsEntry("courseId", 12L)
            .containsEntry("resolvedCourseName", "Java 分布式框架");
}
```

- [ ] **Step 3: Run the targeted backend tests and confirm they pass**

Run:

```powershell
mvn -pl agent-service -am "-Dtest=AgentContextEnrichmentServiceTest,AgentOrchestratorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: PASS

### Task 3: Update the Agent UI if a course-detail affordance is rendered there

**Files:**
- Modify if needed: `frontend/dist/agent-chat-panel.js`
- Modify if needed: `major_assignment/src/main/resources/static/agent-chat-panel.js`

- [ ] **Step 1: Check whether the Agent renderer creates course-detail action buttons**

If no such UI exists in the current source, make no frontend code change and keep the backend fix only.

- [ ] **Step 2: If a course-detail action exists, make it send an explicit command**

Use:

```js
this.send(`查看课程ID ${courseId}详情`);
```

only when `courseId` is present in the rendered result object.

- [ ] **Step 3: If frontend code changed, run a targeted search to confirm both copies stay aligned**

Run:

```powershell
rg -n "查看课程ID .*详情|data-agent-command|QUERY_COURSE_DETAIL" -S .\frontend\dist\agent-chat-panel.js .\major_assignment\src\main\resources\static\agent-chat-panel.js
```

Expected: matching logic appears in both copies.

### Task 4: Verify with focused tests and runtime smoke

**Files:**
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentOrchestratorTest.java`
- Test: `scripts/verify-agent-idea-runtime-smoke.js`

- [ ] **Step 1: Re-run the focused agent tests**

Run:

```powershell
mvn -pl agent-service -am "-Dtest=AgentContextEnrichmentServiceTest,AgentOrchestratorTest,CourseDetailLookupToolTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: PASS

- [ ] **Step 2: Re-run the Agent runtime smoke if the local stack is already up**

Run:

```powershell
node .\scripts\verify-agent-idea-runtime-smoke.js .\.runtime-logs\teacher-session-agent-runtime.json .\.runtime-logs\student-session-agent-runtime.json
```

Expected: PASS

- [ ] **Step 3: Browser-check the teacher Agent page after the fix**

Use the current `http://localhost:5500/teacher-ai-tools.html` page and verify that a teacher can:

1. query courses
2. follow with `查看课程详情`
3. receive `DATA` instead of a missing-slot prompt when only one course is in context
