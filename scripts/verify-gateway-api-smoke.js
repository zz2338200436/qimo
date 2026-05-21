const fs = require('fs');
const path = require('path');

const repoRoot = path.resolve(__dirname, '..');
const defaultTeacherSession = path.resolve(repoRoot, '.runtime-logs/teacher-session-full-smoke.json');
const defaultStudentSession = path.resolve(repoRoot, '.runtime-logs/student-session-full-smoke.json');

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf8').replace(/^\uFEFF/, ''));
}

function isoLocal(date) {
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000)
    .toISOString()
    .replace(/\.\d{3}Z$/, '.000Z');
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

function assert(condition, message, details) {
  if (!condition) {
    const error = new Error(message);
    if (details !== undefined) {
      error.details = details;
    }
    throw error;
  }
}

async function requestJson(method, url, token, body, extraHeaders) {
  const headers = {
    Authorization: `Bearer ${token}`
  };
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  Object.assign(headers, extraHeaders || {});
  const response = await fetch(url, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  const text = await response.text();
  let json = null;
  if (text) {
    try {
      json = JSON.parse(text);
    } catch (error) {
      throw new Error(`${method} ${url} returned non-JSON body: ${text.slice(0, 200)}`);
    }
  }
  if (!response.ok) {
    throw new Error(`${method} ${url} failed with ${response.status}: ${text.slice(0, 300)}`);
  }
  return { response, json, text };
}

async function requestJsonAllowStatus(method, url, token, body, expectedStatuses, extraHeaders) {
  const headers = {};
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  Object.assign(headers, extraHeaders || {});
  const response = await fetch(url, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  const text = await response.text();
  let json = null;
  if (text) {
    try {
      json = JSON.parse(text);
    } catch (error) {
      throw new Error(`${method} ${url} returned non-JSON body: ${text.slice(0, 200)}`);
    }
  }
  if (!expectedStatuses.includes(response.status)) {
    throw new Error(`${method} ${url} expected ${expectedStatuses.join('/')} but got ${response.status}: ${text.slice(0, 300)}`);
  }
  return { response, json, text };
}

async function requestBinary(url, token) {
  const response = await fetch(url, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
  const buffer = Buffer.from(await response.arrayBuffer());
  if (!response.ok) {
    throw new Error(`GET ${url} failed with ${response.status}: ${buffer.toString('utf8', 0, Math.min(buffer.length, 200))}`);
  }
  return { response, buffer };
}

async function retry(action, attempts, delayMs) {
  let lastError = null;
  for (let index = 0; index < attempts; index += 1) {
    try {
      return await action();
    } catch (error) {
      lastError = error;
      if (index === attempts - 1) {
        break;
      }
      await new Promise(resolve => setTimeout(resolve, delayMs));
    }
  }
  throw lastError;
}

async function run() {
  const teacherSession = readJson(process.argv[2] ? path.resolve(process.argv[2]) : defaultTeacherSession);
  const studentSession = readJson(process.argv[3] ? path.resolve(process.argv[3]) : defaultStudentSession);
  const baseUrl = (teacherSession.baseUrl || 'http://localhost:8080').replace(/\/$/, '');
  const teacherToken = teacherSession.accessToken;
  const studentToken = studentSession.accessToken;

  const now = new Date();
  const unique = `ApiSmoke-${Date.now()}`;
  const futureCourseStart = new Date(now.getTime() + 24 * 3600 * 1000);
  const futureCourseEnd = new Date(now.getTime() + 60 * 24 * 3600 * 1000);
  const futurePublish = new Date(now.getTime() + 3600 * 1000);
  const futureDue = new Date(now.getTime() + 5 * 24 * 3600 * 1000);
  const futureExamStart = new Date(now.getTime() + 2 * 3600 * 1000);
  const futureExamEnd = new Date(now.getTime() + 3 * 3600 * 1000);
  const pastSubmission = new Date(now.getTime() - 3600 * 1000);

  const created = {
    courseId: null,
    classId: null,
    assignmentId: null,
    examId: null,
    warningId: null,
    notificationId: null,
    batchNotificationIds: []
  };

  const results = [];

  async function step(name, action) {
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

  async function cleanup() {
    const cleanupErrors = [];

    async function attempt(name, fn) {
      try {
        await fn();
        console.log(`[CLEANUP] ${name}`);
      } catch (error) {
        cleanupErrors.push(`${name}: ${error.message}`);
      }
    }

    if (created.warningId) {
      await attempt(`delete warning ${created.warningId}`, async () => {
        await requestJson('DELETE', `${baseUrl}/api/early-warnings/teacher/${created.warningId}`, teacherToken);
      });
    }
    if (created.notificationId) {
      await attempt(`delete notification ${created.notificationId}`, async () => {
        await requestJson('DELETE', `${baseUrl}/api/notifications/${created.notificationId}`, studentToken);
      });
    }
    for (const notificationId of created.batchNotificationIds) {
      await attempt(`delete batch notification ${notificationId}`, async () => {
        await requestJson('DELETE', `${baseUrl}/api/notifications/${notificationId}`, studentToken);
      });
    }
    if (created.assignmentId) {
      await attempt(`delete assignment ${created.assignmentId}`, async () => {
        await requestJson('DELETE', `${baseUrl}/api/teacher/assignments/${created.assignmentId}`, teacherToken);
      });
    }
    if (created.examId) {
      await attempt(`delete exam ${created.examId}`, async () => {
        await requestJson('DELETE', `${baseUrl}/api/teacher/exams/${created.examId}`, teacherToken);
      });
    }
    if (created.classId) {
      await attempt(`delete class ${created.classId}`, async () => {
        await requestJson('DELETE', `${baseUrl}/api/teacher/classes/${created.classId}`, teacherToken);
      });
    }
    if (created.courseId) {
      await attempt(`delete course ${created.courseId}`, async () => {
        await requestJson('DELETE', `${baseUrl}/api/teacher/courses/${created.courseId}`, teacherToken);
      });
    }

    if (cleanupErrors.length > 0) {
      throw new Error(`Cleanup failed: ${cleanupErrors.join('; ')}`);
    }
  }

  try {
    let teacherCourses;
    let studentCourses;
    let teacherAssignments;
    let studentAssignments;
    let teacherExams;
    let studentExams;
    let knowledgePoints;
    let studentKnowledgePoints;
    let assignmentSubmissions;
    let examSubmissions;

    await step('teacher auth me', async () => {
      const { json } = await requestJson('GET', `${baseUrl}/api/auth/me`, teacherToken);
      assert(json.success === true, 'teacher /api/auth/me should succeed');
      assert(json.data.username === 'teacher7', 'teacher username mismatch', json.data);
    });

    await step('student auth me', async () => {
      const { json } = await requestJson('GET', `${baseUrl}/api/auth/me`, studentToken);
      assert(json.success === true, 'student /api/auth/me should succeed');
      assert(json.data.username === 'student42', 'student username mismatch', json.data);
    });

    await step('auth compatibility negative paths', async () => {
      const [teacherSwitchRes, studentSwitchRes, weakPasswordRes, invalidRefreshRes] = await Promise.all([
        requestJsonAllowStatus('POST', `${baseUrl}/api/auth/switch-role`, teacherToken, { targetRole: 'STUDENT' }, [403]),
        requestJsonAllowStatus('POST', `${baseUrl}/api/auth/switch-role`, studentToken, { targetRole: 'TEACHER' }, [403]),
        requestJsonAllowStatus('POST', `${baseUrl}/api/student/change-password`, studentToken, {
          currentPassword: 'wrong-password',
          newPassword: 'short1',
          confirmPassword: 'short1'
        }, [200]),
        requestJsonAllowStatus('POST', `${baseUrl}/api/auth/refresh`, null, { refreshToken: `invalid-${unique}` }, [401])
      ]);
      assert(teacherSwitchRes.json.success === false, 'teacher forbidden switch role should return failure payload');
      assert(studentSwitchRes.json.success === false, 'student forbidden switch role should return failure payload');
      assert(weakPasswordRes.json.success === false, 'weak student password change should return failure payload');
      assert(invalidRefreshRes.json.success === false, 'invalid refresh should return failure payload');
    });

    await step('teacher base reads', async () => {
      const [teacherCoursesRes, teacherClassesRes, teacherMajorsRes, teacherAssignmentsRes, teacherSubmissionsRes, teacherExamsRes] = await Promise.all([
        requestJson('GET', `${baseUrl}/api/teacher/courses?page=1&size=10`, teacherToken),
        requestJson('GET', `${baseUrl}/api/teacher/classes`, teacherToken),
        requestJson('GET', `${baseUrl}/api/teacher/majors`, teacherToken),
        requestJson('GET', `${baseUrl}/api/teacher/assignments?page=1&size=10`, teacherToken),
        requestJson('GET', `${baseUrl}/api/teacher/submissions?page=1&size=10`, teacherToken),
        requestJson('GET', `${baseUrl}/api/teacher/exams?page=1&size=10`, teacherToken)
      ]);
      teacherCourses = teacherCoursesRes.json.data.content;
      teacherAssignments = teacherAssignmentsRes.json.data.content;
      teacherExams = teacherExamsRes.json.data.content;
      assignmentSubmissions = teacherSubmissionsRes.json.data.content || teacherSubmissionsRes.json.data.submissions || [];
      assert(Array.isArray(teacherCourses), 'teacher courses should be an array');
      assert(Array.isArray(teacherClassesRes.json.data), 'teacher classes should be an array');
      assert(Array.isArray(teacherMajorsRes.json.data), 'teacher majors should be an array');
      assert(Array.isArray(teacherAssignments), 'teacher assignments should be an array');
      assert(Array.isArray(teacherExams), 'teacher exams should be an array');
    });

    await step('student base reads', async () => {
      const [studentCoursesRes, studentAssignmentsRes, studentSubmissionsRes, studentExamsRes, studentScoresRes] = await Promise.all([
        requestJson('GET', `${baseUrl}/api/student/courses?page=1&size=10`, studentToken),
        requestJson('GET', `${baseUrl}/api/student/assignments?page=1&size=10`, studentToken),
        requestJson('GET', `${baseUrl}/api/student/assignment-submissions`, studentToken),
        requestJson('GET', `${baseUrl}/api/student/exams?page=1&size=10`, studentToken),
        requestJson('GET', `${baseUrl}/api/student/scores`, studentToken)
      ]);
      studentCourses = studentCoursesRes.json.data.content;
      studentAssignments = studentAssignmentsRes.json.data.content;
      studentExams = studentExamsRes.json.data.content;
      examSubmissions = studentSubmissionsRes.json.data;
      assert(Array.isArray(studentCourses), 'student courses should be an array');
      assert(Array.isArray(studentAssignments), 'student assignments should be an array');
      assert(Array.isArray(examSubmissions), 'student assignment submissions should be an array');
      assert(Array.isArray(studentExams), 'student exams should be an array');
      assert(Array.isArray(studentScoresRes.json.data), 'student scores should be an array');
    });

    await step('system and user compatibility reads', async () => {
      const [semestersRes, teacherCoursesRes, studentCoursesRes, rangesRes, profileRes, notifSettingsRes, privacyRes, exportDataRes, usersMeRes, userByIdRes, studentClassRes, publicCaptchaRes, authCaptchaRes] = await Promise.all([
        requestJson('GET', `${baseUrl}/api/system/semesters`, teacherToken),
        requestJson('GET', `${baseUrl}/api/system/teacher/courses`, teacherToken),
        requestJson('GET', `${baseUrl}/api/system/student/courses`, studentToken),
        requestJson('GET', `${baseUrl}/api/system/time-ranges`, teacherToken),
        requestJson('GET', `${baseUrl}/api/student/profile`, studentToken),
        requestJson('GET', `${baseUrl}/api/student/notification-settings`, studentToken),
        requestJson('GET', `${baseUrl}/api/student/privacy-settings`, studentToken),
        requestJson('GET', `${baseUrl}/api/student/export-data`, studentToken),
        requestJson('GET', `${baseUrl}/api/users/me`, studentToken),
        requestJson('GET', `${baseUrl}/api/users/42`, studentToken),
        requestJson('GET', `${baseUrl}/api/students/42/class`, teacherToken),
        requestBinary(`${baseUrl}/api/public/captcha`, teacherToken),
        requestBinary(`${baseUrl}/api/auth/captcha`, teacherToken)
      ]);
      assert(Array.isArray(semestersRes.json.data), 'semesters should be an array');
      assert(Array.isArray(teacherCoursesRes.json.data), 'system teacher courses should be an array');
      assert(Array.isArray(studentCoursesRes.json.data), 'system student courses should be an array');
      assert(Array.isArray(rangesRes.json.data), 'time ranges should be an array');
      assert(profileRes.json.data.studentId === 42, 'student profile should resolve to student42', profileRes.json.data);
      assert(typeof notifSettingsRes.json.data === 'object', 'notification settings should be an object');
      assert(typeof privacyRes.json.data === 'object', 'privacy settings should be an object');
      assert(typeof exportDataRes.json.data === 'object', 'export data should be an object');
      assert(usersMeRes.json.data.username === 'student42', 'users/me should resolve current student', usersMeRes.json.data);
      assert(userByIdRes.json.data.username === 'student42', 'users/{id} should resolve student42', userByIdRes.json.data);
      assert(typeof studentClassRes.json.data === 'string', 'student class name should be string');
      assert(publicCaptchaRes.response.headers.get('x-captcha-key'), 'public captcha should include X-Captcha-Key');
      assert(authCaptchaRes.response.headers.get('x-captcha-key'), 'auth captcha should include X-Captcha-Key');
    });

    await step('analysis and dashboard reads', async () => {
      const [teacherDashboardRes, teacherSummaryRes, scoreTrendRes, warningStatsRes, warningListRes, pendingWarningsRes, studentStatsRes, studentStudyTimeRes, studentKnowledgeRes, studentWarningsRes, studentPerformanceRes, studentPerformanceByIdRes, teacherAnalysisCourseCompatRes] = await Promise.all([
        requestJson('GET', `${baseUrl}/api/teacher/dashboard`, teacherToken),
        requestJson('GET', `${baseUrl}/api/teacher/learning-summary`, teacherToken),
        requestJson('GET', `${baseUrl}/api/teacher/score-trend`, teacherToken),
        requestJson('GET', `${baseUrl}/api/early-warnings/teacher/stats`, teacherToken),
        requestJson('GET', `${baseUrl}/api/early-warnings/teacher/list?page=1&size=10`, teacherToken),
        requestJson('GET', `${baseUrl}/api/early-warnings/teacher/pending`, teacherToken),
        requestJson('GET', `${baseUrl}/api/student/stats`, studentToken),
        requestJson('GET', `${baseUrl}/api/student/study-time-distribution`, studentToken),
        requestJson('GET', `${baseUrl}/api/student/knowledge-points`, studentToken),
        requestJson('GET', `${baseUrl}/api/student/early-warnings`, studentToken),
        requestJson('GET', `${baseUrl}/api/dashboard/student-performance`, studentToken),
        requestJson('GET', `${baseUrl}/api/dashboard/student-performance/42`, teacherToken),
        requestJson('GET', `${baseUrl}/api/knowledge-points/analysis/teacher/course?courseId=2`, teacherToken)
      ]);
      assert(typeof teacherDashboardRes.json.data === 'object', 'teacher dashboard should be object');
      assert(typeof teacherSummaryRes.json.data === 'object', 'teacher learning summary should be object');
      assert(Array.isArray(scoreTrendRes.json.data), 'score trend should be array');
      assert(typeof warningStatsRes.json.data === 'object', 'warning stats should be object');
      assert(Array.isArray(warningListRes.json.data.content), 'warning list content should be array');
      assert(Array.isArray(pendingWarningsRes.json.data), 'pending warnings should be array');
      assert(typeof studentStatsRes.json.data === 'object', 'student stats should be object');
      assert(Array.isArray(studentStudyTimeRes.json.data), 'study time distribution should be array');
      studentKnowledgePoints = studentKnowledgeRes.json.data;
      assert(Array.isArray(studentKnowledgePoints), 'student knowledge points should be array');
      assert(Array.isArray(studentWarningsRes.json.data), 'student warnings should be array');
      assert(studentPerformanceRes.json.success === false && studentPerformanceRes.json.unsupported === true, 'student performance compatibility endpoint should return 501 compatibility payload', studentPerformanceRes.json);
      assert(studentPerformanceByIdRes.json.success === false && studentPerformanceByIdRes.json.unsupported === true, 'student performance by id compatibility endpoint should return 501 compatibility payload', studentPerformanceByIdRes.json);
      assert(teacherAnalysisCourseCompatRes.json.success === true, 'teacher knowledge point analysis course compatibility endpoint should succeed');
    });

    await step('teacher knowledge point reads', async () => {
      const teacherCourse = teacherCourses.find(item => item.id === 2) || teacherCourses[0];
      assert(teacherCourse, 'expected at least one teacher course');
      const [knowledgeListRes, knowledgeByCourseRes] = await Promise.all([
        requestJson('GET', `${baseUrl}/api/teacher/knowledge-points`, teacherToken),
        requestJson('GET', `${baseUrl}/api/teacher/knowledge-points/course/${teacherCourse.id}`, teacherToken)
      ]);
      knowledgePoints = knowledgeByCourseRes.json.data;
      assert(Array.isArray(knowledgeListRes.json.data), 'teacher knowledge point list should be array');
      assert(Array.isArray(knowledgePoints), 'teacher knowledge points by course should be array');
      if (knowledgePoints.length > 0) {
        const detailRes = await requestJson('GET', `${baseUrl}/api/teacher/knowledge-points/${knowledgePoints[0].id}`, teacherToken);
        assert(detailRes.json.success === true, 'teacher knowledge point detail should succeed');
      }
    });

    await step('student detail reads', async () => {
      const studentCourse = studentCourses.find(item => item.id === 2) || studentCourses[0];
      const studentAssignment = studentAssignments[0];
      const studentExam = studentExams[0];
      assert(studentCourse, 'expected at least one student course');
      assert(studentAssignment, 'expected at least one student assignment');
      assert(studentExam, 'expected at least one student exam');
      const [courseDetailRes, assignmentDetailRes, examDetailRes] = await Promise.all([
        requestJson('GET', `${baseUrl}/api/student/courses/${studentCourse.id}`, studentToken),
        requestJson('GET', `${baseUrl}/api/student/assignments/${studentAssignment.id}`, studentToken),
        requestJson('GET', `${baseUrl}/api/student/exams/${studentExam.id}`, studentToken)
      ]);
      assert(courseDetailRes.json.success === true, 'student course detail should succeed');
      assert(assignmentDetailRes.json.success === true, 'student assignment detail should succeed');
      assert(examDetailRes.json.success === true, 'student exam detail should succeed');
      if (studentKnowledgePoints.length > 0) {
        const kpDetailRes = await requestJson('GET', `${baseUrl}/api/student/knowledge-points/${studentKnowledgePoints[0].id}`, studentToken);
        assert(kpDetailRes.json.success === true, 'student knowledge point detail should succeed');
      }

      const missingKpRes = await requestJsonAllowStatus('GET', `${baseUrl}/api/student/knowledge-points/999999`, studentToken, undefined, [200]);
      assert(missingKpRes.json.success === false && missingKpRes.json.code === 404, 'missing student knowledge point should return legacy 404 envelope', missingKpRes.json);
    });

    await step('gateway browser error endpoints', async () => {
      const errorPayload = {
        errorType: 'RuntimeError',
        errorMessage: `gateway smoke browser error ${unique}`,
        errorStack: 'Error: smoke',
        pageUrl: '/teacher/dashboard.html',
        lineNumber: 12,
        columnNumber: 34,
        fileUrl: '/assets/smoke.js',
        userAgent: 'gateway-api-smoke',
        status: 500
      };
      const reportRes = await requestJson('POST', `${baseUrl}/api/errors/browser`, teacherToken, errorPayload);
      const errorId = reportRes.json.data;
      assert(Number.isInteger(errorId), 'browser error report should return numeric id', reportRes.json);

      const batchRes = await requestJson('POST', `${baseUrl}/api/errors/browser/batch`, teacherToken, [
        { ...errorPayload, errorMessage: `gateway smoke browser batch A ${unique}`, status: 400 },
        { ...errorPayload, errorMessage: `gateway smoke browser batch B ${unique}`, status: 404 }
      ]);
      assert(batchRes.json.data === 2, 'browser error batch report should return saved count', batchRes.json);

      const listRes = await requestJson('GET', `${baseUrl}/api/errors/browser?page=1&size=5&errorType=RuntimeError`, teacherToken);
      assert(Array.isArray(listRes.json.data.content), 'browser error list should return page content');

      const detailRes = await requestJson('GET', `${baseUrl}/api/errors/browser/${errorId}`, teacherToken);
      assert(detailRes.json.data.id === errorId, 'browser error detail should return created id', detailRes.json.data);
    });

    await step('notifications read-write flow', async () => {
      const sendRes = await requestJson('POST', `${baseUrl}/api/notifications/teacher/send`, teacherToken, {
        type: 'course',
        title: `通知-${unique}`,
        content: '接口烟测通知',
        studentId: 42,
        relatedId: 2,
        isRead: false
      });
      created.notificationId = sendRes.json.data.id;
      assert(created.notificationId, 'notification create should return id', sendRes.json.data);

      const [studentNotificationsRes, allRes, unreadRes] = await Promise.all([
        requestJson('GET', `${baseUrl}/api/notifications/student?page=1&size=10`, studentToken),
        requestJson('GET', `${baseUrl}/api/notifications/student/all`, studentToken),
        requestJson('GET', `${baseUrl}/api/notifications/student/unread-count`, studentToken)
      ]);
      const pagedItems = studentNotificationsRes.json.data.notifications;
      const allItems = allRes.json.data;
      assert(Array.isArray(pagedItems), 'student notifications page should be array');
      assert(Array.isArray(allItems), 'student notifications all should be array');
      assert(allItems.some(item => item.id === created.notificationId), 'created notification should be visible in all notifications', allItems);
      assert(typeof unreadRes.json.data === 'number', 'unread count should be numeric');

      await requestJson('PUT', `${baseUrl}/api/notifications/${created.notificationId}/read`, studentToken);
      await requestJson('PUT', `${baseUrl}/api/notifications/read-all`, studentToken);

      const markedRes = await requestJson('GET', `${baseUrl}/api/notifications/student/all`, studentToken);
      const createdNotification = markedRes.json.data.find(item => item.id === created.notificationId);
      assert(createdNotification && createdNotification.read === true, 'notification should become read after mark-all', createdNotification);

      const batchRes = await requestJson('POST', `${baseUrl}/api/notifications/teacher/send-batch`, teacherToken, [
        {
          type: 'course',
          title: `批量通知A-${unique}`,
          content: '接口烟测批量通知 A',
          studentId: 42,
          relatedId: 2,
          isRead: false
        },
        {
          type: 'assignment',
          title: `批量通知B-${unique}`,
          content: '接口烟测批量通知 B',
          studentId: 42,
          relatedId: 2,
          isRead: false
        }
      ]);
      created.batchNotificationIds = (batchRes.json.data || []).map(item => item.id).filter(Boolean);
      assert(created.batchNotificationIds.length === 2, 'batch notification should return two ids', batchRes.json.data);
    });

    await step('student profile writes', async () => {
      const profileRes = await requestJson('GET', `${baseUrl}/api/student/profile`, studentToken);
      const profile = profileRes.json.data;
      const updateRes = await requestJson('PUT', `${baseUrl}/api/student/profile`, studentToken, {
        name: profile.name,
        email: profile.email,
        phone: profile.phone,
        avatar: profile.avatar
      });
      assert(updateRes.json.success === true, 'student profile update should succeed');

      const notifRes = await requestJson('GET', `${baseUrl}/api/student/notification-settings`, studentToken);
      const notifBody = notifRes.json.data || {};
      const notifUpdateRes = await requestJson('PUT', `${baseUrl}/api/student/notification-settings`, studentToken, notifBody);
      assert(notifUpdateRes.json.success === true, 'student notification settings update should succeed');

      const privacyRes = await requestJson('GET', `${baseUrl}/api/student/privacy-settings`, studentToken);
      const privacyBody = privacyRes.json.data || {};
      const privacyUpdateRes = await requestJson('PUT', `${baseUrl}/api/student/privacy-settings`, studentToken, privacyBody);
      assert(privacyUpdateRes.json.success === true, 'student privacy settings update should succeed');

      const avatarRes = await requestJson('POST', `${baseUrl}/api/student/upload-avatar`, studentToken, {
        avatar: profile.avatar || 'https://example.com/avatar.png'
      });
      assert(avatarRes.json.success === true, 'student avatar upload compatibility should succeed');

      const userMeRes = await requestJson('GET', `${baseUrl}/api/users/me`, studentToken);
      const me = userMeRes.json.data;
      const userUpdateRes = await requestJson('PUT', `${baseUrl}/api/users/me`, studentToken, {
        name: me.name,
        email: me.email,
        phone: me.phone
      });
      assert(userUpdateRes.json.success === true, 'users/me update should succeed');

      const userByIdUpdateRes = await requestJson('PUT', `${baseUrl}/api/users/42`, teacherToken, {
        name: me.name,
        email: me.email,
        phone: me.phone
      });
      assert(userByIdUpdateRes.json.success === true, 'users/{id} update should succeed');
    });

    await step('teacher write flow: course and class', async () => {
      const createCourseRes = await requestJson('POST', `${baseUrl}/api/teacher/courses`, teacherToken, {
        courseName: `Course-${unique}`,
        courseCode: `CS${String(Date.now()).slice(-8)}`,
        description: 'gateway api smoke course',
        credit: 3,
        courseCategory: 'practice',
        totalHours: 32,
        courseStatus: 'active',
        semester: '2026-Spring',
        startDate: sqlDateTime(futureCourseStart).slice(0, 10),
        endDate: sqlDateTime(futureCourseEnd).slice(0, 10),
        maxStudents: 30
      });
      created.courseId = createCourseRes.json.data.id;
      assert(created.courseId, 'course create should return id', createCourseRes.json.data);

      const courseDetailRes = await requestJson('GET', `${baseUrl}/api/teacher/courses/${created.courseId}`, teacherToken);
      assert(courseDetailRes.json.data.courseName.includes(unique), 'created course detail should match');

      const updateCourseRes = await requestJson('PUT', `${baseUrl}/api/teacher/courses/${created.courseId}`, teacherToken, {
        courseName: `Course-${unique}-Updated`,
        courseCode: `CU${String(Date.now()).slice(-8)}`,
        description: 'gateway api smoke course updated',
        credit: 4,
        courseCategory: 'practice',
        totalHours: 48,
        courseStatus: 'active',
        semester: '2026-Spring',
        startDate: sqlDateTime(futureCourseStart).slice(0, 10),
        endDate: sqlDateTime(futureCourseEnd).slice(0, 10),
        maxStudents: 35
      });
      assert(updateCourseRes.json.success === true, 'course update should succeed');

      const majorsRes = await requestJson('GET', `${baseUrl}/api/teacher/majors`, teacherToken);
      const majorId = Array.isArray(majorsRes.json.data) && majorsRes.json.data.length > 0 ? majorsRes.json.data[0].id : null;
      const createClassRes = await requestJson('POST', `${baseUrl}/api/teacher/classes`, teacherToken, {
        className: `Class-${unique}`,
        year: '2026',
        capacity: 35,
        courseId: created.courseId,
        majorId,
        classTime: '周一 08:00-10:00',
        classLocation: 'A101'
      });
      created.classId = createClassRes.json.data.id;
      assert(created.classId, 'class create should return id', createClassRes.json.data);

      const classDetailRes = await requestJson('GET', `${baseUrl}/api/teacher/classes/${created.classId}`, teacherToken);
      assert(classDetailRes.json.success === true, 'class detail should succeed');

      const classUpdateRes = await requestJson('PUT', `${baseUrl}/api/teacher/classes/${created.classId}`, teacherToken, {
        className: `Class-${unique}-Updated`,
        year: '2026',
        capacity: 40,
        courseId: created.courseId,
        majorId,
        classTime: '周二 10:00-12:00',
        classLocation: 'A102'
      });
      assert(classUpdateRes.json.success === true, 'class update should succeed');

      const assignRes = await requestJson('POST', `${baseUrl}/api/teacher/course-assignments`, teacherToken, {
        classId: created.classId,
        courseId: created.courseId,
        classTime: '周二 10:00-12:00',
        classLocation: 'A102'
      });
      assert(assignRes.json.success === true, 'course assignment should succeed');

      const assignmentListRes = await requestJson('GET', `${baseUrl}/api/teacher/course-assignments?page=1&size=20&courseId=${created.courseId}`, teacherToken);
      const assignmentRows = assignmentListRes.json.data.content || [];
      const createdAssignmentRow = assignmentRows.find(item => item.courseId === created.courseId && item.classId === created.classId);
      assert(createdAssignmentRow, 'course assignment should be queryable', assignmentRows);

      const classStudentsRes = await requestJson('GET', `${baseUrl}/api/teacher/classes/${created.classId}/students`, teacherToken);
      assert(Array.isArray(classStudentsRes.json.data), 'class students should be array even when empty');

      const courseStudentsRes = await requestJson('GET', `${baseUrl}/api/teacher/courses/${created.courseId}/students`, teacherToken);
      assert(Array.isArray(courseStudentsRes.json.data), 'course students should be array even when empty');

      await requestJson('DELETE', `${baseUrl}/api/teacher/class-courses/unassign?classId=${created.classId}&courseId=${created.courseId}`, teacherToken);

      const reassignmentRes = await requestJson('POST', `${baseUrl}/api/teacher/course-assignments`, teacherToken, {
        classId: created.classId,
        courseId: created.courseId,
        classTime: '周三 14:00-16:00',
        classLocation: 'A103'
      });
      assert(reassignmentRes.json.success === true, 'course reassignment should succeed after class-course unassign');

      const reassignmentListRes = await requestJson('GET', `${baseUrl}/api/teacher/course-assignments?page=1&size=20&courseId=${created.courseId}`, teacherToken);
      const reassignmentRows = reassignmentListRes.json.data.content || [];
      const reassignedRow = reassignmentRows.find(item => item.courseId === created.courseId && item.classId === created.classId);
      assert(reassignedRow, 'course reassignment should be queryable', reassignmentRows);

      await requestJson('DELETE', `${baseUrl}/api/teacher/course-assignments/${reassignedRow.id || reassignedRow.assignmentId}`, teacherToken);

      const classNameCheckRes = await requestJson('GET', `${baseUrl}/api/teacher/check-class-name?className=${encodeURIComponent(`Class-${unique}-Updated`)}`, teacherToken);
      assert(typeof classNameCheckRes.json.data.exists === 'boolean', 'class name check should return exists boolean');
    });

    await step('teacher write flow: knowledge point', async () => {
      assert(created.courseId, 'created course id required for knowledge point flow');
      const createRes = await requestJson('POST', `${baseUrl}/api/teacher/knowledge-points`, teacherToken, {
        pointName: `KP-${unique}`,
        description: 'gateway api smoke knowledge point',
        difficulty: '中等',
        orderIndex: 1,
        courseId: created.courseId
      });
      const knowledgePointId = createRes.json.data.id;
      assert(knowledgePointId, 'knowledge point create should return id', createRes.json.data);

      const updateRes = await requestJson('PUT', `${baseUrl}/api/teacher/knowledge-points/${knowledgePointId}`, teacherToken, {
        pointName: `KP-${unique}-Updated`,
        description: 'gateway api smoke knowledge point updated',
        difficulty: '困难',
        orderIndex: 2,
        courseId: created.courseId
      });
      assert(updateRes.json.success === true, 'knowledge point update should succeed');

      const getRes = await requestJson('GET', `${baseUrl}/api/teacher/knowledge-points/${knowledgePointId}`, teacherToken);
      assert(getRes.json.data.pointName.includes(`${unique}-Updated`), 'knowledge point detail should reflect update', getRes.json.data);

      await requestJson('DELETE', `${baseUrl}/api/teacher/knowledge-points/${knowledgePointId}`, teacherToken);
    });

    await step('teacher write flow: assignment and grading', async () => {
      const courseId = 2;
      const createAssignmentRes = await requestJson('POST', `${baseUrl}/api/teacher/assignments`, teacherToken, {
        title: `Assign-${unique}`,
        description: 'gateway api smoke assignment',
        courseId,
        publishDate: sqlDateTime(futurePublish),
        dueDate: sqlDateTime(futureDue),
        isActive: true,
        maxScore: 100,
        knowledgePointIds: []
      });
      created.assignmentId = createAssignmentRes.json.data.id;
      assert(created.assignmentId, 'assignment create should return id', createAssignmentRes.json.data);

      const assignmentDetailRes = await requestJson('GET', `${baseUrl}/api/teacher/assignments/${created.assignmentId}`, teacherToken);
      assert(assignmentDetailRes.json.data.title === `Assign-${unique}`, 'assignment detail should match create');

      const assignmentUpdateRes = await requestJson('PUT', `${baseUrl}/api/teacher/assignments/${created.assignmentId}`, teacherToken, {
        title: `Assign-${unique}-Updated`,
        description: 'gateway api smoke assignment updated',
        courseId,
        publishDate: sqlDateTime(futurePublish),
        dueDate: sqlDateTime(new Date(futureDue.getTime() + 3600 * 1000)),
        isActive: true,
        maxScore: 100,
        knowledgePointIds: []
      });
      assert(assignmentUpdateRes.json.success === true, 'assignment update should succeed');

      await requestJson('POST', `${baseUrl}/api/teacher/knowledge-points/assignment/${created.assignmentId}`, teacherToken, {
        knowledgePointIds: []
      });
      const assignmentKpRes = await requestJson('GET', `${baseUrl}/api/teacher/knowledge-points/assignment/${created.assignmentId}`, teacherToken);
      assert(Array.isArray(assignmentKpRes.json.data), 'assignment knowledge points should be array');

      const submitRes = await requestJson('POST', `${baseUrl}/api/student/assignments/${created.assignmentId}/submit`, studentToken, {
        content: `student assignment submission ${unique}`,
        submissionDate: sqlDateTime(pastSubmission)
      });
      assert(submitRes.json.success === true, 'student assignment submit should succeed');

      const teacherSubmissionListRes = await requestJson('GET', `${baseUrl}/api/teacher/assignments/${created.assignmentId}/submissions`, teacherToken);
      const submission = (teacherSubmissionListRes.json.data || [])[0];
      assert(submission && submission.id, 'teacher should see assignment submission', teacherSubmissionListRes.json.data);

      const submissionDetailRes = await requestJson('GET', `${baseUrl}/api/teacher/submissions/${submission.id}`, teacherToken);
      assert(submissionDetailRes.json.success === true, 'teacher submission detail should succeed');

      const gradeRes = await requestJson('PUT', `${baseUrl}/api/teacher/submissions/${submission.id}/grade`, teacherToken, {
        score: 88,
        teacherComment: '接口烟测批改',
        graded: true
      });
      assert(gradeRes.json.success === true, 'assignment grading should succeed');

      const studentAssignmentDetailRes = await requestJson('GET', `${baseUrl}/api/student/assignments/${created.assignmentId}`, studentToken);
      assert(studentAssignmentDetailRes.json.data.submission, 'student assignment detail should expose submission after submit', studentAssignmentDetailRes.json.data);

      const teacherAssignmentSubmissionCompatRes = await requestJson('GET', `${baseUrl}/api/teacher/assignments/submissions/${submission.id}`, teacherToken);
      assert(teacherAssignmentSubmissionCompatRes.json.success === true, 'teacher assignment submission compatibility detail should succeed');
    });

    await step('teacher write flow: exam and grading', async () => {
      const createExamRes = await requestJson('POST', `${baseUrl}/api/teacher/exams`, teacherToken, {
        title: `Exam-${unique}`,
        description: 'gateway api smoke exam',
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
      created.examId = createExamRes.json.data.id;
      assert(created.examId, 'exam create should return id', createExamRes.json.data);

      const examDetailRes = await requestJson('GET', `${baseUrl}/api/teacher/exams/${created.examId}`, teacherToken);
      assert(examDetailRes.json.success === true, 'teacher exam detail should succeed');

      const examUpdateRes = await requestJson('PUT', `${baseUrl}/api/teacher/exams/${created.examId}`, teacherToken, {
        title: `Exam-${unique}-Updated`,
        description: 'gateway api smoke exam updated',
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
      assert(examUpdateRes.json.success === true, 'exam update should succeed');

      await requestJson('POST', `${baseUrl}/api/teacher/knowledge-points/exam/${created.examId}`, teacherToken, {
        knowledgePointIds: []
      });
      const examKpRes = await requestJson('GET', `${baseUrl}/api/teacher/knowledge-points/exam/${created.examId}`, teacherToken);
      assert(Array.isArray(examKpRes.json.data), 'exam knowledge points should be array');

      const studentExamDetailRes = await requestJson('GET', `${baseUrl}/api/student/exams/${created.examId}`, studentToken);
      assert(studentExamDetailRes.json.success === true, 'student exam detail should succeed');
      assert(studentExamDetailRes.json.data.id === created.examId, 'student exam detail should return created exam', studentExamDetailRes.json.data);
      const submitExamRes = await requestJson('POST', `${baseUrl}/api/student/exams/${created.examId}/submit`, studentToken, {
        timeTaken: 12,
        answers: {
          content: `student exam submission ${unique}`
        }
      });
      assert(submitExamRes.json.success === true, 'student exam submit should succeed');

      const teacherExamSubmissionsRes = await requestJson('GET', `${baseUrl}/api/teacher/exams/${created.examId}/submissions`, teacherToken);
      const examSubmission = (teacherExamSubmissionsRes.json.data || [])[0];
      assert(examSubmission && examSubmission.id, 'teacher should see exam submission', teacherExamSubmissionsRes.json.data);

      const submissionDetailRes = await requestJson('GET', `${baseUrl}/api/teacher/exams/submissions/${examSubmission.id}`, teacherToken);
      assert(submissionDetailRes.json.success === true, 'teacher exam submission detail should succeed');

      const gradeExamRes = await requestJson('PUT', `${baseUrl}/api/teacher/exams/grade/${examSubmission.id}`, teacherToken, {
        score: 100,
        teacherComment: '接口烟测考试批改',
        graded: true
      });
      assert(gradeExamRes.json.success === true, 'exam grading should succeed');
    });

    await step('teacher student detail compatibility reads', async () => {
      const [studentDetailRes, userStudentRes] = await Promise.all([
        requestJson('GET', `${baseUrl}/api/teacher/students/42`, teacherToken),
        requestJson('GET', `${baseUrl}/api/users/students/42`, teacherToken)
      ]);
      assert(studentDetailRes.json.success === true, 'teacher student detail should succeed');
      assert(userStudentRes.json.success === true, 'user student detail should succeed');
    });

    await step('teacher extended compatibility writes', async () => {
      const teacherStudentRes = await requestJson('GET', `${baseUrl}/api/teacher/students/42`, teacherToken);
      const student = teacherStudentRes.json.data;
      const [teacherStudentUpdateRes, userStudentUpdateRes, authNotificationRes] = await Promise.all([
        requestJson('PUT', `${baseUrl}/api/teacher/students/42`, teacherToken, {
          name: student.name || student.realName || 'Student Forty Two',
          realName: student.realName || student.name || 'Student Forty Two',
          email: student.email,
          phone: student.phone,
          avatar: student.avatar,
          classId: student.classId
        }),
        requestJson('PUT', `${baseUrl}/api/users/students/42`, teacherToken, {
          realName: student.realName || student.name || 'Student Forty Two',
          email: student.email,
          phone: student.phone,
          avatar: student.avatar
        }),
        requestJson('PUT', `${baseUrl}/api/auth/notification-settings`, teacherToken, {
          emailNotifications: true,
          assignmentNotifications: true
        })
      ]);
      assert(teacherStudentUpdateRes.json.success === true, 'teacher student update should succeed');
      assert(userStudentUpdateRes.json.success === true, 'user student update should succeed');
      assert(authNotificationRes.json.success === true, 'auth notification settings compatibility should succeed');
    });

    await step('analysis write flow: warning and trigger', async () => {
      const warningCreateRes = await requestJson('POST', `${baseUrl}/api/early-warnings/teacher`, teacherToken, {
        studentId: 42,
        courseId: 2,
        warningType: 'LOW_SCORE',
        warningLevel: 'HIGH',
        warningMessage: `接口烟测预警 ${unique}`
      });
      created.warningId = warningCreateRes.json.data.id;
      assert(created.warningId, 'warning create should return id', warningCreateRes.json.data);

      const [warningDetailRes, courseWarningsRes, warningExportRes, warningTriggerRes, knowledgeTriggerRes, triggerRes, triggerCompatRes] = await Promise.all([
        requestJson('GET', `${baseUrl}/api/early-warnings/teacher/detail/${created.warningId}`, teacherToken),
        requestJson('GET', `${baseUrl}/api/teacher/early-warnings/course/2?warningType=LOW_SCORE&warningLevel=HIGH&isResolved=false`, teacherToken),
        retry(() => requestBinary(`${baseUrl}/api/early-warnings/teacher/export`, teacherToken), 3, 1200),
        requestJson('POST', `${baseUrl}/api/teacher/analysis/warnings/trigger`, teacherToken),
        requestJson('POST', `${baseUrl}/api/teacher/analysis/knowledge-points/trigger`, teacherToken),
        requestJson('POST', `${baseUrl}/api/teacher/knowledge-points/analyze/student/42/course/2`, teacherToken),
        requestJson('POST', `${baseUrl}/api/teacher/analysis/student/42/course/2/trigger`, teacherToken)
      ]);
      assert(warningDetailRes.json.success === true, 'warning detail should succeed');
      assert(Array.isArray(courseWarningsRes.json.data), 'course warnings should be array');
      assert(warningExportRes.response.headers.get('content-type')?.includes('spreadsheetml'), 'warning export should return xlsx content');
      assert(warningTriggerRes.json.success === true, 'warning analysis trigger should succeed');
      assert(knowledgeTriggerRes.json.success === true, 'knowledge point analysis trigger should succeed');
      assert(triggerRes.json.success === true, 'knowledge point analyze trigger should succeed');
      assert(triggerCompatRes.json.success === true, 'compatibility trigger should succeed');

      const resolveRes = await requestJson('PUT', `${baseUrl}/api/teacher/early-warnings/${created.warningId}/resolve`, teacherToken, {
        resolvedNote: '接口烟测已处理'
      });
      assert(resolveRes.json.success === true, 'warning resolve should succeed');

      const statusUpdateRes = await requestJson('PUT', `${baseUrl}/api/early-warnings/teacher/status/${created.warningId}`, teacherToken, {
        status: 'resolved',
        resolvedNote: '接口烟测再次确认'
      });
      assert(statusUpdateRes.json.success === true, 'warning status update should succeed');

      const [masteryRes, masteryStatsRes, teacherAnalysisRes] = await Promise.all([
        requestJson('GET', `${baseUrl}/api/teacher/knowledge-points/mastery/student/42/course/2`, teacherToken),
        requestJson('GET', `${baseUrl}/api/teacher/knowledge-points/stats/course/2`, teacherToken),
        requestJson('GET', `${baseUrl}/api/knowledge-points/analysis/teacher/course/2`, teacherToken)
      ]);
      assert(Array.isArray(masteryRes.json.data), 'teacher knowledge mastery should be array');
      assert(Array.isArray(masteryStatsRes.json.data), 'teacher knowledge mastery stats should be array');
      assert(teacherAnalysisRes.json.success === true, 'teacher knowledge point analysis should succeed');
    });

    await step('ai endpoints', async () => {
      const [questionsRes, examRes, suggestionsRes] = await Promise.all([
        requestJson('POST', `${baseUrl}/api/ai/generate-questions`, teacherToken, {
          topic: 'Java基础',
          count: 2,
          difficulty: '中等'
        }),
        requestJson('POST', `${baseUrl}/api/ai/generate-exam`, teacherToken, {
          courseName: 'Java',
          totalScore: 100,
          duration: 90,
          difficulty: '中等'
        }),
        requestJson('POST', `${baseUrl}/api/ai/learning-suggestions`, studentToken, {
          studentId: 42
        })
      ]);
      assert(questionsRes.json.success === true, 'AI generate questions should succeed');
      assert(examRes.json.success === true, 'AI generate exam should succeed');
      assert(suggestionsRes.json.success === true, 'AI learning suggestions should succeed');
    });
  } finally {
    await cleanup();
  }

  const failed = results.filter(item => !item.ok);
  if (failed.length > 0) {
    process.exitCode = 1;
    return;
  }

  console.log(`All smoke checks passed: ${results.length}`);
}

run().catch(error => {
  console.error(error.stack || error.message);
  process.exitCode = 1;
});
