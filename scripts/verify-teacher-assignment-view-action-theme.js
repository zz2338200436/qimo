const fs = require('node:fs');
const path = require('node:path');

const root = process.cwd();
const mirrors = [
  'frontend/dist',
  'major_assignment/src/main/resources/static',
];

const failures = [];

function read(baseDir, fileName) {
  return fs.readFileSync(path.join(root, baseDir, fileName), 'utf8');
}

function assertContains(label, content, needle, message) {
  if (!content.includes(needle)) {
    failures.push(`${label}: ${message}`);
  }
}

function assertNoMatch(label, content, regex, message) {
  const match = content.match(regex);
  if (match) {
    failures.push(`${label}: ${message} (${match[0]})`);
  }
}

for (const baseDir of mirrors) {
  const listScript = read(baseDir, 'teacher-assignments-lists.js');
  const page = read(baseDir, 'teacher-assignments.html');

  assertContains(
    `${baseDir}/teacher-assignments-lists.js`,
    listScript,
    'teacher-action-view',
    'view actions should use the teacher-action-view class'
  );
  assertNoMatch(
    `${baseDir}/teacher-assignments-lists.js`,
    listScript,
    /#4f46e5|rgba\(\s*79\s*,\s*70\s*,\s*229\s*,|btn-primary btn-sm" onclick="view(?:Assignment|Exam|Submission)/i,
    'view actions should not keep the old indigo or generic primary styling'
  );
  assertContains(
    `${baseDir}/teacher-assignments.html`,
    page,
    '.teacher-action-view',
    'missing page-scoped teacher view action style'
  );
  assertContains(
    `${baseDir}/teacher-assignments.html`,
    page,
    '#2563eb',
    'teacher view action should use the teacher primary blue hue'
  );
  assertNoMatch(
    `${baseDir}/teacher-assignments.html`,
    page,
    /#0891b2|#0e7490|rgba\(\s*8\s*,\s*145\s*,\s*178\s*,/i,
    'teacher view action should not use the off-palette cyan hue'
  );
  assertContains(
    `${baseDir}/teacher-assignments.html`,
    page,
    'teacher-assignments-lists.js?v=20260609-view-action-1',
    'page should reference the refreshed teacher assignments list script'
  );
}

const distListScript = read('frontend/dist', 'teacher-assignments-lists.js');
const staticListScript = read('major_assignment/src/main/resources/static', 'teacher-assignments-lists.js');
const distPage = read('frontend/dist', 'teacher-assignments.html');
const staticPage = read('major_assignment/src/main/resources/static', 'teacher-assignments.html');

if (distListScript !== staticListScript) {
  failures.push('teacher-assignments-lists.js: differs between frontend/dist and Spring Boot static mirror');
}
if (distPage !== staticPage) {
  failures.push('teacher-assignments.html: differs between frontend/dist and Spring Boot static mirror');
}

if (failures.length) {
  console.error('Teacher assignment view action theme verification failed:');
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log('Teacher assignment view action theme verification passed.');
