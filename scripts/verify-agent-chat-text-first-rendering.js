const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

function assert(condition, message, details) {
  if (!condition) {
    const error = new Error(message);
    if (details !== undefined) {
      error.details = details;
    }
    throw error;
  }
}

class FakeClassList {
  constructor(owner) {
    this.owner = owner;
  }

  _get() {
    return this.owner.className ? this.owner.className.split(/\s+/).filter(Boolean) : [];
  }

  _set(values) {
    this.owner.className = Array.from(new Set(values.filter(Boolean))).join(' ').trim();
  }

  add(...tokens) {
    this._set(this._get().concat(tokens));
  }

  remove(...tokens) {
    const removeSet = new Set(tokens);
    this._set(this._get().filter((token) => !removeSet.has(token)));
  }

  toggle(token, force) {
    const values = this._get();
    const exists = values.includes(token);
    const shouldAdd = force === undefined ? !exists : Boolean(force);
    if (shouldAdd && !exists) {
      values.push(token);
    }
    if (!shouldAdd && exists) {
      this._set(values.filter((value) => value !== token));
      return false;
    }
    this._set(values);
    return shouldAdd;
  }

  contains(token) {
    return this._get().includes(token);
  }
}

class FakeElement {
  constructor(tagName = 'div') {
    this.tagName = tagName.toUpperCase();
    this.children = [];
    this.parentNode = null;
    this.className = '';
    this.dataset = {};
    this.attributes = {};
    this.eventListeners = new Map();
    this.disabled = false;
    this.value = '';
    this._html = '';
    this.classList = new FakeClassList(this);
  }

  appendChild(child) {
    child.parentNode = this;
    this.children.push(child);
    return child;
  }

  addEventListener(type, handler) {
    if (!this.eventListeners.has(type)) {
      this.eventListeners.set(type, []);
    }
    this.eventListeners.get(type).push(handler);
  }

  focus() {}

  scrollTo() {}

  querySelector(selector) {
    for (const child of this.children) {
      if (selector === '[data-agent-messages]' && child.dataset.agentMessages !== undefined) {
        return child;
      }
      if (selector === '[data-agent-form]' && child.dataset.agentForm !== undefined) {
        return child;
      }
      if (selector === '[data-agent-input]' && child.dataset.agentInput !== undefined) {
        return child;
      }
      if (selector === '[data-agent-submit]' && child.dataset.agentSubmit !== undefined) {
        return child;
      }
      const nested = child.querySelector(selector);
      if (nested) {
        return nested;
      }
    }
    return null;
  }

  querySelectorAll() {
    return [];
  }

  closest(selector) {
    if (selector === '[data-agent-shell]' && this.dataset.agentShell !== undefined) {
      return this;
    }
    return this.parentNode?.closest?.(selector) || null;
  }

  setAttribute(name, value) {
    this.attributes[name] = String(value);
    if (name === 'class') {
      this.className = String(value);
    }
  }

  getAttribute(name) {
    if (name === 'class') {
      return this.className;
    }
    return this.attributes[name];
  }

  set innerHTML(value) {
    this._html = String(value);
  }

  get innerHTML() {
    return this._html;
  }

  get textContent() {
    return this._html;
  }

  get scrollHeight() {
    return 0;
  }
}

class FakeDocument {
  createElement(tagName) {
    return new FakeElement(tagName);
  }

  addEventListener() {}

  querySelectorAll() {
    return [];
  }

  getElementById() {
    return null;
  }
}

function createRoot() {
  const root = new FakeElement('section');
  const messages = new FakeElement('div');
  messages.dataset.agentMessages = '';
  const form = new FakeElement('form');
  form.dataset.agentForm = '';
  const input = new FakeElement('textarea');
  input.dataset.agentInput = '';
  const submit = new FakeElement('button');
  submit.dataset.agentSubmit = '';

  root.appendChild(messages);
  root.appendChild(form);
  form.appendChild(input);
  form.appendChild(submit);

  return { root, messages };
}

