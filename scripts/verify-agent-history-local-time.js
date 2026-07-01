const path = require('node:path');
const { chromium } = require('../major_assignment/node_modules/playwright');

function assert(condition, message, details) {
  if (!condition) {
    const error = new Error(message);
    error.details = details;
    throw error;
  }
}

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 960, height: 760 } });

  try {
    await page.goto('http://127.0.0.1:5500/__agent-history-local-time-test__.html', { waitUntil: 'domcontentloaded' })
      .catch(() => {});
    await page.setContent(`
      <!doctype html>
      <html lang="zh-CN">
      <head><meta charset="utf-8"></head>
      <body>
        <button type="button" data-agent-history-toggle>历史</button>
        <aside data-agent-history-panel>
          <button type="button" data-agent-history-refresh>刷新</button>
          <div data-agent-history-kicker></div>
          <div data-agent-history-title></div>
          <div data-agent-history-copy></div>
          <div data-agent-history-list></div>
          <div data-agent-history-detail></div>
          <button type="button" data-agent-history-close>关闭</button>
        </aside>
        <div data-agent-history-backdrop></div>
      </body>
      </html>
    `);
    await page.addScriptTag({ path: path.resolve(__dirname, '../frontend/dist/agent-history-panel.js') });

    const state = await page.evaluate(async () => {
      window.fetch = async (url) => {
        if (url === '/api/agent/sessions') {
          return {
            ok: true,
            json: async () => ({
              success: true,
              data: [
                {
                  sessionId: '88',
                  userRole: 'TEACHER',
                  status: 'EXECUTED',
                  title: '作业3',
                  summary: '发布作业：作业3',
                  createdAt: '2026-06-30T08:50:00',
                  updatedAt: '2026-06-30T08:50:00'
                }
              ]
            })
          };
        }
        if (url === '/api/agent/sessions/88') {
          return {
            ok: true,
            json: async () => ({
              success: true,
              data: {
                sessionId: '88',
                userRole: 'TEACHER',
                status: 'EXECUTED',
                createdAt: '2026-06-30T08:50:00',
                updatedAt: '2026-06-30T08:50:00',
                messages: [
                  {
                    role: 'ASSISTANT',
                    content: '操作已执行。',
                    createdAt: '2026-06-30T08:50:00'
                  }
                ],
                actions: [
                  {
                    actionId: 501,
                    intent: 'PUBLISH_ASSIGNMENT',
                    status: 'EXECUTED',
                    riskLevel: 'MEDIUM',
                    createdAt: '2026-06-30T08:50:00',
                    updatedAt: '2026-06-30T08:50:00'
                  }
                ]
              }
            })
          };
        }
        throw new Error(`unexpected url: ${url}`);
      };

      const root = document.querySelector('[data-agent-history-panel]');
      root.dataset.agentHistoryInitialized = 'true';
      const panel = new window.AgentHistoryPanel(root);
      await panel.loadSessions();
      await panel.loadSessionDetail('88');

      return {
        itemMeta: document.querySelector('.agent-history-item-meta')?.textContent?.trim() || '',
        detailHeader: document.querySelector('.agent-history-detail-header p')?.textContent?.trim() || '',
        messageTime: document.querySelector('.agent-history-message small')?.textContent?.trim() || '',
        actionTime: document.querySelector('.agent-history-action small')?.textContent?.trim() || ''
      };
    });

    assert(state.itemMeta.includes('6/30'), 'history list should keep local date for backend LocalDateTime', state);
    assert(state.detailHeader.includes('08:50:00'), 'history detail should keep local wall-clock time', state);
    assert(state.messageTime.includes('08:50:00'), 'history message time should keep local wall-clock time', state);
    assert(state.actionTime.includes('08:50:00'), 'history action time should keep local wall-clock time', state);

    console.log('agent history local time verification passed.');
  } finally {
    await browser.close();
  }
})().catch(error => {
  console.error(error.message);
  if (error.details !== undefined) {
    console.error(JSON.stringify(error.details, null, 2));
  }
  process.exit(1);
});
