const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('../major_assignment/node_modules/playwright');

const baseUrl = process.env.FRONTEND_BASE_URL || 'http://127.0.0.1:5500';
const sessionDir = path.join(process.cwd(), '.runtime-logs');

const checks = [
  {
    role: 'teacher',
    page: 'teacher-dashboard.html',
    sessionFile: 'teacher-session-theme.json',
    bodyClass: 'teacher',
    avatarPrimary: [37, 99, 235],
    avatarStrong: [29, 78, 216],
    nameColor: [29, 78, 216],
    arrowColor: [37, 99, 235],
    forbidden: /#667eea|#764ba2|#4c1d95|#7c3aed|#6366f1|#4f46e5|#8b5cf6|76,\s*29,\s*149|124,\s*58,\s*237|99,\s*102,\s*241|79,\s*70,\s*229|139,\s*92,\s*246/i,
  },
  {
    role: 'student',
    page: 'student-dashboard.html',
    sessionFile: 'student-session-theme.json',
    bodyClass: 'student-theme',
    avatarPrimary: [13, 148, 136],
    avatarStrong: [15, 118, 110],
    nameColor: [15, 118, 110],
    arrowColor: [13, 148, 136],
    forbidden: /#3b82f6|#2563eb|#1d4ed8|#667eea|#764ba2|#4c1d95|#7c3aed|#6366f1|#4f46e5|#8b5cf6|59,\s*130,\s*246|37,\s*99,\s*235|29,\s*78,\s*216|76,\s*29,\s*149|124,\s*58,\s*237|99,\s*102,\s*241|79,\s*70,\s*229|139,\s*92,\s*246/i,
  },
];

function parseRgbValues(value) {
  const matches = [...String(value || '').matchAll(/rgba?\((\d+),\s*(\d+),\s*(\d+)/g)];
  return matches.map((match) => match.slice(1, 4).map(Number));
}

function roughlyEqual(actual, expected, tolerance = 8) {
  return actual.every((channel, index) => Math.abs(channel - expected[index]) <= tolerance);
}

function styleHasRgb(value, expected) {
  return parseRgbValues(value).some((rgb) => roughlyEqual(rgb, expected));
}

function loadSession(sessionFile) {
  const sessionPath = path.join(sessionDir, sessionFile);
  if (!fs.existsSync(sessionPath)) {
    throw new Error(`Missing auth session ${sessionPath}. Run scripts/get-dev-auth-session.ps1 for this role first.`);
  }
  const payload = JSON.parse(fs.readFileSync(sessionPath, 'utf8'));
  return payload.sessionStorage || {};
}

async function inspectDropdown(browser, check) {
  const page = await browser.newPage({ viewport: { width: 860, height: 792 } });
  const session = loadSession(check.sessionFile);
  await page.addInitScript((sessionStorageValues) => {
    for (const [key, value] of Object.entries(sessionStorageValues)) {
      if (value != null) window.sessionStorage.setItem(key, String(value));
    }
  }, session);

  await page.goto(`${baseUrl}/${check.page}`, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(1600);

  const result = await page.evaluate(() => {
    const readStyle = (selector) => {
      const element = document.querySelector(selector);
      if (!element) return null;
      const style = getComputedStyle(element);
      return {
        backgroundColor: style.backgroundColor,
        backgroundImage: style.backgroundImage,
        color: style.color,
        borderColor: style.borderColor,
        outlineColor: style.outlineColor,
      };
    };

    return {
      path: location.pathname,
      bodyClass: document.body.className,
      containerClass: document.querySelector('#userDropdown')?.className || '',
      button: readStyle('#userDropdownBtn'),
      avatar: readStyle('#userDropdownAvatar'),
      name: readStyle('#userDropdownName'),
      role: readStyle('.user-dropdown-role'),
      arrow: readStyle('.user-dropdown-arrow'),
    };
  });

  await page.close();
  return result;
}

(async () => {
  const browser = await chromium.launch({ headless: true });
  const failures = [];

  try {
    for (const check of checks) {
      const result = await inspectDropdown(browser, check);
      if (!result.bodyClass.includes(check.bodyClass)) {
        failures.push(`${check.page}: missing ${check.bodyClass} body theme class`);
      }

      for (const [part, value] of Object.entries(result)) {
        if (value && typeof value === 'object') {
          const combined = Object.values(value).join(' ');
          if (check.forbidden.test(combined)) {
            failures.push(`${check.page}: ${part} still exposes an old role color (${combined})`);
          }
        }
      }

      const avatarImage = result.avatar?.backgroundImage || result.avatar?.backgroundColor || '';
      if (!styleHasRgb(avatarImage, check.avatarPrimary) || !styleHasRgb(avatarImage, check.avatarStrong)) {
        failures.push(`${check.page}: avatar is not ${check.role} themed (${avatarImage})`);
      }

      if (!styleHasRgb(result.name?.color, check.nameColor)) {
        failures.push(`${check.page}: user name is not ${check.role} themed (${result.name?.color || 'missing'})`);
      }

      if (!styleHasRgb(result.arrow?.color, check.arrowColor)) {
        failures.push(`${check.page}: dropdown arrow is not ${check.role} themed (${result.arrow?.color || 'missing'})`);
      }
    }
  } finally {
    await browser.close();
  }

  if (failures.length) {
    console.error('User dropdown role theme browser verification failed:');
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
  }

  console.log('User dropdown role theme browser verification passed.');
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
