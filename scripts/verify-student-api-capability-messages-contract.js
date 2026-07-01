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

const apiContent = fs.readFileSync('frontend/dist/api.js', 'utf8');

[
  "dashboardPerformance: '当前 JWT 微服务环境暂未提供学生综合表现接口，页面将改用已接通的数据源进行统计。'",
  "activities: '当前 JWT 微服务环境暂未提供学生活动流接口，已仅展示可用数据。'",
  "avatarUpload: '当前 JWT 微服务环境已接通头像上传接口。'",
  "getExams(params = {}) {",
  "getExamDetail(examId) {",
  'getScores() {',
  'getExamSubmissions() {',
  'submitExam(examId, formData) {',
  'submitExamJson(examId, data) {',
  "guardStudentCapability(\n            'exams',\n            () => this.apiService.get('/api/student/exams', params)",
  "guardStudentCapability(\n            'examDetail',\n            () => this.apiService.get(`/api/student/exams/${examId}`)",
  "guardStudentCapability(\n            'scores',\n            () => this.apiService.get('/api/student/scores')",
  "guardStudentCapability(\n            'examSubmit',\n            () => this.apiService.get('/api/student/exam-submissions')"
].forEach(snippet => {
  assertIncludes(apiContent, snippet, 'student api capability message contract mismatch.');
});

[
  '当前 JWT 微服务环境暂未提供考试列表接口',
  '当前 JWT 微服务环境暂未提供考试详情接口',
  '当前 JWT 微服务环境暂未提供成绩查询接口',
  '当前 JWT 微服务环境暂未提供考试提交记录接口',
  '当前 JWT 微服务环境暂未提供考试提交接口'
].forEach(snippet => {
  assertNotIncludes(apiContent, snippet, 'student api should not keep stale exam/score unsupported messages once runtime capability is live.');
});

console.log('student api capability messages contract OK');
