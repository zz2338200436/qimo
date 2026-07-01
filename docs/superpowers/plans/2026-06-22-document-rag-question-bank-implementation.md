# Document RAG Question Bank Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace database-backed question generation with a document-backed RAG question bank that reads markdown files and returns real structured questions.

**Architecture:** Keep the existing knowledge-answering RAG path unchanged, and add a separate question-bank RAG pipeline inside `agent-service`. The new pipeline loads markdown question-bank documents, parses them into structured question objects, builds an embedding index, and serves both `GENERATE_QUESTIONS` and `QUERY_QUESTION_BANK` locally without calling `ai-service`.

**Tech Stack:** Spring Boot, LangChain4j-style embedding workflow, existing in-memory RAG index pattern, JUnit 5, Mockito, AssertJ

---

## File Structure

### New files

- `agent-service/src/main/java/com/_202510007517/platform/agent/config/QuestionBankProperties.java`
  - Separate configuration for document-backed question bank paths and embedding settings.
- `agent-service/src/main/java/com/_202510007517/platform/agent/config/QuestionBankConfiguration.java`
  - Spring beans for loader, parser, index, summary service, and question generation service.
- `agent-service/src/main/java/com/_202510007517/platform/agent/questionbank/QuestionBankDocument.java`
  - Raw loaded markdown document model.
- `agent-service/src/main/java/com/_202510007517/platform/agent/questionbank/QuestionChunk.java`
  - Structured single-question model.
- `agent-service/src/main/java/com/_202510007517/platform/agent/questionbank/QuestionBankDocumentLoader.java`
  - Directory/file loader for `docs/question-bank/**/*.md`.
- `agent-service/src/main/java/com/_202510007517/platform/agent/questionbank/QuestionMarkdownParser.java`
  - Parses markdown documents into `QuestionChunk` objects.
- `agent-service/src/main/java/com/_202510007517/platform/agent/questionbank/QuestionBankIndex.java`
  - In-memory vector index and metadata filtering for question chunks.
- `agent-service/src/main/java/com/_202510007517/platform/agent/questionbank/QuestionBankSummaryService.java`
  - Produces topic/difficulty/question count summary from loaded chunks.
- `agent-service/src/main/java/com/_202510007517/platform/agent/questionbank/QuestionRagService.java`
  - Main local service for question generation requests.
- `agent-service/src/test/java/com/_202510007517/platform/agent/questionbank/QuestionMarkdownParserTest.java`
  - Parser tests.
- `agent-service/src/test/java/com/_202510007517/platform/agent/questionbank/QuestionBankDocumentLoaderTest.java`
  - Loader tests.
- `agent-service/src/test/java/com/_202510007517/platform/agent/questionbank/QuestionBankIndexTest.java`
  - Index filtering and ranking tests.
- `agent-service/src/test/java/com/_202510007517/platform/agent/questionbank/QuestionRagServiceTest.java`
  - Structured generation tests.
- `agent-service/src/test/java/com/_202510007517/platform/agent/questionbank/QuestionBankSummaryServiceTest.java`
  - Summary tests.
- `docs/question-bank/README.md`
  - Human-facing question-bank authoring guide.
- `docs/question-bank/java/java-basic-sample.md`
  - Sample seed question-bank markdown file for local verification.

### Modified files

- `agent-service/src/main/resources/application.yml`
  - Add `agent.question-bank` config block.
- `agent-service/src/main/java/com/_202510007517/platform/agent/tool/GenerateQuestionsTool.java`
  - Stop calling `AiEdgeClient`; call `QuestionRagService`.
- `agent-service/src/main/java/com/_202510007517/platform/agent/tool/QuestionBankSummaryTool.java`
  - Stop calling `AiEdgeClient`; call `QuestionBankSummaryService`.
- `agent-service/src/test/java/com/_202510007517/platform/agent/tool/AgentEdgeToolTest.java`
  - Update tests to verify local document-backed path.
- `agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentOrchestratorTest.java`
  - Update orchestration tests so question generation no longer depends on `AiEdgeClient`.
- `agent-service/src/test/java/com/_202510007517/platform/agent/config/AgentRagConfigurationTest.java`
  - Keep existing knowledge RAG coverage intact; add question-bank config separation checks if needed.
- `agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentAuditService.java`
  - Confirm target-service mapping still makes sense once question generation becomes local.

