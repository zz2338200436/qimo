# Agent Frontend Streaming Output Design

## Goal

Add incremental streaming output to the existing Agent chat UI so users can see assistant replies arrive progressively, while keeping the current structured action preview and data card workflows stable.

## Scope

This design covers the first delivery slice of Agent frontend streaming:

- Add a new streaming chat endpoint in `agent-service`.
- Keep the existing `POST /api/agent/chat` endpoint unchanged for compatibility.
- Update the Agent chat panel frontend to prefer the streaming endpoint.
- Stream plain-text chat content incrementally.
- Preserve existing `DATA` and `ACTION_PREVIEW` rendering by delivering a final structured payload event.
- Support request cancellation from the existing pause button.

Out of scope for this slice:

- Rebuilding the Agent around `AiServices`.
- Replacing the current session, action, or confirmation model.
- Reworking `agent-history-panel.js`.
- Streaming action confirmation or cancellation flows.
- Building a new WebSocket channel.

## Current Context

The repository already has:

- `agent-service/src/main/java/com/_202510007517/platform/agent/controller/AgentController.java`, which exposes only one-shot JSON chat.
- `agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java`, which returns a final `AgentChatResponseDTO`.
- `major_assignment/src/main/resources/static/agent-chat-panel.js`, which currently posts to `/api/agent/chat` and renders the final payload after the whole request completes.
- Existing frontend card rendering for:
  - `TEXT`
  - `DATA`
  - `ACTION_PREVIEW`
- Existing gateway route coverage for `/api/agent/**`.

That means the lowest-risk path is to add a separate streaming endpoint and let the current UI reuse its existing final-response rendering code.

## Chosen Approach

Use a separate `POST /api/agent/chat/stream` endpoint that returns `text/event-stream`, and let the frontend call it with `fetch()` plus a stream reader.

This keeps the current non-streaming path intact, avoids forcing `EventSource` into a `GET`-only shape, and allows the same request body as the existing chat endpoint.

### Why this approach

- It isolates risk from the current `/api/agent/chat` clients.
- It fits the current frontend architecture, which already uses `fetch()` and `AbortController`.
- It supports structured final events, which is important because the Agent does more than plain chat.

### Rejected alternatives

1. Replace `/api/agent/chat` directly with streaming:
   - too much compatibility risk for existing callers and tests.

2. Stream every response type token-by-token:
   - too much frontend state-machine complexity for the current delivery goal.

3. Use WebSocket:
   - unnecessary infrastructure and no advantage for this request-response workflow.

## User-Facing Behavior

After this change:

- When a user sends a normal explanatory question, the assistant bubble appears immediately and fills in progressively.
- When the final response type is `DATA`, the user still sees the assistant in progress first, then the existing structured data card appears.
- When the final response type is `ACTION_PREVIEW`, the user still ends on the current confirmation card flow.
- Clicking the existing pause button aborts the stream and shows the current paused-message behavior.
- If streaming is unavailable or fails before the stream starts, the frontend falls back to the existing non-streaming `POST /api/agent/chat`.

## Stream Protocol

The new endpoint is:

```text
POST /api/agent/chat/stream
Content-Type: application/json
Accept: text/event-stream
```

The request body stays aligned with `AgentChatRequestDTO`:

```json
{
  "message": "什么是服务注册与发现？",
  "sessionId": "12"
}
```

The response uses Server-Sent Events with these event types:

1. `session`
   - emitted once after the session is resolved.
   - payload: `{ "sessionId": "12" }`

2. `delta`
   - emitted zero or more times for visible assistant text.
   - payload: `{ "text": "..." }`

3. `result`
   - emitted once with the full final `AgentChatResponseDTO`.
   - payload shape stays aligned with the current frontend renderer.

4. `error`
   - emitted only if a failure happens after the stream has started.
   - payload: `{ "message": "..." }`

5. `done`
   - emitted once at normal completion.

Example text flow:

```text
event: session
data: {"sessionId":"12"}

event: delta
data: {"text":"服务注册与发现用于"}

event: delta
data: {"text":"让微服务实例彼此定位。"}

event: result
data: {"sessionId":"12","responseType":"TEXT","message":"服务注册与发现用于让微服务实例彼此定位。"}

event: done
data: {}
```

Example structured flow:

```text
event: session
data: {"sessionId":"12"}

event: delta
data: {"text":"正在整理查询结果..."}

event: result
data: {"sessionId":"12","responseType":"DATA","message":"查询完成。","data":{"courses":[]}}

event: done
data: {}
```

## Backend Design

### Controller

Add a new method to `AgentController`:

- `@PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)`

The existing `/chat` endpoint stays unchanged.

### Streaming Facade

Add a dedicated streaming-oriented service in `agent-service` to keep the controller small and avoid pushing stream formatting into `AgentOrchestrator`.

Its responsibilities:

- validate user identity and role the same way as current chat.
- resolve or create the session early.
- emit the `session` event.
- decide whether the response can produce incremental text.
- emit a final `result` event with the full `AgentChatResponseDTO`.
- emit `error` and `done` consistently.

### Orchestrator Integration

`AgentOrchestrator` remains the source of final business behavior and final `AgentChatResponseDTO`.

The streaming layer should not fork Agent decision logic. Instead:

