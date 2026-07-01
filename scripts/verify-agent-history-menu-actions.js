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
    await page.goto('http://127.0.0.1:5500/__agent-history-menu-actions-test__.html', { waitUntil: 'domcontentloaded' })
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
      const requests = [];
      let resetCount = 0;
      window.sessionStorage.setItem('agent.currentSessionId', '88');
      window.prompt = () => '  期末复习会话  ';
      window.confirm = () => true;
      window.alert = (message) => {
        throw new Error(`unexpected alert: ${message}`);
      };
      window.addEventListener('agent-session-reset', () => {
        resetCount += 1;
      });
      window.fetch = async (url, options = {}) => {
        requests.push({
          url,
          method: options.method || 'GET',
          body: options.body || ''
        });
        if (url === '/api/agent/sessions' && (!options.method || options.method === 'GET')) {
          return {
            ok: true,
            json: async () => ({
              success: true,
              data: [
                {
                  sessionId: '88',
                  userRole: 'TEACHER',
                  status: 'ACTIVE',
                  title: '作业3',
                  summary: '发布作业：作业3',
                  createdAt: '2026-06-30T08:50:00',
                  updatedAt: '2026-06-30T08:50:00'
                },
                {
                  sessionId: '89',
                  userRole: 'TEACHER',
                  status: 'ACTIVE',
                  title: '课堂练习',
                  summary: '练习题',
                  createdAt: '2026-06-29T08:50:00',
                  updatedAt: '2026-06-29T08:50:00'
                }
              ]
            })
          };
        }
        if (url === '/api/agent/sessions/88' && options.method === 'PATCH') {
          return {
            ok: true,
            json: async () => ({
              success: true,
              data: {
                sessionId: '88',
                userRole: 'TEACHER',
                status: 'ACTIVE',
                title: '期末复习会话',
                summary: '发布作业：作业3',
                artifacts: { customTitle: '期末复习会话' },
                createdAt: '2026-06-30T08:50:00',
                updatedAt: '2026-06-30T08:52:00'
              }
            })
          };
        }
        if (url === '/api/agent/sessions/88' && options.method === 'DELETE') {
          return {
            ok: true,
            json: async () => ({ success: true, data: null })
          };
        }
        throw new Error(`unexpected request ${options.method || 'GET'} ${url}`);
      };

      const root = document.querySelector('[data-agent-history-panel]');
      root.dataset.agentHistoryInitialized = 'true';
      const panel = new window.AgentHistoryPanel(root);
      await panel.loadSessions();

      document.querySelector('[data-agent-history-menu-toggle="88"]').click();
      document.querySelector('[data-agent-history-rename="88"]').click();
      await new Promise(resolve => setTimeout(resolve, 0));
      const renamedTitle = document.querySelector('[data-agent-history-session="88"] .agent-history-item-title')?.textContent?.trim() || '';

      document.querySelector('[data-agent-history-menu-toggle="88"]').click();
      document.querySelector('[data-agent-history-delete="88"]').click();
      await new Promise(resolve => setTimeout(resolve, 0));

      return {
        renamedTitle,
        remainingSessionIds: Array.from(document.querySelectorAll('[data-agent-history-session]')).map(node => node.getAttribute('data-agent-history-session')),
        currentSessionId: window.sessionStorage.getItem('agent.currentSessionId'),
        resetCount,
        requests
      };
    });

    assert(state.renamedTitle === '期末复习会话', 'rename should update the rendered history title immediately', state);
    assert(!state.remainingSessionIds.includes('88'), 'delete should remove the session row from the rendered list', state);
    assert(state.remainingSessionIds.includes('89'), 'delete should keep other session rows', state);
    assert(state.currentSessionId === null, 'deleting the current session should clear current session storage', state);
    assert(state.resetCount === 1, 'deleting the current session should switch the chat panel back to a new conversation', state);
    assert(state.requests.some(request => request.method === 'PATCH' && request.url === '/api/agent/sessions/88' && request.body.includes('期末复习会话')),
      'rename should call PATCH /api/agent/sessions/{id} with the new title', state);
    assert(state.requests.some(request => request.method === 'DELETE' && request.url === '/api/agent/sessions/88'),
      'delete should call DELETE /api/agent/sessions/{id}', state);

    console.log('agent history menu actions verification passed.');
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
