const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('../major_assignment/node_modules/playwright');

const REPO_ROOT = path.resolve(__dirname, '..');
const FRONTEND_BASE_URL = (process.env.FRONTEND_BASE_URL || 'http://127.0.0.1:5500').replace(/\/$/, '');
const TEACHER_SESSION = path.join(REPO_ROOT, '.runtime-logs', 'teacher-session-agent-runtime.json');
const STUDENT_SESSION = path.join(REPO_ROOT, '.runtime-logs', 'student-session-agent-runtime.json');
const TEACHER_COURSE_ID = Number(process.env.AGENT_SMOKE_COURSE_ID || 91005);

function readSession(filePath) {
  const payload = JSON.parse(fs.readFileSync(filePath, 'utf8').replace(/^\uFEFF/, ''));
  return payload.sessionStorage || {
    token: payload.accessToken,
    refreshToken: payload.refreshToken,
    user: JSON.stringify(payload.user),
    userId: payload.user?.id,
    activeRole: payload.activeRole,
    role: payload.activeRole,
  };
}

function decodeJwtPayload(token) {
  if (!token || typeof token !== 'string') return null;
  const parts = token.split('.');
  if (parts.length < 2) return null;
  try {
    const normalized = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
    return JSON.parse(Buffer.from(padded, 'base64').toString('utf8'));
  } catch {
    return null;
  }
}

function assert(condition, message, details) {
  if (!condition) {
    const error = new Error(message);
    error.details = details;
    throw error;
  }
}

function assertFreshSession(session, label) {
  const payload = decodeJwtPayload(session.token);
  assert(session.token, `${label} session should include token`);
  if (payload?.exp) {
    const nowSeconds = Math.floor(Date.now() / 1000);
    assert(payload.exp > nowSeconds + 30, `${label} token expired or near expiry`, {
      expiresAt: new Date(payload.exp * 1000).toISOString(),
    });
  }
}

