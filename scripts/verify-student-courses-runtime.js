const fs = require('fs');
const path = require('path');
const { chromium } = require(path.resolve(__dirname, '../major_assignment/node_modules/playwright'));

const repoRoot = path.resolve(__dirname, '..');
const studentSessionFile = process.argv[2]
  ? path.resolve(process.argv[2])
  : path.resolve(repoRoot, '.runtime-logs/student-session-polish-fresh.json');

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf8').replace(/^\uFEFF/, ''));
}

function decodeJwtPayload(token) {
  if (!token || typeof token !== 'string') {
    return null;
  }
  const parts = token.split('.');
  if (parts.length < 2) {
    return null;
  }
  try {
    const normalized = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
    return JSON.parse(Buffer.from(padded, 'base64').toString('utf8'));
  } catch (error) {
    return null;
  }
}

function assertFreshSession(storageState, label) {
  const token = storageState?.token;
  const payload = decodeJwtPayload(token);
  if (!payload || typeof payload.exp !== 'number') {
    return;
  }
  const nowSeconds = Math.floor(Date.now() / 1000);
  if (payload.exp <= nowSeconds + 30) {
    const expiredAt = new Date(payload.exp * 1000).toISOString();
    throw new Error(`${label} token expired or near expiry: ${expiredAt}`);
  }
}

function requireRole(session, expectedRole, label) {
  const roles = session.user?.roles || [];
  const activeRole = session.activeRole || session.user?.activeRole || session.sessionStorage?.activeRole || session.sessionStorage?.role;
  if (activeRole !== expectedRole && !roles.includes(expectedRole)) {
    throw new Error(`${label} must use a ${expectedRole} session file; got ${activeRole || roles.join(',') || 'unknown'}`);
  }
}

function assert(condition, message, details) {
  if (!condition) {
    const error = new Error(message);
    if (details !== undefined) {
      error.details = details;
    }
    throw error;
  }
}

async function createAuthedPage(context, storage, url) {
  const page = await context.newPage();
  await page.addInitScript(sessionStorageState => {
    Object.entries(sessionStorageState).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        sessionStorage.setItem(key, String(value));
      }
    });
  }, storage);
  await page.goto(url, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(3500);
  return page;
}

(async () => {
  const studentSession = readJson(studentSessionFile);
  assertFreshSession(studentSession.sessionStorage, 'studentSessionFile');
  requireRole(studentSession, 'STUDENT', 'studentSessionFile');

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  let page;

  try {
    page = await createAuthedPage(
      context,
      studentSession.sessionStorage || {},
      'http://localhost:5500/student-courses.html'
    );

    const responses = [];
    page.on('response', response => {
      if (response.url().includes('/api/student/courses')) {
        responses.push({
          url: response.url(),
          status: response.status()
        });
      }
    });

    await page.waitForFunction(() => {
      const categorySelect = document.getElementById('courseCategory');
      const statusSelect = document.getElementById('courseStatus');
      return categorySelect && statusSelect && categorySelect.options.length > 1 && statusSelect.options.length > 1;
    }, { timeout: 15000 });

    const initialState = await page.evaluate(() => ({
      categoryOptions: Array.from(document.getElementById('courseCategory')?.options || []).map(option => ({
        value: option.value,
        text: option.textContent.trim()
      })),
      statusOptions: Array.from(document.getElementById('courseStatus')?.options || []).map(option => ({
        value: option.value,
        text: option.textContent.trim()
      }))
    }));

    await page.selectOption('#courseCategory', 'practice');
    await page.waitForTimeout(2500);
    await page.selectOption('#courseStatus', 'active');
    await page.waitForTimeout(2500);

    const filteredState = await page.evaluate(() => ({
      cards: Array.from(document.querySelectorAll('#coursesGrid .course-card')).map(card => card.innerText),
      noCourses: document.querySelector('#coursesGrid .no-courses')?.innerText || ''
    }));

    assert(
      initialState.categoryOptions.some(option => option.value === 'practice'),
      'student-courses category filter should expose backend-safe code values like practice',
      initialState.categoryOptions
    );

    assert(
      initialState.statusOptions.some(option => option.value === 'active'),
      'student-courses status filter should expose backend-safe code values like active',
      initialState.statusOptions
    );

    assert(
      responses.some(item => item.status === 200 && item.url.includes('courseCategory=practice')),
      'student-courses should request /api/student/courses with courseCategory=practice when selecting the practice filter',
      responses
    );

    assert(
      responses.some(item => item.status === 200 && item.url.includes('courseStatus=active')),
      'student-courses should request /api/student/courses with courseStatus=active when selecting the active filter',
      responses
    );

    assert(
      filteredState.cards.some(text => text.includes('CourseSmokeA') || text.includes('practice') || text.includes('进行中')),
      'student-courses should still render a runtime practice/active course after applying code-valued filters',
      filteredState
    );

    console.log('student courses runtime verifier passed');
  } finally {
    if (page && !page.isClosed()) {
      await page.close().catch(() => {});
    }
    await context.close().catch(() => {});
    await browser.close().catch(() => {});
  }
})().catch(error => {
  console.error(error.message);
  if (error.details !== undefined) {
    console.error(JSON.stringify(error.details, null, 2));
  }
  process.exit(1);
});
