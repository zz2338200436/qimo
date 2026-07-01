const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

const shellModuleContent = fs.readFileSync('frontend/dist/teacher-assignments-shell.js', 'utf8');

[
  'function populateAllCourseDropdowns(courses) {',
  'async function loadTeacherAssignmentCourses() {',
  'const response = await teacherAPI.getCourses({ page: 1, size: 100 });',
  'populateAllCourseDropdowns(courseList);',
  'syncCourseMap(courseList, course => course.id || course.courseCode);',
  'async function bootstrapTeacherAssignmentsPage(options = {}) {',
  "content.classList.toggle('active', contentTabId === tabId);",
  "content.style.display = contentTabId === tabId ? 'block' : 'none';",
  'await loadTeacherAssignmentCourses();',
  'await global.loadTeacherAssignmentClasses();',
  "typeof global.bindAssignmentClassDropdownRefresh === 'function'",
  'global.bindAssignmentClassDropdownRefresh();',
  'global.loadTeacherAssignmentCourses = loadTeacherAssignmentCourses;',
  'global.bootstrapTeacherAssignmentsPage = bootstrapTeacherAssignmentsPage;'
].forEach(snippet => {
  assertIncludes(
    shellModuleContent,
    snippet,
    'teacher assignments shell contract mismatch.'
  );
});

[
  'const mockCourses = [',
  'populateAllCourseDropdowns(mockCourses);',
  'syncCourseMap(mockCourses, course => course.courseCode);'
].forEach(snippet => {
  if (shellModuleContent.includes(snippet)) {
    throw new Error(`teacher assignments page should no longer prefill course dropdowns with mock courses during bootstrap. Found: ${snippet}`);
  }
});

console.log('teacher assignments shell contract OK');
