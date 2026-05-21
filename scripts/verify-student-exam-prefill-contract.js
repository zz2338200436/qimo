const fs = require('fs');
const path = require('path');
const vm = require('vm');

const filePath = path.join(
  __dirname,
  '..',
  'frontend',
  'dist',
  'student-assignments.html'
);

const html = fs.readFileSync(filePath, 'utf8');

function extractFunction(source, name) {
  const signatures = [`async function ${name}`, `function ${name}`];
  let start = -1;

  for (const signature of signatures) {
    start = source.indexOf(signature);
    if (start !== -1) {
      break;
    }
  }

  if (start === -1) {
    throw new Error(`Unable to find function ${name}`);
  }

  const bodyStart = source.indexOf('{', start);
  if (bodyStart === -1) {
    throw new Error(`Unable to find body for function ${name}`);
  }

  let depth = 0;
  for (let index = bodyStart; index < source.length; index += 1) {
    const char = source[index];
    if (char === '{') {
      depth += 1;
    } else if (char === '}') {
      depth -= 1;
      if (depth === 0) {
        return source.slice(start, index + 1);
      }
    }
  }

  throw new Error(`Unable to parse function ${name}`);
}

const scriptSource = `
var exams = [
  {
    id: 101,
    title: '期末考试',
    description: '旧列表数据',
    status: 'completed',
    submission: null
  }
];

const examDetailResponse = {
  success: true,
  data: {
    id: 101,
    title: '期末考试',
    courseName: '分布式框架技术',
    description: '考试详情',
    startTime: '2026-05-01T08:00:00Z',
    endTime: '2026-05-01T10:00:00Z',
    duration: 120,
    submission: {
      content: JSON.stringify({ content: '上次提交正文', appendix: '附件信息' }),
      submissionDate: '2026-05-01T09:00:00Z',
      graded: false,
      score: null,
      teacherComment: null
    }
  }
};

const elements = {
  examId: { value: '' },
  examContent: { value: '' },
  examFiles: { value: '', files: [] },
  filePreview: { innerHTML: '' },
  examModalBody: { innerHTML: '' },
  examModal: {},
  submitExamModal: {}
};

const document = {
  getElementById(id) {
    if (!elements[id]) {
      elements[id] = {};
    }
    return elements[id];
  }
};

class Modal {
  constructor(element) {
    this.element = element;
    element.__modalInstance = this;
  }

  show() {
    this.element.__shown = true;
  }

  hide() {
    this.element.__hidden = true;
  }

  static getInstance(element) {
    return element.__modalInstance || null;
  }
}

const bootstrap = { Modal };

class APIService {}

class StudentAPI {
  async getExamDetail(examId) {
    if (String(examId) !== '101') {
      throw new Error('unexpected examId');
    }
    return examDetailResponse;
  }
}

function renderStructuredSubmissionHtml(parsedSubmission) {
  return parsedSubmission ? parsedSubmission.displayText : '';
}

function showMessage(message) {
  throw new Error(message);
}

${extractFunction(html, 'parseStructuredSubmissionContent')}
${extractFunction(html, 'syncExamDetailToCache')}
${extractFunction(html, 'viewExam')}
${extractFunction(html, 'showSubmitExamModal')}

(async () => {
  await viewExam(101);
  showSubmitExamModal(101);

  const actual = document.getElementById('examContent').value;
  if (actual !== '上次提交正文') {
    throw new Error(\`Expected textarea to prefill "上次提交正文", got "\${actual}"\`);
  }

  console.log('student exam prefill contract passed');
})().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
`;

vm.runInNewContext(scriptSource, {
  console,
  JSON,
  Date,
  process
});
