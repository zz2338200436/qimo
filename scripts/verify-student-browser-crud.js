const fs = require('fs');
const path = require('path');
const { chromium } = require(path.resolve(__dirname, '../major_assignment/node_modules/playwright'));

const repoRoot = path.resolve(__dirname, '..');
const teacherSessionFile = process.argv[2]
    ? path.resolve(process.argv[2])
    : path.resolve(repoRoot, '.runtime-logs/teacher-session-full-smoke.json');
const studentSessionFile = process.argv[3]
    ? path.resolve(process.argv[3])
    : path.resolve(repoRoot, '.runtime-logs/student-session-full-smoke.json');

function readJson(filePath) {
    return JSON.parse(fs.readFileSync(filePath, 'utf8').replace(/^\uFEFF/, ''));
}

function requireRole(session, expectedRole, label) {
    const roles = session.user?.roles || [];
    const activeRole = session.activeRole || session.user?.activeRole || session.sessionStorage?.activeRole || session.sessionStorage?.role;
    if (activeRole !== expectedRole && !roles.includes(expectedRole)) {
        throw new Error(`${label} must use a ${expectedRole} session file; got ${activeRole || roles.join(',') || 'unknown'}`);
    }
}

function sqlDateTime(date) {
    const pad = value => String(value).padStart(2, '0');
    return [
        date.getFullYear(),
        '-',
        pad(date.getMonth() + 1),
        '-',
        pad(date.getDate()),
        ' ',
        pad(date.getHours()),
        ':',
        pad(date.getMinutes()),
        ':',
        pad(date.getSeconds())
    ].join('');
}

function isoLocal(date) {
    return new Date(date.getTime() - date.getTimezoneOffset() * 60000)
        .toISOString()
        .replace(/\.\d{3}Z$/, '.000Z');
}

function assert(condition, message, details) {
    if (!condition) {
        const error = new Error(message);
        if (details !== undefined) {
            error.details = details;
        }
        throw error;
    }
}

function extractList(data, keys = []) {
    if (Array.isArray(data)) {
        return data;
    }
    if (!data || typeof data !== 'object') {
        return [];
    }
    if (Array.isArray(data.content)) {
        return data.content;
    }
    for (const key of keys) {
        if (Array.isArray(data[key])) {
            return data[key];
        }
    }
    return [];
}

