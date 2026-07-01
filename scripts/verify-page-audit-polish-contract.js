const fs = require('node:fs');

function read(path) {
  return fs.readFileSync(path, 'utf8');
}

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

function assertNotIncludes(content, needle, message) {
  if (content.includes(needle)) {
    throw new Error(`${message} Found unexpected snippet: ${needle}`);
  }
}

const teacherCourses = read('frontend/dist/teacher-courses.html');
const teacherAssignments = read('frontend/dist/teacher-assignments.html');
const teacherKnowledge = read('frontend/dist/teacher-knowledge.html');
const agentHistory = read('frontend/dist/agent-history-panel.js');

[
  [teacherCourses, '课程与班级管理', '先维护课程与班级，再处理分配关系。', '课程数据区', 'teacher-courses'],
  [teacherAssignments, '作业与考试发布', '先发布任务，再跟进提交与批改。', '任务数据区', 'teacher-assignments'],
  [teacherKnowledge, '知识点薄弱分析', '先筛选教学范围，再查看统计和薄弱项。', '分析数据区', 'teacher-knowledge']
].forEach(([content, pageTitle, focusText, dataLabel, name]) => {
  assertIncludes(content, 'class="teacher-workspace-guide"', `${name} should expose a first-screen task guide.`);
  assertIncludes(content, focusText, `${name} should explain the primary page flow.`);
  assertIncludes(content, dataLabel, `${name} should label the main data area.`);
  assertIncludes(content, 'class="teacher-section-label"', `${name} should add section labels for heavy panels.`);
  assertIncludes(content, pageTitle, `${name} should keep the original page title.`);
});

[
  'data-agent-history-preview-open',
  'bindHistoryPreviewOpeners()',
  "document.querySelectorAll('[data-agent-history-preview-open]')",
  "this.open();"
].forEach(snippet => {
  assertIncludes(agentHistory, snippet, 'agent history preview controls should open the existing drawer.');
});

[
  '通知历史接口暂未接入',
  '当前仅展示本地发送成功记录',
  '历史记录已收起'
].forEach(snippet => {
  assertNotIncludes(
    read('frontend/dist/teacher-notifications.html') + read('frontend/dist/student-ai-assistant.html'),
    snippet,
    'audit-facing pages should not expose implementation-status wording.'
  );
});

console.log('page audit polish contract OK');
