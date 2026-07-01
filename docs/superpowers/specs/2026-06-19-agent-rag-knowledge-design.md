# Agent RAG Knowledge Design

## Goal

Add a first-stage RAG knowledge answering path to the existing AI assistant so teachers and students can ask explanation, platform-help, and course-material questions without using RAG as a substitute for authoritative business queries.

## Scope

This design covers the first local-demo RAG slice only:

- Use Markdown documents as the knowledge source.
- Start from the existing `docs/rag-knowledge-base.md` content and later allow documents under `docs/rag/`.
- Integrate RAG inside `agent-service`, because frontend chat requests already flow through `gateway -> agent-service -> AgentOrchestrator`.
- Use Ollama for embeddings with `qwen3-embedding:0.6b`.
- Use the existing Xiaomi-compatible OpenAI chat model configuration for final answer generation.
- Keep RAG read-only. It does not create, update, delete, or fabricate business records.

Out of scope for this first slice:

- PDF or DOCX direct ingestion.
- Persistent vector database tables.
- File upload UI.
- Cross-course permission matrix beyond metadata filtering hooks.
- Replacing existing Agent tools.

## Current Context

The repository already has:

- `agent-service`, which owns Agent sessions, messages, intent recognition, action preview, confirmation, execution, and audit.
- `agent.llm.*` configuration in `agent-service/src/main/resources/application.yml`.
- LangChain4j chat model wiring in `AgentIntentRecognitionConfiguration`.
- Existing frontend Agent chat pages that call `/api/agent/chat`.
- `docs/rag-knowledge-base.md`, a formal Markdown document describing RAG boundaries, suitable questions, unsuitable questions, chunking, metadata, and Agent integration.

Therefore RAG belongs in `agent-service`, not `ai-service`. The `ai-service` remains focused on question generation, exam generation, and learning suggestions.

## User-Facing Behavior

When a user asks a knowledge-style question, the Agent should answer from retrieved documents and include short source references. Example questions:

- "什么是服务注册与发现？"
- "Spring Cloud Gateway 在本系统中负责什么？"
- "学生应该如何使用 AI 助手复习课程知识点？"
- "教师如何通过 AI 助手辅助分析学生学习情况？"

When a user asks for live business data or a write operation, the Agent must keep using existing tools. Example non-RAG questions:

- "张三这门课考了多少分？"
- "课程 ID 91005 有哪些作业？"
- "帮我删除这条通知。"
- "查询云计算技术课程的学生名单。"

For mixed questions, the first slice may route to the stronger existing business intent when recognized. A later slice can combine tool results with RAG explanation.

## Architecture

The first-stage flow is:

```text
frontend AI assistant
  -> gateway
  -> agent-service /api/agent/chat
  -> AgentOrchestrator
  -> intent recognition
  -> existing tool path OR RAG knowledge path
  -> Markdown document loader
  -> chunk splitter
  -> Ollama embedding client
  -> in-memory vector search
  -> Xiaomi chat model answer generation
  -> response with source references
```

The design intentionally keeps storage in memory for this stage. This makes the feature demonstrable while GPU driver and Ollama setup are still being stabilized. Persistent vector storage can be added after the answering behavior is verified.

## Components

### RAG Properties

Add `agent.rag.*` configuration in `agent-service`:

- `enabled`: default `false` for test profile, configurable for dev.
- `document-paths`: Markdown files or directories to load.
- `embedding-base-url`: default `http://localhost:11434`.
- `embedding-model`: default `qwen3-embedding:0.6b`.
- `max-chunks`: retrieval limit, default `4`.
- `min-score`: optional similarity threshold.

### Document Loader

The loader reads Markdown files from configured paths. It extracts:

- document title from front matter `title`, or first `#` heading, or file name.
- source path.
- optional metadata from front matter, such as `roleScope`, `courseId`, and `sourceType`.

### Chunk Splitter

The splitter uses Markdown headings as primary boundaries. It should produce chunks with:

- stable chunk id.
- document title.
- section title.
- source path.
- role scope.
- text content.
- sort order.

For the first slice, chunks can be 300 to 800 Chinese characters when possible. Short adjacent paragraphs may be merged; very long sections may be split by paragraphs.

### Embedding Client

The embedding client calls Ollama's local embed API. It must be isolated behind a small interface so tests can use deterministic fake vectors without requiring Ollama.

If Ollama is unavailable, the RAG path should fail gracefully and return a clear message instead of breaking the whole Agent chat endpoint.

### Vector Search

The first implementation stores chunk embeddings in memory and ranks chunks by cosine similarity. The index can be built at startup or lazily on the first RAG request.

### Answer Generation

The RAG answer generator uses the existing LangChain4j `ChatModel` bean when `agent.llm.enabled=true`. The prompt must instruct the model to:

- answer only from retrieved context.
- say it cannot find enough information when context is insufficient.
- not invent live business data.
- include source names in the answer.

When `agent.llm.enabled=false`, tests should use deterministic fallback text so service behavior can be verified without an external model.

## Intent Routing

Add a read-only intent named `QUERY_RAG_KNOWLEDGE`.

The first slice can classify RAG questions with conservative keyword and pattern rules before falling back to existing intent recognition. It should prefer existing tool intents when the message contains live data or write-action language.

RAG candidates include:

- "什么是..."
- "解释..."
- "说明..."
- "如何理解..."
- "平台怎么..."
- "学生如何..."
- "教师如何..."

Non-RAG indicators include:

- "查询...名单"
- "多少分"
- "有哪些作业"
- "删除"
- "发布"
- "批改"
- "提交"
- concrete ids when paired with business entities.

## Permissions

The first slice should include metadata hooks for `roleScope` filtering:

- `all`: visible to all roles.
- `teacher`: visible to teachers.
- `student`: visible to students.

If metadata is absent, default to `all` for the current demo document. Course-level permission checks are reserved for a later persistent knowledge-base slice.

## Error Handling

The Agent chat endpoint should remain usable if RAG dependencies fail.

- Missing document path: start with an empty index and log the path.
- Ollama unavailable: return a RAG-specific unavailable response.
- Empty retrieval result: return a "没有在知识库中找到足够依据" style answer.
- LLM unavailable: return a concise answer assembled from top retrieved snippets and source titles.

## Testing

Use TDD for implementation.

Test coverage should include:

- Markdown front matter and heading extraction.
- Chunk splitting and metadata preservation.
- Cosine similarity ranking with deterministic vectors.
- RAG routing chooses `QUERY_RAG_KNOWLEDGE` for explanation questions.
- Existing business/action-style prompts do not get stolen by RAG.
- RAG disabled mode leaves existing Agent behavior unchanged.
- Ollama failure is handled without a 500 response from `/api/agent/chat`.

## Verification

Manual verification after implementation:

1. Confirm Ollama is installed and can embed:

```powershell
ollama run qwen3-embedding:0.6b "测试文本"
```

2. Start the Java services normally.
3. Ask the teacher or student AI assistant:

```text
什么是服务注册与发现？
```

Expected result: an explanatory answer based on the Markdown knowledge base, with source reference text.

4. Ask a business query:

```text
课程 ID 91005 有哪些作业？
```

Expected result: existing Agent tool behavior, not RAG-only text.

## Future Work

After the first slice is stable:

- Add database-backed document and chunk tables.
- Add persistent vector storage.
- Add PDF and DOCX to Markdown conversion.
- Add admin upload and reindex workflow.
- Combine tool results with RAG explanations for mixed business-plus-concept questions.