async function newAuthenticatedPage(browser, session, pageName) {
  const page = await browser.newPage({ viewport: { width: 1366, height: 900 } });
  await page.addInitScript((state) => {
    for (const [key, value] of Object.entries(state)) {
      if (value != null) {
        window.sessionStorage.setItem(key, String(value));
      }
    }
  }, session);
  await page.goto(`${FRONTEND_BASE_URL}/${pageName}`, { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('[data-agent-panel]', { timeout: 10000 });
  await page.waitForSelector('[data-agent-history-panel]', { timeout: 10000 });
  await installAgentSmokeHook(page);
  return page;
}

async function installAgentSmokeHook(page) {
  await page.waitForFunction(() => {
    const root = document.querySelector('[data-agent-panel]');
    return Boolean(root && root.agentChatPanel);
  }, { timeout: 10000 });
  await page.evaluate(() => {
    const root = document.querySelector('[data-agent-panel]');
    const panel = root?.agentChatPanel;
    if (!panel) {
      throw new Error('Agent chat panel instance was not initialized.');
    }
    if (window.__agentSmoke?.installed) {
      return;
    }
    window.__agentSmoke = {
      installed: true,
      renderCount: 0,
      lastPayload: null,
      lastError: null,
    };

    const originalRenderResponse = panel.renderResponse.bind(panel);
    panel.renderResponse = function patchedRenderResponse(payload) {
      window.__agentSmoke.lastPayload = payload;
      window.__agentSmoke.lastError = null;
      window.__agentSmoke.renderCount += 1;
      return originalRenderResponse(payload);
    };

    const originalAppend = panel.append.bind(panel);
    panel.append = function patchedAppend(role, html, state) {
      if (role === 'agent' && typeof html === 'string' && html.includes('text-danger')) {
        window.__agentSmoke.lastError = html;
      }
      return originalAppend(role, html, state);
    };
  });
}

async function resetAgentSmokeState(page) {
  await page.evaluate(() => {
    if (!window.__agentSmoke) {
      window.__agentSmoke = { installed: false, renderCount: 0, lastPayload: null, lastError: null };
      return;
    }
    window.__agentSmoke.lastPayload = null;
    window.__agentSmoke.lastError = null;
    window.__agentSmoke.renderCount = 0;
  });
}

async function submitAgentCommand(page, message) {
  await resetAgentSmokeState(page);
  const requestPromise = page.waitForRequest(
    request => request.url().includes('/api/agent/chat') && request.method() === 'POST',
    { timeout: 10000 }
  );
  await page.fill('[data-agent-input]', message);
  await page.click('[data-agent-submit]');
  await requestPromise;
  await page.waitForFunction(() => {
    return Boolean(window.__agentSmoke?.lastPayload || window.__agentSmoke?.lastError);
  }, { timeout: 45000 });
  const state = await page.evaluate(() => ({
    renderCount: window.__agentSmoke?.renderCount || 0,
    lastPayload: window.__agentSmoke?.lastPayload || null,
    lastError: window.__agentSmoke?.lastError || null,
  }));
  assert(!state.lastError, 'Agent chat request should not render an error state', state);
  assert(state.lastPayload, 'Agent chat request should render a payload in the page', state);
  return state.lastPayload;
}

async function waitForDataRendered(page, expectedTexts = []) {
  await page.waitForFunction((texts) => {
    const messageBodies = Array.from(document.querySelectorAll('.agent-message-agent .agent-message-body'));
    if (!messageBodies.length) {
      return false;
    }

    const combinedText = messageBodies.map(node => node.innerText || '').join('\n');
    const hasDataCard = document.querySelector('.agent-data-result');
    if (hasDataCard) {
      return true;
    }

    return (texts || []).some(text => text && combinedText.includes(text));
  }, expectedTexts, { timeout: 10000 });
}

async function confirmLatestAction(page) {
  await page.waitForSelector('[data-agent-confirm]', { timeout: 10000 });
  const responsePromise = page.waitForResponse(
    response => response.url().includes('/api/agent/actions/') && response.url().includes('/confirm'),
    { timeout: 20000 }
  );
  await page.click('[data-agent-confirm]');
  const response = await responsePromise;
  const body = await response.json();
  assert(response.ok(), 'Agent confirm request should return HTTP 2xx', {
    status: response.status(),
    body,
  });
  assert(body.success !== false, 'Agent confirm request should return success envelope', body);
  return body.data || body;
}

async function chatWithToken(token, message, sessionId) {
  const response = await fetch('http://localhost:8080/api/agent/chat', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ message, sessionId }),
  });
  const body = await response.json();
  assert(response.ok, 'Agent API chat should return HTTP 2xx', {
    status: response.status,
    body,
  });
  assert(body.success !== false, 'Agent API chat should return success envelope', body);
  return body.data || body;
}

async function getTeacherAssignments(token, params) {
  const url = new URL('http://localhost:8080/api/teacher/assignments');
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, String(value));
    }
  }
  const response = await fetch(url, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
      'X-Role-Context': 'TEACHER',
    },
  });
  const body = await response.json();
  assert(response.ok, 'Teacher assignment list should return HTTP 2xx', {
    status: response.status,
    body,
  });
  assert(body.success !== false, 'Teacher assignment list should return success envelope', body);
  return body.data || body;
}

async function getTeacherExams(token, params) {
  const url = new URL('http://localhost:8080/api/teacher/exams');
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, String(value));
    }
  }
  const response = await fetch(url, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
      'X-Role-Context': 'TEACHER',
    },
  });
  const body = await response.json();
  assert(response.ok, 'Teacher exam list should return HTTP 2xx', {
    status: response.status,
    body,
  });
  assert(body.success !== false, 'Teacher exam list should return success envelope', body);
  return body.data || body;
}

