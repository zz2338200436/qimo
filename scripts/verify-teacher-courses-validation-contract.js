const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

const pageContent = fs.readFileSync('frontend/dist/teacher-courses.html', 'utf8');
const moduleContent = fs.readFileSync('frontend/dist/teacher-courses-validation.js', 'utf8');

assertIncludes(
  pageContent,
  'teacher-courses-validation.js?v=20260602-1',
  'teacher-courses page should load the extracted validation module.'
);

[
  'async function checkCourseCodeExists(courseCode, currentCourseId = null) {',
  'async function checkClassNameExists(className, currentClassId = null) {',
  'async function validateField(field, feedbackId) {',
  'function initFormValidation(formId) {',
  'async function validateCourseForm(formId) {',
  'async function validateClassForm(formId) {',
  'async function validateForm(formId) {',
  'function resetFormValidation(formId) {',
  'global.checkCourseCodeExists = checkCourseCodeExists;',
  'global.checkClassNameExists = checkClassNameExists;',
  'global.validateField = validateField;',
  'global.initFormValidation = initFormValidation;',
  'global.validateCourseForm = validateCourseForm;',
  'global.validateClassForm = validateClassForm;',
  'global.validateForm = validateForm;',
  'global.resetFormValidation = resetFormValidation;'
].forEach(snippet => {
  assertIncludes(
    moduleContent,
    snippet,
    'teacher courses validation module contract mismatch.'
  );
});

[
  'async function checkCourseCodeExists(courseCode, currentCourseId = null) {',
  'async function validateField(field, feedbackId) {',
  'function initFormValidation(formId) {',
  'async function checkClassNameExists(className, currentClassId = null) {',
  'async function validateCourseForm(formId) {',
  'async function validateClassForm(formId) {',
  'async function validateForm(formId) {',
  'function resetFormValidation(formId) {'
].forEach(snippet => {
  if (pageContent.includes(snippet)) {
    throw new Error(`teacher-courses legacy validation code should be removed from page. Found: ${snippet}`);
  }
});

console.log('teacher courses validation contract OK');
