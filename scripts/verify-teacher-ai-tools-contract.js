const fs = require('node:fs');

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

function assertMirrorFileMatches(frontendPath, staticPath, label) {
  const frontendContent = fs.readFileSync(frontendPath, 'utf8');
  const staticContent = fs.readFileSync(staticPath, 'utf8');

  if (frontendContent !== staticContent) {
    throw new Error(
      `${label} mirror mismatch. Keep frontend/dist and major_assignment/src/main/resources/static in sync.\n` +
      `frontend: ${frontendPath}\nstatic: ${staticPath}`
    );
  }
}

const pageContent = fs.readFileSync('frontend/dist/teacher-ai-tools.html', 'utf8');
const apiContent = fs.readFileSync('frontend/dist/api.js', 'utf8');
const agentChatContent = fs.readFileSync('frontend/dist/agent-chat-panel.js', 'utf8');
const agentChatStyleContent = fs.readFileSync('frontend/dist/agent-chat-panel.css', 'utf8');
const agentHistoryContent = fs.readFileSync('frontend/dist/agent-history-panel.js', 'utf8');

[
  [
    'frontend/dist/teacher-ai-tools.html',
    'major_assignment/src/main/resources/static/teacher-ai-tools.html',
    'teacher-ai-tools.html'
  ],
  [
    'frontend/dist/agent-history-panel.js',
    'major_assignment/src/main/resources/static/agent-history-panel.js',
    'agent-history-panel.js'
  ]
].forEach(([frontendPath, staticPath, label]) => {
  assertMirrorFileMatches(frontendPath, staticPath, label);
});

[
  '<h2 class="page-title">辅助工具</h2>',
  'placeholder="输入课程、作业、题目、试卷或教学目标..."',
  'teacher-ai-chat-shell',
  'data-agent-history-toggle',
  'data-agent-history-close',
  'class="agent-history-hover-corner"',
  'class="agent-history-backdrop"',
  'class="agent-history-panel agent-history-drawer"',
  'agent-input-shell',
  'data-teacher-ai-empty-state',
  '今天要处理什么？',
  '可以直接查询课程、考试、题库信息，或继续输入题目与试卷需求。',
  'teacher-ai-history-rail-label',
  '历史会话',
  '搜索会话',
  '新建对话',
  '打开历史会话',
  '搜索历史会话',
  '创建新对话',
  'teacher-ai-history-heading',
  '选择要继续的对话',
  'teacher-ai-history-group-copy',
  '按时间浏览最近的生成与追问记录',
  'agent-history-item-eyebrow',
  'agent-history-item-summary',
  'agent-history-item-status',
  'data-teacher-ai-example="查看课程列表"',
  'data-teacher-ai-example="查看考试列表"',
  'data-teacher-ai-example="查询题库概览"',
  'data-teacher-ai-web-search',
  '联网搜索',
  'teacher-ai-input-web-search-active',
  'aria-pressed="false"',
  '<i class="fa fa-search" aria-hidden="true"></i>',
  "const webSearchMessage = `联网搜索 ${rawMessage}`;",
  'webSearchButton.setAttribute(\'aria-pressed\', String(isActive));',
  'event.detail.message = webSearchMessage;',
  'event.detail.displayMessage = rawMessage;',
  'teacher-ai-bottom-note',
  '回答会结合课程、作业与历史会话',
  'class="teacher-ai-followup-prompts"',
  '基于刚才内容生成课堂练习题',
  '把刚才内容整理成教案大纲',
  '继续追问并给出可复制的板书要点',
  'fa-times',
  'scrollbar-gutter: stable;',
  'agent-history-item.agent-history-item-current::before',
  'function initializeTeacherAiEmptyState()',
  'async function requestTeacherAi(endpoint, payload) {',
  "const accessToken = sessionStorage.getItem('token');",
  "const response = await fetch(endpoint, {",
  "'Content-Type': 'application/json'",
  "'Authorization': `Bearer ${accessToken}`",
  'function buildQuestionResultsFromApi(payload) {',
  'function buildExamResultsFromApi(payload) {',
  'function buildLearningSuggestionResults(payload, context = {}) {',
  'function buildUnavailableAiToolResult(title, message) {',
  "showResults([buildUnavailableAiToolResult('知识点讲解',",
  "showResults([buildUnavailableAiToolResult('作业批改',",
  "showResults([buildUnavailableAiToolResult('教学计划生成',",
  "showMessage('当前暂未提供知识点讲解能力，请稍后再试。', 'info');",
  "showMessage('当前暂未提供作业批改能力，请稍后再试。', 'info');",
  "showMessage('当前暂未提供教学计划生成能力，请稍后再试。', 'info');",
  'function initializeTeacherAiSidebar()',
  'initializeTeacherAiSidebar();',
  'class="form-control agent-command-input" data-agent-input',
  '.teacher-ai-chat-shell {'
].forEach(snippet => {
  assertIncludes(pageContent, snippet, 'teacher ai tools contract mismatch.');
});

