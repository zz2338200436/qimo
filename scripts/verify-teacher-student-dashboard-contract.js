const fs = require('node:fs');

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

function assertMatches(content, pattern, message) {
  if (!pattern.test(content)) {
    throw new Error(message);
  }
}

function sliceBetween(content, startNeedle, endNeedle) {
  const start = content.indexOf(startNeedle);
  if (start === -1) {
    throw new Error(`Missing start marker: ${startNeedle}`);
  }
  const end = content.indexOf(endNeedle, start);
  if (end === -1) {
    throw new Error(`Missing end marker: ${endNeedle}`);
  }
  return content.slice(start, end);
}

const pageContent = fs.readFileSync('frontend/dist/teacher-student-dashboard.html', 'utf8');

[
  'function clearScoreTrendEmptyState(chartContainer) {',
  'function renderScoreTrendEmptyState(chartContainer, message) {',
  "message: '当前筛选下暂无成绩趋势数据'",
  "message: '成绩趋势数据暂不可用，请稍后重试'",
  "className = 'chart-empty-state score-trend-empty-state';",
  'let totalStudentRows = 0;',
  'let totalUniqueStudents = 0;',
  'function countUniqueStudents(studentRows) {',
  'function buildStudentTableTitle(label, uniqueStudentCount, rowCount) {',
  'function updateStudentTableTitle(label, uniqueStudentCount, rowCount) {',
  "function syncScoreTrendTimeRangeControls(timeRange = 'week') {",
  "const timeRangeLabelMap = {",
  "week: '周',",
  "month: '月',",
  "quarter: '三个月',",
  "semester: '学期'",
  "button.classList.toggle('active', button.textContent.trim() === activeLabel);",
  '<div class="chart-card" data-chart-kind="score-distribution">',
  '<div class="chart-card" data-chart-kind="score-trend">',
  '<h3 class="chart-title">当前筛选平均成绩趋势</h3>',
  '>三个月</button>',
  'function findStudentRow(studentId, courseId = \'\', courseName = \'\', className = \'\') {',
  '<h3 class="chart-title">成绩分布（课程记录）</h3>',
  '<th scope="row">当前课程</th>',
  '<td id="detailCourseName"></td>',
  'totalUniqueStudents = Number.isFinite(Number(summary.totalStudents))',
  'totalStudentRows = performances.length;',
  "updateStudentTableTitle('学生学习数据详情', totalUniqueStudents, totalStudentRows);",
  'totalUniqueStudents = countUniqueStudents(filteredStudents);',
  'totalStudentRows = filteredStudents.length;',
  "buildStudentTableTitle(`分数段：${scoreRange}`, totalUniqueStudents, totalStudentRows)",
  '条课程记录',
  'data-course-name="${courseName}"',
  'data-course-id="${studentCourseId}"',
  "'课程': perf.courseName || '未知课程'",
  '{ wch: 20 }, // 课程',
  'const cachedStudent = findStudentRow(studentId, fallbackCourseId, fallbackCourseName, fallbackClassName);',
  'const rowScopedStudentData = cachedStudent ? {',
  'studentData = rowScopedStudentData;',
  'let resolvedCourseName = \'\';',
  'if (!resolvedCourseName && fallbackCourseName) {',
  'resolvedCourseName = fallbackCourseName;',
  'const trendParams = { studentId };',
  'trendParams.courseId = effectiveCourseId;',
  'const effectiveCourseId = fallbackCourseId || cachedStudent?.courseId || studentData?.courseId || \'\';',
  'recentScoreLabels = trendPoints.map((point, index) => point.date || `第${index + 1}次`);',
  'showStudentDetail(studentId, studentName, className, courseName, courseId);',
  'editStudent(studentId, studentName, courseId, courseName, className);',
  'const selectedCourseId = document.getElementById(\'courseSelect\').value;',
  'if (selectedCourseId !== \'all\') {',
  'params.courseId = selectedCourseId;',
  'editForm.dataset.courseId = courseId || \'\';',
  'editForm.dataset.courseName = courseName || \'\';',
  'editForm.dataset.className = className || \'\';',
  'const courseId = form.dataset.courseId || \'\';',
  'const courseName = form.dataset.courseName || \'\';',
  'const className = form.dataset.className || \'\';',
  '&& (!courseId || String(student?.courseId || \'\').trim() === String(courseId).trim())',
  'currentStudentData = currentStudentData.map(student => {',
  'if (String(student?.studentId) !== String(studentId)) {',
  'console.log(\'已批量更新本地学生数据数组中的学生信息\');',
  'const performances = summaryResponse?.data?.studentPerformances || [];',
  '<td>${resolvedCourseName || \'未知课程\'}</td>',
  "label: '课程记录数'",
  "return `${label}: ${value}条记录 (${percentage}%)`;",
  "text: '课程记录数'",
  "label: '当前筛选平均成绩'",
  "return `当前筛选平均成绩: ${value}分`;",
  "const overallProgress = Number.isFinite(Number(dashboardData.overallProgress))",
  "document.getElementById('learningProgress').textContent = overallProgress + '%';",
  'id="studentCountMeta"',
  'const dashboardStatMetaText = {',
  "studentCount: '当前筛选学生统计'",
  "averageScore: '基于当前筛选结果'",
  "assignmentCompletionRate: '基于最近提交活跃度'",
  "learningProgress: '基于当前筛选学习进度'",
  'function applyDashboardStatMetaText() {',
  "if (timeRange === 'week' || timeRange === 'month' || timeRange === 'quarter') {",
  "isEmpty: true,",
  'initScoreTrendChart(timeRange)',
  "const timeRange = document.getElementById('timeRangeSelect').value;",
  'await initDashboard(classId, courseId, timeRange);'
].forEach(snippet => {
  assertIncludes(
    pageContent,
    snippet,
    'teacher student dashboard contract mismatch.'
  );
});

