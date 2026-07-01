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

function stripTags(value) {
  return String(value || '').replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim();
}

class FakeClassList {
  constructor(owner) {
    this.owner = owner;
  }

  _set(values) {
    this.owner.className = Array.from(new Set(values.filter(Boolean))).join(' ').trim();
  }

  _get() {
    return this.owner.className ? this.owner.className.split(/\s+/).filter(Boolean) : [];
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

  dispatchEvent(event) {
    const handlers = this.eventListeners.get(event.type) || [];
    for (const handler of handlers) {
      handler.call(this, event);
    }
    return true;
  }

  focus() {}

  contains(node) {
    if (node === this) {
      return true;
    }
    return this.children.some((child) => child.contains(node));
  }

  matchesSelector(selector) {
    if (selector.startsWith('.')) {
      return this.classList.contains(selector.slice(1));
    }
    if (selector.startsWith('#')) {
      return this.id === selector.slice(1);
    }
    const dataMatch = selector.match(/^\[data-([a-z0-9-]+)\]$/i);
    if (dataMatch) {
      const key = dataMatch[1].replace(/-([a-z])/g, (_, letter) => letter.toUpperCase());
      return Object.prototype.hasOwnProperty.call(this.dataset, key);
    }
    return false;
  }

  querySelector(selector) {
    for (const child of this.children) {
      if (child.matchesSelector(selector)) {
        return child;
      }
      const nested = child.querySelector(selector);
      if (nested) {
        return nested;
      }
    }
    return null;
  }

  querySelectorAll(selector) {
    let results = [];
    for (const child of this.children) {
      if (child.matchesSelector(selector)) {
        results.push(child);
      }
      results = results.concat(child.querySelectorAll(selector));
    }
    return results;
  }

  closest(selector) {
    let current = this;
    while (current) {
      if (current.matchesSelector(selector)) {
        return current;
      }
      current = current.parentNode;
    }
    return null;
  }

  set innerHTML(value) {
    this._html = String(value);
    this.children = [];
    if (this._html.includes('agent-message-body') && this._html.includes('agent-message-time')) {
      const avatar = new FakeElement('div', this.ownerDocument);
      avatar.className = 'agent-message-avatar';
      const avatarText = new FakeElement('span', this.ownerDocument);
      avatarText.innerHTML = this._html.match(/<span>(.*?)<\/span>/)?.[1] || '';
      avatar.appendChild(avatarText);

      const content = new FakeElement('div', this.ownerDocument);
      content.className = 'agent-message-content';
      const body = new FakeElement('div', this.ownerDocument);
      body.className = 'agent-message-body';
      body.innerHTML = this._html.match(/<div class="agent-message-body">([\s\S]*?)<\/div>/)?.[1] || '';
      const time = new FakeElement('time', this.ownerDocument);
      time.className = 'agent-message-time';
      time.innerHTML = this._html.match(/<time class="agent-message-time"[^>]*>([\s\S]*?)<\/time>/)?.[1] || '';
      content.appendChild(body);
      content.appendChild(time);

      this.appendChild(avatar);
      this.appendChild(content);
    }
  }

  get innerHTML() {
    return this._html;
  }

  get textContent() {
    if (this.children.length === 0) {
      return stripTags(this._html);
    }
    return this.children.map((child) => child.textContent).join(' ').replace(/\s+/g, ' ').trim();
  }

  get scrollHeight() {
    return this.children.length;
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
  submit.innerHTML = '<i class="fa fa-paper-plane" aria-hidden="true"></i><span class="visually-hidden">发送</span>';

  root.appendChild(messages);
  root.appendChild(form);
  form.appendChild(input);
  form.appendChild(submit);

  root.querySelector = (selector) => {
    switch (selector) {
      case '[data-agent-messages]':
        return messages;
      case '[data-agent-form]':
        return form;
      case '[data-agent-input]':
        return input;
      case '[data-agent-submit]':
        return submit;
      default:
        return FakeElement.prototype.querySelector.call(root, selector);
    }
  };

  return { root, messages, input, submit };
}

async function main() {
  const document = new FakeDocument();
  const sessionStorage = new Map();
  const window = {
    document,
    location: {
      pathname: '/teacher-ai-tools.html'
    },
    sessionStorage: {
      getItem(key) {
        return sessionStorage.has(key) ? sessionStorage.get(key) : null;
      },
      setItem(key, value) {
        sessionStorage.set(key, String(value));
      },
      removeItem(key) {
        sessionStorage.delete(key);
      }
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
    location: window.location,
    fetch: async (url) => {
      if (!String(url).includes('/api/agent/chat/stream')) {
        throw new Error(`Unexpected fetch URL: ${url}`);
      }
      const encoder = new TextEncoder();
      const stream = new ReadableStream({
        start(controller) {
          controller.enqueue(encoder.encode('event: session\ndata: {"sessionId":"42"}\n\n'));
          setTimeout(() => {
            controller.enqueue(encoder.encode('event: delta\ndata: {"text":"正在生成"}\n\n'));
          }, 120);
          setTimeout(() => {
            controller.enqueue(encoder.encode('event: result\ndata: {"sessionId":"42","responseType":"TEXT","message":"生成完成"}\n\n'));
            controller.close();
          }, 180);
        }
      });
      return new Response(stream, {
        status: 200,
        headers: {
          'Content-Type': 'text/event-stream'
        }
      });
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
    clearTimeout,
    console
  };
  context.global = window;
  window.fetch = context.fetch;

  const scriptPath = path.resolve(__dirname, '../frontend/dist/agent-chat-panel.js');
  const scriptContent = fs.readFileSync(scriptPath, 'utf8');
  vm.runInNewContext(scriptContent, context, { filename: scriptPath });

  const { root, messages } = createRoot(document);
  const panel = new window.AgentChatPanel(root, { role: 'TEACHER' });

  const sendPromise = panel.send('生成课堂练习题');
  await new Promise((resolve) => setTimeout(resolve, 30));

  const thinkingMessages = messages.children.filter((node) => node.classList.contains('agent-message-agent'));
  const thinkingState = {
    count: thinkingMessages.length,
    texts: thinkingMessages.map((node) => node.textContent)
  };

  assert(
    thinkingState.count === 1,
    'thinking phase should render exactly one agent placeholder message',
    thinkingState
  );
  assert(
    thinkingState.texts[0]?.includes('思考中'),
    'thinking placeholder should show visible thinking text',
    thinkingState
  );

  await sendPromise;

  const finalMessages = messages.children.filter((node) => node.classList.contains('agent-message-agent'));
  const finalState = {
    count: finalMessages.length,
    texts: finalMessages.map((node) => node.textContent)
  };

  assert(finalState.count === 1, 'final text response should still use a single agent message', finalState);
  assert(finalState.texts[0]?.includes('生成完成'), 'final agent message should contain streamed result text', finalState);

  console.log('agent chat thinking placeholder verification OK');
}

main().catch((error) => {
  console.error(error.message);
  if (error.details !== undefined) {
    console.error(JSON.stringify(error.details, null, 2));
  }
  process.exit(1);
});
