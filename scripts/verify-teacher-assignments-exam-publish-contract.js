const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

const apiContent = fs.readFileSync('frontend/dist/api.js', 'utf8');
const examPublishContent = fs.readFileSync('frontend/dist/teacher-assignments-exam-publish.js', 'utf8');
const pageContent = fs.readFileSync('frontend/dist/teacher-assignments.html', 'utf8');

assertIncludes(
  pageContent,
  'api.js?v=20260602-1',
  'teacher assignments page should bust cache for the fixed API module.'
);

assertIncludes(
  pageContent,
  'teacher-assignments-exam-publish.js?v=20260602-1',
  'teacher assignments page should bust cache for the fixed exam publish module.'
);

assertIncludes(
  apiContent,
  'getKnowledgePointsByCourse(courseId) {',
  'TeacherAPI should expose a dedicated course knowledge point endpoint helper.'
);

assertIncludes(
  apiContent,
  "return this.apiService.get(`/api/teacher/knowledge-points/course/${courseId}`);",
  'TeacherAPI course knowledge point helper should use the real teacher endpoint.'
);

[
  'async function submitAddExam() {',
  'const result = await teacherAPI.createExam(examData);',
  'if (!result || result.success === false) {',
  "showMessage('考试发布失败：' + (result?.message || 'API返回空结果'), 'error');",
  'await loadExams();',
  'async function loadKnowledgePoints(courseId, selectElementId) {',
  'const teacherAPI = new TeacherAPI(apiService);',
  'const result = await teacherAPI.getKnowledgePointsByCourse(courseId);',
  "selectElement.innerHTML = '<option value=\"\" disabled>知识点暂不可用，请稍后重试</option>';",
  "selectElement.innerHTML = '<option value=\"\" disabled>加载知识点失败，请稍后重试</option>';"
].forEach(snippet => {
  assertIncludes(
    examPublishContent,
    snippet,
    'teacher assignments exam publish module contract mismatch.'
  );
});

[
  'courseId: data.courseId',
  'startTime: data.startTime',
  'endTime: data.endTime',
  'publishDate: data.publishDate',
  'isActive: data.isActive',
  'isOnline: data.isOnline'
].forEach(snippet => {
  assertIncludes(
    apiContent,
    snippet,
    'TeacherAPI.createExam should send camelCase fields expected by backend validation.'
  );
});

[
  "const response = await fetch(`/api/teacher/knowledge-points/course/${courseId}`, {",
  "selectElement.innerHTML = '<option value=\"\" disabled>当前环境暂未提供知识点接口</option>';",
  'course_id: data.courseId',
  'start_time: data.startTime',
  'end_time: data.endTime',
  'publish_date: data.publishDate',
  'is_active: data.isActive',
  'is_online: data.isOnline'
].forEach(snippet => {
  if (examPublishContent.includes(snippet)) {
    throw new Error(`teacher assignments exam publish module should no longer rely on legacy knowledge point fallback. Found: ${snippet}`);
  }
});

console.log('teacher assignments exam publish contract OK');
