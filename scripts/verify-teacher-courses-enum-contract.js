const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

function assertNotIncludes(content, needle, message) {
  if (content.includes(needle)) {
    throw new Error(`${message} Found forbidden snippet: ${needle}`);
  }
}

const shellContent = fs.readFileSync('frontend/dist/teacher-courses-shell.js', 'utf8');
const coursesContent = fs.readFileSync('frontend/dist/teacher-courses-courses.js', 'utf8');
const crudContent = fs.readFileSync('frontend/dist/teacher-courses-course-crud.js', 'utf8');

[
  'const COURSE_CATEGORY_OPTIONS = [',
  'const COURSE_STATUS_OPTIONS = [',
  "value: 'practice'",
  "value: 'required'",
  "value: 'active'",
  "value: 'inactive'",
  "value: 'closed'",
  'function populateCourseEnumSelects(selects, options) {',
  'function ensureSelectHasOption(select, value, label = value) {',
  'async function loadCourseCategories() {',
  'async function loadCourseStatuses() {'
].forEach(snippet => {
  assertIncludes(shellContent, snippet, 'teacher-courses shell enum contract mismatch.');
});

[
  "const courseCategory = document.getElementById('course-category').value;",
  "const courseStatus = document.getElementById('course-status').value;",
  'function buildCourseCategoryApiParams(courseCategory) {',
  'function buildCourseStatusApiParams(courseStatus) {',
  'const categoryParams = buildCourseCategoryApiParams(courseCategory);',
  'const statusParams = buildCourseStatusApiParams(courseStatus);',
  'apiParams: {',
  '...categoryParams,',
  '...statusParams'
].forEach(snippet => {
  assertIncludes(coursesContent, snippet, 'teacher-courses search enum compatibility contract mismatch.');
});

[
  'ensureSelectHasOption(categorySelect, course.courseCategory',
  'ensureSelectHasOption(statusSelect, course.courseStatus',
  "document.getElementById('edit-course-category').value = course.courseCategory;",
  "document.getElementById('edit-course-status').value = course.courseStatus;"
].forEach(snippet => {
  if (snippet.startsWith('ensureSelectHasOption')) {
    assertIncludes(crudContent, snippet, 'teacher-courses edit compatibility contract mismatch.');
  } else {
    assertNotIncludes(crudContent, snippet, 'teacher-courses edit should no longer assume enum values already exist in the select.');
  }
});

console.log('teacher courses enum contract OK');