[
  'async requestSessionDetail(sessionId) {',
  'renderSessionHistory(session) {',
  "window.dispatchEvent(new CustomEvent('agent-session-cleared'));"
].forEach(snippet => {
  assertIncludes(agentChatContent, snippet, 'teacher ai chat session hydration contract mismatch.');
});

[
  'function getIntentPresentation(value) {',
  'function getSessionSummary(session) {',
  'function getSessionMeta(session) {',
  "window.addEventListener('agent-session-cleared', () => {",
  '继续补充题型、难度或知识点要求。',
  '最近内容：',
  'aria-label="继续会话：',
  'agent-history-item-footer'
].forEach(snippet => {
  assertIncludes(agentHistoryContent, snippet, 'teacher ai history contract mismatch.');
});

[
  'const mockResults = [',
  '// 模拟AI生成过程',
  'showResults(mockResults);',
  "showMessage('知识点讲解生成成功！', 'success');",
  "showMessage('作业批改完成！', 'success');",
  "showMessage('学情分析完成！', 'success');",
  "showMessage('教学计划生成成功！', 'success');",
  'id="teacher-ai-tools-availability-banner"',
  '当前已接通：题目生成、试卷生成、学习建议',
  '知识点讲解、作业批改、教学计划暂不可用',
  'class="teacher-ai-tool-status-list"',
  '<strong>暂未接通</strong>',
  'id="question-type"',
  'id="specific-requirements"',
  'id="knowledge-depth"',
  'id="additional-instructions"',
  'id="exam-description"',
  'id="analyze-course"',
  'id="analyze-dimension"',
  'id="analyze-time-range"',
  '可以试试查询课程',
  '像 ChatGPT 一样输入自然语言指令',
  '智能学习 Agent',
  '已接入企业 Agent',
  '给智能学习 Agent 发送消息',
  'Agent 操作历史',
  'AI正在处理',
  'data-agent-command=',
  'showToolHelp(',
  'class="ai-tools-grid"',
  'class="ai-tool-card"',
  'class="teacher-ai-chat-toolbar"',
  'class="teacher-ai-chat-toolbar-actions"',
  '<h3 class="ai-tool-card-title">题目生成</h3>',
  '<h3 class="ai-tool-card-title">试卷生成</h3>',
  '<h3 class="ai-tool-card-title">AI学习建议</h3>',
  '基于知识点自动生成各种类型的题目',
  '根据课程大纲和知识点自动生成完整的试卷',
  '基于学生身份生成学习建议',
  'id="question-generator"',
  'id="question-generator-form"',
  'id="exam-generator"',
  'id="exam-generator-form"',
  'id="learning-analyzer"',
  'id="learning-analyzer-form"',
  'id="question-topic"',
  'id="difficulty"',
  'id="question-count"',
  'id="exam-course-name"',
  'id="exam-total-score"',
  'id="exam-duration"',
  'id="exam-difficulty"',
  'id="analyze-student"',
  'bindFormSubmissions();',
  'function bindFormSubmissions()',
  'function activateTool(toolId)',
  'function cancelTool(toolId)',
  'function generateQuestions()',
  'function generateExam()',
  'function analyzeLearning()',
  "await requestTeacherAi('/api/ai/generate-questions',",
  "await requestTeacherAi('/api/ai/generate-exam',",
  "await requestTeacherAi('/api/ai/learning-suggestions',",
  "showMessage('题目生成成功！', 'success');",
  "showMessage('试卷生成成功！', 'success');",
  "showMessage('学习建议生成成功！', 'success');",
  "onclick=\"activateTool('knowledge-explainer')\"",
  "onclick=\"activateTool('assignment-evaluator')\"",
  "onclick=\"activateTool('teaching-planner')\"",
  '<h3 class="ai-tool-card-title">知识点讲解</h3>',
  '<h3 class="ai-tool-card-title">作业批改</h3>',
  '<h3 class="ai-tool-card-title">教学计划生成</h3>'
].forEach(snippet => {
  assertNotIncludes(
    pageContent,
    snippet,
    'teacher ai tools should not keep local fake AI generation or unsupported filter inputs.'
  );
});

