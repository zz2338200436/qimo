const path = require('node:path');
const fs = require('node:fs');
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
  const page = await browser.newPage({ viewport: { width: 1366, height: 900 } });
  const cssPath = path.resolve(__dirname, '../major_assignment/src/main/resources/static/agent-chat-panel.css');
  const pagePath = path.resolve(__dirname, '../major_assignment/src/main/resources/static/teacher-ai-tools.html');
  const pageHtml = fs.readFileSync(pagePath, 'utf8');
  const inlineStyle = pageHtml.match(/<style>([\s\S]*?)<\/style>/i)?.[1] || '';

  try {
    await page.setContent(`
      <div class="content-container teacher-ai-chat-shell agent-shell-has-conversation">
        <section class="agent-panel chatgpt-like">
          <div class="agent-panel-body">
            <div class="agent-messages" data-agent-messages>
              <div class="agent-message agent-message-agent">
                <div class="agent-message-avatar"><span>AI</span></div>
                <div class="agent-message-content">
                  <div class="agent-message-body"><p>你好呀，我可以帮你生成课堂练习。</p></div>
                  <time class="agent-message-time">09:45</time>
                </div>
              </div>
              <div class="agent-message agent-message-user">
                <div class="agent-message-avatar"><span>T</span></div>
                <div class="agent-message-content">
                  <div class="agent-message-body"><p>你好</p></div>
                  <time class="agent-message-time">09:45</time>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>
    `);

    await page.addStyleTag({ path: cssPath });
    await page.addStyleTag({ content: inlineStyle });
    await page.addStyleTag({ content: `
      body { margin: 0; }
      .teacher-ai-chat-shell { padding: 20px 30px 28px; }
      .teacher-ai-chat-shell .agent-panel.chatgpt-like { width: 100%; max-width: 980px; }
      .teacher-ai-chat-shell .agent-panel.chatgpt-like .agent-panel-body { display: grid; grid-template-rows: minmax(0, 1fr) auto; }
      .teacher-ai-chat-shell .agent-panel.chatgpt-like .agent-messages {
        display: flex;
        justify-content: flex-start;
        align-items: flex-start;
        padding: 0 16px 18px;
      }
      .teacher-ai-chat-shell.agent-shell-has-conversation .agent-panel.chatgpt-like .agent-messages {
        display: flex;
        justify-content: flex-start;
        align-items: flex-start;
      }
    `});

    const metrics = await page.evaluate(() => {
      const messages = document.querySelector('[data-agent-messages]');
      const agentMessage = document.querySelector('.agent-message-agent');
      const userMessage = document.querySelector('.agent-message-user');
      const agentBody = agentMessage.querySelector('.agent-message-body');
      const userBody = userMessage.querySelector('.agent-message-body');
      const messagesBox = messages.getBoundingClientRect();
      const agentMessageBox = agentMessage.getBoundingClientRect();
      const userMessageBox = userMessage.getBoundingClientRect();
      const agentBodyBox = agentBody.getBoundingClientRect();
      const userBodyBox = userBody.getBoundingClientRect();
      return {
        messagesBox: { left: messagesBox.left, right: messagesBox.right },
        agentMessageBox: { left: agentMessageBox.left, right: agentMessageBox.right },
        userMessageBox: { left: userMessageBox.left, right: userMessageBox.right },
        agentBodyBox: { left: agentBodyBox.left, right: agentBodyBox.right },
        userBodyBox: { left: userBodyBox.left, right: userBodyBox.right }
      };
    });

    assert(
      metrics.agentMessageBox.left - metrics.messagesBox.left < 40,
      'agent messages should stay anchored near the left edge of the message stream',
      metrics
    );
    assert(
      metrics.userMessageBox.left - metrics.messagesBox.left < 40,
      'user message rows should use the same message stream width after sending',
      metrics
    );
    assert(
      Math.abs(metrics.userMessageBox.right - metrics.userBodyBox.right) > 34,
      'user bubble should remain right aligned inside the stable message row',
      metrics
    );

    console.log('Agent chat message alignment check passed.');
  } finally {
    await page.close();
    await browser.close();
  }
})().catch(error => {
  console.error(error.message);
  if (error.details !== undefined) {
    console.error(JSON.stringify(error.details, null, 2));
  }
  process.exit(1);
});
