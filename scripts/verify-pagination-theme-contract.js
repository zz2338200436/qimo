const fs = require('node:fs');
const path = require('node:path');

const root = process.cwd();
const distRoot = path.join(root, 'frontend', 'dist');
const staticRoot = path.join(root, 'major_assignment', 'src', 'main', 'resources', 'static');

const failures = [];
const PAGINATION_STYLE_VERSION = '20260608-pagination-1';
const htmlFilesWithPagination = [
  'teacher-assignments.html',
  'teacher-courses.html',
  'teacher-knowledge.html',
  'teacher-student-dashboard.html',
  'teacher-warning.html',
];

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

function assertContains(label, content, needle, message) {
  if (!content.includes(needle)) {
    failures.push(`${label}: ${message}`);
  }
}

function assertNoMatch(label, content, regex, message) {
  const match = content.match(regex);
  if (match) {
    failures.push(`${label}: ${message} (${match[0].trim()})`);
  }
}

for (const baseDir of ['frontend/dist', 'major_assignment/src/main/resources/static']) {
  const styles = read(`${baseDir}/styles.css`);
  [
    '.pagination {',
    'flex-wrap: wrap;',
    '.page-link.active',
    '.page-item.active .page-link',
    '.page-link:focus-visible',
    '.page-item.disabled .page-link',
    'body.teacher-shell-body .page-link.active',
    'body.teacher-theme .page-link.active',
    'body.student-theme .page-link.active',
    'body.teacher-shell-body .pagination-btn.active',
    'body.student-theme .pagination-btn.active',
    'body.teacher-shell-body .page-item.disabled .page-link',
    'body.student-theme .page-item.disabled .page-link',
    'body.teacher-shell-body .pagination-btn:disabled',
    'body.student-theme .pagination-btn:disabled',
    'body.teacher-shell-body .page-link:focus-visible',
    'body.student-theme .page-link:focus-visible',
  ].forEach((needle) => {
    assertContains(`${baseDir}/styles.css`, styles, needle, `missing unified pagination rule ${needle}`);
  });

  assertNoMatch(
    `${baseDir}/styles.css`,
    styles,
    /\.page-link\s*\{\s*background:\s*white;\s*color:\s*var\(--secondary-color\);\s*border:\s*1px\s+solid\s+var\(--border-color\);\s*border-radius:\s*var\(--border-radius\);\s*padding:\s*8px\s+16px;/s,
    'still contains the old compact Bootstrap pagination block'
  );
}

for (const fileName of htmlFilesWithPagination) {
  for (const baseDir of ['frontend/dist', 'major_assignment/src/main/resources/static']) {
    const relativePath = `${baseDir}/${fileName}`;
    const content = read(relativePath);

    assertNoMatch(
      relativePath,
      content,
      /styles\.css\?v=20260608-theme-4/,
      'pagination page should not reference the stale stylesheet cache version'
    );
    assertContains(
      relativePath,
      content,
      `styles.css?v=${PAGINATION_STYLE_VERSION}`,
      `pagination page should reference stylesheet cache version ${PAGINATION_STYLE_VERSION}`
    );
    assertNoMatch(
      relativePath,
      content,
      /\/\*\s*分页样式\s*\*\/\s*\.pagination\s*\{[\s\S]*?\.page-(?:link|item)(?:\.active|\s*\.active)?[\s\S]*?\}/,
      'page should not keep a page-local legacy pagination style block'
    );
    assertNoMatch(
      relativePath,
      content,
      /\.page-link\.active\s*\{[\s\S]*?linear-gradient\(135deg,\s*#2563eb\s*0%,\s*#1d4ed8\s*100%\)/,
      'page should not keep a hard-coded teacher pagination active gradient'
    );
  }
}

for (const fileName of ['styles.css', ...htmlFilesWithPagination]) {
  const distContent = fs.readFileSync(path.join(distRoot, fileName), 'utf8');
  const staticContent = fs.readFileSync(path.join(staticRoot, fileName), 'utf8');
  if (distContent !== staticContent) {
    failures.push(`${fileName}: differs between frontend/dist and Spring Boot static mirror`);
  }
}

if (failures.length) {
  console.error('Pagination theme verification failed:');
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log('Pagination theme verification passed.');
