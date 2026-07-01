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
  'api.js?v=20260630-attachments-2',
  'teacher assignments page should bust the shared api.js cache.'
);
assertIncludes(
  apiContent,
  "this.apiService.get('/api/teacher/submissions', params)",
  'teacher submissions list should use the dedicated teacher submissions endpoint.'
);
assertIncludes(
  assignmentCrudContent,
  'const assignment = normalizeAssignmentDetail(response.data);',
  'assignment detail should support legacy and microservice response shapes.'
);
assertIncludes(
  assignmentCrudContent,
  'function normalizeAssignmentAttachments(rawAssignment) {',
  'assignment detail should normalize attachments from supported response shapes.'
);
assertIncludes(
  assignmentCrudContent,
  'function renderAssignmentAttachmentLinksHtml(attachments) {',
  'assignment detail should render real downloadable attachment links.'
);
assertIncludes(
  apiContent,
  "async function downloadAttachment(url, filename = '附件') {",
  'shared API module should provide an authorized attachment download helper.'
);
assertIncludes(
  assignmentCrudContent,
  'data-download-url="${escapeHtml(href)}"',
  'assignment detail should download attachments through the authorized helper.'
);
assertIncludes(
  assignmentCrudContent,
  'data-download-name="${escapeHtml(name)}"',
  'assignment edit modal should download attachments through the authorized helper.'
);
assertIncludes(
  assignmentCrudContent,
  'onclick="downloadAttachmentFromButton(this)"',
  'assignment attachments should use the safe data-attribute download handler.'
);
if (assignmentCrudContent.includes('href="${escapeHtml(href)}" download')) {
  throw new Error('assignment attachments should not use bare anchor downloads because they omit Authorization.');
}
if (assignmentCrudContent.includes("onclick=\"downloadAttachment('${escapeHtml(href)}'")) {
  throw new Error('assignment attachments should not pass escaped filenames through inline JavaScript strings.');
}
assertIncludes(
  assignmentCrudContent,
  '${renderAssignmentAttachmentLinksHtml(assignment.attachments)}',
  'assignment view modal should show assignment attachments.'
);
assertIncludes(
  assignmentCrudContent,
  'renderEditAssignmentAttachments(assignment.attachments);',
  'assignment edit modal should populate uploaded attachments from API data.'
);
assertIncludes(
  assignmentCrudContent,
  "const container = document.getElementById('edit-assignment-existing-files');",
  'assignment edit modal should target the uploaded attachment list container.'
);
assertIncludes(
  assignmentCrudContent,
  "const fileInput = document.getElementById('edit-assignment-files');",
  'assignment edit should read newly selected attachment files.'
);
assertIncludes(
  assignmentCrudContent,
  "formData.append('payload', new Blob([JSON.stringify(assignmentData)], { type: 'application/json' }));",
  'assignment edit should send JSON payload together with uploaded files.'
);
assertIncludes(
  assignmentCrudContent,
  "Array.from(fileInput.files).forEach(file => formData.append('files', file));",
  'assignment edit should append selected files to multipart update requests.'
);
assertIncludes(
  apiContent,
  "return this.apiService.request(`/api/teacher/assignments/${assignmentId}`, {",
  'TeacherAPI.updateAssignment should support multipart FormData requests.'
);
if (pageContent.includes('Python基础作业1.docx')) {
  throw new Error('teacher assignments page should not show hard-coded fake assignment attachments.');
}
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
  examCrudContent,
  'function renderExamAttachmentLinksHtml(attachments) {',
  'exam detail should render real downloadable attachment links.'
);
assertIncludes(
  examCrudContent,
  'data-download-url="${escapeHtml(path)}"',
  'exam detail/edit should download attachments through the authorized helper.'
);
assertIncludes(
  examCrudContent,
  'data-download-name="${escapeHtml(name)}"',
  'exam detail/edit should preserve the original filename for authorized downloads.'
);
assertIncludes(
  examCrudContent,
  'onclick="downloadAttachmentFromButton(this)"',
  'exam attachments should use the safe data-attribute download handler.'
);
if (examCrudContent.includes('href="${escapeHtml(path)}" download')) {
  throw new Error('exam attachments should not use bare anchor downloads because they omit Authorization.');
}
if (examCrudContent.includes("onclick=\"downloadAttachment('${escapeHtml(path)}'")) {
  throw new Error('exam attachments should not pass escaped filenames through inline JavaScript strings.');
}
assertIncludes(
  examCrudContent,
  'renderEditExamAttachments(exam.attachments);',
  'exam edit modal should populate uploaded exam attachments from API data.'
);
assertIncludes(
  pageContent,
  'id="edit-exam-existing-files"',
  'exam edit modal should contain a real uploaded attachment list container.'
);
if (pageContent.includes('数据结构与算法单元测试.docx')) {
  throw new Error('teacher assignments page should not show hard-coded fake exam attachments.');
}
assertIncludes(
  gradingContent,
  'gradeAssignment(parseInt(assignmentId, 10));',
  'shared submissions button should open assignment submissions when assignmentId is present.'
);

console.log('teacher assignment detail contract OK');
