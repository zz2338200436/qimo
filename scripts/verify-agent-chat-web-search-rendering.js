const fs = require('fs');
const path = require('path');
const vm = require('vm');

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

  scrollTo(options) {
    if (typeof options === 'number') {
      this.scrollTop = options;
      return;
    }
    this.scrollTop = options?.top || 0;
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
    responseType: 'TEXT',
    message: [
      'Springboot最新的最新版本看起来是 4.0.0。',
      '',
      '1',
      'Spring Boot',
      '5 天之前 · Spring Boot helps you to create stand-alone, production-grade Spring-based applications that you can run. 链接：https://docs.spring.io/spring-boot/index.html',
      'https://docs.spring.io/spring-boot/index.html',
      '2',
      'Spring Boot - Spring 框架',
      '2025年12月4日 · Spring Boot 4.0.0 Spring Boot 让您可以轻松创建独立的、生产级的 Spring 应用程序。 链接：https://springframework.org.cn/projects/spring-boot/'
    ].join('\n')
  });

  const html = messages.children[0].innerHTML;
  assert(html.includes('agent-web-search-response'), `${file} should render web-search responses as a structured result block.`, html);
  assert(html.includes('agent-web-search-results'), `${file} should render a web-search results container.`, html);
  assert(html.includes('Springboot最新的最新版本看起来是 4.0.0。'), `${file} should preserve the answer sentence.`, html);
  assert(html.includes('Spring Boot'), `${file} should preserve result titles.`, html);
  assert(!html.includes('<ol>'), `${file} should not fall back to plain markdown ordered list rendering for web-search results.`, html);

  panel.renderResponse({
    responseType: 'DATA',
    message: [
      'Spring Cloud 最新组件目前常见的核心组件包括服务注册与发现、配置中心、网关、负载均衡和熔断治理。',
      '',
      '解释：我优先整理了当前搜索最相关的公开来源，方便你继续核对组件清单。',
      '',
      '',
      '可参考的来源（旧搜索词）：',
      '1. Old Source',
      '  摘要：stale snippet',
      '  链接：https://old.example.com'
    ].join('\n'),
    data: {
      query: 'Spring Cloud 最新组件',
      message: '联网搜索完成。',
      results: [
        {
          title: 'Spring Cloud',
          url: 'https://spring.io/projects/spring-cloud',
          snippet: 'Spring Cloud provides tools for distributed systems.'
        }
      ]
    }
  });

  const structuredHtml = messages.children[1].innerHTML;
  assert(structuredHtml.includes('agent-web-search-response'),
    `${file} should render structured web-search DATA payloads as a structured result block.`,
    structuredHtml);
  assert(structuredHtml.includes('Spring Cloud 最新组件目前常见的核心组件包括服务注册与发现、配置中心、网关、负载均衡和熔断治理。'),
    `${file} should render the current structured web-search answer from the latest payload message.`,
    structuredHtml);
  assert(structuredHtml.includes('我优先整理了当前搜索最相关的公开来源，方便你继续核对组件清单。'),
    `${file} should render the current structured web-search explanation from the latest payload message.`,
    structuredHtml);
  assert(structuredHtml.includes('Spring Cloud 最新组件'),
    `${file} should render the current structured web-search query.`,
    structuredHtml);
  assert(structuredHtml.includes('https://spring.io/projects/spring-cloud'),
    `${file} should render links from the current structured web-search results.`,
    structuredHtml);
  assert(!structuredHtml.includes('https://old.example.com'),
    `${file} should not reuse stale links from the fallback message when structured results are present.`,
    structuredHtml);
}

for (const file of [
  'frontend/dist/agent-chat-panel.js',
  'major_assignment/src/main/resources/static/agent-chat-panel.js'
]) {
  verifyFile(file);
}

console.log('agent chat web-search rendering verification passed.');