async function getStudentAllNotifications(token, params = {}) {
  const url = new URL('http://localhost:8080/api/notifications/student/all');
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, String(value));
    }
  }
  const response = await fetch(url, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
      'X-Role-Context': 'STUDENT',
    },
  });
  const body = await response.json();
  assert(response.ok, 'Student notification list should return HTTP 2xx', {
    status: response.status,
    body,
  });
  assert(body.success !== false, 'Student notification list should return success envelope', body);
  return body.data || [];
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
  const teacherSession = readSession(TEACHER_SESSION);
  const studentSession = readSession(STUDENT_SESSION);
  const results = [];
  const browser = await chromium.launch({ headless: true });
  let publishedAssignmentId;
  let publishedAssignmentTitle;
  let publishedExamId;
  let publishedExamTitle;
  let studentTargetAssignmentId;
  let studentSubmissionContent;
  let studentExamSubmissionId;
  let agentNotificationTitle;
  let agentNotificationId;

  try {
    await runStep(results, 'teacher and student browser sessions are fresh', async () => {
      assertFreshSession(teacherSession, 'teacher');
      assertFreshSession(studentSession, 'student');
    });

    await runStep(results, 'teacher Agent panel renders and read-only command returns DATA', async () => {
      const page = await newAuthenticatedPage(browser, teacherSession, 'teacher-ai-tools.html');
      const payload = await submitAgentCommand(page, '查看我的课程');
      assert(payload.responseType === 'DATA', 'teacher read-only command should return DATA', payload);
      await page.waitForSelector('.agent-data-result', { timeout: 10000 });
      await page.close();
    });

    await runStep(results, 'teacher Agent publish command renders preview and confirms execution', async () => {
      const page = await newAuthenticatedPage(browser, teacherSession, 'teacher-ai-tools.html');
      const title = `AgentBrowser作业${Date.now()}`;
      publishedAssignmentTitle = title;
      const payload = await submitAgentCommand(
        page,
        `发布作业，课程ID ${TEACHER_COURSE_ID}，标题是${title}，截止2026-12-31 23:59:00，满分100`
      );
      assert(payload.responseType === 'ACTION_PREVIEW', 'publish command should return ACTION_PREVIEW', payload);
      await page.waitForSelector('.agent-action-card', { timeout: 10000 });
      const cardText = await page.locator('.agent-action-card').last().innerText();
      assert(cardText.includes(title), 'preview card should include generated assignment title', cardText);
      const result = await confirmLatestAction(page);
      assert(result.status === 'EXECUTED', 'confirm should execute action', result);
      publishedAssignmentId = result.result?.assignment?.id;
      assert(publishedAssignmentId, 'confirmed publish result should include assignment id', result);
      await page.waitForSelector('.agent-data-result', { timeout: 10000 });
      await page.close();
    });

    await runStep(results, 'new Agent-published assignment appears in teacher assignment list', async () => {
      const payload = await getTeacherAssignments(teacherSession.token, {
        page: 1,
        size: 10,
        keyword: publishedAssignmentTitle,
      });
      const assignments = payload.content || payload.assignments || [];
      assert(
        assignments.some(item =>
          Number(item.id) === Number(publishedAssignmentId) && item.title === publishedAssignmentTitle
        ),
        'teacher assignment list should include the Agent-published assignment',
        payload
      );
    });

    await runStep(results, 'teacher Agent exam publish command renders preview and confirms execution', async () => {
      const page = await newAuthenticatedPage(browser, teacherSession, 'teacher-ai-tools.html');
      publishedExamTitle = `AgentBrowser${Date.now()}考试`;
      const payload = await submitAgentCommand(
        page,
        `发布考试，课程ID ${TEACHER_COURSE_ID}，标题是${publishedExamTitle}，开始时间2026-12-30 09:00:00，结束时间2026-12-30 10:30:00，时长90分钟`
      );
      assert(payload.responseType === 'ACTION_PREVIEW', 'exam publish command should return ACTION_PREVIEW', payload);
      await page.waitForSelector('.agent-action-card', { timeout: 10000 });
      const cardText = await page.locator('.agent-action-card').last().innerText();
      assert(cardText.includes(publishedExamTitle), 'exam preview card should include generated exam title', cardText);
      const result = await confirmLatestAction(page);
      assert(result.status === 'EXECUTED', 'exam publish confirm should execute action', result);
      publishedExamId = result.result?.exam?.id;
      assert(publishedExamId, 'confirmed exam publish result should include exam id', result);
      await page.close();
    });

    await runStep(results, 'new Agent-published exam appears in teacher exam list', async () => {
      const payload = await getTeacherExams(teacherSession.token, {
        page: 1,
        size: 20,
        sortBy: 'id',
        order: 'DESC',
      });
      const exams = payload.content || payload.exams || [];
      assert(
        exams.some(item =>
          Number(item.id) === Number(publishedExamId) && item.title === publishedExamTitle
        ),
        'teacher exam list should include the Agent-published exam',
        payload
      );
    });

    await runStep(results, 'student Agent submit exam command renders preview and confirms execution', async () => {
      const page = await newAuthenticatedPage(browser, studentSession, 'student-ai-assistant.html');
      const payload = await submitAgentCommand(
        page,
        `提交考试ID ${publishedExamId}，用时45分钟，答案是1:A,2:B`
      );
      assert(payload.responseType === 'ACTION_PREVIEW', 'student exam submit command should return ACTION_PREVIEW', payload);
      await page.waitForSelector('.agent-action-card', { timeout: 10000 });
      const cardText = await page.locator('.agent-action-card').last().innerText();
      assert(cardText.includes(String(publishedExamId)), 'exam submit preview should include resolved exam id', cardText);
      assert(cardText.includes('45'), 'exam submit preview should include time taken', cardText);
      const result = await confirmLatestAction(page);
      assert(result.status === 'EXECUTED', 'student exam confirm should execute exam submission', result);
      studentExamSubmissionId = result.result?.submission?.id;
      assert(studentExamSubmissionId, 'confirmed exam submit result should include submission id', result);
      assert(Number(result.result?.submission?.timeTaken) === 45, 'confirm result should include submitted time taken', result);
      await page.close();
    });

    await runStep(results, 'teacher Agent can verify submitted exam record', async () => {
      const payload = await chatWithToken(
        teacherSession.token,
        `查看考试ID ${publishedExamId}提交记录`
      );
      assert(payload.responseType === 'DATA', 'teacher exam submission lookup should return DATA', payload);
      const submissions = payload.data?.submissions || [];
      assert(
        submissions.some(item => Number(item.id) === Number(studentExamSubmissionId)),
        'teacher exam submission lookup should include browser-submitted exam record',
        payload
      );
    });

    await runStep(results, 'student Agent panel renders and pending assignment command returns DATA', async () => {
      const page = await newAuthenticatedPage(browser, studentSession, 'student-ai-assistant.html');
      const payload = await submitAgentCommand(page, '我有哪些待提交作业');
      assert(payload.responseType === 'DATA', 'student pending assignment command should return DATA', payload);
      const pendingItems = payload.data?.pendingAssignments?.content || [];
      const pendingAssignment = pendingItems.find(item => item?.id && !item?.submission);
      assert(pendingAssignment, 'student pending assignment response should include an unsubmitted assignment', payload);
      studentTargetAssignmentId = pendingAssignment.id;
      await waitForDataRendered(page, [
        String(studentTargetAssignmentId),
        pendingAssignment.title,
        '待提交'
      ]);
      await page.close();
    });

    await runStep(results, 'student Agent submit assignment command renders preview and confirms execution', async () => {
      const page = await newAuthenticatedPage(browser, studentSession, 'student-ai-assistant.html');
      studentSubmissionContent = `浏览器Agent提交${Date.now()}`;
      const payload = await submitAgentCommand(
        page,
        `提交作业ID ${studentTargetAssignmentId}，内容是${studentSubmissionContent}`
      );
      assert(payload.responseType === 'ACTION_PREVIEW', 'student submit command should return ACTION_PREVIEW', payload);
      await page.waitForSelector('.agent-action-card', { timeout: 10000 });
      const cardText = await page.locator('.agent-action-card').last().innerText();
      assert(cardText.includes(String(studentTargetAssignmentId)), 'submit preview should include assignment id', cardText);
      assert(cardText.includes(studentSubmissionContent), 'submit preview should include submission content', cardText);
      const result = await confirmLatestAction(page);
      assert(result.status === 'EXECUTED', 'student confirm should execute assignment submission', result);
      assert(result.result?.submission?.content === studentSubmissionContent, 'confirm result should include submitted content', result);
      await page.close();
    });

    await runStep(results, 'teacher Agent can verify submitted assignment content', async () => {
      const payload = await chatWithToken(
        teacherSession.token,
        `查看作业ID ${studentTargetAssignmentId}提交记录`
      );
      assert(payload.responseType === 'DATA', 'teacher submission lookup should return DATA', payload);
      const submissions = payload.data?.submissions || [];
      assert(
        submissions.some(item => item.content === studentSubmissionContent),
        'teacher submission lookup should include browser-submitted content',
        payload
      );
    });

    await runStep(results, 'teacher Agent send notification command renders preview and confirms execution', async () => {
      const page = await newAuthenticatedPage(browser, teacherSession, 'teacher-ai-tools.html');
      agentNotificationTitle = `Agent通知${Date.now()}`;
      const payload = await submitAgentCommand(
        page,
        `给学生ID 42发送通知，标题是${agentNotificationTitle}，内容是请查看新的课程提醒，类型是course`
      );
      assert(payload.responseType === 'ACTION_PREVIEW', 'send notification command should return ACTION_PREVIEW', payload);
      assert(payload.actionPreview?.intent === 'SEND_NOTIFICATION', 'send notification preview should use SEND_NOTIFICATION intent', payload);
      await page.waitForSelector('.agent-action-card', { timeout: 10000 });
      const cardText = await page.locator('.agent-action-card').last().innerText();
      assert(cardText.includes(agentNotificationTitle), 'notification preview should include generated title', cardText);
      const result = await confirmLatestAction(page);
      assert(result.status === 'EXECUTED', 'notification send confirm should execute action', result);
      agentNotificationId = result.result?.notification?.id;
      assert(agentNotificationId, 'confirmed notification send result should include notification id', result);
      await page.close();
    });

    await runStep(results, 'student can see Agent-sent notification as unread', async () => {
      const notifications = await getStudentAllNotifications(studentSession.token, { filter: 'all' });
      const notification = notifications.find(item =>
        Number(item.id) === Number(agentNotificationId) && item.title === agentNotificationTitle
      );
      assert(notification, 'student notification list should include Agent-sent notification', notifications);
      assert(notification.read === false, 'Agent-sent notification should initially be unread', notification);
    });

    await runStep(results, 'student Agent mark notification read command renders preview and confirms execution', async () => {
      const page = await newAuthenticatedPage(browser, studentSession, 'student-ai-assistant.html');
      const payload = await submitAgentCommand(page, `把通知ID ${agentNotificationId}标为已读`);
      assert(payload.responseType === 'ACTION_PREVIEW', 'mark notification read command should return ACTION_PREVIEW', payload);
      assert(payload.actionPreview?.intent === 'MARK_NOTIFICATION_READ', 'mark notification read preview should use MARK_NOTIFICATION_READ intent', payload);
      await page.waitForSelector('.agent-action-card', { timeout: 10000 });
      const cardText = await page.locator('.agent-action-card').last().innerText();
      assert(cardText.includes(String(agentNotificationId)), 'mark-read preview should include notification id', cardText);
      const result = await confirmLatestAction(page);
      assert(result.status === 'EXECUTED', 'mark notification read confirm should execute action', result);
      assert(result.result?.notificationId === Number(agentNotificationId), 'mark-read result should include notification id', result);
      await page.close();
    });

    await runStep(results, 'student notification becomes read after Agent confirmation', async () => {
      const notifications = await getStudentAllNotifications(studentSession.token, { filter: 'all' });
      const notification = notifications.find(item => Number(item.id) === Number(agentNotificationId));
      assert(notification, 'student notification list should still include Agent-sent notification', notifications);
      assert(notification.read === true, 'Agent mark-read should update notification read state', notification);
    });

  } finally {
    await browser.close();
  }

  console.log(`Agent frontend browser smoke passed: ${results.length}`);
})().catch(error => {
  console.error(error.message);
  if (error.details !== undefined) {
    console.error(JSON.stringify(error.details, null, 2));
  }
  process.exit(1);
});
