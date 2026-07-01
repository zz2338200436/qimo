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

  const results = [];
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  let page;

  try {
    page = await createAuthedPage(
      context,
      studentSession.sessionStorage || {},
      'http://localhost:5500/student-dashboard.html'
    );

    await runStep(results, 'student dashboard study-time api returns connected data', async () => {
      const studyTimeResponse = await page.evaluate(async () => {
        const response = await fetch('/api/student/study-time-distribution', {
          method: 'GET',
          credentials: 'include'
        });
        const text = await response.text();
        let json = null;
        if (text) {
          try {
            json = JSON.parse(text);
          } catch (error) {
            json = null;
          }
        }
        return {
          status: response.status,
          ok: response.ok,
          json
        };
      });

      assert(
        studyTimeResponse.status === 200 && studyTimeResponse.json?.success === true,
        'student dashboard should reach /api/student/study-time-distribution successfully',
        studyTimeResponse
      );

      const studyTimeList = Array.isArray(studyTimeResponse.json?.data) ? studyTimeResponse.json.data : [];
      const nonZeroPoint = studyTimeList.find(item => Number(item?.study_time) > 0);
      assert(nonZeroPoint, 'study-time api should provide at least one non-zero daily study-time record', studyTimeList);
    });

    await runStep(results, 'student dashboard study-time chart renders real series values', async () => {
      await page.waitForFunction(() => {
        const chartDom = document.getElementById('studyTimeChart');
        if (!chartDom || !window.echarts || typeof window.echarts.getInstanceByDom !== 'function') {
          return false;
        }
        const chart = window.echarts.getInstanceByDom(chartDom);
        if (!chart) {
          return false;
        }
        const option = chart.getOption();
        const seriesData = option?.series?.[0]?.data;
        return Array.isArray(seriesData) && seriesData.length === 7;
      }, { timeout: 15000 });

      const chartState = await page.evaluate(() => {
        const chartDom = document.getElementById('studyTimeChart');
        const chart = window.echarts?.getInstanceByDom?.(chartDom);
        const option = chart?.getOption?.() || {};
        return {
          xAxis: option?.xAxis?.[0]?.data || [],
          seriesData: option?.series?.[0]?.data || []
        };
      });

      assert(
        Array.isArray(chartState.seriesData) && chartState.seriesData.length === 7,
        'study-time chart should render a full week series',
        chartState
      );

      const numericData = chartState.seriesData.map(value => Number(value) || 0);
      assert(
        numericData.some(value => value > 0),
        'study-time chart should contain at least one non-zero value from the real api response',
        chartState
      );
    });

    console.log(`Student dashboard runtime verifier passed: ${results.length}`);
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
