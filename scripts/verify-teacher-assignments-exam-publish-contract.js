const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

const apiContent = fs.readFileSync('frontend/dist/api.js', 'utf8');
const examPublishContent = fs.readFileSync('frontend/dist/teacher-assignments-exam-publish.js', 'utf8');
const examCrudContent = fs.readFileSync('frontend/dist/teacher-assignments-exam-crud.js', 'utf8');
const pageContent = fs.readFileSync('frontend/dist/teacher-assignments.html', 'utf8');

assertIncludes(
  pageContent,
  'api.js?v=20260611-assignment-detail-1',
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
  'serializeExamLocalDateTime(examStart)',
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
  'function serializeExamLocalDateTime(value) {',
  'function parseExamDateTimeForInput(value) {',
  'function buildExamEndLocalDateTime(startValue, durationMinutes) {',
  'global.serializeExamLocalDateTime = serializeExamLocalDateTime;',
  'global.parseExamDateTimeForInput = parseExamDateTimeForInput;',
  'global.buildExamEndLocalDateTime = buildExamEndLocalDateTime;',
  'document.getElementById(\'edit-exam-start\').value = parseExamDateTimeForInput(exam.startTime);',
  'startTime: serializeExamLocalDateTime(examStart),',
  'endTime: buildExamEndLocalDateTime(examStart, examDuration),'
].forEach(snippet => {
  assertIncludes(
    examCrudContent,
    snippet,
    'teacher assignments exam CRUD module should keep datetime-local values as local wall-clock time.'
  );
});

[
  'startDateTime.toISOString()',
  'endDateTime.toISOString()',
  'new Date(examStart)'
].forEach(snippet => {
  if (examPublishContent.includes(snippet) || examCrudContent.includes(snippet)) {
    throw new Error(`Teacher exam publish/edit should not convert datetime-local values to UTC. Found: ${snippet}`);
  }
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
  'updateExam(examId, data) {',
  'courseId: data.courseId',
  'startTime: data.startTime',
  'endTime: data.endTime',
  'publishDate: data.publishDate',
  'isActive: data.isActive',
  'isOnline: data.isOnline',
  'requestData.knowledgePointIds = data.knowledgePointIds;',
  'requestData.questions = data.questions;'
].forEach(snippet => {
  assertIncludes(
    apiContent,
    snippet,
    'TeacherAPI.updateExam should send camelCase fields expected by backend validation.'
  );
});

[
  'course_id: data.courseId',
  'start_time: data.startTime',
  'end_time: data.endTime',
  'publish_date: data.publishDate',
  'is_active: data.isActive',
  'is_online: data.isOnline'
].forEach(snippet => {
  if (apiContent.includes(snippet)) {
    throw new Error(`TeacherAPI.updateExam should not send legacy snake_case fields. Found: ${snippet}`);
  }
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
