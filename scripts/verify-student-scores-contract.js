const fs = require('fs');
const path = require('path');

const pagePath = path.join(__dirname, '..', 'frontend', 'dist', 'student-assignments.html');
const content = fs.readFileSync(pagePath, 'utf8');

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

assert(
  content.includes('<th>项目名称</th>'),
  '成绩表第二列表头应改为“项目名称”，避免把作业成绩误标成考试名称。'
);

assert(
  content.includes('<th>完成时间</th>'),
  '成绩表最后一列表头应改为“完成时间”，与实际展示字段保持一致。'
);

assert(
  content.includes('submitDate || score.examDate || score.submissionDate || score.completedAt'),
  '成绩时间应优先使用 submitDate，再回退到 examDate/submissionDate/completedAt。'
);

assert(
  content.includes('type: score.type ||') && content.includes('typeLabel:'),
  '成绩映射应保留 type/typeLabel，方便前端区分作业和考试成绩。'
);

assert(
  content.includes('score.score ?? null') && content.includes('score.rank ?? null'),
  '成绩和排名缺值时应保留 null，由渲染层显示“-”，不能直接回退成 0。'
);

assert(
  content.includes('.sort((left, right) => right.timestamp - left.timestamp)'),
  '成绩列表应按最新完成时间倒序展示。'
);

assert(
  content.includes('formatScoreTypeLabel(score.type, score.typeLabel)'),
  '成绩列表渲染时应显示作业/考试类型标识。'
);

console.log('student scores contract passed');
