const fs = require('node:fs');

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

function assertNotIncludes(content, needle, message) {
  if (content.includes(needle)) {
    throw new Error(`${message} Found unexpected snippet: ${needle}`);
  }
}

const pageContent = fs.readFileSync('frontend/dist/student-stats.html', 'utf8');

[
  'id="studyTimeMeta"',
  'id="completedTasksMeta"',
  'id="averageScoreMeta"',
  'id="knowledgeMasteryMeta"',
  'buildEmptyStatsPayload()',
  'buildEmptyScoreChartData(timeRange = \'month\')',
  'buildEmptyStudyTimeChartData()',
  'applyJwtStatsOnlyFallback(filters = {})',
  'await fetchKnowledgeData(filters);',
  '使用默认值（0）',
  '当前筛选学习时长',
  '当前筛选完成任务数',
  '当前筛选平均成绩',
  '当前筛选知识点掌握'
].forEach(snippet => {
  assertIncludes(
    pageContent,
    snippet,
    'student stats contract mismatch.'
  );
});

[
  'id="studyTimeChange"',
  'id="completedTasksChange"',
  'id="averageScoreChange"',
  'id="knowledgeMasteryChange"',
  'stats.studyTimeChange',
  'stats.completedTasksChange',
  'stats.averageScoreChange',
  'stats.knowledgeMasteryChange',
  'studyTimeChange: 0',
  'completedTasksChange: 0',
  'averageScoreChange: 0.0',
  'knowledgeMasteryChange: 0.0',
  "const studyTimeChange = document.getElementById('studyTimeChange');",
  "const completedTasksChange = document.getElementById('completedTasksChange');",
  "const averageScoreChange = document.getElementById('averageScoreChange');",
  "const knowledgeMasteryChange = document.getElementById('knowledgeMasteryChange');",
  '不使用模拟数据',
  '使用默认值而不是模拟数据',
  '比上周',
  '与上周持平',
  '当前 JWT 微服务环境暂未接通知识点统计接口。'
].forEach(snippet => {
  assertNotIncludes(
    pageContent,
    snippet,
    'student stats should not render placeholder change fields as weekly deltas.'
  );
});

console.log('student stats contract OK');
