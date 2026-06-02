const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

const pageContent = fs.readFileSync('frontend/dist/teacher-courses.html', 'utf8');
const moduleContent = fs.readFileSync('frontend/dist/teacher-courses-metadata.js', 'utf8');

assertIncludes(
  pageContent,
  'teacher-courses-metadata.js?v=20260524-1',
  'teacher-courses page should load the extracted metadata module.'
);

[
  'async function loadCourseOptions() {',
  'function populateCourseDropdown(courses) {',
  'async function loadTeacherMajors() {',
  'function applyGradeAndMajorMetadata(classes) {',
  'async function loadGradeAndMajorData(force = false) {',
  'async function ensureClassMetadataLoaded(force = false) {',
  'async function ensureClassCourseOptionsLoaded(force = false) {',
  'async function ensureAssignmentCourseOptionsLoaded(force = false) {',
  'function bindModalMetadataPrefetch() {',
  '/api/teacher/majors',
  'global.loadCourseOptions = loadCourseOptions;',
  'global.applyGradeAndMajorMetadata = applyGradeAndMajorMetadata;',
  'global.ensureClassMetadataLoaded = ensureClassMetadataLoaded;',
  'global.ensureClassCourseOptionsLoaded = ensureClassCourseOptionsLoaded;',
  'global.ensureAssignmentCourseOptionsLoaded = ensureAssignmentCourseOptionsLoaded;',
  'global.bindTeacherCoursesMetadata = bindModalMetadataPrefetch;',
  'function showCourseLoadFailureState() {',
  'global.showCourseLoadFailureState = showCourseLoadFailureState;'
].forEach(snippet => {
  assertIncludes(
    moduleContent,
    snippet,
    'teacher courses metadata module contract mismatch.'
  );
});

[
  'async function loadCourseOptions() {',
  'function populateCourseDropdown(courses) {',
  'function useMockCourseData() {',
  'function buildGradeAndMajorMetadata(classes) {',
  'function applyGradeAndMajorMetadata(classes) {',
  'function areClassSearchFiltersEmpty() {',
  'async function loadGradeAndMajorData(force = false) {',
  'function populateGradeDropdown(grades) {',
  'function populateMajorDropdown(majorList) {',
  'async function ensureClassCourseOptionsLoaded(force = false) {',
  'async function ensureAssignmentCourseOptionsLoaded(force = false) {',
  "const addClassModalElement = document.getElementById('addClassModal');",
  "const editClassModalElement = document.getElementById('editClassModal');",
  "const assignCourseModalElement = document.getElementById('assignCourseModal');"
].forEach(snippet => {
  if (pageContent.includes(snippet)) {
    throw new Error(`teacher-courses legacy metadata/modal preload code should be removed from page. Found: ${snippet}`);
  }
});

[
  'const mockMajorList = [',
  'const allMajors = [',
  "['计算机科学与技术', 1]",
  'useMockGradeAndMajorData();',
  'function useMockCourseData() {',
  'global.useMockCourseData = useMockCourseData;'
].forEach(snippet => {
  if (moduleContent.includes(snippet)) {
    throw new Error(`teacher-courses metadata should no longer hard-code fallback major catalogs. Found: ${snippet}`);
  }
});

console.log('teacher courses metadata contract OK');
