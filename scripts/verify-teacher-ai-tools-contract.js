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

const pageContent = fs.readFileSync('frontend/dist/teacher-ai-tools.html', 'utf8');
const apiContent = fs.readFileSync('frontend/dist/api.js', 'utf8');
const agentChatContent = fs.readFileSync('frontend/dist/agent-chat-panel.js', 'utf8');
const agentChatStyleContent = fs.readFileSync('frontend/dist/agent-chat-panel.css', 'utf8');
const agentHistoryContent = fs.readFileSync('frontend/dist/agent-history-panel.js', 'utf8');

[
  '<h2 class="page-title">辅助工具</h2>',
  '<h3 class="agent-panel-title">学习助手</h3>',
  '服务已开通',
  'placeholder="输入你的需求"',
  '<h3>历史记录</h3>',
  'teacher-ai-chat-shell',
  'class="teacher-ai-panel-actions"',
  'teacher-ai-history-button',
  'data-agent-history-toggle',
  'data-agent-history-close',
  'class="agent-history-hover-corner"',
  'class="agent-history-backdrop"',
  'class="agent-history-panel agent-history-drawer"',
  'agent-input-shell',
  'data-teacher-ai-empty-state',
  '可以这样开始',
  '选择一个示例快速填入输入框，也可以直接输入具体要求。',
  'data-teacher-ai-example="生成课堂练习题"',
  'data-teacher-ai-example="生成一份试卷"',
  'data-teacher-ai-example="生成学习建议"',
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

assertIncludes(
  apiContent,
  "currentPage === 'teacher-ai-tools.html'",
  'api.js should treat teacher-ai-tools.html as a page with its own initialization so it does not call assignment boot helpers.'
);

[
  'function renderAgentText(value) {',
  'function hasValue(value) {',
  ".replace(/\\*\\*([^*\\n][^*\\n]*?)\\*\\*/g, '<strong>$1</strong>');",
  'const AGENT_FIELD_LABELS = {',
  "status: '状态'",
  "aiResult: '处理结果'",
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
  'function renderQuestionList(questions) {',
  'class="agent-course-card"',
  'class="agent-course-detail-card"',
  'class="agent-course-meta"',
  'value.courses.every(isCourseLike)',
  'class="agent-question-card"',
  'class="agent-question-content"',
  'class="agent-question-options"',
  'class="agent-question-meta"',
  "<p>${renderAgentText(payload.message || '已处理')}</p>",
  "<p><strong>${renderAgentText(payload.message || '执行完成')}</strong></p>",
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
  "this.removeThinking();"
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
  '.agent-question-card',
  '.agent-question-header',
  '.agent-question-option',
  '.agent-course-card',
  '.agent-course-meta',
  '.agent-collection-summary',
  '.agent-form .btn.agent-submit-paused',
  '.agent-panel.chatgpt-like',
  '.agent-input-shell',
  '.agent-history-panel.agent-history-drawer',
  '.agent-history-panel.agent-history-drawer.agent-history-open',
  '.agent-form .agent-command-input',
  'min-height: 52px;',
  'line-height: 1.5;'
].forEach(snippet => {
  assertIncludes(agentChatStyleContent, snippet, 'agent chat thinking/pause state should have explicit styling.');
});

[
  "this.toggleEl = document.querySelector('[data-agent-history-toggle]');",
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
