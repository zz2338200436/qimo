const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

const pageContent = fs.readFileSync('frontend/dist/teacher-courses.html', 'utf8');
const moduleContent = fs.readFileSync('frontend/dist/teacher-courses-classes.js', 'utf8');

assertIncludes(
  pageContent,
  'teacher-courses-classes.js?v=20260524-1',
  'teacher-courses page should load the extracted class list/search module.'
);

[
  'function initClassSearch() {',
  'async function handleClassSearch() {',
  'function handleClassClear() {',
  'async function performClassSearch() {',
  'function renderClassList(classes) {',
  'function handleClassPaginationClick(pageType) {',
  'function getAllClasses() {',
  'global.initClassSearch = initClassSearch;',
  'global.handleClassSearch = handleClassSearch;',
  'global.handleClassClear = handleClassClear;',
  'global.performClassSearch = performClassSearch;',
  'global.renderClassList = renderClassList;',
  'global.handleClassPaginationClick = handleClassPaginationClick;',
  'global.getTeacherCoursesAllClasses = getAllClasses;'
].forEach(snippet => {
  assertIncludes(
    moduleContent,
    snippet,
    'teacher courses classes module contract mismatch.'
  );
});

[
  'function initClassSearch() {',
  'function handleClassSearch() {',
  'function handleClassClear() {',
  'let currentClassPage = 1;',
  'const classesPerPage = 9;',
  'let totalClassPages = 1;',
  'let allClasses = [];',
  'function performClassSearch() {',
  'function getCurrentPageClasses() {',
  'function updateClassPaginationUI() {',
  'function showLoadingState() {',
  'function handleClassSearchError(error) {',
  'function showError(message) {',
  'function renderClassList(classes) {',
  'function handleClassPaginationClick(pageType) {'
].forEach(snippet => {
  if (pageContent.includes(snippet)) {
    throw new Error(`teacher-courses legacy class host code should be removed from page. Found: ${snippet}`);
  }
});

[
  'async function handleClassSearch() {',
  'searchHistory.classes.unshift(searchParams);',
  'function handleClassClear() {',
  "document.getElementById('class-teacher').value = '';",
].forEach(snippet => {
  if (pageContent.includes(snippet)) {
    throw new Error(`teacher-courses duplicate class search tail code should be removed from page. Found: ${snippet}`);
  }
});

console.log('teacher courses classes contract OK');
