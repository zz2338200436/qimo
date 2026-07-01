# Agent RAG Knowledge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a first-stage Markdown-based RAG knowledge answering path to `agent-service` using Ollama embeddings and the existing Xiaomi-compatible chat model path.

**Architecture:** RAG remains inside `agent-service` as a read-only Agent capability. Markdown files are loaded and chunked, chunks are embedded through a small Ollama client abstraction, an in-memory cosine index retrieves top chunks, and `AgentOrchestrator` routes the new `QUERY_RAG_KNOWLEDGE` intent to a `RagKnowledgeService` instead of action planning or business tools.

**Tech Stack:** Java 17, Spring Boot, LangChain4j `ChatModel`, Spring `RestClient`, JUnit 5, AssertJ, Mockito, Maven.

---

## File Structure

- Modify: `agent-service/src/main/resources/application.yml`
  - Adds `agent.rag.*` defaults and disables RAG in the `test` profile unless explicitly enabled per test.
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/model/AgentIntent.java`
  - Adds `QUERY_RAG_KNOWLEDGE`.
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentPermissionPolicy.java`
  - Allows teacher, student, and admin roles to use `QUERY_RAG_KNOWLEDGE`.
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentConfirmationPolicy.java`
  - Marks `QUERY_RAG_KNOWLEDGE` as no-confirmation.
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/service/RuleBasedIntentRecognitionService.java`
  - Adds conservative RAG routing while keeping business/action prompts on existing intents.
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java`
  - Routes `QUERY_RAG_KNOWLEDGE` to `RagKnowledgeService`.
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/config/AgentRagProperties.java`
  - Binds `agent.rag.*`.
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/config/AgentRagConfiguration.java`
  - Wires the optional RAG beans.
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagDocument.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagChunk.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagSearchResult.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagMarkdownDocumentLoader.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagMarkdownChunker.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagEmbeddingClient.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/rag/OllamaRagEmbeddingClient.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/rag/InMemoryRagIndex.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagKnowledgeService.java`
- Create tests under `agent-service/src/test/java/com/_202510007517/platform/agent/rag/`.
- Modify tests under `agent-service/src/test/java/com/_202510007517/platform/agent/service/`.

---

### Task 1: Add RAG Configuration Properties

**Files:**
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/config/AgentRagProperties.java`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/config/AgentRagPropertiesTest.java`

- [ ] **Step 1: Write the failing test**

Create `agent-service/src/test/java/com/_202510007517/platform/agent/config/AgentRagPropertiesTest.java`:

```java
package com._202510007517.platform.agent.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRagPropertiesTest {

    @Test
    void bindsRagPropertiesWithDefaultsAndOverrides() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "agent.rag.enabled", "true",
                "agent.rag.document-paths[0]", "docs/rag-knowledge-base.md",
                "agent.rag.embedding-base-url", "http://localhost:11434",
                "agent.rag.embedding-model", "qwen3-embedding:0.6b",
                "agent.rag.max-chunks", "5",
                "agent.rag.min-score", "0.25"
        ));

        AgentRagProperties properties = new Binder(source)
                .bind("agent.rag", Bindable.of(AgentRagProperties.class))
                .orElseThrow();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getDocumentPaths()).isEqualTo(List.of("docs/rag-knowledge-base.md"));
        assertThat(properties.getEmbeddingBaseUrl()).isEqualTo("http://localhost:11434");
        assertThat(properties.getEmbeddingModel()).isEqualTo("qwen3-embedding:0.6b");
        assertThat(properties.getMaxChunks()).isEqualTo(5);
        assertThat(properties.getMinScore()).isEqualTo(0.25);
    }

    @Test
    void providesSafeDefaults() {
        AgentRagProperties properties = new AgentRagProperties();

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getDocumentPaths()).containsExactly("docs/rag-knowledge-base.md");
        assertThat(properties.getEmbeddingBaseUrl()).isEqualTo("http://localhost:11434");
        assertThat(properties.getEmbeddingModel()).isEqualTo("qwen3-embedding:0.6b");
        assertThat(properties.getMaxChunks()).isEqualTo(4);
        assertThat(properties.getMinScore()).isEqualTo(0.0);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
mvn -pl agent-service -Dtest=AgentRagPropertiesTest test
```

Expected: compilation fails because `AgentRagProperties` does not exist.

- [ ] **Step 3: Add properties class**

Create `agent-service/src/main/java/com/_202510007517/platform/agent/config/AgentRagProperties.java`:

```java
package com._202510007517.platform.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "agent.rag")
public class AgentRagProperties {
    private boolean enabled = false;
    private List<String> documentPaths = new ArrayList<>(List.of("docs/rag-knowledge-base.md"));
    private String embeddingBaseUrl = "http://localhost:11434";
    private String embeddingModel = "qwen3-embedding:0.6b";
    private int maxChunks = 4;
    private double minScore = 0.0;

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
        this.documentPaths = documentPaths == null || documentPaths.isEmpty()
                ? new ArrayList<>(List.of("docs/rag-knowledge-base.md"))
                : new ArrayList<>(documentPaths);
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

    public int getMaxChunks() {
        return maxChunks;
    }

    public void setMaxChunks(int maxChunks) {
        this.maxChunks = Math.max(1, maxChunks);
    }

    public double getMinScore() {
        return minScore;
    }

    public void setMinScore(double minScore) {
        this.minScore = Math.max(0.0, minScore);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
mvn -pl agent-service -Dtest=AgentRagPropertiesTest test
```

Expected: test passes.

- [ ] **Step 5: Commit**

```powershell
git add agent-service/src/main/java/com/_202510007517/platform/agent/config/AgentRagProperties.java agent-service/src/test/java/com/_202510007517/platform/agent/config/AgentRagPropertiesTest.java
git commit -m "feat(agent): add rag configuration properties"
```

---

### Task 2: Add Markdown Document Loading

**Files:**
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagDocument.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagMarkdownDocumentLoader.java`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/rag/RagMarkdownDocumentLoaderTest.java`

- [ ] **Step 1: Write the failing test**

Create `agent-service/src/test/java/com/_202510007517/platform/agent/rag/RagMarkdownDocumentLoaderTest.java`:

