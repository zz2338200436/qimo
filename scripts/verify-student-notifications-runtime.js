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

(async () => {
  const studentSession = readJson(studentSessionFile);
  assertFreshSession(studentSession.sessionStorage, 'studentSessionFile');
  requireRole(studentSession, 'STUDENT', 'studentSessionFile');

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  let page;

  let readMarked = false;
  let unreadCountRequestCount = 0;
  const notificationListRequests = [];

  const baseNotification = {
    id: 901,
    type: 'course',
    title: '课程通知',
    content: '这是一条未读课程通知',
    createdAt: '2026-05-24 10:00:00',
    read: false,
    isRead: false
  };

  try {
    await context.route('**/api/notifications/student**', async route => {
      const url = new URL(route.request().url());
      if (url.pathname.endsWith('/student/unread-count')) {
        unreadCountRequestCount += 1;
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            message: 'ok',
            code: 200,
            data: readMarked ? 4 : 5
          })
        });
        return;
      }

      const filter = url.searchParams.get('filter') || 'all';
      notificationListRequests.push(filter);

      let notifications;
      if (filter === 'unread') {
        notifications = readMarked ? [] : [baseNotification];
      } else if (filter === 'read') {
        notifications = readMarked ? [{ ...baseNotification, read: true, isRead: true }] : [];
      } else {
        notifications = readMarked ? [{ ...baseNotification, read: true, isRead: true }] : [baseNotification];
      }

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          message: 'ok',
          code: 200,
          data: {
            notifications,
            total: notifications.length
          }
        })
      });
    });

    await context.route('**/api/notifications/901/read**', async route => {
      readMarked = true;
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          message: 'ok',
          code: 200,
          data: null
        })
      });
    });

    page = await createAuthedPage(
      context,
      studentSession.sessionStorage || {},
      'http://localhost:5500/student-notifications.html'
    );

    await page.waitForFunction(() => {
      const list = document.getElementById('notificationList');
      return list && list.innerText.includes('课程通知');
    }, { timeout: 10000 });

    await page.click('.filter-btn[data-filter="unread"]');
    await page.waitForFunction(() => {
      const list = document.getElementById('notificationList');
      return list && list.innerText.includes('课程通知');
    }, { timeout: 10000 });

    await page.click('.notification-item .action-btn:not(.delete-btn)');

    await page.waitForFunction(() => {
      const message = document.getElementById('messageContainer');
      return message && message.innerText.includes('通知已标记为已读');
    }, { timeout: 10000 });

    await page.waitForFunction(() => {
      const list = document.getElementById('notificationList');
      return list && list.innerText.includes('暂无通知');
    }, { timeout: 10000 });

    assert(
      unreadCountRequestCount >= 2,
      'student notifications should re-fetch unread count from the server after mark-as-read mutations',
      { unreadCountRequestCount, notificationListRequests }
    );

    const unreadRequests = notificationListRequests.filter(item => item === 'unread').length;
    assert(
      unreadRequests >= 2,
      'student notifications should refresh the current unread-filtered list after mark-as-read mutations',
      { notificationListRequests }
    );

    console.log('student notifications runtime verifier passed');
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
