const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

const studentPage = fs.readFileSync('frontend/dist/student-ai-assistant.html', 'utf8');
const teacherPage = fs.readFileSync('frontend/dist/teacher-ai-tools.html', 'utf8');

[
  '.student-ai-chat-shell .agent-panel.chatgpt-like .agent-panel-header {',
  'opacity: 0.62;',
  'padding: 0 0 14px;',
  '.student-ai-empty-state {',
  'margin: -38px auto 0;',
  '.student-ai-empty-label {',
  'align-items: center;',
  'background: rgba(255, 255, 255, 0.8);',
  'height: 70px;',
  'width: 70px;',
  'font-size: 40px;',
  '.student-ai-chat-shell .agent-input-shell {',
  'background: linear-gradient(180deg, rgba(248, 250, 252, 0), #f8fafc 24%);',
  '.student-ai-chat-shell .agent-panel.chatgpt-like .agent-form {',
  'border-color: rgba(167, 243, 208, 0.72);',
  '.student-ai-bottom-note {',
  'margin: 12px auto 0;'
].forEach((snippet) => {
  assertIncludes(studentPage, snippet, 'student home hero refinement contract mismatch.');
});

[
  '.teacher-ai-chat-shell .agent-panel.chatgpt-like .agent-panel-header {',
  'opacity: 0.62;',
  'padding: 0 0 14px;',
  '.teacher-ai-empty-state {',
  'margin: -38px auto 0;',
  '.teacher-ai-empty-label {',
  'align-items: center;',
  'background: rgba(255, 255, 255, 0.8);',
  'height: 70px;',
  'width: 70px;',
  'font-size: 40px;',
  '.teacher-ai-chat-shell .agent-input-shell {',
  'background: linear-gradient(180deg, rgba(248, 251, 255, 0), #f8fbff 24%);',
  '.teacher-ai-chat-shell .agent-panel.chatgpt-like .agent-form {',
  'border-color: rgba(191, 219, 254, 0.84);',
  '.teacher-ai-bottom-note {',
  'margin: 12px auto 0;'
].forEach((snippet) => {
  assertIncludes(teacherPage, snippet, 'teacher home hero refinement contract mismatch.');
});

console.log('agent home hero refinement contract OK');
