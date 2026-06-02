const fs = require('fs');
const path = require('path');
const { chromium } = require(path.resolve(__dirname, '../major_assignment/node_modules/playwright'));

const sessionFile = process.argv[2]
    ? path.resolve(process.argv[2])
    : path.resolve(__dirname, '../.runtime-logs/teacher-session-final-verify.json');

const session = JSON.parse(fs.readFileSync(sessionFile, 'utf8').replace(/^\uFEFF/, ''));
const sessionStorageState = session.sessionStorage || {};

function decodeJwtPayload(token) {
    if (!token || typeof token !== 'string') {
        return null;
    }
    const parts = token.split('.');
    if (parts.length < 2) {
        return null;
    }
    try {
        const normalized = parts[1].replace(/-/g, '+').replace(/_/g, '/');
        const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
        return JSON.parse(Buffer.from(padded, 'base64').toString('utf8'));
    } catch (error) {
        return null;
    }
}

function assertFreshSession(storageState) {
    const token = storageState.token;
    const payload = decodeJwtPayload(token);
    if (!payload || typeof payload.exp !== 'number') {
        return;
    }
    const nowSeconds = Math.floor(Date.now() / 1000);
    if (payload.exp <= nowSeconds + 30) {
        const expiredAt = new Date(payload.exp * 1000).toISOString();
        throw new Error(`teacher session token expired or near expiry: ${expiredAt}`);
    }
}

assertFreshSession(sessionStorageState);

const checks = [
    {
        name: 'teacher-dashboard',
        url: 'http://localhost:5500/teacher-dashboard.html',
        expectedText: '仪表盘',
        readyText: '教授课程',
        expectedApis: [
            '/api/teacher/courses',
            '/api/teacher/assignments',
            '/api/teacher/submissions'
        ]
    },
    {
        name: 'teacher-courses',
        url: 'http://localhost:5500/teacher-courses.html',
        expectedText: '课程与班级管理',
        expectedApis: [
            '/api/teacher/courses'
        ]
    },
    {
        name: 'teacher-assignments',
        url: 'http://localhost:5500/teacher-assignments.html',
        expectedText: '作业与考试发布',
        expectedApis: [
            '/api/teacher/assignments',
            '/api/teacher/courses'
        ]
    },
    {
        name: 'teacher-knowledge',
        url: 'http://localhost:5500/teacher-knowledge.html',
        expectedText: '知识点薄弱分析',
        expectedApis: [
            '/api/teacher/knowledge-points',
            '/api/knowledge-points/analysis/teacher/course'
        ]
    },
    {
        name: 'teacher-warning',
        url: 'http://localhost:5500/teacher-warning.html',
        expectedText: '学情预警',
        fallbackText: '当前本地联调环境未接入预警服务',
        expectedApis: [
            '/api/early-warnings/teacher/list',
            '/api/early-warnings/teacher/stats'
        ]
    },
    {
        name: 'teacher-student-dashboard',
        url: 'http://localhost:5500/teacher-student-dashboard.html',
        expectedText: '学生学习数据',
        fallbackText: '当前本地联调环境未接入该分析页面所需服务',
        expectedApis: [
            '/api/teacher/dashboard',
            '/api/teacher/learning-summary'
        ]
    },
    {
        name: 'teacher-notifications',
        url: 'http://localhost:5500/teacher-notifications.html',
        expectedText: '通知管理',
        expectedApis: []
    },
    {
        name: 'teacher-settings',
        url: 'http://localhost:5500/teacher-settings.html',
        expectedText: '设置',
        expectedApis: [
            '/api/users/me'
        ]
    },
    {
        name: 'teacher-ai-tools',
        url: 'http://localhost:5500/teacher-ai-tools.html',
        expectedText: 'AI工具',
        expectedApis: []
    }
];

const visibleErrorFragments = [
    '加载失败',
    '获取课程列表失败',
    '获取作业列表失败',
    '获取考试列表失败',
    '加载用户信息失败',
    '请先登录',
    '未登录',
    'Unauthorized',
    'Forbidden',
    'NetworkError',
    '当前本地联调环境未接入预警服务',
    '当前本地联调环境未接入该分析页面所需服务'
];

