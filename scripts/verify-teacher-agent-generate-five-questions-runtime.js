const fs = require('fs');
const path = require('path');
const { chromium } = require(path.resolve(__dirname, '../major_assignment/node_modules/playwright'));

const REPO_ROOT = path.resolve(__dirname, '..');
const FRONTEND_BASE_URL = (process.env.FRONTEND_BASE_URL || 'http://127.0.0.1:5500').replace(/\/$/, '');
const TEACHER_SESSION = path.join(REPO_ROOT, '.runtime-logs', 'teacher-session-agent-runtime.json');
const PROMPT = '随机生成五道 Java基础中等难度课堂练习题';

function assert(condition, message, details) {
  if (!condition) {
    const error = new Error(message);
    error.details = details;
    throw error;
  }
}

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

async function newAuthenticatedPage(browser, session) {
  const page = await browser.newPage({ viewport: { width: 1366, height: 900 } });
  await page.addInitScript((state) => {
    for (const [key, value] of Object.entries(state)) {
      if (value != null) {
        window.sessionStorage.setItem(key, String(value));
      }
    }
  }, session);
  await page.goto(`${FRONTEND_BASE_URL}/teacher-ai-tools.html`, { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('[data-agent-panel]', { timeout: 10000 });
  return page;
}

async function installHook(page) {
  await page.waitForFunction(() => {
    const root = document.querySelector('[data-agent-panel]');
    return Boolean(root && root.agentChatPanel);
  }, { timeout: 10000 });

  await page.evaluate(() => {
    const panel = document.querySelector('[data-agent-panel]')?.agentChatPanel;
    if (!panel) {
      throw new Error('Agent chat panel instance was not initialized.');
    }
    if (window.__agentQuestionSmoke?.installed) {
      return;
    }
    window.__agentQuestionSmoke = {
      installed: true,
      payload: null,
      error: null,
    };

    const originalRenderResponse = panel.renderResponse.bind(panel);
    panel.renderResponse = function patchedRenderResponse(payload) {
      window.__agentQuestionSmoke.payload = payload;
      window.__agentQuestionSmoke.error = null;
      return originalRenderResponse(payload);
    };

    const originalAppend = panel.append.bind(panel);
    panel.append = function patchedAppend(role, html, state) {
      if (role === 'agent' && typeof html === 'string' && html.includes('text-danger')) {
        window.__agentQuestionSmoke.error = html;
      }
      return originalAppend(role, html, state);
    };
  });
}

async function main() {
  const browser = await chromium.launch({ headless: true });
  const session = readSession(TEACHER_SESSION);

  try {
    const page = await newAuthenticatedPage(browser, session);
    await installHook(page);

    await page.fill('[data-agent-input]', PROMPT);
    await page.click('[data-agent-submit]');

    await page.waitForFunction(() => {
      return Boolean(window.__agentQuestionSmoke?.payload || window.__agentQuestionSmoke?.error);
    }, { timeout: 45000 });

    const state = await page.evaluate(() => {
      const payload = window.__agentQuestionSmoke?.payload || null;
      const latestQuestionList = document.querySelector('.agent-message-agent:last-of-type .agent-question-list')
        || Array.from(document.querySelectorAll('.agent-question-list')).at(-1);
      const latestQuestionCards = latestQuestionList
        ? latestQuestionList.querySelectorAll('.agent-question-card').length
        : 0;
      return {
        error: window.__agentQuestionSmoke?.error || null,
        payload,
        latestQuestionCards,
        latestQuestionText: latestQuestionList ? latestQuestionList.innerText : '',
      };
    });

    assert(!state.error, 'teacher agent question generation should not render an error state', state);
    const data = state.payload?.data || {};
    const questions = data.questions || data.aiResult?.questions || [];
    assert(Array.isArray(questions), 'teacher agent question generation should return questions array', state.payload);
    assert(questions.length === 5, 'teacher agent question generation API payload should contain 5 questions', {
      payloadCount: questions.length,
      payload: state.payload,
    });
    assert(state.latestQuestionCards === 5, 'teacher agent UI should render 5 question cards', state);

    console.log(JSON.stringify({
      ok: true,
      payloadCount: questions.length,
      renderedCount: state.latestQuestionCards,
      prompt: PROMPT,
    }, null, 2));

    await page.close();
  } finally {
    await browser.close();
  }
}

main().catch(error => {
  console.error(error.message);
  if (error.details !== undefined) {
    console.error(JSON.stringify(error.details, null, 2));
  }
  process.exit(1);
});
