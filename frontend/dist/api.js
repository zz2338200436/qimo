// API基础URL
const API_BASE_URL = '';

// 存储课程数据，用于映射courseId到courseName
let courseMap = new Map();

// 从Cookie中获取CSRF Token
function getCsrfToken() {
    if (typeof document === 'undefined' || !document.cookie) {
        return null;
    }
    const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
    return match ? decodeURIComponent(match[1]) : null;
}

function getAccessToken() {
    if (typeof window === 'undefined' || !window.sessionStorage) {
        return null;
    }
    return window.sessionStorage.getItem('token');
}

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
        return JSON.parse(atob(padded));
    } catch (error) {
        return null;
    }
}

function isJwtLikelyExpired(token, thresholdSeconds = 30) {
    const payload = decodeJwtPayload(token);
    if (!payload || typeof payload.exp !== 'number') {
        return false;
    }
    const nowSeconds = Math.floor(Date.now() / 1000);
    return payload.exp <= (nowSeconds + thresholdSeconds);
}

function getCurrentUserId() {
    if (typeof window === 'undefined' || !window.sessionStorage) {
        return null;
    }
    const storedUserId = window.sessionStorage.getItem('userId');
    if (storedUserId) {
        return storedUserId;
    }
    const rawUser = window.sessionStorage.getItem('user');
    if (!rawUser) {
        return null;
    }
    try {
        const user = JSON.parse(rawUser);
        return user && user.id != null ? String(user.id) : null;
    } catch (error) {
        return null;
    }
}

function clearAuthSession() {
    if (typeof window === 'undefined' || !window.sessionStorage) {
        return;
    }
    window.sessionStorage.removeItem('user');
    window.sessionStorage.removeItem('token');
    window.sessionStorage.removeItem('refreshToken');
    window.sessionStorage.removeItem('userId');
    window.sessionStorage.removeItem('activeRole');
    window.sessionStorage.removeItem('role');
}

function persistAuthSession(authData) {
    if (!authData || typeof window === 'undefined' || !window.sessionStorage) {
        return;
    }

    clearAuthSession();

    const { accessToken, refreshToken, user } = authData;
    if (accessToken) {
        window.sessionStorage.setItem('token', accessToken);
    }
    if (refreshToken) {
        window.sessionStorage.setItem('refreshToken', refreshToken);
    }
    if (user) {
        window.sessionStorage.setItem('user', JSON.stringify(user));
        if (user.id != null) {
            window.sessionStorage.setItem('userId', String(user.id));
        }

        const resolvedRole = user.activeRole
            || (Array.isArray(user.roles) && user.roles.length > 0 ? user.roles[0] : null);
        if (resolvedRole) {
            window.sessionStorage.setItem('activeRole', resolvedRole);
            window.sessionStorage.setItem('role', resolvedRole);
        }
    }
}

let refreshPromise = null;

async function tryRefreshAuthSession() {
    if (typeof window === 'undefined' || !window.sessionStorage) {
        return false;
    }
    const refreshToken = window.sessionStorage.getItem('refreshToken');
    if (!refreshToken) {
        return false;
    }

    if (!refreshPromise) {
        refreshPromise = (async () => {
            try {
                const csrfToken = getCsrfToken();
                const headers = {
                    'Content-Type': 'application/json'
                };
                if (csrfToken) {
                    headers['X-XSRF-TOKEN'] = csrfToken;
                }

                const response = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
                    method: 'POST',
                    credentials: 'include',
                    headers,
                    body: JSON.stringify({ refreshToken })
                });

                const payload = await response.json().catch(() => null);
                if (!response.ok || !payload?.success || !payload?.data) {
                    clearAuthSession();
                    return false;
                }

                persistAuthSession(payload.data);
                return true;
            } catch (error) {
                clearAuthSession();
                return false;
            } finally {
                refreshPromise = null;
            }
        })();
    }

    return refreshPromise;
}

function getRoleContext() {
    if (typeof window === 'undefined' || !window.location) {
        return null;
    }
    const path = (window.location.pathname || '').toLowerCase();
    const fileName = path.split('/').pop() || '';
    if (fileName.startsWith('teacher-') || path.includes('/teacher/')) {
        return 'TEACHER';
    }
    if (fileName.startsWith('student-') || path.includes('/student/')) {
        return 'STUDENT';
    }
    if (fileName.startsWith('admin-') || path.includes('/admin/')) {
        return 'ADMIN';
    }
    return null;
}

function getCurrentPageName() {
    if (typeof window === 'undefined' || !window.location) {
        return '';
    }
    return (window.location.pathname.split('/').pop() || '').toLowerCase();
}

function shouldSilenceUnauthorizedLog(url) {
    const currentPage = getCurrentPageName();
    if (currentPage === 'teacher-knowledge.html') {
        return url.startsWith('/api/teacher/knowledge-points') ||
            url.startsWith('/api/knowledge-points/analysis/teacher/course');
    }
    return false;
}

function isLegacySessionUnavailable() {
    if (typeof window === 'undefined' || !window.sessionStorage) {
        return false;
    }
    const token = window.sessionStorage.getItem('token');
    if (!token) {
        return false;
    }
    const hasSessionCookie = typeof document !== 'undefined' && document.cookie.includes('JSESSIONID=');
    return !hasSessionCookie;
}

function isJwtOnlyStudentSession() {
    return getRoleContext() === 'STUDENT' && isLegacySessionUnavailable();
}

function getStudentSessionContext() {
    const token = getAccessToken();
    const userId = getCurrentUserId();
    let user = null;
    if (typeof window !== 'undefined' && window.sessionStorage) {
        const rawUser = window.sessionStorage.getItem('user');
        if (rawUser) {
            try {
                user = JSON.parse(rawUser);
            } catch (error) {
                user = null;
            }
        }
    }

    return {
        token,
        userId,
        user,
        hasUser: !!user,
        isJwtOnly: isJwtOnlyStudentSession(),
        canRender: getRoleContext() === 'STUDENT' && !!token
    };
}

const DEFAULT_STUDENT_MICROSERVICE_CAPABILITIES = Object.freeze({
    dashboardPerformance: false,
    courses: true,
    courseDetail: true,
    assignments: true,
    assignmentDetail: true,
    assignmentSubmit: true,
    exams: true,
    examDetail: true,
    examSubmit: true,
    scores: true,
    stats: true,
    studyTimeDistribution: true,
    knowledgePoints: true,
    studentProfile: true,
    studentProfileUpdate: true,
    changePassword: true,
    notificationSettings: false,
    privacySettings: false,
    exportData: true,
    avatarUpload: true,
    activities: false
});

let runtimeStudentCapabilities = null;
let frontendCapabilitiesPromise = null;

function getStudentCapabilities() {
    return runtimeStudentCapabilities || DEFAULT_STUDENT_MICROSERVICE_CAPABILITIES;
}

async function loadFrontendCapabilities(forceRefresh = false) {
    if (frontendCapabilitiesPromise && !forceRefresh) {
        return frontendCapabilitiesPromise;
    }

    frontendCapabilitiesPromise = (async () => {
        try {
            const response = await fetch(`${API_BASE_URL}/api/frontend/capabilities`, {
                method: 'GET',
                credentials: 'include'
            });

            if (!response.ok) {
                return null;
            }

            const payload = await response.json().catch(() => null);
            const studentCapabilities = payload?.data?.student;
            if (studentCapabilities && typeof studentCapabilities === 'object') {
                runtimeStudentCapabilities = Object.assign({}, DEFAULT_STUDENT_MICROSERVICE_CAPABILITIES, studentCapabilities);
            }
            return payload?.data || null;
        } catch (error) {
            return null;
        }
    })();

    return frontendCapabilitiesPromise;
}

function studentCapabilitySupported(capability) {
    if (!capability) {
        return true;
    }
    if (!isJwtOnlyStudentSession()) {
        return true;
    }
    return getStudentCapabilities()[capability] !== false;
}

function getStudentCapabilityMessage(capability, fallbackMessage) {
    const capabilityMessages = {
        dashboardPerformance: '当前 JWT 微服务环境暂未提供学生综合表现接口，页面将改用已接通的数据源进行统计。',
        activities: '当前 JWT 微服务环境暂未提供学生活动流接口，已仅展示可用数据。',
        avatarUpload: '当前 JWT 微服务环境已接通头像上传接口。'
    };
    return capabilityMessages[capability] || fallbackMessage || '当前环境暂未提供该学生功能。';
}

