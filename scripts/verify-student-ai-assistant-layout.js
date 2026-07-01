const path = require('path');
const fs = require('fs');
const { chromium } = require(path.resolve(__dirname, '../major_assignment/node_modules/playwright'));

function assert(condition, message, details) {
  if (!condition) {
    const error = new Error(message);
    if (details !== undefined) {
      error.details = details;
    }
    throw error;
  }
}

async function createPage(context, viewport) {
  const page = await context.newPage();
  await page.setViewportSize(viewport);
  await page.addInitScript(() => {
    sessionStorage.setItem('token', 'layout-check-token');
    sessionStorage.setItem('activeRole', 'STUDENT');
    sessionStorage.setItem('role', 'STUDENT');
    sessionStorage.setItem('userId', '42');
    sessionStorage.setItem('user', JSON.stringify({
      id: 42,
      username: 'student42',
      name: 'Student Forty Two',
      roles: ['STUDENT'],
      activeRole: 'STUDENT'
    }));
  });
  await page.goto('http://localhost:5500/student-ai-assistant.html', { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('.student-ai-history-rail');
  return page;
}

async function verifyDesktopLayout(page) {
  const rail = page.locator('.student-ai-history-rail');
  const railButtons = rail.locator('.student-ai-history-rail-button');
  const railLabels = rail.locator('.student-ai-history-rail-label');
  const shell = page.locator('.student-ai-chat-shell');
  const panel = page.locator('.agent-panel.chatgpt-like');
  const title = page.locator('.student-ai-empty-hint strong');
  const input = page.locator('.agent-input-shell');

  assert(await rail.count() === 1, 'student history actions should live in one floating rail');
  assert(await railButtons.count() === 3, 'student history rail should expose history, search, and new-chat actions');
  assert(await railLabels.count() === 3, 'student history rail should include accessible labels for all actions');
  assert(await page.locator('.student-ai-service-note').count() === 0, 'student learning-suggestion note should be removed');
  assert(await page.locator('[data-student-ai-example="生成我的学习建议"]').count() === 0, 'student learning-suggestion quick prompt should be removed');

  const shellBox = await shell.boundingBox();
  const railBox = await rail.boundingBox();
  const panelBox = await panel.boundingBox();
  const titleBox = await title.boundingBox();
  const inputBox = await input.boundingBox();
  const viewport = page.viewportSize();
  assert(shellBox && railBox && panelBox && titleBox && inputBox && viewport,
    'student AI shell, rail, panel, empty title, and input should be visible');

  const shellCenter = shellBox.x + shellBox.width / 2;
  const panelCenter = panelBox.x + panelBox.width / 2;
  assert(railBox.x + railBox.width / 2 > shellCenter,
    'student history rail should sit on the right side like the teacher page',
    { railBox, shellBox, shellCenter });
  assert(Math.abs(panelCenter - shellCenter) < 56,
    'student assistant panel should remain visually centered in the content shell',
    { panelBox, shellBox, panelCenter, shellCenter });
  assert(titleBox.y > viewport.height * 0.24 && titleBox.y < viewport.height * 0.64,
    'student empty-state headline should remain in the centered hero band',
    { titleBox, viewport });
  assert(inputBox.y > titleBox.y && inputBox.y < viewport.height * 0.92,
    'student prompt input should sit below the hero copy and fit in the first viewport',
    { inputBox, viewport });
}

async function verifyMobileLayout(page) {
  const metrics = await page.evaluate(() => ({
    clientWidth: document.documentElement.clientWidth,
    scrollWidth: document.documentElement.scrollWidth
  }));
  assert(metrics.scrollWidth <= metrics.clientWidth + 1, 'student AI assistant should not create horizontal scroll on mobile', metrics);

  const rail = page.locator('.student-ai-history-rail');
  const railBox = await rail.boundingBox();
  assert(railBox, 'student history rail should remain visible on mobile');
  assert(railBox.x >= 0 && railBox.x + railBox.width <= metrics.clientWidth + 1,
    'student history rail should fit within the mobile viewport',
    { railBox, metrics });
}

(async () => {
  const bundledChromium = path.join(
    process.env.LOCALAPPDATA || '',
    'ms-playwright',
    'chromium_headless_shell-1228',
    'chrome-headless-shell-win64',
    'chrome-headless-shell.exe'
  );
  const launchOptions = fs.existsSync(bundledChromium)
    ? { headless: true, executablePath: bundledChromium }
    : { headless: true };
  const browser = await chromium.launch(launchOptions);
  const context = await browser.newContext();
  try {
    const desktopPage = await createPage(context, { width: 894, height: 711 });
    await verifyDesktopLayout(desktopPage);
    await desktopPage.close();

    const mobilePage = await createPage(context, { width: 390, height: 844 });
    await verifyMobileLayout(mobilePage);
    await mobilePage.close();

    console.log('Student AI assistant layout verification passed.');
  } finally {
    await context.close().catch(() => {});
    await browser.close().catch(() => {});
  }
})().catch(error => {
  console.error(error.message);
  if (error.details !== undefined) {
    console.error(JSON.stringify(error.details, null, 2));
  }
  process.exit(1);
});
