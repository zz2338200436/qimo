const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

const pageContent = fs.readFileSync('frontend/dist/teacher-courses.html', 'utf8');
const moduleContent = fs.readFileSync('frontend/dist/teacher-courses-courses.js', 'utf8');

assertIncludes(
  pageContent,
  'teacher-courses-courses.js?v=20260524-1',
  'teacher-courses page should load the extracted course list/search module.'
);

[
  'async function loadCourses(page = 1, params = {}) {',
  'async function handleCourseSearch() {',
  'function handleCourseClear() {',
  'async function handleCoursePageClick(page) {',
  'function renderCoursePagination(currentPage, totalPages, paginationElement) {',
  'async function initCourseSearch() {',
  'global.loadCourses = loadCourses;',
  'global.handleCourseSearch = handleCourseSearch;',
  'global.handleCourseClear = handleCourseClear;',
  'global.handleCoursePageClick = handleCoursePageClick;',
  'global.handlePageClick = handleCoursePageClick;',
  'global.renderCoursePagination = renderCoursePagination;',
  'global.initCourseSearch = initCourseSearch;'
].forEach(snippet => {
  assertIncludes(
    moduleContent,
    snippet,
    'teacher courses courses module contract mismatch.'
  );
});

[
  'async function loadCourses(page = 1, params = {}) {',
  'async function handleCourseSearch() {',
  'function handleCourseClear() {',
  'async function initCourseSearch() {',
  'function loadCoursePageData(page) {',
  'function renderCoursePagination(currentPage, totalPages, paginationElement) {',
  'async function handlePageClick(page) {',
  'function showCourseLoadingState() {',
  'function handleCourseResponse(data) {',
  'function hideCourseError() {',
  'function renderCourseList(courses) {',
  'function getStatusColor(status) {',
  'function showCourseNoResults() {',
  'function hideCourseNoResults() {',
  'function clearCourseList() {',
  'function showCourseError(message) {',
  'function handleCourseError(error) {',
  'async function viewCourseDetails(courseId) {',
  'function initPage() {'
].forEach(snippet => {
  if (pageContent.includes(snippet)) {
    throw new Error(`teacher-courses legacy host code should be removed from page. Found: ${snippet}`);
  }
});

[
  'function handleCoursePaginationClick(pageType) {',
  'function updateCoursePaginationUI(newPage) {'
].forEach(snippet => {
  if (pageContent.includes(snippet)) {
    throw new Error(`teacher-courses legacy pagination chain should be removed from page. Found: ${snippet}`);
  }
});

[
  "document.addEventListener('DOMContentLoaded', function() {",
  'let currentTab = \'courses\';',
  'let currentPage = 1;',
  'let totalPages = 1;',
  'let currentSearchParams = {};',
  'courses: [],'
].forEach(snippet => {
  if (pageContent.includes(snippet)) {
    throw new Error(`teacher-courses legacy state/init should be removed from page. Found: ${snippet}`);
  }
});

console.log('teacher courses courses contract OK');