function buildUnsupportedStudentCapabilityResult(capability, fallbackMessage, code = 501) {
    return {
        success: false,
        code,
        unsupported: true,
        capability,
        message: getStudentCapabilityMessage(capability, fallbackMessage),
        data: null
    };
}

async function guardStudentCapability(capability, executor, fallbackMessage) {
    if (!studentCapabilitySupported(capability)) {
        return buildUnsupportedStudentCapabilityResult(capability, fallbackMessage);
    }
    return executor();
}

function toBackendUrl(url) {
    if (!url) {
        return url;
    }
    if (typeof url !== 'string') {
        return url;
    }
    if (url.startsWith('http://') || url.startsWith('https://')) {
        return url;
    }
    if (url.startsWith('/api/')) {
        return `${API_BASE_URL}${url}`;
    }
    return url;
}

function isAuthLoginRequest(url) {
    try {
        const normalizedUrl = new URL(toBackendUrl(url), typeof window !== 'undefined' ? window.location.href : API_BASE_URL);
        return normalizedUrl.pathname === '/api/auth/login' || normalizedUrl.pathname === '/api/auth/refresh';
    } catch (error) {
        return false;
    }
}

function shouldIncludeExamDataForTeacherStats(options = {}) {
    if (typeof options.includeExams === 'boolean') {
        return options.includeExams;
    }
    return false;
}

function shouldIncludeEarlyWarningsForTeacherStats(options = {}) {
    if (typeof options.includeWarnings === 'boolean') {
        return options.includeWarnings;
    }
    return false;
}

// 全局 fetch CSRF 补丁
// --------------------------------------------------------------
// 后端 SecurityConfig 对 /api/**（除 /api/auth、/api/public）全部启用 CSRF 校验。
// 项目里散落在各个页面的裸 fetch 不经过 APIService，不会自动附 X-XSRF-TOKEN，
// 导致所有 POST/PUT/DELETE/PATCH 收到 403。
//
// 这里一次性劫持 window.fetch：对同源（或显式指向后端 host）的非 GET/HEAD 请求，
// 从 XSRF-TOKEN cookie 读值并追加 X-XSRF-TOKEN 头（若调用方已显式设置则不覆盖）。
// 这样所有页面无需改动即可通过 CSRF 校验。
// --------------------------------------------------------------
(function patchFetchForCsrf() {
    if (typeof window === 'undefined' || !window.fetch || window.__csrfFetchPatched) {
        return;
    }
    const originalFetch = window.fetch.bind(window);

    function isSameBackend(url) {
        try {
            if (!url) return true;
            if (typeof url !== 'string') {
                url = url.url || String(url);
            }
            if (url.startsWith('/')) return true;
            const u = new URL(url, window.location.href);
            return u.host === window.location.host || u.origin === API_BASE_URL;
        } catch (e) {
            return false;
        }
    }

    function needsCsrf(method) {
        const m = (method || 'GET').toUpperCase();
        return m !== 'GET' && m !== 'HEAD' && m !== 'OPTIONS' && m !== 'TRACE';
    }

    window.fetch = function patchedFetch(input, init) {
        try {
            const originalUrl = typeof input === 'string' ? input : (input && input.url);
            const url = toBackendUrl(originalUrl);
            const method = (init && init.method) || (input && input.method) || 'GET';
            const isLoginRequest = isAuthLoginRequest(originalUrl || url);

            if (isSameBackend(url)) {
                const roleContext = getRoleContext();
                const accessToken = isLoginRequest ? null : getAccessToken();
                const token = needsCsrf(method) ? getCsrfToken() : null;
                if (token || roleContext || accessToken) {
                    const headers = new Headers((init && init.headers) || (input && input.headers) || undefined);
                    if (token && !headers.has('X-XSRF-TOKEN')) {
                        headers.set('X-XSRF-TOKEN', token);
                    }
                    if (roleContext && !headers.has('X-Role-Context')) {
                        headers.set('X-Role-Context', roleContext);
                    }
                    if (accessToken && !headers.has('Authorization')) {
                        headers.set('Authorization', `Bearer ${accessToken}`);
                    }
                    init = Object.assign({}, init, { headers });
                }
            }
            if (typeof input === 'string') {
                input = url;
            } else if (input && input.url && url !== input.url) {
                input = new Request(url, input);
            }
        } catch (e) {
            // 不影响主流程
            console.warn('[csrf-patch] failed to attach token:', e);
        }
        return originalFetch(input, init);
    };
    window.__csrfFetchPatched = true;
})();

// API服务类
class APIService {
    constructor() {
        this.baseUrl = API_BASE_URL;
    }
    
