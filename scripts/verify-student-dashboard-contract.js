const fs = require('fs');
const path = require('path');

const dashboardPath = path.join(__dirname, '..', 'frontend', 'dist', 'student-dashboard.html');
const content = fs.readFileSync(dashboardPath, 'utf8');
const apiJsPath = path.join(__dirname, '..', 'frontend', 'dist', 'api.js');
const apiContent = fs.readFileSync(apiJsPath, 'utf8');

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

assert(
  /function normalizeStudentCourseProgress\s*\(/.test(content),
  'student-dashboard.html 应定义 normalizeStudentCourseProgress()，避免直接依赖不存在的 course.progress 字段。'
);

assert(
  /function buildCourseProgressChartData\s*\(/.test(content),
  'student-dashboard.html 应定义 buildCourseProgressChartData()，统一生成课程进度图数据。'
);

assert(
  /buildCourseProgressChartData\(courseList\)/.test(content),
  'student-dashboard.html 应在课程列表 fallback 分支里复用 buildCourseProgressChartData(courseList)。'
);

assert(
  !content.includes('chartData.courseProgress = courseList.map(course => course.progress || 0);'),
  'student-dashboard.html 仍在直接读取 course.progress || 0，这会让当前微服务课程数据全部显示为 0。'
);

assert(
  /function populateDashboardFromConnectedSources\s*\(/.test(content),
  'student-dashboard.html 应定义 populateDashboardFromConnectedSources()，在综合表现接口 unsupported 时继续走已接通的数据源。'
);

assert(
  /await populateDashboardFromConnectedSources\(studentAPI,\s*statsData,\s*chartData\)/.test(content),
  'student-dashboard.html 应在综合表现接口 unavailable / unsupported 时调用 populateDashboardFromConnectedSources(studentAPI, statsData, chartData)。'
);

assert(
  /updateRecentActivities\(activities\.slice\(0,\s*5\)\)/.test(content),
  'student-dashboard.html 的 connected sources fallback 应补齐最近活动，而不是只刷新统计卡片。'
);

assert(
  /function isJwtLikelyExpired\s*\(/.test(apiContent),
  'api.js 应定义 isJwtLikelyExpired()，用于请求前预判过期 token。'
);

assert(
  /await tryRefreshAuthSession\(\);/.test(apiContent),
  'api.js 应在请求前对即将过期的 JWT 执行预刷新，减少控制台 401 噪音。'
);

console.log('student-dashboard contract OK');
