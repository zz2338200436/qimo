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
  await page.waitForTimeout(2000);
  return page;
}

async function activateTool(page, toolId) {
  await page.evaluate(id => {
    if (typeof window.backToTools === 'function') {
      window.backToTools();
    }
    window.activateTool(id);
  }, toolId);
  await page.waitForTimeout(200);
}

async function waitForResultCount(page, expectedMinimum) {
  await page.waitForFunction(min => {
    const node = document.getElementById('result-count');
    return node && Number(node.textContent || '0') >= min;
  }, expectedMinimum, { timeout: 15000 });
}

async function collectApiResponses(page, action) {
  const responses = [];
  const handler = response => {
    const url = response.url();
    if (url.includes('/api/ai/')) {
      responses.push({ url, status: response.status() });
    }
  };
  page.on('response', handler);
  try {
    await action();
    await page.waitForTimeout(1500);
  } finally {
    page.off('response', handler);
  }
  return responses;
}

async function assertMessageContains(page, expectedText) {
  await page.waitForFunction(text => {
    const node = document.getElementById('message-text');
    return node && node.textContent.includes(text);
  }, expectedText, { timeout: 10000 });
}

async function assertResultContentContains(page, expectedText) {
  await page.waitForFunction(text => {
    const node = document.getElementById('result-content');
    return node && node.innerText.includes(text);
  }, expectedText, { timeout: 10000 });
}

(async () => {
  const teacherSession = readJson(teacherSessionFile);
  assertFreshSession(teacherSession.sessionStorage, 'teacherSessionFile');
  requireRole(teacherSession, 'TEACHER', 'teacherSessionFile');

  const results = [];
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  let page;

  try {
    page = await createAuthedPage(
      context,
      teacherSession.sessionStorage || {},
      'http://localhost:5500/teacher-ai-tools.html'
    );

    await runStep(results, 'teacher ai tools banner renders truthful availability', async () => {
      const bannerText = await page.locator('#teacher-ai-tools-availability-banner').innerText();
      assert(
        bannerText.includes('当前已接通：题目生成、试卷生成、学习建议') &&
        bannerText.includes('知识点讲解、作业批改、教学计划暂不可用'),
        'availability banner should describe connected and unavailable abilities',
        bannerText
      );
    });

    await runStep(results, 'teacher ai tools generate questions hits real api and renders results', async () => {
      await activateTool(page, 'question-generator');
      await page.fill('#question-topic', '条件语句');
      await page.fill('#question-count', '3');
      const responses = await collectApiResponses(page, async () => {
        await page.click('#question-generator-form button[type="submit"]');
        await waitForResultCount(page, 1);
        await assertMessageContains(page, '题目生成成功');
        await assertResultContentContains(page, '条件语句');
      });
      const hit = responses.find(item => item.url.includes('/api/ai/generate-questions'));
      assert(hit && hit.status === 200, 'generate questions should call /api/ai/generate-questions successfully', responses);
    });

    await runStep(results, 'teacher ai tools generate exam hits real api and renders results', async () => {
      await activateTool(page, 'exam-generator');
      await page.fill('#exam-course-name', '数据结构与算法');
      const responses = await collectApiResponses(page, async () => {
        await page.click('#exam-generator-form button[type="submit"]');
        await waitForResultCount(page, 1);
        await assertMessageContains(page, '试卷生成成功');
        await assertResultContentContains(page, '数据结构与算法');
      });
      const hit = responses.find(item => item.url.includes('/api/ai/generate-exam'));
      assert(hit && hit.status === 200, 'generate exam should call /api/ai/generate-exam successfully', responses);
    });

    await runStep(results, 'teacher ai tools learning suggestions hit real api and render results', async () => {
      await activateTool(page, 'learning-analyzer');
      await page.fill('#analyze-student', '42');
      const responses = await collectApiResponses(page, async () => {
        await page.click('#learning-analyzer-form button[type="submit"]');
        await waitForResultCount(page, 1);
        await assertMessageContains(page, '学习建议生成成功');
        await assertResultContentContains(page, '建议');
      });
      const hit = responses.find(item => item.url.includes('/api/ai/learning-suggestions'));
      assert(hit && hit.status === 200, 'learning suggestions should call /api/ai/learning-suggestions successfully', responses);
    });

    await runStep(results, 'teacher ai tools unsupported knowledge explainer stays honest', async () => {
      await activateTool(page, 'knowledge-explainer');
      const responses = await collectApiResponses(page, async () => {
        await page.click('#knowledge-explainer-form button[type="submit"]');
        await waitForResultCount(page, 1);
        await assertMessageContains(page, '当前 AI 服务暂未提供知识点讲解能力');
        await assertResultContentContains(page, '当前 AI 服务暂未提供知识点讲解能力');
      });
      assert(responses.length === 0, 'knowledge explainer should not call any /api/ai endpoint', responses);
    });

    await runStep(results, 'teacher ai tools unsupported assignment evaluator stays honest', async () => {
      await activateTool(page, 'assignment-evaluator');
      const responses = await collectApiResponses(page, async () => {
        await page.click('#assignment-evaluator-form button[type="submit"]');
        await waitForResultCount(page, 1);
        await assertMessageContains(page, '当前 AI 服务暂未提供作业批改能力');
        await assertResultContentContains(page, '当前 AI 服务暂未提供作业批改能力');
      });
      assert(responses.length === 0, 'assignment evaluator should not call any /api/ai endpoint', responses);
    });

    await runStep(results, 'teacher ai tools unsupported teaching planner stays honest', async () => {
      await activateTool(page, 'teaching-planner');
      const responses = await collectApiResponses(page, async () => {
        await page.click('#teaching-planner-form button[type="submit"]');
        await waitForResultCount(page, 1);
        await assertMessageContains(page, '当前 AI 服务暂未提供教学计划生成能力');
        await assertResultContentContains(page, '当前 AI 服务暂未提供教学计划生成能力');
      });
      assert(responses.length === 0, 'teaching planner should not call any /api/ai endpoint', responses);
    });

    console.log(`Teacher AI tools runtime verifier passed: ${results.length}`);
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
