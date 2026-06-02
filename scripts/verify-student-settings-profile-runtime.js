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

async function waitForMessage(page, expectedText) {
  await page.waitForFunction(text => {
    const container = document.getElementById('messageContainer');
    return container && container.innerText.includes(text);
  }, expectedText, { timeout: 15000 });
}

(async () => {
  const studentSession = readJson(studentSessionFile);
  assertFreshSession(studentSession.sessionStorage, 'studentSessionFile');
  requireRole(studentSession, 'STUDENT', 'studentSessionFile');

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  let page;
  let originalLocalAcademicState = null;
  let originalProfile = null;
  let savedProfileRequestBodies = [];

  try {
    page = await createAuthedPage(
      context,
      studentSession.sessionStorage || {},
      'http://localhost:5500/student-settings.html'
    );

    page.on('request', request => {
      if (request.method() === 'PUT' && request.url().includes('/api/student/profile')) {
        try {
          savedProfileRequestBodies.push(request.postDataJSON());
        } catch (error) {
          savedProfileRequestBodies.push({ parseError: error.message, raw: request.postData() });
        }
      }
    });

    originalLocalAcademicState = await page.evaluate(() => {
      const userId = sessionStorage.getItem('userId') || 'current';
      const key = `studentAcademicProfile:${userId}`;
      return {
        key,
        value: localStorage.getItem(key)
      };
    });

    originalProfile = await page.evaluate(() => ({
      name: document.getElementById('profile-name')?.value || '',
      email: document.getElementById('profile-email')?.value || '',
      phone: document.getElementById('profile-phone')?.value || '',
      major: document.getElementById('profile-major')?.value || '',
      grade: document.getElementById('gradeSelect')?.value || '',
      className: document.getElementById('profile-class')?.value || ''
    }));

    const pageText = await page.locator('body').innerText();
    assert(
      pageText.includes('姓名、邮箱、手机号会同步到账号资料；专业、年级以当前浏览器补充信息为准。'),
      'student settings should explain split persistence semantics for profile fields',
      pageText
    );
    assert(
      pageText.includes('班级由系统班级关系自动生成，当前页不可手动修改。'),
      'student settings should explain class name is read-only',
      pageText
    );

    const classReadOnly = await page.locator('#profile-class').evaluate(element => element.hasAttribute('readonly'));
    assert(classReadOnly, 'student class field should be read-only in settings');

    const profileSuffix = ` 设定校验${Date.now().toString().slice(-5)}`;
    const temporaryName = `${originalProfile.name || 'student42'}${profileSuffix}`;
    const temporaryMajor = `软件工程-${Date.now().toString().slice(-4)}`;
    const temporaryGrade = '2024';

    await page.locator('#profile-name').fill(temporaryName);
    await page.locator('#profile-major').fill(temporaryMajor);
    await page.locator('#gradeSelect').selectOption(temporaryGrade);

    await page.locator('#saveProfileBtn').click();
    await waitForMessage(page, '账号资料已保存，专业/年级已保存在当前浏览器。');

    const lastProfileRequest = savedProfileRequestBodies[savedProfileRequestBodies.length - 1];
    assert(lastProfileRequest, 'student settings profile save should send a profile update request');
    assert(
      !Object.prototype.hasOwnProperty.call(lastProfileRequest, 'major') &&
        !Object.prototype.hasOwnProperty.call(lastProfileRequest, 'grade') &&
        !Object.prototype.hasOwnProperty.call(lastProfileRequest, 'className'),
      'student settings profile save should not send browser-local academic fields to the account profile API',
      lastProfileRequest
    );

    await page.reload({ waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(2500);

    const reloadedState = await page.evaluate(() => ({
      name: document.getElementById('profile-name')?.value || '',
      major: document.getElementById('profile-major')?.value || '',
      grade: document.getElementById('gradeSelect')?.value || '',
      className: document.getElementById('profile-class')?.value || ''
    }));

    assert(reloadedState.name === temporaryName, 'student account profile fields should persist through the real profile API', reloadedState);
    assert(reloadedState.major === temporaryMajor, 'student major should persist as browser-local academic data after reload', reloadedState);
    assert(reloadedState.grade === temporaryGrade, 'student grade should persist as browser-local academic data after reload', reloadedState);
    assert(reloadedState.className === originalProfile.className, 'student class name should remain server-derived and unchanged by local profile saves', reloadedState);

    console.log('student settings profile runtime verifier passed');
  } finally {
    if (page && !page.isClosed() && originalProfile) {
      await page.evaluate(async profile => {
        const headers = {
          'Content-Type': 'application/json'
        };
        const token = window.getAccessToken && window.getAccessToken();
        const roleContext = window.getRoleContext && window.getRoleContext();
        if (token) {
          headers.Authorization = `Bearer ${token}`;
        }
        if (roleContext) {
          headers['X-Role-Context'] = roleContext;
        }
        await fetch('/api/student/profile', {
          method: 'PUT',
          credentials: 'include',
          headers,
          body: JSON.stringify({
            name: profile.name,
            email: profile.email,
            phone: profile.phone
          })
        });
      }, originalProfile).catch(() => {});
    }

    if (page && !page.isClosed() && originalLocalAcademicState) {
      await page.evaluate(state => {
        if (state.value === null) {
          localStorage.removeItem(state.key);
        } else {
          localStorage.setItem(state.key, state.value);
        }
      }, originalLocalAcademicState).catch(() => {});
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