## Task 1: Add Question-Bank Configuration

**Files:**
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/config/QuestionBankProperties.java`
- Modify: `agent-service/src/main/resources/application.yml`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/config/QuestionBankPropertiesTest.java`

- [ ] **Step 1: Write the failing configuration-binding test**

```java
package com._202510007517.platform.agent.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionBankPropertiesTest {

    @Test
    void bindsConfiguredQuestionBankProperties() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("agent.question-bank.enabled", "true")
                .withProperty("agent.question-bank.document-paths[0]", "docs/question-bank")
                .withProperty("agent.question-bank.embedding-base-url", "http://localhost:11434")
                .withProperty("agent.question-bank.embedding-model", "qwen3-embedding:0.6b")
                .withProperty("agent.question-bank.max-candidates", "50")
                .withProperty("agent.question-bank.min-score", "0.15")
                .withProperty("agent.question-bank.allow-reload", "true");

        QuestionBankProperties properties = Binder.get(env)
                .bind("agent.question-bank", Bindable.of(QuestionBankProperties.class))
                .orElseThrow();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getDocumentPaths()).isEqualTo(List.of("docs/question-bank"));
        assertThat(properties.getEmbeddingBaseUrl()).isEqualTo("http://localhost:11434");
        assertThat(properties.getEmbeddingModel()).isEqualTo("qwen3-embedding:0.6b");
        assertThat(properties.getMaxCandidates()).isEqualTo(50);
        assertThat(properties.getMinScore()).isEqualTo(0.15);
        assertThat(properties.isAllowReload()).isTrue();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
mvn --% -pl agent-service -Dtest=QuestionBankPropertiesTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because `QuestionBankProperties` and test file do not exist yet.

- [ ] **Step 3: Add `QuestionBankProperties`**

```java
package com._202510007517.platform.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ConfigurationProperties(prefix = "agent.question-bank")
public class QuestionBankProperties {
    public static final String DEFAULT_DOCUMENT_PATH = "docs/question-bank";

    private boolean enabled = false;
    private List<String> documentPaths = new ArrayList<>(List.of(DEFAULT_DOCUMENT_PATH));
    private String embeddingBaseUrl = "http://localhost:11434";
    private String embeddingModel = "qwen3-embedding:0.6b";
    private int maxCandidates = 50;
    private double minScore = 0.15;
    private boolean allowReload = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getDocumentPaths() {
        return documentPaths;
    }

    public void setDocumentPaths(List<String> documentPaths) {
        if (documentPaths == null || documentPaths.isEmpty()) {
            this.documentPaths = new ArrayList<>(List.of(DEFAULT_DOCUMENT_PATH));
            return;
        }
        List<String> configuredDocumentPaths = documentPaths.stream()
                .filter(Objects::nonNull)
                .filter(path -> !path.isBlank())
                .toList();
        this.documentPaths = configuredDocumentPaths.isEmpty()
                ? new ArrayList<>(List.of(DEFAULT_DOCUMENT_PATH))
                : new ArrayList<>(configuredDocumentPaths);
    }

    public String getEmbeddingBaseUrl() {
        return embeddingBaseUrl;
    }

