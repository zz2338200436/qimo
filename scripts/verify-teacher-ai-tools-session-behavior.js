const path = require('path');
const fs = require('fs');
const { chromium } = require(path.resolve(__dirname, '../major_assignment/node_modules/playwright'));

function assert(condition, message, details) {
  if (!condition) {
    const error = new Error(message);
    if (details !== undefined) {
      error.details = details;
    }
    throw error;
  }
}

function buildSession(sessionId, title, summary, messages, updatedAt) {
  return {
    sessionId,
    userRole: 'TEACHER',
    status: 'ACTIVE',
    title,
    summary,
    pendingIntent: 'GENERATE_QUESTIONS',
    artifacts: {},
    createdAt: updatedAt,
    updatedAt,
    messages,
    actions: []
  };
}

async function newPage(context, options = {}) {
  const page = await context.newPage({ viewport: { width: 1366, height: 900 } });
  await page.addInitScript((initOptions) => {
    sessionStorage.setItem('token', 'teacher-token');
    sessionStorage.setItem('userId', '7');
    sessionStorage.setItem('activeRole', 'TEACHER');
    sessionStorage.setItem('role', 'TEACHER');
    sessionStorage.setItem('user', JSON.stringify({
      id: 7,
      username: 'teacher7',
      name: 'Teacher Seven',
      roles: ['TEACHER'],
      activeRole: 'TEACHER'
    }));
    if (initOptions.currentSessionId) {
      sessionStorage.setItem('agent.currentSessionId', initOptions.currentSessionId);
    }
  }, options);
  return page;
}

