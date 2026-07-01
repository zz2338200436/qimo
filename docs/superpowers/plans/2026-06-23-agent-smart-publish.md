# Agent Smart Publish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make teacher assignment publishing feel intelligent by using page context and assignment drafts so Agent asks only for truly missing publish targets while keeping final execution validation strict.

**Architecture:** Add a `context` map to Agent chat requests, pass it through controller, streaming service, and orchestrator, then centralize publish-draft inference in a new `AgentAssignmentDraftService`. The draft service enriches `PUBLISH_ASSIGNMENT` slots from page context, pending context, and defaults before the existing course resolution, slot requirement, preview, and execution flow run.

**Tech Stack:** Java 17, Spring Boot, Jackson DTOs, JUnit 5, AssertJ, Mockito, browser-side vanilla JavaScript static assets.

---

## Scope Notes

Current assignment publishing executes against `TeacherAssignmentUpsertRequestDTO`, which requires `courseId` and has no `classIds` field. This plan improves context, defaults, and follow-up prompts now; true class-targeted publishing is a later API/database feature unless an existing course-to-class mapping is reused by assignment service.

The execution layer must remain strict: `AssignmentPublishTool` still rejects missing `title`, `courseId`, `dueDate`, or `maxScore`.

## File Structure

- Modify `agent-service-api/src/main/java/com/_202510007517/platform/agent/api/dto/AgentChatRequestDTO.java`: add `context`.
- Modify `agent-service/src/main/java/com/_202510007517/platform/agent/controller/AgentController.java`: pass request context to normal and streaming paths.
- Modify `agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentChatStreamingService.java`: add stream/chat methods accepting the full request.
- Modify `agent-service/src/main/java/com/_202510007517/platform/agent/service/DefaultAgentChatStreamingService.java`: pass context into orchestrator.
- Modify `agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java`: add context-aware overload and call assignment draft enrichment before course resolution.
- Create `agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentAssignmentDraftService.java`: infer assignment draft slots from page context and user message.
- Modify `agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentSlotRequirementService.java`: allow inferred defaults to satisfy title, due date, and max score; improve publish-assignment prompt.
- Modify `frontend/dist/agent-chat-panel.js` and `major_assignment/src/main/resources/static/agent-chat-panel.js`: include page context in `/api/agent/chat` and `/api/agent/chat/stream` payloads.
- Test `agent-service/src/test/java/com/_202510007517/platform/agent/controller/AgentControllerTest.java`.
- Test `agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentAssignmentDraftServiceTest.java`.
- Test `agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentOrchestratorTest.java`.
- Test `agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentSlotRequirementServiceTest.java`.
- Test `scripts/verify-agent-service-frontend-contract.js` or add assertions there if it already covers chat payload construction.

---

### Task 1: Pass Chat Context Through DTO, Controller, And Streaming

**Files:**
- Modify: `agent-service-api/src/main/java/com/_202510007517/platform/agent/api/dto/AgentChatRequestDTO.java`
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/controller/AgentController.java`
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentChatStreamingService.java`
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/service/DefaultAgentChatStreamingService.java`
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/controller/AgentControllerTest.java`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/service/DefaultAgentChatStreamingServiceTest.java`

- [ ] **Step 1: Write failing controller test for context forwarding**

Add this test to `AgentControllerTest`:

```java
@Test
void chatForwardsRequestContextToOrchestrator() throws Exception {
    AgentChatResponseDTO response = new AgentChatResponseDTO();
    response.setSessionId("session-1");
    response.setResponseType("TEXT");
    response.setMessage("ok");
    when(orchestrator.chat(
            eq(7L),
            eq("TEACHER"),
            eq("session-1"),
            eq("把这个题发布到班级"),
            argThat(context -> context != null
                    && "teacher-question-bank".equals(context.get("page"))
                    && context.containsKey("selectedQuestionIds"))))
            .thenReturn(response);

    mockMvc.perform(post("/api/agent/chat")
                    .header("X-User-Id", "7")
                    .header("X-Active-Role", "TEACHER")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "sessionId": "session-1",
                              "message": "把这个题发布到班级",
                              "context": {
                                "page": "teacher-question-bank",
                                "selectedQuestionIds": [91022]
                              }
                            }
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.message").value("ok"));
}
```

- [ ] **Step 2: Run controller test and verify it fails**

Run:

```powershell
mvn -pl agent-service -Dtest=AgentControllerTest#chatForwardsRequestContextToOrchestrator test
```

Expected: compilation fails because `AgentChatRequestDTO` has no `context` property and `AgentOrchestrator.chat` has no context-aware overload.

- [ ] **Step 3: Add context property to request DTO**

Update `AgentChatRequestDTO`:

```java
package com._202510007517.platform.agent.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.LinkedHashMap;
import java.util.Map;

