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

assertContains(
  /async function mergeKnowledgeAnalysisWithPointList\(analysisData, filters\)/,
  'missing mergeKnowledgeAnalysisWithPointList helper'
);
assertContains(
  /const rawPointList = await getKnowledgePointList\(\);/,
  'merge helper should load raw knowledge point list'
);
assertContains(
  /formattedData = await mergeKnowledgeAnalysisWithPointList\(formattedData, filters\);/,
  'knowledge analysis data should be merged with raw knowledge point list'
);
assertContains(
  /distribution\.push\(normalizeKnowledgeDistributionEntry\(point, distribution\.length \+ 1\)\);/,
  'merge helper should append missing knowledge points to the rendered distribution'
);
assertContains(
  /while \(excellentStudentAverage\.length < distribution\.length\) \{\s*excellentStudentAverage\.push\(0\.0\);/s,
  'merge helper should pad excellentStudentAverage for newly added knowledge points'
);

console.log('teacher knowledge analysis merge contract ok');
