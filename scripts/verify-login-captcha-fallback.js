const { chromium } = require('../major_assignment/node_modules/playwright');

const baseUrl = process.env.FRONTEND_BASE_URL || 'http://127.0.0.1:5500';

async function verifyPage(browser, pageName) {
  const page = await browser.newPage({ viewport: { width: 555, height: 744 } });
  await page.route('**/api/auth/captcha**', (route) => route.abort('failed'));

  await page.goto(`${baseUrl}/${pageName}`, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(1200);

  const result = await page.evaluate(() => {
    const loginMessage = document.querySelector('#loginMessage');
    const captchaBox = document.querySelector('.captcha-image');
    const captchaImg = document.querySelector('#captchaImage');
    const fallback = document.querySelector('.captcha-fallback');

    const isVisible = (element) => {
      if (!element) return false;
      const style = getComputedStyle(element);
      const rect = element.getBoundingClientRect();
      return style.display !== 'none' && style.visibility !== 'hidden' && rect.width > 0 && rect.height > 0;
    };

    return {
      alertText: loginMessage?.innerText.trim() || '',
      captchaBoxClass: captchaBox?.className || '',
      fallbackText: fallback?.innerText.trim() || '',
      fallbackVisible: isVisible(fallback),
      imageVisible: isVisible(captchaImg),
      imageSrc: captchaImg?.getAttribute('src') || '',
      scrollWidth: document.documentElement.scrollWidth,
      clientWidth: document.documentElement.clientWidth,
    };
  });

  await page.close();

  if (result.alertText.includes('请求失败')) {
    throw new Error(`${pageName} should not show a top-level request failure when captcha is unavailable.`);
  }
  if (!result.fallbackVisible) {
    throw new Error(`${pageName} should show an inline captcha fallback.`);
  }
  if (!result.fallbackText.includes('验证码服务未连接')) {
    throw new Error(`${pageName} fallback should explain the captcha service is unavailable.`);
  }
  if (result.imageVisible || result.imageSrc) {
    throw new Error(`${pageName} should hide the broken captcha image when captcha loading fails.`);
  }
  if (result.scrollWidth > result.clientWidth + 1) {
    throw new Error(`${pageName} should not horizontally overflow at narrow width.`);
  }
}

function verifyMarkup(pageName) {
  const fs = require('fs');
  const content = fs.readFileSync(`frontend/dist/${pageName}`, 'utf8');
  if (content.includes('id="captchaImage" src="/api/auth/captcha"')) {
    throw new Error(`${pageName} should not ship an eager captcha image src before JavaScript can handle fallback.`);
  }
  if (!content.includes('AbortController')) {
    throw new Error(`${pageName} should timeout captcha fetches quickly when the gateway is unavailable.`);
  }
}

(async () => {
  const browser = await chromium.launch({ headless: true });
  try {
    verifyMarkup('teacher-login.html');
    verifyMarkup('student-login.html');
    await verifyPage(browser, 'teacher-login.html');
    await verifyPage(browser, 'student-login.html');
    console.log('Login captcha fallback verification passed.');
  } finally {
    await browser.close();
  }
})().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
