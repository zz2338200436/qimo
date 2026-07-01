const { execFileSync } = require('child_process');
const path = require('path');

const repoRoot = path.resolve(__dirname, '..');
const seedScript = path.resolve(repoRoot, 'scripts/seed-isolated-side-effect-smoke.ps1');

const config = {
  baseUrl: (process.env.GATEWAY_BASE_URL || process.argv[2] || 'http://localhost:8080').replace(/\/$/, ''),
  redisContainer: process.env.REDIS_CONTAINER || 'qimo-redis',
  username: process.env.ISOLATED_SMOKE_USERNAME || 'isolated_api_smoke',
  userId: Number(process.env.ISOLATED_SMOKE_USER_ID || 90042),
  originalPassword: process.env.ISOLATED_SMOKE_PASSWORD || 'Teach1234',
  authPassword: process.env.ISOLATED_SMOKE_AUTH_PASSWORD || 'AuthSmoke1234',
  studentPassword: process.env.ISOLATED_SMOKE_STUDENT_PASSWORD || 'StudentSmoke1234',
  examId: Number(process.env.ISOLATED_SMOKE_EXAM_ID || 90042),
  submissionId: Number(process.env.ISOLATED_SMOKE_SUBMISSION_ID || 90042)
};

function assert(condition, message, details) {
  if (!condition) {
    const error = new Error(message);
    if (details !== undefined) {
      error.details = details;
    }
    throw error;
  }
}

function applySeed() {
  execFileSync('powershell', [
    '-ExecutionPolicy',
    'Bypass',
    '-File',
    seedScript
  ], {
    cwd: repoRoot,
    stdio: 'pipe'
  });
}

function readCaptchaCode(captchaKey) {
  const redisKey = `CAPTCHA:IMG:${captchaKey}`;
  const output = execFileSync('docker', [
    'exec',
    config.redisContainer,
    'redis-cli',
    '--raw',
    'GET',
    redisKey
  ], {
    cwd: repoRoot,
    encoding: 'utf8'
  }).trim();
  assert(output, `captcha code was empty for redis key ${redisKey}`);
  return output;
}

async function parseJsonResponse(method, url, response) {
  const text = await response.text();
  let json = null;
  if (text) {
    try {
      json = JSON.parse(text);
    } catch (error) {
      throw new Error(`${method} ${url} returned non-JSON body: ${text.slice(0, 200)}`);
    }
  }
  return { text, json };
}

