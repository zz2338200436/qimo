const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

const apiJs = fs.readFileSync('frontend/dist/api.js', 'utf8');
const signature = 'async function submitGradeSubmission()';
const lastStart = apiJs.lastIndexOf(signature);

if (lastStart === -1) {
  throw new Error('submitGradeSubmission definition not found in api.js');
}

const nextMarker = '\n// 为查看提交模态框的批改按钮添加事件监听';
const endIndex = apiJs.indexOf(nextMarker, lastStart);
const activeFunctionBody = endIndex === -1
  ? apiJs.slice(lastStart)
  : apiJs.slice(lastStart, endIndex);

assertIncludes(
  activeFunctionBody,
  'await loadAssignments();',
  'Active submitGradeSubmission should refresh the outer assignment list after grading.'
);

console.log('teacher grade refresh contract OK');
