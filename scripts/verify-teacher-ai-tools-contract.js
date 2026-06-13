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

[
  'id="teacher-ai-tools-availability-banner"',
  '当前已接通：题目生成、试卷生成、学习建议',
  '知识点讲解、作业批改、教学计划暂不可用',
  'AI学习建议',
  '基于学生身份生成学习建议',
  'async function requestTeacherAi(endpoint, payload) {',
  "const accessToken = sessionStorage.getItem('token');",
  "const response = await fetch(endpoint, {",
  "'Content-Type': 'application/json'",
  "'Authorization': `Bearer ${accessToken}`",
  'function buildQuestionResultsFromApi(payload) {',
  'function buildExamResultsFromApi(payload) {',
  'function buildLearningSuggestionResults(payload, context = {}) {',
  'function buildUnavailableAiToolResult(title, message) {',
  "await requestTeacherAi('/api/ai/generate-questions',",
  "await requestTeacherAi('/api/ai/generate-exam',",
  "await requestTeacherAi('/api/ai/learning-suggestions',",
  "showResults(buildQuestionResultsFromApi(result));",
  "showResults(buildExamResultsFromApi(result));",
  "showResults(buildLearningSuggestionResults(result, {",
  "showResults([buildUnavailableAiToolResult('知识点讲解',",
  "showResults([buildUnavailableAiToolResult('作业批改',",
  "showResults([buildUnavailableAiToolResult('教学计划生成',",
  "showMessage('题目生成成功！', 'success');",
  "showMessage('试卷生成成功！', 'success');",
  "showMessage('学习建议生成成功！', 'success');",
  "showMessage('当前 AI 服务暂未提供知识点讲解能力，请稍后再试。', 'info');",
  "showMessage('当前 AI 服务暂未提供作业批改能力，请稍后再试。', 'info');",
  "showMessage('当前 AI 服务暂未提供教学计划生成能力，请稍后再试。', 'info');"
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
  'id="question-type"',
  'id="specific-requirements"',
  'id="knowledge-depth"',
  'id="additional-instructions"',
  'id="exam-description"',
  'id="analyze-course"',
  'id="analyze-dimension"',
  'id="analyze-time-range"'
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

console.log('teacher ai tools contract OK');
