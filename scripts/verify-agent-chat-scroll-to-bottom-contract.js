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

assertIncludes(js, 'this.shouldAutoScrollMessages = true;', 'chat panel should track whether bottom auto-follow is enabled');
assertIncludes(js, "data-agent-scroll-bottom', 'true", 'chat panel should render a scroll-to-bottom button');
assertIncludes(js, 'createScrollToBottomButton()', 'chat panel should expose a scroll-to-bottom button factory');
assertIncludes(js, 'isNearMessagesBottom(threshold = this.messagesScrollThreshold)', 'chat panel should compute near-bottom state');
assertIncludes(js, 'const shouldScroll = force || this.shouldAutoScrollMessages;', 'chat panel should protect manual history reading from forced auto-scroll');
assertIncludes(js, "this.messagesEl?.addEventListener('scroll'", 'chat panel should update bottom-follow state on scroll');
assertIncludes(js, "this.scrollMessagesToBottom({ smooth: true, force: true });", 'scroll-to-bottom button should force a smooth return');
assertIncludes(css, '.agent-messages-wrap', 'chat panel css should provide a positioning wrapper for the button');
assertIncludes(css, '.agent-scroll-bottom-button', 'chat panel css should style the scroll-to-bottom button');
assertIncludes(css, '.agent-scroll-bottom-button.agent-scroll-bottom-visible', 'chat panel css should expose the visible button state');

console.log('Agent chat scroll-to-bottom contract check passed.');