async function requestJsonAllowStatus(method, url, token, body, expectedStatuses, extraHeaders) {
  const headers = {};
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  Object.assign(headers, extraHeaders || {});
  const response = await fetch(url, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  const { text, json } = await parseJsonResponse(method, url, response);
  if (!expectedStatuses.includes(response.status)) {
    throw new Error(`${method} ${url} expected ${expectedStatuses.join('/')} but got ${response.status}: ${text.slice(0, 300)}`);
  }
  return { response, json, text };
}

async function requestJson(method, url, token, body, extraHeaders) {
  return requestJsonAllowStatus(method, url, token, body, [200, 201], extraHeaders);
}

async function loginAttempt(password, expectedStatuses) {
  const captchaUrl = `${config.baseUrl}/api/auth/captcha?timestamp=${Date.now()}`;
  const captchaResponse = await fetch(captchaUrl);
  assert(captchaResponse.ok, `GET ${captchaUrl} failed with ${captchaResponse.status}`);
  await captchaResponse.arrayBuffer();
  const captchaKey = captchaResponse.headers.get('x-captcha-key');
  assert(captchaKey, 'captcha response should include X-Captcha-Key');
  const captcha = readCaptchaCode(captchaKey);
  return requestJsonAllowStatus('POST', `${config.baseUrl}/api/auth/login`, null, {
    username: config.username,
    password,
    captcha,
    captchaKey
  }, expectedStatuses);
}

async function login(password) {
  const { json } = await loginAttempt(password, [200, 201]);
  assert(json.success === true, 'isolated login should succeed', json);
  assert(json.data?.accessToken, 'isolated login should return accessToken', json);
  assert(json.data?.refreshToken, 'isolated login should return refreshToken', json);
  assert(json.data?.user?.id === config.userId, 'isolated login should resolve fixture user', json.data?.user);
  return json.data;
}

async function expectTokenRejected(token, label) {
  const { json } = await requestJsonAllowStatus('GET', `${config.baseUrl}/api/auth/me`, token, undefined, [401]);
  assert(json.success === false, `${label} should be rejected`, json);
}

async function step(name, action, results) {
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

async function run() {
  const results = [];
  console.log('[SETUP] applying isolated side-effect smoke seed');
  applySeed();

  try {
    let teacherLogin;
    let teacherToken;
    let studentToken;

    await step('isolated multi-role login', async () => {
      teacherLogin = await login(config.originalPassword);
      teacherToken = teacherLogin.accessToken;
      const roles = teacherLogin.user?.roles || [];
      assert(roles.includes('TEACHER'), 'isolated user should include TEACHER role', teacherLogin.user);
      assert(roles.includes('STUDENT'), 'isolated user should include STUDENT role', teacherLogin.user);
      assert(teacherLogin.user.activeRole === 'TEACHER', 'isolated login should default to TEACHER', teacherLogin.user);
    }, results);

    await step('auth refresh success and old refresh rejection', async () => {
      const refreshRes = await requestJson('POST', `${config.baseUrl}/api/auth/refresh`, null, {
        refreshToken: teacherLogin.refreshToken
      });
      assert(refreshRes.json.success === true, 'refresh should succeed', refreshRes.json);
      assert(refreshRes.json.data.accessToken, 'refresh should return a new access token', refreshRes.json);
      assert(refreshRes.json.data.refreshToken, 'refresh should return a new refresh token', refreshRes.json);

      const reusedRefreshRes = await requestJsonAllowStatus('POST', `${config.baseUrl}/api/auth/refresh`, null, {
        refreshToken: teacherLogin.refreshToken
      }, [401]);
      assert(reusedRefreshRes.json.success === false, 'old refresh token should be consumed', reusedRefreshRes.json);

      teacherLogin = refreshRes.json.data;
      teacherToken = teacherLogin.accessToken;
    }, results);

    await step('auth switch-role success in both directions', async () => {
      const switchStudentRes = await requestJson('POST', `${config.baseUrl}/api/auth/switch-role`, teacherToken, {
        targetRole: 'STUDENT'
      });
      assert(switchStudentRes.json.success === true, 'switch to STUDENT should succeed', switchStudentRes.json);
      assert(switchStudentRes.json.data.user.activeRole === 'STUDENT', 'new token should be active as STUDENT', switchStudentRes.json.data.user);
      await expectTokenRejected(teacherToken, 'old teacher token after switch-role');

      studentToken = switchStudentRes.json.data.accessToken;
      const switchTeacherRes = await requestJson('POST', `${config.baseUrl}/api/auth/switch-role`, studentToken, {
        targetRole: 'TEACHER'
      });
      assert(switchTeacherRes.json.success === true, 'switch back to TEACHER should succeed', switchTeacherRes.json);
      assert(switchTeacherRes.json.data.user.activeRole === 'TEACHER', 'new token should be active as TEACHER', switchTeacherRes.json.data.user);
      await expectTokenRejected(studentToken, 'old student token after switch-role');

      teacherToken = switchTeacherRes.json.data.accessToken;
    }, results);

    await step('auth change-password success and restore', async () => {
      const changeRes = await requestJson('POST', `${config.baseUrl}/api/auth/change-password`, teacherToken, {
        currentPassword: config.originalPassword,
        newPassword: config.authPassword
      });
      assert(changeRes.json.success === true, 'auth change-password should succeed', changeRes.json);
      assert(changeRes.json.data.changed === true, 'auth change-password should return changed=true', changeRes.json);
      await expectTokenRejected(teacherToken, 'token after auth change-password');

      const oldLoginRes = await loginAttempt(config.originalPassword, [401]);
      assert(oldLoginRes.json.success === false, 'old password login should fail after auth change-password', oldLoginRes.json);

      const changedLogin = await login(config.authPassword);
      const restoreRes = await requestJson('POST', `${config.baseUrl}/api/auth/change-password`, changedLogin.accessToken, {
        currentPassword: config.authPassword,
        newPassword: config.originalPassword
      });
      assert(restoreRes.json.success === true, 'auth password restore should succeed', restoreRes.json);
      teacherLogin = await login(config.originalPassword);
      teacherToken = teacherLogin.accessToken;
    }, results);

    await step('student change-password compatibility success and restore', async () => {
      const switchStudentRes = await requestJson('POST', `${config.baseUrl}/api/auth/switch-role`, teacherToken, {
        targetRole: 'STUDENT'
      });
      studentToken = switchStudentRes.json.data.accessToken;

      const changeRes = await requestJson('POST', `${config.baseUrl}/api/student/change-password`, studentToken, {
        currentPassword: config.originalPassword,
        newPassword: config.studentPassword,
        confirmPassword: config.studentPassword
      });
      assert(changeRes.json.success === true, 'student change-password should succeed', changeRes.json);
      assert(changeRes.json.data === true, 'student change-password should return true', changeRes.json);
      await expectTokenRejected(studentToken, 'token after student change-password');

      const oldLoginRes = await loginAttempt(config.originalPassword, [401]);
      assert(oldLoginRes.json.success === false, 'old password login should fail after student change-password', oldLoginRes.json);

      const changedLogin = await login(config.studentPassword);
      const restoreRes = await requestJson('POST', `${config.baseUrl}/api/auth/change-password`, changedLogin.accessToken, {
        currentPassword: config.studentPassword,
        newPassword: config.originalPassword
      });
      assert(restoreRes.json.success === true, 'student password restore should succeed', restoreRes.json);
      teacherLogin = await login(config.originalPassword);
      teacherToken = teacherLogin.accessToken;
    }, results);

    await step('auth logout success revokes access and refresh tokens', async () => {
      const logoutLogin = await login(config.originalPassword);
      const logoutRes = await requestJson('POST', `${config.baseUrl}/api/auth/logout`, logoutLogin.accessToken, {
        refreshToken: logoutLogin.refreshToken
      });
      assert(logoutRes.json.success === true, 'logout should succeed', logoutRes.json);
      assert(logoutRes.json.data.revoked === true, 'logout should return revoked=true', logoutRes.json);
      await expectTokenRejected(logoutLogin.accessToken, 'access token after logout');

      const refreshRes = await requestJsonAllowStatus('POST', `${config.baseUrl}/api/auth/refresh`, null, {
        refreshToken: logoutLogin.refreshToken
      }, [401]);
      assert(refreshRes.json.success === false, 'refresh token after logout should be rejected', refreshRes.json);
    }, results);

    await step('notification delete-all-read isolated success', async () => {
      const notificationTeacher = await login(config.originalPassword);
      const notificationStudentSource = await login(config.originalPassword);
      const switchStudentRes = await requestJson('POST', `${config.baseUrl}/api/auth/switch-role`, notificationStudentSource.accessToken, {
        targetRole: 'STUDENT'
      });
      const freshStudentToken = switchStudentRes.json.data.accessToken;
      const unique = `IsolatedSmoke-${Date.now()}`;

      const sendRes = await requestJson('POST', `${config.baseUrl}/api/notifications/teacher/send-batch`, notificationTeacher.accessToken, [
        {
          type: 'course',
          title: `${unique}-A`,
          content: 'isolated notification A',
          studentId: config.userId,
          relatedId: config.examId,
          isRead: false
        },
        {
          type: 'exam',
          title: `${unique}-B`,
          content: 'isolated notification B',
          studentId: config.userId,
          relatedId: config.examId,
          isRead: false
        }
      ]);
      const ids = (sendRes.json.data || []).map(item => item.id).filter(Boolean);
      assert(ids.length === 2, 'batch notification should create two isolated notifications', sendRes.json.data);

      const markAllRes = await requestJson('PUT', `${config.baseUrl}/api/notifications/read-all`, freshStudentToken);
      assert(markAllRes.json.success === true, 'mark all read should succeed for isolated user', markAllRes.json);

      const beforeDeleteRes = await requestJson('GET', `${config.baseUrl}/api/notifications/student/all`, freshStudentToken);
      const beforeRows = beforeDeleteRes.json.data || [];
      assert(ids.every(id => beforeRows.some(item => item.id === id && item.read === true)), 'created notifications should be read before bulk delete', beforeRows);

      const deleteRes = await requestJson('DELETE', `${config.baseUrl}/api/notifications/delete-all-read`, freshStudentToken);
      assert(deleteRes.json.success === true, 'delete-all-read should succeed', deleteRes.json);

      const afterDeleteRes = await requestJson('GET', `${config.baseUrl}/api/notifications/student/all`, freshStudentToken);
      const afterRows = afterDeleteRes.json.data || [];
      assert(ids.every(id => !afterRows.some(item => item.id === id)), 'created read notifications should be deleted', afterRows);
    }, results);

    await step('teacher exam submission direct update and delete', async () => {
      const freshTeacher = await login(config.originalPassword);

      const detailRes = await requestJson('GET', `${config.baseUrl}/api/teacher/exams/submissions/${config.submissionId}`, freshTeacher.accessToken);
      assert(detailRes.json.success === true, 'seeded exam submission detail should succeed', detailRes.json);
      assert(detailRes.json.data.id === config.submissionId, 'seeded submission id mismatch', detailRes.json.data);

      const updateRes = await requestJson('PUT', `${config.baseUrl}/api/teacher/exams/submissions/${config.submissionId}`, freshTeacher.accessToken, {
        examId: config.examId,
        studentId: config.userId,
        content: '{"answers":{"1":"B"},"source":"isolated-side-effect-smoke"}',
        timeTaken: 45,
        graded: true,
        score: 88,
        teacherComment: 'Updated by isolated side-effect smoke'
      });
      assert(updateRes.json.success === true, 'direct submission update should succeed', updateRes.json);
      assert(updateRes.json.data.score === 88, 'direct submission update should persist score', updateRes.json.data);
      assert(updateRes.json.data.teacherComment === 'Updated by isolated side-effect smoke', 'direct submission update should persist comment', updateRes.json.data);

      const deleteRes = await requestJson('DELETE', `${config.baseUrl}/api/teacher/exams/submissions/${config.submissionId}`, freshTeacher.accessToken);
      assert(deleteRes.json.success === true, 'direct submission delete should succeed', deleteRes.json);

      const missingRes = await requestJsonAllowStatus('GET', `${config.baseUrl}/api/teacher/exams/submissions/${config.submissionId}`, freshTeacher.accessToken, undefined, [400, 404]);
      assert(missingRes.json.success === false, 'deleted submission should not be readable', missingRes.json);
    }, results);
  } finally {
    console.log('[CLEANUP] resetting isolated side-effect smoke seed');
    applySeed();
  }

  const failed = results.filter(result => !result.ok);
  if (failed.length > 0) {
    throw new Error(`Isolated side-effect smoke failed: ${failed.map(result => result.name).join(', ')}`);
  }
  console.log(`All isolated side-effect smoke checks passed: ${results.length}`);
}

run().catch(error => {
  console.error(error.stack || error.message);
  process.exit(1);
});
