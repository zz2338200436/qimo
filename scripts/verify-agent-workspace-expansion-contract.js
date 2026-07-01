const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

const studentPage = fs.readFileSync('frontend/dist/student-ai-assistant.html', 'utf8');
const teacherPage = fs.readFileSync('frontend/dist/teacher-ai-tools.html', 'utf8');

[
  '.student-ai-chat-shell.agent-shell-has-conversation .agent-panel.chatgpt-like {',
  'min-height: calc(100vh - 144px);',
  '.student-ai-chat-shell.agent-shell-has-conversation .agent-panel.chatgpt-like .agent-messages {',
  'min-height: clamp(360px, 50vh, 560px);',
  '.student-ai-chat-shell.agent-shell-has-conversation .agent-input-shell {',
  'padding-top: 0;'
].forEach((snippet) => {
  assertIncludes(studentPage, snippet, 'student expanded workspace contract mismatch.');
});

[
  '.teacher-ai-chat-shell.agent-shell-has-conversation .agent-panel.chatgpt-like {',
  'min-height: calc(100vh - 144px);',
  '.teacher-ai-chat-shell.agent-shell-has-conversation .agent-panel.chatgpt-like .agent-messages {',
  'min-height: clamp(360px, 50vh, 560px);',
  '.teacher-ai-chat-shell.agent-shell-has-conversation .agent-input-shell {',
  'padding-top: 0;'
].forEach((snippet) => {
  assertIncludes(teacherPage, snippet, 'teacher expanded workspace contract mismatch.');
});

console.log('agent workspace expansion contract OK');
