const fs = require('node:fs');
const path = require('node:path');

const repoRoot = path.resolve(__dirname, '..');
const seedPath = path.join(repoRoot, 'scripts', 'seed-rich-learning-demo-data.sql');
const content = fs.readFileSync(seedPath, 'utf8');

function assertIncludes(needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

assertIncludes('(@class_id, 42)', 'student42 must be enrolled in the rich-learning demo class so class-based assignment visibility works.');
assertIncludes('SELECT 9930101 assignment_id, 42 student_id', 'student42 must have rich-learning assignment submissions.');
assertIncludes('WHERE id BETWEEN 9940101 AND 9940505', 'student42 pending assignment seed range must remain class-visible.');
assertIncludes('demo-student42-recent-study-time', 'student42 must have recent analysis score-trend rows so the student dashboard study-time chart is not all zero.');
assertIncludes('9974201', 'student42 recent study-time seed ids must remain stable and rerunnable.');
assertIncludes('demo-student42-course-descriptions', 'student42 class-visible courses must keep non-empty descriptions for the course cards.');
assertIncludes('WHERE id = 91010', 'student42 distributed framework practicum course description must be backfilled.');

console.log('student42 assignment seed contract OK');
