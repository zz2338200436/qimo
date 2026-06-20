const http = require('node:http');
const https = require('node:https');

const FRONTEND_BASE_URL = process.env.FRONTEND_BASE_URL || 'http://localhost:5500';
const GATEWAY_BASE_URL = process.env.GATEWAY_BASE_URL || 'http://localhost:8080';
const CONFIG_BASE_URL = process.env.CONFIG_BASE_URL || 'http://localhost:8888';

function assert(condition, message, details) {
  if (!condition) {
    const error = new Error(message);
    if (details !== undefined) {
      error.details = details;
    }
    throw error;
  }
}

function requestWithTimeout(url, options = {}) {
  return new Promise((resolve, reject) => {
    const parsed = new URL(url);
    const client = parsed.protocol === 'https:' ? https : http;
    const request = client.request(parsed, { method: options.method || 'GET' }, response => {
      const chunks = [];
      response.on('data', chunk => chunks.push(chunk));
      response.on('end', () => {
        const body = Buffer.concat(chunks);
        const headers = new Map(
          Object.entries(response.headers).map(([key, value]) => [
            key.toLowerCase(),
            Array.isArray(value) ? value.join(', ') : String(value || '')
          ])
        );
        resolve({
          ok: response.statusCode >= 200 && response.statusCode < 300,
          status: response.statusCode,
          statusText: response.statusMessage || '',
          headers,
          text: async () => body.toString('utf8'),
          arrayBuffer: async () => body
        });
      });
    });
    request.on('error', reject);
    request.setTimeout(options.timeoutMs || 5000, () => {
      request.destroy(new Error(`request timed out after ${options.timeoutMs || 5000}ms`));
    });
    request.end();
  });
}

async function requireHttpOk(url, label) {
  let response;
  try {
    response = await requestWithTimeout(url);
  } catch (error) {
    const cause = [
      error.message,
      error.code,
      error.address,
      error.port
    ].filter(Boolean).join(' ');
    throw new Error(`${label} is not reachable at ${url}. Start scripts/start-idea-dev-frontend.ps1 and the IDEA Java services first. Cause: ${cause || error.name || 'connection failed'}`);
  }
  assert(response.ok, `${label} should return HTTP 2xx at ${url}`, {
    status: response.status,
    statusText: response.statusText
  });
  return response;
}

async function runStep(results, name, action) {
  try {
    await action();
    results.push({ name, ok: true });
    console.log(`[PASS] ${name}`);
  } catch (error) {
    results.push({ name, ok: false, error: error.message, details: error.details });
    console.error(`[FAIL] ${name}`);
    console.error(`  ${error.message}`);
    if (error.details !== undefined) {
      console.error(`  details: ${JSON.stringify(error.details)}`);
    }
    throw error;
  }
}

async function verifyConfigServerDevProfile() {
  const response = await requireHttpOk(`${CONFIG_BASE_URL}/gateway/dev`, 'Config Server gateway/dev profile');
  const text = await response.text();
  assert(
    text.includes('http://localhost:5500') || text.includes('http://127.0.0.1:5500'),
    'gateway/dev config should expose the IDEA frontend server URL',
    text.slice(0, 500)
  );
}

async function verifyFrontendResources() {
  const studentLogin = await requireHttpOk(`${FRONTEND_BASE_URL}/student-login.html`, 'student login page');
  const teacherLogin = await requireHttpOk(`${FRONTEND_BASE_URL}/teacher-login.html`, 'teacher login page');
  for (const [label, response] of [['student login page', studentLogin], ['teacher login page', teacherLogin]]) {
    const text = await response.text();
    assert(
      text.includes('/api/auth/captcha') && text.includes('CaptchaKey'),
      `${label} should use gateway captcha and CaptchaKey`,
      text.slice(0, 500)
    );
  }
  await requireHttpOk(`${FRONTEND_BASE_URL}/agent-chat-panel.js`, 'agent-chat-panel.js');
  await requireHttpOk(`${FRONTEND_BASE_URL}/agent-chat-panel.css`, 'agent-chat-panel.css');
}

async function verifyGatewayStaticRoute() {
  const response = await requireHttpOk(`${GATEWAY_BASE_URL}/student-login.html`, 'Gateway static frontend route');
  const text = await response.text();
  assert(
    text.includes('/api/auth/captcha') && text.includes('CaptchaKey'),
    'Gateway static route should serve the same login page that uses /api/auth/captcha and CaptchaKey',
    text.slice(0, 500)
  );
}

async function verifyCaptchaThroughFrontend(label) {
  const url = `${FRONTEND_BASE_URL}/api/auth/captcha?timestamp=${Date.now()}`;
  const response = await requireHttpOk(url, label);
  const contentType = response.headers.get('content-type') || '';
  const captchaKey = response.headers.get('x-captcha-key') || '';
  const bytes = new Uint8Array(await response.arrayBuffer());
  assert(contentType.includes('image/'), `${label} should return an image content type`, { contentType });
  assert(Boolean(captchaKey), `${label} should expose X-Captcha-Key`, { headers: Object.fromEntries(response.headers) });
  assert(bytes.length > 100, `${label} should return non-empty captcha bytes`, { byteLength: bytes.length });
}

(async () => {
  const results = [];

  await runStep(results, 'config-server exposes gateway dev profile for IDEA frontend', verifyConfigServerDevProfile);
  await runStep(results, 'frontend dev server serves login and Agent assets', verifyFrontendResources);
  await runStep(results, 'gateway static route points to the IDEA frontend server', verifyGatewayStaticRoute);

  await runStep(results, 'captcha works through 5500 -> gateway -> auth-service', async () => {
    await verifyCaptchaThroughFrontend('frontend proxied /api/auth/captcha');
  });

  console.log(`IDEA dev runtime smoke passed: ${results.length}`);
})().catch(error => {
  console.error(error.message);
  if (error.details !== undefined) {
    console.error(JSON.stringify(error.details, null, 2));
  }
  process.exit(1);
});