    async request(url, options = {}, retryState = { attemptedRefresh: false }) {
        try {
            // 后端使用基于Session的认证，不需要Token
            console.log('API请求URL:', url);
            const csrfToken = getCsrfToken();
            const roleContext = getRoleContext();
            const isLoginRequest = isAuthLoginRequest(url);
            if (!isLoginRequest && !retryState.attemptedRefresh) {
                const currentToken = getAccessToken();
                if (currentToken && isJwtLikelyExpired(currentToken)) {
                    await tryRefreshAuthSession();
                }
            }
            const headers = {
                ...(options.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
                ...options.headers
            };
            const accessToken = isLoginRequest ? null : getAccessToken();
            if (csrfToken && !headers['X-XSRF-TOKEN']) {
                headers['X-XSRF-TOKEN'] = csrfToken;
            }
            if (roleContext && !headers['X-Role-Context']) {
                headers['X-Role-Context'] = roleContext;
            }
            if (accessToken && !headers['Authorization']) {
                headers['Authorization'] = `Bearer ${accessToken}`;
            }
            
            const response = await fetch(`${this.baseUrl}${url}`, {
                credentials: 'include',
                ...options,
                headers
            });
            
            console.log('API Response:', response);
            
            // 解析响应数据
            // 204 No Content 响应没有响应体，不需要解析JSON
            let data;
            if (response.status === 204 || response.status === 201) {
                // 204 No Content 或 201 Created，返回成功响应
                data = {
                    success: true,
                    code: response.status,
                    message: response.status === 204 ? '操作成功' : '创建成功',
                    data: null
                };
                console.log('API Response Data:', data);
            } else {
                try {
                    const text = await response.text();
                    if (text) {
                        data = JSON.parse(text);
                        console.log('API Response Data:', data);
                    } else {
                        // 空响应体，返回默认成功响应
                        data = {
                            success: response.ok,
                            code: response.status,
                            message: response.ok ? '操作成功' : '操作失败',
                            data: null
                        };
                    }
                } catch (jsonError) {
                    console.error('JSON Parse Error:', jsonError);
                    // 如果解析失败，检查HTTP状态码
                    if (response.ok) {
                        data = {
                            success: true,
                            code: response.status,
                            message: '操作成功',
                            data: null
                        };
                    } else {
                        throw new Error(`Failed to parse response: ${jsonError.message}`);
                    }
                }
            }
            
            // 统一处理未授权/禁止访问，便于前端页面重定向
            const isLoginApi = url.startsWith('/api/auth/login');
            const unauthorized = !isLoginApi && (
                response.status === 401 ||
                response.status === 403 ||
                data?.code === 401 ||
                data?.code === 403
            );
            if (unauthorized) {
                const tokenExpired = typeof (data?.message || data?.error) === 'string'
                    && (data.message || data.error).includes('JWT 已过期');
                if (!retryState.attemptedRefresh && tokenExpired) {
                    const refreshed = await tryRefreshAuthSession();
                    if (refreshed) {
                        return this.request(url, options, { attemptedRefresh: true });
                    }
                }
                if (!shouldSilenceUnauthorizedLog(url)) {
                    console.error('未授权或权限不足，请重新登录');
                }
                return {
                    success: false,
                    code: response.status || data?.code || 401,
                    message: data?.message || data?.error || '未授权或权限不足，请重新登录',
                    data: null,
                    raw: data
                };
            }
            
            // 检查HTTP状态码，201也是成功状态
            if (!response.ok && response.status !== 201) {
                const message = data?.message || data?.error || `HTTP error! status: ${response.status}`;
                return {
                    success: false,
                    code: response.status,
                    message,
                    data: data?.data ?? null,
                    raw: data
                };
            }
            
            // 直接返回数据，不检查success字段，让调用者根据业务逻辑处理
            return data;
        } catch (error) {
            console.error('API Request Error:', error);
            // 显示错误消息
            const messageContainer = document.getElementById('messageContainer');
            if (messageContainer) {
                messageContainer.innerHTML = `<div class="error-message">${error.message}</div>`;
                messageContainer.style.display = 'block';
                setTimeout(() => {
                    messageContainer.style.display = 'none';
                }, 5000);
            }
            return null;
        }
    }
    
    get(url, params = {}) {
        // 只保留非undefined、非null、非空字符串和非字符串'undefined'的参数
        const filteredParams = {};
        for (const [key, value] of Object.entries(params)) {
            if (value !== undefined && value !== null && value !== '' && value !== 'undefined') {
                filteredParams[key] = value;
            }
        }
        const queryString = new URLSearchParams(filteredParams).toString();
        return this.request(`${url}${queryString ? `?${queryString}` : ''}`);
    }
    
    post(url, data = {}) {
        return this.request(url, {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }
    
    put(url, data = {}) {
        return this.request(url, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    }
    
    delete(url) {
        return this.request(url, {
            method: 'DELETE'
        });
    }
    
    // 上报浏览器错误
    async reportBrowserError(error) {
        try {
            const errorData = {
                errorType: error.errorType || 'Error',
                errorMessage: error.message || error.errorMessage || '未知错误',
                errorStack: error.stack || error.errorStack || '',
                pageUrl: window.location.href,
                lineNumber: error.lineNumber || error.lineno || 0,
                columnNumber: error.columnNumber || error.colno || 0,
                fileUrl: error.fileUrl || error.filename || '',
                userAgent: navigator.userAgent
            };
            return this.post('/api/errors/browser', errorData);
        } catch (reportError) {
            console.error('上报错误失败:', reportError);
            return null;
        }
    }
}

// 全局错误捕获
window.addEventListener('error', function(errorEvent) {
    if (!window.__ENABLE_BROWSER_ERROR_REPORTING__) {
        return;
    }
    const error = {
        errorType: 'JavaScript Error',
        errorMessage: errorEvent.message,
        errorStack: errorEvent.error ? errorEvent.error.stack : '',
        lineNumber: errorEvent.lineno,
        columnNumber: errorEvent.colno,
        fileUrl: errorEvent.filename
    };
    
    // 检查apiService是否已存在，避免重复声明
    const apiServiceInstance = typeof apiService !== 'undefined' && apiService !== null ? 
        apiService : new APIService();
    apiServiceInstance.reportBrowserError(error).catch(reportError => {
        console.error('上报JavaScript错误失败:', reportError);
    });
});

// 全局未处理Promise拒绝捕获
window.addEventListener('unhandledrejection', function(promiseRejectionEvent) {
    if (!window.__ENABLE_BROWSER_ERROR_REPORTING__) {
        return;
    }
    const error = {
        errorType: 'Unhandled Promise Rejection',
        errorMessage: promiseRejectionEvent.reason ? 
            (promiseRejectionEvent.reason.message || String(promiseRejectionEvent.reason)) : 
            '未知Promise拒绝',
        errorStack: promiseRejectionEvent.reason ? 
            (promiseRejectionEvent.reason.stack || '') : 
            ''
    };
    
    // 检查apiService是否已存在，避免重复声明
    const apiServiceInstance = typeof apiService !== 'undefined' && apiService !== null ? 
        apiService : new APIService();
    apiServiceInstance.reportBrowserError(error).catch(reportError => {
        console.error('上报Promise拒绝错误失败:', reportError);
    });
});

// 认证API类
class AuthAPI {
    constructor(apiService) {
        this.apiService = apiService;
    }
    
    async login(username, password, captcha, captchaKey) {
        const result = await this.apiService.post('/api/auth/login', {
            username,
            password,
            captcha,
            captchaKey
        });

        if (result?.success && result?.data) {
            persistAuthSession(result.data);
        }

        return result;
    }
    
    async getCurrentUser() {
        return this.apiService.get('/api/auth/me');
    }
    
    logout() {
        // 清除sessionStorage中的用户信息（每个标签页独立）
        clearAuthSession();
        // 跳转到登录页面
        window.location.href = 'index.html';
    }
}

// 学生API类
class StudentAPI {
    constructor(apiService) {
        this.apiService = apiService;
    }
    
    // 学生相关API方法
    getStudentCourses(params = {}) {
        return this.apiService.get('/api/student/courses', params);
    }
    
    getCourseDetail(courseId) {
        return this.apiService.get(`/api/student/courses/${courseId}`);
    }
    
    getCourseProgress() {
        return guardStudentCapability(
            'dashboardPerformance',
            () => this.apiService.get('/api/student/course-progress'),
            '当前 JWT 微服务环境暂未提供课程进度接口，页面将改用已接通的数据源进行统计。'
        );
    }
    
    getAssignments(params = {}) {
        return this.apiService.get('/api/student/assignments', params);
    }
    
    getAssignmentDetail(assignmentId) {
        return this.apiService.get(`/api/student/assignments/${assignmentId}`);
    }
    
    getExams(params = {}) {
        return guardStudentCapability(
            'exams',
            () => this.apiService.get('/api/student/exams', params)
        );
    }
    
    getExamDetail(examId) {
        return guardStudentCapability(
            'examDetail',
            () => this.apiService.get(`/api/student/exams/${examId}`)
        );
    }
    
    getScores() {
        return guardStudentCapability(
            'scores',
            () => this.apiService.get('/api/student/scores')
        );
    }
    
    getStudentStats() {
        return this.apiService.get('/api/student/stats');
    }
    
    getStudyTimeDistribution() {
        return this.apiService.get('/api/student/study-time-distribution');
    }
    
    getKnowledgePoints() {
        return this.apiService.get('/api/student/knowledge-points');
    }
    
    getRecentActivities() {
        return guardStudentCapability(
            'activities',
            () => this.apiService.get('/api/student/activities'),
            '当前 JWT 微服务环境暂未提供学生活动流接口。'
        );
    }
    
    getCurrentStudentPerformance() {
        return guardStudentCapability(
            'dashboardPerformance',
            () => this.apiService.get('/api/dashboard/student-performance'),
            '当前 JWT 微服务环境暂未提供学生综合表现接口。'
        );
    }
    
    // 获取学生的考试提交记录
    getExamSubmissions() {
        return guardStudentCapability(
            'examSubmit',
            () => this.apiService.get('/api/student/exam-submissions')
        );
    }
    
    // 获取学生的作业提交记录
    getAssignmentSubmissions() {
        return this.apiService.get('/api/student/assignment-submissions');
    }
    
    submitAssignment(assignmentId, content) {
        return this.apiService.post(`/api/student/assignments/${assignmentId}/submit`, {
            content
        });
    }
    
    // 提交考试（支持FormData，包含文件上传）
    submitExam(examId, formData) {
        return guardStudentCapability(
            'examSubmit',
            () => this.apiService.request(`/api/student/exams/${examId}/submit`, {
                method: 'POST',
                body: formData,
                headers: {}
            })
        );
    }
    
    // 提交考试（JSON格式，兼容当前后端API）
    submitExamJson(examId, data) {
        return guardStudentCapability(
            'examSubmit',
            () => this.apiService.post(`/api/student/exams/${examId}/submit`, data)
        );
    }
    
    // 通知相关API方法
    getNotifications(page = 1, size = 10, filter = 'all') {
        const studentId = getCurrentUserId();
        return this.apiService.get(`/api/notifications/student`, {
            studentId,
            page,
            size,
            filter
        });
    }
    
    getAllNotifications() {
        const studentId = getCurrentUserId();
        return this.apiService.get(`/api/notifications/student/all`, {
            studentId
        });
    }
    
    getUnreadNotificationCount() {
        const studentId = getCurrentUserId();
        return this.apiService.get(`/api/notifications/student/unread-count`, {
            studentId
        });
    }
    
    markNotificationAsRead(notificationId) {
        const studentId = getCurrentUserId();
        const query = studentId ? `?studentId=${encodeURIComponent(studentId)}` : '';
        return this.apiService.put(`/api/notifications/${notificationId}/read${query}`);
    }
    
    markAllNotificationsAsRead() {
        const studentId = getCurrentUserId();
        const query = studentId ? `?studentId=${encodeURIComponent(studentId)}` : '';
        return this.apiService.put(`/api/notifications/read-all${query}`);
    }
    
    deleteNotification(notificationId) {
        const studentId = getCurrentUserId();
        const query = studentId ? `?studentId=${encodeURIComponent(studentId)}` : '';
        return this.apiService.delete(`/api/notifications/${notificationId}${query}`);
    }
    
    deleteAllReadNotifications() {
        const studentId = getCurrentUserId();
        const query = studentId ? `?studentId=${encodeURIComponent(studentId)}` : '';
        return this.apiService.delete(`/api/notifications/delete-all-read${query}`);
    }
    
    // 设置相关API方法
    
    // 获取学生个人信息
    getStudentProfile() {
        return this.apiService.get('/api/student/profile');
    }
    
    // 更新学生个人信息
    updateStudentProfile(data) {
        return this.apiService.put('/api/student/profile', data);
    }
    
    // 修改学生密码
    changePassword(currentPassword, newPassword, confirmPassword) {
        return this.apiService.post('/api/auth/change-password', {
            currentPassword,
            newPassword
        });
    }
    
    // 获取学生通知设置
    getNotificationSettings() {
        return this.apiService.get('/api/student/notification-settings');
    }
    
    // 更新学生通知设置
    updateNotificationSettings(data) {
        return this.apiService.put('/api/student/notification-settings', data);
    }
    
    // 获取学生隐私设置
    getPrivacySettings() {
        return this.apiService.get('/api/student/privacy-settings');
    }
    
    // 更新学生隐私设置
    updatePrivacySettings(data) {
        return this.apiService.put('/api/student/privacy-settings', data);
    }
    
    // 上传学生头像
    uploadAvatar(avatarData) {
        return guardStudentCapability(
            'avatarUpload',
            () => this.apiService.post('/api/student/upload-avatar', avatarData),
            '当前 JWT 微服务环境已接通头像上传接口。'
        );
    }
    
    // 导出学生数据
    exportStudentData() {
        return this.apiService.get('/api/student/export-data', {
            responseType: 'blob'
        });
    }
}

// 教师API类
class TeacherAPI {
    constructor(apiService) {
        this.apiService = apiService;
    }
    
    // 教师相关API方法
    getCourses(params = {}) {
        return this.apiService.get('/api/teacher/courses', params);
    }
    
    getCourseById(courseId) {
        return this.apiService.get(`/api/teacher/courses/${courseId}`);
    }
    
    createCourse(data) {
        return this.apiService.post('/api/teacher/courses', data);
    }
    
    updateCourse(courseId, data) {
        return this.apiService.put(`/api/teacher/courses/${courseId}`, data);
    }
    
    deleteCourse(courseId) {
        return this.apiService.delete(`/api/teacher/courses/${courseId}`);
    }
    
    getClasses(params = {}) {
        return this.apiService.get('/api/teacher/classes', params);
    }
    
    createClass(data) {
        return this.apiService.post('/api/teacher/classes', data);
    }
    
    updateClass(classId, data) {
        return this.apiService.put(`/api/teacher/classes/${classId}`, data);
    }
    
    getClassAssignments(params = {}) {
        return this.apiService.get('/api/teacher/course-assignments', params);
    }
    
    assignCourse(data) {
        return this.apiService.post('/api/teacher/course-assignments', data);
    }
    
    unassignCourse(assignmentId) {
        return this.apiService.delete(`/api/teacher/course-assignments/${assignmentId}`);
    }
    
    deleteClass(classId) {
        return this.apiService.delete(`/api/teacher/classes/${classId}`);
    }
    
    getAssignments(params = {}) {
        return this.apiService.get('/api/teacher/assignments', params);
    }
    
    getExams(params = {}) {
        return this.apiService.get('/api/teacher/exams', params);
    }
    
    getSubmissions(params = {}) {
        return this.apiService.get('/api/teacher/submissions', params);
    }
    
    getTeacherDashboard() {
        return buildTeacherDashboardResponse();
    }
    
    // 新增方法：创建作业
    createAssignment(data) {
        return this.apiService.post('/api/teacher/assignments', data);
    }
    
    // 新增方法：更新作业
    updateAssignment(assignmentId, data) {
        // 构建与后端期望格式一致的请求数据（使用驼峰命名，与后端Controller一致）
        const requestData = {
            title: data.title,
            description: data.description,
            courseId: data.courseId,
            dueDate: data.dueDate,
            publishDate: data.publishDate,
            isActive: data.isActive !== undefined ? data.isActive : true
        };
        
        // 如果有maxScore，也添加进去
        if (data.maxScore !== undefined) {
            requestData.maxScore = data.maxScore;
        }
        
        return this.apiService.put(`/api/teacher/assignments/${assignmentId}`, requestData);
    }

    // 新增方法：获取作业详情
    getAssignmentById(assignmentId) {
        return this.apiService.get(`/api/teacher/assignments/${assignmentId}`);
    }

    // 新增方法：获取考试详情
    getExamById(examId) {
        return this.apiService.get(`/api/teacher/exams/${examId}`);
    }
    
    // 新增方法：创建考试
    createExam(data) {
        // 构建与后端期望格式一致的请求数据
        const requestData = {
            title: data.title,
            description: data.description,
            courseId: data.courseId,
            startTime: data.startTime,
            endTime: data.endTime,
            publishDate: data.publishDate,
            duration: data.duration,
            isActive: data.isActive,
            isOnline: data.isOnline,
            location: data.location
        };
        if (Array.isArray(data.knowledgePointIds)) {
            requestData.knowledgePointIds = data.knowledgePointIds;
        }
        return this.apiService.post('/api/teacher/exams', requestData);
    }
    
    // 新增方法：更新考试
    updateExam(examId, data) {
        // Spring Cloud exam-service 的 TeacherExamUpsertRequestDTO 使用 camelCase 字段；
        // 单体 ExamController 同时兼容 camelCase，因此这里与创建考试保持一致，避免编辑保存时 courseId/startTime 等字段绑定失败。
        const requestData = {
            title: data.title,
            description: data.description,
            courseId: data.courseId,
            startTime: data.startTime,
            endTime: data.endTime,
            publishDate: data.publishDate,
            duration: data.duration,
            isActive: data.isActive,
            isOnline: data.isOnline,
            location: data.location
        };
        if (Array.isArray(data.knowledgePointIds)) {
            requestData.knowledgePointIds = data.knowledgePointIds;
        }
        if (Array.isArray(data.questions)) {
            requestData.questions = data.questions;
        }
        return this.apiService.put(`/api/teacher/exams/${examId}`, requestData);
    }
    
    // 新增方法：评分提交
    gradeSubmission(data) {
        return this.apiService.put(`/api/teacher/submissions/${data.submissionId}/grade`, data);
    }
    
    // 新增方法：删除作业
    deleteAssignment(assignmentId) {
        return this.apiService.delete(`/api/teacher/assignments/${assignmentId}`);
    }
    
    // 新增方法：删除考试
    deleteExam(examId) {
        return this.apiService.delete(`/api/teacher/exams/${examId}`);
    }
    
    // 新增方法：发送通知
    sendNotification(data) {
        const teacherId = getCurrentUserId();
        const query = teacherId ? `?teacherId=${encodeURIComponent(teacherId)}` : '';
        return this.apiService.post(`/api/notifications/teacher/send${query}`, data);
    }
    
    // 新增方法：批量发送通知
    sendBatchNotification(data) {
        return this.apiService.post('/api/notifications/teacher/send-batch', data);
    }
    
    // 新增方法：获取学习汇总数据
    getLearningSummary(params = {}) {
        return this.apiService.get('/api/teacher/learning-summary', params);
    }
    
    // 新增方法：获取成绩趋势数据
    getScoreTrend(params = {}) {
        return this.apiService.get('/api/teacher/score-trend', params);
    }
    
    // 新增方法：获取学生详情
    getStudentDetails(studentId) {
        return this.apiService.get(`/api/teacher/students/${studentId}`);
    }
    
    // 新增方法：获取学生列表
    getStudents(params = {}) {
        if (params.classId) {
            return this.apiService.get(`/api/teacher/classes/${params.classId}/students`);
        } else if (params.courseId) {
            return this.apiService.get(`/api/teacher/courses/${params.courseId}/students`);
        } else {
            // 如果没有参数，返回空数组，因为后端没有实现无参数的学生列表接口
            return Promise.resolve({ code: 200, data: [] });
        }
    }
    
    // 新增方法：获取知识点列表
    getKnowledgePoints(params = {}) {
        return this.apiService.get('/api/teacher/knowledge-points', params);
    }

    getKnowledgePointsByCourse(courseId) {
        return this.apiService.get(`/api/teacher/knowledge-points/course/${courseId}`);
    }
    
    // 新增方法：获取知识点分析数据
    getKnowledgePointAnalysis(params = {}) {
        return this.apiService.get('/api/knowledge-points/analysis/teacher/course', params);
    }
    
    // 新增方法：更新学生信息
    updateStudent(studentId, data) {
        return this.apiService.put(`/api/teacher/students/${studentId}`, data);
    }

    // 新增方法：向班级添加学生（支持学生ID或用户名）
    addStudentToClass(classId, data) {
        return this.apiService.post(`/api/teacher/classes/${classId}/students`, data);
    }
    
    // 新增方法：获取教师仪表盘数据
    getDashboard(params = {}) {
        return buildTeacherDashboardResponse(params);
    }
    
    // 新增方法：获取学情预警列表
    getEarlyWarnings(params = {}) {
        return this.apiService.get('/api/early-warnings/teacher/list', params);
    }
}

// 显示加载状态
function showLoading() {
    const loadingOverlay = document.getElementById('loadingOverlay');
    if (loadingOverlay) {
        loadingOverlay.style.display = 'flex';
    }
}

// 隐藏加载状态
function hideLoading() {
    const loadingOverlay = document.getElementById('loadingOverlay');
    if (loadingOverlay) {
        loadingOverlay.style.display = 'none';
    }
}

// 显示消息
function showMessage(message, type = 'error') {
    const messageContainer = document.getElementById('messageContainer');
    if (messageContainer) {
        messageContainer.innerHTML = `
            <div class="${type}-message">
                ${message}
            </div>
        `;
        const messageElement = messageContainer.querySelector(`.${type}-message`);
        if (messageElement) {
            messageElement.style.display = 'block';
            setTimeout(() => {
                messageElement.style.display = 'none';
            }, 3000);
        }
    }
}

// API请求函数
async function fetchAPI(url, options = {}) {
    console.log('=== 开始API请求 ===');
    console.log('1. 请求URL:', url);
    console.log('2. 请求选项:', options);
    
    showLoading();
    try {
        // 确保URL格式正确，移除重复的/api前缀
        const apiUrl = toBackendUrl(url).replace(/\/api\/api/g, '/api');
        console.log('3. API_BASE_URL:', API_BASE_URL);
        console.log('4. 完整请求URL:', apiUrl);
        
        const accessToken = getAccessToken();
        console.log('5. 当前Token状态:', accessToken ? '已获取' : '未获取');
        
        // 发送请求
        console.log('6. 发送请求...');
        const response = await fetch(apiUrl, {
            credentials: 'include',
            ...options,
            headers: {
                'Content-Type': 'application/json',
                ...(accessToken ? { 'Authorization': `Bearer ${accessToken}` } : {}),
                ...options.headers
            }
        });
        
        console.log('7. 响应状态:', response.status);
        console.log('8. 响应状态文本:', response.statusText);
        console.log('9. 响应头:', [...response.headers]);
        
        // 尝试解析响应体
        let data;
        try {
            console.log('10. 开始解析响应体...');
            data = await response.json();
            console.log('11. 解析响应体成功:', data);
        } catch (jsonError) {
            console.error('11. 解析响应体失败:', jsonError);
            // 处理非JSON响应
            const text = await response.text().catch(() => '');
            console.log('12. 响应体文本:', text);
            
            // 手动构建响应对象
            data = {
                success: response.ok,
                message: text || `HTTP error! status: ${response.status}`,
                code: response.status
            };
            console.log('13. 手动构建的响应对象:', data);
        }
        
        // 检查响应是否成功
        if (!data.success) {
            console.error('14. API请求失败，success为false');
            // 处理401未授权错误
            if (response.status === 401 || data.code === 401) {
                console.error('15. 401未授权错误');
                showMessage('请先登录', 'error');
                // 跳转到登录页面
                setTimeout(() => {
                    window.location.href = 'index.html';
                }, 1500);
                throw new Error('未授权访问');
            }
            const errorMessage = data.message || `HTTP error! status: ${response.status}`;
            console.error('16. 抛出错误:', errorMessage);
            throw new Error(errorMessage);
        }
        
        console.log('17. API请求成功');
        console.log('18. 响应对象:', data);
        
        // 根据响应格式返回数据
        const returnData = data.data !== undefined ? data.data : data;
        console.log('19. 返回数据:', returnData);
        
        return returnData;
    } catch (error) {
        console.error('20. API请求异常:', error);
        console.error('21. 异常消息:', error.message);
        console.error('22. 异常堆栈:', error.stack);
        
        // 只抛出错误，不显示消息，让调用者处理
        throw error;
    } finally {
        hideLoading();
        console.log('=== API请求结束 ===');
    }
}

// TeacherAPI类已在上方定义，此处不再重复定义

// 动态加载课程下拉列表
async function loadCourses() {
    console.log('=== 开始加载课程流程 ===');
    try {
        // 直接使用浏览器的fetch API发送请求，不使用封装的fetchAPI函数
        const apiUrl = '/api/teacher/courses';
        console.log('1. API请求URL:', apiUrl);
        
        // 发送请求
        const response = await fetch(apiUrl, {
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        console.log('2. 响应状态:', response.status);
        
        // 解析响应体
        const data = await response.json();
        console.log('3. 响应数据:', data);
        
        // 检查响应是否成功
        if (data.success) {
            // 获取课程列表，并确保它是数组
            // 处理分页响应：课程列表可能在data.data.content中（分页）或直接在data.data中（不分页）
            let courses = [];
            if (data.data) {
                if (Array.isArray(data.data)) {
                    // 不分页响应：直接使用data.data作为课程列表
                    courses = data.data;
                } else if (data.data.content && Array.isArray(data.data.content)) {
                    // 分页响应：提取data.data.content作为课程列表
                    courses = data.data.content;
                } else {
                    console.warn('课程数据不是预期格式，转换为空数组');
                    courses = [];
                }
            }
            console.log('4. 课程列表:', courses);
            console.log('5. 课程数量:', courses.length);
            
            // 只有在API调用成功时才更新courseMap和下拉列表
            // 清空并更新courseMap
            courseMap.clear();
            courses.forEach(course => {
                const courseId = course.id;
                const courseName = course.courseName || course.course_name || course.name || '未知课程';
                courseMap.set(courseId, courseName);
            });
            console.log('6. 课程映射表:', Array.from(courseMap.entries()));
            
            // 更新所有课程下拉列表
            updateCourseSelectors(courses);
        } else {
            console.warn('API请求失败，使用模拟数据');
            // API请求失败时，不清除现有的courseMap
        }
        
        console.log('=== 课程加载流程结束 ===');
    } catch (error) {
        console.error('=== 课程加载流程出错 ===');
        console.error('1. 错误对象:', error);
        console.error('2. 错误消息:', error.message);
        console.error('3. 错误堆栈:', error.stack);
        
        showMessage('加载课程失败: ' + error.message, 'error');
        
        console.log('=== 错误处理结束 ===');
    }
}

// 更新课程选择器
function updateCourseSelectors(courses) {
    const pageName = getCurrentPageName();
    const shouldWarnMissingSelector = !['teacher-notifications.html', 'teacher-settings.html'].includes(pageName);
    const courseSelectors = [
        '#assignment-course',
        '#exam-course',
        '#edit-assignment-course',
        '#edit-exam-course',
        '#add-assignment-course'
    ];
    
    courseSelectors.forEach(selector => {
        const select = document.querySelector(selector);
        if (select) {
            console.log('7. 更新课程选择器:', selector);
            
            // 清空所有选项
            select.innerHTML = '<option value="">请选择所属课程</option>';
            
            // 添加新选项
            if (courses.length > 0) {
                console.log('8. 添加课程选项，数量:', courses.length);
                courses.forEach((course, index) => {
                    // 检查课程对象是否包含必要字段
                    // 注意：后端返回的字段名可能是下划线命名法，需要转换为驼峰命名法
                    const courseId = course.id;
                    const courseName = course.courseName || course.course_name || course.name || '未知课程';
                    console.log(`10. 课程ID: ${courseId}, 课程名称: ${courseName}`);
                    console.log(`11. 课程对象完整字段: ${Object.keys(course).join(', ')}`);
                    
                    console.log(`9. 第${index + 1}个课程选项:`, courseName, courseId);
                    console.log(`10. 课程对象详情:`, course);
                    const option = document.createElement('option');
                    option.value = courseId;
                    option.textContent = courseName;
                    select.appendChild(option);
                });
            } else {
                console.warn('11. 没有课程数据');
                const option = document.createElement('option');
                option.value = '';
                option.textContent = '暂无课程数据';
                select.appendChild(option);
            }
        } else {
            if (shouldWarnMissingSelector) {
                console.warn('12. 课程选择器未找到:', selector);
            }
        }
    });
}

// 动态加载班级下拉列表
async function loadClasses() {
    try {
        console.log('=== 开始加载班级流程 ===');
        const accessToken = getAccessToken();
        // 尝试获取班级数据
        let classes = [];
        try {
            // 首先尝试使用teacher/classes接口
            const apiUrl = '/api/teacher/classes';
            console.log('1. 调用班级API:', apiUrl);
            
            const response = await fetch(apiUrl, {
                method: 'GET',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json',
                    ...(accessToken ? { 'Authorization': `Bearer ${accessToken}` } : {})
                }
            });
            
            console.log('2. 班级API响应状态:', response.status);
            const responseText = await response.text();
            console.log('3. 班级API响应文本:', responseText);
            
            const data = JSON.parse(responseText);
            console.log('4. 班级API响应JSON:', data);
            
            if (data.success) {
                classes = data.data || [];
                console.log('5. 班级列表:', classes);
            } else {
                throw new Error(data.message || '班级API请求失败');
            }
        } catch (error1) {
            console.error('/teacher/classes接口失败，尝试使用/teacher/courses接口获取班级信息:', error1);
            // 如果teacher/classes接口失败，尝试使用其他接口
            try {
                // 尝试从课程数据中获取班级信息
                const apiUrl = '/api/teacher/courses';
                const response = await fetch(apiUrl, {
                    method: 'GET',
                    credentials: 'include',
                    headers: {
                        'Content-Type': 'application/json',
                        ...(accessToken ? { 'Authorization': `Bearer ${accessToken}` } : {})
                    }
                });
                
                const data = await response.json();
                if (data.success) {
                    const courses = data.data || [];
                    // 从课程数据中提取班级信息（假设有班级关联）
                    const courseList = Array.isArray(courses) ? courses : (courses.content || []);
                    // 模拟班级数据
                    classes = courseList.map(course => ({
                        id: course.id,
                        className: course.courseName + '班'
                    }));
                    console.log('从课程数据中生成班级列表:', classes);
                } else {
                    throw new Error(data.message || '课程API请求失败');
                }
            } catch (error2) {
                console.error('所有班级接口都失败:', error2);
                throw error2;
            }
        }
        
        // 更新所有班级下拉列表
        const classSelectors = [
            '#assignment-class',
            '#exam-class',
            '#edit-assignment-class',
            '#edit-exam-class',
            '#editStudentClass'  // 添加编辑学生模态框的班级选择器
        ];
        
        // 确保classes是数组
        const classList = Array.isArray(classes) ? classes : (classes.content || []);
        console.log('最终班级列表:', classList);
        
        classSelectors.forEach(selector => {
            const select = document.querySelector(selector);
            if (select) {
                console.log('更新班级选择器:', selector);
                // 保存当前选中值
                const currentValues = Array.from(select.selectedOptions).map(option => option.value);
                
                // 清空所有选项
                select.innerHTML = '';
                
                // 添加新选项
                if (classList.length > 0) {
                    console.log('班级数量:', classList.length);
                    classList.forEach(cls => {
                        // 检查班级对象是否包含必要字段
                        const classId = cls.id || cls.classId;
                        const className = cls.className || cls.name || cls.title;
                        if (classId && className) {
                            console.log('添加班级选项:', className, classId);
                            const option = document.createElement('option');
                            option.value = classId;
                            option.textContent = className;
                            select.appendChild(option);
                        } else {
                            console.warn('班级缺少必要字段:', cls);
                        }
                    });
                } else {
                    console.warn('没有班级数据');
                    const option = document.createElement('option');
                    option.value = '';
                    option.textContent = '暂无班级数据';
                    select.appendChild(option);
                }
                
                // 恢复选中值
                currentValues.forEach(value => {
                    const option = select.querySelector(`option[value="${value}"]`);
                    if (option) {
                        option.selected = true;
                    }
                });
            } else {
                // 只在调试模式下显示警告，某些页面可能不需要所有选择器
                // console.warn('班级选择器未找到:', selector);
            }
        });
    } catch (error) {
        console.error('加载班级失败:', error);
        showMessage('加载班级失败: ' + error.message, 'error');
        
        // 在控制台显示完整错误信息，便于调试
        console.error('完整错误信息:', error);
    }
}

// 导出提交记录
function exportSubmissions() {
    // 实现导出功能
    console.log('导出提交记录');
    showMessage('当前交付环境暂未开放导出提交记录，请先在列表中完成筛选与批改。', 'success');
}

// 初始化全局API服务实例
const apiService = new APIService();
const authAPI = new AuthAPI(apiService);
const teacherAPI = new TeacherAPI(apiService);
const studentAPI = new StudentAPI(apiService);

// 将API服务实例挂载到window对象，供页面直接使用
window.apiService = apiService;
window.authAPI = authAPI;
window.teacherAPI = teacherAPI;
window.studentAPI = studentAPI;
window.getCurrentUserId = getCurrentUserId;
window.isJwtOnlyStudentSession = isJwtOnlyStudentSession;
window.getStudentSessionContext = getStudentSessionContext;
window.clearAuthSession = clearAuthSession;
window.persistAuthSession = persistAuthSession;
window.loadFrontendCapabilities = loadFrontendCapabilities;

// 提交发布考试功能已移至teacher-assignments.html文件中

function extractCollectionFromApiResponse(response, collectionKeys = []) {
    if (!response) {
        return [];
    }

    const payload = extractTeacherPayload(response);
    if (Array.isArray(payload)) {
        return payload;
    }

    const defaultKeys = ['content', 'items', 'records', ...collectionKeys];
    for (const key of defaultKeys) {
        if (Array.isArray(payload?.[key])) {
            return payload[key];
        }
    }

    return findFirstArrayInObject(payload);
}

function extractTeacherPayload(response) {
    let payload = response;
    const unwrapKeys = ['data', 'result', 'payload'];

    for (let depth = 0; depth < 5; depth += 1) {
        if (!payload || Array.isArray(payload) || typeof payload !== 'object') {
            break;
        }

        let unwrapped = false;
        for (const key of unwrapKeys) {
            const candidate = payload[key];
            if (candidate !== undefined && candidate !== null) {
                payload = candidate;
                unwrapped = true;
                break;
            }
        }

        if (!unwrapped) {
            break;
        }
    }

    return payload;
}

function findFirstArrayInObject(payload) {
    if (!payload || typeof payload !== 'object' || Array.isArray(payload)) {
        return [];
    }

    for (const value of Object.values(payload)) {
        if (Array.isArray(value)) {
            return value;
        }
    }

    return [];
}

function extractTeacherApiList(response, collectionKeys = []) {
    return extractCollectionFromApiResponse(response, [
        'list',
        'rows',
        'result',
        'results',
        'data',
        ...collectionKeys
    ]);
}

function extractTeacherSingleRecord(response) {
    if (!response) {
        return null;
    }

    const payload = extractTeacherPayload(response);
    if (Array.isArray(payload)) {
        return payload[0] ?? null;
    }

    if (payload && typeof payload === 'object') {
        const nestedKeys = ['record', 'item', 'detail'];
        for (const key of nestedKeys) {
            const candidate = payload[key];
            if (candidate && typeof candidate === 'object' && !Array.isArray(candidate)) {
                return candidate;
            }
        }
        return payload;
    }

    return null;
}

function isEndpointUnavailable(error) {
    const message = String(error?.message || '').toLowerCase();
    return message.includes('404')
        || message.includes('405')
        || message.includes('not found')
        || message.includes('method not allowed');
}

function normalizeStatusValue(record) {
    return String(
        record?.status ??
        record?.submissionStatus ??
        record?.state ??
        ''
    ).trim().toLowerCase();
}

function parseDateValue(value) {
    if (!value) {
        return null;
    }
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date;
}

function isFutureWithinDays(value, days) {
    const date = parseDateValue(value);
    if (!date) {
        return false;
    }
    const now = new Date();
    const diffMs = date.getTime() - now.getTime();
    return diffMs >= 0 && diffMs <= days * 24 * 60 * 60 * 1000;
}

function extractTeacherStudentCount(courses) {
    return courses.reduce((total, course) => {
        const directCount = Number(
            course?.studentCount ??
            course?.student_count ??
            course?.enrollmentCount ??
            course?.enrollment_count ??
            course?.studentTotal ??
            course?.student_total
        );
        if (Number.isFinite(directCount) && directCount >= 0) {
            return total + directCount;
        }
        if (Array.isArray(course?.students)) {
            return total + course.students.length;
        }
        if (Array.isArray(course?.classes)) {
            return total + course.classes.reduce((classTotal, cls) => {
                const classCount = Number(
                    cls?.studentCount ??
                    cls?.student_count ??
                    cls?.studentTotal ??
                    cls?.student_total
                );
                if (Number.isFinite(classCount) && classCount >= 0) {
                    return classTotal + classCount;
                }
                if (Array.isArray(cls?.students)) {
                    return classTotal + cls.students.length;
                }
                return classTotal;
            }, 0);
        }
        return total;
    }, 0);
}

function extractTeacherNumericValue(record, keys = [], fallback = 0) {
    for (const key of keys) {
        const value = Number(record?.[key]);
        if (Number.isFinite(value)) {
            return value;
        }
    }
    return fallback;
}

function getTeacherAssignmentCourseKey(assignment) {
    const courseId = assignment?.courseId ?? assignment?.course_id;
    if (courseId !== undefined && courseId !== null && courseId !== '') {
        return `id:${courseId}`;
    }
    const courseName = assignment?.courseName ?? assignment?.course?.courseName ?? assignment?.name;
    return courseName ? `name:${courseName}` : null;
}

function buildTeacherAssignmentCourseLookup(assignments) {
    const lookup = new Map();
    assignments.forEach(assignment => {
        if (assignment?.id === undefined || assignment?.id === null) {
            return;
        }
        const key = getTeacherAssignmentCourseKey(assignment);
        if (key) {
            lookup.set(String(assignment.id), key);
        }
    });
    return lookup;
}

function getTeacherSubmissionCourseKey(submission, assignmentCourseLookup) {
    const courseId = submission?.courseId ?? submission?.course_id;
    if (courseId !== undefined && courseId !== null && courseId !== '') {
        return `id:${courseId}`;
    }
    const courseName = submission?.courseName ?? submission?.course?.courseName;
    if (courseName) {
        return `name:${courseName}`;
    }
    const assignmentId = submission?.assignmentId ?? submission?.assignment_id;
    if (assignmentId !== undefined && assignmentId !== null) {
        return assignmentCourseLookup.get(String(assignmentId)) ?? null;
    }
    return null;
}

function resolveTeacherSubmissionScorePercent(submission) {
    const scoreRate = Number(submission?.scoreRate ?? submission?.score_rate);
    if (Number.isFinite(scoreRate) && scoreRate >= 0) {
        return scoreRate <= 1 ? scoreRate * 100 : scoreRate;
    }

    const score = Number(submission?.score);
    if (!Number.isFinite(score)) {
        return null;
    }

    const maxScore = Number(submission?.maxScore ?? submission?.max_score);
    if (Number.isFinite(maxScore) && maxScore > 0 && maxScore !== 100) {
        return (score / maxScore) * 100;
    }

    return score;
}

function buildTeacherCourseAverageScores(courses, submissions, assignments = []) {
    const assignmentCourseLookup = buildTeacherAssignmentCourseLookup(assignments);
    const scoresByCourse = new Map();

    submissions.forEach(submission => {
        const score = resolveTeacherSubmissionScorePercent(submission);
        if (!Number.isFinite(score)) {
            return;
        }

        const courseKey = getTeacherSubmissionCourseKey(submission, assignmentCourseLookup);
        if (!courseKey) {
            return;
        }

        const bucket = scoresByCourse.get(courseKey) ?? { total: 0, count: 0 };
        bucket.total += score;
        bucket.count += 1;
        scoresByCourse.set(courseKey, bucket);
    });

    return courses.map(course => {
        const directScore = Number(course?.averageScore ?? course?.avgScore ?? course?.avg_score ?? course?.courseAverageScore);
        if (Number.isFinite(directScore)) {
            return Math.round(directScore);
        }

        const courseKeys = [
            course?.id !== undefined && course?.id !== null ? `id:${course.id}` : null,
            course?.courseName || course?.name ? `name:${course.courseName || course.name}` : null
        ].filter(Boolean);

        for (const key of courseKeys) {
            const bucket = scoresByCourse.get(key);
            if (bucket && bucket.count > 0) {
                return Math.round(bucket.total / bucket.count);
            }
        }

        return 0;
    });
}

function getTeacherAssignmentTrendLabel(assignment) {
    const title = assignment?.title || assignment?.assignmentTitle || assignment?.name;
    if (title) {
        return title.length > 12 ? `${title.slice(0, 12)}...` : title;
    }

    const date = parseDateValue(assignment?.publishDate ?? assignment?.dueDate ?? assignment?.deadline ?? assignment?.createdAt);
    if (date) {
        return `${date.getMonth() + 1}/${date.getDate()}`;
    }

    return `作业${assignment?.id ?? ''}`.trim();
}

function buildTeacherAssignmentSubmissionTrend(assignments) {
    const assignmentRates = assignments
        .map(assignment => {
            const submittedCount = Number(
                assignment?.submittedCount ??
                assignment?.submissionCount ??
                assignment?.submitted_count ??
                assignment?.submission_count
            );
            const totalStudents = Number(
                assignment?.totalStudents ??
                assignment?.studentCount ??
                assignment?.total_students ??
                assignment?.student_count
            );

            if (!Number.isFinite(submittedCount) || !Number.isFinite(totalStudents) || totalStudents <= 0) {
                return null;
            }

            const timestamp = parseDateValue(
                assignment?.publishDate ?? assignment?.dueDate ?? assignment?.deadline ?? assignment?.createdAt
            )?.getTime() ?? 0;

            return {
                label: getTeacherAssignmentTrendLabel(assignment),
                rate: Math.min(Math.round((submittedCount / totalStudents) * 100), 100),
                timestamp
            };
        })
        .filter(Boolean)
        .sort((a, b) => a.timestamp - b.timestamp)
        .slice(-7);

    if (assignmentRates.length === 0) {
        return null;
    }

    return {
        submissionRateDays: assignmentRates.map(item => item.label),
        submissionRates: assignmentRates.map(item => item.rate)
    };
}

function buildTeacherRecentActivities(submissions, assignments, exams) {
    const submissionActivities = submissions.map(item => ({
        activityType: item?.graded ? '作业批改' : '作业提交',
        details: `${item?.studentName || '学生'}${item?.graded ? '的提交已批改' : '提交了作业'}：${item?.title || item?.assignmentTitle || '未命名作业'}`,
        activityDate: item?.submissionDate || item?.updatedAt || item?.createTime || new Date().toISOString()
    }));

    const assignmentActivities = assignments
        .filter(item => parseDateValue(item?.dueDate ?? item?.deadline ?? item?.endTime))
        .map(item => ({
            activityType: '作业通知',
            details: `作业「${item?.title || '未命名作业'}」截止时间临近`,
            activityDate: item?.dueDate ?? item?.deadline ?? item?.endTime
        }));

    const examActivities = exams
        .filter(item => parseDateValue(item?.examDate ?? item?.startTime ?? item?.date))
        .map(item => ({
            activityType: '考试通知',
            details: `考试「${item?.title || item?.examTitle || '未命名考试'}」已排期`,
            activityDate: item?.examDate ?? item?.startTime ?? item?.date
        }));

    return [...submissionActivities, ...assignmentActivities, ...examActivities]
        .filter(item => item?.details)
        .sort((a, b) => new Date(b.activityDate || 0) - new Date(a.activityDate || 0))
        .slice(0, 5);
}

function buildTeacherSubmissionTrend(assignments, submissions) {
    const assignmentTrend = buildTeacherAssignmentSubmissionTrend(assignments);
    if (assignmentTrend) {
        return assignmentTrend;
    }

    const submissionRateDays = Array.from({ length: 7 }, (_, index) => {
        const date = new Date();
        date.setDate(date.getDate() - (6 - index));
        return `${date.getMonth() + 1}/${date.getDate()}`;
    });

    const submissionsByDay = new Map(submissionRateDays.map(day => [day, 0]));
    submissions.forEach(item => {
        const date = parseDateValue(item?.submissionDate ?? item?.updatedAt ?? item?.createTime);
        if (!date) {
            return;
        }
        const key = `${date.getMonth() + 1}/${date.getDate()}`;
        if (submissionsByDay.has(key)) {
            submissionsByDay.set(key, submissionsByDay.get(key) + 1);
        }
    });

    const submissionBase = Math.max(assignments.length, 1);
    const submissionRates = submissionRateDays.map(day => {
        const count = submissionsByDay.get(day) || 0;
        return Math.min(Math.round((count / submissionBase) * 100), 100);
    });

    return {
        submissionRateDays,
        submissionRates
    };
}

async function buildTeacherDashboardSnapshot(params = {}, options = {}) {
    const apiService = new APIService();
    const teacherAPI = new TeacherAPI(apiService);
    const includeExams = shouldIncludeExamDataForTeacherStats(options);
    const includeWarnings = shouldIncludeEarlyWarningsForTeacherStats(options);
    const [coursesResult, assignmentsResult, examsResult, submissionsResult, warningsResult] = await Promise.allSettled([
        teacherAPI.getCourses(params),
        teacherAPI.getAssignments({ page: 1, size: 100 }),
        includeExams
            ? teacherAPI.getExams({ page: 1, size: 100 })
            : Promise.resolve({ success: true, data: { content: [] } }),
        teacherAPI.getSubmissions({ page: 1, size: 100 }),
        includeWarnings
            ? teacherAPI.getEarlyWarnings({ page: 1, size: 100 })
            : Promise.resolve({ success: true, data: { content: [] } })
    ]);

    const courses = coursesResult.status === 'fulfilled'
        ? extractTeacherApiList(coursesResult.value, ['courses'])
        : [];
    const assignments = assignmentsResult.status === 'fulfilled'
        ? extractTeacherApiList(assignmentsResult.value, ['assignments'])
        : [];
    const exams = examsResult.status === 'fulfilled'
        ? extractTeacherApiList(examsResult.value, ['exams'])
        : [];
    const submissions = submissionsResult.status === 'fulfilled'
        ? extractTeacherApiList(submissionsResult.value, ['submissions'])
        : [];
    const warnings = warningsResult.status === 'fulfilled'
        ? extractTeacherApiList(warningsResult.value, ['warnings'])
        : [];

    const pendingAssignments = assignments.filter(assignment => {
        const status = normalizeStatusValue(assignment);
        return !['graded', 'closed', 'completed', 'archived', 'deleted', 'cancelled'].includes(status);
    }).length;

    const pendingExams = exams.filter(exam => {
        const status = normalizeStatusValue(exam);
        if (status) {
            return ['pending', 'scheduled', 'upcoming', 'published', 'open', 'active'].includes(status);
        }
        return !!parseDateValue(exam?.examDate ?? exam?.startTime ?? exam?.date);
    }).length;

    const missingSubmissions = submissions.filter(submission => {
        const status = normalizeStatusValue(submission);
        if (status) {
            return !['graded', 'completed', 'reviewed'].includes(status);
        }
        return !(submission?.graded === true || submission?.score !== null && submission?.score !== undefined);
    }).length;

    const upcomingDeadlines = assignments.filter(assignment =>
        isFutureWithinDays(assignment?.dueDate ?? assignment?.deadline ?? assignment?.endTime, 7)
    ).length;
    const courseNames = courses.map(course => course?.courseName || course?.name || `课程${course?.id ?? ''}`.trim()).filter(Boolean);
    const averageScores = buildTeacherCourseAverageScores(courses, submissions, assignments);
    const recentActivities = buildTeacherRecentActivities(submissions, assignments, exams);
    const { submissionRateDays, submissionRates } = buildTeacherSubmissionTrend(assignments, submissions);

    return {
        totalCourses: courses.length,
        totalStudents: extractTeacherStudentCount(courses),
        pendingAssignments,
        pendingGrades: missingSubmissions,
        pendingExams,
        upcomingExams: pendingExams,
        missingSubmissions,
        upcomingDeadlines,
        warningCount: warnings.length,
        pendingAssignmentsChange: 0,
        pendingGradesChange: 0,
        pendingExamsChange: 0,
        upcomingExamsChange: 0,
        missingSubmissionsChange: 0,
        upcomingDeadlinesChange: 0,
        totalCoursesChange: 0,
        totalStudentsChange: 0,
        warningCountChange: 0,
        courseNames,
        averageScores,
        submissionRateDays,
        submissionRates,
        recentActivities
    };
}

async function buildTeacherDashboardResponse(params = {}, options = {}) {
    const data = await buildTeacherDashboardSnapshot(params, options);
    return {
        success: true,
        code: 200,
        message: '仪表盘数据聚合成功',
        data
    };
}

// 初始化函数
async function init() {
    // 检查当前页面是否是登录页面，如果是，则不调用需要授权的API
    const currentPage = window.location.pathname.split('/').pop();
    const isLoginPage = currentPage.includes('login') || currentPage === 'index.html';

    if (
        currentPage === 'teacher-assignments.html' ||
        currentPage === 'teacher-courses.html' ||
        currentPage === 'teacher-warning.html' ||
        currentPage === 'teacher-student-dashboard.html' ||
        currentPage === 'teacher-knowledge.html'
    ) {
        return;
    }
    
    // 检查是否是已经拥有页面级初始化逻辑的教师页面；这些页面不再走 api.js 的全局自动初始化
    const isTeacherPageWithOwnInit =
        currentPage === 'teacher-dashboard.html' ||
        currentPage === 'teacher-student-dashboard.html' ||
        currentPage === 'teacher-assignments.html' ||
        currentPage === 'teacher-courses.html' ||
        currentPage === 'teacher-notifications.html' ||
        currentPage === 'teacher-settings.html' ||
        currentPage === 'teacher-ai-tools.html' ||
        currentPage === 'teacher-warning.html' ||
        currentPage === 'teacher-knowledge.html';
    
    // 仅对教师相关页面（文件名以 teacher- 开头）执行自动加载，避免学生页面触发教师接口导致 403
    const isTeacherPage = currentPage.startsWith('teacher-');
    
    if (!isLoginPage && isTeacherPage && !isTeacherPageWithOwnInit) {
        const teacherBootTasks = [
            loadCourses(),
            loadClasses()
        ];

        if (currentPage === 'teacher-assignments.html') {
            teacherBootTasks.push(loadAssignments(), loadSubmissions());
        } else {
            teacherBootTasks.push(loadAssignments(), loadSubmissions());
        }

        await Promise.all(teacherBootTasks);

        // 初始化考试方式切换（仅教师考试管理页面需要）
        const examTypeSelects = [
            document.getElementById('exam-is-online'),
            document.getElementById('edit-exam-is-online')
        ];
        
        examTypeSelects.forEach(select => {
            if (select) {
                select.addEventListener('change', function() {
                    const locationGroup = this.id === 'exam-is-online' 
                        ? document.getElementById('exam-location-group') 
                        : document.getElementById('edit-exam-location-group');
                    
                    if (locationGroup) {
                        if (this.value === 'false') {
                            locationGroup.style.display = 'block';
                            locationGroup.querySelector('input').required = true;
                        } else {
                            locationGroup.style.display = 'none';
                            locationGroup.querySelector('input').required = false;
                        }
                    }
                });
                
                // 初始触发一次，确保正确显示/隐藏位置字段
                select.dispatchEvent(new Event('change'));
            }
        });
    }
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', init);

// 侧边栏切换
function initSidebar() {
    const toggleBtn = document.getElementById('toggleBtn');
    const sidebar = document.getElementById('sidebar');
    const mainContent = document.getElementById('mainContent');

    if (toggleBtn && sidebar && mainContent) {
        toggleBtn.addEventListener('click', () => {
            sidebar.classList.toggle('collapsed');
            mainContent.classList.toggle('collapsed');
        });
    }
}

// 在DOM加载完成后初始化侧边栏
initSidebar();

// 重置模态框表单
document.querySelectorAll('.modal').forEach(modal => {
    modal.addEventListener('hidden.bs.modal', function() {
        const form = this.querySelector('form');
        if (form) {
            form.reset();
        }
    });
});

// API服务实例已在文件上方初始化并挂载到window对象

// 为模态框添加表单验证
document.querySelectorAll('.modal form').forEach(form => {
    form.addEventListener('submit', function(e) {
        e.preventDefault();
    });
});

