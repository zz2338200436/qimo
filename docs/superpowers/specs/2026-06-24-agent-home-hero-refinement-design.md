# Agent Home Hero Refinement Design

## Goal

Refine the student and teacher AI entry screens so their initial empty state feels closer to the ChatGPT home experience while still fitting inside the existing business page shells.

The selected direction is the lighter "business page with ChatGPT feel" variant:

- keep the page-level teacher/student shell intact
- keep the AI panel header and history affordance visible
- reduce the visual weight of the header in the empty state
- concentrate attention on the centered hero and composer
- preserve the existing post-send transition into the full conversation workspace

## Scope

Apply the refinement to both:

- `frontend/dist/student-ai-assistant.html`
- `frontend/dist/teacher-ai-tools.html`

Then sync the same markup and styles into:

- `major_assignment/src/main/resources/static/student-ai-assistant.html`
- `major_assignment/src/main/resources/static/teacher-ai-tools.html`

Shared conversation-state logic in `agent-chat-panel.js` stays functionally unchanged unless a visual fix requires a small class or state hook.

## Selected Visual Direction

### Empty State

The initial empty state should feel lighter and more focused than the current version:

- the panel header remains present, but with lower contrast and more breathing room
- the top-right service status and history button remain available, but appear quieter
- the hero block moves slightly upward and becomes the clear focal point
- the mode badge, main title, helper copy, and example prompts stay centered
- the composer feels like a primary entry surface rather than a footer form

### After First Message

The existing workspace transition remains:

- once real messages exist, the shell enters the compact conversation layout
- the empty-state hero collapses away
- the header becomes tighter and more work-focused
- the composer settles into the conversation workspace form

## Layout and Styling Changes

For both student and teacher pages:

1. Empty-state header treatment
   - lower the header opacity/contrast in the no-conversation state
   - slightly reduce the perceived separation between header and hero
   - keep controls usable and readable

2. Hero composition
   - pull the hero upward so it feels more like a homepage prompt, not a lower card section
   - reduce visual clutter around the hero
   - preserve role-specific icon/accent styling

3. Composer treatment
   - make the input shell feel more central and premium in the empty state
   - keep the existing send affordance and textarea behavior
   - maintain stable dimensions so the transition does not jump awkwardly

4. Example prompt chips
   - keep them centered beneath the hero copy
   - ensure they visually support the hero instead of competing with it

5. Conversation-state boundary
   - do not change the overall empty-state/full-workspace behavior contract
   - the refinement is about visual hierarchy, spacing, and perceived weight

## Constraints

- no regression to history drawer behavior
- no regression to Enter-to-send behavior
- no regression to streaming/thinking placeholder behavior
- no role-specific behavior drift between student and teacher beyond existing theme differences
- keep the implementation CSS-first and lightweight

## Verification

Verify with existing targeted scripts plus a visual smoke check:

- student contract verifier
- teacher contract verifier
- thinking placeholder verifier
- Enter submit verifier
- frontend/static sync check

Also manually confirm:

- empty state is visibly lighter and more centered than before
- first send still transitions into the compact workspace
- both student and teacher pages reflect the same structural refinement
