const fs = require('fs');
const path = require('path');

const root = process.cwd();

const pairs = [
  ['styles.css', 'frontend/dist/styles.css', 'major_assignment/src/main/resources/static/styles.css'],
  ['student sidebar', 'frontend/dist/components/sidebar-nav.html', 'major_assignment/src/main/resources/static/components/sidebar-nav.html'],
  ['teacher sidebar', 'frontend/dist/components/teacher-sidebar-nav.html', 'major_assignment/src/main/resources/static/components/teacher-sidebar-nav.html'],
  ['student ai assistant', 'frontend/dist/student-ai-assistant.html', 'major_assignment/src/main/resources/static/student-ai-assistant.html'],
  ['student assignments', 'frontend/dist/student-assignments.html', 'major_assignment/src/main/resources/static/student-assignments.html'],
  ['student courses', 'frontend/dist/student-courses.html', 'major_assignment/src/main/resources/static/student-courses.html'],
  ['student dashboard', 'frontend/dist/student-dashboard.html', 'major_assignment/src/main/resources/static/student-dashboard.html'],
  ['student login', 'frontend/dist/student-login.html', 'major_assignment/src/main/resources/static/student-login.html'],
  ['student notifications', 'frontend/dist/student-notifications.html', 'major_assignment/src/main/resources/static/student-notifications.html'],
  ['student settings', 'frontend/dist/student-settings.html', 'major_assignment/src/main/resources/static/student-settings.html'],
  ['student stats', 'frontend/dist/student-stats.html', 'major_assignment/src/main/resources/static/student-stats.html'],
  ['teacher login', 'frontend/dist/teacher-login.html', 'major_assignment/src/main/resources/static/teacher-login.html'],
];

const files = Object.fromEntries(
  pairs.flatMap(([label, distPath, legacyPath]) => [
    [`${label}:dist`, read(distPath)],
    [`${label}:legacy`, read(legacyPath)],
  ])
);

const failures = [];

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

function assertContains(label, content, needle) {
  if (!content.includes(needle)) {
    failures.push(`${label} is missing ${needle}`);
  }
}

function assertNotContains(label, content, needle) {
  if (content.includes(needle)) {
    failures.push(`${label} still contains ${needle}`);
  }
}

function assertEqual(label, left, right) {
  if (left !== right) {
    failures.push(`${label} differs between frontend/dist and legacy static mirror`);
  }
}

const styles = files['styles.css:dist'];
[
  '--teacher-primary: #2563eb',
  '--teacher-primary-strong: #1d4ed8',
  '--teacher-shell: #0f172a',
  '--student-primary: #0d9488',
  '--student-primary-strong: #0f766e',
  '--student-accent: #14b8a6',
  'body.student-theme',
  'body.teacher-theme',
  '[data-role-theme="student"]',
  '[data-role-theme="teacher"]',
].forEach((needle) => assertContains('frontend/dist/styles.css', styles, needle));

assertContains('student sidebar', files['student sidebar:dist'], 'data-role-theme="student"');
assertContains('student sidebar', files['student sidebar:dist'], '#0f766e');
assertContains('student sidebar', files['student sidebar:dist'], '#0d9488');
assertContains('student sidebar', files['student sidebar:dist'], '#14b8a6');

assertContains('teacher sidebar', files['teacher sidebar:dist'], 'data-role-theme="teacher"');
assertContains('teacher sidebar', files['teacher sidebar:dist'], '#0f172a');
assertContains('teacher sidebar', files['teacher sidebar:dist'], '#2563eb');
assertContains('teacher sidebar', files['teacher sidebar:dist'], '#1d4ed8');

assertContains('student login', files['student login:dist'], 'student-login-page');
assertContains('student login', files['student login:dist'], '#0f766e');
assertContains('student login', files['student login:dist'], '#0d9488');
assertContains('student login', files['student login:dist'], '#14b8a6');

[
  'student ai assistant',
  'student assignments',
  'student courses',
  'student dashboard',
  'student notifications',
  'student settings',
  'student stats',
].forEach((label) => {
  assertContains(label, files[`${label}:dist`], 'student-theme');
});

assertContains('teacher login', files['teacher login:dist'], 'teacher-login-page');
assertContains('teacher login', files['teacher login:dist'], '#0f172a');
assertContains('teacher login', files['teacher login:dist'], '#2563eb');
assertContains('teacher login', files['teacher login:dist'], '#1d4ed8');

[
  ['student sidebar', files['student sidebar:dist']],
  ['teacher sidebar', files['teacher sidebar:dist']],
  ['student login', files['student login:dist']],
  ['teacher login', files['teacher login:dist']],
].forEach(([label, content]) => {
  assertNotContains(label, content, '#667eea');
  assertNotContains(label, content, '#764ba2');
});

pairs.forEach(([label, distPath, legacyPath]) => {
  assertEqual(label, read(distPath), read(legacyPath));
});

if (failures.length) {
  console.error('Role color theme verification failed:');
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log('Role color theme verification passed.');
