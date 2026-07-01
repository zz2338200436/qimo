# Agent LangChain4j Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Introduce LangChain4j incrementally into the existing Agent stack by adding assistant-specific AiServices and memory seams while preserving the current business orchestration, session persistence, confirmation flow, and deterministic tool execution.

**Architecture:** Keep `AgentOrchestrator` as the top-level business router. Add a dedicated assistant capability layer for general chat and knowledge answering, then attach LangChain4j `AiServices`, assistant-scoped memory, and later true streaming behind that seam. Business write workflows continue using the current orchestrator, slot rules, preview generation, confirmation, and `ToolRegistry`.

**Tech Stack:** Java 17, Spring Boot, LangChain4j, Jackson, JUnit 5, AssertJ, Mockito, Redis-ready Spring configuration, existing SSE and static frontend contract scripts.

---

## Scope Notes

This plan intentionally starts with the lowest-risk migration slice:

- Phase 1: create a clean assistant capability boundary
- Phase 2: move general chat and RAG answering behind LangChain4j-style assistant services

The plan does not yet migrate:

- publish-assignment execution
- write-side tool calling
- business slot state into LangChain4j memory
- confirmation workflows into model-driven decisions

Those remain custom by design.

## File Structure

- Create `agent-service/src/main/java/com/_202510007517/platform/agent/assistant/AgentAssistantType.java`: enumerates assistant categories such as general and knowledge.
- Create `agent-service/src/main/java/com/_202510007517/platform/agent/assistant/AgentAssistantRequest.java`: normalized assistant request envelope carrying user, role, session, message, and memory key.
- Create `agent-service/src/main/java/com/_202510007517/platform/agent/assistant/AssistantConversationService.java`: shared assistant boundary used by orchestrator.
- Create `agent-service/src/main/java/com/_202510007517/platform/agent/assistant/FallbackAssistantConversationService.java`: deterministic fallback implementation for tests and disabled-LLM mode.
- Create `agent-service/src/main/java/com/_202510007517/platform/agent/assistant/LangChain4jAssistantConversationService.java`: production assistant adapter using LangChain4j-backed collaborators.
- Create `agent-service/src/main/java/com/_202510007517/platform/agent/assistant/GeneralAssistant.java`: AiServices interface for general chat.
- Create `agent-service/src/main/java/com/_202510007517/platform/agent/assistant/KnowledgeAssistant.java`: AiServices interface for knowledge answers.
- Create `agent-service/src/main/java/com/_202510007517/platform/agent/assistant/AssistantMemoryKeyFactory.java`: derives assistant-scoped memory ids from session id and assistant type.
- Create `agent-service/src/main/java/com/_202510007517/platform/agent/config/AgentAssistantConfiguration.java`: wires AiServices collaborators while preserving existing fallback behavior.
- Modify `agent-service/src/main/java/com/_202510007517/platform/agent/service/GeneralChatService.java`: convert to an adapter over the new assistant boundary or replace usages.
- Modify `agent-service/src/main/java/com/_202510007517/platform/agent/service/LlmGeneralChatService.java`: either remove direct orchestrator use or delegate into assistant services.
- Modify `agent-service/src/main/java/com/_202510007517/platform/agent/service/FallbackGeneralChatService.java`: align fallback message path with assistant boundary.
- Modify `agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagKnowledgeService.java`: expose a retriever-oriented answer path consumable by the assistant layer.
- Modify `agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java`: delegate general chat and RAG answers through the new assistant boundary while preserving current business control flow.
- Modify `agent-service/src/main/resources/application.yml`: add assistant-specific configuration switches and defaults.
- Test `agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentOrchestratorTest.java`.
- Create `agent-service/src/test/java/com/_202510007517/platform/agent/assistant/LangChain4jAssistantConversationServiceTest.java`.
- Create `agent-service/src/test/java/com/_202510007517/platform/agent/assistant/AssistantMemoryKeyFactoryTest.java`.
- Modify `agent-service/src/test/java/com/_202510007517/platform/agent/rag/RagKnowledgeServiceTest.java`.
- Modify `agent-service/src/test/java/com/_202510007517/platform/agent/service/LlmIntentRecognitionServiceTest.java` only if bean wiring changes force updates.

