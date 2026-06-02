const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

function assertNotIncludes(content, needle, message) {
  if (content.includes(needle)) {
    throw new Error(`${message} Found: ${needle}`);
  }
}

const pageContent = fs.readFileSync('frontend/dist/teacher-courses.html', 'utf8');
const moduleContent = fs.readFileSync('frontend/dist/teacher-courses-students.js', 'utf8');
const apiContent = fs.readFileSync('frontend/dist/api.js', 'utf8');

assertIncludes(
  pageContent,
  'teacher-courses-students.js?v=20260524-1',
  'teacher-courses page should load the updated class students module.'
);

[
  'async function addStudentToCurrentClass() {',
  'const studentIdentifier = studentIdInput.value.trim();',
  "showNotification('请输入学生ID或用户名', 'warning');",
  'const requestBody = { studentIdentifier };',
  "body: JSON.stringify(requestBody)",
  'async function confirmReplaceStudentClass(studentIdentifier, classId, message) {',
  "showNotification('学生已添加到班级', 'success');",
  'global.confirmReplaceStudentClass = confirmReplaceStudentClass;'
].forEach(snippet => {
  assertIncludes(
    moduleContent,
    snippet,
    'teacher courses students module contract mismatch.'
  );
});

[
  'addStudentToClass(classId, data) {',
  "return this.apiService.post(`/api/teacher/classes/${classId}/students`, data);"
].forEach(snippet => {
  assertIncludes(
    apiContent,
    snippet,
    'teacher API client should expose the class student add endpoint.'
  );
});

[
  'placeholder="输入学生ID或用户名"',
  '学生需已存在于系统，输入学生ID或用户名后即可加入当前班级。',
  '<th>学生ID</th>',
  '<th>用户名（学号）</th>'
].forEach(snippet => {
  assertIncludes(
    pageContent,
    snippet,
    'teacher-courses student modal copy should match supported identifier behavior.'
  );
});

[
  "showNotification('请输入学生ID/学号', 'warning');",
  "placeholder=\"输入学生ID或学号\"",
  '学生需已存在于系统，输入ID/学号后即可加入当前班级。',
  '<th>学号</th>'
].forEach(snippet => {
  assertNotIncludes(
    pageContent + '\n' + moduleContent,
    snippet,
    'teacher-courses legacy student identifier wording should be removed.'
  );
});

console.log('teacher courses students contract OK');
