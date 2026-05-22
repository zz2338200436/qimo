const fs = require('fs');
const path = require('path');
const { chromium } = require(path.resolve(__dirname, '../major_assignment/node_modules/playwright'));

const sessionFile = process.argv[2]
    ? path.resolve(process.argv[2])
    : path.resolve(__dirname, '../.runtime-logs/student-session-full-smoke.json');

const session = JSON.parse(fs.readFileSync(sessionFile, 'utf8').replace(/^\uFEFF/, ''));
const sessionStorageState = session.sessionStorage || {};

const checks = [
    {
        name: 'student-dashboard',
        url: 'http://localhost:5500/student-dashboard.html',
        expectedApis: [
            '/api/student/courses',
            '/api/student/assignments',
            '/api/student/exams'
        ],
        expectedText: [
            '学生控制面板',
            '我的课程',
            '最近活动'
        ],
        toleratedInfoText: [
            '当前 JWT 微服务环境暂未提供学生综合表现接口'
        ]
    },
    {
        name: 'student-courses',
        url: 'http://localhost:5500/student-courses.html',
        expectedApis: [
            '/api/student/courses'
        ],
        expectedText: [
            '我的课程',
            '课程列表'
        ]
    },
    {
        name: 'student-assignments',
        url: 'http://localhost:5500/student-assignments.html',
        expectedApis: [
            '/api/student/assignments'
        ],
        expectedText: [
            '作业与考试',
            '作业列表'
        ]
    },
    {
        name: 'student-notifications',
        url: 'http://localhost:5500/student-notifications.html',
        expectedApis: [
            '/api/notifications/student',
            '/api/notifications/student/unread-count'
        ],
        expectedText: [
            '通知中心'
        ]
    },
    {
        name: 'student-settings',
        url: 'http://localhost:5500/student-settings.html',
        expectedApis: [
            '/api/student/profile',
            '/api/student/notification-settings',
            '/api/student/privacy-settings'
        ],
        expectedText: [
            '系统设置',
            '基本信息',
            '通知偏好'
        ]
    }
];

const genericErrorFragments = [
    '获取课程列表失败',
    '获取作业数据失败',
    '获取通知列表失败',
    '页面初始化失败',
    '加载学生信息失败',
    '加载通知设置失败',
    '加载隐私设置失败',
    '用户未登录'
];

function collectMatchedResponses(responses, expectedApis) {
    return expectedApis.map(apiFragment => {
        const matches = responses.filter(item => item.url.includes(apiFragment));
        const success = matches.some(item => item.status < 400);
        return {
            apiFragment,
            found: matches.length > 0,
            success,
            matches
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
            const consoleErrors = [];
            page.on('response', response => {
                responses.push({
                    url: response.url(),
                    status: response.status()
                });
            });
            page.on('console', msg => {
                if (msg.type() === 'error') {
                    consoleErrors.push(msg.text());
                }
            });

            await page.goto(check.url, { waitUntil: 'domcontentloaded' });
            await page.waitForTimeout(3500);

            const renderedText = await page.locator('body').innerText();
            const matchedResponses = collectMatchedResponses(responses, check.expectedApis);
            const missingApi = matchedResponses.find(item => !item.found || !item.success);
            const missingText = (check.expectedText || []).find(text => !renderedText.includes(text));
            const genericError = genericErrorFragments.find(fragment => renderedText.includes(fragment));

            if (missingApi || missingText || genericError) {
                hasFailure = true;
                console.error(`[FAIL] ${check.name}`);
                if (missingApi) {
                    console.error(`  api check failed: ${missingApi.apiFragment}`);
                    console.error(`  matches: ${JSON.stringify(missingApi.matches)}`);
                }
                if (missingText) {
                    console.error(`  missing text: ${missingText}`);
                }
                if (genericError) {
                    console.error(`  generic error text: ${genericError}`);
                }
                if (consoleErrors.length) {
                    console.error(`  console errors: ${JSON.stringify(consoleErrors)}`);
                }
            } else {
                console.log(`[PASS] ${check.name}`);
            }

            await page.close();
        }
    } finally {
        await browser.close();
    }

    if (hasFailure) {
        process.exitCode = 1;
    }
})();
