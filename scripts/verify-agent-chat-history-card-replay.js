const path = require('node:path');
const { chromium } = require('../major_assignment/node_modules/playwright');

const FRONTEND_BASE_URL = (process.env.FRONTEND_BASE_URL || 'http://127.0.0.1:5500').replace(/\/$/, '');

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
    await page.goto(`${FRONTEND_BASE_URL}/__agent-history-card-replay-test__.html`, { waitUntil: 'domcontentloaded' });
    await page.setContent(`
      <!doctype html>
      <html lang="zh-CN">
      <head>
        <meta charset="utf-8">
        <link rel="stylesheet" href="${FRONTEND_BASE_URL}/agent-chat-panel.css">
      </head>
      <body>
        <section data-agent-panel data-agent-role="TEACHER">
          <div data-agent-messages></div>
          <form data-agent-form>
            <textarea data-agent-input></textarea>
            <button type="submit" data-agent-submit>发送</button>
          </form>
        </section>
      </body>
      </html>
    `);
    await page.addScriptTag({ path: path.resolve(__dirname, '../frontend/dist/agent-chat-panel.js') });
    await page.evaluate(() => window.initAgentChatPanels());

    const state = await page.evaluate(async () => {
      const panel = document.querySelector('[data-agent-panel]').agentChatPanel;
      window.apiService = {
        get: async (url) => {
          if (url !== '/api/agent/sessions/88') {
            throw new Error(`unexpected url: ${url}`);
          }
          return {
            success: true,
            data: {
              sessionId: '88',
              actions: [
                {
                  actionId: 501,
                  sessionId: '88',
                  intent: 'PUBLISH_ASSIGNMENT',
                  status: 'EXECUTED',
                  riskLevel: 'MEDIUM',
                  result: {
                    status: 'EXECUTED',
                    message: '操作已执行。',
                    assignment: {
                      id: 169,
                      title: 'Java基础课堂练习',
                      courseId: 12,
                      classId: 34
                    }
                  },
                  executedAt: '2026-06-30T08:50:00',
                  updatedAt: '2026-06-30T08:50:00'
                }
              ],
              messages: [
                {
                  role: 'USER',
                  content: '帮我随机生成五道题目',
                  createdAt: '2026-06-30T08:20:00'
                },
                {
                  role: 'ASSISTANT',
                  content: '已生成 2 道Java基础题',
                  createdAt: '2026-06-30T08:20:01',
                  metadata: {
                    responseType: 'DATA',
                    intent: 'GENERATE_QUESTIONS',
                    data: {
                      topic: 'Java基础',
                      difficulty: '中等',
                      totalQuestions: 2,
                      actualCount: 2,
                      questions: [
                        {
                          type: '选择题',
                          difficulty: '中等',
                          content: '下列关于 Java 基本数据类型的说法，正确的是哪一项？',
                          options: ['String 是基本数据类型', 'int 默认占用 4 字节'],
                          answer: 'B',
                          explanation: 'int 为 32 位整数。'
                        },
                        {
                          type: '简答题',
                          difficulty: '中等',
                          content: 'finally 代码块的主要作用是什么？',
                          answer: '释放资源'
                        }
                      ]
                    }
                  }
                },
                {
                  role: 'USER',
                  content: '发布作业，课程ID 12，班级ID 34，标题是Java基础课堂练习，使用当前题目草稿',
                  createdAt: '2026-06-30T08:21:00'
                },
                {
                  role: 'ASSISTANT',
                  content: '请确认是否执行该操作。',
                  createdAt: '2026-06-30T08:50:00',
                  metadata: {
                    responseType: 'ACTION_PREVIEW',
                    intent: 'PUBLISH_ASSIGNMENT',
                    actionPreview: {
                      actionId: 501,
                      intent: 'PUBLISH_ASSIGNMENT',
                      title: '发布作业',
                      riskLevel: 'MEDIUM',
                      summary: '发布作业: Java基础课堂练习',
                      preview: {
                        title: 'Java基础课堂练习',
                        questionCount: 2
                      },
                      idempotencyKey: 'idem-501'
                    }
                  }
                }
              ]
            }
          };
        }
      };
      await panel.switchSession('88');
      return {
        messageCount: document.querySelectorAll('.agent-message').length,
        draftExists: Boolean(document.querySelector('.agent-question-draft')),
        draftTitle: document.querySelector('.agent-question-draft-header strong')?.textContent || '',
        questionCount: document.querySelectorAll('.agent-question-card').length,
        confirmExists: Boolean(document.querySelector('[data-agent-confirm="501"]')),
        executedText: document.querySelector('#agent-action-501 .agent-action-footer')?.textContent?.trim() || '',
        actionResultText: document.querySelector('.agent-action-success-card')?.textContent || '',
        previewTimeText: document.querySelector('#agent-action-501')?.closest('.agent-message')?.querySelector('.agent-message-time')?.textContent?.trim() || '',
        actionTitle: document.querySelector('.agent-action-card strong')?.textContent || '',
        bodyText: document.body.innerText
      };
    });

    assert(state.messageCount === 4, 'history restore should render all session messages', state);
    assert(state.draftExists, 'history restore should replay generated-question draft card from metadata', state);
    assert(state.draftTitle === '已生成 2 道题', 'history draft card should preserve generated question count', state);
    assert(state.questionCount === 2, 'history draft card should render question cards', state);
    assert(!state.confirmExists, 'executed history action should not show a clickable confirm button', state);
    assert(state.executedText.includes('已执行'), 'executed history action should render executed state in preview card', state);
    assert(state.actionResultText.includes('作业已发布'), 'executed history action should replay execution result', state);
    assert(state.previewTimeText === '08:50', 'local history timestamp should display without timezone offset', state);
    assert(state.actionTitle === '发布作业', 'history action card should preserve title', state);
    assert(!state.bodyText.includes('[object Object]'), 'history restore should not leak raw object text', state);

    console.log('agent chat history card replay verification passed.');
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
