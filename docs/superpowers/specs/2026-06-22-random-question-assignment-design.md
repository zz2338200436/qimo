# Random Question Assignment Design

## Goal

Support a two-step teacher workflow in the Agent chat:

1. Generate a random set of questions from the question bank.
2. In a later message, publish that generated set as a class/course assignment.

## Design

Question generation remains backed by `ai-service` and the `questions + knowledge_points` tables. Matching questions are returned in random order so repeated generation can produce different question sets when the topic, difficulty, and count stay the same.

`agent-service` stores the last successful generated question payload in the existing `agent_sessions.pending_slots_json` mechanism. If the next teacher message is a publish-assignment request and does not include explicit content, the Agent injects the generated questions into the assignment content before creating the existing publish-assignment preview.

The assignment API is unchanged. Because assignments currently store a single `description` field rather than structured question rows, the selected questions are formatted into readable text and saved as the assignment description.

## Error Handling

If the generated question set is empty, no reusable assignment content is saved. A later publish request without content still asks for the normal missing slots and will not publish an empty generated assignment.

## Testing

Add Agent tests for:

- A generated question response saves reusable pending assignment content.
- A follow-up publish-assignment message reuses that content in the preview and create request.

Keep AI service tests proving question-bank no-match behavior does not fall back to fake built-in questions.
