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
  await page.waitForTimeout(2000);
  return page;
}

async function activateTool(page, toolId) {
  await page.evaluate(id => {
    if (typeof window.backToTools === 'function') {
      window.backToTools();
    }
    window.activateTool(id);
  }, toolId);
  await page.waitForTimeout(200);
}

async function waitForResultCount(page, expectedMinimum) {
  await page.waitForFunction(min => {
    const node = document.getElementById('result-count');
    return node && Number(node.textContent || '0') >= min;
  }, expectedMinimum, { timeout: 15000 });
}

async function collectApiResponses(page, action) {
  const responses = [];
  const handler = response => {
    const url = response.url();
    if (url.includes('/api/ai/')) {
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

async function assertResultContentContains(page, expectedText) {
  await page.waitForFunction(text => {
    const node = document.getElementById('result-content');
    return node && node.innerText.includes(text);
  }, expectedText, { timeout: 10000 });
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
      'http://localhost:5500/teacher-ai-tools.html'
    );

    await runStep(results, 'teacher ai tools does not render unfinished capability banner', async () => {
      const bannerCount = await page.locator('#teacher-ai-tools-availability-banner').count();
      const visibleText = await page.locator('body').innerText();
      assert(bannerCount === 0, 'unfinished capability banner should be removed from teacher page', bannerCount);
      assert(!visibleText.includes('当前已接通：题目生成、试卷生成、学习建议'), 'connected capability banner copy should not render', visibleText);
      assert(!visibleText.includes('暂未接通'), 'unfinished capability chips should not render', visibleText);
    });

    await runStep(results, 'teacher ai tools removes tool cards, forms, and demo shortcuts', async () => {
      const visibleText = await page.locator('body').innerText();
      const toolGridCount = await page.locator('.ai-tools-grid').count();
      const toolCardCount = await page.locator('.ai-tool-card-title').count();
      const formSectionCount = await page.locator('.form-section').count();
      const quickCommandCount = await page.locator('[data-agent-command]').count();
      assert(toolGridCount === 0, 'AI tool grid should be removed from teacher page', toolGridCount);
      assert(toolCardCount === 0, 'AI tool cards should be removed from teacher page', toolCardCount);
      assert(formSectionCount === 0, 'hidden local AI tool forms should be removed from teacher page', formSectionCount);
      assert(quickCommandCount === 0, 'agent demo shortcut buttons should be removed', quickCommandCount);
      assert(!visibleText.includes('可以试试查询课程'), 'agent panel should not render static example prompt', visibleText);
    });

    console.log(`Teacher AI tools runtime verifier passed: ${results.length}`);
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
