const fs = require('fs');
const path = require('path');

const filePath = path.join(
  __dirname,
  '..',
  'frontend',
  'dist',
  'api.js'
);

const content = fs.readFileSync(filePath, 'utf8');

const requiredSnippets = [
  "const viewExamSubmissionsButton = document.getElementById('view-exam-submissions-btn');",
  'viewExamSubmissionsButton.dataset.examId = String(examId);',
  'function openExamSubmissionListFromViewModal()',
  "const examId = viewExamSubmissionsButton?.dataset?.examId;",
  'gradeExam(parseInt(examId, 10));'
];

for (const snippet of requiredSnippets) {
  if (!content.includes(snippet)) {
    throw new Error(`Missing expected teacher exam view contract snippet: ${snippet}`);
  }
}

console.log('Teacher exam view contract verified.');
