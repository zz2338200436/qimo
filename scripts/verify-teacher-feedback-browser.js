const fs = require('fs');
const path = require('path');
const { chromium } = require('../major_assignment/package-lock.json')
  ? require('../major_assignment/node_modules/playwright')
  : require('../major_assignment/node_modules/playwright');

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

async function seedTeacherSession(page) {
  await page.addInitScript(() => {
    const teacherUser = {
      id: 3,
      username: 'teacher-feedback',
      role: 'TEACHER',
      activeRole: 'TEACHER',
      roles: ['TEACHER']
    };

    sessionStorage.setItem('token', 'teacher-feedback-token');
    sessionStorage.setItem('user', JSON.stringify(teacherUser));
    sessionStorage.setItem('role', 'TEACHER');
    sessionStorage.setItem('activeRole', 'TEACHER');

    localStorage.setItem('token', 'teacher-feedback-token');
    localStorage.setItem('user', JSON.stringify(teacherUser));
    localStorage.setItem('role', 'TEACHER');
    localStorage.setItem('activeRole', 'TEACHER');
  });
}

(async () => {
  const executablePath = resolveBundledChromium();
  const browser = await chromium.launch(executablePath ? { headless: true, executablePath } : { headless: true });

  try {
    const page = await browser.newPage({ viewport: { width: 1366, height: 900 } });
    await seedTeacherSession(page);

    await page.goto(`${baseUrl}/teacher-warning.html`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1800);

    const warningFeedback = await page.evaluate(() => {
      const state = document.querySelector('.warning-list[data-feedback-state]');
      return {
        exists: Boolean(state),
        state: state?.getAttribute('data-feedback-state') || null,
        title: state?.querySelector('.feedback-state-title')?.textContent?.trim() || null
      };
    });
    assert(warningFeedback.exists, 'teacher warning page should render unified list feedback state', warningFeedback);
    assert(['empty', 'error'].includes(warningFeedback.state), 'teacher warning feedback state should be empty or error', warningFeedback);

    await page.goto(`${baseUrl}/teacher-knowledge.html`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1800);

    const knowledgeFeedback = await page.evaluate(() => {
      const state = document.querySelector('.knowledge-list[data-feedback-state]');
      return {
        exists: Boolean(state),
        state: state?.getAttribute('data-feedback-state') || null,
        title: state?.querySelector('.feedback-state-title')?.textContent?.trim() || null
      };
    });
    assert(knowledgeFeedback.exists, 'teacher knowledge page should render unified knowledge feedback state', knowledgeFeedback);
    assert(['empty', 'error'].includes(knowledgeFeedback.state), 'teacher knowledge feedback state should be empty or error', knowledgeFeedback);

    await page.goto(`${baseUrl}/teacher-student-dashboard.html`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(2200);

    const dashboardFeedback = await page.evaluate(() => {
      const feedbackCell = document.querySelector('#studentTableBody .feedback-table-cell');
      return {
        exists: Boolean(feedbackCell),
        state: feedbackCell?.getAttribute('data-feedback-state') || null,
        text: feedbackCell?.textContent?.trim() || null
      };
    });
    assert(dashboardFeedback.exists, 'teacher student dashboard should render unified table feedback cell', dashboardFeedback);
    assert(['empty', 'error', 'loading'].includes(dashboardFeedback.state), 'teacher student dashboard feedback state should be loading, empty, or error', dashboardFeedback);

    console.log('Teacher feedback verification passed.');
  } finally {
    await browser.close();
  }
})().catch((error) => {
  console.error('Teacher feedback verification failed:');
  console.error(`- ${error.message}`);
  process.exit(1);
});