function collectMatchedResponses(responses, expectedApis) {
    return expectedApis.map(apiFragment => {
        const match = responses.find(item => item.url.includes(apiFragment));
        return {
            apiFragment,
            found: !!match,
            status: match ? match.status : null
        };
    });
}

(async () => {
    const browser = await chromium.launch({ headless: true });
    const context = await browser.newContext();
    await context.addInitScript(storage => {
        Object.entries(storage).forEach(([key, value]) => {
            if (value !== undefined && value !== null) {
                sessionStorage.setItem(key, String(value));
            }
        });
    }, sessionStorageState);

    let hasFailure = false;

    try {
        for (const check of checks) {
            const page = await context.newPage();
            const responses = [];
            const pageErrors = [];
            const consoleErrors = [];
            const failedResponses = [];
            let renderedText = '';

            page.on('pageerror', error => {
                pageErrors.push(error.message);
            });

            page.on('console', message => {
                if (message.type() === 'error') {
                    consoleErrors.push(message.text());
                }
            });

            page.on('response', response => {
                const url = response.url();
                const status = response.status();

                responses.push({
                    url,
                    status
                });

                if (
                    status >= 400 &&
                    (url.includes('/api/') || url.includes('.html') || url.includes('.js') || url.includes('.css'))
                ) {
                    failedResponses.push({ url, status });
                }
            });

            try {
                await page.goto(check.url, { waitUntil: 'domcontentloaded' });
                await page.waitForTimeout(5000);
                if (check.readyText) {
                    await page.waitForFunction(
                        expected => document.body && document.body.innerText.includes(expected),
                        check.readyText,
                        { timeout: 10000 }
                    );
                }
                renderedText = await page.locator('body').innerText();

                const matchedResponses = collectMatchedResponses(responses, check.expectedApis);
                const missingApi = matchedResponses.find(item => !item.found || item.status >= 400);
                const hasFallback = check.fallbackText ? renderedText.includes(check.fallbackText) : false;
                const missingText = check.expectedText ? !renderedText.includes(check.expectedText) : false;
                const visibleError = visibleErrorFragments.find(fragment => renderedText.includes(fragment));
                const unexpectedFailure =
                    missingText ||
                    hasFallback ||
                    missingApi ||
                    visibleError ||
                    failedResponses.length > 0 ||
                    pageErrors.length > 0 ||
                    consoleErrors.length > 0;

                if (unexpectedFailure) {
                    hasFailure = true;
                    console.error(`[FAIL] ${check.name}`);
                    if (missingText) {
                        console.error(`  expected text missing: ${check.expectedText}`);
                    }
                    if (hasFallback) {
                        console.error(`  fallback text rendered: ${check.fallbackText}`);
                    }
                    if (missingApi) {
                        console.error(`  api check failed: ${missingApi.apiFragment} (found=${missingApi.found}, status=${missingApi.status})`);
                    }
                    if (visibleError) {
                        console.error(`  visible error fragment rendered: ${visibleError}`);
                    }
                    if (failedResponses.length > 0) {
                        console.error(`  failed responses: ${failedResponses.map(item => `${item.status} ${item.url}`).join(' | ')}`);
                    }
                    if (pageErrors.length > 0) {
                        console.error(`  page errors: ${pageErrors.join(' | ')}`);
                    }
                    if (consoleErrors.length > 0) {
                        console.error(`  console errors: ${consoleErrors.join(' | ')}`);
                    }
                    console.error(`  rendered text: ${renderedText.slice(0, 1000)}`);
                } else {
                    console.log(`[PASS] ${check.name}`);
                }
            } catch (error) {
                hasFailure = true;
                console.error(`[FAIL] ${check.name}`);
                console.error(`  navigation/check error: ${error.message}`);
                if (pageErrors.length > 0) {
                    console.error(`  page errors: ${pageErrors.join(' | ')}`);
                }
                if (consoleErrors.length > 0) {
                    console.error(`  console errors: ${consoleErrors.join(' | ')}`);
                }
                if (renderedText) {
                    console.error(`  rendered text: ${renderedText.slice(0, 1000)}`);
                }
            } finally {
                await page.close().catch(() => {});
            }
        }
    } finally {
        await browser.close();
    }

    if (hasFailure) {
        process.exitCode = 1;
    }
})();
