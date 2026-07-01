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
  try {
    const page = await context.newPage();
    await page.setViewportSize({ width: 894, height: 711 });
    await page.addInitScript(() => {
      sessionStorage.setItem('token', 'web-search-toggle-token');
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
    await page.waitForSelector('[data-teacher-ai-web-search]');
    await page.waitForFunction(() => Boolean(document.querySelector('[data-agent-panel]')?.agentChatPanel));

    const toggle = page.locator('[data-teacher-ai-web-search]');
    const input = page.locator('[data-agent-input]');
    assert(await toggle.count() === 1, 'teacher AI composer should expose exactly one web-search toggle');

    await input.fill('Spring Cloud Gateway 断言怎么配置');
    await toggle.click();

    const activeState = await toggle.evaluate((button) => ({
      pressed: button.getAttribute('aria-pressed'),
      active: button.classList.contains('teacher-ai-input-web-search-active'),
      inputValue: document.querySelector('[data-agent-input]')?.value || ''
    }));
    assert(activeState.pressed === 'true' && activeState.active,
      'web-search toggle should expose a selected visual/accessibility state after click',
      activeState);
    assert(activeState.inputValue === 'Spring Cloud Gateway 断言怎么配置',
      'clicking web-search should not prepend text into the textarea',
      activeState);

    await page.evaluate(() => {
      const panel = document.querySelector('[data-agent-panel]').agentChatPanel;
      window.__teacherAiSentMessages = [];
      panel.send = async (message, options = {}) => {
        window.__teacherAiSentMessages.push({
          message,
          displayMessage: options.displayMessage || message
        });
        panel.append('user', `<p>${options.displayMessage || message}</p>`);
        panel.inputEl.value = '';
      };
    });

    await page.locator('[data-agent-form]').evaluate((form) => form.requestSubmit());
    const sendState = await page.evaluate(() => ({
      sentMessages: window.__teacherAiSentMessages,
      userBubble: document.querySelector('.agent-message-user .agent-message-body')?.textContent?.trim() || '',
      inputValue: document.querySelector('[data-agent-input]')?.value || ''
    }));

    assert(sendState.sentMessages?.length === 1,
      'submitting with web-search selected should send one message',
      sendState);
    assert(sendState.sentMessages[0].message === '联网搜索 Spring Cloud Gateway 断言怎么配置',
      'selected web-search should prefix only the backend-bound message',
      sendState);
    assert(sendState.sentMessages[0].displayMessage === 'Spring Cloud Gateway 断言怎么配置',
      'selected web-search should preserve the original text for the visible user message',
      sendState);
    assert(sendState.userBubble === 'Spring Cloud Gateway 断言怎么配置',
      'visible user bubble should not include the web-search prefix',
      sendState);

    await toggle.click();
    const inactiveState = await toggle.evaluate((button) => ({
      pressed: button.getAttribute('aria-pressed'),
      active: button.classList.contains('teacher-ai-input-web-search-active')
    }));
    assert(inactiveState.pressed === 'false' && !inactiveState.active,
      'clicking web-search again should clear the selected state',
      inactiveState);

    console.log('teacher AI web-search toggle verification passed.');
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
