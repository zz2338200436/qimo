const path = require('path');
const fs = require('fs');
const { chromium } = require(path.resolve(__dirname, '../major_assignment/node_modules/playwright'));
const FRONTEND_BASE_URL = (process.env.FRONTEND_BASE_URL || 'http://localhost:5500').replace(/\/$/, '');

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
    sessionStorage.setItem('activeRole', 'TEACHER');
    sessionStorage.setItem('role', 'TEACHER');
    sessionStorage.setItem('userId', '7');
    sessionStorage.setItem('user', JSON.stringify({
      id: 7,
      username: 'teacher7',
      name: 'Teacher Seven',
      roles: ['TEACHER'],
      activeRole: 'TEACHER'
    }));
  });
  await page.goto(`${FRONTEND_BASE_URL}/teacher-ai-tools.html`, { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('.teacher-ai-chat-shell');
  await page.waitForSelector('[data-agent-input]');
  return page;
}

async function verifyDesktopLayout(page) {
  const toolbarCount = await page.locator('.teacher-ai-chat-toolbar').count();
  assert(toolbarCount === 0, 'history control should not occupy a standalone toolbar row', toolbarCount);

  const panel = page.locator('.agent-panel.chatgpt-like');
  const hiddenHeader = page.locator('.agent-panel.chatgpt-like .agent-panel-header');
  const historyRail = page.locator('.teacher-ai-history-rail');
  const railButtons = historyRail.locator('.teacher-ai-history-rail-button');
  const railLabels = historyRail.locator('.teacher-ai-history-rail-label');
  const sidebarToggle = page.locator('#toggleSidebar');
  const webSearchToggle = page.locator('[data-teacher-ai-web-search]');
  const starterPrompts = page.locator('.teacher-ai-example-prompts .teacher-ai-example-prompt');
  const followupPrompts = page.locator('.teacher-ai-followup-prompts .teacher-ai-example-prompt');

  assert(await panel.count() === 1, 'assistant panel should render once');
  assert(await hiddenHeader.count() === 1, 'assistant header placeholder should remain in the DOM for shared panel structure');
  assert(await hiddenHeader.isHidden(), 'teacher AI page should hide the unused shared assistant header');
  assert(await historyRail.count() === 1, 'history actions should live in the floating rail');
  assert(await railButtons.count() === 3, 'history rail should expose history, search, and new-chat actions');
  assert(await railLabels.count() === 3, 'history rail should label all three actions in desktop layout');
  assert(await sidebarToggle.count() === 1, 'topbar sidebar toggle should expose the id used by CommonUI.bindSidebarToggle');
  assert(await webSearchToggle.count() === 1, 'composer should expose exactly one web-search toggle');
  assert(await webSearchToggle.getAttribute('aria-pressed') === 'false', 'web-search toggle should start unselected');
  assert(!(await webSearchToggle.evaluate((button) => button.classList.contains('teacher-ai-input-web-search-active'))),
    'web-search toggle should not start with the active class');
  assert(await starterPrompts.count() === 3, 'empty state should keep three starter prompts after removing the duplicate search prompt');
  assert(!(await page.locator('.teacher-ai-example-prompts').innerText()).includes('联网搜索'),
    'empty-state starter prompt row should not contain a duplicate web-search entry');
  assert(await followupPrompts.count() === 3, 'follow-up prompt row should keep three non-search prompts');
  assert(!(await page.locator('.teacher-ai-followup-prompts').innerText()).includes('联网搜索'),
    'follow-up prompt row should not contain a duplicate web-search entry');

  const railBox = await historyRail.boundingBox();
  const titleBox = await page.locator('.teacher-ai-empty-hint strong').boundingBox();
  const inputBox = await page.locator('.agent-input-shell').boundingBox();
  const formBox = await page.locator('[data-agent-form]').boundingBox();
  const webSearchBox = await webSearchToggle.boundingBox();
  const submitBox = await page.locator('[data-agent-submit]').boundingBox();
  const shellBox = await page.locator('.teacher-ai-chat-shell').boundingBox();
  const panelBox = await panel.boundingBox();
  const viewport = page.viewportSize();
  const shellCenter = shellBox.x + shellBox.width / 2;
  assert(railBox && titleBox && inputBox && formBox && webSearchBox && submitBox && shellBox && panelBox && viewport,
    'history rail, empty title, input, web-search toggle, submit button, shell, and panel should be visible');

  assert(railBox.x + (railBox.width / 2) > shellCenter,
    'history rail should sit on the right side of the working shell',
    { railBox, shellCenter });
  assert(titleBox.y > viewport.height * 0.28 && titleBox.y < viewport.height * 0.62,
    'empty-state headline should stay in the centered hero band before the first send',
    { titleBox, viewport });
  assert(inputBox.y > titleBox.y && inputBox.y < viewport.height * 0.9,
    'prompt input should remain below the hero copy and still fit inside the first viewport',
    { inputBox, viewport });
  assert(webSearchBox.x > formBox.x && submitBox.x > webSearchBox.x,
    'web-search toggle should sit between the textarea and submit button inside the composer',
    { formBox, webSearchBox, submitBox });
  assert(webSearchBox.y >= formBox.y && webSearchBox.y + webSearchBox.height <= formBox.y + formBox.height + 1,
    'web-search toggle should stay vertically contained in the composer',
    { formBox, webSearchBox });
  assert(submitBox.y >= formBox.y && submitBox.y + submitBox.height <= formBox.y + formBox.height + 1,
    'submit button should stay vertically contained in the composer',
    { formBox, submitBox });
  const webSearchCenterY = webSearchBox.y + webSearchBox.height / 2;
  const submitCenterY = submitBox.y + submitBox.height / 2;
  assert(Math.abs(webSearchCenterY - submitCenterY) <= 2,
    'web-search toggle and submit button should be vertically aligned',
    { webSearchBox, submitBox, webSearchCenterY, submitCenterY });
  assert(await page.locator('.teacher-agent-history-preview').count() === 0,
    'recent-session preview card should be removed because the header history button opens the same drawer');
  const panelCenter = panelBox.x + panelBox.width / 2;
  assert(Math.abs(panelCenter - shellCenter) < 48,
    'assistant panel should stay visually centered in the content shell even with the right utility rail',
    { panelBox, shellBox, panelCenter, shellCenter });
}

async function verifyMobileLayout(page) {
  const metrics = await page.evaluate(() => ({
    clientWidth: document.documentElement.clientWidth,
    scrollWidth: document.documentElement.scrollWidth
  }));
  assert(metrics.scrollWidth <= metrics.clientWidth + 1, 'teacher AI tools page should not create horizontal scroll on mobile', metrics);

  const rail = page.locator('.teacher-ai-history-rail');
  const railBox = await rail.boundingBox();
  assert(railBox, 'history rail should remain visible on mobile');
  assert(railBox.x >= 0 && railBox.x + railBox.width <= metrics.clientWidth + 1,
    'history rail should fit within the mobile viewport',
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

    console.log('Teacher AI tools layout verification passed.');
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
