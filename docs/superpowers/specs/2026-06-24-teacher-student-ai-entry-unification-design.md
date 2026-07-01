# Teacher Student AI Entry Unification Design

Date: 2026-06-24
Scope: `frontend/dist/student-ai-assistant.html`, `frontend/dist/teacher-ai-tools.html`

## Goal

Continue the recent teacher-side AI entry redesign by tightening the student-side and teacher-side entry pages into one coherent interaction family, while keeping role-specific color language and all existing Agent behavior intact.

## Current Context

- The teacher page has already been moved to a minimal centered AI-entry layout in the previous session.
- The student page in Spring static has already adopted the same direction, but `frontend/dist` is still behind and remains the stale source for sync and contract verification.
- Existing Agent chat, history drawer, quick-fill behavior, and backend/API contracts must remain unchanged.

## Design Direction

Both pages should feel like the same product surface:

- centered empty-state hero
- lightweight status and history controls in the header
- three example prompts as low-friction starting points
- one large bottom composer with a single primary action
- one concise bottom note that explains the current focus of the page

Role separation stays visible through color:

- student: teal/green
- teacher: blue/indigo

## Specific Unification Decisions

### Header rhythm

- Keep the slim panel header with title, service status, and history button on both pages.
- Use short titles with stronger role language:
  - student: `AI学习助手`
  - teacher: `AI教学助手`

### Empty-state content

- Add a small role label above the main empty-state headline on both pages.
- Keep the empty-state centered and single-purpose.
- Use similar density and spacing for headline, description, and prompt chips.

### Composer area

- Keep the large rounded bottom composer on both pages.
- Align textarea height, shell padding, button size, and shadow weight.
- Use role-specific placeholders instead of generic “输入你的需求”.

### Footer note

- Keep one muted line under the composer on both pages.
- Student note emphasizes course context and focus.
- Teacher note emphasizes course/task context and history continuity.

## Out Of Scope

- No Agent API changes
- No history drawer behavior changes
- No new teacher tools or student capabilities
- No backend or database changes
- No global shell redesign outside these two pages
