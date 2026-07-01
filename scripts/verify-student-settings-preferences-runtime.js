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
  await page.waitForTimeout(2500);
  return page;
}

async function setCheckboxState(page, selector, checked) {
  const locator = page.locator(selector);
  if (checked) {
    await locator.check();
  } else {
    await locator.uncheck();
  }
}

(async () => {
  const studentSession = readJson(studentSessionFile);
  assertFreshSession(studentSession.sessionStorage, 'studentSessionFile');
  requireRole(studentSession, 'STUDENT', 'studentSessionFile');

  const results = [];
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  let page;
  let originalLocalPreferenceState = null;

  try {
    page = await createAuthedPage(
      context,
      studentSession.sessionStorage || {},
      'http://localhost:5500/student-settings.html'
    );

    originalLocalPreferenceState = await page.evaluate(() => {
      const userId = sessionStorage.getItem('userId') || 'current';
      const notificationKey = `studentNotificationSettings:${userId}`;
      const privacyKey = `studentPrivacySettings:${userId}`;
      return {
        notificationKey,
        privacyKey,
        notificationValue: localStorage.getItem(notificationKey),
        privacyValue: localStorage.getItem(privacyKey)
      };
    });

    const responses = [];
    page.on('response', response => {
      responses.push({ url: response.url(), status: response.status() });
    });

    await page.evaluate(() => {
      const userId = sessionStorage.getItem('userId') || 'current';
      localStorage.removeItem(`studentNotificationSettings:${userId}`);
      localStorage.removeItem(`studentPrivacySettings:${userId}`);
    });
    responses.length = 0;
    await page.reload({ waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(2500);

    await runStep(results, 'student settings preferences stay browser-local and avoid placeholder preference APIs', async () => {
      const pageText = await page.locator('body').innerText();
      assert(
        pageText.includes('当前环境下通知偏好以本浏览器保存为准。'),
        'student settings should explain notification preferences are stored in the current browser',
        pageText
      );
      assert(
        pageText.includes('当前环境下隐私设置以本浏览器保存为准。'),
        'student settings should explain privacy preferences are stored in the current browser',
        pageText
      );

      const unexpectedPreferenceApis = responses.filter(item =>
        item.url.includes('/api/student/notification-settings') ||
        item.url.includes('/api/student/privacy-settings')
      );
      assert(
        unexpectedPreferenceApis.length === 0,
        'student settings should no longer call placeholder notification/privacy preference APIs on load',
        unexpectedPreferenceApis
      );

      await setCheckboxState(page, '#emailNotifications', true);
      await setCheckboxState(page, '#smsNotifications', false);
      await setCheckboxState(page, '#webNotifications', true);
      await setCheckboxState(page, '#assignmentNotifications', true);
      await setCheckboxState(page, '#gradeNotifications', false);
      await setCheckboxState(page, '#courseNotifications', false);
      await setCheckboxState(page, '#systemNotifications', true);
      await setCheckboxState(page, '#reminderNotifications', false);

      await page.locator('#saveNotificationBtn').click();
      await page.waitForFunction(() => {
        const container = document.getElementById('messageContainer');
        return container && container.innerText.includes('通知设置已保存在当前浏览器。');
      }, { timeout: 10000 });

      await setCheckboxState(page, '#shareProfile', true);
      await setCheckboxState(page, '#shareAchievements', false);
      await setCheckboxState(page, '#dataCollection', true);

      await page.locator('#savePrivacyBtn').click();
      await page.waitForFunction(() => {
        const container = document.getElementById('messageContainer');
        return container && container.innerText.includes('隐私设置已保存在当前浏览器。');
      }, { timeout: 10000 });

      const unexpectedSaveApis = responses.filter(item =>
        item.url.includes('/api/student/notification-settings') ||
        item.url.includes('/api/student/privacy-settings')
      );
      assert(
        unexpectedSaveApis.length === 0,
        'student settings should not call placeholder notification/privacy preference APIs when saving',
        unexpectedSaveApis
      );

      await page.reload({ waitUntil: 'domcontentloaded' });
      await page.waitForTimeout(2500);

      const expectedCheckboxStates = {
        emailNotifications: true,
        smsNotifications: false,
        webNotifications: true,
        assignmentNotifications: true,
        gradeNotifications: false,
        courseNotifications: false,
        systemNotifications: true,
        reminderNotifications: false,
        shareProfile: true,
        shareAchievements: false,
        dataCollection: true
      };

      const actualCheckboxStates = await page.evaluate(() => {
        const ids = [
          'emailNotifications',
          'smsNotifications',
          'webNotifications',
          'assignmentNotifications',
          'gradeNotifications',
          'courseNotifications',
          'systemNotifications',
          'reminderNotifications',
          'shareProfile',
          'shareAchievements',
          'dataCollection'
        ];
        return ids.reduce((result, id) => {
          const input = document.getElementById(id);
          result[id] = !!input?.checked;
          return result;
        }, {});
      });

      assert(
        JSON.stringify(actualCheckboxStates) === JSON.stringify(expectedCheckboxStates),
        'student settings should restore locally saved notification/privacy preferences after reload',
        actualCheckboxStates
      );
    });

    console.log(`Student settings preferences runtime verifier passed: ${results.length}`);
  } finally {
    if (page && !page.isClosed() && originalLocalPreferenceState) {
      await page.evaluate(state => {
        const { notificationKey, privacyKey, notificationValue, privacyValue } = state;
        if (notificationValue === null) {
          localStorage.removeItem(notificationKey);
        } else {
          localStorage.setItem(notificationKey, notificationValue);
        }
        if (privacyValue === null) {
          localStorage.removeItem(privacyKey);
        } else {
          localStorage.setItem(privacyKey, privacyValue);
        }
      }, originalLocalPreferenceState).catch(() => {});
    }
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
