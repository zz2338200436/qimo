const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

const pageContent = fs.readFileSync('frontend/dist/teacher-assignments.html', 'utf8');
const apiContent = fs.readFileSync('frontend/dist/api.js', 'utf8');
const publishModuleContent = fs.readFileSync('frontend/dist/teacher-assignments-assignment-publish.js', 'utf8');

assertIncludes(
  pageContent,
  'teacher-assignments-assignment-publish.js?v=20260602-2',
  'teacher-assignments page should load the extracted assignment publish module.'
);

[
  'async function submitAddAssignment() {',
  'async function loadTeacherAssignmentClasses() {',
  'function populateAssignmentClassDropdowns(classes) {',
  'function filterClassesByCourse(classes, courseId) {',
  'function refreshClassDropdownForCourse(courseSelectId, classSelectId) {',
  'function bindCourseClassFilters() {',
  "showClassDropdownPlaceholder(classSelect, '请先选择课程');",
  "function populateSingleClassDropdown(select, classes, emptyText = '该课程暂无可发布班级') {",
  'function bindAssignmentClassDropdownRefresh() {',
  'const result = await teacherAPI.createAssignment(assignmentData);',
  "showMessage('作业发布成功！', 'success');",
  'await loadAssignments();',
  'await refreshTeacherAssignmentsSummary();',
  'global.bindAssignmentClassDropdownRefresh = bindAssignmentClassDropdownRefresh;',
  'global.bindCourseClassFilters = bindCourseClassFilters;',
  'global.refreshClassDropdownForCourse = refreshClassDropdownForCourse;',
  'global.loadTeacherAssignmentClasses = loadTeacherAssignmentClasses;',
  'global.submitAddAssignment = submitAddAssignment;'
].forEach(snippet => {
  assertIncludes(
    publishModuleContent,
    snippet,
    'teacher assignments assignment publish module contract mismatch.'
  );
});

if (apiContent.includes('async function submitAddAssignment() {')) {
  throw new Error('submitAddAssignment should no longer live in frontend/dist/api.js');
}

[
  '计算机科学与技术1班',
  '软件工程2班',
  '数据科学1班',
  '人工智能1班',
  'value="class1"',
  'value="class2"',
  'value="class3"',
  'value="class4"'
].forEach(snippet => {
  if (pageContent.includes(snippet)) {
    throw new Error(`teacher assignments should not keep hard-coded class options. Found: ${snippet}`);
  }
});

console.log('teacher assignment publish contract OK');
