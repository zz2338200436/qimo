const fs = require('node:fs');
const path = require('node:path');

const root = process.cwd();
const distRoot = path.join(root, 'frontend', 'dist');
const staticRoot = path.join(root, 'major_assignment', 'src', 'main', 'resources', 'static');

const failures = [];
const ROLE_THEME_VERSION = '20260608-theme-4';
const skippedRoleThemeFiles = new Set([
  'student-ai-assistant.html',
]);

const ignoredDirs = new Set(['lib']);
const scannedExtensions = new Set(['.html', '.css', '.js']);

function listFiles(dir) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  return entries.flatMap((entry) => {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (ignoredDirs.has(entry.name)) return [];
      return listFiles(fullPath);
    }
    if (!scannedExtensions.has(path.extname(entry.name))) return [];
    return [fullPath];
  });
}

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

function relativeFromDist(fullPath) {
  return path.relative(distRoot, fullPath).replace(/\\/g, '/');
}

function assertNoMatch(label, content, regex, message) {
  const match = content.match(regex);
  if (match) {
    failures.push(`${label}: ${message} (${match[0]})`);
  }
}

function assertContains(label, content, needle, message) {
  if (!content.includes(needle)) {
    failures.push(`${label}: ${message}`);
  }
}

for (const fullPath of listFiles(distRoot)) {
  const relativePath = relativeFromDist(fullPath);
  const content = fs.readFileSync(fullPath, 'utf8');

  if (skippedRoleThemeFiles.has(relativePath)) {
    continue;
  }

  assertNoMatch(relativePath, content, /20260523-1/, 'still references the old cache-busting asset version');
  assertNoMatch(relativePath, content, /20260608-theme-2/, 'still references the stale role theme cache-busting asset version');
  assertNoMatch(relativePath, content, /20260608-theme-3/, 'still references the stale role theme cache-busting asset version');

  const userDropdownUrlMatches = content.match(/components\/user-dropdown\.html\?v=([^'")]+)/g) || [];
  userDropdownUrlMatches.forEach((match) => {
    if (!match.endsWith(ROLE_THEME_VERSION)) {
      failures.push(`${relativePath}: user dropdown component should use ${ROLE_THEME_VERSION} cache-busting version (${match})`);
    }
  });

  const commonUiUrlMatches = content.match(/common-ui\.js\?v=([^'")]+)/g) || [];
  commonUiUrlMatches.forEach((match) => {
    if (!match.endsWith(ROLE_THEME_VERSION)) {
      failures.push(`${relativePath}: common UI script should use ${ROLE_THEME_VERSION} cache-busting version (${match})`);
    }
  });

  assertNoMatch(relativePath, content, /#667eea|#764ba2|rgba\(\s*102\s*,\s*126\s*,\s*234\s*,|rgba\(\s*118\s*,\s*75\s*,\s*162\s*,/i, 'still contains the previous purple teacher palette');

  if (relativePath.startsWith('student-')) {
    assertNoMatch(relativePath, content, /#3b82f6|#2563eb|#1d4ed8|rgba\(\s*59\s*,\s*130\s*,\s*246\s*,|rgba\(\s*37\s*,\s*99\s*,\s*235\s*,|rgba\(\s*29\s*,\s*78\s*,\s*216\s*,/i, 'still contains teacher/default-blue primary styling on a student page');
  }

  if (relativePath.startsWith('teacher-') && relativePath.endsWith('.html')) {
    if (relativePath !== 'teacher-login.html') {
      assertContains(relativePath, content, 'teacher-shell-body', 'teacher shell page should use the teacher shell theme class');
    }
  }

  if (relativePath.startsWith('student-') && relativePath.endsWith('.html')) {
    assertContains(relativePath, content, 'student-theme', 'student page should use the student theme class');
  }
}

const styles = read('frontend/dist/styles.css');
[
  'body.teacher-shell-body .btn-primary',
  'body.student-theme .btn-primary',
  'body.teacher-shell-body .text-primary',
  'body.student-theme .text-primary',
  'body.teacher-shell-body .bg-primary',
  'body.student-theme .bg-primary',
  'body.teacher-shell-body .form-control:focus',
  'body.student-theme .form-control:focus',
  'body.teacher-shell-body .tab-item.active',
  'body.student-theme .tab-item.active',
].forEach((needle) => assertContains('frontend/dist/styles.css', styles, needle, `missing role override ${needle}`));

const userDropdown = read('frontend/dist/components/user-dropdown.html');
assertNoMatch(
  'frontend/dist/components/user-dropdown.html',
  userDropdown,
  /#667eea|#764ba2|#4c1d95|#7c3aed|#6366f1|#4f46e5|#8b5cf6|#3b82f6|rgba\(\s*102\s*,\s*126\s*,\s*234\s*,|rgba\(\s*118\s*,\s*75\s*,\s*162\s*,|rgba\(\s*76\s*,\s*29\s*,\s*149\s*,|rgba\(\s*124\s*,\s*58\s*,\s*237\s*,|rgba\(\s*99\s*,\s*102\s*,\s*241\s*,|rgba\(\s*79\s*,\s*70\s*,\s*229\s*,|rgba\(\s*139\s*,\s*92\s*,\s*246\s*,/i,
  'user dropdown should not contain previous purple or default-blue role styling'
);
[
  '.user-dropdown-container.teacher-theme .user-dropdown-avatar',
  '.user-dropdown-container.teacher-theme .user-dropdown-name',
  '.user-dropdown-container.student-theme .user-dropdown-avatar',
  '.user-dropdown-container.student-theme .user-dropdown-name',
  'this.applyRoleTheme(this.resolveRole());',
  'resolveRole(user)',
].forEach((needle) => assertContains('frontend/dist/components/user-dropdown.html', userDropdown, needle, `missing user dropdown role theme hook ${needle}`));

for (const fullPath of listFiles(distRoot)) {
  const relativePath = relativeFromDist(fullPath);
  if (skippedRoleThemeFiles.has(relativePath)) continue;
  const staticPath = path.join(staticRoot, relativePath);
  if (!fs.existsSync(staticPath)) continue;
  const distContent = fs.readFileSync(fullPath, 'utf8');
  const staticContent = fs.readFileSync(staticPath, 'utf8');
  if (distContent !== staticContent) {
    failures.push(`${relativePath}: differs between frontend/dist and Spring Boot static mirror`);
  }
}

if (failures.length) {
  console.error('All-page role theme verification failed:');
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log('All-page role theme verification passed.');
