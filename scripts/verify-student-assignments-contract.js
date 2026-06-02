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

const page = fs.readFileSync('frontend/dist/student-assignments.html', 'utf8');

[
  "data-tab=\"assignments\"",
  "data-tab=\"exams\"",
  "data-tab=\"scores\"",
  "const loadedTabs = new Set(['assignments']);",
  "if (tabId === 'exams') {",
  'fetchExams();',
  "if (tabId === 'scores') {",
  'fetchScores();',
  "const response = await studentAPI.getExams();",
  "const response = await studentAPI.getScores();",
  "showMessage('检测到当前为 JWT 微服务登录环境，页面将优先使用已接通的作业、考试和成绩接口。', 'info');",
  '首屏先只加载默认打开的作业标签，考试和成绩在切换到对应 tab 时按需取数'
].forEach(snippet => {
  assertIncludes(page, snippet, 'student-assignments contract mismatch.');
});

[
  '避免未切通的考试/成绩链路制造噪音'
].forEach(snippet => {
  assertNotIncludes(page, snippet, 'student-assignments should no longer describe exams/scores as unconnected chains.');
});

console.log('student assignments contract OK');
