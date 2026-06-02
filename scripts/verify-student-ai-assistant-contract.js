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
  'id="assistantModeBanner"',
  '当前已接通：AI学习建议',
  '自由问答暂不可用',
  '生成我的学习建议',
  '给我本周复习建议',
  '帮我梳理当前学习重点',
  "placeholder=\"可输入“生成学习建议”或“复习建议”，当前暂不支持通用问答...\"",
  'async function requestStudentLearningSuggestions(context = {}) {',
  "const accessToken = sessionStorage.getItem('token');",
  "'Authorization': `Bearer ${accessToken}`",
  "headers['X-XSRF-TOKEN'] = csrfToken;",
  "const response = await fetch('/api/ai/learning-suggestions', {",
  'function classifyAssistantRequest(message) {',
  'const learningSuggestionKeywords = [',
  'function buildLearningSuggestionMessage(payload, context = {}) {',
  'function buildUnsupportedChatMessage(question) {',
  'async function handleStudentAiRequest(message, context = {}) {',
  "await handleStudentAiRequest(message);",
  "sendQuickQuestion('生成我的学习建议')",
  "sendQuickQuestion('给我本周复习建议')",
  "sendQuickQuestion('帮我梳理当前学习重点')"
].forEach(snippet => {
  assertIncludes(pageContent, snippet, 'student ai assistant contract mismatch.');
});

[
  '当前页面为演示模式：会展示本地模拟回复，不会调用真实 AI 服务。',
  '当前 JWT 微服务联调环境下，AI 助手仍处于前端演示模式：消息会显示本地模拟回复，不会调用真实 AI 服务。',
  'setTimeout(() => {',
  "addMessage('ai', getAIMockResponse(message));",
  'function getAIMockResponse(question) {',
  'Python的面向对象编程怎么理解？',
  '数据库索引的作用是什么？',
  '数据结构中链表和数组的区别？'
].forEach(snippet => {
  assertNotIncludes(
    pageContent,
    snippet,
    'student ai assistant should not keep local demo chat or fake quick questions.'
  );
});

console.log('student ai assistant contract OK');
