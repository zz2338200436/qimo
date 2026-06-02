const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

const pageContent = fs.readFileSync('frontend/dist/teacher-courses.html', 'utf8');
const moduleContent = fs.readFileSync('frontend/dist/teacher-courses-class-crud.js', 'utf8');

assertIncludes(
  pageContent,
  'teacher-courses-class-crud.js?v=20260524-1',
  'teacher-courses page should load the extracted class CRUD module.'
);

[
  'async function deleteClass(classId) {',
  'async function submitAddClass() {',
  'function clearClassFormState(form) {',
  'function resolveClassCreateErrorMessage(data) {',
  'global.deleteClass = deleteClass;',
  'global.submitAddClass = submitAddClass;'
].forEach(snippet => {
  assertIncludes(
    moduleContent,
    snippet,
    'teacher courses class CRUD module contract mismatch.'
  );
});

[
  'async function deleteClass(classId) {',
  'async function submitAddClass() {',
  'function showFieldError(field, message) {',
  'function clearFieldValidation(field) {',
  'function clearValidation(formId) {',
  'function resetFormValidation(formId) {'
].forEach(snippet => {
  if (pageContent.includes(snippet)) {
    throw new Error(`teacher-courses legacy class CRUD/helper code should be removed from page. Found: ${snippet}`);
  }
});

console.log('teacher courses class crud contract OK');
