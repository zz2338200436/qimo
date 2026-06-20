const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

const pageContent = fs.readFileSync('frontend/dist/teacher-assignments.html', 'utf8');
const gradingContent = fs.readFileSync('frontend/dist/teacher-assignments-grading.js', 'utf8');

[
  'teacher-assignments-grading.js?v=20260611-content-display-1'
].forEach(snippet => {
  assertIncludes(pageContent, snippet, 'teacher assignments page should bust the grading module cache.');
});

[
  'function formatSubmissionContent(value)',
  'const parsed = JSON.parse(text);',
  'return String(parsed.content);',
  "formatSubmissionContent(submission.content || submission.answerContent || '') || '（无提交内容）'",
  'const displayContent = formatSubmissionContent(submission.content);',
  'const content = formatSubmissionContent(submission.content || submission.answerContent || \'\');'
].forEach(snippet => {
  assertIncludes(gradingContent, snippet, 'teacher submission content display contract mismatch.');
});

console.log('teacher submission content display contract OK');
