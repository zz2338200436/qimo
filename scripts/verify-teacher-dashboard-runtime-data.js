const fs = require('node:fs');
const vm = require('node:vm');

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

const sessionFile = process.argv[2];
if (!sessionFile) {
  throw new Error('Usage: node scripts/verify-teacher-dashboard-runtime-data.js <teacher-session.json>');
}

const apiSource = fs.readFileSync('frontend/dist/api.js', 'utf8');
const session = JSON.parse(fs.readFileSync(sessionFile, 'utf8'));
const noop = () => {};
const storage = new Map([
  ['token', session.accessToken],
  ['refreshToken', session.refreshToken],
  ['user', JSON.stringify(session.user)],
  ['userId', String(session.user?.id ?? '')],
  ['activeRole', 'TEACHER'],
  ['role', 'TEACHER']
]);

const storageStub = {
  getItem: key => storage.get(key) ?? null,
  setItem: (key, value) => storage.set(key, String(value)),
  removeItem: key => storage.delete(key)
};
const documentStub = {
  cookie: '',
  addEventListener: noop,
  querySelectorAll: () => [],
  getElementById: () => null
};
const windowStub = {
  location: { pathname: '/teacher-dashboard.html' },
  sessionStorage: storageStub,
  localStorage: storageStub,
  addEventListener: noop
};

const fetchWithGatewayBase = (url, options) => {
  const resolvedUrl = String(url).startsWith('/')
    ? `http://localhost:8080${url}`
    : url;
  return fetch(resolvedUrl, options);
};

const context = {
  console: { ...console, log: noop },
  setTimeout,
  clearTimeout,
  document: documentStub,
  window: windowStub,
  localStorage: storageStub,
  Event: function Event() {},
  URLSearchParams,
  URL,
  Headers,
  Request,
  Response,
  FormData,
  Blob,
  atob: value => Buffer.from(value, 'base64').toString('binary'),
  fetch: fetchWithGatewayBase
};
windowStub.window = windowStub;
windowStub.document = documentStub;
context.globalThis = context;

vm.runInNewContext(apiSource, context, { filename: 'frontend/dist/api.js' });

(async () => {
  const snapshot = await context.buildTeacherDashboardSnapshot({ page: 1, size: 100 });
  const nonzeroAverageScores = snapshot.averageScores.filter(value => Number(value) > 0);
  const nonzeroSubmissionRates = snapshot.submissionRates.filter(value => Number(value) > 0);

  assert(
    nonzeroAverageScores.length >= 4,
    `teacher dashboard should expose at least four nonzero average-score bars. Got ${nonzeroAverageScores.length}: ${snapshot.averageScores.join(',')}`
  );
  assert(
    nonzeroSubmissionRates.length > 0,
    `teacher dashboard should expose nonzero submission-rate data. Got: ${snapshot.submissionRates.join(',')}`
  );

  console.log(JSON.stringify({
    nonzeroAverageScores: nonzeroAverageScores.length,
    averageScores: snapshot.averageScores,
    submissionRates: snapshot.submissionRates
  }));
})().catch(error => {
  console.error(error.message);
  process.exitCode = 1;
});
