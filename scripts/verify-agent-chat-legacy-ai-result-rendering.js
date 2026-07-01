const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

const files = [
  'frontend/dist/agent-chat-panel.js',
  'major_assignment/src/main/resources/static/agent-chat-panel.js'
];

for (const file of files) {
  const content = fs.readFileSync(file, 'utf8');
  assertIncludes(
    content,
    'const legacyQuestions = extractLegacyQuestionPayload(value);',
    `${file} should derive legacy nested aiResult questions before generic object rendering.`
  );
  assertIncludes(
    content,
    'function extractLegacyQuestionPayload(value) {',
    `${file} should define legacy aiResult extraction helper.`
  );
  assertIncludes(
    content,
    'value.aiResult?.exam?.questions',
    `${file} should support nested aiResult.exam.questions payloads from older runtime services.`
  );
  assertIncludes(
    content,
    'value.aiResult?.questions',
    `${file} should support nested aiResult.questions payloads from older runtime services.`
  );
}

console.log('legacy aiResult question rendering verification passed.');
