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

async function waitForMajorApiResponse(responses) {
  const startedAt = Date.now();
  while (Date.now() - startedAt < 10000) {
    const hit = responses.find(item => item.url.includes('/api/teacher/majors'));
    if (hit) {
      return hit;
    }
    await new Promise(resolve => setTimeout(resolve, 200));
  }
  return null;
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

    const responses = [];
    page.on('response', response => {
      responses.push({
        url: response.url(),
        status: response.status()
      });
    });

    await page.evaluate(async () => {
      if (typeof window.ensureClassMetadataLoaded === 'function') {
        await window.ensureClassMetadataLoaded(true);
      }
    });

    await page.waitForFunction(() => {
      const select = document.getElementById('class-major');
      return select && select.options.length > 1;
    }, { timeout: 15000 });

    const initialMetadataState = await page.evaluate(() => ({
      majorOptions: Array.from(document.getElementById('class-major')?.options || []).map(option => ({
        value: option.value,
        text: option.textContent.trim()
      })),
      gradeOptions: Array.from(document.getElementById('class-grade')?.options || []).map(option => ({
        value: option.value,
        text: option.textContent.trim()
      }))
    }));

    const majorApiHit = await waitForMajorApiResponse(responses);
    assert(
      majorApiHit && majorApiHit.status === 200,
      'teacher-courses page should request /api/teacher/majors successfully during metadata preload',
      responses
    );

    assert(
      initialMetadataState.majorOptions.length > 1,
      'teacher-courses search major dropdown should be populated before opening class modals',
      initialMetadataState
    );

    await page.click('[data-bs-target="#addClassModal"]');
    await page.waitForTimeout(1200);

    const modalState = await page.evaluate(() => ({
      addClassMajorOptions: Array.from(document.getElementById('add-class-major')?.options || []).map(option => ({
        value: option.value,
        text: option.textContent.trim()
      })),
      addClassGradeOptions: Array.from(document.getElementById('add-class-grade')?.options || []).map(option => ({
        value: option.value,
        text: option.textContent.trim()
      }))
    }));

    assert(
      modalState.addClassMajorOptions.length > 1,
      'add-class modal should have populated major options from real metadata',
      modalState
    );

    assert(
      modalState.addClassGradeOptions.length > 1,
      'add-class modal should have populated grade options from metadata preload',
      modalState
    );

    console.log('teacher courses metadata runtime verifier passed');
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