async function createAuthedPage(context, storage, url) {
    const page = await context.newPage();
    await page.addInitScript(sessionStorageState => {
        Object.entries(sessionStorageState).forEach(([key, value]) => {
            if (value !== undefined && value !== null) {
                sessionStorage.setItem(key, String(value));
            }
        });
    }, storage);
    await page.goto(url, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(2000);
    return page;
}

async function api(page, method, url, body, expectedStatuses) {
    const result = await page.evaluate(async ({ method, url, body }) => {
        const options = { method, credentials: 'include' };
        if (body !== undefined) {
            options.headers = { 'Content-Type': 'application/json' };
            options.body = JSON.stringify(body);
        }
        const response = await window.fetch(url, options);
        const text = await response.text();
        let json = null;
        if (text) {
            try {
                json = JSON.parse(text);
            } catch (error) {
                return { ok: response.ok, status: response.status, text, json: null };
            }
        }
        return { ok: response.ok, status: response.status, text, json };
    }, { method, url, body });

    const acceptedStatuses = expectedStatuses || [200, 201, 204];
    if (!acceptedStatuses.includes(result.status)) {
        throw new Error(`${method} ${url} failed with ${result.status}: ${result.text.slice(0, 300)}`);
    }
    return result.json;
}

async function runStep(results, name, action) {
    try {
        await action();
        results.push({ name, ok: true });
        console.log(`[PASS] ${name}`);
    } catch (error) {
        results.push({ name, ok: false, error: error.message, details: error.details });
        console.error(`[FAIL] ${name}`);
        console.error(`  ${error.message}`);
        if (error.details !== undefined) {
            console.error(`  details: ${JSON.stringify(error.details)}`);
        }
        throw error;
    }
}

(async () => {
    const teacherSession = readJson(teacherSessionFile);
    const studentSession = readJson(studentSessionFile);
    requireRole(teacherSession, 'TEACHER', 'teacherSessionFile');
    requireRole(studentSession, 'STUDENT', 'studentSessionFile');
    const unique = `StudentBrowserCrud${Date.now()}`;
    const now = new Date();
    const futurePublish = new Date(now.getTime() + 3600 * 1000);
    const futureDue = new Date(now.getTime() + 5 * 24 * 3600 * 1000);
    const futureExamStart = new Date(now.getTime() + 2 * 3600 * 1000);
    const futureExamEnd = new Date(now.getTime() + 3 * 3600 * 1000);

    const created = {
        assignmentId: null,
        examId: null,
        notificationId: null
    };
    const results = [];

    const browser = await chromium.launch({ headless: true });
    const context = await browser.newContext();
    let teacherPage;
    let studentPage;

    async function cleanup() {
        if (created.notificationId && studentPage && !studentPage.isClosed()) {
            try {
                await api(studentPage, 'DELETE', `/api/notifications/${created.notificationId}?studentId=42`);
                console.log('[CLEANUP] delete notification');
            } catch (error) {
                console.warn(`[CLEANUP-WARN] delete notification: ${error.message}`);
            }
        }
        if (created.assignmentId && teacherPage && !teacherPage.isClosed()) {
            try {
                await api(teacherPage, 'DELETE', `/api/teacher/assignments/${created.assignmentId}`);
                console.log('[CLEANUP] delete assignment');
            } catch (error) {
                console.warn(`[CLEANUP-WARN] delete assignment: ${error.message}`);
            }
        }
        if (created.examId && teacherPage && !teacherPage.isClosed()) {
            try {
                await api(teacherPage, 'DELETE', `/api/teacher/exams/${created.examId}`);
                console.log('[CLEANUP] delete exam');
            } catch (error) {
                console.warn(`[CLEANUP-WARN] delete exam: ${error.message}`);
            }
        }
    }

    try {
        teacherPage = await createAuthedPage(
            context,
            teacherSession.sessionStorage || {},
            'http://localhost:5500/teacher-assignments.html'
        );
        studentPage = await createAuthedPage(
            context,
            studentSession.sessionStorage || {},
            'http://localhost:5500/student-dashboard.html'
        );

        await runStep(results, 'student courses page read detail', async () => {
            const courses = await api(studentPage, 'GET', '/api/student/courses?page=1&size=10');
            const course = extractList(courses.data)[0];
            assert(course?.id, 'student course list should contain a course', courses.data);

            const detail = await api(studentPage, 'GET', `/api/student/courses/${course.id}`);
            assert(detail.success === true, 'student course detail should succeed', detail);
        });

        await runStep(results, 'student assignments page submit and read graded result', async () => {
            const create = await api(teacherPage, 'POST', '/api/teacher/assignments', {
                title: `StudentAssign-${unique}`,
                description: 'student browser crud assignment',
                courseId: 2,
                publishDate: sqlDateTime(futurePublish),
                dueDate: sqlDateTime(futureDue),
                isActive: true,
                maxScore: 100,
                knowledgePointIds: []
            });
            created.assignmentId = create.data.id;
            assert(created.assignmentId, 'teacher should create assignment for student flow', create);

            const detail = await api(studentPage, 'GET', `/api/student/assignments/${created.assignmentId}`);
            assert(detail.success === true, 'student assignment detail should succeed', detail);

            const submit = await api(studentPage, 'POST', `/api/student/assignments/${created.assignmentId}/submit`, {
                content: `学生端浏览器提交 ${unique}`,
                submissionDate: sqlDateTime(now)
            });
            assert(submit.success === true, 'student assignment submit should succeed', submit);

            const submissions = await api(teacherPage, 'GET', `/api/teacher/assignments/${created.assignmentId}/submissions`);
            const submission = extractList(submissions.data)[0];
            assert(submission?.id, 'teacher should see student assignment submission', submissions.data);

            const grade = await api(teacherPage, 'PUT', `/api/teacher/submissions/${submission.id}/grade`, {
                score: 91,
                teacherComment: '学生端深测作业批改',
                graded: true
            });
            assert(grade.success === true, 'teacher should grade assignment submission', grade);

            const afterGrade = await api(studentPage, 'GET', `/api/student/assignments/${created.assignmentId}`);
            assert(afterGrade.data.submission, 'student should read own assignment submission', afterGrade.data);
        });

        await runStep(results, 'student exams page submit and read score', async () => {
            const create = await api(teacherPage, 'POST', '/api/teacher/exams', {
                title: `StudentExam-${unique}`,
                description: 'student browser crud exam',
                courseId: 2,
                startTime: isoLocal(futureExamStart),
                endTime: isoLocal(futureExamEnd),
                publishDate: isoLocal(futurePublish),
                isActive: true,
                isOnline: true,
                location: '线上',
                duration: 30,
                knowledgePointIds: [],
                questions: [
                    {
                        questionText: '3 + 3 = ?',
                        questionType: 'single_choice',
                        options: ['3', '5', '6', '8'],
                        correctAnswer: '6',
                        score: 10
                    }
                ]
            });
            created.examId = create.data.id;
            assert(created.examId, 'teacher should create exam for student flow', create);

            const detail = await api(studentPage, 'GET', `/api/student/exams/${created.examId}`);
            assert(detail.success === true, 'student exam detail should succeed', detail);

            const submit = await api(studentPage, 'POST', `/api/student/exams/${created.examId}/submit`, {
                timeTaken: 10,
                answers: { content: `学生端考试提交 ${unique}` }
            });
            assert(submit.success === true, 'student exam submit should succeed', submit);

            const submissions = await api(teacherPage, 'GET', `/api/teacher/exams/${created.examId}/submissions`);
            const submission = extractList(submissions.data)[0];
            assert(submission?.id, 'teacher should see student exam submission', submissions.data);

            const grade = await api(teacherPage, 'PUT', `/api/teacher/exams/grade/${submission.id}`, {
                score: 93,
                teacherComment: '学生端深测考试批改',
                graded: true
            });
            assert(grade.success === true, 'teacher should grade exam submission', grade);

            const scores = await api(studentPage, 'GET', '/api/student/scores');
            assert(extractList(scores.data).some(score => Number(score.score) === 93 || Number(score.examId) === created.examId), 'student scores should include graded exam or score value', scores.data);
        });

        await runStep(results, 'student notifications page read mark delete', async () => {
            const send = await api(teacherPage, 'POST', '/api/notifications/teacher/send?teacherId=7', {
                type: 'course',
                title: `学生通知-${unique}`,
                content: '学生端浏览器通知深测',
                studentId: 42,
                relatedId: 2,
                isRead: false
            });
            created.notificationId = send.data.id;
            assert(created.notificationId, 'teacher notification send should return id', send);

            const all = await api(studentPage, 'GET', '/api/notifications/student/all?studentId=42');
            assert(extractList(all.data).some(item => item.id === created.notificationId), 'student notification list should include sent notification', all.data);

            const read = await api(studentPage, 'PUT', `/api/notifications/${created.notificationId}/read?studentId=42`);
            assert(read.success === true, 'student mark notification read should succeed', read);

            const del = await api(studentPage, 'DELETE', `/api/notifications/${created.notificationId}?studentId=42`);
            assert(del.success === true, 'student delete notification should succeed', del);
            created.notificationId = null;
        });

        await runStep(results, 'student settings page profile notification privacy validation', async () => {
            const profile = await api(studentPage, 'GET', '/api/student/profile');
            assert(profile.data.studentId === 42, 'student profile should resolve to student42', profile.data);

            const updateProfile = await api(studentPage, 'PUT', '/api/student/profile', {
                name: profile.data.name,
                email: profile.data.email,
                phone: profile.data.phone,
                avatar: profile.data.avatar
            });
            assert(updateProfile.success === true, 'student profile update should succeed', updateProfile);

            const notificationSettings = await api(studentPage, 'GET', '/api/student/notification-settings');
            const updateNotification = await api(studentPage, 'PUT', '/api/student/notification-settings', notificationSettings.data || {});
            assert(updateNotification.success === true, 'student notification settings update should succeed', updateNotification);

            const privacySettings = await api(studentPage, 'GET', '/api/student/privacy-settings');
            const updatePrivacy = await api(studentPage, 'PUT', '/api/student/privacy-settings', privacySettings.data || {});
            assert(updatePrivacy.success === true, 'student privacy settings update should succeed', updatePrivacy);

            const weakPassword = await api(studentPage, 'POST', '/api/auth/change-password', {
                currentPassword: '',
                newPassword: 'short'
            }, [400]);
            assert(weakPassword.success === false, 'invalid student password change should return validation failure', weakPassword);
        });
    } finally {
        await cleanup();
        await browser.close();
    }

    console.log(`Student browser CRUD smoke passed: ${results.length}`);
})().catch(error => {
    console.error(error.stack || error.message);
    process.exitCode = 1;
});