public class AgentChatRequestDTO {
    private String sessionId;

    @NotBlank
    private String message;

    private Map<String, Object> context = new LinkedHashMap<>();

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getContext() {
        return context == null ? Map.of() : context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context == null ? new LinkedHashMap<>() : new LinkedHashMap<>(context);
    }
}
```

- [ ] **Step 4: Add context-aware orchestrator overload**

In `AgentOrchestrator`, keep the current signature for existing tests and add a delegating overload:

```java
public AgentChatResponseDTO chat(Long userId, String userRole, String sessionId, String message) {
    return chat(userId, userRole, sessionId, message, Map.of());
}

@Transactional
public AgentChatResponseDTO chat(Long userId, String userRole, String sessionId, String message,
                                 Map<String, Object> pageContext) {
    AgentSessionEntity session = sessionService.resolveSession(userId, userRole, sessionId);
    sessionService.saveMessage(session.getId(), "USER", message, null);
    RecognizedIntent recognizedIntent = mergePendingContext(session, intentRecognitionService.recognize(message));
    AgentChatResponseDTO response;
    // Move the current body of chat(...) here. Task 3 will insert draft enrichment.
}
```

After moving the body, replace the old method body entirely with the delegation. Keep imports compiling by adding `java.util.Map` if needed.

- [ ] **Step 5: Forward context from controller**

Change both controller methods:

```java
return ResponseResult.success(orchestrator.chat(
        userId,
        role,
        request.getSessionId(),
        request.getMessage(),
        request.getContext()));
```

For streaming:

```java
return streamingService.streamChat(userId, role, request);
```

- [ ] **Step 6: Update streaming service interface and implementation**

Replace `AgentChatStreamingService` with:

```java
public interface AgentChatStreamingService {
    SseEmitter streamChat(Long userId, String userRole, AgentChatRequestDTO request);

    SseEmitter streamChat(Long userId, String userRole, String sessionId, String message);

    AgentChatResponseDTO chat(Long userId, String userRole, String sessionId, String message);

    AgentChatResponseDTO chat(AgentChatRequestDTO request, Long userId, String userRole);
}
```

In `DefaultAgentChatStreamingService`, implement:

```java
@Override
public SseEmitter streamChat(Long userId, String userRole, String sessionId, String message) {
    AgentChatRequestDTO request = new AgentChatRequestDTO();
    request.setSessionId(sessionId);
    request.setMessage(message);
    return streamChat(userId, userRole, request);
}

@Override
public SseEmitter streamChat(Long userId, String userRole, AgentChatRequestDTO request) {
    SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
    try {
        EXECUTOR.execute(() -> {
            try {
                sendEvent(emitter, "start", startPayload(request.getSessionId()));
                AgentChatResponseDTO response = orchestrator.chat(
                        userId,
                        userRole,
                        request.getSessionId(),
                        request.getMessage(),
                        request.getContext());
                sendEvent(emitter, "session", Map.of("sessionId", response.getSessionId()));
                emitResponse(emitter, response);
                sendEvent(emitter, "done", Map.of());
                emitter.complete();
            } catch (Exception ex) {
                completeWithError(emitter, ex);
            }
        });
    } catch (RejectedExecutionException ex) {
        completeWithError(emitter, new IllegalStateException("Agent stream is busy. Please retry later.", ex));
    }
    return emitter;
}

