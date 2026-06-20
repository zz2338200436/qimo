const path = require('path');
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
  const page = await context.newPage({ viewport });
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
  await page.goto('http://localhost:5500/teacher-ai-tools.html', { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('.agent-panel.chatgpt-like .agent-panel-header');
  return page;
}

async function verifyDesktopLayout(page) {
  const toolbarCount = await page.locator('.teacher-ai-chat-toolbar').count();
  assert(toolbarCount === 0, 'history control should not occupy a standalone toolbar row', toolbarCount);

  const header = page.locator('.agent-panel.chatgpt-like .agent-panel-header');
  const actions = header.locator('.teacher-ai-panel-actions');
  const button = actions.locator('[data-agent-history-toggle]');
  const status = actions.locator('.agent-panel-status');

  assert(await actions.count() === 1, 'assistant header should expose one compact action group');
  assert(await button.count() === 1, 'history trigger should live inside the assistant header actions');
  assert(await status.count() === 1, 'service status should remain in the assistant header actions');

  const headerBox = await header.boundingBox();
  const buttonBox = await button.boundingBox();
  const statusBox = await status.boundingBox();
  assert(headerBox && buttonBox && statusBox, 'header, status, and history trigger should be visible');

  assert(buttonBox.y >= headerBox.y - 1, 'history trigger should be vertically contained by the assistant header', {
    headerBox,
    buttonBox
  });
  assert(buttonBox.y + buttonBox.height <= headerBox.y + headerBox.height + 1, 'history trigger should not spill below the assistant header', {
    headerBox,
    buttonBox
  });
  assert(statusBox.y >= headerBox.y - 1 && statusBox.y + statusBox.height <= headerBox.y + headerBox.height + 1,
    'service status should stay aligned inside the assistant header',
    { headerBox, statusBox });
}

async function verifyMobileLayout(page) {
  const metrics = await page.evaluate(() => ({
    clientWidth: document.documentElement.clientWidth,
    scrollWidth: document.documentElement.scrollWidth
  }));
  assert(metrics.scrollWidth <= metrics.clientWidth + 1, 'teacher AI tools page should not create horizontal scroll on mobile', metrics);

  const buttonBox = await page.locator('[data-agent-history-toggle]').boundingBox();
  assert(buttonBox, 'history trigger should remain visible on mobile');
  assert(buttonBox.x >= 0 && buttonBox.x + buttonBox.width <= metrics.clientWidth + 1,
    'history trigger should fit within the mobile viewport',
    { buttonBox, metrics });
}

(async () => {
  const browser = await chromium.launch({ headless: true });
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
