const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('../major_assignment/node_modules/playwright');

const repoRoot = path.resolve(__dirname, '..');
const baseUrl = process.env.FRONTEND_BASE_URL || 'http://127.0.0.1:5500';
const teacherSessionPath = path.join(repoRoot, '.runtime-logs', 'teacher-session-teacher7-student42-data.json');
const studentSessionPath = path.join(repoRoot, '.runtime-logs', 'student-session-teacher7-student42-data.json');

const pageChecks = [
  { page: 'student-dashboard.html', role: 'student', waitMs: 5500 },
  { page: 'student-courses.html', role: 'student', waitMs: 4500 },
  { page: 'student-assignments.html', role: 'student', waitMs: 4500 },
  { page: 'student-stats.html', role: 'student', waitMs: 6000 },
  { page: 'student-notifications.html', role: 'student', waitMs: 4500 },
  { page: 'student-ai-assistant.html', role: 'student', waitMs: 4500, allowEmptySelectors: ['[data-student-ai-empty-state]'] },
  { page: 'teacher-dashboard.html', role: 'teacher', waitMs: 6500 },
  { page: 'teacher-courses.html', role: 'teacher', waitMs: 5000 },
  { page: 'teacher-assignments.html', role: 'teacher', waitMs: 5500 },
  { page: 'teacher-knowledge.html', role: 'teacher', waitMs: 6500 },
  { page: 'teacher-warning.html', role: 'teacher', waitMs: 6500 },
  { page: 'teacher-student-dashboard.html', role: 'teacher', waitMs: 7000 },
  { page: 'teacher-notifications.html', role: 'teacher', waitMs: 5000 },
  { page: 'teacher-settings.html', role: 'teacher', waitMs: 3500 },
  { page: 'teacher-ai-tools.html', role: 'teacher', waitMs: 4500, allowEmptySelectors: ['[data-teacher-ai-empty-state]'] }
];

const emptyTextPatterns = [
  /暂无(课程|作业|考试|成绩|通知|预警|知识点|学生|班级|活动|图表|学习计划|历史记录|已发送通知记录|提交记录|统计数据|知识点明细|可计算的学习进度|成绩趋势)/
];

function readSession(filePath) {
  const payload = JSON.parse(fs.readFileSync(filePath, 'utf8').replace(/^\uFEFF/, ''));
  return payload.sessionStorage || {};
}

function sessionForRole(role) {
  return role === 'teacher'
    ? readSession(teacherSessionPath)
    : readSession(studentSessionPath);
}

function responseSummary(response) {
  return `${response.status()} ${response.request().method()} ${response.url()}`;
}

async function installSession(page, role) {
  await page.addInitScript((state) => {
    for (const [key, value] of Object.entries(state)) {
      if (value !== undefined && value !== null) {
        window.sessionStorage.setItem(key, String(value));
      }
    }
  }, sessionForRole(role));
}