---

### Task 1: Create an Assistant Capability Boundary

**Files:**
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/assistant/AgentAssistantType.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/assistant/AgentAssistantRequest.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/assistant/AssistantConversationService.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/assistant/FallbackAssistantConversationService.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/assistant/AssistantMemoryKeyFactory.java`
- Create: `agent-service/src/test/java/com/_202510007517/platform/agent/assistant/AssistantMemoryKeyFactoryTest.java`

- [ ] **Step 1: Write a failing test for assistant memory key derivation**

Create `AssistantMemoryKeyFactoryTest.java`:

```java
package com._202510007517.platform.agent.assistant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssistantMemoryKeyFactoryTest {

    private final AssistantMemoryKeyFactory factory = new AssistantMemoryKeyFactory();

    @Test
    void buildsScopedMemoryKeyFromSessionAndAssistantType() {
        String key = factory.build("42", AgentAssistantType.GENERAL);

        assertThat(key).isEqualTo("agent-session:42:assistant:GENERAL");
    }

    @Test
    void fallsBackWhenSessionIdMissing() {
        String key = factory.build(null, AgentAssistantType.KNOWLEDGE);

        assertThat(key).isEqualTo("agent-session:anonymous:assistant:KNOWLEDGE");
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
mvn -pl agent-service -Dtest=AssistantMemoryKeyFactoryTest test
```

Expected: FAIL because `AssistantMemoryKeyFactory` and the assistant package do not exist.

- [ ] **Step 3: Add assistant type enum**

Create `AgentAssistantType.java`:

```java
package com._202510007517.platform.agent.assistant;

public enum AgentAssistantType {
    GENERAL,
    KNOWLEDGE
}
```

- [ ] **Step 4: Add normalized assistant request object**

Create `AgentAssistantRequest.java`:

```java
package com._202510007517.platform.agent.assistant;

public record AgentAssistantRequest(
        Long userId,
        String userRole,
        String sessionId,
        String memoryId,
        String message
) {
}
```

- [ ] **Step 5: Add memory key factory**

Create `AssistantMemoryKeyFactory.java`:

```java
package com._202510007517.platform.agent.assistant;

import org.springframework.stereotype.Component;

@Component
public class AssistantMemoryKeyFactory {

    public String build(String sessionId, AgentAssistantType type) {
        String safeSessionId = (sessionId == null || sessionId.isBlank()) ? "anonymous" : sessionId.trim();
        return "agent-session:" + safeSessionId + ":assistant:" + type.name();
    }
}
```

- [ ] **Step 6: Add assistant boundary interface**

Create `AssistantConversationService.java`:

```java
package com._202510007517.platform.agent.assistant;

public interface AssistantConversationService {

    String reply(AgentAssistantType type, AgentAssistantRequest request);
}
```

- [ ] **Step 7: Add deterministic fallback assistant implementation**

Create `FallbackAssistantConversationService.java`:

```java
package com._202510007517.platform.agent.assistant;

import org.springframework.stereotype.Service;

@Service
public class FallbackAssistantConversationService implements AssistantConversationService {
    private static final String GENERAL_FALLBACK = "暂时还不能处理这个请求。";
    private static final String KNOWLEDGE_FALLBACK = "知识库问答暂未启用。";

    @Override
    public String reply(AgentAssistantType type, AgentAssistantRequest request) {
        if (type == AgentAssistantType.KNOWLEDGE) {
            return KNOWLEDGE_FALLBACK;
        }
        return GENERAL_FALLBACK;
    }
}
```

- [ ] **Step 8: Run the new assistant boundary test**

Run:

```powershell
mvn -pl agent-service -Dtest=AssistantMemoryKeyFactoryTest test
```

Expected: PASS.

- [ ] **Step 9: Commit**

```powershell
git add -- agent-service/src/main/java/com/_202510007517/platform/agent/assistant/AgentAssistantType.java agent-service/src/main/java/com/_202510007517/platform/agent/assistant/AgentAssistantRequest.java agent-service/src/main/java/com/_202510007517/platform/agent/assistant/AssistantConversationService.java agent-service/src/main/java/com/_202510007517/platform/agent/assistant/FallbackAssistantConversationService.java agent-service/src/main/java/com/_202510007517/platform/agent/assistant/AssistantMemoryKeyFactory.java agent-service/src/test/java/com/_202510007517/platform/agent/assistant/AssistantMemoryKeyFactoryTest.java
git commit -m "feat: add agent assistant conversation boundary"
```

---

### Task 2: Route General Chat Through the Assistant Boundary

**Files:**
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java`
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/service/GeneralChatService.java`
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/service/LlmGeneralChatService.java`
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/service/FallbackGeneralChatService.java`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentOrchestratorTest.java`

- [ ] **Step 1: Add a failing orchestrator test for general assistant delegation**

Add to `AgentOrchestratorTest.java`:

```java
@Test
void unknownIntentUsesGeneralAssistantBoundary() {
    when(intentRecognitionService.recognize("你是谁"))
            .thenReturn(new RecognizedIntent(AgentIntent.UNKNOWN, 0.2, Map.of()));
    when(generalChatService.reply(7L, "TEACHER", "你是谁"))
            .thenReturn("我是教学管理平台内置的 AI 助手。");

    AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null, "你是谁");

    assertThat(response.getResponseType()).isEqualTo("TEXT");
    assertThat(response.getMessage()).isEqualTo("我是教学管理平台内置的 AI 助手。");
}
```

- [ ] **Step 2: Run the test to verify current behavior is covered**

Run:

```powershell
mvn -pl agent-service -Dtest=AgentOrchestratorTest#unknownIntentUsesGeneralAssistantBoundary test
```

Expected: PASS or near-pass under current behavior. This establishes the baseline before internal refactor.

- [ ] **Step 3: Convert `GeneralChatService` into a small adapter over the new boundary**

Replace `GeneralChatService.java` with:

```java
package com._202510007517.platform.agent.service;

public interface GeneralChatService {
    String reply(Long userId, String userRole, String sessionId, String message);

    default String reply(Long userId, String userRole, String message) {
        return reply(userId, userRole, null, message);
    }
}
```

- [ ] **Step 4: Update fallback general chat service**

Replace `FallbackGeneralChatService.java` with:

```java
package com._202510007517.platform.agent.service;

public class FallbackGeneralChatService implements GeneralChatService {
    private static final String FALLBACK_MESSAGE = "暂时还不能处理这个请求。";

    @Override
    public String reply(Long userId, String userRole, String sessionId, String message) {
        return FALLBACK_MESSAGE;
    }
}
```

- [ ] **Step 5: Update LLM general chat service signature**

In `LlmGeneralChatService.java`, change the method signature only:

```java
@Override
public String reply(Long userId, String userRole, String sessionId, String message) {
    String prompt = buildPrompt(userRole, message);
    log.info("agent general chat prompt: {}", dataMaskingPolicy.maskText(prompt));
    try {
        String output = chatModel.chat(prompt);
        log.info("agent general chat output: {}", dataMaskingPolicy.maskText(output));
        if (output == null || output.isBlank()) {
            return FALLBACK_MESSAGE;
        }
        return output.trim();
    } catch (RuntimeException ex) {
        log.info("agent general chat failed: reason={}", ex.getClass().getSimpleName());
        return FALLBACK_MESSAGE;
    }
}
```

- [ ] **Step 6: Update orchestrator to pass session id to general chat**

In `AgentOrchestrator.java`, change the `UNKNOWN` branch:

```java
response = textResponse(session, generalChatService.reply(
        userId,
        userRole,
        String.valueOf(session.getId()),
        message));
```

- [ ] **Step 7: Run focused orchestrator and general chat tests**

Run:

```powershell
mvn -pl agent-service -Dtest=AgentOrchestratorTest test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add -- agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java agent-service/src/main/java/com/_202510007517/platform/agent/service/GeneralChatService.java agent-service/src/main/java/com/_202510007517/platform/agent/service/LlmGeneralChatService.java agent-service/src/main/java/com/_202510007517/platform/agent/service/FallbackGeneralChatService.java agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentOrchestratorTest.java
git commit -m "refactor: pass session context into general chat replies"
```

---

### Task 3: Introduce LangChain4j Assistant Services for General and Knowledge Chat

**Files:**
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/assistant/GeneralAssistant.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/assistant/KnowledgeAssistant.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/assistant/LangChain4jAssistantConversationService.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/config/AgentAssistantConfiguration.java`
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagKnowledgeService.java`
- Modify: `agent-service/src/main/resources/application.yml`
- Create: `agent-service/src/test/java/com/_202510007517/platform/agent/assistant/LangChain4jAssistantConversationServiceTest.java`

- [ ] **Step 1: Write a failing assistant conversation service test**

Create `LangChain4jAssistantConversationServiceTest.java`:

```java
package com._202510007517.platform.agent.assistant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LangChain4jAssistantConversationServiceTest {

    private final GeneralAssistant generalAssistant = mock(GeneralAssistant.class);
    private final KnowledgeAssistant knowledgeAssistant = mock(KnowledgeAssistant.class);
    private final LangChain4jAssistantConversationService service =
            new LangChain4jAssistantConversationService(generalAssistant, knowledgeAssistant);

    @Test
    void routesGeneralRequestsToGeneralAssistant() {
        AgentAssistantRequest request = new AgentAssistantRequest(7L, "TEACHER", "12", "agent-session:12:assistant:GENERAL", "你是谁");
        when(generalAssistant.chat("TEACHER", "你是谁", "agent-session:12:assistant:GENERAL"))
                .thenReturn("我是教学管理平台内置的 AI 助手。");

        String answer = service.reply(AgentAssistantType.GENERAL, request);

        assertThat(answer).isEqualTo("我是教学管理平台内置的 AI 助手。");
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
mvn -pl agent-service -Dtest=LangChain4jAssistantConversationServiceTest test
```

Expected: FAIL because the assistant interfaces and service do not yet exist.

- [ ] **Step 3: Add AiServices interfaces**

Create `GeneralAssistant.java`:

```java
package com._202510007517.platform.agent.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

@SystemMessage("""
        你是教学管理平台内置的 AI 助手。
        你可以正常进行中文对话，回答学习、教学、平台使用相关问题。
        如果用户只是闲聊或询问你的身份，请自然回答。
        如果用户想让你执行课程、班级、作业、考试、通知、学习分析等平台操作，
        你只能说明“请把操作内容说清楚，我会生成待确认的操作卡片”，不要假装已经执行。
        不要编造平台数据库里不存在的结果。
        回答保持简洁。
        """)
public interface GeneralAssistant {

    @UserMessage("""
            当前用户角色：{{userRole}}

            用户消息：
            {{message}}
            """)
    String chat(@V("userRole") String userRole, @V("message") String message);
}
```

Create `KnowledgeAssistant.java`:

```java
package com._202510007517.platform.agent.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

@SystemMessage("""
        你是教学管理平台内置的 RAG 知识问答助手。
        只能依据提供的知识片段回答。不能编造实时课程、作业、考试、成绩、通知或学生名单。
        如果知识片段不足以回答，请明确说明没有足够依据。
        回答保持中文、简洁、适合教学平台用户阅读。
        """)
public interface KnowledgeAssistant {

    @UserMessage("""
            当前用户角色：{{userRole}}

            知识片段：
            {{retrievedContext}}

            用户问题：
            {{question}}
            """)
    String answer(@V("userRole") String userRole,
                  @V("question") String question,
                  @V("retrievedContext") String retrievedContext);
}
```

- [ ] **Step 4: Add LangChain4j-backed assistant conversation service**

Create `LangChain4jAssistantConversationService.java`:

```java
package com._202510007517.platform.agent.assistant;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class LangChain4jAssistantConversationService implements AssistantConversationService {

    private final GeneralAssistant generalAssistant;
    private final KnowledgeAssistant knowledgeAssistant;

    public LangChain4jAssistantConversationService(GeneralAssistant generalAssistant,
                                                   KnowledgeAssistant knowledgeAssistant) {
        this.generalAssistant = generalAssistant;
        this.knowledgeAssistant = knowledgeAssistant;
    }

    @Override
    public String reply(AgentAssistantType type, AgentAssistantRequest request) {
        return switch (type) {
            case GENERAL -> generalAssistant.chat(request.userRole(), request.message());
            case KNOWLEDGE -> knowledgeAssistant.answer(
                    request.userRole(),
                    request.message(),
                    "");
        };
    }
}
```

- [ ] **Step 5: Add `AiServices.builder(...)` configuration seam**

Create `AgentAssistantConfiguration.java`:

```java
package com._202510007517.platform.agent.config;

import com._202510007517.platform.agent.assistant.GeneralAssistant;
import com._202510007517.platform.agent.assistant.KnowledgeAssistant;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentAssistantConfiguration {

    @Bean
    @ConditionalOnBean(ChatModel.class)
    GeneralAssistant generalAssistant(ChatModel chatModel) {
        return AiServices.builder(GeneralAssistant.class)
                .chatModel(chatModel)
                .build();
    }

    @Bean
    @ConditionalOnBean(ChatModel.class)
    KnowledgeAssistant knowledgeAssistant(ChatModel chatModel) {
        return AiServices.builder(KnowledgeAssistant.class)
                .chatModel(chatModel)
                .build();
    }
}
```

- [ ] **Step 6: Extract retrieved context assembly from RAG service**

In `RagKnowledgeService.java`, add:

```java
public List<RagSearchResult> retrieve(Long userId, String userRole, String question) {
    if (!properties.isEnabled()) {
        return List.of();
    }
    try {
        List<Double> queryVector = embeddingClient.embed(question);
        return index.search(queryVector, userRole, properties.getMaxChunks(), properties.getMinScore());
    } catch (RuntimeException ex) {
        log.info("rag answer failed before generation: userId={}, reason={}", userId, ex.getClass().getSimpleName());
        return List.of();
    }
}

public String buildContext(List<RagSearchResult> results) {
    return results.stream()
            .map(result -> "[来源: %s / %s]\n%s".formatted(
                    result.chunk().documentTitle(),
                    result.chunk().sectionTitle(),
                    result.chunk().content()))
            .collect(Collectors.joining("\n\n"));
}
```

Then refactor `answer(...)` to call `retrieve(...)` and `buildContext(...)` instead of duplicating retrieval logic.

- [ ] **Step 7: Run assistant and RAG focused tests**

Run:

```powershell
mvn -pl agent-service -Dtest=LangChain4jAssistantConversationServiceTest,RagKnowledgeServiceTest test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add -- agent-service/src/main/java/com/_202510007517/platform/agent/assistant/GeneralAssistant.java agent-service/src/main/java/com/_202510007517/platform/agent/assistant/KnowledgeAssistant.java agent-service/src/main/java/com/_202510007517/platform/agent/assistant/LangChain4jAssistantConversationService.java agent-service/src/main/java/com/_202510007517/platform/agent/config/AgentAssistantConfiguration.java agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagKnowledgeService.java agent-service/src/test/java/com/_202510007517/platform/agent/assistant/LangChain4jAssistantConversationServiceTest.java
git commit -m "feat: add langchain4j assistant services"
```

---

### Task 4: Delegate General and Knowledge Paths Through the Assistant Layer

**Files:**
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java`
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/service/LlmGeneralChatService.java`
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagKnowledgeService.java`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentOrchestratorTest.java`

- [ ] **Step 1: Write failing orchestrator test for knowledge assistant memory scoping**

Add to `AgentOrchestratorTest.java`:

```java
@Test
void ragPathKeepsSessionScopedResponseShape() {
    when(intentRecognitionService.recognize("什么是服务注册与发现"))
            .thenReturn(new RecognizedIntent(AgentIntent.QUERY_RAG_KNOWLEDGE, 0.9, Map.of()));
    when(ragKnowledgeService.answer(7L, "TEACHER", "什么是服务注册与发现"))
            .thenReturn("服务注册与发现用于让微服务实例彼此定位。");

    AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null, "什么是服务注册与发现");

    assertThat(response.getResponseType()).isEqualTo("TEXT");
    assertThat(response.getMessage()).contains("服务注册与发现");
    assertThat(response.getSessionId()).isNotBlank();
}
```

- [ ] **Step 2: Run focused orchestrator tests**

Run:

```powershell
mvn -pl agent-service -Dtest=AgentOrchestratorTest#ragPathKeepsSessionScopedResponseShape,AgentOrchestratorTest#unknownIntentUsesGeneralAssistantBoundary test
```

Expected: PASS, confirming refactor safety baseline.

- [ ] **Step 3: Update orchestrator internals to construct assistant request objects**

In `AgentOrchestrator.java`, add constructor dependencies:

```java
private final AssistantMemoryKeyFactory assistantMemoryKeyFactory;
```

Add it to the constructor and assignments.

Then replace the `UNKNOWN` branch with:

```java
AgentAssistantRequest assistantRequest = new AgentAssistantRequest(
        userId,
        userRole,
        String.valueOf(session.getId()),
        assistantMemoryKeyFactory.build(String.valueOf(session.getId()), AgentAssistantType.GENERAL),
        message);
response = textResponse(session, generalChatService.reply(
        assistantRequest.userId(),
        assistantRequest.userRole(),
        assistantRequest.sessionId(),
        assistantRequest.message()));
```

Leave the method signature of `GeneralChatService` stable for now so this task does not yet widen the dependency graph.

- [ ] **Step 4: Keep `GeneralChatService` as the adapter consumed by orchestrator**

Update `LlmGeneralChatService.java` to remain the adapter entry point. Keep its current prompt behavior for now; Task 5 will replace internals with assistant delegation.

No external behavior change should happen in this step.

- [ ] **Step 5: Run orchestrator suite**

Run:

```powershell
mvn -pl agent-service -Dtest=AgentOrchestratorTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add -- agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentOrchestratorTest.java
git commit -m "refactor: prepare orchestrator for assistant-scoped routing"
```

---

### Task 5: Replace Direct General Chat Prompting With Assistant Delegation

**Files:**
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/service/LlmGeneralChatService.java`
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/service/FallbackGeneralChatService.java`
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/config/AgentIntentRecognitionConfiguration.java`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentOrchestratorTest.java`

- [ ] **Step 1: Write a failing service-level assistant delegation test**

Add to `AgentOrchestratorTest.java`:

```java
@Test
void unknownIntentStillReturnsFallbackWhenAssistantCannotAnswer() {
    when(intentRecognitionService.recognize("你好"))
            .thenReturn(new RecognizedIntent(AgentIntent.UNKNOWN, 0.2, Map.of()));
    when(generalChatService.reply(7L, "TEACHER", null, "你好"))
            .thenReturn("暂时还不能处理这个请求。");

    AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null, "你好");

    assertThat(response.getMessage()).isEqualTo("暂时还不能处理这个请求。");
}
```

- [ ] **Step 2: Run the focused test**

Run:

```powershell
mvn -pl agent-service -Dtest=AgentOrchestratorTest#unknownIntentStillReturnsFallbackWhenAssistantCannotAnswer test
```

Expected: PASS baseline.

- [ ] **Step 3: Keep current configuration stable while assistant layer matures**

Do not remove `GeneralChatService` wiring yet. Instead, update `AgentIntentRecognitionConfiguration.java` only if necessary to continue exposing `GeneralChatService` as the bean consumed by `AgentOrchestrator`.

The expected end state for this task is:

- `AgentOrchestrator` still depends on `GeneralChatService`
- `GeneralChatService` remains the stable facade
- future migration can switch its internals to delegate to `AssistantConversationService`

If no code change is needed after inspection, leave this task with no patch and document that the seam is already preserved by the current interface.

- [ ] **Step 4: Run targeted regression**

Run:

```powershell
mvn -pl agent-service -Dtest=AgentOrchestratorTest,DefaultAgentChatStreamingServiceTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add -- agent-service/src/main/java/com/_202510007517/platform/agent/config/AgentIntentRecognitionConfiguration.java agent-service/src/main/java/com/_202510007517/platform/agent/service/LlmGeneralChatService.java agent-service/src/main/java/com/_202510007517/platform/agent/service/FallbackGeneralChatService.java agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentOrchestratorTest.java
git commit -m "refactor: preserve general chat facade for assistant migration"
```

---

### Task 6: Verify the First Integration Slice End-to-End

**Files:**
- Modify: `docs/superpowers/specs/2026-06-23-agent-langchain4j-integration-design.md` only if implementation findings require clarifications
- Verify: `agent-service` focused tests

- [ ] **Step 1: Run focused assistant integration suite**

Run:

```powershell
$env:MAVEN_OPTS='-Xmx384m -XX:MaxMetaspaceSize=192m'
mvn --% -pl agent-service -Dmaven.compiler.fork=true -Dmaven.compiler.meminitial=128m -Dmaven.compiler.maxmem=256m -DforkCount=0 -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AgentOrchestratorTest,DefaultAgentChatStreamingServiceTest,RagKnowledgeServiceTest,LangChain4jAssistantConversationServiceTest,AssistantMemoryKeyFactoryTest test
```

Expected: PASS.

- [ ] **Step 2: Run compile verification**

Run:

```powershell
$env:MAVEN_OPTS='-Xmx384m -XX:MaxMetaspaceSize=192m'
mvn --% -pl agent-service -DskipTests compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Review whether design doc needs clarification**

Check whether implementation revealed any mismatch in:

- assistant boundary responsibilities
- fallback bean precedence
- memory key strategy
- general vs knowledge routing

If a clarification is needed, update the spec document before completion.

- [ ] **Step 4: Commit final integration slice**

```powershell
git add -- agent-service/src/main/java/com/_202510007517/platform/agent/assistant agent-service/src/main/java/com/_202510007517/platform/agent/config/AgentAssistantConfiguration.java agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java agent-service/src/main/java/com/_202510007517/platform/agent/service/GeneralChatService.java agent-service/src/main/java/com/_202510007517/platform/agent/service/LlmGeneralChatService.java agent-service/src/main/java/com/_202510007517/platform/agent/service/FallbackGeneralChatService.java agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagKnowledgeService.java agent-service/src/test/java/com/_202510007517/platform/agent/assistant agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentOrchestratorTest.java agent-service/src/test/java/com/_202510007517/platform/agent/rag/RagKnowledgeServiceTest.java docs/superpowers/specs/2026-06-23-agent-langchain4j-integration-design.md
git commit -m "feat: add first langchain4j assistant integration slice"
```

---

## Longer-Horizon Phases After This Executable Slice

After the first implementation slice above is stable, continue in this order:

1. Add assistant-scoped in-memory `ChatMemory` using `sessionId + assistantType` keys while keeping `agent_sessions` and `agent_messages` as the business source of truth.
2. Add Redis-backed assistant memory persistence inside `agent-service` only after in-memory behavior is stable and isolated by session.
3. Introduce true model-token streaming for assistant paths while keeping deterministic SSE events for business execution paths.
4. Add a retriever seam around the existing RAG pipeline so it can later migrate toward LangChain4j `ContentRetriever` without changing orchestrator behavior.
5. Pilot LangChain4j `@Tool` only for read-only queries such as course detail and learning statistics; keep write tools under explicit orchestrator preview and confirmation.
