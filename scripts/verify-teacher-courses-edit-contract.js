const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

const page = fs.readFileSync('frontend/dist/teacher-courses.html', 'utf8');

const editClassSignature = 'async function editClass(classId)';
const editClassStart = page.indexOf(editClassSignature);
if (editClassStart === -1) {
  throw new Error('editClass definition not found in teacher-courses.html');
}

const editClassEndMarker = '\n        // 验证字段的事件处理函数';
const editClassEnd = page.indexOf(editClassEndMarker, editClassStart);
const editClassBody = editClassEnd === -1
  ? page.slice(editClassStart)
  : page.slice(editClassStart, editClassEnd);

assertIncludes(
  editClassBody,
  "gradeField.value = cls.grade || cls.classGrade || cls.year || '';",
  'editClass should map backend year into the edit grade select.'
);

console.log('teacher courses edit contract OK');
