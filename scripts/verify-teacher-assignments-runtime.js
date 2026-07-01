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
  await page.waitForTimeout(3000);
  return page;
}

(async () => {
  const teacherSession = readJson(teacherSessionFile);
  assertFreshSession(teacherSession.sessionStorage, 'teacherSessionFile');
  requireRole(teacherSession, 'TEACHER', 'teacherSessionFile');

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  let page;
  const courseResponses = [];

  try {
    page = await createAuthedPage(
      context,
      teacherSession.sessionStorage || {},
      'http://localhost:5500/teacher-assignments.html'
    );

    page.on('response', response => {
      if (response.url().includes('/api/teacher/courses')) {
        courseResponses.push({
          url: response.url(),
          status: response.status()
        });
      }
    });

    await page.evaluate(async () => {
      if (typeof window.loadTeacherAssignmentCourses === 'function') {
        await window.loadTeacherAssignmentCourses();
      }
    });

    await page.waitForFunction(() => {
      const select = document.getElementById('assignment-course');
      return select && select.options.length > 1;
    }, { timeout: 15000 });

    const state = await page.evaluate(() => ({
      assignmentCourseOptions: Array.from(document.getElementById('assignment-course')?.options || []).map(option => ({
        value: option.value,
        text: option.textContent.trim()
      })),
      examCourseOptions: Array.from(document.getElementById('exam-course-search')?.options || []).map(option => ({
        value: option.value,
        text: option.textContent.trim()
      }))
    }));

    assert(
      courseResponses.some(item => item.status === 200),
      'teacher-assignments page should request /api/teacher/courses successfully for real course dropdown data',
      courseResponses
    );

    assert(
      !state.assignmentCourseOptions.some(option => option.text === 'Python编程基础' && option.value === ''),
      'teacher-assignments assignment course filter should not be prefilled with legacy mock course placeholders',
      state
    );

    assert(
      state.assignmentCourseOptions.length > 1 && state.examCourseOptions.length > 1,
      'teacher-assignments page should populate course filters from real teacher courses',
      state
    );

    console.log('teacher assignments runtime verifier passed');
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
