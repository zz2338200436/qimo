# Agent LangChain4j Integration Design

## Goal

Introduce LangChain4j into the existing `majorassignment` AI assistant stack in a low-risk way that improves model integration, streaming, memory, and RAG ergonomics without replacing the current business orchestration, approval flow, or session persistence model.

## Scope

This design covers a phased integration path for LangChain4j inside the existing project.

In scope:

- Keep the current `agent-service` business orchestration as the primary control layer.
- Add LangChain4j `AiServices` for low-risk assistant-style capabilities.
- Add assistant-scoped conversation memory for question answering and knowledge assistance.
- Upgrade chat streaming toward true model-token streaming.
- Standardize the AI capability layer so later RAG and tool upgrades are easier.
- Define which existing modules must remain custom and which can gradually adopt LangChain4j.

Out of scope:

- Full rewrite of the Agent architecture into pure LangChain4j patterns.
- Replacing the current database-backed business session model with LangChain4j `ChatMemory`.
- Letting the model directly execute high-risk write operations such as publish, delete, batch notify, or grade updates.
- Immediate migration of all current custom RAG and tool code to LangChain4j-native implementations.

## Current Context

The repository already has a substantial custom Agent architecture:

- `agent-service` owns intent recognition, slot filling, context enrichment, action preview, confirmation, execution, and audit.
- `AgentOrchestrator` is the business control center for all Agent interactions.
- `AgentSessionService` persists sessions, messages, and pending slots in the application database.
- `ToolRegistry` and `AgentTool` implementations provide deterministic business tool execution.
- `DefaultAgentChatStreamingService` exposes SSE-based chat streaming, but currently sends the result after orchestration finishes instead of token-by-token model output.
- `AgentRagConfiguration` and related classes already provide a custom RAG pipeline based on local embeddings and an in-memory index.

This means the project is already beyond the “tutorial demo” stage. The Feishu LangChain4j tutorial is useful as a capability reference, but it is not an appropriate blueprint for replacing the entire architecture.

## Design Principle

The integration principle is:

> Preserve the existing business orchestration layer and gradually replace or enhance only the AI capability layer.

In practical terms:

- Business workflow state remains custom.
- Business tool invocation remains custom.
- Business confirmation and permission control remain custom.
- LangChain4j is introduced where it improves AI interaction quality, consistency, and maintainability.

This avoids the highest-risk failure mode: losing business determinism while trying to make the whole system look like a generic AI demo.

## Preserve vs Migrate

### Must Remain Custom

The following modules should remain project-defined, not replaced by LangChain4j abstractions:

- `AgentOrchestrator`
- `AgentSessionService`
- slot requirement and slot prompting rules
- action preview generation
- first and second confirmation logic
- risk policy and approval gating
- `ToolRegistry` and current business `AgentTool` implementations

These modules encode project-specific workflow rules that LangChain4j does not model well:

- business write safety
- permission-aware workflow
- deterministic slot completion
- explicit preview-before-execute behavior
- durable auditability

### Good Candidates for LangChain4j Adoption

The following areas are good candidates for gradual adoption:

- general question answering
- knowledge-base answering
- learning suggestion style assistant interactions
- assistant-only memory for natural conversational continuity
- model-side token streaming
- later, selected read-only tools where model-chosen invocation is acceptable

These areas benefit from LangChain4j because they are model-centric rather than transaction-centric.

## Target Architecture

The target shape is:

```text
frontend chat panel
  -> gateway
  -> agent-service /api/agent/chat or /api/agent/chat/stream
  -> AgentOrchestrator
      -> business intent path
          -> custom slot filling / preview / confirmation / ToolRegistry execute
      -> assistant intent path
          -> LangChain4j-backed assistant service
              -> AiServices
              -> assistant-scoped ChatMemory
              -> optional RAG retriever
              -> optional low-risk read-only tools
```

This is a split-brain architecture on purpose:

- the custom Agent layer decides what kind of request this is
- LangChain4j improves how assistant-style interactions are fulfilled

The project should not treat LangChain4j as the top-level workflow engine.