[
  'class="agent-history-preview teacher-agent-history-preview"',
  'data-agent-history-preview',
  '最近会话',
  '打开历史记录可继续最近的教学问答和已确认操作。',
  'data-agent-history-preview-open'
].forEach(snippet => {
  assertNotIncludes(
    pageContent,
    snippet,
    'teacher ai tools should keep history in the header drawer button instead of a duplicate recent-session card.'
  );
});

assertIncludes(
  apiContent,
  "currentPage === 'teacher-ai-tools.html'",
  'api.js should treat teacher-ai-tools.html as a page with its own initialization so it does not call assignment boot helpers.'
);

[
  'function renderAgentText(value) {',
  'function renderAgentRichText(value) {',
  'function hasValue(value) {',
  ".replace(/\\*\\*([^*\\n][^*\\n]*?)\\*\\*/g, '<strong>$1</strong>')",
  ".replace(/`([^`\\n]+?)`/g, '<code>$1</code>');",
  'const AGENT_FIELD_LABELS = {',
  "difficulty: '难度'",
  "topic: '主题'",
  "count: '数量'",
  'const AGENT_VALUE_LABELS = {',
  "EXECUTED: '已执行'",
  "ACTION_PREVIEW: '待确认操作'",
  "DATA: '查询结果'",
  'function formatAgentLabel(value) {',
  'function renderScalarValue(value) {',
  'function isQuestionLike(value) {',
  'function isCourseLike(value) {',
  'function renderCollectionPayload(value) {',
  'function renderQuestionCard(question, index) {',
  'function renderCourseCard(course, options = {}) {',
  'function renderCourseDetailCard(course) {',
  'function buildQuestionDraftSummary(value, questions) {',
  'function buildQuestionDraftCommands(value, questions) {',
  'function renderQuestionDraftPanel(value, questions) {',
  'function buildResponseTitle(payload) {',
  'function renderQuestionList(questions) {',
  'class="agent-question-draft"',
  'class="agent-question-draft-toolbar"',
  'class="agent-question-draft-summary"',
  'data-agent-question-action="publish"',
  'data-agent-question-action="regenerate"',
  'data-agent-question-action="edit"',
  'data-agent-copy-questions',
  'data-agent-question-draft-payload',
  'data-agent-publish-course',
  'data-agent-publish-class',
  'data-agent-publish-confirm',
  'data-agent-publish-cancel',
  "this.fetchTeacherOptions('/api/teacher/courses')",
  "this.fetchTeacherOptions('/api/teacher/classes')",
  "context.questionDraft = { ...options.questionDraft };",
  "agentCommandAction: 'publish_question_draft'",
  'async sendJsonChat(message, options = {})',
  'forceJson: true',
  '题目草稿',
  '已生成',
  '题库充足',
  'class="agent-course-card"',
  'class="agent-course-detail-card"',
  'class="agent-course-meta"',
  'value.courses.every(isCourseLike)',
  'class="agent-question-card"',
  'class="agent-question-content"',
  'class="agent-question-options"',
  'class="agent-question-meta"',
  "body.innerHTML = renderAgentRichText(text);",
  "this.append('agent', renderAgentRichText(payload.message || '已处理'), '', options);",
  'const responseTitle = buildResponseTitle(payload);',
  '<p><strong>${renderAgentText(responseTitle)}</strong></p>',
  'function formatMessageTime(value = new Date()) {',
  'class="agent-message-content"',
  'class="agent-message-time"',
  'const avatarText = role === \'user\' ? \'T\' : \'AI\';',
  'role="img"',
  '教师头像',
  '学习助手头像',
  "this.currentController = new AbortController();",
  "this.thinkingEl = this.append('agent', '<p>思考中...</p>', 'thinking');",
  "'<i class=\"fa fa-pause\" aria-hidden=\"true\"></i><span class=\"visually-hidden\">暂停</span>'",
  "'<i class=\"fa fa-paper-plane\" aria-hidden=\"true\"></i><span class=\"visually-hidden\">发送</span>'",
  "this.currentController.abort();",
  "signal: controller?.signal",
  "error?.name === 'AbortError'",
  "this.removeThinking();",
  "const event = new CustomEvent('agent-before-send'",
  "this.formEl?.dispatchEvent(event);",
  "this.append('user', renderAgentRichText(options.displayMessage || message));"
].forEach(snippet => {
  assertIncludes(agentChatContent, snippet, 'agent chat panel should show thinking state and expose pause behavior.');
});

