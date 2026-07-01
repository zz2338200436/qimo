const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

const chatPanel = fs.readFileSync('frontend/dist/agent-chat-panel.js', 'utf8');
const historyPanel = fs.readFileSync('frontend/dist/agent-history-panel.js', 'utf8');

[
  'hydrateSession(session) {',
  'const messages = Array.isArray(session?.messages) ? session.messages : [];',
  'this.messagesEl.innerHTML = \'\';',
  'messages.forEach((message) => {',
  "const role = message.role === 'USER' ? 'user' : 'agent';",
  'this.append(role,',
  'this.syncConversationState();'
].forEach((snippet) => {
  assertIncludes(chatPanel, snippet, 'agent per-session first-message contract mismatch.');
});

[
  "window.dispatchEvent(new CustomEvent('agent-session-selected', {",
  'detail: {',
  'sessionId,',
  'label: continueLabel,',
  'session'
].forEach((snippet) => {
  assertIncludes(historyPanel, snippet, 'agent history should forward full session detail for per-session hydration.');
});

console.log('agent per-session first-message contract OK');