## Phase Plan

### Phase 1: Stabilize the AI Capability Boundary

Goal:

- Define a clean seam between business orchestration and model-driven assistant behavior.

Changes:

- keep `AgentOrchestrator` as the top-level router
- keep current session/message persistence unchanged
- identify assistant-only paths such as:
  - general chat
  - RAG knowledge questions
  - learning suggestions that do not directly mutate business data
- centralize model-facing behavior behind assistant-oriented interfaces instead of spreading model usage across multiple services

Expected result:

- later LangChain4j integration affects assistant modules, not the whole project

### Phase 2: Introduce AiServices for Low-Risk Assistants

Goal:

- Use LangChain4j `AiServices` where it clearly improves readability and model orchestration.

Recommended first assistants:

- general assistant
- knowledge assistant
- learning suggestion assistant

Not recommended for the first AiServices slice:

- publish assignment
- delete course or exam
- submit or grade workflows
- batch notification workflows

Design rule:

- `AiServices` should be used only after `AgentOrchestrator` decides the request belongs to an assistant path
- `AiServices` should not replace intent recognition plus business execution

Expected result:

- assistant prompts and model interaction become more standardized
- the project gains a controlled entry point for LangChain4j features

### Phase 3: Add Assistant-Scoped Conversation Memory

Goal:

- add conversational continuity for assistant responses without replacing the project’s durable business session model

Memory design:

- retain current database-backed business sessions as the source of truth for workflow state
- add LangChain4j `ChatMemory` only for assistant dialogue continuity
- scope AI memory by assistant context, for example:
  - `sessionId + assistantType`

This creates two layers:

- business memory:
  - user identity
  - role
  - pending intent
  - pending slots
  - action history
  - auditable messages
- assistant memory:
  - natural conversation context for answer quality
  - topic continuity
  - pronoun/reference resolution

Recommended rollout:

1. start with in-memory `ChatMemory`
2. validate behavior and limits
3. later move assistant memory to Redis-backed persistence

Important non-goal:

- do not replace the database-backed `agent_sessions` and `agent_messages` tables with LangChain4j memory primitives

### Phase 4: Upgrade to True Token Streaming

Goal:

- replace current pseudo-streaming behavior with true model-token streaming

Current behavior:

- `/api/agent/chat/stream` returns SSE
- the stream opens early, but the final assistant message is emitted only after orchestration completes

Target behavior:

- assistant-style requests stream model tokens progressively
- business-tool requests can still use staged SSE events if deterministic execution is required

Recommended split:

- assistant path:
  - use LangChain4j streaming model APIs
  - emit token or chunk deltas through SSE
- business path:
  - preserve start/session/result/done semantics
  - keep confirmations and execution summaries deterministic

Expected result:

- better user-perceived responsiveness
- stronger demo and defense value

### Phase 5: Standardize RAG Behind a Retriever Boundary

Goal:

- make the custom RAG path easier to evolve without forcing an immediate rewrite

Current RAG:

- custom loader
- custom chunker
- custom embedding client
- custom in-memory vector index

Recommended direction:

- keep current RAG working
- introduce a retrieval boundary compatible with later `ContentRetriever`-style integration
- allow a future move to LangChain4j-native retrievers only after current knowledge-answering behavior is stable

This phase is evolutionary, not destructive.

### Phase 6: Trial LangChain4j Tools for Read-Only Actions

Goal:

- selectively test LangChain4j `@Tool` for low-risk, read-only scenarios

Appropriate first candidates:

- course detail lookups
- learning statistics queries
- unread notification count
- knowledge point explanation helpers

Not appropriate:

- publish assignment
- delete entities
- batch writes
- grading workflows
- any operation that must pass through preview and explicit confirmation

Design rule:

- model-selected tools are acceptable only where a mistaken invocation is low-risk and reversible at the answer level
- business writes remain under explicit orchestrator control

## AiServices Strategy

The project should adopt `AiServices`, but only as a subordinate assistant layer.

Recommended usage pattern:

