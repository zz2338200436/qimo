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
    const shouldAdd = force === undefined ? !exists : !!force;
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
  constructor(tagName = 'div', ownerDocument = null) {
    this.tagName = tagName.toUpperCase();
    this.ownerDocument = ownerDocument;
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

  removeChild(child) {
    const index = this.children.indexOf(child);
    if (index >= 0) {
      this.children.splice(index, 1);
      child.parentNode = null;
    }
    return child;
  }

  remove() {
    this.parentNode?.removeChild(this);
  }

  addEventListener(type, handler) {
    if (!this.eventListeners.has(type)) {
      this.eventListeners.set(type, []);
    }
    this.eventListeners.get(type).push(handler);
  }

  focus() {}

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
    if (name === 'id') {
      this.id = String(value);
    }
  }

  getAttribute(name) {
    if (name === 'class') {
      return this.className;
    }
    if (name === 'id') {
      return this.id || '';
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
  constructor() {
    this.listeners = new Map();
  }

  createElement(tagName) {
    return new FakeElement(tagName, this);
  }

  addEventListener(type, handler) {
    if (!this.listeners.has(type)) {
      this.listeners.set(type, []);
    }
    this.listeners.get(type).push(handler);
  }

  querySelectorAll() {
    return [];
  }

  getElementById() {
    return null;
  }
}

function createRoot(ownerDocument) {
  const root = new FakeElement('section', ownerDocument);
  const messages = new FakeElement('div', ownerDocument);
  messages.dataset.agentMessages = '';
  const form = new FakeElement('form', ownerDocument);
  form.dataset.agentForm = '';
  const input = new FakeElement('textarea', ownerDocument);
  input.dataset.agentInput = '';
  const submit = new FakeElement('button', ownerDocument);
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
    dispatchEvent() {},
    prompt() {
      return '确认执行';
    }
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

  const { root, messages } = createRoot(document);
  const panel = new window.AgentChatPanel(root, { role: 'TEACHER' });
  return { panel, messages };
}

function verifyFile(file) {
  const scriptPath = path.resolve(file);
  const { panel, messages } = loadPanel(scriptPath);
  const payload = {
    responseType: 'DATA',
    message: '查询完成。',
    data: {
      status: 'EXECUTED',
      topic: 'Java',
      difficulty: '中等',
      count: 2,
      questions: [
        { content: 'Java 中哪个关键字用于继承类？', answer: 'extends' },
        { content: 'Spring Boot 默认配置文件名是什么？', answer: 'application.yml' }
      ],
      aiResult: {
        topic: 'Java',
        difficulty: '中等',
        questions: [
          { content: 'Java 中哪个关键字用于继承类？', answer: 'extends' },
          { content: 'Spring Boot 默认配置文件名是什么？', answer: 'application.yml' }
        ]
      }
    }
  };

  panel.renderResponse(payload);

  assert(messages.children.length === 1, `${file} should render one agent response message.`, {
    messageCount: messages.children.length
  });
  const html = messages.children[0].innerHTML;
  assert(html.includes('Java 中哪个关键字用于继承类？'), `${file} should render first generated question.`, html);
  assert(html.includes('Spring Boot 默认配置文件名是什么？'), `${file} should render second generated question.`, html);
}

for (const file of [
  'frontend/dist/agent-chat-panel.js',
  'major_assignment/src/main/resources/static/agent-chat-panel.js'
]) {
  verifyFile(file);
}

console.log('agent chat aiResult recursion verification passed.');
