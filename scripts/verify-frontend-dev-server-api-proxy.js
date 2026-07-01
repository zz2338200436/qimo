const http = require('node:http');
const path = require('node:path');
const { spawn } = require('node:child_process');

const REPO_ROOT = path.resolve(__dirname, '..');
const FRONTEND_PROXY_MODULE = path.join(REPO_ROOT, 'scripts', 'frontend_dev_server.py');
const MOCK_GATEWAY_PORT = 19081;
const PROXY_PORT = 15501;

function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function waitFor(check, timeoutMs, label) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (await check()) {
      return;
    }
    await delay(100);
  }
  throw new Error(`Timed out waiting for ${label}`);
}

function assert(condition, message, details) {
  if (!condition) {
    const error = new Error(message);
    error.details = details;
    throw error;
  }
}

function startMockGateway() {
  const captchaPayload = Buffer.from('fake-captcha-image');
  const chatPayload = Buffer.from(JSON.stringify({
    sessionId: 'proxy-test',
    responseType: 'TEXT',
    message: 'ok'
  }));
  const renamedSessionPayload = Buffer.from(JSON.stringify({
    success: true,
    data: {
      sessionId: '88',
      title: '代理重命名会话'
    }
  }));

  const server = http.createServer((req, res) => {
    if (req.url === '/api/public/captcha') {
      res.writeHead(200, {
        'Content-Type': 'image/jpeg',
        'Content-Length': String(captchaPayload.length),
        'X-Captcha-Key': 'proxy-captcha-key'
      });
      if (req.method !== 'HEAD') {
        res.end(captchaPayload);
      } else {
        res.end();
      }
      return;
    }

    if (req.method === 'POST' && req.url === '/api/agent/chat') {
      req.resume();
      res.writeHead(200, {
        'Content-Type': 'application/json; charset=utf-8',
        'Content-Length': String(chatPayload.length)
      });
      res.end(chatPayload);
      return;
    }

    if (req.method === 'PATCH' && req.url === '/api/agent/sessions/88') {
      req.resume();
      res.writeHead(200, {
        'Content-Type': 'application/json; charset=utf-8',
        'Content-Length': String(renamedSessionPayload.length)
      });
      res.end(renamedSessionPayload);
      return;
    }

    res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('not found');
  });

  return new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(MOCK_GATEWAY_PORT, '127.0.0.1', () => resolve(server));
  });
}

function startProxy() {
  const pythonCode = [
    'import importlib.util',
    'import os',
    'import pathlib',
    'module_path = pathlib.Path(os.environ["FRONTEND_PROXY_MODULE"])',
    'spec = importlib.util.spec_from_file_location("frontend_dev_server", module_path)',
    'mod = importlib.util.module_from_spec(spec)',
    'spec.loader.exec_module(mod)',
    'mod.GATEWAY_HOST = "127.0.0.1"',
    'mod.GATEWAY_PORT = int(os.environ["MOCK_GATEWAY_PORT"])',
    'with mod.ReusableThreadingTCPServer(("127.0.0.1", int(os.environ["PROXY_PORT"])), mod.FrontendProxyHandler) as httpd:',
    '    httpd.serve_forever()'
  ].join('\n');

  const child = spawn('python', ['-c', pythonCode], {
    cwd: REPO_ROOT,
    env: {
      ...process.env,
      FRONTEND_PROXY_MODULE,
      MOCK_GATEWAY_PORT: String(MOCK_GATEWAY_PORT),
      PROXY_PORT: String(PROXY_PORT)
    },
    stdio: ['ignore', 'pipe', 'pipe']
  });

  let stderr = '';
  child.stderr.on('data', chunk => {
    stderr += chunk.toString();
  });

  return {
    child,
    getStderr: () => stderr
  };
}

function probeProxyReady() {
  return new Promise(resolve => {
    const req = http.request({
      host: '127.0.0.1',
      port: PROXY_PORT,
      path: '/missing.html',
      method: 'GET',
      timeout: 500
    }, res => {
      res.resume();
      resolve(true);
    });
    req.on('error', () => resolve(false));
    req.on('timeout', () => {
      req.destroy();
      resolve(false);
    });
    req.end();
  });
}

