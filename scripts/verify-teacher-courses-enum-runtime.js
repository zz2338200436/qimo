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
  await page.waitForTimeout(3500);
  return page;
}

(async () => {
  const teacherSession = readJson(teacherSessionFile);
  assertFreshSession(teacherSession.sessionStorage, 'teacherSessionFile');
  requireRole(teacherSession, 'TEACHER', 'teacherSessionFile');

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  let page;

  try {
    page = await createAuthedPage(
      context,
      teacherSession.sessionStorage || {},
      'http://localhost:5500/teacher-courses.html'
    );

    await page.waitForFunction(() => {
      const categorySelect = document.getElementById('course-category');
      const statusSelect = document.getElementById('course-status');
      return categorySelect && statusSelect && categorySelect.options.length > 1 && statusSelect.options.length > 1;
    }, { timeout: 15000 });

    const enumState = await page.evaluate(async () => {
      const getOptions = id => Array.from(document.getElementById(id)?.options || []).map(option => ({
        value: option.value,
        text: option.textContent.trim()
      }));

      const categorySelect = document.getElementById('course-category');
      const statusSelect = document.getElementById('course-status');

      categorySelect.value = 'practice';
      statusSelect.value = 'active';
      await window.loadCourses(1, { ...window.getCourseSearchParams(1).apiParams });

      const practiceRows = Array.from(document.querySelectorAll('#courses-content tbody tr'))
        .map(row => row.innerText)
        .filter(text => text.includes('practice') || text.includes('CourseNoStudent') || text.includes('CourseSmokeA'));

      await window.editCourse(4, { readOnly: true });
      const editCategory = document.getElementById('edit-course-category');
      const editStatus = document.getElementById('edit-course-status');

      return {
        searchCategoryOptions: getOptions('course-category'),
        searchStatusOptions: getOptions('course-status'),
        editCategoryValue: editCategory?.value || null,
        editStatusValue: editStatus?.value || null,
        editCategoryOptions: getOptions('edit-course-category'),
        editStatusOptions: getOptions('edit-course-status'),
        practiceRows
      };
    });

    assert(
      enumState.searchCategoryOptions.some(option => option.value === 'practice'),
      'teacher-courses search category select should expose backend-safe code values like practice',
      enumState.searchCategoryOptions
    );

    assert(
      enumState.searchStatusOptions.some(option => option.value === 'active'),
      'teacher-courses search status select should expose backend-safe code values like active',
      enumState.searchStatusOptions
    );

    assert(
      enumState.practiceRows.length > 0,
      'teacher-courses should be able to filter runtime practice courses via the search select value',
      enumState
    );

    assert(
      enumState.editCategoryValue === 'practice',
      'teacher-courses edit modal should preserve code-valued courseCategory rows like practice',
      enumState
    );

    assert(
      enumState.editStatusValue === 'active',
      'teacher-courses edit modal should preserve code-valued courseStatus rows like active',
      enumState
    );

    console.log('teacher courses enum runtime verifier passed');
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
