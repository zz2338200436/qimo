const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

const gradingModule = fs.readFileSync('frontend/dist/teacher-assignments-grading.js', 'utf8');
const signature = 'async function submitGradeSubmission()';
const lastStart = gradingModule.lastIndexOf(signature);

if (lastStart === -1) {
  throw new Error('submitGradeSubmission definition not found in teacher-assignments-grading.js');
}

const nextMarker = '\n    function bindGradeSubmissionButton()';
const endIndex = gradingModule.indexOf(nextMarker, lastStart);
const activeFunctionBody = endIndex === -1
  ? gradingModule.slice(lastStart)
  : gradingModule.slice(lastStart, endIndex);

assertIncludes(
  activeFunctionBody,
  'await loadAssignments();',
  'Active submitGradeSubmission should refresh the outer assignment list after grading.'
);

console.log('teacher grade refresh contract OK');