function makeRequest({ method, path, headers, body }) {
  return new Promise((resolve, reject) => {
    const req = http.request({
      host: '127.0.0.1',
      port: PROXY_PORT,
      path,
      method,
      headers
    }, res => {
      const chunks = [];
      res.on('data', chunk => {
        chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk));
      });
      res.on('end', () => {
        resolve({
          statusCode: res.statusCode,
          headers: res.headers,
          body: Buffer.concat(chunks)
        });
      });
    });

    req.on('error', reject);
    if (body) {
      req.write(body);
    }
    req.end();
  });
}

(async () => {
  let gatewayServer;
  let proxy;
  try {
    gatewayServer = await startMockGateway();
    proxy = startProxy();
    await waitFor(probeProxyReady, 5000, 'frontend proxy readiness');

    const headResponse = await makeRequest({
      method: 'HEAD',
      path: '/api/public/captcha'
    });
    assert(headResponse.statusCode === 200, 'proxy should forward HEAD /api requests', headResponse);
    assert(headResponse.headers['x-captcha-key'] === 'proxy-captcha-key', 'proxy should preserve HEAD response headers', headResponse);
    assert(headResponse.body.length === 0, 'HEAD responses should not include a body', headResponse);

    const getResponse = await makeRequest({
      method: 'GET',
      path: '/api/public/captcha'
    });
    assert(getResponse.statusCode === 200, 'proxy should forward GET /api requests', getResponse);
    assert(getResponse.headers['content-type'] === 'image/jpeg', 'proxy should preserve content type for binary responses', getResponse);
    assert(getResponse.headers['content-length'] === String(Buffer.byteLength('fake-captcha-image')), 'proxy should preserve a correct content length for binary responses', getResponse);
    assert(getResponse.body.equals(Buffer.from('fake-captcha-image')), 'proxy should return the full binary response body', {
      ...getResponse,
      body: getResponse.body.toString('utf8')
    });

    const postResponse = await makeRequest({
      method: 'POST',
      path: '/api/agent/chat',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ message: 'hello' })
    });
    assert(postResponse.statusCode === 200, 'proxy should forward JSON POST requests', postResponse);
    assert(postResponse.headers['content-type'] === 'application/json; charset=utf-8', 'proxy should preserve JSON response headers', postResponse);
    assert(JSON.parse(postResponse.body.toString('utf8')).sessionId === 'proxy-test', 'proxy should return the full JSON response body', {
      ...postResponse,
      body: postResponse.body.toString('utf8')
    });

    const patchResponse = await makeRequest({
      method: 'PATCH',
      path: '/api/agent/sessions/88',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ title: '代理重命名会话' })
    });
    assert(patchResponse.statusCode === 200, 'proxy should forward JSON PATCH requests for agent session rename', patchResponse);
    assert(patchResponse.headers['content-type'] === 'application/json; charset=utf-8', 'proxy should preserve PATCH JSON response headers', patchResponse);
    assert(JSON.parse(patchResponse.body.toString('utf8')).data.title === '代理重命名会话', 'proxy should return the full PATCH JSON response body', {
      ...patchResponse,
      body: patchResponse.body.toString('utf8')
    });

    console.log('PASS: frontend proxy forwarded HEAD, binary GET, JSON POST, and JSON PATCH responses');
  } catch (error) {
    console.error(error.message);
    if (error.details) {
      console.error(JSON.stringify(error.details, null, 2));
    }
    if (proxy?.getStderr()?.trim()) {
      console.error(proxy.getStderr().trim());
    }
    process.exitCode = 1;
  } finally {
    if (proxy?.child && !proxy.child.killed) {
      proxy.child.kill();
    }
    if (gatewayServer) {
      await new Promise(resolve => gatewayServer.close(resolve));
    }
  }
})();
