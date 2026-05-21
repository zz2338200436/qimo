const fs = require('fs');
const path = require('path');

const htmlPath = path.join(__dirname, '..', 'frontend', 'dist', 'teacher-knowledge.html');
const content = fs.readFileSync(htmlPath, 'utf8');

function assertContains(pattern, message) {
  if (!pattern.test(content)) {
    console.error(`FAIL: ${message}`);
    process.exit(1);
  }
}

assertContains(/function normalizeKnowledgeDifficultyValue\(rawDifficulty\)/, 'missing difficulty normalization helper');
assertContains(/'中等'\s*:\s*'中级'/, 'missing 中等 -> 中级 mapping');
assertContains(/'较难'\s*:\s*'困难'/, 'missing 较难 -> 困难 mapping');
assertContains(
  /document\.getElementById\('knowledgeDifficulty'\)\.value\s*=\s*normalizeKnowledgeDifficultyValue\(knowledge\.difficulty\);/,
  'knowledge difficulty field does not use normalization helper'
);

console.log('teacher knowledge difficulty contract ok');
