# Agent Scroll To Bottom Button Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a floating "scroll to bottom" button to the Agent chat panel that appears when the user leaves the bottom of the conversation and protects manual history reading from forced auto-scroll.

**Architecture:** Keep the feature entirely in the existing frontend chat panel by extending the `AgentChatPanel` scroll-state logic and adding one floating control inside the message region. Preserve the current rendering pipeline while teaching the panel to distinguish "user is near bottom" from "user is reading history", then style the new button in the existing chat CSS.

**Tech Stack:** Vanilla JavaScript, static HTML/CSS, Node.js verification scripts, Playwright-based browser smoke checks

---

### Task 1: Add a failing contract check for the new button hooks

**Files:**
- Create: `D:/111/Distributed framework technology/JavaCode/majorassignment/scripts/verify-agent-chat-scroll-to-bottom-contract.js`
- Modify: `D:/111/Distributed framework technology/JavaCode/majorassignment/major_assignment/src/main/resources/static/agent-chat-panel.js`
- Modify: `D:/111/Distributed framework technology/JavaCode/majorassignment/major_assignment/src/main/resources/static/agent-chat-panel.css`

- [ ] **Step 1: Write the failing test**

```javascript
const fs = require('node:fs');
const path = require('node:path');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

const jsPath = path.resolve(__dirname, '../major_assignment/src/main/resources/static/agent-chat-panel.js');
const cssPath = path.resolve(__dirname, '../major_assignment/src/main/resources/static/agent-chat-panel.css');
const js = fs.readFileSync(jsPath, 'utf8');
const css = fs.readFileSync(cssPath, 'utf8');

assertIncludes(js, 'data-agent-scroll-bottom', 'chat panel should render a scroll-to-bottom control');
assertIncludes(js, 'updateScrollToBottomVisibility', 'chat panel should manage scroll-to-bottom visibility');
assertIncludes(js, 'isNearMessagesBottom', 'chat panel should compute near-bottom state');
assertIncludes(css, '.agent-scroll-bottom-button', 'chat panel css should style the scroll-to-bottom button');

console.log('Agent chat scroll-to-bottom contract check passed.');
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node scripts/verify-agent-chat-scroll-to-bottom-contract.js`
Expected: FAIL with a missing `data-agent-scroll-bottom` or missing helper function string because the feature is not implemented yet.

- [ ] **Step 3: Write minimal implementation**

```javascript
// Placeholder target for the next tasks:
// render one button with data-agent-scroll-bottom
// add updateScrollToBottomVisibility()
// add isNearMessagesBottom()
```

- [ ] **Step 4: Run test to verify it passes**

Run: `node scripts/verify-agent-chat-scroll-to-bottom-contract.js`
Expected: PASS with `Agent chat scroll-to-bottom contract check passed.`

- [ ] **Step 5: Commit**

```bash
git add scripts/verify-agent-chat-scroll-to-bottom-contract.js major_assignment/src/main/resources/static/agent-chat-panel.js major_assignment/src/main/resources/static/agent-chat-panel.css
git commit -m "test: add agent scroll-to-bottom contract check"
```

### Task 2: Teach the chat panel to track bottom-following state

**Files:**
- Modify: `D:/111/Distributed framework technology/JavaCode/majorassignment/major_assignment/src/main/resources/static/agent-chat-panel.js`
- Test: `D:/111/Distributed framework technology/JavaCode/majorassignment/scripts/verify-agent-chat-scroll-to-bottom-contract.js`

- [ ] **Step 1: Write the failing test**

Add these assertions to `scripts/verify-agent-chat-scroll-to-bottom-contract.js`:

```javascript
assertIncludes(js, 'this.shouldAutoScrollMessages = true;', 'chat panel should keep a bottom-following flag');
assertIncludes(js, "this.messagesEl?.addEventListener('scroll'", 'chat panel should react to message scrolling');
assertIncludes(js, 'this.shouldAutoScrollMessages = this.isNearMessagesBottom();', 'chat panel should stop forcing scroll when user leaves bottom');
assertIncludes(js, 'this.updateScrollToBottomVisibility();', 'chat panel should refresh button visibility after scroll state changes');
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node scripts/verify-agent-chat-scroll-to-bottom-contract.js`
Expected: FAIL on one of the new bottom-following state assertions.

- [ ] **Step 3: Write minimal implementation**

