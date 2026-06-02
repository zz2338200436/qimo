const fs = require('fs');
const path = require('path');

const examCrudPath = path.join(
  __dirname,
  '..',
  'frontend',
  'dist',
  'teacher-assignments-exam-crud.js'
);
const assignmentCrudPath = path.join(
  __dirname,
  '..',
  'frontend',
  'dist',
  'teacher-assignments-assignment-crud.js'
);
const shellPath = path.join(
  __dirname,
  '..',
  'frontend',
  'dist',
  'teacher-assignments-shell.js'
);
const gradingPath = path.join(
  __dirname,
  '..',
  'frontend',
  'dist',
  'teacher-assignments-grading.js'
);

const assignmentCrudContent = fs.readFileSync(assignmentCrudPath, 'utf8');
const examCrudContent = fs.readFileSync(examCrudPath, 'utf8');
const shellContent = fs.readFileSync(shellPath, 'utf8');
const gradingContent = fs.readFileSync(gradingPath, 'utf8');

const fileExpectations = [
  {
    fileLabel: 'teacher-assignments-shell.js',
    content: shellContent,
    snippets: [
      "const assignmentId = params.get('assignmentId');",
      "await editAssignment(entryState.assignmentId);",
      "await viewAssignment(entryState.assignmentId);"
    ]
  },
  {
    fileLabel: 'teacher-assignments-assignment-crud.js',
    content: assignmentCrudContent,
    snippets: [
      'async function viewAssignment(assignmentId) {',
      'async function editAssignment(assignmentId) {'
    ]
  },
  {
    fileLabel: 'teacher-assignments-exam-crud.js',
    content: examCrudContent,
    snippets: [
      "const viewExamSubmissionsButton = document.getElementById('view-exam-submissions-btn');",
      'viewExamSubmissionsButton.dataset.examId = String(examId);'
    ]
  },
  {
    fileLabel: 'teacher-assignments-grading.js',
    content: gradingContent,
    snippets: [
      'function openExamSubmissionListFromViewModal()',
      "const examId = viewExamSubmissionsButton?.dataset?.examId;",
      'gradeExam(parseInt(examId, 10));'
    ]
  }
];

for (const expectation of fileExpectations) {
  for (const snippet of expectation.snippets) {
    if (!expectation.content.includes(snippet)) {
      throw new Error(`Missing expected teacher exam view contract snippet in ${expectation.fileLabel}: ${snippet}`);
    }
  }
}

console.log('Teacher exam view contract verified.');
