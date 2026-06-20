const fs = require('fs');

function assertIncludes(content, snippet, message) {
  if (!content.includes(snippet)) {
    throw new Error(message + ` Missing: ${snippet}`);
  }
}

const pageContent = fs.readFileSync('frontend/dist/teacher-assignments.html', 'utf8');
const apiContent = fs.readFileSync('frontend/dist/api.js', 'utf8');
const assignmentCrudContent = fs.readFileSync('frontend/dist/teacher-assignments-assignment-crud.js', 'utf8');
const examCrudContent = fs.readFileSync('frontend/dist/teacher-assignments-exam-crud.js', 'utf8');
const gradingContent = fs.readFileSync('frontend/dist/teacher-assignments-grading.js', 'utf8');

assertIncludes(
  pageContent,
  'api.js?v=20260611-assignment-detail-1',
  'teacher assignments page should bust the shared api.js cache.'
);
assertIncludes(
  apiContent,
  "this.apiService.get('/api/teacher/submissions', params)",
  'teacher submissions list should use the dedicated teacher submissions endpoint.'
);
assertIncludes(
  assignmentCrudContent,
  'const assignment = response.data?.assignment || response.data;',
  'assignment detail should support legacy and microservice response shapes.'
);
assertIncludes(
  assignmentCrudContent,
  "viewExamTitle.textContent = '作业详情';",
  'assignment detail should set the shared view modal title to assignment detail.'
);
assertIncludes(
  assignmentCrudContent,
  'viewExamSubmissionsButton.dataset.assignmentId = String(assignmentId);',
  'assignment detail should store assignment id on the submissions button.'
);
assertIncludes(
  assignmentCrudContent,
  'delete viewExamSubmissionsButton.dataset.examId;',
  'assignment detail should clear stale exam id from the shared submissions button.'
);
assertIncludes(
  examCrudContent,
  "viewExamTitle.textContent = '考试详情';",
  'exam detail should restore the shared view modal title to exam detail.'
);
assertIncludes(
  examCrudContent,
  'delete viewExamSubmissionsButton.dataset.assignmentId;',
  'exam detail should clear stale assignment id from the shared submissions button.'
);
assertIncludes(
  gradingContent,
  'gradeAssignment(parseInt(assignmentId, 10));',
  'shared submissions button should open assignment submissions when assignmentId is present.'
);

console.log('teacher assignment detail contract OK');
