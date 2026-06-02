const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

const pageContent = fs.readFileSync('frontend/dist/teacher-assignments.html', 'utf8');
const gradingContent = fs.readFileSync('frontend/dist/teacher-assignments-grading.js', 'utf8');

[
  'id="viewSubmissionModal"',
  'id="viewSubmissionBody"',
  'id="gradeSubmissionBtn"',
  'teacher-assignments-grading.js?v=20260602-1'
].forEach(snippet => {
  assertIncludes(
    pageContent,
    snippet,
    'teacher assignments page should include the submission detail modal and bust the grading module cache.'
  );
});

[
  'function escapeSubmissionText(value)',
  'function formatSubmissionDate(value)',
  'function isSubmissionGraded(submission)',
  "const modalElement = document.getElementById('viewSubmissionModal');",
  "const modalBody = document.getElementById('viewSubmissionBody');",
  "throw new Error('页面缺少提交详情弹窗，请刷新后重试');",
  'const assignmentTitle = submission.title || submission.assignmentTitle',
  'bootstrap.Modal.getOrCreateInstance(modalElement)',
  'white-space: pre-wrap; word-break: break-word;'
].forEach(snippet => {
  assertIncludes(
    gradingContent,
    snippet,
    'teacher assignment submission view module contract mismatch.'
  );
});

if (gradingContent.includes("new bootstrap.Modal(document.getElementById('viewSubmissionModal'))")) {
  throw new Error('viewSubmission should not instantiate a Bootstrap modal with a possibly missing element.');
}

console.log('teacher submission view contract OK');