function loadPanel(scriptPath) {
  const document = new FakeDocument();
  const window = {
    document,
    sessionStorage: {
      getItem() {
        return null;
      },
      setItem() {},
      removeItem() {}
    },
    addEventListener() {},
    dispatchEvent() {}
  };

  const context = {
    window,
    document,
    console,
    fetch: async () => {
      throw new Error('Unexpected fetch in renderer verification.');
    },
    Response,
    ReadableStream,
    TextDecoder,
    TextEncoder,
    AbortController,
    DOMException,
    CustomEvent: class CustomEvent {
      constructor(type, init = {}) {
        this.type = type;
        this.detail = init.detail;
      }
    },
    setTimeout,
    clearTimeout
  };
  context.global = window;

  const scriptContent = fs.readFileSync(scriptPath, 'utf8');
  vm.runInNewContext(scriptContent, context, { filename: scriptPath });

  const { root, messages } = createRoot();
  const panel = new window.AgentChatPanel(root, { role: 'TEACHER' });
  return { panel, messages };
}

function verifyFile(file) {
  const scriptPath = path.resolve(file);
  const { panel, messages } = loadPanel(scriptPath);

  panel.renderResponse({
    responseType: 'DATA',
    message: '查询到 2 门课程。',
    data: {
      totalElements: 2,
      courses: [
        {
          id: 91010,
          courseName: '分布式框架技术实训',
          courseCode: '001',
          description: '面向 Spring Cloud 微服务、配置中心、网关路由、服务治理与部署排障的综合实训课程。',
          semester: '第二学期',
          credit: 2,
          totalHours: 2,
          studentCount: 1,
          startDate: '2026-06-29',
          endDate: '2026-06-29'
        },
        {
          id: 91011,
          courseName: '分布式框架技术',
          courseCode: 'DFT101',
          semester: '第二学期',
          credit: 3,
          totalHours: 48,
          studentCount: 32
        }
      ]
    }
  });

  let html = messages.children[0].innerHTML;
  assert(html.includes('<ul>') || html.includes('<ol>'), `${file} should render course queries as markdown lists.`, html);
  assert(html.includes('分布式框架技术实训'), `${file} should include course names in markdown output.`, html);
  assert(!html.includes('agent-course-card'), `${file} should not render course queries as course cards.`, html);
  assert(!html.includes('查看课程详情'), `${file} should not render course detail buttons for query results.`, html);

  panel.renderResponse({
    responseType: 'DATA',
    message: '注册中心用于保存服务实例地址、端口和健康状态。',
    data: {
      answer: '注册中心用于保存服务实例地址、端口和健康状态。',
      source: '知识库'
    }
  });

  html = messages.children[1].innerHTML;
  assert(html.includes('注册中心用于保存服务实例地址'), `${file} should render ordinary DATA text.`, html);
  assert(!html.includes('agent-data-result'), `${file} should not wrap ordinary DATA text in a data card.`, html);
  assert(!html.includes('agent-result-map'), `${file} should not render ordinary DATA as a metadata map.`, html);

  panel.renderResponse({
    responseType: 'DATA',
    message: '已生成 1 道题。',
    data: {
      topic: 'Java',
      difficulty: '中等',
      count: 1,
      actualCount: 1,
      questions: [
        {
          content: 'Java 中哪个关键字用于继承类？',
          options: ['import', 'extends'],
          answer: 'extends',
          difficulty: '中等',
          type: '选择题'
        }
      ]
    }
  });

  html = messages.children[2].innerHTML;
  assert(html.includes('agent-question-draft'), `${file} should keep generated questions as a structured draft card.`, html);
  assert(html.includes('Java 中哪个关键字用于继承类？'), `${file} should render generated question content.`, html);

  panel.renderResponse({
    responseType: 'TEXT',
    message: [
      '# 复习提纲',
      '',
      '1. 第一项',
      '2. 第二项',
      '',
      '> 重点：注册中心用于服务发现。',
      '',
      '| 列名 | 值 |',
      '| --- | --- |',
      '| 服务 | gateway |',
      '',
      '```java',
      'System.out.println("hello");',
      '```'
    ].join('\n')
  });

  html = messages.children[3].innerHTML;
  assert(html.includes('<ol>'), `${file} should render ordered lists.`, html);
  assert(html.includes('<blockquote>'), `${file} should render blockquotes.`, html);
  assert(html.includes('<table'), `${file} should render markdown tables.`, html);
  assert(html.includes('<pre><code'), `${file} should render fenced code blocks.`, html);
}

for (const file of [
  'frontend/dist/agent-chat-panel.js',
  'major_assignment/src/main/resources/static/agent-chat-panel.js'
]) {
  verifyFile(file);
}

console.log('agent chat text-first rendering verification passed.');