@Override
public AgentChatResponseDTO chat(AgentChatRequestDTO request, Long userId, String userRole) {
    return orchestrator.chat(userId, userRole, request.getSessionId(), request.getMessage(), request.getContext());
}
```

- [ ] **Step 7: Run focused tests**

Run:

```powershell
mvn -pl agent-service -Dtest=AgentControllerTest,DefaultAgentChatStreamingServiceTest test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add -- agent-service-api/src/main/java/com/_202510007517/platform/agent/api/dto/AgentChatRequestDTO.java agent-service/src/main/java/com/_202510007517/platform/agent/controller/AgentController.java agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentChatStreamingService.java agent-service/src/main/java/com/_202510007517/platform/agent/service/DefaultAgentChatStreamingService.java agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java agent-service/src/test/java/com/_202510007517/platform/agent/controller/AgentControllerTest.java agent-service/src/test/java/com/_202510007517/platform/agent/service/DefaultAgentChatStreamingServiceTest.java
git commit -m "feat: pass agent chat page context"
```

---

### Task 2: Add Assignment Draft Inference Service

**Files:**
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentAssignmentDraftService.java`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentAssignmentDraftServiceTest.java`

- [ ] **Step 1: Write failing unit tests**

Create `AgentAssignmentDraftServiceTest.java`:

```java
package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.RecognizedIntent;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentAssignmentDraftServiceTest {

    private final AgentAssignmentDraftService service = new AgentAssignmentDraftService(
            Clock.fixed(Instant.parse("2026-06-23T02:00:00Z"), ZoneId.of("Asia/Shanghai")));

    @Test
    void enrichesSelectedQuestionPublishDraftFromPageContext() {
        RecognizedIntent input = new RecognizedIntent(
                AgentIntent.PUBLISH_ASSIGNMENT,
                0.9,
                Map.of(),
                List.of());

        RecognizedIntent result = service.enrich(input, "把这个题发布到班级", Map.of(
                "page", "teacher-question-bank",
                "selectedQuestionIds", List.of(91022),
                "selectedQuestionScore", 2,
                "selectedQuestionType", "TRUE_FALSE",
                "selectedQuestionContent", "服务注册中心通常保存服务实例的地址、端口和健康状态等信息。",
                "currentCourseId", 12
        ));

        assertThat(result.slots())
                .containsEntry("selectionMode", "SELECTED_QUESTIONS")
                .containsEntry("questionIds", List.of(91022L))
                .containsEntry("courseId", 12L)
                .containsEntry("maxScore", 2)
                .containsEntry("dueDate", "2026-06-30 23:59:59");
        assertThat(String.valueOf(result.slots().get("title"))).contains("服务注册中心");
        assertThat(String.valueOf(result.slots().get("content"))).contains("服务注册中心通常保存");
    }

    @Test
    void enrichesRandomQuestionBankDraftWhenUserSaysAllRandom() {
        RecognizedIntent input = new RecognizedIntent(
                AgentIntent.PUBLISH_ASSIGNMENT,
                0.9,
                Map.of("courseId", 12L),
                List.of());

        RecognizedIntent result = service.enrich(input, "全部随机", Map.of());

        assertThat(result.slots())
                .containsEntry("selectionMode", "RANDOM_QUESTION_BANK")
                .containsEntry("title", "随机题库练习")
                .containsEntry("maxScore", 100)
                .containsEntry("dueDate", "2026-06-30 23:59:59");
    }

    @Test
    void keepsExplicitUserSlotsOverDefaults() {
        RecognizedIntent input = new RecognizedIntent(
                AgentIntent.PUBLISH_ASSIGNMENT,
                0.9,
                Map.of(
                        "title", "自定义标题",
                        "maxScore", 20,
                        "dueDate", "明晚",
                        "courseId", 12L),
                List.of());

        RecognizedIntent result = service.enrich(input, "把这个题发布到班级", Map.of(
                "selectedQuestionIds", List.of(91022),
                "selectedQuestionScore", 2,
                "selectedQuestionContent", "服务注册中心通常保存服务实例的地址、端口和健康状态等信息。"
        ));

        assertThat(result.slots())
                .containsEntry("title", "自定义标题")
                .containsEntry("maxScore", 20)
                .containsEntry("dueDate", "明晚");
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run:

```powershell
mvn -pl agent-service -Dtest=AgentAssignmentDraftServiceTest test
```

Expected: compilation fails because `AgentAssignmentDraftService` does not exist.

- [ ] **Step 3: Implement draft service**

Create `AgentAssignmentDraftService.java`:

```java
package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.RecognizedIntent;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AgentAssignmentDraftService {
    static final String SELECTED_QUESTIONS = "SELECTED_QUESTIONS";
    static final String RANDOM_QUESTION_BANK = "RANDOM_QUESTION_BANK";

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Clock clock;

    public AgentAssignmentDraftService() {
        this(Clock.systemDefaultZone());
    }

    AgentAssignmentDraftService(Clock clock) {
        this.clock = clock;
    }

    public RecognizedIntent enrich(RecognizedIntent intent, String message, Map<String, Object> pageContext) {
        if (intent.intent() != AgentIntent.PUBLISH_ASSIGNMENT) {
            return intent;
        }
        Map<String, Object> context = pageContext == null ? Map.of() : pageContext;
        Map<String, Object> slots = new LinkedHashMap<>(intent.slots());

        applyCourseContext(slots, context);
        applySelectedQuestionContext(slots, context);
        applyRandomContext(slots, message);
        applyDefaultDueDate(slots);

        return new RecognizedIntent(intent.intent(), intent.confidence(), slots, intent.missingSlots());
    }

    private void applyCourseContext(Map<String, Object> slots, Map<String, Object> context) {
        putLongIfMissing(slots, "courseId", context.get("currentCourseId"));
        putLongIfMissing(slots, "classId", context.get("currentClassId"));
        Object questionFilter = context.get("questionFilter");
        if (questionFilter instanceof Map<?, ?> filter) {
            putLongIfMissing(slots, "courseId", filter.get("courseId"));
            putLongIfMissing(slots, "classId", filter.get("classId"));
        }
    }

    private void applySelectedQuestionContext(Map<String, Object> slots, Map<String, Object> context) {
        List<Long> questionIds = longList(context.get("selectedQuestionIds"));
        if (questionIds.isEmpty()) {
            Long singleId = asLong(context.get("selectedQuestionId"));
            if (singleId != null) {
                questionIds = List.of(singleId);
            }
        }
        if (questionIds.isEmpty()) {
            return;
        }
        slots.putIfAbsent("selectionMode", SELECTED_QUESTIONS);
        slots.putIfAbsent("questionIds", questionIds);
        Object content = context.get("selectedQuestionContent");
        if (content != null && !String.valueOf(content).isBlank()) {
            slots.putIfAbsent("content", String.valueOf(content));
            slots.putIfAbsent("title", titleFromQuestion(String.valueOf(content), context.get("selectedQuestionType")));
        }
        Integer score = asInteger(context.get("selectedQuestionScore"));
        if (score != null) {
            slots.putIfAbsent("maxScore", score);
        }
    }

    private void applyRandomContext(Map<String, Object> slots, String message) {
        String text = message == null ? "" : message;
        if (!text.contains("随机") || !text.contains("全部")) {
            return;
        }
        slots.putIfAbsent("selectionMode", RANDOM_QUESTION_BANK);
        slots.putIfAbsent("title", "随机题库练习");
        slots.putIfAbsent("maxScore", 100);
        slots.putIfAbsent("content", "随机题库练习");
    }

    private void applyDefaultDueDate(Map<String, Object> slots) {
        if (hasValue(slots.get("dueDate"))) {
            return;
        }
        LocalDate dueDate = LocalDate.now(clock).plusDays(7);
        slots.put("dueDate", LocalDateTime.of(dueDate, LocalTime.of(23, 59, 59)).format(DATE_TIME));
    }

    private String titleFromQuestion(String content, Object type) {
        String compact = content.replaceAll("[，。！？；：,.!?;:\\s]+", "");
        String keyword = compact.length() <= 8 ? compact : compact.substring(0, 8);
        String suffix = "TRUE_FALSE".equals(String.valueOf(type)) ? "判断题练习" : "题目练习";
        return keyword + suffix;
    }

    private void putLongIfMissing(Map<String, Object> slots, String key, Object value) {
        if (hasValue(slots.get(key))) {
            return;
        }
        Long parsed = asLong(value);
        if (parsed != null) {
            slots.put(key, parsed);
        }
    }

    private List<Long> longList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (Object item : iterable) {
            Long id = asLong(item);
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && text.matches("\\d+")) {
            return Long.valueOf(text);
        }
        return null;
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && text.matches("\\d+")) {
            return Integer.valueOf(text);
        }
        return null;
    }

    private boolean hasValue(Object value) {
        return value != null && !String.valueOf(value).isBlank();
    }
}
```

- [ ] **Step 4: Run draft service tests**

Run:

```powershell
mvn -pl agent-service -Dtest=AgentAssignmentDraftServiceTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add -- agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentAssignmentDraftService.java agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentAssignmentDraftServiceTest.java
git commit -m "feat: infer assignment publish drafts"
```

---

### Task 3: Use Draft Inference In Orchestrator Before Slot Checks

**Files:**
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentOrchestratorTest.java`

- [ ] **Step 1: Write failing orchestrator tests**

Add to `AgentOrchestratorTest`:

```java
@Test
void usesPageContextToCreateSelectedQuestionAssignmentPreview() {
    stubJavaCourse();

    AgentChatResponseDTO response = orchestrator.chat(
            7L,
            "TEACHER",
            null,
            "把这个题发布到班级",
            Map.of(
                    "page", "teacher-question-bank",
                    "selectedQuestionIds", List.of(91022),
                    "selectedQuestionScore", 2,
                    "selectedQuestionType", "TRUE_FALSE",
                    "selectedQuestionContent", "服务注册中心通常保存服务实例的地址、端口和健康状态等信息。",
                    "currentCourseId", 12
            ));

    assertThat(response.getResponseType()).isEqualTo("ACTION_PREVIEW");
    assertThat(response.getActionPreview().getIntent()).isEqualTo("PUBLISH_ASSIGNMENT");
    assertThat(response.getActionPreview().getPreview())
            .containsEntry("courseId", 12L)
            .containsEntry("questionIds", List.of(91022L))
            .containsEntry("maxScore", 2)
            .containsEntry("selectionMode", "SELECTED_QUESTIONS");
    assertThat(String.valueOf(response.getActionPreview().getPreview().get("title"))).contains("服务注册中心");
}

@Test
void selectedQuestionPublishWithoutCourseOnlyAsksForPublishTarget() {
    AgentChatResponseDTO response = orchestrator.chat(
            7L,
            "TEACHER",
            null,
            "把这个题发布到班级",
            Map.of(
                    "selectedQuestionIds", List.of(91022),
                    "selectedQuestionScore", 2,
                    "selectedQuestionType", "TRUE_FALSE",
                    "selectedQuestionContent", "服务注册中心通常保存服务实例的地址、端口和健康状态等信息。"
            ));

    assertThat(response.getResponseType()).isEqualTo("TEXT");
    assertThat(response.getMessage()).contains("课程或班级");
    assertThat(response.getMessage())
            .doesNotContain("标题")
            .doesNotContain("截止时间")
            .doesNotContain("满分");
    assertThat(actionRepository.findAll()).isEmpty();
}
```

- [ ] **Step 2: Run orchestrator tests and verify failure**

Run:

```powershell
mvn -pl agent-service -Dtest=AgentOrchestratorTest#usesPageContextToCreateSelectedQuestionAssignmentPreview,AgentOrchestratorTest#selectedQuestionPublishWithoutCourseOnlyAsksForPublishTarget test
```

Expected: fails because `AgentAssignmentDraftService` is not injected or used.

- [ ] **Step 3: Inject and call draft service**

In `AgentOrchestrator`, add a field and constructor parameter:

```java
private final AgentAssignmentDraftService assignmentDraftService;
```

Constructor parameter:

```java
AgentAssignmentDraftService assignmentDraftService,
```

Assignment:

```java
this.assignmentDraftService = assignmentDraftService;
```

In the context-aware `chat` method, after permission checks and before `contextEnrichmentService.enrich(...)`, add:

```java
recognizedIntent = assignmentDraftService.enrich(recognizedIntent, message, pageContext);
```

The order should be:

```java
permissionPolicy.assertAllowed(userId, userRole, recognizedIntent.intent());
if (recognizedIntent.intent() == AgentIntent.QUERY_RAG_KNOWLEDGE) {
    ...
}
recognizedIntent = assignmentDraftService.enrich(recognizedIntent, message, pageContext);
recognizedIntent = contextEnrichmentService.enrich(userId, userRole, recognizedIntent);
var missingSlots = slotRequirementService.missingSlots(recognizedIntent, message);
```

- [ ] **Step 4: Ensure generated-question pending context still works**

Keep `saveGeneratedQuestionsForAssignment(...)` unchanged. It should still save `title` and `content`; Task 2 defaults fill `dueDate` and `maxScore` only when not explicit.

- [ ] **Step 5: Run focused orchestrator tests**

Run:

```powershell
mvn -pl agent-service -Dtest=AgentOrchestratorTest#usesPageContextToCreateSelectedQuestionAssignmentPreview,AgentOrchestratorTest#selectedQuestionPublishWithoutCourseOnlyAsksForPublishTarget,AgentOrchestratorTest#reusesGeneratedQuestionsAsAssignmentContentInFollowUpPublish test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add -- agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentOrchestratorTest.java
git commit -m "feat: use assignment publish draft context"
```

---

### Task 4: Improve Publish Assignment Missing-Slot Rules And Prompt

**Files:**
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentSlotRequirementService.java`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentSlotRequirementServiceTest.java`

- [ ] **Step 1: Write failing slot requirement tests**

Add to `AgentSlotRequirementServiceTest`:

```java
@Test
void selectedQuestionDraftOnlyRequiresPublishTargetWhenDefaultsArePresent() {
    RecognizedIntent intent = new RecognizedIntent(
            AgentIntent.PUBLISH_ASSIGNMENT,
            0.9,
            Map.of(
                    "selectionMode", "SELECTED_QUESTIONS",
                    "questionIds", List.of(91022L),
                    "title", "服务注册中心判断题练习",
                    "dueDate", "2026-06-30 23:59:59",
                    "maxScore", 2
            ),
            List.of());

    List<String> missingSlots = service.missingSlots(intent, "把这个题发布到班级");

    assertThat(missingSlots).containsExactly("课程或班级");
}

@Test
void publishAssignmentPromptAsksOnlyForPublishTarget() {
    String prompt = service.buildPrompt(AgentIntent.PUBLISH_ASSIGNMENT, List.of("课程或班级"));

    assertThat(prompt).isEqualTo("我已准备好作业内容、标题、截止时间和满分。还需要选择发布课程或班级。");
}
```

- [ ] **Step 2: Run slot tests and verify failure**

Run:

```powershell
mvn -pl agent-service -Dtest=AgentSlotRequirementServiceTest#selectedQuestionDraftOnlyRequiresPublishTargetWhenDefaultsArePresent,AgentSlotRequirementServiceTest#publishAssignmentPromptAsksOnlyForPublishTarget test
```

Expected: prompt and missing slot labels do not match.

- [ ] **Step 3: Adjust publish-assignment requirement**

In `AgentSlotRequirementService`, change `PUBLISH_ASSIGNMENT` block to:

```java
case PUBLISH_ASSIGNMENT -> {
    requirePublishTarget(missing, slots);
    require(missing, slots, "标题", "title");
    requireDueDate(missing, slots, message);
    require(missing, slots, "满分", "maxScore");
    requireNumberIfPresent(missing, slots, "课程ID", "courseId");
    requireNumberIfPresent(missing, slots, "班级ID", "classId");
    requireNumberIfPresent(missing, slots, "满分", "maxScore");
}
```

Add helper:

```java
private void requirePublishTarget(Set<String> missing, Map<String, Object> slots) {
    if (!hasAny(slots, "courseId", "courseName", "classId", "className")) {
        missing.add("课程或班级");
    }
}
```

Update `removeSatisfiedMissingSlots`:

```java
removeIfSatisfied(missing, slots, "课程或班级", "courseId", "courseName", "classId", "className");
removeIfSatisfied(missing, slots, "班级", "classId", "className");
```

Update `describeMissingSlot`:

```java
case "课程或班级" -> "发布课程或班级";
case "classId", "className" -> "班级";
```

Update `buildPrompt` before the generic return:

```java
if (intent == AgentIntent.PUBLISH_ASSIGNMENT && missingSlots.size() == 1
        && missingSlots.contains("课程或班级")) {
    return "我已准备好作业内容、标题、截止时间和满分。还需要选择发布课程或班级。";
}
```

- [ ] **Step 4: Run slot tests**

Run:

```powershell
mvn -pl agent-service -Dtest=AgentSlotRequirementServiceTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add -- agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentSlotRequirementService.java agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentSlotRequirementServiceTest.java
git commit -m "feat: ask for publish target only when draft has defaults"
```

---

### Task 5: Send Page Context From Agent Chat Panel

**Files:**
- Modify: `frontend/dist/agent-chat-panel.js`
- Modify: `major_assignment/src/main/resources/static/agent-chat-panel.js`
- Test: `scripts/verify-agent-service-frontend-contract.js`

- [ ] **Step 1: Add failing frontend contract assertion**

Open `scripts/verify-agent-service-frontend-contract.js`. Add an assertion that both static chat panels contain these strings:

```js
assertContains(frontendAgentChatPanel, 'buildPageContext()');
assertContains(frontendAgentChatPanel, 'context: this.buildPageContext()');
assertContains(staticAgentChatPanel, 'buildPageContext()');
assertContains(staticAgentChatPanel, 'context: this.buildPageContext()');
```

If the script does not currently load these files, add:

```js
const frontendAgentChatPanel = fs.readFileSync(path.join(root, 'frontend/dist/agent-chat-panel.js'), 'utf8');
const staticAgentChatPanel = fs.readFileSync(path.join(root, 'major_assignment/src/main/resources/static/agent-chat-panel.js'), 'utf8');
```

- [ ] **Step 2: Run contract script and verify failure**

Run:

```powershell
node .\scripts\verify-agent-service-frontend-contract.js
```

Expected: FAIL because `buildPageContext()` is not present.

- [ ] **Step 3: Add context builder to both agent chat panel files**

Inside the `AgentChatPanel` class, add this method before `send(message)` in both files:

```js
        buildPageContext() {
            const context = {
                page: document.body?.dataset?.page || document.documentElement?.dataset?.page || location.pathname
            };
            const selectedQuestion = window.agentSelectedQuestion || window.currentQuestion || null;
            if (selectedQuestion && typeof selectedQuestion === 'object') {
                if (selectedQuestion.id) {
                    context.selectedQuestionId = Number(selectedQuestion.id);
                    context.selectedQuestionIds = [Number(selectedQuestion.id)];
                }
                if (selectedQuestion.score || selectedQuestion.points) {
                    context.selectedQuestionScore = Number(selectedQuestion.score || selectedQuestion.points);
                }
                if (selectedQuestion.type || selectedQuestion.questionType) {
                    context.selectedQuestionType = selectedQuestion.type || selectedQuestion.questionType;
                }
                if (selectedQuestion.content || selectedQuestion.title) {
                    context.selectedQuestionContent = selectedQuestion.content || selectedQuestion.title;
                }
            }
            const selectedQuestionIds = window.agentSelectedQuestionIds || window.selectedQuestionIds;
            if (Array.isArray(selectedQuestionIds) && selectedQuestionIds.length > 0) {
                context.selectedQuestionIds = selectedQuestionIds
                    .map(item => Number(item))
                    .filter(item => Number.isFinite(item));
            }
            const currentCourseId = window.agentCurrentCourseId || window.currentCourseId;
            if (currentCourseId) {
                context.currentCourseId = Number(currentCourseId);
            }
            const currentClassId = window.agentCurrentClassId || window.currentClassId;
            if (currentClassId) {
                context.currentClassId = Number(currentClassId);
            }
            const questionFilter = window.agentQuestionFilter || window.currentQuestionFilter;
            if (questionFilter && typeof questionFilter === 'object') {
                context.questionFilter = { ...questionFilter };
            }
            return context;
        }
```

- [ ] **Step 4: Include context in non-stream and stream payloads**

In `send(message)`, change fallback request body:

```js
const requestBody = {
    message,
    sessionId: this.sessionId,
    context: this.buildPageContext()
};
const payload = await this.request('/api/agent/chat', requestBody, this.currentController);
```

In `sendStream(message)`, change:

```js
const payload = await this.requestStream('/api/agent/chat/stream', {
    message,
    sessionId: this.sessionId,
    context: this.buildPageContext()
}, this.currentController);
```

Apply the same edits to both `frontend/dist/agent-chat-panel.js` and `major_assignment/src/main/resources/static/agent-chat-panel.js`.

- [ ] **Step 5: Run frontend contract**

Run:

```powershell
node .\scripts\verify-agent-service-frontend-contract.js
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add -- frontend/dist/agent-chat-panel.js major_assignment/src/main/resources/static/agent-chat-panel.js scripts/verify-agent-service-frontend-contract.js
git commit -m "feat: send agent page context from chat panel"
```

---

### Task 6: Integration Verification And Regression Sweep

**Files:**
- Modify only if tests reveal defects in files touched by Tasks 1-5.

- [ ] **Step 1: Run focused Agent service tests**

Run:

```powershell
mvn -pl agent-service -Dtest=AgentControllerTest,DefaultAgentChatStreamingServiceTest,AgentAssignmentDraftServiceTest,AgentSlotRequirementServiceTest,AgentOrchestratorTest test
```

Expected: PASS.

- [ ] **Step 2: Run frontend contract**

Run:

```powershell
node .\scripts\verify-agent-service-frontend-contract.js
```

Expected: PASS.

- [ ] **Step 3: Run existing browser smoke if services are available**

If local Java services and frontend dev server are already running, run:

```powershell
node .\scripts\verify-agent-frontend-browser-smoke.js
```

Expected: PASS. If services are not running, record that browser smoke was not run and do not start the whole distributed stack as part of this task.

- [ ] **Step 4: Inspect git diff for unrelated changes**

Run:

```powershell
git diff --stat
git diff -- agent-service-api/src/main/java/com/_202510007517/platform/agent/api/dto/AgentChatRequestDTO.java agent-service/src/main/java/com/_202510007517/platform/agent/controller/AgentController.java agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentChatStreamingService.java agent-service/src/main/java/com/_202510007517/platform/agent/service/DefaultAgentChatStreamingService.java agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentAssignmentDraftService.java agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentSlotRequirementService.java frontend/dist/agent-chat-panel.js major_assignment/src/main/resources/static/agent-chat-panel.js
```

Expected: diff only contains smart publish context, draft inference, slot prompt, and tests.

- [ ] **Step 5: Final commit if Task 6 required fixes**

If Task 6 changed files, commit:

```powershell
git add -- <changed-files>
git commit -m "fix: stabilize smart publish agent flow"
```

If Task 6 did not change files, do not create an empty commit.

## Self-Review

- Spec coverage: covered page context, draft defaults, prompt narrowing, confirmation preservation, strict execution validation, frontend payload, and tests.
- Known limitation: class-specific assignment execution is not implemented because `TeacherAssignmentUpsertRequestDTO` does not support `classIds`; the plan avoids pretending otherwise.
- Placeholder scan: no placeholder markers remain in executable steps.
- Type consistency: `context` is a `Map<String, Object>` across DTO, controller, streaming service, and orchestrator. Draft service returns `RecognizedIntent` and keeps slot keys compatible with existing tool and preview flow.
