const fs = require('fs');
const path = require('path');
const { chromium } = require(path.resolve(__dirname, '../major_assignment/node_modules/playwright'));

const repoRoot = path.resolve(__dirname, '..');
const teacherSessionFile = process.argv[2]
  ? path.resolve(process.argv[2])
  : path.resolve(repoRoot, '.runtime-logs/teacher-session-polish-fresh.json');

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
  await page.waitForTimeout(2500);
  return page;
}

(async () => {
  const teacherSession = readJson(teacherSessionFile);
  assertFreshSession(teacherSession.sessionStorage, 'teacherSessionFile');
  requireRole(teacherSession, 'TEACHER', 'teacherSessionFile');

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  let page;

  const intercepted = [];

  try {
    await context.route('**/api/teacher/course-assignments**', async route => {
      intercepted.push(route.request().url());
      await route.fulfill({
        status: 503,
        contentType: 'application/json',
        body: JSON.stringify({
          success: false,
          message: '课程分配服务暂不可用',
          code: 503
        })
      });
    });

    page = await createAuthedPage(
      context,
      teacherSession.sessionStorage || {},
      'http://localhost:5500/teacher-courses.html'
    );

    await page.click('.tab-item[data-tab="assignments"]');

    await page.waitForFunction(() => {
      const loading = document.getElementById('assignments-loading');
      return loading && loading.style.display === 'none';
    }, { timeout: 15000 });

    const state = await page.evaluate(() => ({
      noResultsText: document.getElementById('assignments-no-results')?.innerText || '',
      noResultsDisplay: document.getElementById('assignments-no-results')?.style.display || '',
      errorDisplay: document.getElementById('assignments-error')?.style.display || '',
      errorMessage: document.getElementById('assignments-error-message')?.innerText || ''
    }));

    assert(
      intercepted.length > 0,
      'teacher-courses assignments tab should request /api/teacher/course-assignments',
      intercepted
    );

    assert(
      state.errorDisplay !== 'none' && state.errorMessage.includes('课程分配服务暂不可用'),
      'teacher-courses assignments tab should surface a real error-state when course assignment API fails',
      state
    );

    assert(
      !state.noResultsText.includes('当前联调环境的课程分配列表暂不可用，不影响课程新增与课程列表使用。'),
      'teacher-courses assignments tab should not render the legacy degraded unavailable copy',
      state
    );

    console.log('teacher courses assignments runtime verifier passed');
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
