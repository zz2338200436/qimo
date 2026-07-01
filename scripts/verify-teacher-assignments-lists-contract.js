const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

const pageContent = fs.readFileSync('frontend/dist/teacher-assignments.html', 'utf8');
const listsModuleContent = fs.readFileSync('frontend/dist/teacher-assignments-lists.js', 'utf8');
const shellModuleContent = fs.readFileSync('frontend/dist/teacher-assignments-shell.js', 'utf8');

assertIncludes(
  pageContent,
  'teacher-assignments-lists.js?v=20260524-1',
  'teacher-assignments page should load the extracted lists module.'
);

[
  'async function loadAssignments(page = 1, params = {}) {',
  'function renderAssignments(assignmentsData) {',
  'async function loadExams(page = 1, params = {}) {',
  'function renderExams(examsData) {',
  'async function loadSubmissions(page = 1, params = {}) {',
  'function renderSubmissions(submissionsData) {',
  "global.loadAssignments = loadAssignments;",
  "global.loadExams = loadExams;",
  "global.loadSubmissions = loadSubmissions;"
].forEach(snippet => {
  assertIncludes(
    listsModuleContent,
    snippet,
    'teacher assignments lists module contract mismatch.'
  );
});

[
  'global.searchAssignments = runAssignmentSearch;',
  'global.searchExams = runExamSearch;',
  'global.searchSubmissions = runSubmissionSearch;',
  'global.resetAssignmentSearch = resetAssignmentSearch;',
  'global.resetExamSearch = resetExamSearch;',
  'global.resetSubmissionSearch = resetSubmissionSearch;'
].forEach(snippet => {
  assertIncludes(
    shellModuleContent,
    snippet,
    'teacher assignments shell contract mismatch.'
  );
});

[
  "bindSearchButton('#submissions-content .search-filter .btn-primary', runSubmissionSearch);",
  'global.searchSubmissions = runSubmissionSearch;',
  'async function refreshTeacherAssignmentsSummary() {',
  'await refreshTeacherAssignmentsSummary();',
  'global.refreshTeacherAssignmentsSummary = refreshTeacherAssignmentsSummary;'
].forEach(snippet => {
  assertIncludes(
    shellModuleContent,
    snippet,
    'teacher assignments submission search ownership contract mismatch.'
  );
});

console.log('teacher assignments lists contract OK');
