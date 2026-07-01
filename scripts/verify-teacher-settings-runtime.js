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
  await page.waitForTimeout(2500);
  return page;
}

async function waitForMessage(page, expectedText) {
  await page.waitForFunction(text => {
    const node = document.getElementById('messageContainer');
    return node && node.innerText.includes(text);
  }, expectedText, { timeout: 10000 });
}

(async () => {
  const teacherSession = readJson(teacherSessionFile);
  assertFreshSession(teacherSession.sessionStorage, 'teacherSessionFile');
  requireRole(teacherSession, 'TEACHER', 'teacherSessionFile');

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  let page;
  let capturedPasswordPayload = null;
  let capturedNotificationPayload = null;

  try {
    await context.route('**/api/auth/change-password', async route => {
      const request = route.request();
      try {
        capturedPasswordPayload = request.postDataJSON();
      } catch (error) {
        capturedPasswordPayload = { parseError: error.message, raw: request.postData() };
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          message: '密码更新成功',
          code: 200,
          data: true
        })
      });
    });

    await context.route('**/api/auth/notification-settings', async route => {
      const request = route.request();
      try {
        capturedNotificationPayload = request.postDataJSON();
      } catch (error) {
        capturedNotificationPayload = { parseError: error.message, raw: request.postData() };
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          message: '通知设置保存成功',
          code: 200,
          data: {
            mode: 'compatibility-placeholder',
            persisted: false
          }
        })
      });
    });

    page = await createAuthedPage(
      context,
      teacherSession.sessionStorage || {},
      'http://localhost:5500/teacher-settings.html'
    );

    await page.fill('#currentPassword', 'Teacher123!');
    await page.fill('#newPassword', 'Teacher456!');
    await page.fill('#confirmPassword', 'Teacher456!');

    await page.click('button[onclick="updatePassword()"]');
    await waitForMessage(page, '密码更新成功，请重新登录');

    assert(capturedPasswordPayload, 'teacher settings password update should send a change-password request');
    assert(
      capturedPasswordPayload.currentPassword === 'Teacher123!' &&
      capturedPasswordPayload.newPassword === 'Teacher456!' &&
      capturedPasswordPayload.confirmPassword === 'Teacher456!',
      'teacher settings password update request should include current/new/confirm password fields',
      capturedPasswordPayload
    );

    await page.uncheck('#emailNotifications');
    await page.check('#assignmentNotifications');
    await page.uncheck('#examNotifications');
    await page.check('#warningNotifications');

    await page.click('button[onclick="saveNotificationSettings()"]');
    await waitForMessage(page, '通知偏好已同步到当前登录会话，并保留当前浏览器副本');

    assert(
      capturedNotificationPayload,
      'teacher settings notification preferences should send a notification-settings request'
    );
    assert(
      capturedNotificationPayload.emailNotifications === false &&
      capturedNotificationPayload.assignmentNotifications === true &&
      capturedNotificationPayload.examNotifications === false &&
      capturedNotificationPayload.warningNotifications === true,
      'teacher settings notification preferences request should include checkbox state payload',
      capturedNotificationPayload
    );

    const notificationInfoText = await page.locator('#notificationSettingsInfo').innerText();
    assert(
      notificationInfoText.includes('当前登录会话') && notificationInfoText.includes('当前浏览器副本'),
      'teacher settings notification info should explain session sync plus browser copy semantics',
      notificationInfoText
    );

    console.log('teacher settings runtime verifier passed');
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
