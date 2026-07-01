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
  'data-agent-restore-session="false"'
].forEach((snippet) => {
  assertIncludes(studentPage, snippet, 'student entry no-autorestore contract mismatch.');
  assertIncludes(teacherPage, snippet, 'teacher entry no-autorestore contract mismatch.');
});

[
  'this.shouldRestoreSession = this.shellEl?.dataset?.agentRestoreSession !== \'false\';',
  'if (!this.shouldRestoreSession) {',
  'window.sessionStorage.removeItem(this.sessionStorageKey);',
  'return;'
].forEach((snippet) => {
  assertIncludes(chatPanel, snippet, 'shared agent panel no-autorestore contract mismatch.');
});

console.log('agent entry no-autorestore contract OK');
