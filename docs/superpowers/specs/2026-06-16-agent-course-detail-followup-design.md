# Agent Course Detail Follow-Up Design

Date: 2026-06-16
Scope: teacher Agent follow-up behavior for `查看课程详情`

## Goal

Make teacher-side `查看课程详情` work naturally after a course query or a course detail button click, without forcing the user to manually restate `courseId` in common follow-up flows.

## Problem Summary

The current Agent flow recognizes `QUERY_COURSE_DETAIL`, but the execution path still requires `courseId`.

That creates two broken experiences:

- if the user types only `查看课程详情`, the Agent responds with a missing-slot prompt
- if the frontend presents a course-detail follow-up action without embedding `courseId`, the follow-up still fails the same way

This is inconsistent with the existing assignment and course disambiguation behavior, where the Agent already resolves business identifiers from natural language context.

## Approaches Considered

### 1. Frontend-only fix

Have the UI always send `查看课程ID xxx详情`.

Pros:

- simple
- deterministic

Cons:

- plain follow-up text in the same session still fails
- backend remains brittle for other clients

### 2. Backend-only fix

Teach the backend to reuse recent course context when handling `QUERY_COURSE_DETAIL`.

Pros:

- fixes the behavior for all clients
- keeps the Agent conversational

Cons:

- needs careful fallback when multiple courses are present
- still leaves the frontend action less explicit than it could be

### 3. Hybrid fix

Add backend follow-up resolution and make frontend detail actions send explicit course IDs when available.

Pros:

- fixes the root cause in the backend
- makes the UI path deterministic
- keeps plain chat follow-ups usable

Cons:

- touches both backend and frontend

## Recommendation

Choose approach 3.

## Design

### Backend behavior

For teacher-side `QUERY_COURSE_DETAIL`:

- keep accepting direct `courseId`
- keep resolving from `courseName + semester + className` when those are present
- add one-session follow-up resolution:
  - if the current request is `查看课程详情`
  - and the immediately preceding Agent `DATA` response contains exactly one teacher course candidate
  - then reuse that course's `courseId`
- if there are multiple candidate courses and no explicit identifier, continue prompting for `课程ID`

### Frontend behavior

When the Agent course list/result UI renders a detail affordance, it should send an explicit command that includes the resolved course ID, for example:

`查看课程ID 91005详情`

This should be treated as a convenience improvement, not the only path.

### Testing

Add regression coverage for:

- follow-up `查看课程详情` after `查看我的课程` with exactly one course
- the old missing-slot behavior when there are multiple courses and no explicit identifier
- frontend rendering or command generation for course detail buttons if a dedicated renderer exists

## In Scope

- teacher Agent follow-up resolution for course detail
- explicit course-detail command generation in the Agent UI if present
- regression tests for the new behavior

## Out of Scope

- redesigning the whole Agent result renderer
- changing student course detail page behavior
- general memory for every Agent intent

## Success Criteria

This change is successful when:

- a teacher can query courses and then say `查看课程详情` in the same session when only one course is in context
- ambiguous follow-ups still ask for clarification instead of guessing
- UI-driven course detail actions include the target `courseId` when available