```javascript
this.shouldAutoScrollMessages = true;

isNearMessagesBottom(threshold = 48) {
  if (!this.messagesEl) {
    return true;
  }
  const distanceFromBottom = this.messagesEl.scrollHeight - this.messagesEl.scrollTop - this.messagesEl.clientHeight;
  return distanceFromBottom <= threshold;
}

bindMessageScrollTracking() {
  this.messagesEl?.addEventListener('scroll', () => {
    this.shouldAutoScrollMessages = this.isNearMessagesBottom();
    this.updateScrollToBottomVisibility();
  });
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `node scripts/verify-agent-chat-scroll-to-bottom-contract.js`
Expected: PASS and the contract script prints the success line.

- [ ] **Step 5: Commit**

```bash
git add scripts/verify-agent-chat-scroll-to-bottom-contract.js major_assignment/src/main/resources/static/agent-chat-panel.js
git commit -m "feat: track agent chat bottom-follow state"
```

### Task 3: Add the floating button and wire its visibility

**Files:**
- Modify: `D:/111/Distributed framework technology/JavaCode/majorassignment/major_assignment/src/main/resources/static/agent-chat-panel.js`
- Modify: `D:/111/Distributed framework technology/JavaCode/majorassignment/major_assignment/src/main/resources/static/agent-chat-panel.css`
- Test: `D:/111/Distributed framework technology/JavaCode/majorassignment/scripts/verify-agent-chat-scroll-to-bottom-contract.js`

- [ ] **Step 1: Write the failing test**

Add these assertions to `scripts/verify-agent-chat-scroll-to-bottom-contract.js`:

```javascript
assertIncludes(js, 'createScrollToBottomButton()', 'chat panel should build the floating button');
assertIncludes(js, 'scrollMessagesToBottom({ smooth: true, force: true })', 'button should smooth-scroll back to the bottom');
assertIncludes(js, 'agent-scroll-bottom-visible', 'chat panel should toggle a visible button state');
assertIncludes(css, '.agent-messages-wrap', 'chat panel should have a positioning wrapper for the button');
assertIncludes(css, '.agent-scroll-bottom-button.agent-scroll-bottom-visible', 'chat panel css should expose the visible state');
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node scripts/verify-agent-chat-scroll-to-bottom-contract.js`
Expected: FAIL because the wrapper/button rendering and visible-state styling are not present yet.

- [ ] **Step 3: Write minimal implementation**

```javascript
createScrollToBottomButton() {
  const button = document.createElement('button');
  button.type = 'button';
  button.className = 'agent-scroll-bottom-button';
  button.setAttribute('data-agent-scroll-bottom', 'true');
  button.innerHTML = '<i class="fa fa-arrow-down" aria-hidden="true"></i><span>回到底部</span>';
  button.addEventListener('click', () => {
    this.scrollMessagesToBottom({ smooth: true, force: true });
  });
  return button;
}
```

```css
.agent-messages-wrap {
  position: relative;
  min-height: 0;
}

