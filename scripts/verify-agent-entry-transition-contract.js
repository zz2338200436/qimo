const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

const studentPage = fs.readFileSync('frontend/dist/student-ai-assistant.html', 'utf8');
const teacherPage = fs.readFileSync('frontend/dist/teacher-ai-tools.html', 'utf8');
const chatPanel = fs.readFileSync('frontend/dist/agent-chat-panel.js', 'utf8');

[
  'data-agent-shell',
  '.student-ai-chat-shell.agent-shell-has-conversation',
  '.student-ai-chat-shell.agent-shell-has-conversation .student-ai-empty-state',
  '.student-ai-chat-shell.agent-shell-has-conversation .agent-messages'
].forEach((snippet) => {
  assertIncludes(studentPage, snippet, 'student entry transition contract mismatch.');
});

[
  'data-agent-shell',
  '.teacher-ai-chat-shell.agent-shell-has-conversation',
  '.teacher-ai-chat-shell.agent-shell-has-conversation .teacher-ai-empty-state',
  '.teacher-ai-chat-shell.agent-shell-has-conversation .agent-messages'
].forEach((snippet) => {
  assertIncludes(teacherPage, snippet, 'teacher entry transition contract mismatch.');
});

[
  "this.shellEl = root.closest('[data-agent-shell]') || root;",
  'setConversationState(hasConversation) {',
  "this.shellEl?.classList.toggle('agent-shell-has-conversation', hasConversation);",
  'syncConversationState() {',
  "const realMessages = this.messagesEl.querySelectorAll('.agent-message').length;",
  'this.syncConversationState();'
].forEach((snippet) => {
  assertIncludes(chatPanel, snippet, 'shared agent shell transition contract mismatch.');
});

console.log('agent entry transition contract OK');
