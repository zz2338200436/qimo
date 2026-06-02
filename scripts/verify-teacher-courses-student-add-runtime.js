const { execFileSync } = require('child_process');
const path = require('path');

const repoRoot = path.resolve(__dirname, '..');
const seedScript = path.resolve(repoRoot, 'scripts/seed-isolated-teacher-course-student-assign.ps1');

const config = {
  baseUrl: (process.env.GATEWAY_BASE_URL || process.argv[2] || 'http://localhost:8080').replace(/\/$/, ''),
  redisContainer: process.env.REDIS_CONTAINER || 'qimo-redis',
  username: process.env.ISOLATED_TEACHER_CLASS_ASSIGN_USERNAME || 'isolated_teacher_class_assign',
  password: process.env.ISOLATED_TEACHER_CLASS_ASSIGN_PASSWORD || 'Teach1234',
  teacherUserId: Number(process.env.ISOLATED_TEACHER_CLASS_ASSIGN_USER_ID || 90052),
  targetClassId: Number(process.env.ISOLATED_TEACHER_CLASS_ASSIGN_TARGET_CLASS_ID || 90052),
  managedSourceClassId: Number(process.env.ISOLATED_TEACHER_CLASS_ASSIGN_MANAGED_CLASS_ID || 90053),
  unmanagedSourceClassId: Number(process.env.ISOLATED_TEACHER_CLASS_ASSIGN_UNMANAGED_CLASS_ID || 90054),
  unassignedStudentUsername: process.env.ISOLATED_TEACHER_CLASS_ASSIGN_UNASSIGNED_USERNAME || 'isolated_student_unassigned',
  managedStudentUsername: process.env.ISOLATED_TEACHER_CLASS_ASSIGN_MANAGED_USERNAME || 'isolated_student_managed',
  unmanagedStudentUsername: process.env.ISOLATED_TEACHER_CLASS_ASSIGN_UNMANAGED_USERNAME || 'isolated_student_unmanaged'
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

async function requestJsonAllowStatus(method, url, token, body, expectedStatuses) {
  const headers = {};
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
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

async function requestJson(method, url, token, body) {
  return requestJsonAllowStatus(method, url, token, body, [200, 201]);
}

async function login() {
  const captchaUrl = `${config.baseUrl}/api/auth/captcha?timestamp=${Date.now()}`;
  const captchaResponse = await fetch(captchaUrl);
  assert(captchaResponse.ok, `GET ${captchaUrl} failed with ${captchaResponse.status}`);
  await captchaResponse.arrayBuffer();
  const captchaKey = captchaResponse.headers.get('x-captcha-key');
  assert(captchaKey, 'captcha response should include X-Captcha-Key');
  const captcha = readCaptchaCode(captchaKey);

  const { json } = await requestJson('POST', `${config.baseUrl}/api/auth/login`, null, {
    username: config.username,
    password: config.password,
    captcha,
    captchaKey
  });
  assert(json.success === true, 'isolated teacher login should succeed', json);
  assert(json.data?.accessToken, 'isolated teacher login should return accessToken', json);
  assert(json.data?.user?.id === config.teacherUserId, 'isolated teacher login should resolve fixture user', json.data?.user);
  return json.data.accessToken;
}

function extractRows(json) {
  if (Array.isArray(json?.data)) {
    return json.data;
  }
  if (Array.isArray(json?.data?.content)) {
    return json.data.content;
  }
  return [];
}

async function listClassStudents(token, classId) {
  const result = await requestJson('GET', `${config.baseUrl}/api/teacher/classes/${classId}/students`, token);
  assert(result.json.success === true, `class ${classId} student list should succeed`, result);
  return extractRows(result.json);
}

async function addStudentToClass(token, classId, requestBody, expectedStatuses = [200]) {
  return requestJsonAllowStatus(
    'POST',
    `${config.baseUrl}/api/teacher/classes/${classId}/students`,
    token,
    requestBody,
    expectedStatuses
  );
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
  console.log('[SETUP] applying isolated teacher class-assign seed');
  applySeed();

  const token = await login();

  await step('unassigned student can be added by username', async () => {
    const addRes = await addStudentToClass(token, config.targetClassId, {
      studentIdentifier: config.unassignedStudentUsername
    });
    assert(addRes.json.success === true, 'adding unassigned student should succeed', addRes.json);
    assert(addRes.json.data?.classId === config.targetClassId, 'add response should target requested class', addRes.json.data);

    const targetStudents = await listClassStudents(token, config.targetClassId);
    assert(
      targetStudents.some(student => student.username === config.unassignedStudentUsername),
      'target class should include the newly added unassigned student',
      targetStudents
    );
  }, results);

  await step('managed student requires confirm before replace', async () => {
    const confirmRes = await addStudentToClass(token, config.targetClassId, {
      studentIdentifier: config.managedStudentUsername
    });
    assert(confirmRes.json.success === true, 'managed student confirm probe should return legacy success envelope', confirmRes.json);
    assert(confirmRes.json.data?.needConfirm === true, 'managed student should require confirm before replace', confirmRes.json.data);

    const sourceStudents = await listClassStudents(token, config.managedSourceClassId);
    assert(
      sourceStudents.some(student => student.username === config.managedStudentUsername),
      'managed student should remain in original managed class before force replace',
      sourceStudents
    );
  }, results);

  await step('managed student can be force moved and restored', async () => {
    const moveRes = await addStudentToClass(token, config.targetClassId, {
      studentIdentifier: config.managedStudentUsername,
      forceReplace: true
    });
    assert(moveRes.json.success === true, 'force replace should succeed for teacher-manageable student', moveRes.json);

    const targetStudents = await listClassStudents(token, config.targetClassId);
    assert(
      targetStudents.some(student => student.username === config.managedStudentUsername),
      'target class should include force-moved student',
      targetStudents
    );

    const sourceAfterMove = await listClassStudents(token, config.managedSourceClassId);
    assert(
      sourceAfterMove.every(student => student.username !== config.managedStudentUsername),
      'managed source class should no longer include moved student after force replace',
      sourceAfterMove
    );

    const restoreRes = await addStudentToClass(token, config.managedSourceClassId, {
      studentIdentifier: config.managedStudentUsername,
      forceReplace: true
    });
    assert(restoreRes.json.success === true, 'restoring force-moved student should succeed', restoreRes.json);

    const sourceAfterRestore = await listClassStudents(token, config.managedSourceClassId);
    assert(
      sourceAfterRestore.some(student => student.username === config.managedStudentUsername),
      'managed source class should include restored student',
      sourceAfterRestore
    );
  }, results);

  await step('student in unmanaged class is rejected', async () => {
    const rejected = await addStudentToClass(token, config.targetClassId, {
      studentIdentifier: config.unmanagedStudentUsername
    }, [403]);
    assert(rejected.json.success === false, 'unmanaged student move should be rejected', rejected.json);
    assert(rejected.json.message === '无权移动该学生所在班级', 'unmanaged student rejection message should stay stable', rejected.json);

    const unmanagedSourceStudents = await listClassStudents(token, config.unmanagedSourceClassId).catch(() => null);
    assert(unmanagedSourceStudents === null, 'teacher should not be able to list unmanaged source class students directly');
  }, results);

  console.log(`Teacher class student assign runtime verifier passed: ${results.length}`);
}

run().catch(error => {
  console.error(error.message);
  if (error.details !== undefined) {
    console.error(JSON.stringify(error.details, null, 2));
  }
  process.exit(1);
});