```java
package com._202510007517.platform.agent.rag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RagMarkdownDocumentLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsMarkdownFileWithFrontMatter() throws Exception {
        Path file = tempDir.resolve("guide.md");
        Files.writeString(file, """
                ---
                title: 平台使用指南
                source_type: markdown
                roleScope: teacher
                courseId: 91005
                ---

                # 平台使用指南

                教师可以使用 AI 助手查询教学数据。
                """);

        RagMarkdownDocumentLoader loader = new RagMarkdownDocumentLoader();
        List<RagDocument> documents = loader.load(List.of(file.toString()));

        assertThat(documents).hasSize(1);
        RagDocument document = documents.get(0);
        assertThat(document.documentId()).isEqualTo(file.toAbsolutePath().normalize().toString());
        assertThat(document.title()).isEqualTo("平台使用指南");
        assertThat(document.sourcePath()).isEqualTo(file.toAbsolutePath().normalize().toString());
        assertThat(document.metadata())
                .containsEntry("source_type", "markdown")
                .containsEntry("roleScope", "teacher")
                .containsEntry("courseId", "91005");
        assertThat(document.content()).contains("教师可以使用 AI 助手查询教学数据。");
    }

    @Test
    void loadsAllMarkdownFilesFromDirectoryAndIgnoresMissingPath() throws Exception {
        Path first = tempDir.resolve("first.md");
        Path second = tempDir.resolve("second.txt");
        Files.writeString(first, "# 第一份文档\n\n正文");
        Files.writeString(second, "# 不是 Markdown\n\n正文");

        RagMarkdownDocumentLoader loader = new RagMarkdownDocumentLoader();
        List<RagDocument> documents = loader.load(List.of(tempDir.toString(), tempDir.resolve("missing").toString()));

        assertThat(documents).hasSize(1);
        assertThat(documents.get(0).title()).isEqualTo("第一份文档");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
mvn -pl agent-service -Dtest=RagMarkdownDocumentLoaderTest test
```

Expected: compilation fails because `RagDocument` and `RagMarkdownDocumentLoader` do not exist.

- [ ] **Step 3: Add document record**

Create `agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagDocument.java`:

```java
package com._202510007517.platform.agent.rag;

import java.util.Map;

public record RagDocument(
        String documentId,
        String title,
        String sourcePath,
        Map<String, String> metadata,
        String content
) {
}
```

- [ ] **Step 4: Add Markdown loader**

Create `agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagMarkdownDocumentLoader.java`:

```java
package com._202510007517.platform.agent.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class RagMarkdownDocumentLoader {
    private static final Logger log = LoggerFactory.getLogger(RagMarkdownDocumentLoader.class);
    private static final Pattern FIRST_HEADING = Pattern.compile("(?m)^#\\s+(.+)$");

    public List<RagDocument> load(List<String> configuredPaths) {
        List<RagDocument> documents = new ArrayList<>();
        for (String configuredPath : configuredPaths == null ? List.<String>of() : configuredPaths) {
            if (configuredPath == null || configuredPath.isBlank()) {
                continue;
            }
            Path path = Path.of(configuredPath).toAbsolutePath().normalize();
            if (!Files.exists(path)) {
                log.info("rag document path does not exist: {}", path);
                continue;
            }
            if (Files.isDirectory(path)) {
                documents.addAll(loadDirectory(path));
            } else if (isMarkdown(path)) {
                loadFile(path).ifPresent(documents::add);
            }
        }
        return documents;
    }

    private List<RagDocument> loadDirectory(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                    .filter(this::isMarkdown)
                    .sorted(Comparator.comparing(Path::toString))
                    .map(this::loadFile)
                    .flatMap(java.util.Optional::stream)
                    .toList();
        } catch (IOException ex) {
            log.info("failed to load rag directory: {}", directory);
            return List.of();
        }
    }

    private java.util.Optional<RagDocument> loadFile(Path file) {
        try {
            String raw = Files.readString(file);
            ParsedMarkdown parsed = parseFrontMatter(raw);
            String title = title(parsed.metadata(), parsed.content(), file);
            String source = file.toAbsolutePath().normalize().toString();
            return java.util.Optional.of(new RagDocument(source, title, source, parsed.metadata(), parsed.content()));
        } catch (IOException ex) {
            log.info("failed to load rag markdown file: {}", file);
            return java.util.Optional.empty();
        }
    }

    private ParsedMarkdown parseFrontMatter(String raw) {
        if (raw == null || !raw.startsWith("---")) {
            return new ParsedMarkdown(Map.of(), raw == null ? "" : raw);
        }
        String normalized = raw.replace("\r\n", "\n");
        int end = normalized.indexOf("\n---", 3);
        if (end < 0) {
            return new ParsedMarkdown(Map.of(), raw);
        }
        String frontMatter = normalized.substring(3, end).trim();
        String content = normalized.substring(end + 4).trim();
        Map<String, String> metadata = new LinkedHashMap<>();
        for (String line : frontMatter.split("\n")) {
            int separator = line.indexOf(':');
            if (separator > 0) {
                metadata.put(line.substring(0, separator).trim(), line.substring(separator + 1).trim());
            }
        }
        return new ParsedMarkdown(metadata, content);
    }

    private String title(Map<String, String> metadata, String content, Path file) {
        String explicit = metadata.get("title");
        if (explicit != null && !explicit.isBlank()) {
            return explicit;
        }
        Matcher matcher = FIRST_HEADING.matcher(content == null ? "" : content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        String fileName = file.getFileName().toString();
        return fileName.endsWith(".md") ? fileName.substring(0, fileName.length() - 3) : fileName;
    }

    private boolean isMarkdown(Path path) {
        return path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".md");
    }

    private record ParsedMarkdown(Map<String, String> metadata, String content) {
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run:

```powershell
mvn -pl agent-service -Dtest=RagMarkdownDocumentLoaderTest test
```

Expected: test passes.

- [ ] **Step 6: Commit**

```powershell
git add agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagDocument.java agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagMarkdownDocumentLoader.java agent-service/src/test/java/com/_202510007517/platform/agent/rag/RagMarkdownDocumentLoaderTest.java
git commit -m "feat(agent): load markdown rag documents"
```

---

### Task 3: Add Markdown Chunking

**Files:**
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagChunk.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagMarkdownChunker.java`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/rag/RagMarkdownChunkerTest.java`

- [ ] **Step 1: Write the failing test**

Create `agent-service/src/test/java/com/_202510007517/platform/agent/rag/RagMarkdownChunkerTest.java`:

```java
package com._202510007517.platform.agent.rag;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RagMarkdownChunkerTest {

    @Test
    void splitsDocumentByHeadingsAndPreservesMetadata() {
        RagDocument document = new RagDocument(
                "doc-1",
                "RAG 方案",
                "docs/rag-knowledge-base.md",
                Map.of("roleScope", "teacher", "courseId", "91005"),
                """
                # RAG 方案

                总体介绍。

                ## 适合回答的问题

                RAG 适合回答知识解释类问题。

                ## 不适合回答的问题

                RAG 不适合回答实时成绩和删除通知。
                """);

        RagMarkdownChunker chunker = new RagMarkdownChunker();
        List<RagChunk> chunks = chunker.chunk(document);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).chunkId()).isEqualTo("doc-1#1");
        assertThat(chunks.get(0).documentTitle()).isEqualTo("RAG 方案");
        assertThat(chunks.get(0).sectionTitle()).isEqualTo("适合回答的问题");
        assertThat(chunks.get(0).roleScope()).isEqualTo("teacher");
        assertThat(chunks.get(0).courseId()).isEqualTo("91005");
        assertThat(chunks.get(0).content()).contains("知识解释类问题");
        assertThat(chunks.get(1).sectionTitle()).isEqualTo("不适合回答的问题");
    }

    @Test
    void defaultsRoleScopeToAllAndUsesDocumentTitleWhenNoSectionExists() {
        RagDocument document = new RagDocument("doc-2", "平台指南", "platform.md", Map.of(), "只有一段正文。");

        RagMarkdownChunker chunker = new RagMarkdownChunker();
        List<RagChunk> chunks = chunker.chunk(document);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).sectionTitle()).isEqualTo("平台指南");
        assertThat(chunks.get(0).roleScope()).isEqualTo("all");
        assertThat(chunks.get(0).content()).isEqualTo("只有一段正文。");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
