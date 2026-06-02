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
  await page.waitForTimeout(3000);
  return page;
}

(async () => {
  const teacherSession = readJson(teacherSessionFile);
  assertFreshSession(teacherSession.sessionStorage, 'teacherSessionFile');
  requireRole(teacherSession, 'TEACHER', 'teacherSessionFile');

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  let page;
  const knowledgeResponses = [];

  try {
    page = await createAuthedPage(
      context,
      teacherSession.sessionStorage || {},
      'http://localhost:5500/teacher-assignments.html'
    );

    page.on('response', response => {
      if (response.url().includes('/api/teacher/knowledge-points/course/')) {
        knowledgeResponses.push({
          url: response.url(),
          status: response.status()
        });
      }
    });

    await page.evaluate(async () => {
      if (typeof window.loadTeacherAssignmentCourses === 'function') {
        await window.loadTeacherAssignmentCourses();
      }
    });

    const knowledgeSource = await page.evaluate(async () => {
      const coursesResponse = await window.fetch('/api/teacher/courses?page=1&size=100', {
        method: 'GET',
        credentials: 'include'
      });
      const coursesPayload = await coursesResponse.json();
      const courses = coursesPayload?.data?.content || [];

      for (const course of courses) {
        const knowledgeResponse = await window.fetch(`/api/teacher/knowledge-points/course/${course.id}`, {
          method: 'GET',
          credentials: 'include'
        });
        const result = await knowledgeResponse.json();
        if (result?.success && Array.isArray(result.data) && result.data.length > 0) {
          return {
            courseId: course.id,
            courseName: course.courseName,
            knowledgePointCount: result.data.length
          };
        }
      }

      return null;
    });

    assert(
      knowledgeSource && knowledgeSource.courseId,
      'teacher assignments runtime verifier requires at least one teacher course with knowledge points',
      knowledgeSource
    );

    await page.evaluate(async ({ courseId }) => {
      const assignmentCourse = document.getElementById('add-assignment-course');
      if (assignmentCourse) {
        assignmentCourse.value = String(courseId);
      }
      await window.loadKnowledgePoints(String(courseId), 'assignment-knowledge-points');

      const examCourse = document.getElementById('exam-course');
      if (examCourse) {
        examCourse.value = String(courseId);
      }
      await window.loadKnowledgePoints(String(courseId), 'exam-knowledge-points');
    }, knowledgeSource);

    await page.waitForFunction(expectedCount => {
      const invalidFragments = ['当前环境暂未提供知识点接口', '知识点暂不可用，请稍后重试', '加载知识点失败，请稍后重试'];
      const selectors = ['assignment-knowledge-points', 'exam-knowledge-points'];
      return selectors.every(id => {
        const select = document.getElementById(id);
        if (!select || select.options.length < expectedCount) {
          return false;
        }
        return !Array.from(select.options).some(option =>
          invalidFragments.some(fragment => (option.textContent || '').includes(fragment))
        );
      });
    }, knowledgeSource.knowledgePointCount, { timeout: 15000 });

    const state = await page.evaluate(() => ({
      assignmentKnowledgePoints: Array.from(document.getElementById('assignment-knowledge-points')?.options || []).map(option => ({
        value: option.value,
        text: option.textContent.trim()
      })),
      examKnowledgePoints: Array.from(document.getElementById('exam-knowledge-points')?.options || []).map(option => ({
        value: option.value,
        text: option.textContent.trim()
      }))
    }));

    assert(
      knowledgeResponses.some(item => item.status === 200),
      'teacher-assignments page should request /api/teacher/knowledge-points/course/{courseId} successfully for publish knowledge point dropdowns',
      knowledgeResponses
    );

    assert(
      state.assignmentKnowledgePoints.length >= knowledgeSource.knowledgePointCount,
      'assignment publish knowledge point dropdown should populate from real course knowledge points',
      state
    );

    assert(
      state.examKnowledgePoints.length >= knowledgeSource.knowledgePointCount,
      'exam publish knowledge point dropdown should populate from real course knowledge points',
      state
    );

    console.log('teacher assignments knowledge points runtime verifier passed');
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
