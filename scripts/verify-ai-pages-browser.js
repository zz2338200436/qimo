const fs = require('fs');
const path = require('path');

function loadPlaywright() {
  const candidates = [
    '../major_assignment/node_modules/playwright',
    '../../major_assignment/node_modules/playwright',
    '../frontend/node_modules/playwright'
  ];

  for (const candidate of candidates) {
    try {
      return require(candidate);
    } catch (error) {
      if (error.code !== 'MODULE_NOT_FOUND') {
        throw error;
      }
    }
  }

  throw new Error('Cannot find Playwright installation for browser verification scripts.');
}

const { chromium } = loadPlaywright();

const baseUrl = process.env.FRONTEND_BASE_URL || 'http://127.0.0.1:5500';

function resolveBundledChromium() {
  const playwrightHome = path.join(process.env.LOCALAPPDATA || '', 'ms-playwright');
  if (!playwrightHome || !fs.existsSync(playwrightHome)) {
    return null;
  }

  const revisions = fs.readdirSync(playwrightHome)
    .filter((entry) => entry.startsWith('chromium_headless_shell-'))
    .sort()
    .reverse();

  for (const revision of revisions) {
    const candidate = path.join(playwrightHome, revision, 'chrome-headless-shell-win64', 'chrome-headless-shell.exe');
    if (fs.existsSync(candidate)) {
      return candidate;
    }
  }

  return null;
}

function assert(condition, message, details) {
  if (!condition) {
    const suffix = details ? ` ${JSON.stringify(details)}` : '';
    throw new Error(`${message}${suffix}`);
  }
}

async function seedStudentSession(page) {
  await page.addInitScript(() => {
    const studentUser = {
      id: 2,
      username: 'student-ai',
      role: 'student',
      activeRole: 'STUDENT',
      roles: ['STUDENT']
    };

    sessionStorage.setItem('token', 'student-ai-token');
    sessionStorage.setItem('user', JSON.stringify(studentUser));
    sessionStorage.setItem('role', 'STUDENT');
    sessionStorage.setItem('activeRole', 'STUDENT');
    localStorage.setItem('token', 'student-ai-token');
    localStorage.setItem('user', JSON.stringify(studentUser));
  });
}

async function seedTeacherSession(page) {
  await page.addInitScript(() => {
    const teacherUser = {
      id: 3,
      username: 'teacher-ai',
      role: 'TEACHER',
      activeRole: 'TEACHER',
      roles: ['TEACHER']
    };

    sessionStorage.setItem('token', 'teacher-ai-token');
    sessionStorage.setItem('user', JSON.stringify(teacherUser));
    sessionStorage.setItem('role', 'TEACHER');
    sessionStorage.setItem('activeRole', 'TEACHER');
    localStorage.setItem('token', 'teacher-ai-token');
    localStorage.setItem('user', JSON.stringify(teacherUser));
  });
}

(async () => {
  const executablePath = resolveBundledChromium();
  const browser = await chromium.launch(executablePath ? { headless: true, executablePath } : { headless: true });

  try {
    const studentPage = await browser.newPage({ viewport: { width: 1366, height: 900 } });
    await seedStudentSession(studentPage);
    await studentPage.goto(`${baseUrl}/student-ai-assistant.html`, { waitUntil: 'domcontentloaded' });
    await studentPage.waitForFunction(() => {
      return document.title !== 'Error response'
        && (!!document.querySelector('#sidebar-container .sidebar') || !!document.querySelector('[data-student-ai-empty-state]'));
    }, { timeout: 5000 });
    await studentPage.waitForTimeout(800);

    const studentState = await studentPage.evaluate(() => ({
      sidebarExists: Boolean(document.querySelector('#sidebar-container .sidebar')),
      emptyStateExists: Boolean(document.querySelector('[data-student-ai-empty-state]')),
      emptyStateTitle: document.querySelector('[data-student-ai-empty-state] h1')?.textContent?.trim() || null,
      historyPanelExists: Boolean(document.querySelector('[data-agent-history-panel]'))
    }));
    assert(studentState.sidebarExists, 'student ai page should load unified sidebar shell', studentState);
    assert(studentState.emptyStateExists, 'student ai page should render welcome empty state', studentState);
    assert(studentState.historyPanelExists, 'student ai page should render history panel container', studentState);

    const studentHistoryState = await studentPage.evaluate(() => {
      const panel = document.querySelector('[data-agent-history-panel]');
      const root = panel?.querySelector('[data-agent-history-list] [data-agent-history-state], [data-agent-history-detail] [data-agent-history-state]');
      return {
        exists: Boolean(root),
        state: root?.getAttribute('data-agent-history-state') || null
      };
    });
    assert(studentHistoryState.exists, 'student ai page history panel should render a unified idle state block on initial load', studentHistoryState);
    assert(studentHistoryState.state === 'empty', 'student ai page history panel should stay idle before opening drawer', studentHistoryState);

    await studentPage.click('[data-agent-history-toggle]');
    await studentPage.waitForTimeout(400);

    const studentHistoryAfterOpen = await studentPage.evaluate(() => {
      const root = document.querySelector('[data-agent-history-list] [data-agent-history-state]');
      return {
        exists: Boolean(root),
        state: root?.getAttribute('data-agent-history-state') || null
      };
    });
    assert(studentHistoryAfterOpen.exists, 'student ai page history list should render a unified state block after opening drawer', studentHistoryAfterOpen);

    const teacherPage = await browser.newPage({ viewport: { width: 1366, height: 900 } });
    await seedTeacherSession(teacherPage);
    await teacherPage.goto(`${baseUrl}/teacher-ai-tools.html`, { waitUntil: 'domcontentloaded' });
    await teacherPage.waitForFunction(() => {
      return document.title !== 'Error response'
        && (!!document.querySelector('#sidebar-container .sidebar') || !!document.querySelector('[data-teacher-ai-empty-state]'));
    }, { timeout: 5000 });
    await teacherPage.waitForTimeout(800);

    const teacherState = await teacherPage.evaluate(() => ({
      sidebarExists: Boolean(document.querySelector('#sidebar-container .sidebar')),
      emptyStateExists: Boolean(document.querySelector('[data-teacher-ai-empty-state]')),
      emptyStateText: document.querySelector('[data-teacher-ai-empty-state] strong')?.textContent?.trim() || null,
      historyPanelExists: Boolean(document.querySelector('[data-agent-history-panel]'))
    }));
    assert(teacherState.sidebarExists, 'teacher ai page should load unified sidebar shell', teacherState);
    assert(teacherState.emptyStateExists, 'teacher ai page should render teacher welcome empty state', teacherState);
    assert(teacherState.historyPanelExists, 'teacher ai page should render history panel container', teacherState);

    const teacherResultEmptyState = await teacherPage.evaluate(() => {
      if (typeof clearResults === 'function') {
        clearResults();
      }

      const resultHost = document.querySelector('#result-content[data-feedback-state]');
      return {
        exists: Boolean(resultHost),
        state: resultHost?.getAttribute('data-feedback-state') || null,
        title: resultHost?.querySelector('.feedback-state-title')?.textContent?.trim() || null
      };
    });
    assert(teacherResultEmptyState.exists, 'teacher ai page should render unified result empty state after clearing results', teacherResultEmptyState);
    assert(teacherResultEmptyState.state === 'empty', 'teacher ai result state should be empty after clearing results', teacherResultEmptyState);

    console.log('AI pages verification passed.');
  } finally {
    await browser.close();
  }
})().catch((error) => {
  console.error('AI pages verification failed:');
  console.error(`- ${error.message}`);
  process.exit(1);
});
