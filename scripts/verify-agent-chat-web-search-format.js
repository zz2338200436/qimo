const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

const frontendPath = 'frontend/dist/agent-chat-panel.js';
const staticPath = 'major_assignment/src/main/resources/static/agent-chat-panel.js';
const frontendContent = fs.readFileSync(frontendPath, 'utf8');
const staticContent = fs.readFileSync(staticPath, 'utf8');

if (frontendContent !== staticContent) {
  throw new Error('agent-chat-panel.js mirror mismatch. Keep frontend/dist and major_assignment static files in sync.');
}

[
  'function parseWebSearchResponse(value) {',
  'function parseWebSearchSources(answer, query, body) {',
  'function parseLooseWebSearchSources(body) {',
  'function looksLikeLooseWebSearchResponse(text) {',
  'function renderWebSearchResponse(value) {',
  "const sourcesMatch = text.match(/(?:^|\\n)\\s*(?:可参考的来源|以下是可参考的来源)",
  "const legacyHeaderMatch = text.match(/^联网搜索完成[，,]\\s*以下是可参考的来源",
  'if (!looksLikeLooseWebSearchResponse(text)) {',
  'const indexOnlyMatch = line.match(/^(\\d+)\\s*$/);',
  'const inlineLinkMatch = line.match(/链接[：:]\\s*(https?:\\/\\/\\S+)/);',
  'class="agent-web-search-response"',
  'class="agent-web-search-summary"',
  'class="agent-web-search-explanation"',
  'class="agent-web-search-results"',
  'class="agent-web-search-sources"',
  '<summary>',
  'class="agent-web-search-result"',
  'class="agent-web-search-result-link"',
  'const webSearchHtml = renderWebSearchResponse(value);',
  'if (webSearchHtml) {',
  'return webSearchHtml;'
].forEach((snippet) => {
  assertIncludes(frontendContent, snippet, 'agent chat web-search formatting contract mismatch.');
});

console.log('agent chat web-search format contract OK');
