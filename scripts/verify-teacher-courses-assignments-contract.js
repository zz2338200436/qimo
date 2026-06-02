const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

const pageContent = fs.readFileSync('frontend/dist/teacher-courses.html', 'utf8');
const moduleContent = fs.readFileSync('frontend/dist/teacher-courses-assignments.js', 'utf8');

assertIncludes(
  pageContent,
  'teacher-courses-assignments.js?v=20260523-1',
  'teacher-courses page should load the extracted assignments module.'
);

[
  'async function loadCourseAssignments(page = 1) {',
  'const loadingEl = document.getElementById(\'assignments-loading\');',
  'const noResultsEl = document.getElementById(\'assignments-no-results\');',
  'const errorEl = document.getElementById(\'assignments-error\');',
  'const errorMsgEl = document.getElementById(\'assignments-error-message\');',
  'const response = await teacherAPI.getClassAssignments(params);',
  'if (!assignments || assignments.length === 0) {',
  'if (errorEl && errorMsgEl) {',
  'errorEl.style.display = \'block\';',
  'errorMsgEl.textContent = error.message || \'加载课程分配列表失败，请稍后重试\';',
  'global.loadCourseAssignments = loadCourseAssignments;'
].forEach(snippet => {
  assertIncludes(
    moduleContent,
    snippet,
    'teacher-courses assignments contract mismatch.'
  );
});

[
  '当前联调环境的课程分配列表暂不可用，不影响课程新增与课程列表使用。',
  'if (isEndpointUnavailable(error)) {',
  'noResultsEl.innerHTML = `'
].forEach(snippet => {
  if (moduleContent.includes(snippet)) {
    throw new Error(`teacher-courses assignments should no longer render the legacy degraded unavailable-state copy. Found: ${snippet}`);
  }
});

console.log('teacher courses assignments contract OK');
