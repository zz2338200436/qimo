const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

function assertMirrorFileMatches(frontendPath, staticPath, label) {
  const frontendContent = fs.readFileSync(frontendPath, 'utf8');
  const staticContent = fs.readFileSync(staticPath, 'utf8');

  if (frontendContent !== staticContent) {
    throw new Error(
      `${label} mirror mismatch. Keep frontend/dist and major_assignment/src/main/resources/static in sync.\n` +
      `frontend: ${frontendPath}\nstatic: ${staticPath}`
    );
  }
}

[
  [
    'frontend/dist/student-assignments.html',
    'major_assignment/src/main/resources/static/student-assignments.html',
    'student-assignments.html'
  ],
  [
    'frontend/dist/teacher-assignments-assignment-crud.js',
    'major_assignment/src/main/resources/static/teacher-assignments-assignment-crud.js',
    'teacher-assignments-assignment-crud.js'
  ]
].forEach(([frontendPath, staticPath, label]) => {
  assertMirrorFileMatches(frontendPath, staticPath, label);
});

const studentAssignments = fs.readFileSync('frontend/dist/student-assignments.html', 'utf8');
const teacherAssignmentCrud = fs.readFileSync('frontend/dist/teacher-assignments-assignment-crud.js', 'utf8');

[
  'function renderAssignmentDescriptionHtml(value) {',
  "class=\"assignment-description-text border rounded p-3 bg-light\"",
  'style="white-space: pre-wrap;"',
  '${renderAssignmentDescriptionHtml(assignment.description)}'
].forEach(snippet => {
  assertIncludes(
    studentAssignments,
    snippet,
    'student assignment detail should preserve generated-question description line breaks.'
  );
});

[
  'function escapeHtml(value) {',
  'function renderAssignmentDescriptionHtml(value) {',
  "class=\"assignment-description-text border rounded p-3 bg-light\"",
  'style="white-space: pre-wrap;"',
  '${renderAssignmentDescriptionHtml(assignment.description)}'
].forEach(snippet => {
  assertIncludes(
    teacherAssignmentCrud,
    snippet,
    'teacher assignment detail should preserve generated-question description line breaks.'
  );
});

console.log('assignment description format contract OK');
