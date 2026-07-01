const http = require('node:http');
const path = require('node:path');
const { spawn } = require('node:child_process');

const REPO_ROOT = path.resolve(__dirname, '..');
const FRONTEND_PROXY_MODULE = path.join(REPO_ROOT, 'scripts', 'frontend_dev_server.py');
const MOCK_GATEWAY_PORT = 19080;
const PROXY_PORT = 15500;
const FIRST_EVENT_DEADLINE_MS = 700;

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

function startMockGateway() {
  const server = http.createServer((req, res) => {
    if (req.method === 'POST' && req.url === '/api/agent/chat/stream') {
      req.resume();
      res.writeHead(200, {
        'Content-Type': 'text/event-stream',
        'Cache-Control': 'no-cache',
        Connection: 'keep-alive'
      });
      res.write('event: session\n');
      res.write('data: {"sessionId":"stream-test"}\n\n');
      setTimeout(() => {
        res.write('event: delta\n');
        res.write('data: {"text":"hello"}\n\n');
      }, 250);
      setTimeout(() => {
        res.write('event: result\n');
        res.write('data: {"sessionId":"stream-test","responseType":"TEXT","message":"hello"}\n\n');
        res.write('event: done\n');
        res.write('data: {}\n\n');
        res.end();
      }, 1300);
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

  child.on('exit', code => {
    if (code !== null && code !== 0) {
      console.error(`frontend proxy exited with code ${code}`);
      if (stderr.trim()) {
        console.error(stderr.trim());
      }
    }
  });

  return { child, getStderr: () => stderr };
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

function requestStreamThroughProxy() {
  return new Promise((resolve, reject) => {
    const startedAt = Date.now();
    let headersAt = null;
    let firstChunkAt = null;
    let body = '';

    const req = http.request({
      host: '127.0.0.1',
      port: PROXY_PORT,
      path: '/api/agent/chat/stream',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream'
      }
    }, res => {
      headersAt = Date.now() - startedAt;
      res.setEncoding('utf8');
      res.on('data', chunk => {
        if (firstChunkAt == null) {
          firstChunkAt = Date.now() - startedAt;
        }
        body += chunk;
      });
      res.on('end', () => resolve({ headersAt, firstChunkAt, body }));
    });

    req.on('error', reject);
    req.write(JSON.stringify({ message: 'stream proxy test' }));
    req.end();
  });
}

function assert(condition, message, details) {
  if (!condition) {
    const error = new Error(message);
    error.details = details;
    throw error;
  }
}

(async () => {
  let gatewayServer;
  let proxy;
  try {
    gatewayServer = await startMockGateway();
    proxy = startProxy();
    await waitFor(probeProxyReady, 5000, 'frontend proxy readiness');

    const result = await requestStreamThroughProxy();
    assert(result.headersAt != null, 'proxy should return response headers');
    assert(result.firstChunkAt != null, 'proxy should forward the first SSE chunk');
    assert(
      result.headersAt < FIRST_EVENT_DEADLINE_MS,
      'proxy should expose SSE headers before upstream finishes',
      result
    );
    assert(
      result.firstChunkAt < FIRST_EVENT_DEADLINE_MS,
      'proxy should forward the first SSE chunk before upstream finishes',
      result
    );
    assert(
      result.body.includes('event: result') && result.body.includes('"responseType":"TEXT"'),
      'proxy should preserve SSE payload body',
      result
    );

    console.log(`PASS: frontend proxy streamed SSE early (headers=${result.headersAt}ms, firstChunk=${result.firstChunkAt}ms)`);
  } catch (error) {
    console.error(error.message);
    if (error.details) {
      console.error(JSON.stringify(error.details, null, 2));
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
