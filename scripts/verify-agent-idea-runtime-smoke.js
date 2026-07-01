const fs = require('node:fs');
const path = require('node:path');

const REPO_ROOT = path.resolve(__dirname, '..');
const DEFAULT_TEACHER_SESSION = path.join(REPO_ROOT, '.runtime-logs', 'teacher-session-agent-runtime.json');
const DEFAULT_STUDENT_SESSION = path.join(REPO_ROOT, '.runtime-logs', 'student-session-agent-runtime.json');
const GATEWAY_BASE_URL = (process.env.GATEWAY_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const AGENT_BASE_URL = (process.env.AGENT_BASE_URL || 'http://localhost:8092').replace(/\/$/, '');
let teacherCourseId = Number(process.env.AGENT_SMOKE_COURSE_ID || 91005);

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf8').replace(/^\uFEFF/, ''));
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

function assertFreshSession(session, label) {
  const token = session?.accessToken || session?.sessionStorage?.token;
  const payload = decodeJwtPayload(token);
  assert(token, `${label} should include an access token`);
  if (payload?.exp) {
    const nowSeconds = Math.floor(Date.now() / 1000);
    assert(payload.exp > nowSeconds + 30, `${label} token expired or near expiry`, {
      expiresAt: new Date(payload.exp * 1000).toISOString()
    });
  }
}

function token(session) {
  return session.accessToken || session.sessionStorage?.token;
}

async function sleep(ms) {
  await new Promise(resolve => setTimeout(resolve, ms));
}

async function requestJson(method, url, accessToken, body, attempts = 5) {
  let lastError;
  for (let attempt = 1; attempt <= attempts; attempt += 1) {
    try {
      const headers = {};
      if (accessToken) {
        headers.Authorization = `Bearer ${accessToken}`;
      }
      if (body !== undefined) {
        headers['Content-Type'] = 'application/json';
      }
      const response = await fetch(url, {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body)
      });
      const text = await response.text();
      const json = text ? JSON.parse(text) : null;
      if (response.status === 503 && attempt < attempts) {
        lastError = new Error(`${method} ${url} returned 503: ${text.slice(0, 300)}`);
        await sleep(1500);
        continue;
      }
      assert(response.ok, `${method} ${url} should return HTTP 2xx`, {
        status: response.status,
        body: text.slice(0, 500)
      });
      assert(json?.success !== false, `${method} ${url} should return a successful ResponseResult`, json);
      return json?.data ?? json;
    } catch (error) {
      lastError = error;
      if (attempt >= attempts) {
        break;
      }
      await sleep(1500);
    }
  }
  throw lastError;
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

(async () => {
  const teacherSessionFile = process.argv[2] ? path.resolve(process.argv[2]) : DEFAULT_TEACHER_SESSION;
  const studentSessionFile = process.argv[3] ? path.resolve(process.argv[3]) : DEFAULT_STUDENT_SESSION;
  const teacherSession = readJson(teacherSessionFile);
  const studentSession = readJson(studentSessionFile);
  const teacherToken = token(teacherSession);
  const studentToken = token(studentSession);
  const results = [];
  let teacherSessionId;
  let preview;
  let disambiguationTarget;

  await runStep(results, 'fresh teacher and student sessions are available', async () => {
    assertFreshSession(teacherSession, 'teacher session');
    assertFreshSession(studentSession, 'student session');
  });

  await runStep(results, 'agent service health is UP', async () => {
    const health = await requestJson('GET', `${AGENT_BASE_URL}/actuator/health`, null, undefined, 2);
    assert(health?.status === 'UP', 'agent-service actuator health should be UP', health);
  });

  await runStep(results, 'teacher runtime has a course assignment that can be disambiguated', async () => {
    const coursesPage = await requestJson('GET', `${GATEWAY_BASE_URL}/api/teacher/courses?page=1&size=100`, teacherToken);
    const assignmentsPage = await requestJson('GET', `${GATEWAY_BASE_URL}/api/teacher/course-assignments?page=1&size=100`, teacherToken);
    const courses = Array.isArray(coursesPage?.content) ? coursesPage.content : [];
    const assignments = Array.isArray(assignmentsPage?.content) ? assignmentsPage.content : [];
    const courseNameCounts = new Map();
    for (const course of courses) {
      if (!course?.courseName) {
        continue;
      }
      courseNameCounts.set(course.courseName, (courseNameCounts.get(course.courseName) || 0) + 1);
    }
    disambiguationTarget = assignments.find(item =>
      item?.courseId
      && item?.courseName
      && item?.className
      && item?.semester
      && (courseNameCounts.get(item.courseName) || 0) > 1
    ) || assignments.find(item =>
      item?.courseId
      && item?.courseName
      && item?.className
      && item?.semester
    );
    assert(disambiguationTarget, 'teacher should have at least one course assignment with course, class, and semester', {
      courses,
      assignments
    });
    if (!process.env.AGENT_SMOKE_COURSE_ID) {
      teacherCourseId = disambiguationTarget.courseId;
    }
  });

  await runStep(results, 'teacher Agent resolves semester, course, and class into an action preview', async () => {
    const response = await requestJson('POST', `${GATEWAY_BASE_URL}/api/agent/chat`, teacherToken, {
      message: `请在 ${disambiguationTarget.semester}《${disambiguationTarget.courseName}》${disambiguationTarget.className} 这门课下发布作业：标题“Java基础”，中等难度，2道题，满分100分，截止时间 2026-06-17 23:59。`
    });
    assert(response.responseType === 'ACTION_PREVIEW', 'semester + course + class command should return ACTION_PREVIEW', response);
    assert(
      response.actionPreview?.preview?.courseId === disambiguationTarget.courseId
        && response.actionPreview?.preview?.courseName === disambiguationTarget.courseName
        && response.actionPreview?.preview?.className === disambiguationTarget.className
        && response.actionPreview?.preview?.semester === disambiguationTarget.semester,
      'action preview should resolve the target course from semester and class context',
      response
    );
  });

  await runStep(results, 'teacher Agent read-only query returns DATA without preview', async () => {
    const response = await requestJson('POST', `${GATEWAY_BASE_URL}/api/agent/chat`, teacherToken, {
      message: '查看我的课程'
    });
    teacherSessionId = response.sessionId;
    assert(response.responseType === 'DATA', 'teacher course query should return DATA', response);
    assert(!response.actionPreview, 'read-only teacher query should not create an action preview', response);
    assert(response.data != null, 'teacher course query should include data', response);
  });

  await runStep(results, 'teacher Agent publish assignment returns ACTION_PREVIEW', async () => {
    const uniqueTitle = `AgentRuntime作业${Date.now()}`;
    const response = await requestJson('POST', `${GATEWAY_BASE_URL}/api/agent/chat`, teacherToken, {
      sessionId: teacherSessionId,
      message: `发布作业，课程ID ${teacherCourseId}，标题是${uniqueTitle}，截止2026-12-31 23:59:00，满分100`
    });
    preview = response.actionPreview;
    assert(response.responseType === 'ACTION_PREVIEW', 'publish assignment should return ACTION_PREVIEW', response);
    assert(preview?.intent === 'PUBLISH_ASSIGNMENT', 'preview should be for PUBLISH_ASSIGNMENT', preview);
    assert(preview?.actionId, 'preview should include actionId', preview);
    assert(preview?.idempotencyKey, 'preview should include idempotencyKey', preview);
  });

  await runStep(results, 'teacher Agent confirm executes and remains idempotent', async () => {
    const first = await requestJson('POST', `${GATEWAY_BASE_URL}/api/agent/actions/${preview.actionId}/confirm`, teacherToken, {
      idempotencyKey: preview.idempotencyKey
    });
    assert(first.status === 'EXECUTED', 'first confirmation should execute', first);
    assert(first.result?.assignment, 'confirmation should return the created assignment', first);

    const second = await requestJson('POST', `${GATEWAY_BASE_URL}/api/agent/actions/${preview.actionId}/confirm`, teacherToken, {
      idempotencyKey: preview.idempotencyKey
    });
    assert(second.status === 'EXECUTED', 'second confirmation should stay executed', second);
  });

  await runStep(results, 'teacher Agent session detail includes messages and actions', async () => {
    const session = await requestJson('GET', `${GATEWAY_BASE_URL}/api/agent/sessions/${teacherSessionId}`, teacherToken);
    assert(Array.isArray(session.messages) && session.messages.length >= 4, 'session should include chat messages', session);
    assert(Array.isArray(session.actions) && session.actions.some(action => action.actionId === preview.actionId || action.id === preview.actionId), 'session should include the confirmed action', session);
  });

  await runStep(results, 'student Agent pending assignment query returns DATA without preview', async () => {
    const response = await requestJson('POST', `${GATEWAY_BASE_URL}/api/agent/chat`, studentToken, {
      message: '我有哪些待提交作业'
    });
    assert(response.responseType === 'DATA', 'student pending assignment query should return DATA', response);
    assert(!response.actionPreview, 'read-only student query should not create an action preview', response);
  });

  console.log(`Agent IDEA runtime smoke passed: ${results.length}`);
})().catch(error => {
  console.error(error.message);
  if (error.details !== undefined) {
    console.error(JSON.stringify(error.details, null, 2));
  }
  process.exit(1);
});
