# Agent Entry To Workspace Transition Design

Date: 2026-06-24
Scope: `frontend/dist/student-ai-assistant.html`, `frontend/dist/teacher-ai-tools.html`, `frontend/dist/agent-chat-panel.js`

## Goal

Turn the student and teacher AI pages into a two-stage conversation surface:

- before the first sent message, the page behaves like a centered entry experience
- after the first sent message, the page expands into a full working conversation layout

The transition must feel deliberate and smooth while preserving all existing Agent behavior, request flow, history drawer logic, and page-specific color language.

## Current Context

- Both pages already use a centered empty-state hero with a bottom composer.
- The message area already changes layout implicitly through `:has(.agent-message)` selectors, but the shift is abrupt and does not coordinate the empty-state, composer, and shell rhythm.
- The shared `agent-chat-panel.js` already controls send flow for both pages, which gives us one safe place to toggle a “conversation has started” state.

## Design Direction

The experience should behave like a lightweight assistant workspace:

### Stage 1: Entry

- Empty-state headline stays visually centered.
- Example prompts and role badge remain fully visible.
- Composer sits as a hero card near the center-bottom of the viewport.
- The page communicates “start here”.

### Stage 2: Workspace

- Once the first real message is sent or restored from history, the shell shifts into a working layout.
- The headline zone compresses upward and becomes secondary.
- Example prompts fade/slide away.
- Message list claims the vertical space and aligns to the top.
- Composer settles into the bottom workspace position with reduced visual dominance.

## State Model

Both pages will use one shell state class:

- default: entry mode
- `.agent-shell-has-conversation`: workspace mode

This state is applied by shared JavaScript when:

- a message is sent
- a session with existing messages is restored
- a historical session is loaded

The state is removed only when the page returns to an actual empty conversation state.

## Interaction Rules

- Transition duration should stay short and calm, around 220ms to 320ms.
- Motion should be based on opacity and translate changes, not large-scale zoom effects.
- `prefers-reduced-motion` must collapse the animation into near-instant state changes.
- The transition must not delay message rendering or block input.

## Visual Decisions

### Shared behavior

- Center the initial shell vertically using the message area and composer together, not absolute positioning.
- Animate:
  - empty-state container opacity and translate
  - message area alignment and padding
  - composer max-width, shadow, and vertical offset
  - shell top spacing
- Keep layout stable on mobile by reducing travel distance rather than preserving the exact desktop choreography.

### Student page

- Preserve teal / green identity.
- Keep the current clean academic tone.

### Teacher page

- Preserve blue / indigo identity.
- Keep the current more operational teaching-assistant tone.

## Implementation Notes

- Prefer using a class on the page shell or panel root over additional inline style toggles.
- Keep page-specific CSS in each HTML file because that matches the current codebase pattern.
- Put transition state orchestration in `agent-chat-panel.js`, because it already owns send, session restore, and append flows.

## Verification

- Add one lightweight contract script that verifies the new shell-state hooks exist in both pages and shared JS.
- Re-run existing student/teacher contract checks.
- Re-run existing shared thinking-placeholder verification.
- Sync `frontend/dist` back to Spring static and confirm `-CheckOnly` is green.

## Out Of Scope

- No backend changes
- No Agent response format changes
- No history drawer redesign
- No message bubble redesign
- No new page navigation behavior
