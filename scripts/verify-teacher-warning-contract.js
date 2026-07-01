const fs = require('node:fs');
const vm = require('node:vm');

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

const pageContent = fs.readFileSync('frontend/dist/teacher-warning.html', 'utf8');

[
  'id="totalWarningsMetaLabel"',
  'id="pendingWarningsMetaLabel"',
  'id="processingWarningsMetaLabel"',
  'id="resolvedWarningsMetaLabel"',
  'const warningStatMetaLabelText = {',
  "totalWarnings: '当前筛选预警统计'",
  "pendingWarnings: '当前未处理预警统计'",
  "processingWarnings: '当前处理中预警统计'",
  "resolvedWarnings: '当前已解决预警统计'",
  'function buildWarningStatCardMarkup(title, iconColor, iconClass, valueMarkup, metaKey) {',
  'function buildWarningStatsCardsMarkup(stats = {}, options = {}) {',
  'const { loading = false } = options;',
  'function normalizeResponseList(response) {',
  'async function fetchStudentsForCourse(courseId) {',
  'async function fetchStudentsForClass(classId) {',
  'function renderStudentOptions(students, emptyMessage = \'暂无学生数据\') {',
  'async function loadStudents(courseId = null) {',
  'const selectedCourseId = courseId || document.getElementById(\'course-select-add\')?.value || null;',
  'fetchAPI(`/teacher/classes?courseId=${encodeURIComponent(courseId)}`',
  'courseSelectAdd.addEventListener(\'change\', () => loadStudents(courseSelectAdd.value));',
  "'暂无该课程的学生数据'",
  "element.classList.add('is-invalid');",
  "element.parentElement.appendChild(errorElement);",
  'let currentWarningTypeChartType = \'bar\';',
  'function destroyWarningCharts() {',
  'function buildWarningChartsCanvasMarkup() {',
  'function buildWarningChartsEmptyStateMarkup(typeMessage = \'暂无图表数据\', trendMessage = \'暂无图表数据\') {',
  'function bindWarningChartControls() {',
  'function renderWarningChartsCanvasState() {',
  'function renderWarningChartsEmptyState(typeMessage = \'暂无图表数据\', trendMessage = \'暂无图表数据\') {',
  'let warningAnalyticsListState = [];',
  'function buildWarningAnalyticsListParams(filters = {}, size = 100) {',
  'async function fetchWarningAnalyticsList(filters = {}, options = {}) {',
  'const { syncCache = true } = options;',
  'warningAnalyticsListState = uniqueWarnings;',
  'return uniqueWarnings;',
  'function normalizeWarningStatus(status, isResolved = false) {',
  'function updateWarningDetailStatusBadge(status) {',
  'function updateWarningDetailActionState(status) {',
  'warningDetail.studentName || studentName',
  'warningDetail.courseName || \'未关联课程\'',
  'warningDetail.warningLevel || \'未设置\'',
  'warningDetail.reason || warningDetail.warningMessage || \'暂无预警原因说明\'',
  'warningDetail.suggestion || getSuggestionByWarningType(warningType)',
  '<strong>课程：</strong>${warningCourseName}',
  "const warningStatus = normalizeWarningStatus(warning.status, warning.isResolved);",
  "viewWarningDetail('${warning.id}', '${warning.studentId}', '${warning.studentName}', '${warning.warningType}', '${warning.status || ''}', '${warning.isResolved ? 'true' : 'false'}')",
  'const warningStatus = normalizeWarningStatus(initialStatus, initialResolved);',
  'id="warningDetailStatusBadge"',
  'updateWarningDetailStatusBadge(warningStatus);',
  'updateWarningDetailActionState(warningStatus);',
  "document.getElementById('markAsProcessingBtn').addEventListener('click', async function() {",
  "document.getElementById('markAsResolvedBtn').addEventListener('click', async function() {",
  "await markAsProcessing(currentWarningId);",
  "await resolveWarning(currentWarningId);",
  "updateWarningDetailStatusBadge('processing');",
  "updateWarningDetailActionState('processing');",
  "updateWarningDetailStatusBadge('resolved');",
  "updateWarningDetailActionState('resolved');",
  'const analyticsWarnings = await fetchWarningAnalyticsList(filters, { syncCache: false });',
  'const listData = await fetchWarningAnalyticsList({}, { syncCache: true });',
  'const listData = await fetchWarningAnalyticsList(filters, { syncCache: true });',
  'renderWarningChartsCanvasState();',
  'renderWarningChartsEmptyState(',
  'if (listData.length === 0) {',
  "statsCards.innerHTML = buildWarningStatsCardsMarkup({}, { loading: true });",
  'statsCards.innerHTML = buildWarningStatsCardsMarkup(stats);',
  'statsCards.innerHTML = buildWarningStatsCardsMarkup({',
  "'当前筛选预警统计'",
  "'当前未处理预警统计'",
  "'当前处理中预警统计'",
  "'当前已解决预警统计'"
].forEach(snippet => {
  assertIncludes(pageContent, snippet, 'teacher warning contract mismatch.');
});

