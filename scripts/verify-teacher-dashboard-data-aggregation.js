const fs = require('node:fs');
const vm = require('node:vm');

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

const apiSource = fs.readFileSync('frontend/dist/api.js', 'utf8');
const noop = () => {};
const documentStub = {
  cookie: '',
  addEventListener: noop,
  querySelectorAll: () => [],
  getElementById: () => null
};
const storageStub = {
  getItem: () => null,
  setItem: noop,
  removeItem: noop
};
const windowStub = {
  location: { pathname: '/teacher-dashboard.html' },
  sessionStorage: storageStub,
  localStorage: storageStub,
  addEventListener: noop
};

const context = {
  console,
  setTimeout,
  clearTimeout,
  document: documentStub,
  window: windowStub,
  localStorage: storageStub,
  Event: function Event() {},
  atob: value => Buffer.from(value, 'base64').toString('binary'),
  fetch: async () => {
    throw new Error('fetch disabled in dashboard aggregation contract');
  }
};
windowStub.window = windowStub;
windowStub.document = documentStub;
context.globalThis = context;

vm.runInNewContext(apiSource, context, { filename: 'frontend/dist/api.js' });

assert(
  typeof context.buildTeacherSubmissionTrend === 'function',
  'buildTeacherSubmissionTrend should be available for aggregation verification.'
);
assert(
  typeof context.buildTeacherCourseAverageScores === 'function',
  'buildTeacherCourseAverageScores should derive chart scores from connected submission data.'
);

const assignments = [
  {
    id: 101,
    title: '单元测验一',
    courseId: 201,
    courseName: '分布式系统',
    publishDate: '2026-05-10 08:00:00',
    submittedCount: 3,
    totalStudents: 4
  },
  {
    id: 102,
    title: '单元测验二',
    courseId: 201,
    courseName: '分布式系统',
    publishDate: '2026-05-18 08:00:00',
    submittedCount: 4,
    totalStudents: 4
  },
  {
    id: 103,
    title: '实验报告',
    courseId: 202,
    courseName: '云计算技术',
    publishDate: '2026-05-25 08:00:00',
    submittedCount: 2,
    totalStudents: 5
  }
];

const submissions = [
  {
    assignmentId: 101,
    courseId: 201,
    courseName: '分布式系统',
    score: 90,
    maxScore: 100,
    status: 'graded',
    graded: true,
    submissionDate: '2026-05-11 09:00:00'
  },
  {
    assignmentId: 102,
    courseId: 201,
    courseName: '分布式系统',
    score: 80,
    maxScore: 100,
    status: 'graded',
    graded: true,
    submissionDate: '2026-05-19 09:00:00'
  },
  {
    assignmentId: 103,
    courseId: 202,
    courseName: '云计算技术',
    score: 45,
    maxScore: 50,
    status: 'graded',
    graded: true,
    submissionDate: '2026-05-26 09:00:00'
  }
];

const trend = context.buildTeacherSubmissionTrend(assignments, submissions);
assert(
  trend.submissionRateDays.length === 3,
  `submission trend should use assignment titles/dates when assignment-level counts are available. Got ${trend.submissionRateDays.length} points.`
);
assert(
  trend.submissionRates.join(',') === '75,100,40',
  `submission trend should derive rates from submittedCount / totalStudents. Got: ${trend.submissionRates.join(',')}`
);

const scores = context.buildTeacherCourseAverageScores(
  [
    { id: 201, courseName: '分布式系统' },
    { id: 202, courseName: '云计算技术' }
  ],
  submissions
);
assert(
  scores.join(',') === '85,90',
  `course average scores should be derived from graded submissions when course averageScore is absent. Got: ${scores.join(',')}`
);

const multiCourseScores = context.buildTeacherCourseAverageScores(
  [
    { id: 301, courseName: '分布式系统' },
    { id: 302, courseName: '云计算技术' },
    { id: 303, courseName: '数据库原理' },
    { id: 304, courseName: '软件测试' }
  ],
  [
    { courseId: 301, score: 88, graded: true },
    { courseId: 302, score: 76, graded: true },
    { courseId: 303, score: 91, graded: true },
    { courseId: 304, score: 83, graded: true }
  ]
);
assert(
  multiCourseScores.filter(value => Number(value) > 0).length >= 4,
  `course average chart should support at least four nonzero bars. Got: ${multiCourseScores.join(',')}`
);

console.log('teacher dashboard data aggregation OK');
