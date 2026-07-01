const fs = require('fs');
const path = require('path');
const { chromium } = require('../major_assignment/node_modules/playwright');

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

async function seedSession(page, role) {
  await page.addInitScript((currentRole) => {
    const user = currentRole === 'teacher'
      ? { id: 1, username: 'teacher-shell', role: 'teacher', roles: ['TEACHER'] }
      : { id: 2, username: 'student-shell', role: 'student', roles: ['STUDENT'] };
    sessionStorage.setItem('token', `${currentRole}-token`);
    sessionStorage.setItem('user', JSON.stringify(user));
  }, role);
}

async function snapshotState(page) {
  await page.waitForTimeout(1200);
  return page.evaluate(() => {
    const sidebar = document.getElementById('sidebar');
    const mainContent = document.getElementById('mainContent');
    const overlay = document.getElementById('sidebarOverlay');
    return {
      sidebarExists: Boolean(sidebar),
      mainContentExists: Boolean(mainContent),
      overlayExists: Boolean(overlay),
      sidebarClasses: sidebar ? Array.from(sidebar.classList) : [],
      mainContentClasses: mainContent ? Array.from(mainContent.classList) : [],
      overlayHidden: overlay ? overlay.hidden : null,
      overlayActive: overlay ? overlay.classList.contains('active') : null,
      bodyClasses: Array.from(document.body.classList),
      scrollWidth: document.documentElement.scrollWidth,
      clientWidth: document.documentElement.clientWidth,
    };
  });
}

async function openScenario(browser, role, pageName, viewport) {
  const page = await browser.newPage({ viewport });
  await seedSession(page, role);
  await page.goto(`${baseUrl}/${pageName}`, { waitUntil: 'domcontentloaded' });
  return page;
}

async function clickToggle(page, toggleId) {
  await page.click(`#${toggleId}`);
  await page.waitForTimeout(250);
}

(async () => {
  const executablePath = resolveBundledChromium();
  const browser = await chromium.launch(executablePath ? { headless: true, executablePath } : { headless: true });

  try {
    const desktopStudent = await openScenario(browser, 'student', 'student-dashboard.html', { width: 1366, height: 900 });
    let state = await snapshotState(desktopStudent);
    assert(state.sidebarExists, 'student desktop sidebar should exist');
    assert(state.mainContentExists, 'student desktop mainContent should exist');
    assert(state.overlayExists, 'student desktop overlay should exist');
    assert(!state.sidebarClasses.includes('mobile-open'), 'student desktop should not start in mobile-open state', state);
    assert(!state.sidebarClasses.includes('desktop-collapsed'), 'student desktop should not start collapsed', state);

    await clickToggle(desktopStudent, 'toggleSidebar');
    state = await snapshotState(desktopStudent);
    assert(state.sidebarClasses.includes('desktop-collapsed'), 'student desktop toggle should apply desktop-collapsed', state);
    assert(state.mainContentClasses.includes('collapsed'), 'student desktop toggle should collapse main content', state);
    assert(!state.sidebarClasses.includes('mobile-open'), 'student desktop toggle should not use mobile-open', state);
    await desktopStudent.close();

    const mobileStudent = await openScenario(browser, 'student', 'student-dashboard.html', { width: 390, height: 844 });
    state = await snapshotState(mobileStudent);
    assert(!state.sidebarClasses.includes('desktop-collapsed'), 'student mobile should not start desktop-collapsed', state);
    assert(!state.sidebarClasses.includes('mobile-open'), 'student mobile should not start open', state);
    assert(state.overlayHidden === true, 'student mobile overlay should start hidden', state);

    await clickToggle(mobileStudent, 'toggleSidebar');
    state = await snapshotState(mobileStudent);
    assert(state.sidebarClasses.includes('mobile-open'), 'student mobile toggle should open mobile drawer', state);
    assert(!state.sidebarClasses.includes('desktop-collapsed'), 'student mobile toggle should not set desktop-collapsed', state);
    assert(state.overlayActive === true, 'student mobile toggle should activate overlay', state);
    assert(state.overlayHidden === false, 'student mobile toggle should reveal overlay', state);
    assert(!state.mainContentClasses.includes('collapsed'), 'student mobile should not collapse main content', state);
    assert(state.bodyClasses.includes('sidebar-mobile-open'), 'student mobile should mark body as sidebar-mobile-open', state);
    await mobileStudent.close();

    const desktopTeacher = await openScenario(browser, 'teacher', 'teacher-dashboard.html', { width: 1366, height: 900 });
    state = await snapshotState(desktopTeacher);
    assert(state.sidebarExists, 'teacher desktop sidebar should exist');
    assert(state.mainContentExists, 'teacher desktop mainContent should exist');
    assert(state.overlayExists, 'teacher desktop overlay should exist');

    await clickToggle(desktopTeacher, 'toggleBtn');
    state = await snapshotState(desktopTeacher);
    assert(state.sidebarClasses.includes('desktop-collapsed'), 'teacher desktop toggle should apply desktop-collapsed', state);
    assert(state.mainContentClasses.includes('collapsed'), 'teacher desktop toggle should collapse main content', state);
    await desktopTeacher.close();

    const mobileTeacher = await openScenario(browser, 'teacher', 'teacher-dashboard.html', { width: 390, height: 844 });
    state = await snapshotState(mobileTeacher);
    assert(!state.sidebarClasses.includes('desktop-collapsed'), 'teacher mobile should not start desktop-collapsed', state);
    assert(!state.sidebarClasses.includes('mobile-open'), 'teacher mobile should not start open', state);

    await clickToggle(mobileTeacher, 'toggleBtn');
    state = await snapshotState(mobileTeacher);
    assert(state.sidebarClasses.includes('mobile-open'), 'teacher mobile toggle should open mobile drawer', state);
    assert(state.overlayActive === true, 'teacher mobile toggle should activate overlay', state);
    assert(state.overlayHidden === false, 'teacher mobile overlay should become visible', state);
    assert(state.scrollWidth <= state.clientWidth + 1, 'teacher mobile page should not create horizontal overflow after opening drawer', state);

    await mobileTeacher.evaluate(() => {
      document.getElementById('sidebarOverlay')?.click();
    });
    state = await snapshotState(mobileTeacher);
    assert(!state.sidebarClasses.includes('mobile-open'), 'teacher mobile overlay click should close drawer', state);
    assert(state.overlayHidden === true, 'teacher mobile overlay should hide after close', state);
    await mobileTeacher.close();

    console.log('Sidebar shell verification passed.');
  } finally {
    await browser.close();
  }
})().catch((error) => {
  console.error('Sidebar shell verification failed:');
  console.error(`- ${error.message}`);
  process.exit(1);
});