assertNotIncludes(
  agentChatContent,
  "'<i class=\"fa fa-spinner fa-spin\"></i> 发送中'",
  'agent chat send button should not use a spinner while the model is thinking.'
);

[
  '.agent-message-thinking .agent-message-body',
  '.agent-thinking-dots',
  '.agent-message-agent .agent-message-body',
  '.agent-message-avatar span',
  '.agent-message-agent .agent-message-avatar',
  '.agent-message-content',
  '.agent-message-time',
  '.agent-data-result',
  '.agent-question-draft',
  '.agent-question-draft-header',
  '.agent-question-draft-toolbar',
  '.agent-question-draft-summary',
  '.agent-question-action',
  '.agent-question-publish-panel',
  '.agent-question-publish-fields',
  '.agent-question-publish-actions',
  '.agent-question-card',
  '.agent-question-header',
  '.agent-question-option',
  '.agent-course-card',
  '.agent-course-meta',
  '.agent-collection-summary',
  '.agent-form .btn.agent-submit-paused',
  '.agent-panel.chatgpt-like',
  '.agent-input-shell',
  '.agent-panel.chatgpt-like .agent-message-body h4',
  '.agent-panel.chatgpt-like .agent-message-body code',
  '.agent-history-panel.agent-history-drawer',
  '.agent-history-panel.agent-history-drawer.agent-history-open',
  '.agent-form .agent-command-input',
  'min-height: 52px;',
  'line-height: 1.5;'
].forEach(snippet => {
  assertIncludes(agentChatStyleContent, snippet, 'agent chat thinking/pause state should have explicit styling.');
});

[
  'teacher-ai-followup-prompts',
  'position: sticky;',
  'max-width: 900px;',
  'grid-template-rows: minmax(0, 1fr) auto auto;'
].forEach(snippet => {
  assertIncludes(pageContent, snippet, 'teacher ai tools conversation layout should keep the composer visible and the reading lane constrained.');
});

if ((pageContent.match(/<button[^>]*data-teacher-ai-web-search/g) || []).length !== 1) {
  throw new Error('teacher ai tools should expose exactly one internet search toggle in the composer.');
}

const webSearchButtonMatch = pageContent.match(/<button[^>]*data-teacher-ai-web-search[\s\S]*?<\/button>/);
if (!webSearchButtonMatch) {
  throw new Error('teacher ai tools should render the web-search toggle button.');
}

if (webSearchButtonMatch[0].includes('teacher-ai-input-web-search-label') || />\s*联网搜索\s*</.test(webSearchButtonMatch[0])) {
  throw new Error('teacher ai tools web-search toggle should be icon-only, with no visible text label.');
}

[
  'class="teacher-ai-example-prompt teacher-ai-web-search"',
  "const message = query ? `联网搜索 ${query}` : '联网搜索 ';",
  'input.form?.requestSubmit();'
].forEach(snippet => {
  assertNotIncludes(
    pageContent,
    snippet,
    'teacher ai tools should not keep the old duplicate web-search starter or auto-submit behavior.'
  );
});

[
  "this.toggleEls = Array.from(document.querySelectorAll('[data-agent-history-toggle]'));",
  "this.closeEl = root.querySelector('[data-agent-history-close]');",
  "this.backdropEl = document.querySelector('[data-agent-history-backdrop]');",
  "this.hoverZoneEl = document.querySelector('[data-agent-history-hover-zone]');",
  "this.hoverZoneEl?.addEventListener('mouseenter', () => this.open());",
  "event.key === 'Escape'",
  "open() {",
  "close() {",
  "toggle() {"
].forEach(snippet => {
  assertIncludes(agentHistoryContent, snippet, 'agent history panel should support drawer controls.');
});

console.log('teacher ai tools contract OK');
