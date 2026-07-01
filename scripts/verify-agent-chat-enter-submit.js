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

  dispatchEvent(event) {
    event.target = event.target || this;
    event.currentTarget = this;
    const handlers = this.eventListeners.get(event.type) || [];
    for (const handler of handlers) {
      handler.call(this, event);
    }
    return !event.defaultPrevented;
  }

  focus() {}

  contains(node) {
    if (node === this) {
      return true;
    }
    return this.children.some((child) => child.contains(node));
  }

  matchesSelector(selector) {
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

  return { root, input };
}

function createKeyboardEvent(overrides = {}) {
  return {
    type: 'keydown',
    key: 'Enter',
    shiftKey: false,
    ctrlKey: false,
    metaKey: false,
    altKey: false,
    defaultPrevented: false,
    preventDefault() {
      this.defaultPrevented = true;
    },
    ...overrides
  };
}

async function main() {
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
    location: { pathname: '/student-ai-assistant.html' }
  };

  const context = {
    window,
    document,
    location: window.location,
    fetch: async () => {
      throw new Error('fetch should not run in enter submit verification');
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
    requestAnimationFrame(callback) {
      return setTimeout(callback, 0);
    },
    setTimeout,
    clearTimeout,
    console
  };
  context.global = window;

  const scriptPath = path.resolve(__dirname, '../frontend/dist/agent-chat-panel.js');
  const scriptContent = fs.readFileSync(scriptPath, 'utf8');
  vm.runInNewContext(scriptContent, context, { filename: scriptPath });

  const { root, input } = createRoot(document);
  const panel = new window.AgentChatPanel(root, { role: 'STUDENT' });

  const sentMessages = [];
  panel.send = async (message) => {
    sentMessages.push(message);
  };

  input.value = '生成学习建议';
  const enterEvent = createKeyboardEvent();
  input.dispatchEvent(enterEvent);

  assert(
    enterEvent.defaultPrevented,
    'plain Enter should prevent default so textarea does not insert a newline before submit'
  );
  assert(
    sentMessages.length === 1 && sentMessages[0] === '生成学习建议',
    'plain Enter should submit the trimmed textarea content',
    sentMessages
  );

  input.value = '第一行';
  const shiftEnterEvent = createKeyboardEvent({ shiftKey: true });
  input.dispatchEvent(shiftEnterEvent);

  assert(
    !shiftEnterEvent.defaultPrevented,
    'Shift+Enter should keep newline behavior and must not be intercepted'
  );
  assert(
    sentMessages.length === 1,
    'Shift+Enter should not trigger a send action',
    sentMessages
  );

  console.log('agent chat enter submit verification OK');
}

main().catch((error) => {
  console.error(error.message);
  if (error.details !== undefined) {
    console.error(JSON.stringify(error.details, null, 2));
  }
  process.exit(1);
});