.agent-scroll-bottom-button {
  position: absolute;
  right: 20px;
  bottom: 18px;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `node scripts/verify-agent-chat-scroll-to-bottom-contract.js`
Expected: PASS and the success line prints again.

- [ ] **Step 5: Commit**

```bash
git add scripts/verify-agent-chat-scroll-to-bottom-contract.js major_assignment/src/main/resources/static/agent-chat-panel.js major_assignment/src/main/resources/static/agent-chat-panel.css
git commit -m "feat: add agent chat scroll-to-bottom control"
```

### Task 4: Preserve history reading by guarding auto-scroll

**Files:**
- Modify: `D:/111/Distributed framework technology/JavaCode/majorassignment/major_assignment/src/main/resources/static/agent-chat-panel.js`
- Test: `D:/111/Distributed framework technology/JavaCode/majorassignment/scripts/verify-agent-chat-scroll-to-bottom-contract.js`

- [ ] **Step 1: Write the failing test**

Add these assertions to `scripts/verify-agent-chat-scroll-to-bottom-contract.js`:

```javascript
assertIncludes(js, 'scrollMessagesToBottom(options = {})', 'chat panel should accept scroll options');
assertIncludes(js, 'const shouldScroll = force || this.shouldAutoScrollMessages;', 'chat panel should only auto-scroll when appropriate');
assertIncludes(js, 'if (!shouldScroll) {', 'chat panel should skip forced scrolling while user is reading history');
assertIncludes(js, 'this.shouldAutoScrollMessages = true;', 'chat panel should resume bottom-follow mode after explicit return-to-bottom');
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node scripts/verify-agent-chat-scroll-to-bottom-contract.js`
Expected: FAIL because `scrollMessagesToBottom` still always hard-scrolls unconditionally.

- [ ] **Step 3: Write minimal implementation**

```javascript
scrollMessagesToBottom(options = {}) {
  const { smooth = false, force = false } = options;
  if (!this.messagesEl) {
    return;
  }
  const shouldScroll = force || this.shouldAutoScrollMessages;
  this.updateScrollToBottomVisibility();
  if (!shouldScroll) {
    return;
  }
  const behavior = smooth ? 'smooth' : 'auto';
  this.messagesEl.scrollTo({ top: this.messagesEl.scrollHeight, behavior });
  this.shouldAutoScrollMessages = true;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `node scripts/verify-agent-chat-scroll-to-bottom-contract.js`
Expected: PASS and no contract regressions.

- [ ] **Step 5: Commit**

```bash
git add scripts/verify-agent-chat-scroll-to-bottom-contract.js major_assignment/src/main/resources/static/agent-chat-panel.js
git commit -m "feat: protect agent chat history reading from auto-scroll"
```

### Task 5: Add a browser check for visible behavior

**Files:**
- Create: `D:/111/Distributed framework technology/JavaCode/majorassignment/scripts/verify-agent-chat-scroll-to-bottom-browser.js`
- Modify: `D:/111/Distributed framework technology/JavaCode/majorassignment/major_assignment/src/main/resources/static/agent-chat-panel.css`
- Modify: `D:/111/Distributed framework technology/JavaCode/majorassignment/major_assignment/src/main/resources/static/agent-chat-panel.js`

- [ ] **Step 1: Write the failing test**

```javascript
const path = require('node:path');
const fs = require('node:fs');
const { chromium } = require(path.resolve(__dirname, '../major_assignment/node_modules/playwright'));

function assert(condition, message, details) {
  if (!condition) {
    const error = new Error(message);
    if (details !== undefined) {
      error.details = details;
    }
    throw error;
  }
}

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1366, height: 900 } });
  const cssPath = path.resolve(__dirname, '../major_assignment/src/main/resources/static/agent-chat-panel.css');
  const jsPath = path.resolve(__dirname, '../major_assignment/src/main/resources/static/agent-chat-panel.js');

  try {
    await page.setContent('<div id="app"></div>');
    await page.addStyleTag({ path: cssPath });
    await page.addScriptTag({ path: jsPath });
    await page.evaluate(() => {
      document.getElementById('app').innerHTML = `
        <section class="agent-panel chatgpt-like" data-agent-panel>
          <div class="agent-panel-body">
            <div class="agent-messages" data-agent-messages></div>
          </div>
        </section>
      `;
    });
    throw new Error('replace with scroll behavior assertions after implementation');
  } finally {
    await page.close();
    await browser.close();
  }
})().catch(error => {
  console.error(error.message);
  if (error.details !== undefined) {
    console.error(JSON.stringify(error.details, null, 2));
  }
  process.exit(1);
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node scripts/verify-agent-chat-scroll-to-bottom-browser.js`
Expected: FAIL with the placeholder error because the browser assertions are not implemented yet.

- [ ] **Step 3: Write minimal implementation**

Replace the placeholder with runtime assertions that:

```javascript
assert(buttonHiddenAtBottom, 'button should be hidden when already at bottom', metrics);
assert(buttonVisibleAfterScrollUp, 'button should appear after leaving the bottom region', metrics);
assert(returnedToBottomAfterClick, 'button click should bring the scroller back to bottom', metrics);
assert(buttonHiddenAfterReturn, 'button should hide again after returning to bottom', metrics);
```

- [ ] **Step 4: Run test to verify it passes**

Run: `node scripts/verify-agent-chat-scroll-to-bottom-browser.js`
Expected: PASS with `Agent chat scroll-to-bottom browser check passed.`

- [ ] **Step 5: Commit**

```bash
git add scripts/verify-agent-chat-scroll-to-bottom-browser.js major_assignment/src/main/resources/static/agent-chat-panel.js major_assignment/src/main/resources/static/agent-chat-panel.css
git commit -m "test: verify agent chat scroll-to-bottom behavior"
```

### Task 6: Final verification

**Files:**
- Test: `D:/111/Distributed framework technology/JavaCode/majorassignment/scripts/verify-agent-chat-scroll-to-bottom-contract.js`
- Test: `D:/111/Distributed framework technology/JavaCode/majorassignment/scripts/verify-agent-chat-scroll-to-bottom-browser.js`
- Test: `D:/111/Distributed framework technology/JavaCode/majorassignment/scripts/verify-agent-chat-message-alignment.js`

- [ ] **Step 1: Run the contract check**

Run: `node scripts/verify-agent-chat-scroll-to-bottom-contract.js`
Expected: PASS with the contract success line.

- [ ] **Step 2: Run the browser behavior check**

Run: `node scripts/verify-agent-chat-scroll-to-bottom-browser.js`
Expected: PASS with `Agent chat scroll-to-bottom browser check passed.`

- [ ] **Step 3: Run the existing alignment regression check**

Run: `node scripts/verify-agent-chat-message-alignment.js`
Expected: PASS with `Agent chat message alignment check passed.`

- [ ] **Step 4: Inspect the working tree**

Run: `git diff -- scripts/verify-agent-chat-scroll-to-bottom-contract.js scripts/verify-agent-chat-scroll-to-bottom-browser.js major_assignment/src/main/resources/static/agent-chat-panel.js major_assignment/src/main/resources/static/agent-chat-panel.css`
Expected: only the planned scroll-to-bottom button and verification changes are present.

- [ ] **Step 5: Commit**

```bash
git add scripts/verify-agent-chat-scroll-to-bottom-contract.js scripts/verify-agent-chat-scroll-to-bottom-browser.js major_assignment/src/main/resources/static/agent-chat-panel.js major_assignment/src/main/resources/static/agent-chat-panel.css
git commit -m "feat: add agent chat scroll-to-bottom button"
```
