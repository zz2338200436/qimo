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

const pageContent = fs.readFileSync('frontend/dist/student-ai-assistant.html', 'utf8');

[
  'class="content-container student-ai-page"',
  'class="ai-assistant-container student-ai-chat-shell"',
  'class="agent-panel chatgpt-like" data-agent-panel data-agent-role="STUDENT"',
  'class="student-ai-history-rail"',
  'student-ai-history-rail-label',
  '历史会话',
  '搜索会话',
  '新建对话',
  '打开历史会话',
  '搜索历史会话',
  '创建新对话',
  'data-agent-history-toggle',
  'data-agent-history-close',
  'class="agent-history-panel agent-history-drawer"',
  'student-ai-history-heading',
  '选择要继续的对话',
  'student-ai-history-group-copy',
  '按时间浏览最近的学习问答和复习记录',
  'data-student-ai-empty-state',
  'student-ai-empty-hint',
  '今天想学什么？',
  '把题目、知识点或作业要求发给我',
  'data-student-ai-example="查看我的作业"',
  'data-student-ai-example="查看考试安排"',
  '回答会结合课程资料',
  '界面保持单一焦点',
  'placeholder="输入题目、知识点、作业或考试相关问题..."',
  'function initializeStudentAiEmptyState()'
].forEach(snippet => {
  assertIncludes(pageContent, snippet, 'student ai assistant contract mismatch.');
});

[
  'assistantModeBanner',
  '当前已接通：AI学习建议',
  '自由问答暂不可用',
  'setTimeout(() => {',
  "addMessage('ai', getAIMockResponse(message));",
  'function getAIMockResponse(question) {',
  '给我本周复习建议',
  '帮我梳理当前学习重点',
  'class="student-ai-service-note"',
  'data-student-ai-service-note',
  'AI学习建议',
  '当前优先开放学习建议能力',
  'data-student-ai-example="生成我的学习建议"',
  '生成学习建议',
  'student-agent-history-preview'
].forEach(snippet => {
  assertNotIncludes(
    pageContent,
    snippet,
    'student ai assistant should not keep local demo chat or fake quick questions.'
  );
});

console.log('student ai assistant contract OK');
