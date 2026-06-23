const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');

function read(relativePath) {
  const fullPath = path.join(root, relativePath);
  if (!fs.existsSync(fullPath)) {
    throw new Error(`Missing file: ${relativePath}`);
  }
  return fs.readFileSync(fullPath, 'utf8');
}

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

function assertNotIncludes(content, needle, message) {
  if (content.includes(needle)) {
    throw new Error(`${message} Found unexpected snippet: ${needle}`);
  }
}

const requiredAssetPaths = [
  'major_assignment/src/main/resources/static/agent-chat-panel.js',
  'major_assignment/src/main/resources/static/agent-chat-panel.css',
  'major_assignment/src/main/resources/static/agent-history-panel.js',
  'frontend/dist/agent-chat-panel.js',
  'frontend/dist/agent-chat-panel.css',
  'frontend/dist/agent-history-panel.js',
];

for (const relativePath of requiredAssetPaths) {
  if (!fs.existsSync(path.join(root, relativePath))) {
    throw new Error(`Agent frontend asset missing: ${relativePath}`);
  }
}

const syncScript = read('scripts/sync-frontend-to-static.ps1');
[
  "'agent-chat-panel.js'",
  "'agent-chat-panel.css'",
  "'agent-history-panel.js'",
].forEach(snippet => {
  assertIncludes(syncScript, snippet, 'frontend sync script should preserve Agent panel assets.');
});

const pageChecks = [
  {
    file: 'major_assignment/src/main/resources/static/student-ai-assistant.html',
    snippets: [
      'agent-chat-panel.css?v=20260620-agent-stream-1',
      'data-agent-history-panel',
      'data-agent-panel data-agent-role="STUDENT"',
      'data-agent-command="查看我的作业列表"',
      'agent-chat-panel.js?v=20260620-agent-stream-1',
      'agent-history-panel.js?v=20260617-agent-history-2',
    ],
  },
  {
    file: 'major_assignment/src/main/resources/static/teacher-ai-tools.html',
    snippets: [
      'agent-chat-panel.css?v=20260620-agent-stream-1',
      'data-agent-history-panel',
      'data-agent-panel data-agent-role="TEACHER"',
      'data-agent-history-toggle',
      'agent-chat-panel.js?v=20260620-agent-stream-1',
      'agent-history-panel.js?v=20260617-agent-history-2',
    ],
  },
  {
    file: 'frontend/dist/student-ai-assistant.html',
    snippets: [
      'agent-chat-panel.css?v=20260620-agent-stream-1',
      'data-agent-history-panel',
      'data-agent-panel data-agent-role="STUDENT"',
      'data-agent-command="查看我的作业列表"',
      'agent-chat-panel.js?v=20260620-agent-stream-1',
      'agent-history-panel.js?v=20260617-agent-history-2',
    ],
  },
  {
    file: 'frontend/dist/teacher-ai-tools.html',
    snippets: [
      'agent-chat-panel.css?v=20260620-agent-stream-1',
      'data-agent-history-panel',
      'data-agent-panel data-agent-role="TEACHER"',
      'data-agent-history-toggle',
      'agent-chat-panel.js?v=20260620-agent-stream-1',
      'agent-history-panel.js?v=20260617-agent-history-2',
    ],
  },
];

for (const check of pageChecks) {
  const content = read(check.file);
  for (const snippet of check.snippets) {
    assertIncludes(content, snippet, `${check.file} should mount the Agent chat panel.`);
  }
}

const panelScript = read('major_assignment/src/main/resources/static/agent-chat-panel.js');
const frontendAgentChatPanel = read('frontend/dist/agent-chat-panel.js');
const staticAgentChatPanel = panelScript;
[
  "this.request('/api/agent/chat'",
  "/api/agent/chat/stream",
  'response.body.getReader()',
  "'Accept': 'text/event-stream'",
  "'X-User-Id'",
  "'X-Active-Role'",
  "'X-Roles'",
  '`/api/agent/actions/${preview.actionId}/confirm`',
  '`/api/agent/actions/${preview.actionId}/cancel`',
  'cancel(preview',
  'return course?.id ?? course?.courseId ?? course?.course_id ?? null;',
  "return hasValue(courseId) ? `查看课程详情 课程ID ${courseId}` : '查看课程详情';",
  'function renderCourseDetailCard(course) {',
  'return renderCourseDetailCard(value.course);',
  'switchSession(sessionId, options = {}) {',
  'persistCurrentSessionId(sessionId) {',
  "window.sessionStorage.setItem(this.sessionStorageKey, sessionId);",
  "window.addEventListener('agent-session-selected'",
  'AgentStreamFallback',
  'global.initAgentChatPanels = initAll',
].forEach(snippet => {
  assertIncludes(panelScript, snippet, 'Agent panel script should call the Agent service and expose initializer.');
});

[
  'buildPageContext()',
  'context: this.buildPageContext()',
].forEach(snippet => {
  assertIncludes(frontendAgentChatPanel, snippet, 'Frontend agent chat panel should send page context.');
  assertIncludes(staticAgentChatPanel, snippet, 'Static agent chat panel should send page context.');
});

assertNotIncludes(
  panelScript,
  'return renderCourseCard(value.course, { showDetailAction: false });',
  'Agent panel course detail should render a dedicated detail view instead of reusing the list card.'
);

assertNotIncludes(
  panelScript,
  "const meta = [\r\n            renderCourseMetaItem('课程ID', courseId),\r\n            renderCourseMetaItem('课程代码', courseCode),",
  'Agent panel course list card should not repeat the course code in both the header and metadata.'
);

const historyScript = read('major_assignment/src/main/resources/static/agent-history-panel.js');
[
  "this.request('/api/agent/sessions')",
  "`/api/agent/sessions/${sessionId}`",
  'data-agent-history-list',
  'data-agent-history-detail',
  'data-agent-history-continue',
  'agent-history-item-current',
  "window.sessionStorage.getItem(getCurrentSessionStorageKey())",
  "window.addEventListener('agent-session-changed'",
  "window.dispatchEvent(new CustomEvent('agent-session-selected'",
  'global.initAgentHistoryPanels = initAll',
].forEach(snippet => {
  assertIncludes(historyScript, snippet, 'Agent history panel should load session and action history.');
});

console.log('Agent service frontend contract verification passed.');