(async () => {
  const bundledChromium = path.join(
    process.env.LOCALAPPDATA || '',
    'ms-playwright',
    'chromium_headless_shell-1228',
    'chrome-headless-shell-win64',
    'chrome-headless-shell.exe'
  );
  const browser = await chromium.launch(fs.existsSync(bundledChromium)
    ? { headless: true, executablePath: bundledChromium }
    : { headless: true });
  const context = await browser.newContext();

  const sessionOneMessages = [
    { role: 'USER', content: '帮我生成第一章练习题', createdAt: '2026-06-30T09:00:00' },
    { role: 'ASSISTANT', content: '已为你整理第一章练习题思路。', createdAt: '2026-06-30T09:01:00' }
  ];
  const sessionTwoMessages = [
    { role: 'USER', content: '给我一份分布式框架测验卷', createdAt: '2026-06-29T14:00:00' },
    { role: 'ASSISTANT', content: '测验卷结构已经准备好，可以继续细化题量。', createdAt: '2026-06-29T14:02:00' }
  ];
  const sessionThreeMessages = [];

  const sessionStore = [
    buildSession('1', '第一章课堂练习', '最近内容：第一章练习题草稿', sessionOneMessages, '2026-06-30T09:01:00'),
    buildSession('2', '分布式框架测验卷', '最近内容：测验卷结构草稿', sessionTwoMessages, '2026-06-29T14:02:00')
  ];

  const details = {
    '1': buildSession('1', '第一章课堂练习', '最近内容：第一章练习题草稿', sessionOneMessages, '2026-06-30T09:01:00'),
    '2': buildSession('2', '分布式框架测验卷', '最近内容：测验卷结构草稿', sessionTwoMessages, '2026-06-29T14:02:00')
  };
  const delayedDetailSessionIds = new Set();

  await context.route('**/api/agent/sessions', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: sessionStore.map(({ messages, actions, ...session }) => session) })
    });
  });

  await context.route('**/api/agent/sessions/*', async (route) => {
    const sessionId = route.request().url().split('/').pop();
    if (delayedDetailSessionIds.has(sessionId)) {
      await new Promise((resolve) => setTimeout(resolve, 350));
      delayedDetailSessionIds.delete(sessionId);
    }
    const payload = details[sessionId];
    await route.fulfill({
      status: payload ? 200 : 404,
      contentType: 'application/json',
      body: JSON.stringify(payload
        ? { success: true, data: payload }
        : { success: false, message: 'not found' })
    });
  });

  await context.route('**/api/agent/chat/stream', async (route) => {
    await route.fulfill({
      status: 500,
      contentType: 'application/json',
      body: JSON.stringify({ success: false, message: 'mock stream disabled' })
    });
  });

  await context.route('**/api/agent/chat', async (route) => {
    const body = JSON.parse(route.request().postData() || '{}');
    const message = body.message || '';
    const sessionId = body.sessionId || '3';
    if (!details[sessionId]) {
      const updatedAt = '2026-06-30T11:01:00';
      const generatedMessages = [
        { role: 'USER', content: message, createdAt: '2026-06-30T11:00:00' },
        { role: 'ASSISTANT', content: '好的，我已经为你创建一个新的教学对话。', createdAt: updatedAt }
      ];
      const session = buildSession(sessionId, '新的教学对话', '最近内容：新的教学对话', generatedMessages, updatedAt);
      details[sessionId] = session;
      sessionStore.unshift(session);
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          sessionId,
          responseType: 'TEXT',
          message: '好的，我已经为你创建一个新的教学对话。'
        }
      })
    });
  });

  const page = await newPage(context);

  try {
    delayedDetailSessionIds.add('1');
    const restoringPage = await newPage(context, { currentSessionId: '1' });
    await restoringPage.goto('http://localhost:5500/teacher-ai-tools.html', { waitUntil: 'domcontentloaded' });
    await restoringPage.waitForSelector('[data-agent-panel]');
    const initialRestoreState = await restoringPage.evaluate(() => {
      const shell = document.querySelector('.teacher-ai-chat-shell');
      const emptyState = document.querySelector('[data-teacher-ai-empty-state]');
      const body = document.querySelector('.agent-panel.chatgpt-like .agent-panel-body');
      return {
        hasConversationClass: shell?.classList.contains('agent-shell-has-conversation') || false,
        emptyStateDisplay: emptyState ? getComputedStyle(emptyState).display : '',
        bodyJustifyContent: body ? getComputedStyle(body).justifyContent : ''
      };
    });
    assert(
      initialRestoreState.hasConversationClass && initialRestoreState.emptyStateDisplay === 'none',
      'restoring a remembered session should not show the centered empty conversation state before history loads',
      initialRestoreState
    );
    await restoringPage.waitForFunction(() => {
      const texts = Array.from(document.querySelectorAll('.agent-message-body')).map((node) => node.textContent || '');
      return texts.some((text) => text.includes('帮我生成第一章练习题'));
    });
    await restoringPage.close();

    await page.goto('http://localhost:5500/teacher-ai-tools.html', { waitUntil: 'domcontentloaded' });
    await page.waitForSelector('[data-teacher-ai-empty-state]');

    const railBox = await page.locator('.teacher-ai-history-rail').boundingBox();
    const viewport = page.viewportSize();
    assert(railBox && viewport && railBox.x > viewport.width / 2, 'history rail should render on the right side', { railBox, viewport });

    await page.locator('[data-agent-history-toggle]').click();
    await page.waitForSelector('[data-agent-history-session]');
    const summaries = await page.locator('.agent-history-item-summary').allTextContents();
    assert(summaries.length >= 2 && summaries[0] !== summaries[1], 'history items should not reuse the same static summary', summaries);

    await page.locator('[data-agent-history-close]').click();
    await page.locator('[data-agent-input]').fill('帮我新建一段新的教学对话');
    await page.locator('[data-agent-submit]').click();
    await page.waitForFunction(() => document.querySelectorAll('.agent-message').length >= 2);

    await page.locator('[data-agent-history-toggle]').click();
    await page.waitForFunction(() => {
      return Array.from(document.querySelectorAll('.agent-history-item-title'))
        .some((node) => (node.textContent || '').includes('新的教学对话'));
    });

    await page.locator('.teacher-ai-history-new-chat').click();
    await page.waitForFunction(() => {
      const shell = document.querySelector('.teacher-ai-chat-shell');
      return shell && !shell.classList.contains('agent-shell-has-conversation')
        && document.querySelectorAll('.agent-message').length === 0;
    });

    await page.locator('[data-agent-history-toggle]').click();
    await page.locator('[data-agent-history-session="1"]').click();
    await page.waitForFunction(() => {
      const texts = Array.from(document.querySelectorAll('.agent-message-body')).map((node) => node.textContent || '');
      return texts.some((text) => text.includes('帮我生成第一章练习题'))
        && texts.some((text) => text.includes('已为你整理第一章练习题思路。'))
        && !texts.some((text) => text.includes('已切换到会话 #'));
    });

    console.log('Teacher AI tools session behavior verification passed.');
  } finally {
    await page.close().catch(() => {});
    await context.close().catch(() => {});
    await browser.close().catch(() => {});
  }
})().catch((error) => {
  console.error(error.message);
  if (error.details !== undefined) {
    console.error(JSON.stringify(error.details, null, 2));
  }
  process.exit(1);
});