- reuse the existing orchestration path for final response generation.
- add optional streaming hooks for text-producing branches where the underlying LLM path supports it.
- keep `DATA` and `ACTION_PREVIEW` finalization on the existing orchestrator response object.

This keeps one authority for:

- session persistence
- intent routing
- RAG routing
- action preview generation
- permission checks

### Text Streaming Strategy

This slice should support two backend modes:

1. **Real incremental text mode**
   - for LLM-backed text responses where a streaming model path is available.
   - emit multiple `delta` events as text arrives.

2. **Fallback chunk mode**
   - when the current branch only provides a final string.
   - emit one short status `delta` or chunk the final text into a small number of display fragments before `result`.

This fallback is important because the current codebase already mixes:

- general LLM chat
- RAG answers
- deterministic prompts
- tool-backed structured responses

The first slice must not depend on every branch becoming truly token-streaming on day one.

### Structured Response Handling

For `DATA` and `ACTION_PREVIEW`:

- streaming is used only for progress text.
- the final UI still renders from the single `result` event payload.

That means the frontend keeps using the existing:

- `renderResponse(payload)`
- `renderPreview(preview)`

and does not need to incrementally assemble cards.

### Error Handling

Before the stream starts:

- missing user identity should still return the same failure semantics as the existing endpoint.
- request validation failure should still be a normal HTTP error response.

After the stream starts:

- runtime failures emit an `error` event with a user-safe message.
- the stream then emits `done`.

## Frontend Design

### Transport

Update `major_assignment/src/main/resources/static/agent-chat-panel.js` and `frontend/dist/agent-chat-panel.js` to:

- prefer `POST /api/agent/chat/stream`
- use `fetch()` with `AbortController`
- parse SSE frames from `response.body.getReader()`
- fall back to the existing `this.request('/api/agent/chat', ...)` path if the stream endpoint is unavailable before any useful stream data is received

### Rendering State

The chat panel gets a lightweight streaming state:

- create an empty assistant message bubble immediately after submit
- append streamed text into that bubble on each `delta`
- persist `sessionId` when the `session` event arrives
- on `result`:
  - for `TEXT`, finalize the same bubble
  - for `DATA` or `ACTION_PREVIEW`, remove the temporary streaming bubble and reuse the existing structured renderers

This keeps the visible behavior clear:

- text answers look progressively typed
- structured answers still end as full cards

### Abort Behavior

The current pause button logic already uses `AbortController`.

It should continue to:

- abort the active stream request
- stop further rendering
- append the existing paused message

No new button or control is needed.

## Data Flow

The intended flow is:

```text
Agent chat panel submit
  -> POST /api/agent/chat/stream
  -> AgentController stream endpoint
  -> streaming service resolves session
  -> emit session event
  -> orchestrator / text streaming delegate
  -> emit delta events when available
  -> emit final result event
  -> frontend finalizes message or renders existing data/preview card
```

Fallback flow:

```text
stream request fails before usable stream data
  -> frontend retries existing POST /api/agent/chat
  -> existing one-shot rendering path
```

## Testing

Implementation should add focused coverage for both backend and frontend integration surfaces.

### Backend tests

Extend `agent-service/src/test/java/com/_202510007517/platform/agent/controller/AgentControllerTest.java` to cover:

- stream endpoint returns `text/event-stream`
- session event is emitted
- result event is emitted
- missing user identity still fails safely

If a dedicated streaming service is added, give it unit tests for:

- event ordering
- text delta emission
- structured result emission
- post-start error emission

### Frontend tests

Update `scripts/verify-agent-service-frontend-contract.js` to assert that the Agent panel now includes:

- `/api/agent/chat/stream`
- stream reader logic
- fallback to `/api/agent/chat`

If a runtime browser verifier is already present for Agent chat, add one incremental-render assertion rather than replacing the existing submission flow checks.

## Verification

Manual verification after implementation:

1. Open teacher or student Agent page through the gateway.
2. Ask a knowledge-style question such as:

```text
什么是服务注册与发现？
```

Expected:

- assistant bubble appears immediately
- text grows progressively
- final text matches the final `TEXT` payload

3. Ask a structured query such as:

```text
查看我的课程列表
```

Expected:

- temporary in-progress assistant content appears
- final course list still renders with the existing card UI

4. Trigger an action-preview request such as:

```text
发布作业，标题是Spring Cloud实验，满分100
```

Expected:

- user sees in-progress assistant state first
- final confirmation card still works exactly as before

5. Click pause during a long-running streamed text response.

Expected:

- current request aborts cleanly
- UI returns to idle state
- paused message is shown

## Risks And Guardrails

Primary risks:

- duplicating orchestration logic between normal and streaming chat
- frontend stream parser becoming fragile
- regressions to existing structured response rendering

Guardrails:

- keep `/api/agent/chat` unchanged
- centralize final response generation in existing orchestrator logic
- keep structured rendering paths intact and reuse them from the final `result` event
- add a frontend fallback to the non-streaming endpoint

## Future Work

After this slice is stable:

- upgrade fallback chunk mode to true token streaming across more LLM paths
- stream action confirmation result text as well
- expose richer progress stages for long-running tool executions
- consider consolidating stream event DTOs if more Agent streaming endpoints are added
