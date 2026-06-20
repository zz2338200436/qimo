const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('../major_assignment/node_modules/playwright');

const baseUrl = process.env.FRONTEND_BASE_URL || 'http://127.0.0.1:5500';
const htmlDir = path.join(process.cwd(), 'frontend', 'dist');
const teacherSessionPath = path.join(process.cwd(), '.runtime-logs', 'teacher-session-theme.json');
const studentSessionPath = path.join(process.cwd(), '.runtime-logs', 'student-session-theme.json');
const skippedBrowserRoleThemePages = new Set([
  'student-ai-assistant.html',
]);

const pages = fs.readdirSync(htmlDir)
  .filter((file) => file.endsWith('.html'))
  .filter((file) => !skippedBrowserRoleThemePages.has(file))
  .sort();

function parseRgb(value) {
  const match = /rgba?\((\d+),\s*(\d+),\s*(\d+)/.exec(value || '');
  return match ? match.slice(1, 4).map(Number) : null;
}

function roughlyEqual(actual, expected, tolerance = 8) {
  if (!actual) return false;
  return actual.every((channel, index) => Math.abs(channel - expected[index]) <= tolerance);
}

function parseAllRgbValues(value) {
  const matches = [...String(value || '').matchAll(/rgba?\((\d+),\s*(\d+),\s*(\d+)/g)];
  return matches.map((match) => match.slice(1, 4).map(Number));
}

function styleHasRgb(value, expected) {
  return parseAllRgbValues(value).some((rgb) => roughlyEqual(rgb, expected));
}

function loadSession(filePath) {
  if (!fs.existsSync(filePath)) return null;
  const payload = JSON.parse(fs.readFileSync(filePath, 'utf8'));
  return payload.sessionStorage || null;
}

function authStateForPage(pageName) {
  if (pageName.startsWith('teacher-') && pageName !== 'teacher-login.html') {
    return loadSession(teacherSessionPath);
  }
  if (pageName.startsWith('student-') && pageName !== 'student-login.html') {
    return loadSession(studentSessionPath);
  }
  return null;
}

async function inspectPage(browser, pageName) {
  const page = await browser.newPage({ viewport: { width: 1366, height: 900 } });
  const authState = authStateForPage(pageName);
  if (authState) {
    await page.addInitScript((state) => {
      for (const [key, value] of Object.entries(state)) {
        if (value != null) window.sessionStorage.setItem(key, String(value));
      }
    }, authState);
  }
  const url = `${baseUrl}/${pageName}`;
  await page.goto(url, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(1400);

  const result = await page.evaluate(() => {
    const bodyClass = document.body.className;
    const firstPrimary = document.querySelector('.btn-primary');
    const activeTab = document.querySelector('.tab-item.active, .nav-pills .nav-link.active');
    const bgPrimary = document.querySelector('.bg-primary');
    const textPrimary = document.querySelector('.text-primary');
    const sidebar = document.querySelector('.sidebar, #sidebar');
    const dropdownMount = document.querySelector('#userDropdownContainer');
    const dropdownButton = document.querySelector('#userDropdownBtn');
    const dropdownAvatar = document.querySelector('#userDropdownAvatar');
    const dropdownName = document.querySelector('#userDropdownName');
    const dropdownRole = document.querySelector('.user-dropdown-role');
    const dropdownArrow = document.querySelector('.user-dropdown-arrow');
    const teacherDashboardCourseIcon = document.querySelector('.teacher-stat-panel .stat-icon.courses');
    const teacherDashboardExamIcon = document.querySelector('.teacher-stat-panel .stat-icon.exams');
    const teacherTopbar = document.querySelector('.teacher-topbar');
    const teacherTopbarBreadcrumb = document.querySelector('.teacher-topbar .breadcrumb');
    const styles = (element) => element ? getComputedStyle(element) : null;
    const topbarStyle = styles(teacherTopbar);

    return {
      bodyClass,
      path: location.pathname,
      scrollWidth: document.documentElement.scrollWidth,
      clientWidth: document.documentElement.clientWidth,
      primaryButtonBackground: styles(firstPrimary)?.backgroundColor || '',
      primaryButtonImage: styles(firstPrimary)?.backgroundImage || '',
      activeTabColor: styles(activeTab)?.color || '',
      activeTabBorderColor: styles(activeTab)?.borderBottomColor || styles(activeTab)?.borderColor || '',
      bgPrimaryBackground: styles(bgPrimary)?.backgroundColor || '',
      textPrimaryColor: styles(textPrimary)?.color || '',
      sidebarBackground: styles(sidebar)?.backgroundImage || styles(sidebar)?.backgroundColor || '',
      hasUserDropdownMount: Boolean(dropdownMount),
      hasUserDropdownButton: Boolean(dropdownButton),
      userDropdownContainerClass: document.querySelector('#userDropdown')?.className || '',
      userDropdownButtonBackground: styles(dropdownButton)?.backgroundColor || '',
      userDropdownButtonImage: styles(dropdownButton)?.backgroundImage || '',
      userDropdownAvatarBackground: styles(dropdownAvatar)?.backgroundColor || '',
      userDropdownAvatarImage: styles(dropdownAvatar)?.backgroundImage || '',
      userDropdownNameColor: styles(dropdownName)?.color || '',
      userDropdownRoleColor: styles(dropdownRole)?.color || '',
      userDropdownArrowColor: styles(dropdownArrow)?.color || '',
      teacherDashboardCourseIconBackground: styles(teacherDashboardCourseIcon)?.backgroundImage || styles(teacherDashboardCourseIcon)?.backgroundColor || '',
      teacherDashboardExamIconBackground: styles(teacherDashboardExamIcon)?.backgroundImage || styles(teacherDashboardExamIcon)?.backgroundColor || '',
      teacherTopbarBackground: topbarStyle?.backgroundColor || '',
      teacherTopbarBackgroundImage: topbarStyle?.backgroundImage || '',
      teacherTopbarBorderColor: topbarStyle?.borderBottomColor || '',
      teacherTopbarBoxShadow: topbarStyle?.boxShadow || '',
      teacherTopbarBackdropFilter: topbarStyle?.backdropFilter || topbarStyle?.webkitBackdropFilter || '',
      hasTeacherTopbarBreadcrumb: Boolean(teacherTopbarBreadcrumb),
    };
  });

  await page.close();
  return result;
}

(async () => {
  const browser = await chromium.launch({ headless: true });
  const failures = [];
  try {
    for (const pageName of pages) {
      const result = await inspectPage(browser, pageName);
      const isTeacher = pageName.startsWith('teacher-');
      const isStudent = pageName.startsWith('student-');

      if (isTeacher && pageName !== 'teacher-login.html' && result.path.endsWith('/teacher-login.html')) {
        failures.push(`${pageName}: redirected to teacher login during authenticated inspection`);
      }

      if (isStudent && pageName !== 'student-login.html' && result.path.endsWith('/student-login.html')) {
        failures.push(`${pageName}: redirected to student login during authenticated inspection`);
      }

      if (result.scrollWidth > result.clientWidth + 1) {
        failures.push(`${pageName}: horizontal overflow ${result.scrollWidth}/${result.clientWidth}`);
      }

      if (isTeacher && !result.bodyClass.includes('teacher')) {
        failures.push(`${pageName}: missing teacher body theme class`);
      }

      if (isStudent && !result.bodyClass.includes('student-theme')) {
        failures.push(`${pageName}: missing student body theme class`);
      }

      const combined = [
        result.primaryButtonBackground,
        result.primaryButtonImage,
        result.activeTabColor,
        result.activeTabBorderColor,
        result.bgPrimaryBackground,
        result.textPrimaryColor,
        result.sidebarBackground,
        result.userDropdownButtonBackground,
        result.userDropdownButtonImage,
        result.userDropdownAvatarBackground,
        result.userDropdownAvatarImage,
        result.userDropdownNameColor,
        result.userDropdownRoleColor,
        result.userDropdownArrowColor,
        result.teacherDashboardCourseIconBackground,
        result.teacherDashboardExamIconBackground,
        result.teacherTopbarBackground,
        result.teacherTopbarBackgroundImage,
        result.teacherTopbarBorderColor,
      ].join(' ');

      if (/#667eea|#764ba2|#4c1d95|#7c3aed|#6366f1|#4f46e5|#8b5cf6|102,\s*126,\s*234|118,\s*75,\s*162|76,\s*29,\s*149|124,\s*58,\s*237|99,\s*102,\s*241|79,\s*70,\s*229|139,\s*92,\s*246/i.test(combined)) {
        failures.push(`${pageName}: computed style still exposes previous purple palette`);
      }

      if (isStudent && /#3b82f6|#2563eb|#1d4ed8|59,\s*130,\s*246|37,\s*99,\s*235|29,\s*78,\s*216/i.test(combined)) {
        failures.push(`${pageName}: computed style still exposes teacher/default-blue palette on student page`);
      }

      if (isTeacher && /#0d9488|#0f766e|#14b8a6|13,\s*148,\s*136|15,\s*118,\s*110|20,\s*184,\s*166/i.test(result.sidebarBackground)) {
        failures.push(`${pageName}: teacher sidebar is using the student teal palette (${result.sidebarBackground})`);
      }

      const primaryRgb = parseRgb(result.primaryButtonBackground);
      if (isTeacher && result.primaryButtonBackground && !roughlyEqual(primaryRgb, [37, 99, 235]) && !result.primaryButtonImage.includes('37, 99, 235')) {
        failures.push(`${pageName}: primary button is not teacher blue (${result.primaryButtonBackground || result.primaryButtonImage})`);
      }

      if (isStudent && result.primaryButtonBackground && !roughlyEqual(primaryRgb, [13, 148, 136]) && !result.primaryButtonImage.includes('13, 148, 136')) {
        failures.push(`${pageName}: primary button is not student teal (${result.primaryButtonBackground || result.primaryButtonImage})`);
      }

      const isAuthenticatedRolePage = (isTeacher && pageName !== 'teacher-login.html') || (isStudent && pageName !== 'student-login.html');
      if (isAuthenticatedRolePage && result.hasUserDropdownMount && !result.hasUserDropdownButton) {
        failures.push(`${pageName}: user dropdown mount exists but the component did not render`);
      }

      if (isTeacher && result.hasUserDropdownButton) {
        if (!result.userDropdownContainerClass.includes('teacher-theme')) {
          failures.push(`${pageName}: user dropdown is missing teacher-theme class`);
        }
        const avatarStyle = `${result.userDropdownAvatarBackground} ${result.userDropdownAvatarImage}`;
        if (!styleHasRgb(avatarStyle, [37, 99, 235]) || !styleHasRgb(avatarStyle, [29, 78, 216])) {
          failures.push(`${pageName}: user dropdown avatar is not teacher blue (${avatarStyle.trim()})`);
        }
        if (!styleHasRgb(result.userDropdownNameColor, [29, 78, 216])) {
          failures.push(`${pageName}: user dropdown name is not teacher blue (${result.userDropdownNameColor})`);
        }
        if (!styleHasRgb(result.userDropdownArrowColor, [37, 99, 235])) {
          failures.push(`${pageName}: user dropdown arrow is not teacher blue (${result.userDropdownArrowColor})`);
        }
      }

      if (isTeacher && pageName !== 'teacher-login.html') {
        if (!styleHasRgb(result.sidebarBackground, [15, 23, 42]) || !styleHasRgb(result.sidebarBackground, [29, 78, 216]) || !styleHasRgb(result.sidebarBackground, [37, 99, 235])) {
          failures.push(`${pageName}: teacher sidebar is not the navy-to-blue teacher gradient (${result.sidebarBackground})`);
        }
        if (result.hasTeacherTopbarBreadcrumb) {
          failures.push(`${pageName}: teacher topbar should not render breadcrumb navigation`);
        }
        if (!styleHasRgb(result.teacherTopbarBackgroundImage, [239, 246, 255]) || !styleHasRgb(result.teacherTopbarBackgroundImage, [219, 234, 254])) {
          failures.push(`${pageName}: teacher topbar is not using the light-blue glass gradient (${result.teacherTopbarBackgroundImage || result.teacherTopbarBackground})`);
        }
        if (!styleHasRgb(result.teacherTopbarBorderColor, [37, 99, 235])) {
          failures.push(`${pageName}: teacher topbar border is not teacher blue tinted (${result.teacherTopbarBorderColor})`);
        }
        if (!result.teacherTopbarBoxShadow || result.teacherTopbarBoxShadow === 'none') {
          failures.push(`${pageName}: teacher topbar is missing the glass shadow`);
        }
        if (!/blur\(/i.test(result.teacherTopbarBackdropFilter)) {
          failures.push(`${pageName}: teacher topbar is missing backdrop blur (${result.teacherTopbarBackdropFilter || 'none'})`);
        }
      }

      if (isStudent && result.hasUserDropdownButton) {
        if (!result.userDropdownContainerClass.includes('student-theme')) {
          failures.push(`${pageName}: user dropdown is missing student-theme class`);
        }
        const avatarStyle = `${result.userDropdownAvatarBackground} ${result.userDropdownAvatarImage}`;
        if (!styleHasRgb(avatarStyle, [13, 148, 136]) || !styleHasRgb(avatarStyle, [15, 118, 110])) {
          failures.push(`${pageName}: user dropdown avatar is not student teal (${avatarStyle.trim()})`);
        }
        if (!styleHasRgb(result.userDropdownNameColor, [15, 118, 110])) {
          failures.push(`${pageName}: user dropdown name is not student teal (${result.userDropdownNameColor})`);
        }
        if (!styleHasRgb(result.userDropdownArrowColor, [13, 148, 136])) {
          failures.push(`${pageName}: user dropdown arrow is not student teal (${result.userDropdownArrowColor})`);
        }
      }

      if (pageName === 'teacher-dashboard.html') {
        if (!styleHasRgb(result.teacherDashboardCourseIconBackground, [15, 23, 42]) || !styleHasRgb(result.teacherDashboardCourseIconBackground, [37, 99, 235])) {
          failures.push(`${pageName}: course stat icon is not the teacher shell gradient (${result.teacherDashboardCourseIconBackground})`);
        }
        if (!styleHasRgb(result.teacherDashboardExamIconBackground, [29, 78, 216]) || !styleHasRgb(result.teacherDashboardExamIconBackground, [37, 99, 235])) {
          failures.push(`${pageName}: exam stat icon is not the teacher blue gradient (${result.teacherDashboardExamIconBackground || 'missing .stat-icon.exams'})`);
        }
      }
    }
  } finally {
    await browser.close();
  }

  if (failures.length) {
    console.error('All-page role theme browser verification failed:');
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
  }

  console.log(`All-page role theme browser verification passed for ${pages.length} pages.`);
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
