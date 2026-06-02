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

async function collectKnowledgeResponses(page, action) {
  const responses = [];
  const handler = response => {
    const url = response.url();
    if (url.includes('/api/student/knowledge-points')) {
      responses.push({ url, status: response.status() });
    }
  };
  page.on('response', handler);
  try {
    await action();
    await page.waitForTimeout(1500);
  } finally {
    page.off('response', handler);
  }
  return responses;
}

async function collectStudyTimeResponses(page, action) {
  const responses = [];
  const handler = response => {
    const url = response.url();
    if (url.includes('/api/student/study-time-distribution')) {
      responses.push({ url, status: response.status() });
    }
  };
  page.on('response', handler);
  try {
    await action();
    await page.waitForTimeout(1500);
  } finally {
    page.off('response', handler);
  }
  return responses;
}

async function collectStatsAndScoresResponses(page, action) {
  const responses = [];
  const handler = response => {
    const url = response.url();
    if (url.includes('/api/student/stats') || url.includes('/api/student/scores')) {
      responses.push({ url, status: response.status() });
    }
  };
  page.on('response', handler);
  try {
    await action();
    await page.waitForTimeout(1500);
  } finally {
    page.off('response', handler);
  }
  return responses;
}

async function collectCourseResponses(page, action) {
  const responses = [];
  const handler = response => {
    const url = response.url();
    if (url.includes('/api/student/courses')) {
      responses.push({ url, status: response.status() });
    }
  };
  page.on('response', handler);
  try {
    await action();
    await page.waitForTimeout(1500);
  } finally {
    page.off('response', handler);
  }
  return responses;
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
      'http://localhost:5500/student-stats.html'
    );

    await runStep(results, 'student stats initial load hits knowledge-points api and renders knowledge cards', async () => {
      const responses = await collectKnowledgeResponses(page, async () => {
        await page.reload({ waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(3500);
      });

      const successHit = responses.find(item => item.url.includes('/api/student/knowledge-points') && item.status === 200);
      assert(successHit, 'student stats initial load should call /api/student/knowledge-points successfully', responses);

      await page.waitForFunction(() => {
        const cards = document.querySelectorAll('#knowledgeList .knowledge-item');
        return cards.length > 0;
      }, { timeout: 15000 });
    });

    await runStep(results, 'student stats initial load hits study-time api and renders non-zero chart values', async () => {
      const responses = await collectStudyTimeResponses(page, async () => {
        await page.reload({ waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(3500);
      });

      const successHit = responses.find(item => item.url.includes('/api/student/study-time-distribution') && item.status === 200);
      assert(successHit, 'student stats initial load should call /api/student/study-time-distribution successfully', responses);

      await page.waitForFunction(() => {
        const canvas = document.getElementById('studyTimeChart');
        if (!canvas || !window.Chart || typeof window.Chart.getChart !== 'function') {
          return false;
        }
        const chart = window.Chart.getChart(canvas);
        const seriesData = chart?.data?.datasets?.[0]?.data;
        return Array.isArray(seriesData) && seriesData.length > 0;
      }, { timeout: 15000 });

      const chartState = await page.evaluate(() => {
        const canvas = document.getElementById('studyTimeChart');
        const chart = window.Chart?.getChart?.(canvas);
        return {
          labels: chart?.data?.labels || [],
          seriesData: chart?.data?.datasets?.[0]?.data || []
        };
      });

      const numericData = chartState.seriesData.map(value => Number(value) || 0);
      assert(
        numericData.some(value => value > 0),
        'student stats study-time chart should contain at least one non-zero value from the real api response',
        chartState
      );
    });

    await runStep(results, 'student stats initial load hits stats and scores apis and renders real cards/charts', async () => {
      const responses = await collectStatsAndScoresResponses(page, async () => {
        await page.reload({ waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(3500);
      });

      const statsHit = responses.find(item => item.url.includes('/api/student/stats') && item.status === 200);
      const scoresHit = responses.find(item => item.url.includes('/api/student/scores') && item.status === 200);
      assert(statsHit, 'student stats initial load should call /api/student/stats successfully', responses);
      assert(scoresHit, 'student stats initial load should call /api/student/scores successfully', responses);

      await page.waitForFunction(() => {
        const averageScore = document.getElementById('averageScore')?.textContent?.trim() || '';
        const completedTasks = document.getElementById('completedTasks')?.textContent?.trim() || '';
        const scoreCanvas = document.getElementById('scoreChart');
        const scoreChart = window.Chart?.getChart?.(scoreCanvas);
        const scoreSeries = scoreChart?.data?.datasets?.[0]?.data || [];
        return averageScore !== '0.0' && completedTasks !== '0' && Array.isArray(scoreSeries) && scoreSeries.length > 0;
      }, { timeout: 15000 });
    });

    await runStep(results, 'student stats initial load hits courses api and populates course filter in JWT mode', async () => {
      const responses = await collectCourseResponses(page, async () => {
        await page.reload({ waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(3500);
      });

      const successHit = responses.find(item => item.url.includes('/api/student/courses') && item.status === 200);
      assert(successHit, 'student stats initial load should call /api/student/courses successfully', responses);

      await page.waitForFunction(() => {
        const courseSelect = document.getElementById('course');
        return courseSelect && courseSelect.options.length > 1;
      }, { timeout: 15000 });
    });

    await runStep(results, 'student stats refresh knowledge button re-fetches real knowledge data', async () => {
      const responses = await collectKnowledgeResponses(page, async () => {
        await page.getByRole('button', { name: '刷新' }).click();
        await page.waitForFunction(() => {
          const cards = document.querySelectorAll('#knowledgeList .knowledge-item');
          return cards.length > 0;
        }, { timeout: 15000 });
      });

      const successHit = responses.find(item => item.url.includes('/api/student/knowledge-points') && item.status === 200);
      assert(successHit, 'student stats refresh should re-fetch /api/student/knowledge-points successfully', responses);

      const pageText = await page.locator('body').innerText();
        assert(
          !pageText.includes('当前 JWT 微服务环境暂未接通知识点统计接口。'),
          'student stats should no longer claim the knowledge-points api is unavailable in JWT mode',
          pageText
        );
      });

    await runStep(results, 'student stats study-time view toggle re-fetches real study-time data in JWT mode', async () => {
      const responses = await collectStudyTimeResponses(page, async () => {
        await page.getByRole('button', { name: '每日' }).click();
        await page.waitForFunction(() => {
          const canvas = document.getElementById('studyTimeChart');
          if (!canvas || !window.Chart || typeof window.Chart.getChart !== 'function') {
            return false;
          }
          const chart = window.Chart.getChart(canvas);
          const labels = chart?.data?.labels || [];
          const seriesData = chart?.data?.datasets?.[0]?.data || [];
          return Array.isArray(labels) && Array.isArray(seriesData) && labels.length === seriesData.length && labels.length > 0;
        }, { timeout: 15000 });
      });

      const successHit = responses.find(item => item.url.includes('/api/student/study-time-distribution') && item.status === 200);
      assert(successHit, 'student stats time view toggle should re-fetch /api/student/study-time-distribution successfully', responses);
    });

    await runStep(results, 'student stats course filter re-fetches connected data with courseId in JWT mode', async () => {
      await page.reload({ waitUntil: 'domcontentloaded' });
      await page.waitForTimeout(3500);

      const selectedCourseId = await page.evaluate(() => {
        const courseSelect = document.getElementById('course');
        if (!courseSelect || courseSelect.options.length < 2) {
          return '';
        }
        return courseSelect.options[1].value;
      });
      assert(selectedCourseId, 'student stats runtime needs at least one real course option to verify course filtering');

      const responses = [];
      const handler = response => {
        const url = response.url();
        if (
          url.includes('/api/student/stats') ||
          url.includes('/api/student/scores') ||
          url.includes('/api/student/study-time-distribution') ||
          url.includes('/api/student/knowledge-points')
        ) {
          responses.push({ url, status: response.status() });
        }
      };
      page.on('response', handler);
      try {
        await page.selectOption('#course', selectedCourseId);
        await page.getByRole('button', { name: '应用筛选' }).click();
        await page.waitForTimeout(3500);
      } finally {
        page.off('response', handler);
      }

      const expectedFragments = [
        '/api/student/stats',
        '/api/student/scores',
        '/api/student/study-time-distribution',
        '/api/student/knowledge-points'
      ];

      expectedFragments.forEach(fragment => {
        const matched = responses.find(item => item.url.includes(fragment) && item.url.includes(`courseId=${selectedCourseId}`) && item.status === 200);
        assert(matched, `student stats course filter should re-fetch ${fragment} with courseId=${selectedCourseId}`, responses);
      });
    });

    console.log(`Student stats runtime verifier passed: ${results.length}`);
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
