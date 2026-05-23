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
    const unique = `BrowserCrud${Date.now()}`;
    const now = new Date();
    const futureCourseStart = new Date(now.getTime() + 24 * 3600 * 1000);
    const futureCourseEnd = new Date(now.getTime() + 60 * 24 * 3600 * 1000);
    const futurePublish = new Date(now.getTime() + 3600 * 1000);
    const futureDue = new Date(now.getTime() + 5 * 24 * 3600 * 1000);
    const futureExamStart = new Date(now.getTime() + 2 * 3600 * 1000);
    const futureExamEnd = new Date(now.getTime() + 3 * 3600 * 1000);

    const created = {
        courseId: null,
        classId: null,
        courseAssignmentId: null,
        knowledgePointId: null,
        assignmentId: null,
        examId: null,
        notificationId: null,
        batchNotificationIds: []
    };
    const results = [];

    const browser = await chromium.launch({ headless: true });
    const context = await browser.newContext();
    let teacherPage;
    let studentPage;

    async function cleanup() {
        if (!teacherPage || teacherPage.isClosed()) {
            return;
        }
        const attempts = [];
        if (created.notificationId && studentPage && !studentPage.isClosed()) {
            attempts.push(['delete notification', () => api(studentPage, 'DELETE', `/api/notifications/${created.notificationId}?studentId=42`)]);
        }
        for (const notificationId of created.batchNotificationIds) {
            if (studentPage && !studentPage.isClosed()) {
                attempts.push([`delete batch notification ${notificationId}`, () => api(studentPage, 'DELETE', `/api/notifications/${notificationId}?studentId=42`)]);
            }
        }
        if (created.assignmentId) {
            attempts.push(['delete assignment', () => api(teacherPage, 'DELETE', `/api/teacher/assignments/${created.assignmentId}`)]);
        }
        if (created.examId) {
            attempts.push(['delete exam', () => api(teacherPage, 'DELETE', `/api/teacher/exams/${created.examId}`)]);
        }
        if (created.knowledgePointId) {
            attempts.push(['delete knowledge point', () => api(teacherPage, 'DELETE', `/api/teacher/knowledge-points/${created.knowledgePointId}`)]);
        }
        if (created.courseAssignmentId) {
            attempts.push(['delete course assignment', () => api(teacherPage, 'DELETE', `/api/teacher/course-assignments/${created.courseAssignmentId}`)]);
        }
        if (created.classId) {
            attempts.push(['delete class', () => api(teacherPage, 'DELETE', `/api/teacher/classes/${created.classId}`)]);
        }
        if (created.courseId) {
            attempts.push(['delete course', () => api(teacherPage, 'DELETE', `/api/teacher/courses/${created.courseId}`)]);
        }

        for (const [label, action] of attempts) {
            try {
                await action();
                console.log(`[CLEANUP] ${label}`);
            } catch (error) {
                console.warn(`[CLEANUP-WARN] ${label}: ${error.message}`);
            }
        }
    }

    try {
        teacherPage = await createAuthedPage(
            context,
            teacherSession.sessionStorage || {},
            'http://localhost:5500/teacher-courses.html'
        );
        studentPage = await createAuthedPage(
            context,
            studentSession.sessionStorage || {},
            'http://localhost:5500/student-dashboard.html'
        );

        await runStep(results, 'teacher courses page CRUD: course/class/assignment', async () => {
            const createCourse = await api(teacherPage, 'POST', '/api/teacher/courses', {
                courseName: `Course-${unique}`,
                courseCode: `BC${String(Date.now()).slice(-8)}`,
                description: 'browser crud smoke course',
                credit: 3,
                courseCategory: 'practice',
                totalHours: 32,
                courseStatus: 'active',
                semester: '2026-Spring',
                startDate: sqlDateTime(futureCourseStart).slice(0, 10),
                endDate: sqlDateTime(futureCourseEnd).slice(0, 10),
                maxStudents: 30
            });
            created.courseId = createCourse.data.id;
            assert(created.courseId, 'course create should return id', createCourse);

            const detail = await api(teacherPage, 'GET', `/api/teacher/courses/${created.courseId}`);
            assert(detail.data.courseName === `Course-${unique}`, 'created course detail should match', detail.data);

            const update = await api(teacherPage, 'PUT', `/api/teacher/courses/${created.courseId}`, {
                courseName: `Course-${unique}-Updated`,
                courseCode: `BU${String(Date.now()).slice(-8)}`,
                description: 'browser crud smoke course updated',
                credit: 4,
                courseCategory: 'practice',
                totalHours: 48,
                courseStatus: 'active',
                semester: '2026-Spring',
                startDate: sqlDateTime(futureCourseStart).slice(0, 10),
                endDate: sqlDateTime(futureCourseEnd).slice(0, 10),
                maxStudents: 35
            });
            assert(update.success === true, 'course update should succeed', update);

            const majors = await api(teacherPage, 'GET', '/api/teacher/majors');
            const majorId = extractList(majors.data)[0]?.id;
            assert(majorId, 'major id is required for class create', majors.data);

            const createClass = await api(teacherPage, 'POST', '/api/teacher/classes', {
                className: `Class-${unique}`,
                year: '2026',
                capacity: 35,
                courseId: created.courseId,
                majorId,
                classTime: '周一 08:00-10:00',
                classLocation: 'A101'
            });
            created.classId = createClass.data.id;
            assert(created.classId, 'class create should return id', createClass);

            const classUpdate = await api(teacherPage, 'PUT', `/api/teacher/classes/${created.classId}`, {
                className: `Class-${unique}-Updated`,
                year: '2026',
                capacity: 40,
                courseId: created.courseId,
                majorId,
                classTime: '周二 10:00-12:00',
                classLocation: 'A102'
            });
            assert(classUpdate.success === true, 'class update should succeed', classUpdate);

            const assign = await api(teacherPage, 'POST', '/api/teacher/course-assignments', {
                classId: created.classId,
                courseId: created.courseId,
                classTime: '周二 10:00-12:00',
                classLocation: 'A102'
            });
            assert(assign.success === true, 'course assignment should succeed', assign);

            const assignmentList = await api(teacherPage, 'GET', `/api/teacher/course-assignments?page=1&size=20&courseId=${created.courseId}`);
            const row = extractList(assignmentList.data).find(item => item.courseId === created.courseId && item.classId === created.classId);
            assert(row, 'course assignment should be queryable', assignmentList.data);
            created.courseAssignmentId = row.id || row.assignmentId;
        });

        await runStep(results, 'teacher knowledge page CRUD', async () => {
            const knowledgePointName = `KP-${unique}-Updated`;
            const create = await api(teacherPage, 'POST', '/api/teacher/knowledge-points', {
                pointName: `KP-${unique}`,
                description: 'browser crud smoke knowledge point',
                difficulty: '中等',
                orderIndex: 1,
                courseId: created.courseId
            });
            created.knowledgePointId = create.data.id;
            assert(created.knowledgePointId, 'knowledge point create should return id', create);

            const update = await api(teacherPage, 'PUT', `/api/teacher/knowledge-points/${created.knowledgePointId}`, {
                pointName: `KP-${unique}-Updated`,
                description: 'browser crud smoke knowledge point updated',
                difficulty: '困难',
                orderIndex: 2,
                courseId: created.courseId
            });
            assert(update.success === true, 'knowledge point update should succeed', update);

            const detail = await api(teacherPage, 'GET', `/api/teacher/knowledge-points/${created.knowledgePointId}`);
            assert(detail.data.pointName.endsWith('-Updated'), 'knowledge point detail should reflect update', detail.data);

            await teacherPage.evaluate(() => localStorage.removeItem('knowledgePageFilters'));
            await teacherPage.goto('http://localhost:5500/teacher-knowledge.html', { waitUntil: 'domcontentloaded' });
            await teacherPage.waitForFunction(courseId => {
                const select = document.getElementById('course-select');
                return !!select && Array.from(select.options).some(option => option.value === String(courseId));
            }, created.courseId);
            await teacherPage.selectOption('#course-select', String(created.courseId));
            await teacherPage.click('#queryBtn');
            await teacherPage.waitForFunction(expectedTitle => {
                return Array.from(document.querySelectorAll('.knowledge-card-title'))
                    .some(node => (node.textContent || '').includes(expectedTitle));
            }, knowledgePointName, { timeout: 10000 });
        });

        await runStep(results, 'teacher assignments page CRUD: assignment/exam/grade', async () => {
            const createAssignment = await api(teacherPage, 'POST', '/api/teacher/assignments', {
                title: `Assign-${unique}`,
                description: 'browser crud smoke assignment',
                courseId: 2,
                publishDate: sqlDateTime(futurePublish),
                dueDate: sqlDateTime(futureDue),
                isActive: true,
                maxScore: 100,
                knowledgePointIds: []
            });
            created.assignmentId = createAssignment.data.id;
            assert(created.assignmentId, 'assignment create should return id', createAssignment);

            const assignmentUpdate = await api(teacherPage, 'PUT', `/api/teacher/assignments/${created.assignmentId}`, {
                title: `Assign-${unique}-Updated`,
                description: 'browser crud smoke assignment updated',
                courseId: 2,
                publishDate: sqlDateTime(futurePublish),
                dueDate: sqlDateTime(new Date(futureDue.getTime() + 3600 * 1000)),
                isActive: true,
                maxScore: 100,
                knowledgePointIds: []
            });
            assert(assignmentUpdate.success === true, 'assignment update should succeed', assignmentUpdate);

            const assignmentSubmit = await api(studentPage, 'POST', `/api/student/assignments/${created.assignmentId}/submit`, {
                content: `student assignment submission ${unique}`,
                submissionDate: sqlDateTime(now)
            });
            assert(assignmentSubmit.success === true, 'student assignment submit should succeed', assignmentSubmit);

            const assignmentSubmissions = await api(teacherPage, 'GET', `/api/teacher/assignments/${created.assignmentId}/submissions`);
            const submission = extractList(assignmentSubmissions.data)[0];
            assert(submission?.id, 'teacher should see assignment submission', assignmentSubmissions.data);

            const grade = await api(teacherPage, 'PUT', `/api/teacher/submissions/${submission.id}/grade`, {
                score: 88,
                teacherComment: '浏览器深测批改',
                graded: true
            });
            assert(grade.success === true, 'assignment grade should succeed', grade);

            const createExam = await api(teacherPage, 'POST', '/api/teacher/exams', {
                title: `Exam-${unique}`,
                description: 'browser crud smoke exam',
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
                        questionText: '1 + 1 = ?',
                        questionType: 'single_choice',
                        options: ['1', '2', '3', '4'],
                        correctAnswer: '2',
                        score: 10
                    }
                ]
            });
            created.examId = createExam.data.id;
            assert(created.examId, 'exam create should return id', createExam);

            const examUpdate = await api(teacherPage, 'PUT', `/api/teacher/exams/${created.examId}`, {
                title: `Exam-${unique}-Updated`,
                description: 'browser crud smoke exam updated',
                courseId: 2,
                startTime: isoLocal(futureExamStart),
                endTime: isoLocal(new Date(futureExamEnd.getTime() + 1800 * 1000)),
                publishDate: isoLocal(futurePublish),
                isActive: true,
                isOnline: true,
                location: '线上',
                duration: 45,
                knowledgePointIds: [],
                questions: [
                    {
                        questionText: '2 + 2 = ?',
                        questionType: 'single_choice',
                        options: ['2', '3', '4', '5'],
                        correctAnswer: '4',
                        score: 20
                    }
                ]
            });
            assert(examUpdate.success === true, 'exam update should succeed', examUpdate);

            const examSubmit = await api(studentPage, 'POST', `/api/student/exams/${created.examId}/submit`, {
                timeTaken: 12,
                answers: { content: `student exam submission ${unique}` }
            });
            assert(examSubmit.success === true, 'student exam submit should succeed', examSubmit);

            const examSubmissions = await api(teacherPage, 'GET', `/api/teacher/exams/${created.examId}/submissions`);
            const examSubmission = extractList(examSubmissions.data)[0];
            assert(examSubmission?.id, 'teacher should see exam submission', examSubmissions.data);

            const examGrade = await api(teacherPage, 'PUT', `/api/teacher/exams/grade/${examSubmission.id}`, {
                score: 96,
                teacherComment: '浏览器深测考试批改',
                graded: true
            });
            assert(examGrade.success === true, 'exam grade should succeed', examGrade);
        });

        await runStep(results, 'teacher notifications page send and student read', async () => {
            const send = await api(teacherPage, 'POST', '/api/notifications/teacher/send?teacherId=7', {
                type: 'course',
                title: `通知-${unique}`,
                content: '浏览器深测通知',
                studentId: 42,
                relatedId: 2,
                isRead: false
            });
            created.notificationId = send.data.id;
            assert(created.notificationId, 'notification create should return id', send);

            const all = await api(studentPage, 'GET', '/api/notifications/student/all?studentId=42');
            assert(extractList(all.data).some(item => item.id === created.notificationId), 'student should see created notification', all.data);

            const markRead = await api(studentPage, 'PUT', `/api/notifications/${created.notificationId}/read?studentId=42`);
            assert(markRead.success === true, 'student mark notification read should succeed', markRead);
        });

        await runStep(results, 'teacher settings page profile and notification settings', async () => {
            const me = await api(teacherPage, 'GET', '/api/users/me');
            assert(me.data.id === 7, 'teacher /api/users/me should return teacher7', me.data);

            const update = await api(teacherPage, 'PUT', '/api/users/7', {
                name: me.data.name,
                email: me.data.email,
                phone: me.data.phone
            });
            assert(update.success === true, 'teacher profile update should succeed', update);

            const notificationSettings = await api(teacherPage, 'PUT', '/api/auth/notification-settings', {
                emailNotifications: true,
                assignmentNotifications: true
            });
            assert(notificationSettings.success === true, 'teacher notification settings should save', notificationSettings);

            const weakPassword = await api(teacherPage, 'POST', '/api/auth/change-password', {
                currentPassword: '',
                newPassword: 'short'
            }, [400]);
            assert(weakPassword.success === false, 'invalid password change should return validation failure', weakPassword);
        });
    } finally {
        await cleanup();
        await browser.close();
    }

    console.log(`Teacher browser CRUD smoke passed: ${results.length}`);
})().catch(error => {
    console.error(error.stack || error.message);
    process.exitCode = 1;
});
