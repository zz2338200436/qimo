const fs = require('fs');
const path = require('path');
const { chromium } = require('../major_assignment/node_modules/playwright');

const baseUrl = process.env.FRONTEND_BASE_URL || 'http://127.0.0.1:5501';

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

function normalizeColor(value) {
  return String(value || '').replace(/\s+/g, ' ').trim();
}

async function inspectPage(page, pageName) {
  await page.goto(`${baseUrl}/${pageName}`, { waitUntil: 'domcontentloaded' });
  if (pageName.includes('dashboard')) {
    await page.waitForSelector('.sidebar', { timeout: 5000 });
  } else {
    await page.waitForTimeout(250);
  }
  return page.evaluate(() => {
    const body = document.body;
    const sidebar = document.querySelector('.sidebar');
    const loginHeader = document.querySelector('.login-header');
    const loginButton = document.querySelector('.login-btn');
    return {
      bodyClass: body.className,
      bodyBackground: getComputedStyle(body).backgroundImage || getComputedStyle(body).backgroundColor,
      sidebarTheme: sidebar ? sidebar.getAttribute('data-role-theme') : null,
      sidebarBackground: sidebar ? getComputedStyle(sidebar).backgroundImage : null,
      loginHeaderBackground: loginHeader ? getComputedStyle(loginHeader).backgroundImage : null,
      loginButtonBackground: loginButton ? getComputedStyle(loginButton).backgroundImage : null,
      scrollWidth: document.documentElement.scrollWidth,
      clientWidth: document.documentElement.clientWidth,
    };
  });
}

function expectIncludes(label, actual, expected) {
  if (!normalizeColor(actual).includes(expected)) {
    throw new Error(`${label} expected to include "${expected}", got "${actual}"`);
  }
}

function expectNoHorizontalOverflow(label, result) {
  if (result.scrollWidth > result.clientWidth + 1) {
    throw new Error(`${label} has horizontal overflow: scrollWidth=${result.scrollWidth}, clientWidth=${result.clientWidth}`);
  }
}

(async () => {
  const executablePath = resolveBundledChromium();
  const browser = await chromium.launch(executablePath ? { headless: true, executablePath } : { headless: true });
  const page = await browser.newPage({ viewport: { width: 1366, height: 768 } });
  const mobilePage = await browser.newPage({ viewport: { width: 390, height: 844 } });

  const seedSession = () => {
    sessionStorage.setItem('token', 'role-theme-verification-token');
    sessionStorage.setItem('user', JSON.stringify({
      id: 1,
      username: 'role-theme-verifier',
      realName: 'Role Theme Verifier',
      role: 'teacher',
    }));
  };
  await page.addInitScript(seedSession);
  await mobilePage.addInitScript(seedSession);

  try {
    const studentLogin = await inspectPage(page, 'student-login.html');
    expectIncludes('student login body class', studentLogin.bodyClass, 'student-login-page');
    expectIncludes('student login header', studentLogin.loginHeaderBackground, 'rgb(15, 118, 110)');
    expectIncludes('student login button', studentLogin.loginButtonBackground, 'rgb(13, 148, 136)');
    expectNoHorizontalOverflow('student login desktop', studentLogin);

    const teacherLogin = await inspectPage(page, 'teacher-login.html');
    expectIncludes('teacher login body class', teacherLogin.bodyClass, 'teacher-login-page');
    expectIncludes('teacher login header', teacherLogin.loginHeaderBackground, 'rgb(15, 23, 42)');
    expectIncludes('teacher login button', teacherLogin.loginButtonBackground, 'rgb(37, 99, 235)');
    expectNoHorizontalOverflow('teacher login desktop', teacherLogin);

    const studentDashboard = await inspectPage(page, 'student-dashboard.html');
    expectIncludes('student dashboard body class', studentDashboard.bodyClass, 'student-theme');
    expectIncludes('student sidebar theme', studentDashboard.sidebarTheme, 'student');
    expectIncludes('student sidebar background', studentDashboard.sidebarBackground, 'rgb(15, 118, 110)');
    expectNoHorizontalOverflow('student dashboard desktop', studentDashboard);

    const teacherDashboard = await inspectPage(page, 'teacher-dashboard.html');
    expectIncludes('teacher dashboard body class', teacherDashboard.bodyClass, 'teacher-shell-body');
    expectIncludes('teacher sidebar theme', teacherDashboard.sidebarTheme, 'teacher');
    expectIncludes('teacher sidebar background', teacherDashboard.sidebarBackground, 'rgb(15, 23, 42)');
    expectNoHorizontalOverflow('teacher dashboard desktop', teacherDashboard);

    const studentMobile = await inspectPage(mobilePage, 'student-dashboard.html');
    expectNoHorizontalOverflow('student dashboard mobile', studentMobile);

    const teacherMobile = await inspectPage(mobilePage, 'teacher-dashboard.html');
    expectNoHorizontalOverflow('teacher dashboard mobile', teacherMobile);

    console.log('Role color browser verification passed.');
  } finally {
    await browser.close();
  }
})().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