const scoreRangeFunctionBlock = sliceBetween(
  pageContent,
  'async function showStudentsByScoreRange(scoreRange, color) {',
  'let scoreDistributionChart = null;'
);

assertIncludes(
  scoreRangeFunctionBlock,
  "const timeRange = document.getElementById('timeRangeSelect').value;",
  'teacher student dashboard score-range drilldown should read current timeRange.'
);

assertIncludes(
  scoreRangeFunctionBlock,
  "params.append('timeRange', timeRange);",
  'teacher student dashboard score-range drilldown should preserve timeRange context.'
);

const searchButtonBlock = sliceBetween(
  pageContent,
  "if (searchBtn) {",
  "if (resetBtn) {"
);

assertIncludes(
  searchButtonBlock,
  'await initScoreTrendChart(timeRange);',
  'teacher student dashboard search flow should re-render trend chart with timeRange.'
);

const resetButtonBlock = sliceBetween(
  pageContent,
  "if (resetBtn) {",
  "// 图表类型切换"
);

assertIncludes(
  resetButtonBlock,
  'await initScoreTrendChart(timeRange);',
  'teacher student dashboard reset flow should re-render trend chart with timeRange.'
);

const resetFiltersBlock = sliceBetween(
  pageContent,
  'function resetFilters() {',
  '// 初始化班级和课程列表'
);

assertIncludes(
  resetFiltersBlock,
  "syncScoreTrendTimeRangeControls('week');",
  'teacher student dashboard resetFilters should also reset trend button active state.'
);

const chartToggleBlock = sliceBetween(
  pageContent,
  "document.querySelectorAll('.chart-card-actions button').forEach(button => {",
  "// 初始化图表"
);

assertIncludes(
  chartToggleBlock,
  "const chartKind = chartCard?.dataset?.chartKind || '';",
  'teacher student dashboard chart toggle should branch on stable chartKind metadata.'
);

assertIncludes(
  chartToggleBlock,
  "if (chartKind === 'score-distribution') {",
  'teacher student dashboard chart toggle should handle score distribution by chartKind.'
);

assertIncludes(
  chartToggleBlock,
  "} else if (chartKind === 'score-trend') {",
  'teacher student dashboard chart toggle should handle score trend by chartKind.'
);

assertIncludes(
  chartToggleBlock,
  "const timeRangeText = this.textContent;",
  'teacher student dashboard trend toggle should derive timeRange from the clicked trend button.'
);

assertIncludes(
  chartToggleBlock,
  "const timeRange = timeRangeMap[timeRangeText] || 'week';",
  'teacher student dashboard trend toggle should map button text to timeRange.'
);

