const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const context = await browser.newContext();
  const page = await context.newPage();

  try {
    // 1. 访问登录页面
    console.log('1. 访问登录页面...');
    await page.goto('http://localhost:8080/');
    await page.waitForTimeout(2000);
    
    // 截图登录页面
    await page.screenshot({ path: 'screenshots/01-login-page.png' });
    console.log('   截图已保存: screenshots/01-login-page.png');

    // 2. 检查页面元素
    console.log('2. 检查页面元素...');
    const title = await page.title();
    console.log('   页面标题: ' + title);
    
    // 检查是否有用户名输入框
    const usernameInput = await page.$('input[name="username"], input[type="text"], #username');
    console.log('   用户名输入框: ' + (usernameInput ? '存在' : '不存在'));
    
    // 检查是否有密码输入框
    const passwordInput = await page.$('input[name="password"], input[type="password"], #password');
    console.log('   密码输入框: ' + (passwordInput ? '存在' : '不存在'));
    
    // 检查是否有登录按钮
    const loginButton = await page.$('button[type="submit"], .login-btn, #loginBtn');
    console.log('   登录按钮: ' + (loginButton ? '存在' : '不存在'));

    // 3. 尝试登录（使用测试账号）
    console.log('3. 尝试登录...');
    if (usernameInput && passwordInput) {
      await usernameInput.fill('admin');
      await passwordInput.fill('admin123');
      
      // 截图填写后的登录页面
      await page.screenshot({ path: 'screenshots/02-login-filled.png' });
      console.log('   截图已保存: screenshots/02-login-filled.png');
      
      // 点击登录按钮
      if (loginButton) {
        await loginButton.click();
        await page.waitForTimeout(3000);
        
        // 截图登录后的页面
        await page.screenshot({ path: 'screenshots/03-after-login.png' });
        console.log('   截图已保存: screenshots/03-after-login.png');
        
        // 检查是否登录成功
        const currentUrl = page.url();
        console.log('   当前URL: ' + currentUrl);
      }
    }

    // 4. 访问课程管理页面
    console.log('4. 访问课程管理页面...');
    await page.goto('http://localhost:8080/#/teacher/courses');
    await page.waitForTimeout(3000);
    
    // 截图课程管理页面
    await page.screenshot({ path: 'screenshots/04-courses-page.png' });
    console.log('   截图已保存: screenshots/04-courses-page.png');

    // 5. 检查课程列表
    console.log('5. 检查课程列表...');
    const courseList = await page.$$('.course-item, .list-item, tr[data-id]');
    console.log('   课程数量: ' + courseList.length);

    // 6. 测试课程搜索功能
    console.log('6. 测试课程搜索功能...');
    const searchInput = await page.$('input[placeholder*="搜索"], input[type="search"], .search-input');
    if (searchInput) {
      await searchInput.fill('Java');
      await page.waitForTimeout(1000);
      await page.screenshot({ path: 'screenshots/05-search-result.png' });
      console.log('   截图已保存: screenshots/05-search-result.png');
    }

    // 7. 测试创建课程功能
    console.log('7. 测试创建课程功能...');
    const createButton = await page.$('button:has-text("创建"), button:has-text("新增"), .create-btn, .add-btn');
    if (createButton) {
      await createButton.click();
      await page.waitForTimeout(2000);
      await page.screenshot({ path: 'screenshots/06-create-course-dialog.png' });
      console.log('   截图已保存: screenshots/06-create-course-dialog.png');
    }

    console.log('\n测试完成！');
  } catch (error) {
    console.error('测试过程中发生错误:', error.message);
    await page.screenshot({ path: 'screenshots/error.png' });
  } finally {
    await browser.close();
  }
})();
