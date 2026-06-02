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
  await page.waitForTimeout(2000);
  return page;
}

async function collectAiResponses(page, action) {
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

async function assertLatestAiMessageContains(page, expectedText) {
  await page.waitForFunction(text => {
    const messages = Array.from(document.querySelectorAll('#chatMessages .message.ai .message-content'));
    const latest = messages[messages.length - 1];
    return latest && latest.innerText.includes(text);
  }, expectedText, { timeout: 15000 });
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
      'http://localhost:5500/student-ai-assistant.html'
    );

    await runStep(results, 'student ai assistant banner renders truthful availability', async () => {
      const bannerText = await page.locator('#assistantModeBanner').innerText();
      assert(
        bannerText.includes('AI学习建议') &&
        bannerText.includes('自由问答暂不可用'),
        'availability banner should describe real student AI capability',
        bannerText
      );
    });

    await runStep(results, 'student ai assistant quick learning suggestion hits real api and renders response', async () => {
      const responses = await collectAiResponses(page, async () => {
        await page.getByRole('button', { name: '生成我的学习建议' }).click();
        await assertLatestAiMessageContains(page, '学习建议');
        await assertLatestAiMessageContains(page, '建议');
      });
      const hit = responses.find(item => item.url.includes('/api/ai/learning-suggestions'));
      assert(hit && hit.status === 200, 'student assistant should call /api/ai/learning-suggestions successfully', responses);
    });

    await runStep(results, 'student ai assistant honest free chat downgrade makes no ai request', async () => {
      await page.fill('#chatInput', 'Python的面向对象编程怎么理解？');
      const responses = await collectAiResponses(page, async () => {
        await page.click('#chatForm button[type="submit"]');
        await assertLatestAiMessageContains(page, '当前运行时仅支持学习建议');
        await assertLatestAiMessageContains(page, '自由问答暂不可用');
      });
      assert(responses.length === 0, 'unsupported free chat should not call any /api/ai endpoint', responses);
    });

    console.log(`Student AI assistant runtime verifier passed: ${results.length}`);
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