async function inspectPage(browser, config) {
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } });
  const failedApiResponses = [];
  const consoleErrors = [];

  page.on('response', (response) => {
    const url = response.url();
    if (url.includes('/api/') && response.status() >= 400) {
      failedApiResponses.push(responseSummary(response));
    }
  });
  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push(message.text());
    }
  });
  page.on('pageerror', (error) => {
    consoleErrors.push(error.message);
  });

  await installSession(page, config.role);
  await page.goto(`${baseUrl}/${config.page}`, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(config.waitMs || 4500);

  const state = await page.evaluate(({ emptyPatternSources, allowEmptySelectors }) => {
    const visible = (element) => {
      if (!element) return false;
      const style = window.getComputedStyle(element);
      const rect = element.getBoundingClientRect();
      return style.visibility !== 'hidden'
        && style.display !== 'none'
        && rect.width > 0
        && rect.height > 0;
    };

    const allowed = (element) => {
      return allowEmptySelectors.some((selector) => element.closest(selector));
    };

    const emptyRegexes = emptyPatternSources.map((source) => new RegExp(source));
    const emptyTexts = [...document.body.querySelectorAll('body *')]
      .filter(visible)
      .filter((element) => !allowed(element))
      .map((element) => (element.innerText || element.textContent || '').trim().replace(/\s+/g, ' '))
      .filter((text) => text.length > 0 && text.length < 120)
      .filter((text, index, texts) => texts.indexOf(text) === index)
      .filter((text) => emptyRegexes.some((regex) => regex.test(text)));

    const echartsStates = [];
    if (window.echarts && typeof window.echarts.getInstanceByDom === 'function') {
      document.querySelectorAll('div, canvas').forEach((element) => {
        const chart = window.echarts.getInstanceByDom(element);
        if (!chart || typeof chart.getOption !== 'function') return;
        const option = chart.getOption();
        const seriesValues = (option.series || []).flatMap((series) => {
          return (series.data || []).map((value) => {
            if (Array.isArray(value)) return Number(value[value.length - 1]);
            if (value && typeof value === 'object') return Number(value.value);
            return Number(value);
          }).filter(Number.isFinite);
        });
        echartsStates.push({
          id: element.id || element.className || 'echarts',
          count: seriesValues.length,
          hasNonZero: seriesValues.some((value) => value !== 0)
        });
      });
    }

    const chartJsStates = [];
    if (window.Chart && window.Chart.instances) {
      Object.values(window.Chart.instances).forEach((chart) => {
        const values = (chart.data?.datasets || []).flatMap((dataset) =>
          (dataset.data || []).map(Number).filter(Number.isFinite)
        );
        chartJsStates.push({
          id: chart.canvas?.id || 'chartjs',
          count: values.length,
          hasNonZero: values.some((value) => value !== 0)
        });
      });
    }

    return {
      path: window.location.pathname,
      title: document.title,
      emptyTexts,
      echartsStates,
      chartJsStates
    };
  }, {
    emptyPatternSources: emptyTextPatterns.map((regex) => regex.source),
    allowEmptySelectors: config.allowEmptySelectors || []
  });

  await page.close();
  return {
    page: config.page,
    ...state,
    failedApiResponses,
    consoleErrors
  };
}

(async () => {
  const browser = await chromium.launch({ headless: true });
  const failures = [];

  try {
    for (const config of pageChecks) {
      const result = await inspectPage(browser, config);
      const loginPage = config.role === 'teacher' ? 'teacher-login.html' : 'student-login.html';
      const nonZeroChartFailures = [...result.echartsStates, ...result.chartJsStates]
        .filter((chart) => chart.count > 0 && !chart.hasNonZero);

      if (result.path.endsWith(loginPage)) {
        failures.push(`${config.page}: redirected to ${loginPage}`);
      }
      if (result.failedApiResponses.length) {
        failures.push(`${config.page}: API failures: ${result.failedApiResponses.join(' | ')}`);
      }
      const actionableConsoleErrors = result.consoleErrors.filter((message) =>
        !message.includes('favicon') && !message.includes('net::ERR_ABORTED')
      );
      if (actionableConsoleErrors.length) {
        failures.push(`${config.page}: console errors: ${actionableConsoleErrors.slice(0, 3).join(' | ')}`);
      }
      if (result.emptyTexts.length) {
        failures.push(`${config.page}: visible empty states: ${result.emptyTexts.slice(0, 5).join(' | ')}`);
      }
      if (nonZeroChartFailures.length) {
        failures.push(`${config.page}: charts have only zero values: ${nonZeroChartFailures.map((chart) => chart.id).join(', ')}`);
      }

      console.log(`[${config.page}] empty=${result.emptyTexts.length} apiFailures=${result.failedApiResponses.length} charts=${result.echartsStates.length + result.chartJsStates.length}`);
    }
  } finally {
    await browser.close();
  }

  if (failures.length) {
    console.error('teacher7/student42 page data verification failed:');
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
  }

  console.log(`teacher7/student42 page data verification passed for ${pageChecks.length} pages.`);
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
