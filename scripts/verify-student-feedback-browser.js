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

async function seedStudentSession(page) {
  await page.addInitScript(() => {
    sessionStorage.setItem('token', 'student-feedback-token');
    sessionStorage.setItem('user', JSON.stringify({
      id: 2,
      username: 'student-feedback',
      role: 'student',
      roles: ['STUDENT']
    }));
  });
}

(async () => {
  const executablePath = resolveBundledChromium();
  const browser = await chromium.launch(executablePath ? { headless: true, executablePath } : { headless: true });

  try {
    const page = await browser.newPage({ viewport: { width: 1366, height: 900 } });
    await seedStudentSession(page);
    await page.goto(`${baseUrl}/student-courses.html`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1400);

    const courseFeedback = await page.evaluate(() => {
      const state = document.querySelector('#coursesGrid[data-feedback-state]');
      return {
        exists: Boolean(state),
        state: state?.getAttribute('data-feedback-state') || null,
        title: state?.querySelector('.feedback-state-title')?.textContent?.trim() || null
      };
    });
    assert(courseFeedback.exists, 'student courses page should render unified feedback state when API data is unavailable', courseFeedback);
    assert(['empty', 'error'].includes(courseFeedback.state), 'student courses feedback state should be empty or error', courseFeedback);

    await page.goto(`${baseUrl}/student-assignments.html`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1400);

    const assignmentFeedback = await page.evaluate(() => {
      const assignmentState = document.querySelector('#assignment-list[data-feedback-state]');
      return {
        exists: Boolean(assignmentState),
        state: assignmentState?.getAttribute('data-feedback-state') || null,
        title: assignmentState?.querySelector('.feedback-state-title')?.textContent?.trim() || null
      };
    });
    assert(assignmentFeedback.exists, 'student assignments page should render unified assignment feedback state', assignmentFeedback);
    assert(['empty', 'error'].includes(assignmentFeedback.state), 'student assignments feedback state should be empty or error', assignmentFeedback);

    console.log('Student feedback verification passed.');
  } finally {
    await browser.close();
  }
})().catch((error) => {
  console.error('Student feedback verification failed:');
  console.error(`- ${error.message}`);
  process.exit(1);
});
