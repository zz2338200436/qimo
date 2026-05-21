const fs = require('fs');
const path = require('path');
const { chromium } = require(path.resolve(__dirname, '../major_assignment/node_modules/playwright'));

const sessionFile = process.argv[2]
    ? path.resolve(process.argv[2])
    : path.resolve(__dirname, '../.runtime-logs/teacher-session-final-verify.json');

const session = JSON.parse(fs.readFileSync(sessionFile, 'utf8'));
const sessionStorageState = session.sessionStorage || {};

const checks = [
    {
        name: 'teacher-warning',
        url: 'http://localhost:5500/teacher-warning.html',
        fallbackText: '当前本地联调环境未接入预警服务',
        expectedApis: [
            '/api/early-warnings/teacher/list',
            '/api/early-warnings/teacher/stats'
        ],
        readText: async page => page.locator('.warning-list').innerText()
    },
    {
        name: 'teacher-student-dashboard',
        url: 'http://localhost:5500/teacher-student-dashboard.html',
        fallbackText: '当前本地联调环境未接入该分析页面所需服务',
        expectedApis: [
            '/api/teacher/dashboard',
            '/api/teacher/learning-summary'
        ],
        readText: async page => page.locator('#studentTableBody').innerText()
    }
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
            page.on('response', response => {
                responses.push({
                    url: response.url(),
                    status: response.status()
                });
            });

            await page.goto(check.url, { waitUntil: 'domcontentloaded' });
            await page.waitForTimeout(3000);

            const renderedText = await check.readText(page);
            const matchedResponses = collectMatchedResponses(responses, check.expectedApis);
            const missingApi = matchedResponses.find(item => !item.found || item.status >= 400);
            const hasFallback = renderedText.includes(check.fallbackText);

            if (hasFallback || missingApi) {
                hasFailure = true;
                console.error(`[FAIL] ${check.name}`);
                if (hasFallback) {
                    console.error(`  fallback text rendered: ${check.fallbackText}`);
                }
                if (missingApi) {
                    console.error(`  api check failed: ${missingApi.apiFragment} (found=${missingApi.found}, status=${missingApi.status})`);
                }
                console.error(`  rendered text: ${renderedText}`);
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
