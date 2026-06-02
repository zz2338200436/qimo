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
  await page.waitForTimeout(2500);
  return page;
}

async function collectResponses(page, apiFragments, action) {
  const responses = [];
  const handler = response => {
    const url = response.url();
    if (apiFragments.some(fragment => url.includes(fragment))) {
      responses.push({ url, status: response.status() });
    }
  };
  page.on('response', handler);
  try {
    await action();
    await page.waitForTimeout(2500);
  } finally {
    page.off('response', handler);
  }
  return responses;
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
      'http://localhost:5500/student-assignments.html'
    );

    const examResponses = await collectResponses(
      page,
      ['/api/student/exams'],
      async () => {
        await page.click('.tab-btn[data-tab="exams"]');
        await page.waitForFunction(() => {
          const examTab = document.getElementById('exams');
          return examTab && examTab.classList.contains('active');
        }, { timeout: 10000 });
      }
    );

    assert(
      examResponses.some(item => item.status === 200 && item.url.includes('/api/student/exams')),
      'student-assignments exams tab should request /api/student/exams successfully',
      examResponses
    );

    const examState = await page.evaluate(() => ({
      examHeader: document.querySelector('#exams h4')?.textContent?.trim() || '',
      examItems: Array.from(document.querySelectorAll('#exam-list .exam-item')).length,
      errorText: document.getElementById('exam-list')?.innerText || ''
    }));

    assert(
      examState.examHeader.includes('考试列表'),
      'student-assignments exams tab should render the exam list header',
      examState
    );
    assert(
      examState.examItems > 0 && !examState.errorText.includes('获取考试数据失败'),
      'student-assignments exams tab should render real exam items instead of an error state',
      examState
    );

    const scoreResponses = await collectResponses(
      page,
      ['/api/student/scores'],
      async () => {
        await page.click('.tab-btn[data-tab="scores"]');
        await page.waitForFunction(() => {
          const scoreTab = document.getElementById('scores');
          return scoreTab && scoreTab.classList.contains('active');
        }, { timeout: 10000 });
      }
    );

    assert(
      scoreResponses.some(item => item.status === 200 && item.url.includes('/api/student/scores')),
      'student-assignments scores tab should request /api/student/scores successfully',
      scoreResponses
    );

    const scoreState = await page.evaluate(() => ({
      rows: Array.from(document.querySelectorAll('#score-list tr')).map(row => row.innerText.trim()).filter(Boolean),
      filterOptions: Array.from(document.querySelectorAll('#score-search option')).map(option => option.textContent.trim())
    }));

    assert(
      scoreState.rows.length > 0 && !scoreState.rows.some(text => text.includes('获取成绩数据失败')),
      'student-assignments scores tab should render real score rows instead of an error state',
      scoreState
    );
    assert(
      scoreState.filterOptions.length > 1,
      'student-assignments scores tab should populate course filter options from real score data',
      scoreState
    );

    console.log('student assignments runtime verifier passed');
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
