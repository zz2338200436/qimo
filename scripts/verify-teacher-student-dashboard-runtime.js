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

async function runStep(results, name, action) {
  try {
    await action();
    results.push({ name, ok: true });
    console.log(`[PASS] ${name}`);
  } catch (error) {
    results.push({ name, ok: false, error: error.message, details: error.details });
    console.error(`[FAIL] ${name}`);
    console.error(`  ${error.message}`);
    if (error.details !== undefined) {
      console.error(`  details: ${JSON.stringify(error.details)}`);
    }
    throw error;
  }
}

(async () => {
  const teacherSession = readJson(teacherSessionFile);
  assertFreshSession(teacherSession.sessionStorage, 'teacherSessionFile');
  requireRole(teacherSession, 'TEACHER', 'teacherSessionFile');

  const results = [];
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  let page;

  try {
    page = await createAuthedPage(
      context,
      teacherSession.sessionStorage || {},
      'http://localhost:5500/teacher-student-dashboard.html'
    );

    await runStep(results, 'teacher student dashboard trend chart shows honest empty state for empty trend data', async () => {
      await page.evaluate(async () => {
        const original = window.getScoreTrendData;
        window.__scoreTrendOriginal__ = original;
        window.getScoreTrendData = async function mockedEmptyTrend() {
          return {
            labels: [],
            scores: [],
            isEmpty: true,
            message: '当前筛选下暂无成绩趋势数据'
          };
        };
        await window.initScoreTrendChart('week');
      });

      const emptyText = await page.locator('.score-trend-empty-state').innerText();
      assert(
        emptyText.includes('当前筛选下暂无成绩趋势数据'),
        'score trend chart should render an honest empty-state message when API returns no points',
        emptyText
      );

      const chartData = await page.evaluate(() => window.Chart.getChart('scoreTrendChart')?.data?.datasets?.[0]?.data || []);
      assert(
        chartData.every(value => value === null),
        'score trend fallback chart should not render fabricated score values when trend data is empty',
        chartData
      );
    });

    await runStep(results, 'teacher student dashboard trend chart shows unavailable state for trend errors', async () => {
      await page.evaluate(async () => {
        window.getScoreTrendData = async function mockedTrendFailure() {
          throw new Error('synthetic trend failure');
        };
        await window.initScoreTrendChart('week');
      });

      const emptyText = await page.locator('.score-trend-empty-state').innerText();
      assert(
        emptyText.includes('成绩趋势数据暂不可用，请稍后重试'),
        'score trend chart should render an unavailable message instead of fabricated trend points when API fails',
        emptyText
      );

      const bodyText = await page.locator('body').innerText();
      assert(
        !bodyText.includes('已显示占位数据'),
        'score trend chart should no longer claim fabricated placeholder trend data was shown',
        bodyText
      );
    });

    await runStep(results, 'teacher student dashboard student table shows access-denied error instead of empty-result copy', async () => {
      await page.evaluate(async () => {
        const originalFetch = window.fetch.bind(window);
        window.__studentDashboardOriginalFetch__ = originalFetch;
        window.fetch = async function mockedStudentSummaryFetch(input, init) {
          const requestUrl = typeof input === 'string' ? input : input?.url || '';
          if (requestUrl.includes('/api/teacher/learning-summary')) {
            return new Response('Forbidden', {
              status: 403,
              statusText: 'Forbidden',
              headers: { 'Content-Type': 'text/plain; charset=utf-8' }
            });
          }
          return originalFetch(input, init);
        };

        await window.loadStudentData();
      });

      const tableText = await page.locator('#studentTableBody').innerText();
      assert(
        tableText.includes('当前账号暂无权限查看学生学习数据'),
        'student table should show an honest access-denied message when learning-summary returns 403',
        tableText
      );

      assert(
        !tableText.includes('当前环境未开放该页面所需数据，已显示空结果'),
        'student table should no longer describe access-denied responses as empty results',
        tableText
      );
    });

    console.log(`Teacher student dashboard runtime verifier passed: ${results.length}`);
  } finally {
    if (page && !page.isClosed()) {
      await page.evaluate(() => {
        if (window.__scoreTrendOriginal__) {
          window.getScoreTrendData = window.__scoreTrendOriginal__;
          delete window.__scoreTrendOriginal__;
        }
        if (window.__studentDashboardOriginalFetch__) {
          window.fetch = window.__studentDashboardOriginalFetch__;
          delete window.__studentDashboardOriginalFetch__;
        }
      }).catch(() => {});
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
