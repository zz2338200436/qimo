const fs = require('fs');
const path = require('path');
const { chromium } = require(path.resolve(__dirname, '../major_assignment/node_modules/playwright'));

const repoRoot = path.resolve(__dirname, '..');
const runtimeLogDir = path.resolve(repoRoot, '.runtime-logs');
const teacherSessionFile = process.argv[2]
  ? path.resolve(process.argv[2])
  : resolveDefaultTeacherSessionFile();

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
  } catch {
    return null;
  }
}

function resolveDefaultTeacherSessionFile() {
  const candidates = fs.existsSync(runtimeLogDir)
    ? fs.readdirSync(runtimeLogDir)
        .filter(name => /^teacher-session.*\.json$/i.test(name))
        .map(name => path.resolve(runtimeLogDir, name))
    : [];

  const nowSeconds = Math.floor(Date.now() / 1000);
  const freshCandidate = candidates
    .map(filePath => {
      const session = readJson(filePath);
      const token = session.sessionStorage?.token || session.sessionStorage?.accessToken || '';
      const payload = decodeJwtPayload(token);
      const stat = fs.statSync(filePath);
      return {
        filePath,
        exp: payload?.exp || 0,
        mtimeMs: stat.mtimeMs
      };
    })
    .filter(item => item.exp > nowSeconds + 30)
    .sort((a, b) => b.exp - a.exp || b.mtimeMs - a.mtimeMs)[0];

  return freshCandidate?.filePath || path.resolve(runtimeLogDir, 'teacher-session-agent-runtime.json');
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
  await page.waitForTimeout(2000);
  return page;
}

(async () => {
  const teacherSession = readJson(teacherSessionFile);
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  let page;

  try {
    page = await createAuthedPage(
      context,
      teacherSession.sessionStorage || {},
      'http://localhost:5500/teacher-ai-tools.html'
    );

    await page.locator('[data-agent-history-toggle]').click();
    await page.waitForSelector('.agent-history-panel.agent-history-open');
    await page.waitForSelector('[data-agent-history-session]');
    await page.locator('[data-agent-history-session]').first().click();
    await page.waitForSelector('[data-agent-history-continue]');

    const sessionId = await page.locator('[data-agent-history-continue]').getAttribute('data-agent-history-continue');
    assert(sessionId, 'continue session button should carry target sessionId');

    await page.locator('[data-agent-history-continue]').click();
    await page.waitForFunction(targetSessionId => {
      const panel = document.querySelector('[data-agent-panel]');
      const messageText = Array.from(document.querySelectorAll('.agent-message-agent .agent-message-body'))
        .map(node => node.textContent || '')
        .join('\n');
      const currentItem = document.querySelector(`.agent-history-item.agent-history-item-current[data-agent-history-session="${targetSessionId}"]`);
      const currentTitle = currentItem?.querySelector('.agent-history-item-title')?.textContent?.trim() || '';
      return panel?.agentChatPanel?.sessionId === targetSessionId
        && !!currentItem
        && currentTitle === '当前会话'
        && messageText.includes(`已切换到会话 #${targetSessionId}`);
    }, sessionId, { timeout: 10000 });

    await page.reload({ waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(2000);
    await page.locator('[data-agent-history-toggle]').click();
    await page.waitForSelector('.agent-history-panel.agent-history-open');
    await page.waitForFunction(targetSessionId => {
      const currentItem = document.querySelector(`.agent-history-item.agent-history-item-current[data-agent-history-session="${targetSessionId}"]`);
      const currentTitle = currentItem?.querySelector('.agent-history-item-title')?.textContent?.trim() || '';
      return !!currentItem && currentTitle === '当前会话';
    }, sessionId, { timeout: 10000 });

    console.log('Agent history session switch runtime verification passed.');
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
