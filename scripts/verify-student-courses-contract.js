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

const page = fs.readFileSync('frontend/dist/student-courses.html', 'utf8');

[
  'const COURSE_CATEGORY_OPTIONS = [',
  'const COURSE_STATUS_OPTIONS = [',
  "value: 'practice'",
  "value: 'active'",
  'function populateCourseFilterOptions(selectId, options, placeholderLabel) {',
  'function buildCourseCategoryParams(category) {',
  'function buildCourseStatusParams(status) {',
  'const categoryParams = buildCourseCategoryParams(category);',
  'const statusParams = buildCourseStatusParams(status);',
  '...statusParams,',
  '...categoryParams'
].forEach(snippet => {
  assertIncludes(page, snippet, 'student-courses enum compatibility contract mismatch.');
});

[
  '<option value="公共课">公共课</option>',
  '<option value="专业课">专业课</option>',
  '<option value="选修课">选修课</option>',
  '<option value="必修">必修</option>',
  '<option value="进行中">进行中</option>',
  '<option value="已结束">已结束</option>',
  '<option value="未开始">未开始</option>'
].forEach(snippet => {
  assertNotIncludes(page, snippet, 'student-courses page should no longer hard-code legacy Chinese-only filter values.');
});

console.log('student courses contract OK');