mvn -pl agent-service -Dtest=RagMarkdownChunkerTest test
```

Expected: compilation fails because `RagChunk` and `RagMarkdownChunker` do not exist.

- [ ] **Step 3: Add chunk record**

Create `agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagChunk.java`:

```java
package com._202510007517.platform.agent.rag;

public record RagChunk(
        String chunkId,
        String documentId,
        String documentTitle,
        String sectionTitle,
        String sourcePath,
        String roleScope,
        String courseId,
        String content,
        int sortOrder
) {
}
```

- [ ] **Step 4: Add chunker**

Create `agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagMarkdownChunker.java`:

```java
package com._202510007517.platform.agent.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RagMarkdownChunker {
    private static final Pattern SECTION_HEADING = Pattern.compile("(?m)^#{2,3}\\s+(.+)$");

    public List<RagChunk> chunk(RagDocument document) {
        String content = document.content() == null ? "" : document.content().trim();
        if (content.isBlank()) {
            return List.of();
        }
        List<Section> sections = sections(document.title(), content);
        List<RagChunk> chunks = new ArrayList<>();
        int order = 1;
        for (Section section : sections) {
            String text = stripLeadingHeadings(section.content()).trim();
            if (text.isBlank()) {
                continue;
            }
            chunks.add(new RagChunk(
                    document.documentId() + "#" + order,
                    document.documentId(),
                    document.title(),
                    section.title(),
                    document.sourcePath(),
                    valueOrDefault(document.metadata().get("roleScope"), "all"),
                    document.metadata().get("courseId"),
                    text,
                    order
            ));
            order++;
        }
        return chunks;
    }

    private List<Section> sections(String documentTitle, String content) {
        Matcher matcher = SECTION_HEADING.matcher(content);
        List<SectionBoundary> boundaries = new ArrayList<>();
        while (matcher.find()) {
            boundaries.add(new SectionBoundary(matcher.start(), matcher.end(), matcher.group(1).trim()));
        }
        if (boundaries.isEmpty()) {
            return List.of(new Section(documentTitle, stripTopTitle(content)));
        }
        List<Section> sections = new ArrayList<>();
        for (int i = 0; i < boundaries.size(); i++) {
            SectionBoundary current = boundaries.get(i);
            int end = i + 1 < boundaries.size() ? boundaries.get(i + 1).start() : content.length();
            sections.add(new Section(current.title(), content.substring(current.end(), end)));
        }
        return sections;
    }

    private String stripTopTitle(String content) {
        return content.replaceFirst("(?s)^#\\s+.+?(\\R\\R|\\R)", "").trim();
    }

    private String stripLeadingHeadings(String content) {
        return content.replaceFirst("(?s)^#{1,6}\\s+.+?(\\R\\R|\\R)", "").trim();
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private record SectionBoundary(int start, int end, String title) {
    }

    private record Section(String title, String content) {
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run:

```powershell
mvn -pl agent-service -Dtest=RagMarkdownChunkerTest test
```

Expected: test passes.

- [ ] **Step 6: Commit**

```powershell
git add agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagChunk.java agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagMarkdownChunker.java agent-service/src/test/java/com/_202510007517/platform/agent/rag/RagMarkdownChunkerTest.java
git commit -m "feat(agent): split rag markdown into chunks"
```

---

### Task 4: Add Embedding Client Abstraction and Ollama Implementation

**Files:**
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagEmbeddingClient.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/rag/OllamaRagEmbeddingClient.java`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/rag/OllamaRagEmbeddingClientTest.java`

- [ ] **Step 1: Write the failing test**

Create `agent-service/src/test/java/com/_202510007517/platform/agent/rag/OllamaRagEmbeddingClientTest.java`:

```java
package com._202510007517.platform.agent.rag;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OllamaRagEmbeddingClientTest {

    @Test
    void embedsTextThroughOllamaApi() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost:11434/api/embed"))
                .andExpect(content().json("""
                        {
                          "model": "qwen3-embedding:0.6b",
                          "input": "服务注册"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "embeddings": [[0.1, 0.2, 0.3]]
                        }
                        """, MediaType.APPLICATION_JSON));

        OllamaRagEmbeddingClient client = new OllamaRagEmbeddingClient(
                builder.baseUrl("http://localhost:11434").build(),
                "qwen3-embedding:0.6b");

        List<Double> vector = client.embed("服务注册");

        assertThat(vector).containsExactly(0.1, 0.2, 0.3);
        server.verify();
    }

    @Test
    void throwsClearExceptionWhenOllamaFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost:11434/api/embed"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        OllamaRagEmbeddingClient client = new OllamaRagEmbeddingClient(
                builder.baseUrl("http://localhost:11434").build(),
                "qwen3-embedding:0.6b");

        assertThatThrownBy(() -> client.embed("服务注册"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ollama embedding request failed");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
mvn -pl agent-service -Dtest=OllamaRagEmbeddingClientTest test
```

Expected: compilation fails because embedding classes do not exist.

- [ ] **Step 3: Add embedding client interface**

Create `agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagEmbeddingClient.java`:

```java
package com._202510007517.platform.agent.rag;

import java.util.List;

public interface RagEmbeddingClient {
    List<Double> embed(String text);
}
```

- [ ] **Step 4: Add Ollama client**

Create `agent-service/src/main/java/com/_202510007517/platform/agent/rag/OllamaRagEmbeddingClient.java`:

```java
package com._202510007517.platform.agent.rag;

import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

public class OllamaRagEmbeddingClient implements RagEmbeddingClient {
    private final RestClient restClient;
    private final String model;

    public OllamaRagEmbeddingClient(RestClient restClient, String model) {
        this.restClient = restClient;
        this.model = model;
    }

    @Override
    public List<Double> embed(String text) {
        try {
            OllamaEmbedResponse response = restClient.post()
                    .uri("/api/embed")
                    .body(Map.of("model", model, "input", text == null ? "" : text))
                    .retrieve()
                    .body(OllamaEmbedResponse.class);
            if (response == null || response.embeddings() == null || response.embeddings().isEmpty()) {
                throw new IllegalStateException("Ollama embedding response was empty.");
            }
            return response.embeddings().get(0);
        } catch (RuntimeException ex) {
            if (ex instanceof IllegalStateException state && state.getMessage().startsWith("Ollama embedding")) {
                throw state;
            }
            throw new IllegalStateException("Ollama embedding request failed.", ex);
        }
    }

    private record OllamaEmbedResponse(List<List<Double>> embeddings) {
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run:

```powershell
mvn -pl agent-service -Dtest=OllamaRagEmbeddingClientTest test
```

Expected: test passes.

- [ ] **Step 6: Commit**

```powershell
git add agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagEmbeddingClient.java agent-service/src/main/java/com/_202510007517/platform/agent/rag/OllamaRagEmbeddingClient.java agent-service/src/test/java/com/_202510007517/platform/agent/rag/OllamaRagEmbeddingClientTest.java
git commit -m "feat(agent): add ollama rag embedding client"
```

---

### Task 5: Add In-Memory Vector Index

**Files:**
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagSearchResult.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/rag/InMemoryRagIndex.java`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/rag/InMemoryRagIndexTest.java`

- [ ] **Step 1: Write the failing test**

Create `agent-service/src/test/java/com/_202510007517/platform/agent/rag/InMemoryRagIndexTest.java`:

```java
package com._202510007517.platform.agent.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRagIndexTest {

    @Test
    void ranksChunksByCosineSimilarityAndFiltersByRoleScope() {
        RagChunk teacherChunk = chunk("c1", "teacher", "服务注册与发现说明");
        RagChunk studentChunk = chunk("c2", "student", "学生复习路径说明");
        RagChunk allChunk = chunk("c3", "all", "平台通用说明");
        InMemoryRagIndex index = new InMemoryRagIndex();
        index.replaceAll(List.of(
                new InMemoryRagIndex.IndexedChunk(teacherChunk, List.of(1.0, 0.0)),
                new InMemoryRagIndex.IndexedChunk(studentChunk, List.of(0.0, 1.0)),
                new InMemoryRagIndex.IndexedChunk(allChunk, List.of(0.8, 0.2))
        ));

        List<RagSearchResult> results = index.search(List.of(1.0, 0.0), "TEACHER", 2, 0.0);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).chunk().chunkId()).isEqualTo("c1");
        assertThat(results.get(1).chunk().chunkId()).isEqualTo("c3");
        assertThat(results).extracting(result -> result.chunk().chunkId()).doesNotContain("c2");
    }

    @Test
    void appliesMinimumScore() {
        InMemoryRagIndex index = new InMemoryRagIndex();
        index.replaceAll(List.of(new InMemoryRagIndex.IndexedChunk(chunk("c1", "all", "低相关"), List.of(0.0, 1.0))));

        List<RagSearchResult> results = index.search(List.of(1.0, 0.0), "STUDENT", 4, 0.1);

        assertThat(results).isEmpty();
    }

    private RagChunk chunk(String id, String roleScope, String content) {
        return new RagChunk(id, "doc", "文档", "章节", "source.md", roleScope, null, content, 1);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
mvn -pl agent-service -Dtest=InMemoryRagIndexTest test
```

Expected: compilation fails because index classes do not exist.

- [ ] **Step 3: Add search result record**

Create `agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagSearchResult.java`:

```java
package com._202510007517.platform.agent.rag;

public record RagSearchResult(RagChunk chunk, double score) {
}
```

- [ ] **Step 4: Add index**

Create `agent-service/src/main/java/com/_202510007517/platform/agent/rag/InMemoryRagIndex.java`:

```java
package com._202510007517.platform.agent.rag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public class InMemoryRagIndex {
    private final AtomicReference<List<IndexedChunk>> indexedChunks = new AtomicReference<>(List.of());

    public void replaceAll(List<IndexedChunk> chunks) {
        indexedChunks.set(List.copyOf(chunks));
    }

    public boolean isEmpty() {
        return indexedChunks.get().isEmpty();
    }

    public List<RagSearchResult> search(List<Double> queryVector, String userRole, int maxChunks, double minScore) {
        List<RagSearchResult> results = new ArrayList<>();
        for (IndexedChunk indexedChunk : indexedChunks.get()) {
            if (!isVisible(indexedChunk.chunk().roleScope(), userRole)) {
                continue;
            }
            double score = cosine(queryVector, indexedChunk.embedding());
            if (score >= minScore) {
                results.add(new RagSearchResult(indexedChunk.chunk(), score));
            }
        }
        return results.stream()
                .sorted(Comparator.comparingDouble(RagSearchResult::score).reversed())
                .limit(Math.max(1, maxChunks))
                .toList();
    }

    private boolean isVisible(String roleScope, String userRole) {
        String scope = normalize(roleScope);
        String role = normalize(userRole);
        return "all".equals(scope) || scope.equals(role);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "all";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("role_") ? normalized.substring("role_".length()) : normalized;
    }

    private double cosine(List<Double> left, List<Double> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }
        int size = Math.min(left.size(), right.size());
        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int i = 0; i < size; i++) {
            double l = left.get(i);
            double r = right.get(i);
            dot += l * r;
            leftNorm += l * l;
            rightNorm += r * r;
        }
        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    public record IndexedChunk(RagChunk chunk, List<Double> embedding) {
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run:

```powershell
mvn -pl agent-service -Dtest=InMemoryRagIndexTest test
```

Expected: test passes.

- [ ] **Step 6: Commit**

```powershell
git add agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagSearchResult.java agent-service/src/main/java/com/_202510007517/platform/agent/rag/InMemoryRagIndex.java agent-service/src/test/java/com/_202510007517/platform/agent/rag/InMemoryRagIndexTest.java
git commit -m "feat(agent): add in-memory rag vector index"
```

---

### Task 6: Add RAG Knowledge Service

**Files:**
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagKnowledgeService.java`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/rag/RagKnowledgeServiceTest.java`

- [ ] **Step 1: Write the failing test**

Create `agent-service/src/test/java/com/_202510007517/platform/agent/rag/RagKnowledgeServiceTest.java`:

```java
package com._202510007517.platform.agent.rag;

import com._202510007517.platform.agent.config.AgentRagProperties;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RagKnowledgeServiceTest {

    @Test
    void returnsGeneratedAnswerWithSources() {
        AgentRagProperties properties = new AgentRagProperties();
        properties.setEnabled(true);
        properties.setMaxChunks(2);
        FakeEmbeddingClient embeddingClient = new FakeEmbeddingClient();
        InMemoryRagIndex index = new InMemoryRagIndex();
        RagChunk chunk = new RagChunk(
                "chunk-1",
                "doc-1",
                "RAG 方案",
                "适合回答的问题",
                "docs/rag-knowledge-base.md",
                "all",
                null,
                "RAG 适合回答知识解释类问题。",
                1);
        index.replaceAll(List.of(new InMemoryRagIndex.IndexedChunk(chunk, List.of(1.0, 0.0))));
        CapturingChatModel chatModel = new CapturingChatModel("RAG 可以回答知识解释类问题。");

        RagKnowledgeService service = new RagKnowledgeService(properties, embeddingClient, index, chatModel);

        String answer = service.answer(7L, "STUDENT", "什么是 RAG？");

        assertThat(answer).contains("RAG 可以回答知识解释类问题。");
        assertThat(answer).contains("来源：RAG 方案 / 适合回答的问题");
        assertThat(chatModel.prompts).hasSize(1);
        assertThat(chatModel.prompts.get(0)).contains("RAG 适合回答知识解释类问题。");
    }

    @Test
    void returnsUnavailableMessageWhenEmbeddingFails() {
        AgentRagProperties properties = new AgentRagProperties();
        properties.setEnabled(true);
        RagEmbeddingClient failingClient = text -> {
            throw new IllegalStateException("Ollama embedding request failed.");
        };

        RagKnowledgeService service = new RagKnowledgeService(
                properties,
                failingClient,
                new InMemoryRagIndex(),
                null);

        String answer = service.answer(7L, "TEACHER", "解释服务注册");

        assertThat(answer).contains("知识库暂时不可用");
    }

    @Test
    void returnsInsufficientContextWhenNoChunksMatch() {
        AgentRagProperties properties = new AgentRagProperties();
        properties.setEnabled(true);
        properties.setMinScore(0.5);
        RagKnowledgeService service = new RagKnowledgeService(
                properties,
                text -> List.of(1.0, 0.0),
                new InMemoryRagIndex(),
                null);

        String answer = service.answer(7L, "STUDENT", "解释不存在的内容");

        assertThat(answer).contains("没有在知识库中找到足够依据");
    }

    private static class FakeEmbeddingClient implements RagEmbeddingClient {
        @Override
        public List<Double> embed(String text) {
            return List.of(1.0, 0.0);
        }
    }

    private static class CapturingChatModel implements ChatModel {
        private final String answer;
        private final List<String> prompts = new ArrayList<>();

        private CapturingChatModel(String answer) {
            this.answer = answer;
        }

        @Override
        public String chat(String userMessage) {
            prompts.add(userMessage);
            return answer;
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
mvn -pl agent-service -Dtest=RagKnowledgeServiceTest test
```

Expected: compilation fails because `RagKnowledgeService` does not exist.

- [ ] **Step 3: Add RAG knowledge service**

Create `agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagKnowledgeService.java`:

```java
package com._202510007517.platform.agent.rag;

import com._202510007517.platform.agent.config.AgentRagProperties;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RagKnowledgeService {
    private static final Logger log = LoggerFactory.getLogger(RagKnowledgeService.class);

    private final AgentRagProperties properties;
    private final RagEmbeddingClient embeddingClient;
    private final InMemoryRagIndex index;
    private final ChatModel chatModel;

    public RagKnowledgeService(AgentRagProperties properties,
                               RagEmbeddingClient embeddingClient,
                               InMemoryRagIndex index,
                               ChatModel chatModel) {
        this.properties = properties;
        this.embeddingClient = embeddingClient;
        this.index = index;
        this.chatModel = chatModel;
    }

    public String answer(Long userId, String userRole, String question) {
        if (!properties.isEnabled()) {
            return "知识库问答暂未启用。";
        }
        List<RagSearchResult> results;
        try {
            List<Double> queryVector = embeddingClient.embed(question);
            results = index.search(queryVector, userRole, properties.getMaxChunks(), properties.getMinScore());
        } catch (RuntimeException ex) {
            log.info("rag answer failed before generation: reason={}", ex.getClass().getSimpleName());
            return "知识库暂时不可用，请稍后再试。";
        }
        if (results.isEmpty()) {
            return "没有在知识库中找到足够依据，暂时无法基于资料回答这个问题。";
        }
        String generated = generate(question, userRole, results);
        return generated + "\n\n" + sources(results);
    }

    private String generate(String question, String userRole, List<RagSearchResult> results) {
        String context = results.stream()
                .map(result -> "[来源: %s / %s]\n%s".formatted(
                        result.chunk().documentTitle(),
                        result.chunk().sectionTitle(),
                        result.chunk().content()))
                .collect(Collectors.joining("\n\n"));
        String prompt = """
                你是教学管理平台内置的 RAG 知识问答助手。
                当前用户角色：%s。

                只能依据下方知识库片段回答。不能编造实时课程、作业、考试、成绩、通知或学生名单。
                如果知识库片段不足以回答，请明确说明没有足够依据。
                回答保持中文、简洁、适合教学平台用户阅读。

                知识库片段：
                %s

                用户问题：
                %s
                """.formatted(userRole == null ? "UNKNOWN" : userRole, context, question == null ? "" : question);
        if (chatModel == null) {
            return fallbackAnswer(results);
        }
        try {
            String answer = chatModel.chat(prompt);
            if (answer == null || answer.isBlank()) {
                return fallbackAnswer(results);
            }
            return answer.trim();
        } catch (RuntimeException ex) {
            log.info("rag chat generation failed: reason={}", ex.getClass().getSimpleName());
            return fallbackAnswer(results);
        }
    }

    private String fallbackAnswer(List<RagSearchResult> results) {
        return results.stream()
                .map(result -> result.chunk().content())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("没有在知识库中找到足够依据，暂时无法基于资料回答这个问题。");
    }

    private String sources(List<RagSearchResult> results) {
        String sourceText = results.stream()
                .map(result -> "来源：%s / %s".formatted(result.chunk().documentTitle(), result.chunk().sectionTitle()))
                .distinct()
                .collect(Collectors.joining("\n"));
        return sourceText.isBlank() ? "来源：知识库" : sourceText;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
mvn -pl agent-service -Dtest=RagKnowledgeServiceTest test
```

Expected: test passes.

- [ ] **Step 5: Commit**

```powershell
git add agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagKnowledgeService.java agent-service/src/test/java/com/_202510007517/platform/agent/rag/RagKnowledgeServiceTest.java
git commit -m "feat(agent): answer rag knowledge questions"
```

---

### Task 7: Wire RAG Beans and Build the Index

**Files:**
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/config/AgentRagConfiguration.java`
- Modify: `agent-service/src/main/resources/application.yml`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/config/AgentRagConfigurationTest.java`

- [ ] **Step 1: Write the failing test**

Create `agent-service/src/test/java/com/_202510007517/platform/agent/config/AgentRagConfigurationTest.java`:

```java
package com._202510007517.platform.agent.config;

import com._202510007517.platform.agent.rag.InMemoryRagIndex;
import com._202510007517.platform.agent.rag.RagEmbeddingClient;
import com._202510007517.platform.agent.rag.RagKnowledgeService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRagConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AgentRagConfiguration.class));

    @Test
    void createsOnlyDisabledRagServiceWhenDisabled() {
        contextRunner
                .withPropertyValues("agent.rag.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(RagKnowledgeService.class);
                    assertThat(context).doesNotHaveBean(RagEmbeddingClient.class);
                    assertThat(context).doesNotHaveBean(InMemoryRagIndex.class);
                });
    }

    @Test
    void createsRagServiceAndIndexWhenEnabled() {
        contextRunner
                .withUserConfiguration(FakeEmbeddingConfiguration.class)
                .withPropertyValues(
                        "agent.rag.enabled=true",
                        "agent.rag.document-paths[0]=docs/rag-knowledge-base.md")
                .run(context -> {
                    assertThat(context).hasSingleBean(RagKnowledgeService.class);
                    assertThat(context).hasSingleBean(InMemoryRagIndex.class);
                });
    }

    @Configuration
    static class FakeEmbeddingConfiguration {
        @Bean
        RagEmbeddingClient ragEmbeddingClient() {
            return text -> List.of(1.0, 0.0);
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
mvn -pl agent-service -Dtest=AgentRagConfigurationTest test
```

Expected: compilation fails because `AgentRagConfiguration` does not exist.

- [ ] **Step 3: Add configuration class**

Create `agent-service/src/main/java/com/_202510007517/platform/agent/config/AgentRagConfiguration.java`:

```java
package com._202510007517.platform.agent.config;

import com._202510007517.platform.agent.rag.InMemoryRagIndex;
import com._202510007517.platform.agent.rag.OllamaRagEmbeddingClient;
import com._202510007517.platform.agent.rag.RagChunk;
import com._202510007517.platform.agent.rag.RagDocument;
import com._202510007517.platform.agent.rag.RagEmbeddingClient;
import com._202510007517.platform.agent.rag.RagKnowledgeService;
import com._202510007517.platform.agent.rag.RagMarkdownChunker;
import com._202510007517.platform.agent.rag.RagMarkdownDocumentLoader;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties(AgentRagProperties.class)
public class AgentRagConfiguration {
    private static final Logger log = LoggerFactory.getLogger(AgentRagConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "agent.rag", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    RagEmbeddingClient ragEmbeddingClient(AgentRagProperties properties) {
        RestClient restClient = RestClient.builder()
                .baseUrl(properties.getEmbeddingBaseUrl())
                .build();
        return new OllamaRagEmbeddingClient(restClient, properties.getEmbeddingModel());
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.rag", name = "enabled", havingValue = "true")
    InMemoryRagIndex inMemoryRagIndex(AgentRagProperties properties, RagEmbeddingClient embeddingClient) {
        RagMarkdownDocumentLoader loader = new RagMarkdownDocumentLoader();
        RagMarkdownChunker chunker = new RagMarkdownChunker();
        List<RagDocument> documents = loader.load(properties.getDocumentPaths());
        List<InMemoryRagIndex.IndexedChunk> indexedChunks = new ArrayList<>();
        for (RagDocument document : documents) {
            for (RagChunk chunk : chunker.chunk(document)) {
                try {
                    indexedChunks.add(new InMemoryRagIndex.IndexedChunk(chunk, embeddingClient.embed(chunk.content())));
                } catch (RuntimeException ex) {
                    log.info("failed to embed rag chunk: chunkId={}, reason={}", chunk.chunkId(), ex.getClass().getSimpleName());
                }
            }
        }
        InMemoryRagIndex index = new InMemoryRagIndex();
        index.replaceAll(indexedChunks);
        log.info("rag index initialized: documents={}, chunks={}", documents.size(), indexedChunks.size());
        return index;
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.rag", name = "enabled", havingValue = "true")
    RagKnowledgeService ragKnowledgeService(AgentRagProperties properties,
                                            RagEmbeddingClient embeddingClient,
                                            InMemoryRagIndex index,
                                            ObjectProvider<ChatModel> chatModelProvider) {
        return new RagKnowledgeService(properties, embeddingClient, index, chatModelProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean(RagKnowledgeService.class)
    RagKnowledgeService disabledRagKnowledgeService() {
        AgentRagProperties properties = new AgentRagProperties();
        properties.setEnabled(false);
        return new RagKnowledgeService(properties, text -> List.of(), new InMemoryRagIndex(), null);
    }
}
```

- [ ] **Step 4: Add application configuration**

Modify `agent-service/src/main/resources/application.yml`.

Add under the top-level `agent:` block:

```yaml
  rag:
    enabled: ${AGENT_RAG_ENABLED:false}
    document-paths:
      - ${AGENT_RAG_DOCUMENT_PATH:docs/rag-knowledge-base.md}
    embedding-base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
    embedding-model: ${OLLAMA_EMBEDDING_MODEL:qwen3-embedding:0.6b}
    max-chunks: ${AGENT_RAG_MAX_CHUNKS:4}
    min-score: ${AGENT_RAG_MIN_SCORE:0.0}
```

Add under the `test` profile `agent:` block:

```yaml
  rag:
    enabled: false
```

- [ ] **Step 5: Run test to verify it passes**

Run:

```powershell
mvn -pl agent-service -Dtest=AgentRagConfigurationTest test
```

Expected: test passes.

- [ ] **Step 6: Commit**

```powershell
git add agent-service/src/main/java/com/_202510007517/platform/agent/config/AgentRagConfiguration.java agent-service/src/main/resources/application.yml agent-service/src/test/java/com/_202510007517/platform/agent/config/AgentRagConfigurationTest.java
git commit -m "feat(agent): wire rag knowledge beans"
```

---

### Task 8: Add RAG Intent Routing

**Files:**
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/model/AgentIntent.java`
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/service/RuleBasedIntentRecognitionService.java`
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentPermissionPolicy.java`
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentConfirmationPolicy.java`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/service/RuleBasedIntentRecognitionServiceTest.java`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentPermissionPolicyTest.java`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentConfirmationPolicyTest.java`

- [ ] **Step 1: Add failing intent recognition tests**

Append to `RuleBasedIntentRecognitionServiceTest`:

```java
    @Test
    void recognizesRagKnowledgeQuestion() {
        RecognizedIntent result = service.recognize("什么是服务注册与发现？");

        assertThat(result.intent()).isEqualTo(AgentIntent.QUERY_RAG_KNOWLEDGE);
        assertThat(result.confidence()).isEqualTo(0.9);
    }

    @Test
    void keepsBusinessQueriesOutOfRagRouting() {
        RecognizedIntent assignmentQuery = service.recognize("课程ID 91005 有哪些作业？");
        RecognizedIntent deleteNotification = service.recognize("帮我删除通知ID 9");

        assertThat(assignmentQuery.intent()).isEqualTo(AgentIntent.QUERY_ASSIGNMENTS);
        assertThat(deleteNotification.intent()).isEqualTo(AgentIntent.DELETE_NOTIFICATION);
    }
```

Append to `AgentConfirmationPolicyTest`:

```java
    @Test
    void ragKnowledgeQueryDoesNotRequireConfirmation() {
        assertThat(policy.requiresConfirmation(AgentIntent.QUERY_RAG_KNOWLEDGE)).isFalse();
    }
```

Append to `AgentPermissionPolicyTest`:

```java
    @Test
    void teacherAndStudentCanUseRagKnowledgeQuery() {
        AgentPermissionPolicy policy = new AgentPermissionPolicy();

        policy.assertAllowed(7L, "TEACHER", AgentIntent.QUERY_RAG_KNOWLEDGE);
        policy.assertAllowed(8L, "STUDENT", AgentIntent.QUERY_RAG_KNOWLEDGE);
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
mvn -pl agent-service -Dtest=RuleBasedIntentRecognitionServiceTest,AgentConfirmationPolicyTest,AgentPermissionPolicyTest test
```

Expected: compilation fails because `QUERY_RAG_KNOWLEDGE` does not exist.

- [ ] **Step 3: Add enum value**

Modify `AgentIntent.java` by adding `QUERY_RAG_KNOWLEDGE` before `UNKNOWN`:

```java
    QUERY_RAG_KNOWLEDGE,
    UNKNOWN
```

- [ ] **Step 4: Allow RAG in policies**

Modify `AgentPermissionPolicy.java`:

Add `AgentIntent.QUERY_RAG_KNOWLEDGE` to both `TEACHER_INTENTS` and `STUDENT_INTENTS`.

Modify `AgentConfirmationPolicy.java`:

Add `QUERY_RAG_KNOWLEDGE` to the no-confirmation switch arm:

```java
                    QUERY_QUESTION_BANK, QUERY_RAG_KNOWLEDGE,
                    GENERATE_QUESTIONS, GENERATE_LEARNING_SUGGESTIONS, UNKNOWN -> false;
```

- [ ] **Step 5: Add conservative RAG detection**

Modify `RuleBasedIntentRecognitionService.java`.

Near the end of `detectIntent`, before `return AgentIntent.UNKNOWN;`, add:

```java
        if (isRagKnowledgeQuestion(text)) {
            return AgentIntent.QUERY_RAG_KNOWLEDGE;
        }
```

Add helper methods near existing helpers:

```java
    private static boolean isRagKnowledgeQuestion(String text) {
        if (containsBusinessActionOrLiveData(text)) {
            return false;
        }
        return containsAny(text,
                "什么是", "解释", "说明", "如何理解", "平台怎么", "学生如何", "教师如何",
                "怎么使用AI助手", "怎么使用 AI 助手", "服务注册", "配置中心", "网关", "RAG");
    }

    private static boolean containsBusinessActionOrLiveData(String text) {
        return containsAny(text,
                "删除", "发布", "批改", "提交", "发送通知", "发通知", "多少分", "名单",
                "有哪些作业", "有哪些考试", "查询作业", "查询考试", "课程ID", "作业ID", "考试ID", "通知ID");
    }
```

- [ ] **Step 6: Run tests to verify they pass**

Run:

```powershell
mvn -pl agent-service -Dtest=RuleBasedIntentRecognitionServiceTest,AgentConfirmationPolicyTest,AgentPermissionPolicyTest test
```

Expected: tests pass.

- [ ] **Step 7: Commit**

```powershell
git add agent-service/src/main/java/com/_202510007517/platform/agent/model/AgentIntent.java agent-service/src/main/java/com/_202510007517/platform/agent/service/RuleBasedIntentRecognitionService.java agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentPermissionPolicy.java agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentConfirmationPolicy.java agent-service/src/test/java/com/_202510007517/platform/agent/service/RuleBasedIntentRecognitionServiceTest.java agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentPermissionPolicyTest.java agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentConfirmationPolicyTest.java
git commit -m "feat(agent): route knowledge questions to rag intent"
```

---

### Task 9: Route RAG Intent in AgentOrchestrator

**Files:**
- Modify: `agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentOrchestratorTest.java`

- [ ] **Step 1: Write failing orchestrator test**

Modify `AgentOrchestratorTest`.

Add import:

```java
import com._202510007517.platform.agent.rag.RagKnowledgeService;
```

Add field:

```java
    @MockitoBean
    private RagKnowledgeService ragKnowledgeService;
```

Add setup line in `clearActions()`:

```java
        when(ragKnowledgeService.answer(any(), any(), any()))
                .thenReturn("服务注册与发现用于让服务实例自动注册并被调用方发现。\n\n来源：RAG 方案 / 适合回答的问题");
```

Add test:

```java
    @Test
    void routesRagKnowledgeIntentToRagServiceWithoutCreatingAction() {
        AgentChatResponseDTO response = orchestrator.chat(7L, "STUDENT", null, "什么是服务注册与发现？");

        assertThat(response.getResponseType()).isEqualTo("TEXT");
        assertThat(response.getActionPreview()).isNull();
        assertThat(response.getMessage()).contains("服务注册与发现");
        assertThat(actionRepository.findAll()).isEmpty();
        verify(ragKnowledgeService).answer(7L, "STUDENT", "什么是服务注册与发现？");
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
mvn -pl agent-service -Dtest=AgentOrchestratorTest#routesRagKnowledgeIntentToRagServiceWithoutCreatingAction test
```

Expected: test fails because `AgentOrchestrator` does not route to `RagKnowledgeService`.

- [ ] **Step 3: Modify AgentOrchestrator constructor**

Add import:

```java
import com._202510007517.platform.agent.rag.RagKnowledgeService;
```

Add field:

```java
    private final RagKnowledgeService ragKnowledgeService;
```

Add constructor parameter before `ObjectMapper objectMapper`:

```java
                             RagKnowledgeService ragKnowledgeService,
```

Assign:

```java
        this.ragKnowledgeService = ragKnowledgeService;
```

- [ ] **Step 4: Add route in chat flow**

In `chat(...)`, after `permissionPolicy.assertAllowed(...)` and before `contextEnrichmentService.enrich(...)`, add:

```java
        if (recognizedIntent.intent() == AgentIntent.QUERY_RAG_KNOWLEDGE) {
            clearPendingContext(session);
            response = textResponse(session, ragKnowledgeService.answer(userId, userRole, message));
            sessionService.saveAssistantMessage(session.getId(), response, recognizedIntent);
            return response;
        }
```

- [ ] **Step 5: Run test to verify it passes**

Run:

```powershell
mvn -pl agent-service -Dtest=AgentOrchestratorTest#routesRagKnowledgeIntentToRagServiceWithoutCreatingAction test
```

Expected: test passes.

- [ ] **Step 6: Commit**

```powershell
git add agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java agent-service/src/main/java/com/_202510007517/platform/agent/config/AgentRagConfiguration.java agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentOrchestratorTest.java
git commit -m "feat(agent): route rag knowledge intent"
```

---

### Task 10: Add Contract Script for RAG Configuration

**Files:**
- Create: `scripts/verify-agent-rag-contract.js`

- [ ] **Step 1: Write contract script**

Create `scripts/verify-agent-rag-contract.js`:

```javascript
const fs = require('fs');
const path = require('path');

const root = process.cwd();

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

function assertContains(file, expected) {
  const content = read(file);
  if (!content.includes(expected)) {
    throw new Error(`${file} does not contain expected text: ${expected}`);
  }
}

assertContains('agent-service/src/main/resources/application.yml', 'rag:');
assertContains('agent-service/src/main/resources/application.yml', 'AGENT_RAG_ENABLED');
assertContains('agent-service/src/main/resources/application.yml', 'OLLAMA_EMBEDDING_MODEL');
assertContains('agent-service/src/main/java/com/_202510007517/platform/agent/model/AgentIntent.java', 'QUERY_RAG_KNOWLEDGE');
assertContains('agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java', 'ragKnowledgeService.answer');
assertContains('agent-service/src/main/java/com/_202510007517/platform/agent/rag/RagKnowledgeService.java', '只能依据下方知识库片段回答');
assertContains('docs/rag-knowledge-base.md', 'RAG 外挂知识库');

console.log('agent rag contract verified');
```

- [ ] **Step 2: Run script**

Run:

```powershell
node scripts/verify-agent-rag-contract.js
```

Expected: prints `agent rag contract verified`.

- [ ] **Step 3: Commit**

```powershell
git add scripts/verify-agent-rag-contract.js
git commit -m "test(agent): add rag contract verification"
```

---

### Task 11: Run Focused Verification

**Files:**
- No code changes expected.

- [ ] **Step 1: Run RAG-focused unit tests**

Run:

```powershell
mvn -pl agent-service -Dtest=AgentRagPropertiesTest,RagMarkdownDocumentLoaderTest,RagMarkdownChunkerTest,OllamaRagEmbeddingClientTest,InMemoryRagIndexTest,RagKnowledgeServiceTest,AgentRagConfigurationTest test
```

Expected: all selected tests pass.

- [ ] **Step 2: Run Agent routing and policy tests**

Run:

```powershell
mvn -pl agent-service -Dtest=RuleBasedIntentRecognitionServiceTest,AgentConfirmationPolicyTest,AgentPermissionPolicyTest,AgentOrchestratorTest test
```

Expected: all selected tests pass.

- [ ] **Step 3: Run contract script**

Run:

```powershell
node scripts/verify-agent-rag-contract.js
```

Expected: prints `agent rag contract verified`.

- [ ] **Step 4: Optional local Ollama manual check**

Only run after GPU driver update and Ollama are stable:

```powershell
ollama run qwen3-embedding:0.6b "测试文本"
```

Expected: Ollama returns embedding-model output without CUDA error. If GPU still fails, restart Ollama in CPU mode and retry:

```powershell
Stop-Process -Name ollama,llama-server -Force -ErrorAction SilentlyContinue
$env:CUDA_VISIBLE_DEVICES="-1"
ollama serve
```

- [ ] **Step 5: Commit verification note if docs changed**

If a verification result is added to docs, commit it. Otherwise do not create an empty commit.

```powershell
git status --short
```

Expected: only intentional files remain modified.
