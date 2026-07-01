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
    await page.goto(`${FRONTEND_BASE_URL}/__agent-question-draft-test__.html`, { waitUntil: 'domcontentloaded' });
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
    await page.evaluate(() => {
      window.agentCurrentCourseId = 12;
      window.agentCurrentClassId = 34;
      const panel = document.querySelector('[data-agent-panel]').agentChatPanel;
      panel.renderResponse({
        responseType: 'DATA',
        message: '操作已处理。',
        data: {
          topic: 'Java基础',
          difficulty: '中等',
          count: 2,
          actualCount: 2,
          partial: false,
          source: '题库文档',
          questions: [
            {
              type: '选择题',
              difficulty: '中等',
              content: '下列关于 Java 基本数据类型的说法，正确的是哪一项？',
              options: ['String 是基本数据类型', 'boolean 可以与 int 自动转换', 'int 默认占用 4 字节', 'char 只能存储 ASCII 字符'],
              answer: 'C',
              explanation: 'int 为 32 位整数，通常占 4 字节。'
            },
            {
              type: '简答题',
              difficulty: '中等',
              content: 'Java 异常处理中，finally 代码块的主要作用是什么？',
              answer: '用于执行释放资源等收尾逻辑',
              explanation: 'finally 一般用于关闭连接、释放文件句柄等清理工作。'
            }
          ]
        }
      });
    });

    const state = await page.evaluate(() => ({
      title: document.querySelector('.agent-message-agent strong')?.textContent || '',
      draftExists: Boolean(document.querySelector('.agent-question-draft')),
      summary: document.querySelector('.agent-question-draft-summary')?.innerText || '',
      actionLabels: Array.from(document.querySelectorAll('.agent-question-draft-toolbar button')).map(button => button.innerText.trim()),
      publishAction: document.querySelector('[data-agent-question-action="publish"]')?.getAttribute('data-agent-question-action') || '',
      copyButtonExists: Boolean(document.querySelector('[data-agent-copy-questions]')),
      cardCount: document.querySelectorAll('.agent-question-card').length,
      optionCount: document.querySelectorAll('.agent-question-option').length,
    }));

    assert(state.title === '已生成 2 道Java基础题', 'question DATA response should render a specific generated-title', state);
    assert(state.draftExists, 'question response should render a draft artifact panel', state);
    assert(state.summary.includes('Java基础'), 'draft summary should include topic', state);
    assert(state.summary.includes('中等'), 'draft summary should include difficulty', state);
    assert(state.summary.includes('已生成 2/2'), 'draft summary should include generated count', state);
    assert(state.summary.includes('题库充足'), 'draft summary should include sufficiency', state);
    assert(state.actionLabels.join('|').includes('发布给班级'), 'draft toolbar should include publish action', state);
    assert(state.actionLabels.join('|').includes('编辑'), 'draft toolbar should include edit action', state);
    assert(state.actionLabels.join('|').includes('重新生成'), 'draft toolbar should include regenerate action', state);
    assert(state.actionLabels.join('|').includes('复制'), 'draft toolbar should include copy action', state);
    assert(!state.actionLabels.join('|').includes('保存题库'), 'draft toolbar should not include save action because questions are retrieved from the question bank', state);
    assert(state.publishAction === 'publish', 'publish action should use the structured question draft action channel', state);
    assert(state.copyButtonExists, 'copy action should use local copy handler', state);
    assert(state.cardCount === 2, 'draft should render all question cards', state);
    assert(state.optionCount === 4, 'choice question should render options as readable items', state);

    const publishPanelState = await page.evaluate(async () => {
      const panel = document.querySelector('[data-agent-panel]').agentChatPanel;
      let captured = null;
      window.apiService = {
        request: async (url) => {
          if (url === '/api/teacher/courses') {
            return { success: true, data: [{ id: 12, courseName: 'Java企业开发' }, { id: 13, courseName: '分布式框架' }] };
          }
          if (url === '/api/teacher/classes') {
            return { success: true, data: [{ id: 34, className: '软件工程1班' }, { id: 35, className: '软件工程2班' }] };
          }
          throw new Error(`unexpected url: ${url}`);
        }
      };
      panel.requestStream = async (_url, body) => {
        captured = body;
        return { sessionId: 88 };
      };
      document.querySelector('[data-agent-question-action="publish"]').click();
      await new Promise(resolve => setTimeout(resolve, 50));
      return {
        captured,
        panelExists: Boolean(document.querySelector('.agent-question-publish-panel')),
        titleValue: document.querySelector('[data-agent-publish-title]')?.value || '',
        courseValue: document.querySelector('[data-agent-publish-course]')?.value || '',
        classValue: document.querySelector('[data-agent-publish-class]')?.value || '',
        courseOptions: Array.from(document.querySelectorAll('[data-agent-publish-course] option')).map(option => option.textContent.trim()),
        classOptions: Array.from(document.querySelectorAll('[data-agent-publish-class] option')).map(option => option.textContent.trim())
      };
    });

    assert(publishPanelState?.captured === null, 'publish click should open a course/class picker before sending', publishPanelState);
    assert(publishPanelState?.panelExists, 'publish click should render a course/class picker', publishPanelState);
    assert(publishPanelState?.titleValue === 'Java基础课堂练习', 'publish picker should expose an editable assignment title field', publishPanelState);
    assert(publishPanelState?.courseValue === '12', 'publish picker should preselect current course', publishPanelState);
    assert(publishPanelState?.classValue === '34', 'publish picker should preselect current class', publishPanelState);
    assert(publishPanelState?.courseOptions.includes('Java企业开发'), 'publish picker should load teacher courses', publishPanelState);
    assert(publishPanelState?.classOptions.includes('软件工程1班'), 'publish picker should load teacher classes', publishPanelState);

    const publishRequest = await page.evaluate(async () => {
      const panel = document.querySelector('[data-agent-panel]').agentChatPanel;
      let captured = null;
      panel.requestStream = async () => {
        throw new Error('publish question draft should not use the streaming endpoint');
      };
      panel.request = async (url, body) => {
        if (url !== '/api/agent/chat') {
          throw new Error(`unexpected publish url: ${url}`);
        }
        captured = body;
        return {
          sessionId: 88,
          responseType: 'ACTION_PREVIEW',
          actionPreview: {
            actionId: 501,
            intent: 'PUBLISH_ASSIGNMENT',
            title: '发布作业',
            riskLevel: 'MEDIUM',
            summary: '发布作业: Java基础课堂练习',
            preview: {
              title: 'Java基础课堂练习',
              questionCount: 2,
              contentPreview: '题目如下：\n1. 下列关于 Java 基本数据类型的说法，正确的是哪一项？'
            },
            idempotencyKey: 'idem-501'
          }
        };
      };
      document.querySelector('[data-agent-publish-title]').value = 'Java基础课后巩固题';
      document.querySelector('[data-agent-publish-confirm]').click();
      await new Promise(resolve => setTimeout(resolve, 20));
      return {
        request: captured,
        hasPreview: Boolean(document.querySelector('[data-agent-confirm="501"]')),
        thinkingCount: document.querySelectorAll('.agent-message-thinking').length,
        text: document.body.innerText
      };
    });

    assert(publishRequest?.request?.message?.includes('使用当前题目草稿'), 'publish should send a precise draft publish message', publishRequest);
    assert(publishRequest?.request?.message?.includes('标题是Java基础课后巩固题'), 'publish should use the edited assignment title in the command message', publishRequest);
    assert(publishRequest?.request?.context?.agentCommandAction === 'publish_question_draft', 'publish should include a structured action marker', publishRequest);
    assert(publishRequest?.request?.context?.currentCourseId === 12, 'publish should include current course id', publishRequest);
    assert(publishRequest?.request?.context?.currentClassId === 34, 'publish should include current class id', publishRequest);
    assert(publishRequest?.request?.context?.questionDraft?.title === 'Java基础课后巩固题', 'publish should include the edited assignment title in draft context', publishRequest?.request?.context?.questionDraft);
    assert(publishRequest?.request?.context?.questionDraft?.selectionMode === 'GENERATED_QUESTIONS', 'publish should include generated-question selection mode', publishRequest);
    assert(publishRequest?.request?.context?.questionDraft?.topic === 'Java基础', 'publish should include original topic', publishRequest);
    assert(publishRequest?.request?.context?.questionDraft?.difficulty === '中等', 'publish should include original difficulty', publishRequest);
    assert(publishRequest?.request?.context?.questionDraft?.questions?.length === 2, 'publish should include all generated questions', publishRequest);
    assert(publishRequest?.request?.context?.questionDraft?.content?.includes('A. String 是基本数据类型'), 'publish draft content should keep choice options for students', publishRequest?.request?.context?.questionDraft);
    assert(!publishRequest?.request?.context?.questionDraft?.content?.includes('答案'), 'publish draft content should not include answers', publishRequest?.request?.context?.questionDraft);
    assert(!publishRequest?.request?.context?.questionDraft?.content?.includes('解析'), 'publish draft content should not include explanations', publishRequest?.request?.context?.questionDraft);
    assert(publishRequest?.hasPreview, 'publish should render an action preview after JSON response', publishRequest);
    assert(publishRequest?.thinkingCount === 0, 'publish should clear the thinking placeholder after JSON response', publishRequest);

    const confirmState = await page.evaluate(async () => {
      const panel = document.querySelector('[data-agent-panel]').agentChatPanel;
      panel.request = async (url) => {
        if (url !== '/api/agent/actions/502/confirm') {
          throw new Error(`unexpected confirm url: ${url}`);
        }
        return {
          intent: 'PUBLISH_ASSIGNMENT',
          status: 'EXECUTED',
          message: '操作已执行',
          result: {
            assignment: {
              id: 300,
              title: 'Java基础课堂练习',
              courseId: 12,
              classId: 34,
              dueDate: '2026-07-06 23:59:59'
            }
          }
        };
      };
      panel.renderPreview({
        actionId: 502,
        intent: 'PUBLISH_ASSIGNMENT',
        title: '发布作业',
        riskLevel: 'MEDIUM',
        summary: '发布作业: Java基础课堂练习',
        preview: {
          title: 'Java基础课堂练习',
          questionCount: 2,
          content: '题目如下：\n1. Java题'
        },
        idempotencyKey: 'idem-501'
      });
      document.querySelector('[data-agent-confirm="502"]').click();
      await new Promise(resolve => setTimeout(resolve, 0));
      const text = document.body.innerText;
      return {
        text,
        hasSuccessCard: Boolean(document.querySelector('.agent-action-success-card')),
        dataResultCount: document.querySelectorAll('.agent-data-result').length
      };
    });

    assert(confirmState.hasSuccessCard, 'assignment publish confirmation should render a friendly success card', confirmState);
    assert(confirmState.text.includes('作业已发布'), 'assignment publish confirmation should say assignment is published', confirmState);
    assert(confirmState.text.includes('Java基础课堂练习'), 'assignment publish confirmation should include assignment title', confirmState);
    assert(!confirmState.text.includes('assignment:'), 'assignment publish confirmation should not expose raw assignment map keys', confirmState);

    const regenerateRequest = await page.evaluate(async () => {
      const panel = document.querySelector('[data-agent-panel]').agentChatPanel;
      let captured = null;
      panel.requestStream = async (_url, body) => {
        captured = body;
        return { sessionId: 89 };
      };
      document.querySelector('[data-agent-question-action="regenerate"]').click();
      await new Promise(resolve => setTimeout(resolve, 0));
      return captured;
    });

    assert(regenerateRequest?.message === '重新生成 2 道Java基础题，难度中等', 'regenerate should reuse the original topic, count and difficulty', regenerateRequest);
    assert(regenerateRequest?.context?.questionDraft?.topic === 'Java基础', 'regenerate should carry draft context', regenerateRequest);

    await page.evaluate(() => {
      const panel = document.querySelector('[data-agent-panel]').agentChatPanel;
      panel.renderResponse({
        responseType: 'DATA',
        message: '操作已处理。',
        data: {
          topic: null,
          difficulty: '中等',
          count: 5,
          actualCount: 5,
          partial: false,
          source: '题库文档',
          questions: [
            { type: '选择题', difficulty: '中等', content: '随机题 1', options: ['A', 'B'], answer: 'A' },
            { type: '判断题', difficulty: '中等', content: '随机题 2', answer: '正确' },
            { type: '填空题', difficulty: '中等', content: '随机题 3', answer: '答案' },
            { type: '简答题', difficulty: '中等', content: '随机题 4', answer: '答案' },
            { type: '选择题', difficulty: '中等', content: '随机题 5', options: ['A', 'B'], answer: 'B' }
          ]
        }
      });
    });

    const randomRegenerateRequest = await page.evaluate(async () => {
      const panel = document.querySelector('[data-agent-panel]').agentChatPanel;
      let captured = null;
      panel.requestStream = async (_url, body) => {
        captured = body;
        return { sessionId: 90 };
      };
      const drafts = document.querySelectorAll('.agent-question-draft');
      drafts[drafts.length - 1].querySelector('[data-agent-question-action="regenerate"]').click();
      await new Promise(resolve => setTimeout(resolve, 0));
      return captured;
    });

    assert(randomRegenerateRequest?.message === '重新生成 5 道题，难度中等', 'random regenerate should not invent a draft topic', randomRegenerateRequest);
    assert(randomRegenerateRequest?.context?.questionDraft?.topic == null, 'random regenerate should keep topic empty in draft context', randomRegenerateRequest);

    const editState = await page.evaluate(() => {
      document.querySelector('[data-agent-question-action="edit"]').click();
      return {
        editing: document.querySelector('.agent-question-draft')?.classList.contains('agent-question-draft-editing') || false,
        editableCount: document.querySelectorAll('.agent-question-draft [contenteditable="true"]').length,
        inputValue: document.querySelector('[data-agent-input]')?.value || ''
      };
    });
    assert(editState.editing, 'edit should toggle inline draft editing instead of sending a vague chat command', editState);
    assert(editState.editableCount > 0, 'edit should make question fields editable', editState);
    assert(editState.inputValue !== '编辑这批题目', 'edit should not send the vague edit command', editState);

    console.log('agent question draft UI verification passed.');
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
