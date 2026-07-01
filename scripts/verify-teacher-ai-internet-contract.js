const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

function assertSameFile(leftPath, rightPath, label) {
  const left = fs.readFileSync(leftPath, 'utf8');
  const right = fs.readFileSync(rightPath, 'utf8');
  if (left !== right) {
    throw new Error(`${label} mirror mismatch. Keep frontend/dist and major_assignment static files in sync.`);
  }
}

const frontendPagePath = 'frontend/dist/teacher-ai-tools.html';
const staticPagePath = 'major_assignment/src/main/resources/static/teacher-ai-tools.html';
const frontendPage = fs.readFileSync(frontendPagePath, 'utf8');

assertSameFile(frontendPagePath, staticPagePath, 'teacher-ai-tools.html');

[
  'data-teacher-ai-web-search',
  '联网搜索',
  'teacher-ai-input-web-search',
  'teacher-ai-input-web-search-active',
  'data-tooltip="联网搜索"',
  'aria-pressed="false"',
  'data-agent-input placeholder="输入课程、作业、题目、试卷或教学目标..."',
  "const webSearchMessage = `联网搜索 ${rawMessage}`;",
  'webSearchButton.setAttribute(\'aria-pressed\', String(isActive));',
  'event.detail.message = webSearchMessage;',
  'event.detail.displayMessage = rawMessage;'
].forEach((snippet) => {
  assertIncludes(frontendPage, snippet, 'teacher AI internet entry contract mismatch.');
});

const inputShellIndex = frontendPage.indexOf('<div class="agent-input-shell">');
const formIndex = frontendPage.indexOf('<form class="agent-form" data-agent-form>', inputShellIndex);
const textareaIndex = frontendPage.indexOf('data-agent-input', formIndex);
const webSearchIndex = frontendPage.indexOf('data-teacher-ai-web-search', formIndex);
const submitIndex = frontendPage.indexOf('data-agent-submit', formIndex);
const emptyStateIndex = frontendPage.indexOf('data-teacher-ai-empty-state');
const emptyStateEndIndex = frontendPage.indexOf('</div>', frontendPage.indexOf('teacher-ai-example-prompts', emptyStateIndex));

if (inputShellIndex < 0 || formIndex < 0 || textareaIndex < 0 || webSearchIndex < 0 || submitIndex < 0) {
  throw new Error('teacher AI internet button should live inside the persistent composer form.');
}

if (!(textareaIndex < webSearchIndex && webSearchIndex < submitIndex)) {
  throw new Error('teacher AI internet button should sit between the input textarea and send button.');
}

if (emptyStateIndex < 0 || emptyStateEndIndex < 0) {
  throw new Error('teacher AI empty state should remain available for non-search starter prompts.');
}

if (frontendPage.slice(emptyStateIndex, emptyStateEndIndex).includes('data-teacher-ai-web-search')) {
  throw new Error('teacher AI empty-state starter prompts should not duplicate the persistent internet search toggle.');
}

if ((frontendPage.match(/<button[^>]*data-teacher-ai-web-search/g) || []).length !== 1) {
  throw new Error('teacher AI page should expose exactly one internet search toggle.');
}

const webSearchButtonMatch = frontendPage.match(/<button[^>]*data-teacher-ai-web-search[\s\S]*?<\/button>/);
if (!webSearchButtonMatch) {
  throw new Error('teacher AI internet toggle button markup should exist.');
}

if (!webSearchButtonMatch[0].includes('<i class="fa fa-globe" aria-hidden="true"></i>')) {
  throw new Error('teacher AI internet toggle should use the globe icon markup.');
}

if (webSearchButtonMatch[0].includes('teacher-ai-input-web-search-label') || />\s*联网搜索\s*</.test(webSearchButtonMatch[0])) {
  throw new Error('teacher AI internet toggle should render as an icon-only button without visible text.');
}

[
  "const message = query ? `联网搜索 ${query}` : '联网搜索 ';",
  'input.form?.requestSubmit();'
].forEach((snippet) => {
  if (frontendPage.includes(snippet)) {
    throw new Error(`teacher AI internet toggle should not mutate input text or auto-submit. Found: ${snippet}`);
  }
});

console.log('teacher AI internet contract OK');