- `AgentOrchestrator` decides “assistant path” versus “business path”
- assistant path delegates to an `AiServices`-backed service
- the `AiServices` service may use:
  - system prompts
  - assistant-scoped memory
  - retrievers
  - safe read-only tools

This keeps roles clear:

- orchestrator decides workflow
- `AiServices` decides answer composition

The project should not use `AiServices` as a replacement for:

- slot validation
- permission enforcement
- write-operation confirmation
- execution audit

## Conversation Memory Strategy

The project should explicitly maintain two memory systems.

### Business Session Memory

Owned by existing project code.

Responsibilities:

- message persistence
- pending business context
- approval flow state
- action history
- auditability

### Assistant Conversation Memory

Owned by LangChain4j assistant modules.

Responsibilities:

- improve continuity for question answering
- preserve conversational references
- help the assistant avoid repetitive restating

Boundaries:

- assistant memory may influence answer phrasing
- assistant memory must not become the source of truth for business workflow state

This separation is necessary because natural-language memory and business-state memory are not interchangeable.

## Risks and Mitigations

### Risk: Over-replacing working business logic

If too much is rewritten into tutorial-style LangChain4j patterns, the project may lose deterministic business behavior.

Mitigation:

- preserve orchestrator-first routing
- preserve explicit business tool execution
- use LangChain4j only behind assistant boundaries

### Risk: Memory model confusion

Developers may try to treat LangChain4j `ChatMemory` as a replacement for business session persistence.

Mitigation:

- document the two-memory model explicitly
- keep business session APIs unchanged
- scope `ChatMemory` only to assistants

### Risk: Streaming architecture mismatch

Business workflows and assistant chat have different streaming needs.

Mitigation:

- separate assistant streaming from deterministic execution event streaming
- do not force all responses through one identical streaming mechanism

### Risk: Tool-calling unsafe writes

Letting the model directly choose write tools could bypass preview and confirmation safeguards.

Mitigation:

- only test `@Tool` on read-only tools first
- keep write tools under orchestrator control

## Testing Strategy

Each phase should be validated independently.

### Phase 1 and 2

- unit tests for assistant-path routing
- integration tests ensuring business paths still use existing orchestrator logic
- regression tests for current publish/confirm flows

### Phase 3

- memory continuity tests across multiple turns
- isolation tests between sessions and assistant types
- tests proving business pending-slot behavior is unchanged

### Phase 4

- SSE contract tests for progressive token delivery
- timeout and cancellation tests
- frontend smoke tests for visibly incremental answers

### Phase 5

- RAG retrieval ranking tests
- fallback tests when retrieval is empty or unavailable
- tests proving business queries are not accidentally routed to RAG

### Phase 6

- safe tool-selection tests for read-only tools
- explicit negative tests confirming write workflows still require orchestrator-driven confirmation

## Recommended Delivery Order for This Project

For the current academic/project context, the highest-value order is:

1. keep the current smart publish and business assistant flows stable
2. add `AiServices` for assistant-only chat and knowledge answering
3. add assistant-scoped memory
4. upgrade to true token streaming
5. standardize RAG behind a retriever boundary
6. experiment with read-only `@Tool`

This order balances demonstration value, engineering safety, and implementation realism.

## Success Criteria

This migration is successful when:

- existing business write workflows still behave exactly as before
- assistant chat quality improves without destabilizing business flows
- multi-turn assistant chat becomes more natural through scoped memory
- frontend users see genuine progressive streaming for assistant responses
- RAG and assistant modules become easier to evolve independently
- the project can explain a clear architectural story in defense:
  - custom business orchestration for safety
  - LangChain4j for AI capability enhancement

## Recommendation Summary

The project should not be rewritten to match the tutorial.

Instead, it should:

- keep the existing custom Agent business architecture
- adopt LangChain4j gradually at the assistant capability layer
- use `AiServices` for low-risk assistant scenarios
- add assistant-only conversation memory
- retain explicit custom control over business workflows and high-risk operations

This is the best balance between technical quality, delivery confidence, and project defense clarity.
