const fs = require('fs');
const path = require('path');
const os = require('os');
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
  await page.waitForTimeout(2500);
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
  const tempAvatarPath = path.join(os.tmpdir(), 'codex-student-settings-avatar-upload.png');
  fs.writeFileSync(
    tempAvatarPath,
    Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8Xw8AAoMBgN8j04YAAAAASUVORK5CYII=', 'base64')
  );

  try {
    page = await createAuthedPage(
      context,
      studentSession.sessionStorage || {},
      'http://localhost:5500/student-settings.html'
    );

    await runStep(results, 'student settings avatar upload hits real api and succeeds in JWT mode', async () => {
      const responses = [];
      page.on('response', response => {
        const url = response.url();
        if (url.includes('/api/student/upload-avatar')) {
          responses.push({ url, status: response.status() });
        }
      });

      try {
        await page.locator('#avatarUpload').setInputFiles(tempAvatarPath);

        await page.waitForFunction(() => {
          const textNode = document.querySelector('#messageContainer .message-text');
          return textNode && textNode.textContent && textNode.textContent.includes('头像上传成功');
        }, { timeout: 15000 });
      } finally {
        page.removeAllListeners('response');
      }

      const successHit = responses.find(item => item.url.includes('/api/student/upload-avatar') && item.status === 200);
      assert(successHit, 'student settings should call /api/student/upload-avatar successfully', responses);

      const pageText = await page.locator('body').innerText();
      assert(
        !pageText.includes('当前 JWT 微服务环境暂未提供头像上传接口'),
        'student settings should no longer claim avatar upload is unavailable in JWT mode',
        pageText
      );
    });

    console.log(`Student settings avatar runtime verifier passed: ${results.length}`);
  } finally {
    fs.rmSync(tempAvatarPath, { force: true });
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
