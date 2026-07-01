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

const pageContent = fs.readFileSync('frontend/dist/student-settings.html', 'utf8');

[
  "async function exportData() {",
  "const response = await fetch(`${API_BASE_URL}/api/student/export-data`, {",
  "const result = await response.json();",
  "const blob = new Blob([JSON.stringify(result.data, null, 2)], { type: 'application/json;charset=utf-8' });",
  "a.download = `student-data-export-${new Date().toISOString().slice(0, 10)}.json`;",
  "showMessage('学生学习数据导出成功', 'success');"
].forEach(snippet => {
  assertIncludes(pageContent, snippet, 'student settings export contract mismatch.');
});

[
  "const blob = await response.blob();",
  "a.download = `student_data_${new Date().toISOString().slice(0, 10)}.json`;",
  "const fallback = {",
  "notificationSettings: loadStudentLocalPreferences('studentNotificationSettings')",
  "privacySettings: loadStudentLocalPreferences('studentPrivacySettings')",
  "a.download = `student-data-local-${new Date().toISOString().slice(0, 10)}.json`;",
  "showMessage('服务器导出暂不可用，已导出当前页面和本地偏好数据。', 'info');"
].forEach(snippet => {
  assertNotIncludes(
    pageContent,
    snippet,
    'student settings export should not keep the old local-fallback export disguise.'
  );
});

console.log('student settings export contract OK');
