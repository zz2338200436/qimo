const fs = require('node:fs');
const path = require('node:path');

const scriptPath = path.resolve(__dirname, 'verify-agent-frontend-browser-smoke.js');
const content = fs.readFileSync(scriptPath, 'utf8');

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

assert(
  !content.includes('await response.text()'),
  'browser smoke should not read SSE payloads via response.text(); use page-rendered evidence instead'
);

assert(
  content.includes('window.__agentSmoke'),
  'browser smoke should install an Agent panel hook to capture rendered payloads'
);

assert(
  content.includes('page.waitForRequest('),
  'browser smoke should wait for the Agent chat request to be issued before checking rendered payloads'
);

assert(
  content.includes('lastPayload') && content.includes('lastError'),
  'browser smoke should capture rendered payloads and rendered errors from the page hook'
);

console.log('Agent frontend browser smoke contract verification passed.');
