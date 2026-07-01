const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

const pageContent = fs.readFileSync('frontend/dist/teacher-courses.html', 'utf8');
const moduleContent = fs.readFileSync('frontend/dist/teacher-courses-shell.js', 'utf8');

assertIncludes(
  pageContent,
  'teacher-courses-shell.js?v=20260602-1',
  'teacher-courses page should load the extracted shell module.'
);

assertIncludes(
  pageContent,
  'await bootstrapTeacherCoursesPage();',
  'teacher-courses page should bootstrap through the shell module.'
);

[
  'function initializeSearchHistory() {',
  'const teacherCoursesPageState = {',
  'function initCurrentUser() {',
  'function getCurrentUser() {',
  'async function loadCourseCategories() {',
  'async function loadCourseStatuses() {',
  'async function loadAssessmentMethods() {',
  'async function loadUserDropdownForTeacher(containerId) {',
  'function initTabs() {',
  'function showNotification(message, type = \'info\') {',
  'function reportResourceLoadFailure(message) {',
  'function handleApiError(error) {',
  'function showLoading(element) {',
  'function hideLoading(element) {',
  'function verifyBootstrapLoaded() {',
  'function bindSidebarToggle() {',
  'const localAssessmentMethods = [',
  "console.log('加载本地考核方式选项:', localAssessmentMethods);",
  'function showBrowserError(error) {',
  'function bindBrowserErrorHandlers() {',
  'function bindSidebarShortcuts() {',
  'function bindAddCourseModalValidationReset() {',
  'function activateTeacherCoursesTab(targetTab) {',
  'async function bootstrapTeacherCoursesPage() {',
  'global.teacherCoursesPageState = teacherCoursesPageState;',
  'global.getCurrentUser = getCurrentUser;',
  'global.loadCourseCategories = loadCourseCategories;',
  'global.loadCourseStatuses = loadCourseStatuses;',
  'global.initTabs = initTabs;',
  'global.showNotification = showNotification;',
  'global.reportResourceLoadFailure = reportResourceLoadFailure;',
  'global.handleApiError = handleApiError;',
  'global.showLoading = showLoading;',
  'global.hideLoading = hideLoading;',
  'global.loadAssessmentMethods = loadAssessmentMethods;',
  'global.loadUserDropdownForTeacher = loadUserDropdownForTeacher;',
  'global.showBrowserError = showBrowserError;',
  'global.bindAddCourseModalValidationReset = bindAddCourseModalValidationReset;',
  'global.activateTeacherCoursesTab = activateTeacherCoursesTab;',
  'global.bootstrapTeacherCoursesPage = bootstrapTeacherCoursesPage;'
].forEach(snippet => {
  assertIncludes(
    moduleContent,
    snippet,
    'teacher courses shell module contract mismatch.'
  );
});

[
  'let searchHistory = {',
  'const teacherCoursesPageState = {',
  'function initCurrentUser() {',
  'function getCurrentUser() {',
  'async function loadCourseCategories() {',
  'async function loadCourseStatuses() {',
  'async function loadAssessmentMethods() {',
  'async function loadUserDropdownForTeacher(containerId) {',
  'function initTabs() {',
  'function showNotification(message, type = \'info\') {',
  'function reportResourceLoadFailure(message) {',
  'function handleApiError(error) {',
  'function showLoading(element) {',
  'function hideLoading(element) {',
  "document.querySelectorAll('.sidebar-menu .menu-item').forEach(item => {",
  "document.getElementById('toggleBtn').addEventListener('click', function() {",
  'function showBrowserError(error) {',
  "window.addEventListener('error', function(errorEvent) {",
  "window.addEventListener('unhandledrejection', function(promiseRejectionEvent) {"
].forEach(snippet => {
  if (pageContent.includes(snippet)) {
    throw new Error(`teacher-courses legacy shell code should be removed from page. Found: ${snippet}`);
  }
});

[
  'const mockMethods = [',
  "console.log('加载考核方式数据:', mockMethods);",
  "console.error('加载考核方式数据失败:', error);"
].forEach(snippet => {
  if (moduleContent.includes(snippet)) {
    throw new Error(`teacher-courses shell should no longer describe local assessment options as mock data. Found: ${snippet}`);
  }
});

console.log('teacher courses shell contract OK');