assertIncludes(
  chartToggleBlock,
  "'三个月': 'quarter'",
  'teacher student dashboard trend toggle should support quarter button mapping.'
);

assertIncludes(
  chartToggleBlock,
  'syncScoreTrendTimeRangeControls(timeRange);',
  'teacher student dashboard trend toggle should sync shared timeRange controls.'
);

assertIncludes(
  pageContent,
  "timeRangeSelect.addEventListener('change', function() {",
  'teacher student dashboard timeRange select should sync trend buttons.'
);

assertIncludes(
  pageContent,
  'syncScoreTrendTimeRangeControls(this.value);',
  'teacher student dashboard timeRange select should drive trend button active state.'
);

[
  "console.warn('成绩趋势为空，使用默认占位数据');",
  "showNotification('获取成绩趋势数据失败，已显示占位数据', 'warning');",
  'return getDefaultTrendData(timeRange);',
  'function getDefaultTrendData(timeRange) {',
  '当前环境未开放该页面所需数据，已显示空结果',
  'id="studentCountChange"',
  'stat-change positive',
  'stat-change negative',
  "document.getElementById('studentCountMeta').textContent = '当前筛选学生统计';",
  "document.getElementById('averageScoreMeta').textContent = '基于当前筛选结果';",
  "document.getElementById('assignmentCompletionRateMeta').textContent = '基于最近提交活跃度';",
  "document.getElementById('learningProgressMeta').textContent = '基于当前筛选学习进度';",
  "document.getElementById('averageScoreChange').textContent = '暂无变化数据';",
  "document.getElementById('assignmentCompletionRateChange').textContent = '暂无变化数据';",
  "document.getElementById('learningProgressChange').textContent = '暂无变化数据';",
  "document.getElementById('studentCountChange').textContent = '当前筛选学生统计';",
  "document.getElementById('averageScoreChange').textContent = '基于当前筛选结果';",
  "document.getElementById('assignmentCompletionRateChange').textContent = '基于最近提交活跃度';",
  "document.getElementById('learningProgressChange').textContent = '基于当前筛选学习进度';"
].forEach(snippet => {
  assertNotIncludes(
    pageContent,
    snippet,
    'teacher student dashboard should not retain change-style stat shells or pseudo-delta copy.'
  );
});

[
  'id="studentCountMeta"',
  'id="averageScoreMeta"',
  'id="assignmentCompletionRateMeta"',
  'id="learningProgressMeta"',
  'applyDashboardStatMetaText();'
].forEach(snippet => {
  assertIncludes(
    pageContent,
    snippet,
    'teacher student dashboard should expose neutral stat metadata rows instead of change rows.'
  );
});

[
  'function getStudentTableFallbackMessage(error) {',
  "return '登录已过期，正在跳转到登录页';",
  "return '当前账号暂无权限查看学生学习数据';",
  "const fallbackMessage = getStudentTableFallbackMessage(error);"
].forEach(snippet => {
  assertIncludes(
    pageContent,
    snippet,
    'teacher student dashboard should distinguish access-denied table fallback from empty-result copy.'
  );
});

assertIncludes(
  pageContent,
  "const timeRange = document.getElementById('timeRangeSelect').value;",
  'teacher student dashboard initCharts should derive timeRange from the shared select.'
);

assertNotIncludes(
  pageContent,
  "const overallProgress = averageScore;",
  'teacher student dashboard should not derive learningProgress from averageScore fallback anymore.'
);

assertNotIncludes(
  pageContent,
  "const timeRangeText = activeScoreTrendButton ? activeScoreTrendButton.textContent : '周';",
  'teacher student dashboard should not derive initCharts timeRange from stale active trend buttons.'
);

[
  'totalStudents = performances.length;',
  'totalStudents = filteredStudents.length;',
  '(${filteredStudents.length}名学生)',
  '+5 人 (2.8%)',
  '+3.2 分 (4.0%)',
  '-1.5% (1.6%)',
  '+5.8% (7.8%)',
  "formatStatChange(dashboardData.totalStudentsChange, '人')"
].forEach(snippet => {
  assertNotIncludes(
    pageContent,
    snippet,
    'teacher student dashboard should not treat course-row count as student count.'
  );
});

console.log('teacher student dashboard contract OK');