[
  'async function generateWarnings(page = 1, filters = {}) {',
  '<div class="stat-card-change positive">',
  '<div class="stat-card-change negative">',
  '<i class="fa fa-arrow-up"></i>',
  '<i class="fa fa-arrow-down"></i>',
  'const totalChange = `+${Math.floor(totalWarnings * 0.1)} 条 (${Math.floor(totalWarnings * 0.15)}%)`;',
  'const pendingChange = `+${Math.floor(pendingWarnings * 0.1)} 条 (${Math.floor(pendingWarnings * 0.18)}%)`;',
  'const processingChange = `+${Math.floor(processingWarnings * 0.1)} 条 (${Math.floor(processingWarnings * 0.18)}%)`;',
  'const resolvedChange = `+${Math.floor(resolvedWarnings * 0.15)} 条 (${Math.floor(resolvedWarnings * 0.2)}%)`;',
  '假设使用上周数据作为比较基准',
  '<span>0 条 (0%)</span>',
  '// 如果API调用失败，使用默认数据',
  "warningType: '成绩预警'",
  "warningType: '考勤预警'",
  "warningType: '作业预警'",
  "warningType: '进度预警'",
  "const response = await fetchAPI('/early-warnings/teacher/list?page=1&size=100', {",
  '<p><strong>班级：</strong>计算机科学与技术1班</p>',
  '<p><strong>联系方式：</strong>13800138000</p>',
  '<p><strong>家长联系方式：</strong>13900139000</p>',
  '<p><strong>辅导员：</strong>李老师</p>',
  '<h6 class="card-title">学习数据</h6>',
  '<canvas id="studentScoreChart" height="150"></canvas>',
  "label: '测验成绩'",
  "label: '班级平均成绩'",
  "studentDashboard.recentScores.map(score => score.examName)",
  "studentDashboard.recentScores.map(score => score.score)",
  "studentDashboard.recentScores.map(score => score.classAverage || 0)",
  "课程平均成绩：${averageScore.toFixed(1)}分",
  "作业完成率：${(assignmentCompletionRate * 100).toFixed(1)}%",
  "考勤率：${studentDashboard.courseProgress.inProgressCourses > 0 ? '90%' : '100%'}",
  "学习进度：${studentDashboard.courseProgress.inProgressCourses > 0 ? '70%' : '100%'}",
  '预警消息至少需要10个字符',
  "parent.classList.add('is-invalid');",
  'parent.appendChild(errorElement);'
].forEach(snippet => {
  assertNotIncludes(
    pageContent,
    snippet,
    'teacher warning should not frame inferred warning totals as real deltas.'
  );
});

console.log('teacher warning contract OK');

const inlineScripts = [...pageContent.matchAll(/<script(?:\s[^>]*)?>\s*([\s\S]*?)\s*<\/script>/g)]
  .map(match => match[1])
  .filter(script => script.includes('async function loadStudents(courseId = null)'));
const mainScript = inlineScripts[0];
if (!mainScript) {
  throw new Error('teacher warning script block not found');
}

const studentSelect = {
  innerHTML: '',
  appended: [],
  appendChild(option) {
    this.appended.push(option);
  }
};
const courseSelectAdd = { value: '2' };
const notifications = [];

const context = {
  console,
  URLSearchParams,
  Set,
  encodeURIComponent,
  setTimeout,
  bootstrap: {
    Modal: {
      getInstance() {
        return { hide() {} };
      }
    }
  },
  Chart: function Chart() {},
  sessionStorage: {
    getItem() {
      return 'token';
    }
  },
  window: {
    location: { href: 'teacher-warning.html' },
    addEventListener() {},
    print() {},
    exportToExcel() {}
  },
  document: {
    createElement(tag) {
      return { tag, value: '', textContent: '', className: '', appendChild() {} };
    },
    getElementById(id) {
      if (id === 'student-select') {
        return studentSelect;
      }
      if (id === 'course-select-add') {
        return courseSelectAdd;
      }
      if (id === 'addWarningModal') {
        return { addEventListener() {} };
      }
      return {
        value: '',
        innerHTML: '',
        textContent: '',
        style: {},
        classList: { add() {}, remove() {} },
        addEventListener() {},
        appendChild() {},
        querySelector() { return null; },
        querySelectorAll() { return []; }
      };
    },
    querySelector() {
      return null;
    },
    querySelectorAll() {
      return [];
    },
    addEventListener() {}
  },
  fetch: async url => {
    if (url.includes('/teacher/classes?courseId=2')) {
      return { ok: true, status: 200, statusText: 'OK', json: async () => ({ success: true, data: [{ id: 2, className: 'ClassSmokeA' }] }) };
    }
    if (url.includes('/teacher/classes/2/students')) {
      return { ok: true, status: 200, statusText: 'OK', json: async () => ({ success: true, data: [{ id: 42, name: 'Student Forty Two', username: 'student42' }] }) };
    }
    return { ok: true, status: 200, statusText: 'OK', json: async () => ({ success: true, data: [] }) };
  },
  showNotification(message, type) {
    notifications.push({ message, type });
  }
};

vm.createContext(context);
vm.runInContext(mainScript, context);

(async () => {
  await context.loadStudents('2');
  const optionTexts = studentSelect.appended.map(option => option.textContent);
  if (!optionTexts.includes('Student Forty Two (42)')) {
    throw new Error(`student dropdown did not render course students. Options: ${optionTexts.join(', ')}`);
  }
  console.log('teacher warning student dropdown contract OK');
})();
