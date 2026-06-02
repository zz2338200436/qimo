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

function assertMatches(content, regex, message) {
  if (!regex.test(content)) {
    throw new Error(`${message} Missing pattern: ${regex}`);
  }
}

function assertNotMatches(content, regex, message) {
  if (regex.test(content)) {
    throw new Error(`${message} Found unexpected pattern: ${regex}`);
  }
}

function extractFunctionSegment(content, startMarker, endMarker) {
  const startIndex = content.indexOf(startMarker);
  if (startIndex === -1) {
    throw new Error(`Missing start marker: ${startMarker}`);
  }
  const endIndex = content.indexOf(endMarker, startIndex);
  if (endIndex === -1) {
    throw new Error(`Missing end marker: ${endMarker}`);
  }
  return content.slice(startIndex, endIndex);
}

const pageContent = fs.readFileSync('frontend/dist/student-dashboard.html', 'utf8');

[
  'id="courseCountMetaLabel"',
  'id="assignmentCountMetaLabel"',
  'id="examCountMetaLabel"',
  'id="progressPercentageMetaLabel"',
  'async function populateDashboardStudyTimeFromConnectedSources(studentAPI, chartData) {',
  'await populateDashboardStudyTimeFromConnectedSources(studentAPI, chartData);',
  'async function populateDashboardActivitiesFromConnectedSources(studentAPI, statsData) {',
  'await populateDashboardActivitiesFromConnectedSources(studentAPI, statsData);',
  'function showEmptyStateData() {',
  "console.log('显示空状态数据');",
  'const emptyStatsData = {',
  'const emptyChartData = {',
  'const emptyActivities = [];',
  "'当前课程总览'",
  "'当前待完成作业'",
  "'当前考试安排'",
  "'当前学习进度估算'",
  'const labelElement = document.getElementById(`${elementId}MetaLabel`);',
  'labelElement.textContent = labelText;'
].forEach(snippet => {
  assertIncludes(
    pageContent,
    snippet,
    'student dashboard contract mismatch.'
  );
});

assertMatches(
  pageContent,
  /async function populateDashboardFromConnectedSources\(studentAPI, statsData, chartData\) \{[\s\S]*?await populateDashboardStudyTimeFromConnectedSources\(studentAPI, chartData\);[\s\S]*?statsData\.courses = courseList\.length;/,
  'student dashboard should populate study-time data from the shared connected-sources helper before deriving connected-source stats.'
);

const connectedSourcesSegment = extractFunctionSegment(
  pageContent,
  'async function populateDashboardFromConnectedSources(studentAPI, statsData, chartData) {',
  'async function populateDashboardActivitiesFromConnectedSources(studentAPI, statsData) {'
);

assertIncludes(
  connectedSourcesSegment,
  'await populateDashboardStudyTimeFromConnectedSources(studentAPI, chartData);',
  'student dashboard connected-sources segment should hydrate study-time data through the shared helper.'
);

assertNotIncludes(
  connectedSourcesSegment,
  'chartData.studyTime = transformStudyTimeData([]);',
  'student dashboard connected-sources segment should not wipe study-time data back to an empty chart directly.'
);

[
  'id="courseCountChangeLabel"',
  'id="assignmentCountChangeLabel"',
  'id="examCountChangeLabel"',
  'id="progressPercentageChangeLabel"',
  'const changeLabelOverrides = {',
  'coursesChange:',
  'assignmentsChange:',
  'examsChange:',
  'progressChange:',
  'const labelElement = document.getElementById(`${elementId}ChangeLabel`);',
  'showMockData();',
  'function showMockData() {',
  "console.log('显示模拟数据');",
  '显示模拟数据，而不是空数据',
  'Math.random()',
  'data.courseCountChange',
  'data.pendingAssignmentsChange',
  'data.upcomingExamsChange',
  'data.overallProgressChange',
  "updateStatChange('courseCount', stats.coursesChange || 0, '较上月');",
  "updateStatChange('assignmentCount', stats.assignmentsChange || 0, '较上周');",
  "updateStatChange('examCount', stats.examsChange || 0, '较上周');",
  "updateStatChange('progressPercentage', stats.progressChange || 0, '较上月');",
  'changeElement.innerHTML = `<i class="fas ${icon}"></i> ${timePeriod}`;',
  "showMessage(activitiesResponse.message, 'info');\n                            updateRecentActivities([]);"
].forEach(snippet => {
  assertNotIncludes(
    pageContent,
    snippet,
    'student dashboard should not use random deltas or render placeholder change fields as time comparisons.'
  );
});

console.log('student dashboard contract OK');