    public void setEmbeddingBaseUrl(String embeddingBaseUrl) {
        this.embeddingBaseUrl = embeddingBaseUrl;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public int getMaxCandidates() {
        return maxCandidates;
    }

    public void setMaxCandidates(int maxCandidates) {
        this.maxCandidates = Math.max(1, maxCandidates);
    }

    public double getMinScore() {
        return minScore;
    }

    public void setMinScore(double minScore) {
        this.minScore = Math.max(0.0, minScore);
    }

    public boolean isAllowReload() {
        return allowReload;
    }

    public void setAllowReload(boolean allowReload) {
        this.allowReload = allowReload;
    }
}
```

- [ ] **Step 4: Add YAML config block**

Add under `agent:` in `agent-service/src/main/resources/application.yml`:

```yml
  question-bank:
    enabled: ${AGENT_QUESTION_BANK_ENABLED:true}
    document-paths:
      - ${AGENT_QUESTION_BANK_DOCUMENT_PATH:docs/question-bank}
    embedding-base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
    embedding-model: ${OLLAMA_EMBEDDING_MODEL:qwen3-embedding:0.6b}
    max-candidates: ${AGENT_QUESTION_BANK_MAX_CANDIDATES:50}
    min-score: ${AGENT_QUESTION_BANK_MIN_SCORE:0.15}
    allow-reload: ${AGENT_QUESTION_BANK_ALLOW_RELOAD:true}
```

- [ ] **Step 5: Run test to verify it passes**

Run:

```powershell
mvn --% -pl agent-service -Dtest=QuestionBankPropertiesTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add agent-service/src/main/java/com/_202510007517/platform/agent/config/QuestionBankProperties.java agent-service/src/main/resources/application.yml agent-service/src/test/java/com/_202510007517/platform/agent/config/QuestionBankPropertiesTest.java
git commit -m "feat: add question bank configuration"
```

## Task 2: Implement Markdown Loader and Parser

**Files:**
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/questionbank/QuestionBankDocument.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/questionbank/QuestionChunk.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/questionbank/QuestionBankDocumentLoader.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/questionbank/QuestionMarkdownParser.java`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/questionbank/QuestionBankDocumentLoaderTest.java`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/questionbank/QuestionMarkdownParserTest.java`

- [ ] **Step 1: Write failing loader test**

```java
package com._202510007517.platform.agent.questionbank;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionBankDocumentLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsMarkdownFilesRecursively() throws Exception {
        Path topicDir = Files.createDirectories(tempDir.resolve("java"));
        Files.writeString(topicDir.resolve("java-basic.md"), """
                ---
                title: Java基础题库
                topic: Java基础
                ---
                ## Question
                difficulty: 中等
                type: SINGLE_CHOICE
                content: Java中用于表示继承的关键字是什么？
                answer: extends
                """);

        QuestionBankDocumentLoader loader = new QuestionBankDocumentLoader();
        List<QuestionBankDocument> documents = loader.load(List.of(tempDir.toString()));

        assertThat(documents).hasSize(1);
        assertThat(documents.get(0).title()).isEqualTo("Java基础题库");
    }
}
```

- [ ] **Step 2: Write failing parser test**

```java
package com._202510007517.platform.agent.questionbank;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionMarkdownParserTest {

    @Test
    void parsesStructuredQuestionsFromMarkdown() {
        QuestionBankDocument document = new QuestionBankDocument(
                "doc-1",
                "Java基础题库",
                "docs/question-bank/java/java-basic.md",
                Map.of("topic", "Java基础", "roleScope", "teacher"),
                """
                ## Question
                difficulty: 中等
                type: SINGLE_CHOICE
                content: Java中用于表示继承的关键字是什么？
                options:
                - A. import
                - B. extends
                - C. implements
                answer: B
                analysis: extends 用于类继承。
                """
        );

        QuestionMarkdownParser parser = new QuestionMarkdownParser();
        List<QuestionChunk> questions = parser.parse(document);

        assertThat(questions).hasSize(1);
        assertThat(questions.get(0).topic()).isEqualTo("Java基础");
        assertThat(questions.get(0).difficulty()).isEqualTo("中等");
        assertThat(questions.get(0).type()).isEqualTo("SINGLE_CHOICE");
        assertThat(questions.get(0).options()).containsExactly("A. import", "B. extends", "C. implements");
        assertThat(questions.get(0).answer()).isEqualTo("B");
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run:

```powershell
mvn --% -pl agent-service -Dtest=QuestionBankDocumentLoaderTest,QuestionMarkdownParserTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because classes do not exist yet.

- [ ] **Step 4: Add raw document and parsed chunk models**

```java
package com._202510007517.platform.agent.questionbank;

import java.util.Map;

public record QuestionBankDocument(
        String documentId,
        String title,
        String sourcePath,
        Map<String, String> metadata,
        String content
) {
}
```

```java
package com._202510007517.platform.agent.questionbank;

import java.util.List;

public record QuestionChunk(
        String questionId,
        String sourcePath,
        String documentTitle,
        String topic,
        String difficulty,
        String type,
        List<String> tags,
        String content,
        List<String> options,
        String answer,
        String analysis,
        String roleScope,
        Integer order
) {
}
```

- [ ] **Step 5: Add loader implementation**

Use the same path walking pattern as `RagMarkdownDocumentLoader`, but return `QuestionBankDocument` and only load markdown files from configured paths.

- [ ] **Step 6: Add parser implementation**

Implementation requirements:

- split markdown on `## Question`
- read document front matter metadata
- parse per-question key/value pairs
- parse repeated `options:` list items
- require `topic`, `difficulty`, `type`, `content`, `answer`
- default `roleScope` from front matter, fallback `all`
- build stable `questionId` as `documentId#<order>`

Use this helper shape:

```java
private Map<String, String> parseScalarFields(String block) { ... }
private List<String> parseOptions(String block) { ... }
private String required(String fieldName, String value, String sourcePath) { ... }
```

- [ ] **Step 7: Run tests to verify they pass**

Run:

```powershell
mvn --% -pl agent-service -Dtest=QuestionBankDocumentLoaderTest,QuestionMarkdownParserTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add agent-service/src/main/java/com/_202510007517/platform/agent/questionbank agent-service/src/test/java/com/_202510007517/platform/agent/questionbank
git commit -m "feat: add markdown question bank loader and parser"
```

## Task 3: Build Question-Bank Index and Local Generation Service

**Files:**
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/questionbank/QuestionBankIndex.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/questionbank/QuestionRagService.java`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/questionbank/QuestionBankIndexTest.java`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/questionbank/QuestionRagServiceTest.java`

- [ ] **Step 1: Write failing index test**

```java
package com._202510007517.platform.agent.questionbank;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionBankIndexTest {

    @Test
    void filtersByRoleAndDifficultyBeforeRanking() {
        QuestionBankIndex index = new QuestionBankIndex();
        index.replaceAll(List.of(
                new QuestionBankIndex.IndexedQuestion(question("q1", "Java基础", "中等", "teacher"), List.of(1.0, 0.0)),
                new QuestionBankIndex.IndexedQuestion(question("q2", "Java基础", "困难", "teacher"), List.of(1.0, 0.0)),
                new QuestionBankIndex.IndexedQuestion(question("q3", "Java基础", "中等", "student"), List.of(1.0, 0.0))
        ));

        List<QuestionChunk> results = index.search(List.of(1.0, 0.0), "teacher", "Java基础", "中等", null, 10, 0.0);

        assertThat(results).extracting(QuestionChunk::questionId).containsExactly("q1");
    }

    private QuestionChunk question(String id, String topic, String difficulty, String roleScope) {
        return new QuestionChunk(id, "source.md", "题库", topic, difficulty, "SINGLE_CHOICE",
                List.of(), "content", List.of("A", "B"), "A", "analysis", roleScope, 1);
    }
}
```

- [ ] **Step 2: Write failing service test**

```java
package com._202510007517.platform.agent.questionbank;

import com._202510007517.platform.agent.config.QuestionBankProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionRagServiceTest {

    @Test
    void marksPartialWhenNotEnoughQuestionsMatch() {
        QuestionBankProperties properties = new QuestionBankProperties();
        properties.setEnabled(true);
        properties.setMaxCandidates(50);
        properties.setMinScore(0.0);

        QuestionBankIndex index = new QuestionBankIndex();
        index.replaceAll(List.of(
                new QuestionBankIndex.IndexedQuestion(question("q1"), List.of(1.0, 0.0)),
                new QuestionBankIndex.IndexedQuestion(question("q2"), List.of(0.9, 0.0))
        ));

        QuestionRagService service = new QuestionRagService(
                properties,
                text -> List.of(1.0, 0.0),
                index
        );

        Map<String, Object> result = service.generateQuestions("TEACHER", "Java基础", "中等", 5);

        assertThat(result).containsEntry("topic", "Java基础");
        assertThat(result).containsEntry("count", 5);
        assertThat(result).containsEntry("actualCount", 2);
        assertThat(result).containsEntry("partial", true);
        assertThat(String.valueOf(result.get("message"))).contains("2/5");
    }

    private QuestionChunk question(String id) {
        return new QuestionChunk(id, "source.md", "题库", "Java基础", "中等", "SINGLE_CHOICE",
                List.of(), "Java中用于表示继承的关键字是什么？", List.of("A. import", "B. extends"), "B", "analysis", "all", 1);
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run:

```powershell
mvn --% -pl agent-service -Dtest=QuestionBankIndexTest,QuestionRagServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL

- [ ] **Step 4: Implement index**

Requirements:

- keep in-memory `IndexedQuestion` list
- filter by role scope
- filter by normalized difficulty
- filter by topic if exact/contains match
- optional type filter
- cosine rank remaining questions
- return top `maxCandidates`

Method signature:

```java
public List<QuestionChunk> search(List<Double> queryVector,
                                  String userRole,
                                  String topic,
                                  String difficulty,
                                  String type,
                                  int maxCandidates,
                                  double minScore)
```

- [ ] **Step 5: Implement service**

Requirements:

- refuse when disabled
- normalize difficulty like existing `LocalMockAiModelClient`
- embed query text using `topic + difficulty + type`
- call index
- trim to requested `count`
- map `QuestionChunk` to current frontend-compatible payload:

```java
Map.of(
    "id", question.questionId(),
    "content", question.content(),
    "difficulty", question.difficulty(),
    "type", displayType(question.type()),
    "score", null,
    "options", question.options(),
    "answer", question.answer(),
    "analysis", question.analysis(),
    "knowledgePoints", List.of(question.topic()),
    "sourcePath", question.sourcePath()
)
```

- [ ] **Step 6: Run tests to verify they pass**

Run:

```powershell
mvn --% -pl agent-service -Dtest=QuestionBankIndexTest,QuestionRagServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add agent-service/src/main/java/com/_202510007517/platform/agent/questionbank/QuestionBankIndex.java agent-service/src/main/java/com/_202510007517/platform/agent/questionbank/QuestionRagService.java agent-service/src/test/java/com/_202510007517/platform/agent/questionbank/QuestionBankIndexTest.java agent-service/src/test/java/com/_202510007517/platform/agent/questionbank/QuestionRagServiceTest.java
git commit -m "feat: add local question rag generation service"
```

## Task 4: Add Summary Service and Spring Wiring

**Files:**
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/questionbank/QuestionBankSummaryService.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/config/QuestionBankConfiguration.java`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/questionbank/QuestionBankSummaryServiceTest.java`

- [ ] **Step 1: Write failing summary test**

```java
package com._202510007517.platform.agent.questionbank;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionBankSummaryServiceTest {

    @Test
    void summarizesQuestionCountsByTopicAndDifficulty() {
        QuestionBankSummaryService service = new QuestionBankSummaryService(List.of(
                new QuestionChunk("q1", "a.md", "A", "Java基础", "中等", "SINGLE_CHOICE", List.of(), "c1", List.of(), "A", "", "all", 1),
                new QuestionChunk("q2", "a.md", "A", "Java基础", "中等", "TRUE_FALSE", List.of(), "c2", List.of(), "true", "", "all", 2),
                new QuestionChunk("q3", "b.md", "B", "服务注册与发现", "困难", "SHORT_ANSWER", List.of(), "c3", List.of(), "ans", "", "all", 1)
        ));

        Map<String, Object> summary = service.summary();

        assertThat(summary).containsEntry("totalQuestions", 3);
        assertThat(summary).containsEntry("topicCount", 2);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
mvn --% -pl agent-service -Dtest=QuestionBankSummaryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL

- [ ] **Step 3: Implement summary service**

Implementation requirements:

- accept immutable list or supplier of loaded questions
- compute:
  - `totalQuestions`
  - `topicCount`
  - `topics`
  - `difficultyBreakdown`
  - `topicDifficultyBreakdown`
- return `Map<String, Object>` for compatibility

- [ ] **Step 4: Add Spring configuration**

Implementation requirements for `QuestionBankConfiguration`:

- enable `QuestionBankProperties`
- create loader
- create parser
- when enabled:
  - load documents
  - parse question chunks
  - build embeddings
  - create `QuestionBankIndex`
  - create `QuestionRagService`
  - create `QuestionBankSummaryService`
- when disabled:
  - create empty no-op versions

- [ ] **Step 5: Run test to verify it passes**

Run:

```powershell
mvn --% -pl agent-service -Dtest=QuestionBankSummaryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add agent-service/src/main/java/com/_202510007517/platform/agent/questionbank/QuestionBankSummaryService.java agent-service/src/main/java/com/_202510007517/platform/agent/config/QuestionBankConfiguration.java agent-service/src/test/java/com/_202510007517/platform/agent/questionbank/QuestionBankSummaryServiceTest.java
git commit -m "feat: wire question bank services into spring"
```

## Task 5: Switch GenerateQuestionsTool to Local Question RAG

**Files:**
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/tool/GenerateQuestionsTool.java`
- Modify: `agent-service/src/test/java/com/_202510007517/platform/agent/tool/AgentEdgeToolTest.java`
- Modify: `agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentOrchestratorTest.java`

- [ ] **Step 1: Write failing tool test update**

Replace the existing `generateQuestionsExecutesThroughAiServiceWithoutConfirmation` assertions with a local-service version:

```java
@Test
void generateQuestionsExecutesThroughLocalQuestionBankWithoutConfirmation() {
    AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null, "生成五道Java基础中等题");

    assertThat(response.getResponseType()).isEqualTo("DATA");
    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) response.getData();
    assertThat(data)
            .containsEntry("status", "EXECUTED")
            .containsEntry("topic", "Java基础")
            .containsEntry("difficulty", "中等")
            .containsKey("questions")
            .containsKey("aiResult");
    verify(aiEdgeClient, never()).generateQuestions(any(), any(), any(), any());
}
```

- [ ] **Step 2: Run targeted tests to verify they fail**

Run:

```powershell
mvn --% -pl agent-service -Dtest=AgentEdgeToolTest#generateQuestionsExecutesThroughLocalQuestionBankWithoutConfirmation,AgentOrchestratorTest#asksForTopicBeforeGeneratingQuestionsWhenRequestIsTooGeneric -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because tool still depends on `AiEdgeClient`.

- [ ] **Step 3: Update tool constructor and implementation**

Replace `AiEdgeClient` dependency with `QuestionRagService`:

```java
private final QuestionRagService questionRagService;

public GenerateQuestionsTool(QuestionRagService questionRagService) {
    this.questionRagService = questionRagService;
}

@Override
public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
    String topic = String.valueOf(request.getOrDefault("topic", request.getOrDefault("courseName", "综合练习")));
    Integer count = asInteger(request.get("count"), 5);
    String difficulty = String.valueOf(request.getOrDefault("difficulty", "中等"));
    String type = request.get("type") == null ? null : String.valueOf(request.get("type"));

    Map<String, Object> payload = questionRagService.generateQuestions(userRole, topic, difficulty, count, type);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("status", "EXECUTED");
    result.putAll(payload);
    result.put("aiResult", payload);
    result.put("message", payload.get("message"));
    return result;
}
```

- [ ] **Step 4: Update orchestration tests to remove `AiEdgeClient` dependency for question generation**

Remove or rewrite any test stubbing:

```java
when(aiEdgeClient.generateQuestions(...))
```

and replace them with seeded question-bank markdown under `docs/question-bank/java/java-basic-sample.md`.

- [ ] **Step 5: Run tests to verify they pass**

Run:

```powershell
mvn --% -pl agent-service -Dtest=AgentEdgeToolTest,AgentOrchestratorTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add agent-service/src/main/java/com/_202510007517/platform/agent/tool/GenerateQuestionsTool.java agent-service/src/test/java/com/_202510007517/platform/agent/tool/AgentEdgeToolTest.java agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentOrchestratorTest.java
git commit -m "feat: switch generate questions to local document rag"
```

## Task 6: Switch QuestionBankSummaryTool to Local Summary Service

**Files:**
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/tool/QuestionBankSummaryTool.java`
- Modify: `agent-service/src/test/java/com/_202510007517/platform/agent/tool/AgentEdgeToolTest.java`

- [ ] **Step 1: Add failing summary tool test**

```java
@Test
void questionBankSummaryUsesLocalDocumentSummary() {
    AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null, "现在题库有什么题目");

    assertThat(response.getResponseType()).isEqualTo("DATA");
    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) response.getData();
    assertThat(data).containsEntry("status", "EXECUTED").containsKey("questionBank");
    verify(aiEdgeClient, never()).questionBankSummary(any(), any(), any());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
mvn --% -pl agent-service -Dtest=AgentEdgeToolTest#questionBankSummaryUsesLocalDocumentSummary -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL

- [ ] **Step 3: Replace `AiEdgeClient` dependency with `QuestionBankSummaryService`**

Implementation:

```java
private final QuestionBankSummaryService summaryService;

public QuestionBankSummaryTool(QuestionBankSummaryService summaryService) {
    this.summaryService = summaryService;
}

@Override
public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("status", "EXECUTED");
    result.put("questionBank", summaryService.summary());
    result.put("message", "题库查询完成。");
    return result;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
mvn --% -pl agent-service -Dtest=AgentEdgeToolTest#questionBankSummaryUsesLocalDocumentSummary -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add agent-service/src/main/java/com/_202510007517/platform/agent/tool/QuestionBankSummaryTool.java agent-service/src/test/java/com/_202510007517/platform/agent/tool/AgentEdgeToolTest.java
git commit -m "feat: switch question bank summary to local document service"
```

## Task 7: Add Sample Question-Bank Documents and Authoring Guide

**Files:**
- Create: `docs/question-bank/README.md`
- Create: `docs/question-bank/java/java-basic-sample.md`

- [ ] **Step 1: Add authoring guide**

Content:

```md
# 题库文档格式

题库目录用于给 `agent-service` 的文档型题库 RAG 提供真实题目来源。

## 目录规范

- 每个主题建议单独成文件
- 仅支持 `.md`
- 推荐放在 `docs/question-bank/<module>/`

## 必填字段

- `topic`
- `difficulty`
- `type`
- `content`
- `answer`

## 示例

```md
---
title: Java基础中等题库
topic: Java基础
roleScope: all
tags:
  - java
---

## Question
difficulty: 中等
type: SINGLE_CHOICE
content: Java中用于表示继承的关键字是什么？
options:
- A. import
- B. extends
- C. implements
answer: B
analysis: extends 用于类继承。
```
```
```

- [ ] **Step 2: Add sample question-bank file**

Content:

```md
---
title: Java基础中等题库样例
topic: Java基础
roleScope: all
tags:
  - java
  - basic
---

## Question
difficulty: 中等
type: SINGLE_CHOICE
content: Java中用于表示一个类继承另一个类的关键字是哪个？
options:
- A. import
- B. extends
- C. package
- D. implements
answer: B
analysis: extends 用于类继承。

## Question
difficulty: 中等
type: TRUE_FALSE
content: char类型在Java中可以直接表示Unicode字符。
answer: true
analysis: Java 的 char 基于 UTF-16 代码单元。
```

- [ ] **Step 3: Commit**

```bash
git add docs/question-bank/README.md docs/question-bank/java/java-basic-sample.md
git commit -m "docs: add question bank markdown samples"
```

## Task 8: Run Full Verification

**Files:**
- Modify if needed: any files fixed during verification

- [ ] **Step 1: Run focused question-bank test suite**

Run:

```powershell
mvn --% -pl agent-service -Dtest=QuestionBankPropertiesTest,QuestionBankDocumentLoaderTest,QuestionMarkdownParserTest,QuestionBankIndexTest,QuestionRagServiceTest,QuestionBankSummaryServiceTest,AgentEdgeToolTest,AgentOrchestratorTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS

- [ ] **Step 2: Run existing knowledge-RAG tests to guard regression**

Run:

```powershell
mvn --% -pl agent-service -Dtest=AgentRagConfigurationTest,RagKnowledgeServiceTest,RagMarkdownDocumentLoaderTest,RagMarkdownChunkerTest,InMemoryRagIndexTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: PASS

- [ ] **Step 3: Run a manual smoke check in the running app**

Run:

```powershell
node scripts/verify-teacher-agent-generate-five-questions-runtime.js
```

Expected:

- request for Java基础中等题 returns `questions`
- `partial=false` if enough sample data exists
- no dependency on `sc_ai.questions` for answer generation

- [ ] **Step 4: Commit final verification adjustments**

```bash
git add agent-service docs/question-bank
git commit -m "test: verify document rag question bank flow"
```

## Spec Coverage Check

- Document-backed question source: covered by Tasks 2 and 7
- Separate question-bank RAG path: covered by Tasks 3 and 4
- Remove `GenerateQuestionsTool -> ai-service`: covered by Task 5
- Remove `QuestionBankSummaryTool -> ai-service`: covered by Task 6
- Keep strict partial behavior: covered by Task 3 and verified in Task 8
- Keep knowledge-answering RAG separate: guarded in Task 8

## Placeholder Scan

- No `TBD`
- No “write tests later”
- Each task has concrete file paths, commands, and code shape

## Type Consistency

- `QuestionBankProperties`
- `QuestionBankDocument`
- `QuestionChunk`
- `QuestionBankIndex`
- `QuestionRagService`
- `QuestionBankSummaryService`

These names are used consistently across all tasks.
