const fs = require('fs');
const path = require('path');
const { chromium } = require(path.resolve(__dirname, '../major_assignment/node_modules/playwright'));

const sessionFile = process.argv[2]
    ? path.resolve(process.argv[2])
    : path.resolve(__dirname, '../.runtime-logs/student-session-full-smoke.json');

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
        throw new Error(`student session token expired or near expiry: ${expiredAt}`);
    }
}

assertFreshSession(sessionStorageState);

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
        ],
        forbiddenText: [
            '暂无活动记录'
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
            '/api/student/assignments',
            '/api/student/exams',
            '/api/student/scores'
        ],
        expectedText: [
            '作业与考试',
            '考试',
            '成绩查询'
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
            '/api/student/profile'
        ],
        expectedText: [
            '系统设置',
            '基本信息',
            '姓名、邮箱、手机号会同步到账号资料；专业、年级以当前浏览器补充信息为准。',
            '班级由系统班级关系自动生成，当前页不可手动修改。',
            '通知偏好',
            '当前环境下通知偏好以本浏览器保存为准。',
            '当前环境下隐私设置以本浏览器保存为准。'
        ],
        forbiddenText: [
            '当前 JWT 微服务环境暂未提供头像上传接口'
        ]
    },
    {
        name: 'student-stats',
        url: 'http://localhost:5500/student-stats.html',
        expectedApis: [
            '/api/student/courses',
            '/api/student/assignments',
            '/api/student/knowledge-points',
            '/api/student/study-time-distribution',
            '/api/student/stats',
            '/api/student/scores'
        ],
        expectedText: [
            '学习数据',
            '学习时长',
            '成绩趋势'
        ],
        toleratedInfoText: [
            '当前 JWT 微服务环境仅展示已接通的学习数据。'
        ],
        forbiddenText: [
            '当前 JWT 微服务环境暂未接通知识点统计接口。'
        ]
    },
    {
        name: 'student-ai-assistant',
        url: 'http://localhost:5500/student-ai-assistant.html',
        expectedApis: [],
        expectedText: [
            'AI学习助手',
            'AI学习建议',
            '自由问答暂不可用'
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

async function primeStudentPageExpectedTabs(page, check) {
    if (check.name !== 'student-assignments') {
        return;
    }

    await page.click('.tab-btn[data-tab="exams"]');
    await page.waitForFunction(() => {
        const tab = document.getElementById('exams');
        return tab && tab.classList.contains('active');
    }, { timeout: 10000 });
    await page.waitForTimeout(2000);

    await page.click('.tab-btn[data-tab="scores"]');
    await page.waitForFunction(() => {
        const tab = document.getElementById('scores');
        return tab && tab.classList.contains('active');
    }, { timeout: 10000 });
    await page.waitForTimeout(2000);
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
            await primeStudentPageExpectedTabs(page, check);

            const renderedText = await page.locator('body').innerText();
            const matchedResponses = collectMatchedResponses(responses, check.expectedApis);
            const missingApi = matchedResponses.find(item => !item.found || !item.success);
            const missingText = (check.expectedText || []).find(text => !renderedText.includes(text));
            const forbiddenText = (check.forbiddenText || []).find(text => renderedText.includes(text));
            const genericError = genericErrorFragments.find(fragment => renderedText.includes(fragment));

            if (missingApi || missingText || forbiddenText || genericError) {
                hasFailure = true;
                console.error(`[FAIL] ${check.name}`);
                if (missingApi) {
                    console.error(`  api check failed: ${missingApi.apiFragment}`);
                    console.error(`  matches: ${JSON.stringify(missingApi.matches)}`);
                }
                if (missingText) {
                    console.error(`  missing text: ${missingText}`);
                }
                if (forbiddenText) {
                    console.error(`  forbidden text present: ${forbiddenText}`);
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
