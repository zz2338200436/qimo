const path = require('node:path');
const fs = require('node:fs');
const { chromium } = require(path.resolve(__dirname, '../major_assignment/node_modules/playwright'));

const FRONTEND_BASE_URL = (process.env.FRONTEND_BASE_URL || 'http://localhost:5500').replace(/\/$/, '');

function assert(condition, message, details) {
  if (!condition) {
    const error = new Error(message);
    if (details !== undefined) {
      error.details = details;
    }
    throw error;
  }
}

(async () => {
  const bundledChromium = path.join(
    process.env.LOCALAPPDATA || '',
    'ms-playwright',
    'chromium_headless_shell-1228',
    'chrome-headless-shell-win64',
    'chrome-headless-shell.exe'
  );
  const launchOptions = fs.existsSync(bundledChromium)
    ? { headless: true, executablePath: bundledChromium }
    : { headless: true };

  const browser = await chromium.launch(launchOptions);
  const context = await browser.newContext();

  await context.route('**/api/agent/chat/stream', async (route) => {
    await route.fulfill({
      status: 500,
      contentType: 'application/json',
      body: JSON.stringify({ success: false, message: 'mock stream disabled' })
    });
  });

  await context.route('**/api/agent/chat', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          sessionId: 'web-search-runtime-1',
          responseType: 'TEXT',
          message: [
            'Springboot最新的最新版本看起来是 4.0.0。',
            '解释：我优先参考了更接近官方文档和官方项目页的版本信息。',
            '',
            '1',
            'Spring Boot',
            '5 天之前 · Spring Boot helps you to create stand-alone, production-grade Spring-based applications that you can run. 链接：https://docs.spring.io/spring-boot/index.html',
            'https://docs.spring.io/spring-boot/index.html',
            '2',
            'Spring Boot - Spring 框架',
            '2025年12月4日 · Spring Boot 4.0.0 Spring Boot 让您可以轻松创建独立的、生产级的 Spring 应用程序。 链接：https://springframework.org.cn/projects/spring-boot/'
          ].join('\n')
        }
      })
    });
  });

  try {
    const page = await context.newPage();
    await page.addInitScript(() => {
      sessionStorage.setItem('token', 'teacher-runtime-token');
      sessionStorage.setItem('activeRole', 'TEACHER');
      sessionStorage.setItem('role', 'TEACHER');
      sessionStorage.setItem('userId', '7');
      sessionStorage.setItem('user', JSON.stringify({
        id: 7,
        username: 'teacher7',
        name: 'Teacher Seven',
        roles: ['TEACHER'],
        activeRole: 'TEACHER'
      }));
    });

    await page.goto(`${FRONTEND_BASE_URL}/teacher-ai-tools.html`, { waitUntil: 'domcontentloaded' });
    await page.waitForSelector('[data-agent-input]');
    await page.waitForFunction(() => Boolean(document.querySelector('[data-agent-panel]')?.agentChatPanel));

    await page.locator('[data-agent-input]').fill('帮我联网查一下Springboot最新的版本是多少');
    await page.locator('[data-agent-form]').evaluate((form) => form.requestSubmit());

    await page.waitForSelector('.agent-message-agent .agent-message-body', { timeout: 10000 });

    const state = await page.evaluate(() => {
      const body = document.querySelector('.agent-message-agent .agent-message-body');
      return {
        html: body?.innerHTML || '',
        text: body?.textContent?.trim() || ''
      };
    });

    assert(state.html.includes('agent-web-search-response'),
      'teacher runtime should render web-search reply as structured result block',
      state);
    assert(state.html.includes('agent-web-search-summary'),
      'teacher runtime should render a dedicated answer summary block',
      state);
    assert(state.html.includes('agent-web-search-explanation'),
      'teacher runtime should render a short explanation block',
      state);
    assert(state.html.includes('agent-web-search-results'),
      'teacher runtime should render web-search results container',
      state);
    assert(state.html.includes('agent-web-search-sources'),
      'teacher runtime should render a collapsible sources container',
      state);
    assert(state.text.includes('Springboot最新的最新版本看起来是 4.0.0。'),
      'teacher runtime should preserve the direct answer sentence',
      state);
    assert(state.text.includes('解释：我优先参考了更接近官方文档和官方项目页的版本信息。'),
      'teacher runtime should preserve the short explanation sentence',
      state);
    assert(!state.html.includes('<ol>'),
      'teacher runtime should not fall back to ordered-list rendering for this web-search reply',
      state);

    console.log('teacher AI web-search render runtime verification passed.');
  } finally {
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
