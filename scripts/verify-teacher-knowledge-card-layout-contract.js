const fs = require('fs');

function assertIncludes(content, snippet, message) {
  if (!content.includes(snippet)) {
    throw new Error(`${message} Missing: ${snippet}`);
  }
}

for (const file of [
  'major_assignment/src/main/resources/static/teacher-knowledge.html',
  'frontend/dist/teacher-knowledge.html'
]) {
  const content = fs.readFileSync(file, 'utf8');
  assertIncludes(content, '.knowledge-card-body {', `${file} should define a stable card layout.`);
  assertIncludes(content, 'grid-template-columns: minmax(220px, 1.1fr) minmax(200px, 1fr) minmax(180px, 0.8fr) 176px;', `${file} should reserve a fixed action column.`);
  assertIncludes(content, '.knowledge-card-actions {', `${file} should define a dedicated action region.`);
  assertIncludes(content, 'grid-template-columns: repeat(3, minmax(112px, 1fr));', `${file} should keep actions aligned on narrower desktop screens.`);
  assertIncludes(content, '<div class="knowledge-card-body">', `${file} should render the knowledge card body wrapper.`);
  assertIncludes(content, '<div class="knowledge-card-actions">', `${file} should render action buttons in their own wrapper.`);
}

console.log('teacher knowledge card layout contract OK');
