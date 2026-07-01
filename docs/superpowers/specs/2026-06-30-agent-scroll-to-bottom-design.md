# Agent Scroll To Bottom Button Design

Date: 2026-06-30
Scope: `major_assignment/src/main/resources/static/agent-chat-panel.js`, `major_assignment/src/main/resources/static/agent-chat-panel.css`

## Goal

Add a lightweight "scroll to bottom" affordance to the AI assistant chat panel so users can quickly return to the newest messages after scrolling upward through long conversation history.

## Current Context

- The existing Agent chat panel always scrolls to the newest message when rendering new content.
- Long conversations make it easy for users to lose their place after scrolling upward.
- The current panel does not expose any visible shortcut for returning to the latest message region.

## Chosen Approach

Add one floating button inside the chat message area and drive it entirely from the message scroller state.

The button behavior is:

- hidden while the user is already at or very near the bottom
- shown after the user scrolls away from the bottom beyond a small threshold
- hidden again immediately after the panel returns to the bottom
- smooth-scrolls the message list to the bottom when clicked

## Interaction Rules

### Visibility

- Compute "near bottom" from the existing message scroller using `scrollHeight - scrollTop - clientHeight`.
- Treat a small remaining distance as still being at the bottom to avoid flicker from fractional layout changes.
- Update button visibility on:
  - user scroll
  - panel initialization
  - message render completion
  - window/layout changes that can affect scroll height

### Auto-scroll protection

- Preserve auto-scroll when the user was already near the bottom before a new message arrives.
- Do not force-scroll when the user has intentionally moved upward to read older messages.
- After the user clicks the button, resume the normal bottom-following state.

### Placement and style

- Anchor the button to the lower-right corner of the chat message region.
- Keep it visually secondary to the main send action.
- Use a compact rounded style with a subtle shadow so it stays visible above long content without blocking message reading.
- On small screens, keep the button inside the scroll area bounds and large enough for touch interaction.

## Out Of Scope

- No backend or API changes
- No changes to conversation history drawer behavior
- No unread-count badge
- No redesign of the message composer or header

## Testing

Manual verification after implementation:

1. Open the AI assistant page and send enough messages to make the conversation scrollable.
2. Scroll upward several screens.
3. Confirm the "scroll to bottom" button appears only after leaving the bottom region.
4. Click the button and confirm the list smoothly returns to the latest message and the button hides.
5. Scroll upward again, then trigger a new assistant reply.
6. Confirm the panel does not yank the viewport to the bottom while reading history.
7. Return to the bottom and confirm new replies continue auto-following as before.
