const fs = require('fs');
const path = require('path');

const filePath = path.join(
  __dirname,
  '..',
  'frontend',
  'dist',
  'student-assignments.html'
);

const content = fs.readFileSync(filePath, 'utf8');

const checks = [
  {
    description: 'fetchExams should derive submitted status when exam.submission exists',
    test: /if\s*\(\s*exam\.submission\s*\)\s*\{[\s\S]*status\s*=\s*exam\.submission\.graded\s*\?\s*['"]graded['"]\s*:\s*['"]submitted['"]/m
  },
  {
    description: 'renderExams should show submitted status label',
    test: /exam\.status === ['"]submitted['"]\s*\?\s*['"][^'"]*已提交[^'"]*['"]/m
  },
  {
    description: 'fetchExams should mark unsubmitted expired exams as ended',
    test: /else\s+if\s*\(\s*endTime\s*<\s*now\s*\)\s*\{\s*status\s*=\s*['"]ended['"]/m
  },
  {
    description: 'syncExamDetailToCache should keep unsubmitted expired exams ended',
    test: /else\s+if\s*\(\s*endTime\s*&&\s*endTime\s*<\s*now\s*\)\s*\{\s*status\s*=\s*['"]ended['"]/m
  },
  {
    description: 'renderExams should show ended status label',
    test: /exam\.status === ['"]ended['"]\s*\?\s*['"][^'"]*已结束[^'"]*['"]/m
  },
  {
    description: 'exam filters should include ended exams',
    test: /filterExams\(['"]ended['"]\)[^<]*>已结束</m
  },
  {
    description: 'renderExams should avoid startExam button for submitted exams',
    test: /if\s*\(\s*exam\.status === ['"]upcoming['"] \|\| exam\.status === ['"]ongoing['"]\s*\)/m
  },
  {
    description: 'exam detail should parse structured submission content before rendering',
    test: /function\s+parseStructuredSubmissionContent\s*\(/m
  },
  {
    description: 'showSubmitExamModal should prefill existing submission content',
    test: /showSubmitExamModal[\s\S]*parseStructuredSubmissionContent\(exam\.submission\.content\)/m
  },
  {
    description: 'viewExam should sync fetched submission detail back into the cached exams collection',
    test: /function\s+syncExamDetailToCache\s*\(/m
  },
  {
    description: 'viewExam should update exam cache before rendering modal actions',
    test: /syncExamDetailToCache\(exam\)/m
  },
  {
    description: 'exam detail should render structured submission html blocks',
    test: /renderStructuredSubmissionHtml\s*\(/m
  }
];

const failed = checks.filter(item => !item.test.test(content));

if (failed.length > 0) {
  console.error('student exam status contract failed:');
  failed.forEach(item => console.error(`- ${item.description}`));
  process.exit(1);
}

console.log('student exam status contract passed');
