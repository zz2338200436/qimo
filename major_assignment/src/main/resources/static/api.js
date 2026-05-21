// API基础URL
const API_BASE_URL = 'http://localhost:8080';

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
            const url = typeof input === 'string' ? input : (input && input.url);
            const method = (init && init.method) || (input && input.method) || 'GET';

            if (isSameBackend(url)) {
                const roleContext = getRoleContext();
                const token = needsCsrf(method) ? getCsrfToken() : null;
                if (token || roleContext) {
                    const headers = new Headers((init && init.headers) || (input && input.headers) || undefined);
                    if (token && !headers.has('X-XSRF-TOKEN')) {
                        headers.set('X-XSRF-TOKEN', token);
                    }
                    if (roleContext && !headers.has('X-Role-Context')) {
                        headers.set('X-Role-Context', roleContext);
                    }
                    init = Object.assign({}, init, { headers });
                }
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
    
    async request(url, options = {}) {
        try {
            // 后端使用基于Session的认证，不需要Token
            console.log('API请求URL:', url);
            const csrfToken = getCsrfToken();
            const roleContext = getRoleContext();
            const headers = {
                ...(options.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
                ...options.headers
            };
            if (csrfToken && !headers['X-XSRF-TOKEN']) {
                headers['X-XSRF-TOKEN'] = csrfToken;
            }
            if (roleContext && !headers['X-Role-Context']) {
                headers['X-Role-Context'] = roleContext;
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
                console.error('未授权或权限不足，请重新登录');
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
    
    async login(username, password, captcha) {
        return this.apiService.post('/api/auth/login', {
            username,
            password,
            captcha
        });
    }
    
    async getCurrentUser() {
        return this.apiService.get('/api/auth/me');
    }
    
    logout() {
        // 清除sessionStorage中的用户信息（每个标签页独立）
        sessionStorage.removeItem('user');
        sessionStorage.removeItem('token');
        sessionStorage.removeItem('userId');
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
        return this.apiService.get('/api/student/course-progress');
    }
    
    getAssignments(params = {}) {
        return this.apiService.get('/api/student/assignments', params);
    }
    
    getAssignmentDetail(assignmentId) {
        return this.apiService.get(`/api/student/assignments/${assignmentId}`);
    }
    
    getExams(params = {}) {
        return this.apiService.get('/api/student/exams', params);
    }
    
    getExamDetail(examId) {
        return this.apiService.get(`/api/student/exams/${examId}`);
    }
    
    getScores() {
        return this.apiService.get('/api/student/scores');
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
        return this.apiService.get('/api/student/activities');
    }
    
    getCurrentStudentPerformance() {
        return this.apiService.get('/api/dashboard/student-performance');
    }
    
    // 获取学生的考试提交记录
    getExamSubmissions() {
        return this.apiService.get('/api/student/exam-submissions');
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
        return this.apiService.request(`/api/student/exams/${examId}/submit`, {
            method: 'POST',
            body: formData,
            headers: {}
        });
    }
    
    // 提交考试（JSON格式，兼容当前后端API）
    submitExamJson(examId, data) {
        return this.apiService.post(`/api/student/exams/${examId}/submit`, data);
    }
    
    // 通知相关API方法
    getNotifications(page = 1, size = 10, filter = 'all') {
        return this.apiService.get(`/api/notifications/student`, {
            page,
            size,
            filter
        });
    }
    
    getAllNotifications() {
        return this.apiService.get(`/api/notifications/student/all`);
    }
    
    getUnreadNotificationCount() {
        return this.apiService.get(`/api/notifications/student/unread-count`);
    }
    
    markNotificationAsRead(notificationId) {
        return this.apiService.put(`/api/notifications/${notificationId}/read`);
    }
    
    markAllNotificationsAsRead() {
        return this.apiService.put(`/api/notifications/read-all`);
    }
    
    deleteNotification(notificationId) {
        return this.apiService.delete(`/api/notifications/${notificationId}`);
    }
    
    deleteAllReadNotifications() {
        return this.apiService.delete(`/api/notifications/delete-all-read`);
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
        return this.apiService.post('/api/student/change-password', {
            currentPassword,
            newPassword,
            confirmPassword
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
    uploadAvatar(formData) {
        return this.apiService.request('/api/student/upload-avatar', {
            method: 'POST',
            body: formData,
            headers: {}
        });
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
        return this.apiService.get('/api/teacher/dashboard');
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
            course_id: data.courseId, // 使用下划线命名，与数据库字段一致
            start_time: data.startTime, // 使用下划线命名，与数据库字段一致
            end_time: data.endTime, // 使用下划线命名，与数据库字段一致
            publish_date: data.publishDate, // 使用下划线命名，与数据库字段一致
            duration: data.duration,
            is_active: data.isActive, // 使用下划线命名，与数据库字段一致
            is_online: data.isOnline, // 使用下划线命名，与数据库字段一致
            location: data.location
        };
        return this.apiService.post('/api/teacher/exams', requestData);
    }
    
    // 新增方法：更新考试
    updateExam(examId, data) {
        // 构建与后端期望格式一致的请求数据
        const requestData = {
            title: data.title,
            description: data.description,
            course_id: data.courseId, // 使用下划线命名，与数据库字段一致
            start_time: data.startTime, // 使用下划线命名，与数据库字段一致
            end_time: data.endTime, // 使用下划线命名，与数据库字段一致
            publish_date: data.publishDate, // 使用下划线命名，与数据库字段一致
            duration: data.duration,
            is_active: data.isActive, // 使用下划线命名，与数据库字段一致
            is_online: data.isOnline, // 使用下划线命名，与数据库字段一致
            location: data.location
        };
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
        return this.apiService.post('/api/notifications/teacher/send', data);
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
    
    // 新增方法：获取知识点分析数据
    getKnowledgePointAnalysis(params = {}) {
        return this.apiService.get('/api/knowledge-points/analysis/teacher/course', params);
    }
    
    // 新增方法：更新学生信息
    updateStudent(studentId, data) {
        return this.apiService.put(`/api/teacher/students/${studentId}`, data);
    }
    
    // 新增方法：获取教师仪表盘数据
    getDashboard(params = {}) {
        return this.apiService.get('/api/teacher/dashboard', params);
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
        const apiUrl = `${API_BASE_URL}${url}`.replace(/\/api\/api/g, '/api');
        console.log('3. API_BASE_URL:', API_BASE_URL);
        console.log('4. 完整请求URL:', apiUrl);
        
        // 后端使用基于Session的认证，不需要Token
        console.log('5. 基于Session的认证，不需要Token');
        
        // 发送请求
        console.log('6. 发送请求...');
        const response = await fetch(apiUrl, {
            credentials: 'include',
            ...options,
            headers: {
                'Content-Type': 'application/json',
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
        const apiUrl = 'http://localhost:8080/api/teacher/courses';
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
            console.warn('12. 课程选择器未找到:', selector);
        }
    });
}

// 动态加载班级下拉列表
async function loadClasses() {
    try {
        console.log('=== 开始加载班级流程 ===');
        // 尝试获取班级数据
        let classes = [];
        try {
            // 首先尝试使用teacher/classes接口
            const apiUrl = 'http://localhost:8080/api/teacher/classes';
            console.log('1. 调用班级API:', apiUrl);
            
            const response = await fetch(apiUrl, {
                method: 'GET',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json'
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
                const apiUrl = 'http://localhost:8080/api/teacher/courses';
                const response = await fetch(apiUrl, {
                    method: 'GET',
                    credentials: 'include',
                    headers: {
                        'Content-Type': 'application/json'
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

// 加载作业列表
async function loadAssignments(page = 1, params = {}) {
    try {
        const tbody = document.getElementById('assignments-table-body');
        if (!tbody) return;
        
        // 显示加载状态
        tbody.innerHTML = '<tr><td colspan="7" class="text-center">加载中...</td></tr>';
        
        // 合并参数
        const requestParams = {
            page: page,
            size: 10,
            ...params
        };
        
        // 调用API获取作业列表
        const data = await window.teacherAPI.getAssignments(requestParams);
        console.log('作业列表响应数据:', data);
        
        // 处理API返回null的情况
        if (!data) {
            throw new Error('API请求失败，未返回数据');
        }
        
        // 检查API返回的success字段
        if (data.success === false) {
            throw new Error(data.message || 'API请求失败');
        }
        
        // 获取作业列表和分页信息
        const responseData = data.data || {};
        let assignments = responseData.content || [];
        const pageInfo = {
            pageNumber: page,
            totalPages: responseData.totalPages || 1,
            totalElements: responseData.totalElements || assignments.length,
            pageSize: responseData.size || 10
        };
        
        // 渲染作业列表
        renderAssignments(assignments);
        
        // 渲染分页控件
        const paginationContainer = document.getElementById('assignments-pagination');
        if (paginationContainer) {
            // 保存当前筛选条件到分页容器的dataset中，以便生成分页链接时使用
            paginationContainer.dataset.currentParams = JSON.stringify(params);
            renderPagination(paginationContainer, pageInfo, 'loadAssignments');
        }
    } catch (error) {
        console.error('Failed to load assignments:', error);
        const tbody = document.getElementById('assignments-table-body');
        if (tbody) {
            tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger">加载失败: ${error.message}</td></tr>`;
        }
    }
}

// 渲染分页控件
function renderPagination(container, pageInfo, loadFunction) {
    // 支持传入containerId或DOM元素
    if (typeof container === 'string') {
        container = document.getElementById(container);
    }
    
    if (!container || !pageInfo) return;
    
    const { pageNumber, totalPages, totalElements, pageSize } = pageInfo;
    
    // 获取保存的当前筛选条件
    const savedParams = container.dataset.currentParams || '{}';
    const params = JSON.parse(savedParams);
    // 将筛选条件转换为字符串形式，用于onclick事件
    const paramsStr = JSON.stringify(params).replace(/"/g, '&quot;');
    
    // 创建分页HTML
    let paginationHTML = `
            <li class="page-item ${pageNumber === 1 ? 'disabled' : ''}">
                <a class="page-link" onclick="${loadFunction}(${pageNumber - 1}, ${paramsStr})" aria-label="Previous">
                    <span aria-hidden="true">&laquo;</span>
                </a>
            </li>
    `;
    
    // 计算显示的页码范围
    let startPage = Math.max(1, pageNumber - 2);
    let endPage = Math.min(totalPages, pageNumber + 2);
    
    // 确保显示5个页码
    if (endPage - startPage < 4) {
        if (startPage === 1) {
            endPage = Math.min(totalPages, startPage + 4);
        } else if (endPage === totalPages) {
            startPage = Math.max(1, endPage - 4);
        }
    }
    
    // 添加页码
    for (let i = startPage; i <= endPage; i++) {
        paginationHTML += `
            <li class="page-item ${i === pageNumber ? 'active' : ''}">
                <a class="page-link" onclick="${loadFunction}(${i}, ${paramsStr})">${i}</a>
            </li>
        `;
    }
    
    paginationHTML += `
            <li class="page-item ${pageNumber === totalPages ? 'disabled' : ''}">
                <a class="page-link" onclick="${loadFunction}(${pageNumber + 1}, ${paramsStr})" aria-label="Next">
                    <span aria-hidden="true">&raquo;</span>
                </a>
            </li>
    `;
    
    container.innerHTML = paginationHTML;
}

// 渲染作业列表
function renderAssignments(assignmentsData) {
    const tbody = document.getElementById('assignments-table-body');
    if (!tbody) return;
    
    // 确保assignments是数组
    let assignments;
    if (Array.isArray(assignmentsData)) {
        assignments = assignmentsData;
    } else if (assignmentsData && Array.isArray(assignmentsData.content)) {
        // 处理分页格式的数据
        assignments = assignmentsData.content;
    } else {
        // 如果不是数组，默认为空数组
        assignments = [];
    }
    
    if (assignments.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center">暂无作业数据</td></tr>';
        return;
    }
    
    tbody.innerHTML = assignments.map(assignment => {
        // 计算作业状态
        let statusBadge = '';
        
        // 根据后端返回的status字段显示状态，确保与后端状态一致
        switch (assignment.status) {
            case 'pending':
                statusBadge = '<span class="badge badge-warning">待提交</span>';
                break;
            case 'submitted':
                statusBadge = '<span class="badge badge-primary">已提交</span>';
                break;
            case 'graded':
                statusBadge = '<span class="badge badge-success">已批改</span>';
                break;
            case 'closed':
                statusBadge = '<span class="badge badge-danger">已截止</span>';
                break;
            default:
                statusBadge = '<span class="badge badge-secondary">未知</span>';
        }
        
        // 处理课程名称
        const courseName = assignment.courseName || assignment.course_name || courseMap.get(assignment.courseId || assignment.course_id) || '未知课程';
        
        // 处理发布时间
        let publishDate = '未知';
        if (assignment.createdAt) {
            try {
                publishDate = new Date(assignment.createdAt).toLocaleDateString();
            } catch (e) {
                publishDate = assignment.createdAt;
            }
        } else if (assignment.publishDate) {
            try {
                publishDate = new Date(assignment.publishDate).toLocaleDateString();
            } catch (e) {
                publishDate = assignment.publishDate;
            }
        }
        
        // 处理截止时间
        let dueDateStr = '未知';
        const dueDate = assignment.dueDate;
        if (dueDate) {
            try {
                dueDateStr = new Date(dueDate).toLocaleDateString();
            } catch (e) {
                dueDateStr = dueDate;
            }
        }
        
        // 处理提交人数
        const submittedCount = assignment.submittedCount || assignment.submitted_count || 0;
        const totalStudents = assignment.totalStudents || assignment.total_students || 0;
        
        return `
            <tr>
                <td>${assignment.title}</td>
                <td>${courseName}</td>
                <td>${publishDate}</td>
                <td>${dueDateStr}</td>
                <td>${submittedCount}/${totalStudents}</td>
                <td>${statusBadge}</td>
                <td>
                    <div class="action-buttons">
                        <button class="btn" onclick="viewAssignment(${assignment.id})" style="background-color: #4f46e5; color: white; padding: 6px 12px; border-radius: 8px; font-size: 12px; border: none; margin-right: 4px;">
                            <i class="fa fa-eye"></i> 查看
                        </button>
                        <button class="btn" onclick="editAssignment(${assignment.id})" style="background-color: #e5e7eb; color: #374151; padding: 6px 12px; border-radius: 8px; font-size: 12px; border: 1px solid #d1d5db; margin-right: 4px;">
                            <i class="fa fa-edit"></i> 编辑
                        </button>
                        <button class="btn" onclick="gradeAssignment(${assignment.id})" style="background-color: #10b981; color: white; padding: 6px 12px; border-radius: 8px; font-size: 12px; border: none; margin-right: 4px;">
                            <i class="fa fa-check-circle"></i> 批改
                        </button>
                        <button class="btn" onclick="deleteAssignment(${assignment.id})" style="background-color: #ef4444; color: white; padding: 6px 12px; border-radius: 8px; font-size: 12px; border: none;">
                            <i class="fa fa-trash"></i> 删除
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

// 加载考试列表
async function loadExams(page = 1, params = {}) {
    try {
        const tbody = document.getElementById('exams-table-body');
        if (!tbody) return;
        
        // 显示加载状态
        tbody.innerHTML = '<tr><td colspan="7" class="text-center">加载中...</td></tr>';
        
        // 使用封装好的API服务
        const apiService = new APIService();
        const teacherAPI = new TeacherAPI(apiService);
        
        // 合并参数
        const requestParams = {
            page: page,
            size: 10,
            ...params
        };
        
        // 调用API获取考试列表
        const data = await teacherAPI.getExams(requestParams);
        console.log('考试列表响应数据:', data);
        
        // 处理API返回null的情况
        if (!data) {
            throw new Error('API请求失败，未返回数据');
        }
        
        // 获取考试列表和分页信息
        const responseData = data.data || {};
        let exams = responseData.content || [];
        const pageInfo = {
            pageNumber: page,
            totalPages: responseData.totalPages || 1,
            totalElements: responseData.totalElements || exams.length,
            pageSize: responseData.size || 10
        };
        
        // 渲染考试列表
        renderExams(exams);
        
        // 渲染分页控件
        const paginationContainer = document.getElementById('exams-pagination');
        if (paginationContainer) {
            // 保存当前筛选条件到分页容器的dataset中，以便生成分页链接时使用
            paginationContainer.dataset.currentParams = JSON.stringify(params);
            renderPagination(paginationContainer, pageInfo, 'loadExams');
        }
    } catch (error) {
        console.error('Failed to load exams:', error);
        const tbody = document.getElementById('exams-table-body');
        if (tbody) {
            tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger">加载失败: ${error.message}</td></tr>`;
        }
    }
}

// 渲染考试列表
function renderExams(examsData) {
    const tbody = document.getElementById('exams-table-body');
    if (!tbody) return;
    
    // 确保exams是数组
    let exams;
    if (Array.isArray(examsData)) {
        exams = examsData;
    } else if (examsData && Array.isArray(examsData.content)) {
        // 处理分页格式的数据
        exams = examsData.content;
    } else {
        // 如果不是数组，默认为空数组
        exams = [];
    }
    
    if (exams.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center">暂无考试数据</td></tr>';
        return;
    }
    
    tbody.innerHTML = exams.map(exam => {
        // 计算考试状态
        let statusBadge = '';
        const now = new Date();
        const startTime = new Date(exam.startTime);
        const endTime = new Date(exam.endTime);
        
        if (endTime < now) {
            statusBadge = '<span class="badge badge-danger">已结束</span>';
        } else if (startTime <= now && endTime >= now) {
            statusBadge = '<span class="badge badge-success">进行中</span>';
        } else {
            statusBadge = '<span class="badge badge-warning">即将开始</span>';
        }
        
        // 处理课程名称
        const courseName = exam.courseName || exam.course_name || courseMap.get(exam.courseId || exam.course_id) || '未知课程';
        
        // 处理提交人数
        const submittedCount = exam.submittedCount || exam.submitted_count || 0;
        const totalStudents = exam.totalStudents || exam.total_students || 0;
        
        return `
            <tr>
                <td>${exam.title}</td>
                <td>${courseName}</td>
                <td>${new Date(exam.startTime).toLocaleDateString()}</td>
                <td>${exam.duration}分钟</td>
                <td>${submittedCount}/${totalStudents}</td>
                <td>${statusBadge}</td>
                <td>
                    <div class="action-buttons">
                        <button class="btn btn-primary btn-sm" onclick="viewExam(${exam.id})">
                            <i class="fa fa-eye"></i> 查看
                        </button>
                        <button class="btn btn-secondary btn-sm" onclick="editExam(${exam.id})">
                            <i class="fa fa-edit"></i> 编辑
                        </button>
                        <button class="btn btn-success btn-sm" onclick="gradeExam(${exam.id})">
                            <i class="fa fa-check-circle"></i> 评分
                        </button>
                        <button class="btn btn-danger btn-sm" onclick="deleteExam(${exam.id})">
                            <i class="fa fa-trash"></i> 删除
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

// 加载提交记录列表
async function loadSubmissions(page = 1, params = {}) {
    try {
        const tbody = document.getElementById('submissions-table-body') || document.querySelector('#submissions-content tbody');
        if (!tbody) return;

        // 显示加载状态
        tbody.innerHTML = '<tr><td colspan="7" class="text-center">加载中...</td></tr>';

        // 构造请求参数（分页 + 筛选）
        const requestParams = {
            page: page,
            size: 10,
            ...params
        };

        // 调用教师提交记录接口，仅返回当前教师下的提交
        const apiService = new APIService();
        const teacherAPI = new TeacherAPI(apiService);
        const data = await teacherAPI.getSubmissions(requestParams);

        if (!data) {
            throw new Error('API请求失败，未返回数据');
        }
        if (data.success === false) {
            throw new Error(data.message || '加载提交记录失败');
        }

        const responseData = data.data || data;

        // 解析列表数据（兼容分页/非分页格式）
        let submissions = [];
        if (Array.isArray(responseData)) {
            submissions = responseData;
        } else if (Array.isArray(responseData.content)) {
            submissions = responseData.content;
        } else if (Array.isArray(responseData.submissions)) {
            submissions = responseData.submissions;
        }

        // 解析分页信息（兼容后端不同字段）
        const pageNumber = (responseData.pageable?.pageNumber ?? responseData.number ?? (page - 1)) + 1;
        const pageInfo = {
            pageNumber,
            totalPages: responseData.totalPages ?? responseData.total_pages ?? 1,
            totalElements: responseData.totalElements ?? responseData.total_elements ?? submissions.length,
            pageSize: responseData.size ?? responseData.pageSize ?? requestParams.size ?? 10
        };

        // 渲染表格与分页
        renderSubmissions(submissions);

        const paginationContainer = document.getElementById('submissions-pagination');
        if (paginationContainer) {
            // 保存当前筛选条件（确保下次分页/刷新时保留学生、作业/考试、状态筛选）
            paginationContainer.dataset.currentParams = JSON.stringify(params);
            renderPagination(paginationContainer, pageInfo, 'loadSubmissions');
        }
    } catch (error) {
        console.error('Failed to load submissions:', error);
        const tbody = document.getElementById('submissions-table-body') || document.querySelector('#submissions-content tbody');
        if (tbody) {
            tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger">加载失败: ${error.message}</td></tr>`;
        }
        showMessage('加载提交记录失败，请稍后重试：' + error.message, 'error');
    }
}

// 渲染提交记录列表
function renderSubmissions(submissionsData) {
    const tbody = document.querySelector('#submissions-content tbody');
    if (!tbody) return;
    
    // 确保submissions是数组
    let submissions;
    if (Array.isArray(submissionsData)) {
        submissions = submissionsData;
    } else if (submissionsData && Array.isArray(submissionsData.content)) {
        submissions = submissionsData.content;
    } else if (submissionsData && Array.isArray(submissionsData.submissions)) {
        submissions = submissionsData.submissions;
    } else {
        submissions = [];
    }
    
    tbody.innerHTML = submissions.length > 0 ? submissions.map(submission => {
        // 统一字段映射，兼容作业/考试提交及不同命名
        const submissionId = submission.id ?? submission.submissionId;
        const studentId = submission.studentId || submission.student_id || submission.student?.studentId || submission.userId;
        // 优先使用后端返回的真实姓名(realName/name/studentName)，保证显示数据库中的姓名
        const studentName = submission.realName
            || submission.name
            || submission.studentName
            || submission.student_name
            || submission.student?.name
            || submission.student?.realName
            || '-';
        const title = submission.title
            || submission.assignmentTitle
            || submission.assignmentName
            || submission.examTitle
            || submission.examName
            || submission.taskTitle
            || (submission.assignmentId ? `作业#${submission.assignmentId}` : '')
            || (submission.examId ? `考试#${submission.examId}` : '-');
        const submitTime = submission.submissionDate || submission.submittedAt || submission.submitTime || submission.createdAt;
        const score = submission.score ?? submission.grade ?? submission.mark ?? null;
        const gradedFlag = submission.graded === true || submission.isGraded === true;
        const lateFlag = submission.isLate === true || submission.late === true;
        const normalizedStatus = (submission.status || submission.submissionStatus || (gradedFlag ? 'graded' : (lateFlag ? 'late' : 'submitted'))).toString().toLowerCase();

        let statusBadge = '<span class="badge badge-secondary">未知</span>';
        switch (normalizedStatus) {
            case 'submitted':
                statusBadge = '<span class="badge badge-primary">已提交</span>';
                break;
            case 'graded':
                statusBadge = '<span class="badge badge-success">已批改</span>';
                break;
            case 'late':
                statusBadge = '<span class="badge badge-warning">迟交</span>';
                break;
            case 'missing':
                statusBadge = '<span class="badge badge-danger">未提交</span>';
                break;
        }

        const actionButtons = submissionId != null ? `
            <button class="btn btn-primary btn-sm" onclick="viewSubmission(${submissionId})">
                <i class="fa fa-eye"></i> 查看
            </button>
            ${normalizedStatus !== 'graded' ? `
                <button class="btn btn-success btn-sm" onclick="gradeSubmission(${submissionId})">
                    <i class="fa fa-check-circle"></i> 批改
                </button>
            ` : `
                <button class="btn btn-success btn-sm" onclick="regradeSubmission(${submissionId})">
                    <i class="fa fa-edit"></i> 重新批改
                </button>
            `}
        ` : '<span class="text-muted">无可用操作</span>';

        return `
            <tr>
                <td>${studentName}</td>
                <td>${studentId}</td>
                <td>${title}</td>
                <td>${submitTime ? new Date(submitTime).toLocaleString() : '-'}</td>
                <td>${statusBadge}</td>
                <td>${score !== null ? score : '-'}</td>
                <td>
                    <div class="action-buttons">
                        ${actionButtons}
                    </div>
                </td>
            </tr>
        `;
    }).join('') : `
        <tr>
            <td colspan="7" style="text-align: center; color: #64748b;">暂无提交记录</td>
        </tr>
    `;
}

// 作业搜索功能
function initAssignmentSearch() {
    // 这个函数已经不再使用，搜索功能已经在teacher-assignments.html中实现
    console.log('initAssignmentSearch is deprecated');
}

// 考试搜索功能
function initExamSearch() {
    // 这个函数已经不再使用，搜索功能已经在teacher-assignments.html中实现
    console.log('initExamSearch is deprecated');
}

// 提交记录搜索功能
function initSubmissionSearch() {
    // 这个函数已经不再使用，搜索功能已经在teacher-assignments.html中实现
    console.log('initSubmissionSearch is deprecated');
}

// 查看作业详情
async function viewAssignment(assignmentId) {
    try {
        const assignment = await fetchAPI(`/api/teacher/assignments/${assignmentId}`);
        
        // 更新模态框标题
        const modalTitle = document.querySelector('#viewAssignmentModal .modal-title');
        if (modalTitle) modalTitle.textContent = assignment.title;
        
        // 构建作业详情HTML
        let submissionsHTML = '';
        if (assignment.submissions && assignment.submissions.length > 0) {
            submissionsHTML = `
                <h6>提交记录 (${assignment.submissions.length})</h6>
                <table class="table table-sm">
                    <thead>
                        <tr>
                            <th>学生姓名</th>
                            <th>提交时间</th>
                            <th>是否迟交</th>
                            <th>分数</th>
                            <th>评语</th>
                            <th>状态</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${assignment.submissions.map(submission => `
                            <tr>
                                <td>${submission.studentName}</td>
                                <td>${new Date(submission.submissionDate).toLocaleString()}</td>
                                <td>${submission.isLate ? '<span class="badge badge-danger">是</span>' : '<span class="badge badge-success">否</span>'}</td>
                                <td>${submission.score !== null ? submission.score : '-'}</td>
                                <td>${submission.teacherComment || '-'}</td>
                                <td>${submission.graded ? '<span class="badge badge-success">已批改</span>' : '<span class="badge badge-warning">未批改</span>'}</td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            `;
        } else {
            submissionsHTML = '<p class="text-muted">暂无提交记录</p>';
        }
        
        // 更新模态框内容
        const modalBody = document.getElementById('viewAssignmentBody');
        if (modalBody) {
            modalBody.innerHTML = `
                <div class="card mb-3">
                    <div class="card-body">
                        <h5 class="card-title">作业详情</h5>
                        <div class="row">
                            <div class="col-md-6">
                                <p><strong>课程：</strong>${assignment.courseName}</p>
                                <p><strong>发布时间：</strong>${new Date(assignment.publishDate).toLocaleString()}</p>
                                <p><strong>截止时间：</strong>${new Date(assignment.dueDate).toLocaleString()}</p>
                                <p><strong>状态：</strong>${assignment.isActive ? '<span class="badge badge-primary">激活</span>' : '<span class="badge badge-secondary">未激活</span>'}</p>
                            </div>
                            <div class="col-md-6">
                                <p><strong>发布者：</strong>${assignment.teacherName}</p>
                                <p><strong>提交人数：</strong>${assignment.submissions ? assignment.submissions.length : 0}</p>
                                <p><strong>已批改：</strong>${assignment.submissions ? assignment.submissions.filter(s => s.graded).length : 0}</p>
                            </div>
                        </div>
                        <div class="mt-3">
                            <h6>作业描述</h6>
                            <p>${assignment.description}</p>
                        </div>
                    </div>
                </div>
                ${submissionsHTML}
            `;
        }
        
        // 显示模态框
        const modal = new bootstrap.Modal(document.getElementById('viewAssignmentModal'));
        modal.show();
    } catch (error) {
        console.error('Failed to view assignment:', error);
    }
}

// 编辑作业
async function editAssignment(assignmentId) {
    try {
        const assignment = await fetchAPI(`/api/teacher/assignments/${assignmentId}`);
        
        // 填充表单数据
        document.getElementById('edit-assignment-id').value = assignment.id;
        document.getElementById('edit-assignment-title').value = assignment.title;
        document.getElementById('edit-assignment-course').value = assignment.courseId;
        document.getElementById('edit-assignment-description').value = assignment.description;
        document.getElementById('edit-assignment-start').value = new Date(assignment.publishDate).toISOString().slice(0, 16);
        document.getElementById('edit-assignment-end').value = new Date(assignment.dueDate).toISOString().slice(0, 16);
        document.getElementById('edit-assignment-is-active').checked = assignment.isActive;
        
        // 显示模态框
        const modal = new bootstrap.Modal(document.getElementById('editAssignmentModal'));
        modal.show();
    } catch (error) {
        console.error('Failed to edit assignment:', error);
    }
}

// 批改作业
async function gradeAssignment(assignmentId) {
    try {
        // 保存assignmentId到模态框
        const modalElement = document.getElementById('gradeAssignmentModal');
        if (modalElement) {
            modalElement.dataset.assignmentId = assignmentId;
        }
        
        // 复用已存在的实例，避免重复创建多层遮罩
        const modal = bootstrap.Modal.getOrCreateInstance(modalElement);
        if (!modalElement.classList.contains('show')) {
            modal.show();
        }
        
        // 隐藏所有状态容器
        document.getElementById('grade-assignment-loading').style.display = 'block';
        document.getElementById('grade-assignment-error').style.display = 'none';
        document.getElementById('grade-assignment-content').style.display = 'none';
        
        // 调用API获取作业的所有提交记录
        const response = await fetch(`http://localhost:8080/api/teacher/assignments/${assignmentId}/submissions`, {
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        
        // 隐藏加载状态
        document.getElementById('grade-assignment-loading').style.display = 'none';
        
        if (data.success && data.data) {
            const submissions = data.data;
            
            if (submissions.length === 0) {
                document.getElementById('grade-assignment-error-message').textContent = '该作业暂无提交记录';
                document.getElementById('grade-assignment-error').style.display = 'block';
            } else {
                // 渲染提交列表
                renderAssignmentSubmissions(submissions);
                document.getElementById('grade-assignment-content').style.display = 'block';
            }
        } else {
            throw new Error(data.message || '获取提交记录失败');
        }
    } catch (error) {
        // 隐藏加载状态
        document.getElementById('grade-assignment-loading').style.display = 'none';
        
        // 显示错误信息
        const errorContainer = document.getElementById('grade-assignment-error');
        const errorMessage = document.getElementById('grade-assignment-error-message');
        errorMessage.textContent = `获取提交记录失败: ${error.message}`;
        errorContainer.style.display = 'block';
        console.error('获取提交记录错误:', error);
    }
}

// 渲染作业提交列表
function renderAssignmentSubmissions(submissions) {
    const tbody = document.getElementById('grade-assignment-table-body');
    tbody.innerHTML = '';
    
    submissions.forEach(submission => {
        const row = document.createElement('tr');
        const submissionDate = submission.submissionDate ? new Date(submission.submissionDate).toLocaleString('zh-CN') : '-';
        const status = submission.graded ? '<span class="badge bg-success">已批改</span>' : '<span class="badge bg-warning">待批改</span>';
        const score = submission.graded ? (submission.score !== null ? submission.score + '分' : '-') : '-';
        const content = submission.content || '-';
        const contentPreview = content.length > 50 ? content.substring(0, 50) + '...' : content;
        const studentName = submission.studentName || `学生${submission.studentId || '-'}`;
        
        row.innerHTML = `
            <td>${studentName}</td>
            <td>${submissionDate}</td>
            <td>${contentPreview}</td>
            <td>${status}</td>
            <td>${score}</td>
            <td>
                <button class="btn btn-sm btn-primary" onclick="openGradeSubmissionModal(${submission.id})">
                    ${submission.graded ? '重新批改' : '批改'}
                </button>
            </td>
        `;
        tbody.appendChild(row);
    });
}

// 打开批改提交模态框
async function openGradeSubmissionModal(submissionId) {
    try {
        // 获取提交详情
        const response = await fetch(`http://localhost:8080/api/teacher/assignments/submissions/${submissionId}`, {
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        if (!data.success || !data.data) {
            throw new Error(data.message || '获取提交详情失败');
        }
        
        const submission = data.data;
        
        // 填充表单
        document.getElementById('grade-submission-id').value = submissionId;
        document.getElementById('grade-submission-content').value = submission.content || '';
        document.getElementById('grade-score').value = submission.score || '';
        document.getElementById('grade-comment').value = submission.teacherComment || '';
        
        // 显示模态框
        const modal = new bootstrap.Modal(document.getElementById('gradeSubmissionModal'));
        modal.show();
    } catch (error) {
        console.error('打开批改模态框失败:', error);
        alert('打开批改模态框失败: ' + error.message);
    }
}

// 提交批改
async function submitGradeSubmission() {
    try {
        const submissionId = document.getElementById('grade-submission-id').value;
        const scoreInput = document.getElementById('grade-score').value;
        const comment = document.getElementById('grade-comment').value || '';
        
        if (!submissionId || !scoreInput) {
            alert('请输入分数');
            return;
        }
        
        const score = parseInt(scoreInput);
        if (isNaN(score) || score < 0 || score > 100) {
            alert('请输入有效的分数（0-100）');
            return;
        }
        
        // 调用API批改作业 - 确保score是Number类型
        const response = await fetch(`http://localhost:8080/api/teacher/assignments/grade/${submissionId}`, {
            method: 'PUT',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                score: score,  // 确保是Number类型
                teacherComment: comment || ''
            })
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        
        if (data.success || data.code === 200) {
            alert('批改成功！');
            // 关闭模态框
            const modal = bootstrap.Modal.getInstance(document.getElementById('gradeSubmissionModal'));
            if (modal) {
                modal.hide();
            }
            // 重新加载提交列表
            const assignmentModal = document.getElementById('gradeAssignmentModal');
            const assignmentId = assignmentModal?.dataset.assignmentId;
            if (assignmentId) {
                await gradeAssignment(assignmentId);
            }
        } else {
            throw new Error(data.message || '批改失败');
        }
    } catch (error) {
        console.error('批改失败:', error);
        alert('批改失败: ' + error.message);
    }
}

// 加载统计数据
async function loadStats() {
    try {
        console.log('=== 开始加载统计数据 ===');
        
        // 使用封装好的API服务
        const apiService = new APIService();
        const teacherAPI = new TeacherAPI(apiService);
        
        // 调用API获取仪表盘数据
        const dashboardData = await teacherAPI.getDashboard();
        console.log('仪表盘数据:', dashboardData);
        
        // 获取统计数据
        const data = dashboardData.data || {};
        const stats = {
            pendingGrades: data.pendingGrades || 0,
            upcomingExams: data.upcomingExams || 0,
            missingSubmissions: data.missingSubmissions || 0,
            upcomingDeadlines: data.upcomingDeadlines || 0,
            pendingGradesChange: data.pendingGradesChange || 0,
            upcomingExamsChange: data.upcomingExamsChange || 0,
            missingSubmissionsChange: data.missingSubmissionsChange || 0,
            upcomingDeadlinesChange: data.upcomingDeadlinesChange || 0
        };
        
        console.log('统计数据:', stats);
        
        // 更新统计卡片
        updateStatCard('pendingGrades', stats.pendingGrades, stats.pendingGradesChange);
        updateStatCard('upcomingExams', stats.upcomingExams, stats.upcomingExamsChange);
        updateStatCard('missingSubmissions', stats.missingSubmissions, stats.missingSubmissionsChange);
        updateStatCard('upcomingDeadlines', stats.upcomingDeadlines, stats.upcomingDeadlinesChange);
        
        console.log('=== 统计数据加载完成 ===');
    } catch (error) {
        console.error('加载统计数据失败:', error);
        
        // 加载失败时使用默认数据
        updateStatCard('pendingGrades', 0, 0);
        updateStatCard('upcomingExams', 0, 0);
        updateStatCard('missingSubmissions', 0, 0);
        updateStatCard('upcomingDeadlines', 0, 0);
    }
}

// 更新统计卡片
function updateStatCard(statKey, value, change = 0) {
    // 更新数值
    const valueElement = document.querySelector(`[data-stat="${statKey}"]`);
    if (valueElement) {
        valueElement.textContent = value;
        valueElement.classList.remove('loading-skeleton');
    }
    
    // 更新变化
    const changeElement = document.querySelector(`[data-stat-change="${statKey}Change"]`);
    if (changeElement) {
        const changeIcon = changeElement.querySelector('i');
        const changeText = changeElement.querySelector('span');
        
        if (change > 0) {
            changeIcon.className = 'fa fa-plus';
            changeElement.className = 'stat-change positive';
            changeText.textContent = `较昨日 +${change}`;
        } else if (change < 0) {
            changeIcon.className = 'fa fa-minus';
            changeElement.className = 'stat-change negative';
            changeText.textContent = `较昨日 ${change}`;
        } else {
            changeIcon.className = 'fa fa-minus';
            changeElement.className = 'stat-change';
            changeText.textContent = `较昨日 ${change}`;
        }
    }
}

// 作业搜索功能 - 已移至页面内实现，保留兼容性
async function searchAssignments() {
    console.log('=== 开始搜索作业 (兼容函数) ===');
    try {
        const searchKeyword = document.getElementById('assignment-search')?.value || '';
        const courseId = document.getElementById('assignment-course')?.value || '';
        const status = document.getElementById('assignment-status')?.value || '';
        const submitted = document.getElementById('assignment-submitted')?.value || '';
        
        // 确保只传递有效值
        const params = {};
        if (searchKeyword.trim() !== '') params.keyword = searchKeyword;
        if (courseId.trim() !== '') params.courseId = courseId;
        if (status.trim() !== '') params.status = status;
        if (submitted.trim() !== '' && submitted !== 'undefined') params.submitted = submitted;
        
        console.log('作业搜索参数:', params);
        
        // 调用loadAssignments函数，传入搜索参数
        await loadAssignments(1, params);
        
        console.log('=== 作业搜索完成 ===');
    } catch (error) {
        console.error('搜索作业失败:', error);
        showMessage('搜索作业失败: ' + error.message, 'error');
    }
}

// 考试搜索功能 - 已移至页面内实现，保留兼容性
async function searchExams() {
    console.log('=== 开始搜索考试 (兼容函数) ===');
    try {
        const searchKeyword = document.getElementById('exam-search')?.value || '';
        const courseId = document.getElementById('exam-course')?.value || '';
        const status = document.getElementById('exam-status')?.value || '';
        const online = document.getElementById('exam-online')?.value || '';
        
        const params = {};
        if (searchKeyword.trim() !== '') params.keyword = searchKeyword;
        if (courseId.trim() !== '') params.courseId = courseId;
        if (status.trim() !== '') params.status = status;
        if (online.trim() !== '' && online !== 'undefined') params.online = online;
        
        console.log('考试搜索参数:', params);
        
        // 调用loadExams函数，传入搜索参数
        await loadExams(1, params);
        
        console.log('=== 考试搜索完成 ===');
    } catch (error) {
        console.error('搜索考试失败:', error);
        showMessage('搜索考试失败: ' + error.message, 'error');
    }
}

// 提交记录搜索功能 - 已移至页面内实现，保留兼容性
async function searchSubmissions() {
    console.log('=== 开始搜索提交记录 (兼容函数) ===');
    try {
        const studentId = document.getElementById('submission-student')?.value || '';
        const type = document.getElementById('submission-type')?.value || '';
        const assignment = document.getElementById('submission-assignment')?.value || '';
        const status = document.getElementById('submission-status')?.value || '';
        const startDate = document.getElementById('submission-date-start')?.value || '';
        const endDate = document.getElementById('submission-date-end')?.value || '';
        
        const params = {};
        if (studentId.trim() !== '') params.studentId = studentId;
        if (type.trim() !== '') params.type = type;
        if (assignment.trim() !== '') params.assignment = assignment;
        if (status.trim() !== '') params.status = status;
        if (startDate.trim() !== '') params.startDate = startDate;
        if (endDate.trim() !== '') params.endDate = endDate;
        
        console.log('提交记录搜索参数:', params);
        
        // 调用loadSubmissions函数，传入搜索参数
        await loadSubmissions(1, params);
        
        console.log('=== 提交记录搜索完成 ===');
    } catch (error) {
        console.error('搜索提交记录失败:', error);
        showMessage('搜索提交记录失败: ' + error.message, 'error');
    }
}

// 重置作业搜索
function resetAssignmentSearch() {
    document.getElementById('assignment-search').value = '';
    document.getElementById('assignment-course').value = '';
    document.getElementById('assignment-status').value = '';
    if (document.getElementById('assignment-submitted')) {
        document.getElementById('assignment-submitted').value = '';
    }
    searchAssignments();
}

// 重置考试搜索
function resetExamSearch() {
    document.getElementById('exam-search').value = '';
    document.getElementById('exam-course').value = '';
    document.getElementById('exam-status').value = '';
    if (document.getElementById('exam-online')) {
        document.getElementById('exam-online').value = '';
    }
    searchExams();
}

// 重置提交记录搜索
function resetSubmissionSearch() {
    document.getElementById('submission-student').value = '';
    document.getElementById('submission-type').value = '';
    document.getElementById('submission-assignment').value = '';
    document.getElementById('submission-status').value = '';
    if (document.getElementById('submission-date-start')) {
        document.getElementById('submission-date-start').value = '';
    }
    if (document.getElementById('submission-date-end')) {
        document.getElementById('submission-date-end').value = '';
    }
    searchSubmissions();
}

// 导出提交记录
function exportSubmissions() {
    // 实现导出功能
    console.log('导出提交记录');
    showMessage('导出功能正在开发中', 'error');
}

// 页面加载完成后初始化
window.addEventListener('DOMContentLoaded', async () => {
    console.log('=== 页面加载完成，开始初始化 ===');
    
    // 获取当前页面的文件名
    const currentPage = window.location.pathname.split('/').pop();
    console.log('当前页面:', currentPage);
    
    // 只在需要的页面执行特定的初始化函数
    // 定义需要加载课程的页面列表
    const coursePages = [
        'teacher-assignments.html',
        'teacher-courses.html',
        'teacher-exams.html',
        'teacher-student-dashboard.html',
        'teacher-warning.html'
    ];
    
    // 定义需要加载班级的页面列表
    const classPages = [
        'teacher-assignments.html',
        'teacher-courses.html',
        'teacher-exams.html',
        'teacher-student-dashboard.html',
        'teacher-warning.html'
    ];
    
    // 定义需要加载作业的页面列表
    const assignmentPages = [
        'teacher-assignments.html'
    ];
    
    // 定义需要加载考试的页面列表
    const examPages = [
        'teacher-exams.html'
    ];
    
    // 根据当前页面执行相应的初始化函数
    if (coursePages.includes(currentPage)) {
        // 加载课程列表
        await loadCourses().catch(error => {
            console.error('加载课程失败:', error);
        });
    }
    
    if (classPages.includes(currentPage)) {
        // 加载班级列表
        await loadClasses().catch(error => {
            console.error('加载班级失败:', error);
        });
    }
    
    // 定义需要加载统计数据的页面列表
    const statsPages = [
        'teacher-dashboard.html',
        'teacher-assignments.html',
        'teacher-exams.html',
        'teacher-warning.html'
    ];
    
    if (statsPages.includes(currentPage)) {
        // 加载统计数据
        await loadStats().catch(error => {
            console.error('加载统计数据失败:', error);
        });
    }
    
    if (assignmentPages.includes(currentPage)) {
        // 加载作业列表
        await loadAssignments().catch(error => {
            console.error('加载作业列表失败:', error);
        });
    }
    
    if (examPages.includes(currentPage)) {
        // 加载考试列表
        await loadExams().catch(error => {
            console.error('加载考试列表失败:', error);
        });
    }
    
    // 初始化标签页切换
    initTabs();
    
    console.log('=== 页面初始化完成 ===');
});

// 初始化标签页切换
function initTabs() {
    const tabItems = document.querySelectorAll('.tab-item');
    tabItems.forEach(item => {
        item.addEventListener('click', () => {
            // 移除所有active类
            tabItems.forEach(tab => tab.classList.remove('active'));
            
            // 添加当前tab的active类
            item.classList.add('active');
            
            // 隐藏所有tab内容
            const tabContents = document.querySelectorAll('.tab-content');
            tabContents.forEach(content => content.style.display = 'none');
            
            // 显示当前tab内容
            const tabId = item.getAttribute('data-tab');
            const activeContent = document.getElementById(`${tabId}-content`);
            if (activeContent) {
                activeContent.style.display = 'block';
            }
            
            // 根据当前标签加载对应数据
            if (tabId === 'assignments') {
                loadAssignments();
            } else if (tabId === 'exams') {
                loadExams();
            } else if (tabId === 'submissions') {
                loadSubmissions();
            }
        });
    });
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
window.clearAuthSession = clearAuthSession;

async function deleteAssignment(assignmentId) {
    if (confirm('确定要删除这个作业吗？删除后无法恢复！')) {
        try {
            // 显示加载状态
            const loadingOverlay = document.getElementById('loadingOverlay');
            if (loadingOverlay) {
                loadingOverlay.style.display = 'flex';
            }
            
            // 直接使用全局的API服务实例
            const response = await window.teacherAPI.deleteAssignment(assignmentId);
            
            // 检查响应
            if (response && (response.success === false)) {
                throw new Error(response.message || '删除作业失败');
            }
            
            // 显示成功消息
            showMessage('作业删除成功！', 'success');
            
            // 重新加载作业列表
            loadAssignments();
            
            // 刷新统计卡片数据
            if (typeof loadDashboardStats === 'function') {
                loadDashboardStats();
            }
        } catch (error) {
            console.error('Failed to delete assignment:', error);
            showMessage('作业删除失败：' + error.message, 'error');
        } finally {
            // 隐藏加载状态
            const loadingOverlay = document.getElementById('loadingOverlay');
            if (loadingOverlay) {
                loadingOverlay.style.display = 'none';
            }
        }
    }
}

// 查看考试详情
async function viewExam(examId) {
    try {
        const exam = await fetchAPI(`/api/teacher/exams/${examId}`);
        
        // 更新模态框标题
        const modalTitle = document.querySelector('#viewExamModal .modal-title');
        if (modalTitle) modalTitle.textContent = exam.title;
        
        // 更新模态框内容
        const modalBody = document.getElementById('viewExamBody');
        if (modalBody) {
            modalBody.innerHTML = `
                <div class="card mb-3">
                    <div class="card-body">
                        <h5 class="card-title">考试详情</h5>
                        <div class="row">
                            <div class="col-md-6">
                                <p><strong>课程：</strong>${exam.courseName}</p>
                                <p><strong>开始时间：</strong>${new Date(exam.startTime).toLocaleString()}</p>
                                <p><strong>结束时间：</strong>${new Date(exam.endTime).toLocaleString()}</p>
                                <p><strong>持续时间：</strong>${exam.duration} 分钟</p>
                            </div>
                            <div class="col-md-6">
                                <p><strong>发布者：</strong>${exam.teacherName}</p>
                                <p><strong>考试类型：</strong>${exam.isOnline ? '在线考试' : '线下考试'}</p>
                                <p><strong>状态：</strong>${exam.isActive ? '<span class="badge badge-primary">激活</span>' : '<span class="badge badge-secondary">未激活</span>'}</p>
                                <p><strong>地点：</strong>${exam.location || '-'}</p>
                            </div>
                        </div>
                        <div class="mt-3">
                            <h6>考试说明</h6>
                            <p>${exam.description}</p>
                        </div>
                    </div>
                </div>
            `;
        }
        
        // 显示模态框
        const modal = new bootstrap.Modal(document.getElementById('viewExamModal'));
        modal.show();
    } catch (error) {
        console.error('Failed to view exam:', error);
    }
}

// 编辑考试
async function editExam(examId) {
    try {
        const exam = await fetchAPI(`/api/teacher/exams/${examId}`);
        
        // 填充表单数据
        document.getElementById('edit-exam-id').value = exam.id;
        document.getElementById('edit-exam-title').value = exam.title;
        document.getElementById('edit-exam-course').value = exam.courseId;
        document.getElementById('edit-exam-description').value = exam.description;
        document.getElementById('edit-exam-start').value = new Date(exam.startTime).toISOString().slice(0, 16);
        document.getElementById('edit-exam-duration').value = exam.duration;
        document.getElementById('edit-exam-score').value = exam.maxScore || exam.totalScore;
        
        // 设置考试类型
        const examTypeSelect = document.getElementById('edit-exam-type');
        if (examTypeSelect) {
            examTypeSelect.value = exam.examType || 'unit';
        }
        
        // 显示模态框
        const modal = new bootstrap.Modal(document.getElementById('editExamModal'));
        modal.show();
    } catch (error) {
        console.error('Failed to edit exam:', error);
        showMessage('加载考试详情失败：' + error.message, 'error');
    }
}

// 评分考试
async function gradeExam(examId) {
    try {
        // 保存examId到模态框
        const modalElement = document.getElementById('gradeExamModal');
        if (modalElement) {
            modalElement.dataset.examId = examId;
        }

        // 复用已存在的实例，避免重复创建多层遮罩导致页面灰屏
        const modal = bootstrap.Modal.getOrCreateInstance(modalElement);
        if (!modalElement.classList.contains('show')) {
            modal.show();
        }
        
        // 隐藏所有状态容器
        document.getElementById('grade-exam-loading').style.display = 'block';
        document.getElementById('grade-exam-error').style.display = 'none';
        document.getElementById('grade-exam-content').style.display = 'none';
        
        // 调用API获取考试的所有提交记录
        const data = await apiService.get(`/api/teacher/exams/${examId}/submissions`);
        
        // 隐藏加载状态
        document.getElementById('grade-exam-loading').style.display = 'none';
        
        if (data && data.success && data.data) {
            const submissions = data.data;
            
            if (submissions.length === 0) {
                document.getElementById('grade-exam-error-message').textContent = '该考试暂无提交记录';
                document.getElementById('grade-exam-error').style.display = 'block';
            } else {
                // 渲染提交列表
                renderExamSubmissions(submissions);
                document.getElementById('grade-exam-content').style.display = 'block';
            }
        } else {
            throw new Error(data.message || '获取提交记录失败');
        }
    } catch (error) {
        // 隐藏加载状态
        document.getElementById('grade-exam-loading').style.display = 'none';
        
        // 显示错误信息
        const errorContainer = document.getElementById('grade-exam-error');
        const errorMessage = document.getElementById('grade-exam-error-message');
        errorMessage.textContent = `获取提交记录失败: ${error.message}`;
        errorContainer.style.display = 'block';
        console.error('获取提交记录错误:', error);
    }
}

// 渲染考试提交列表
function renderExamSubmissions(submissions) {
    const tbody = document.getElementById('grade-exam-table-body');
    tbody.innerHTML = '';
    
    submissions.forEach(submission => {
        const row = document.createElement('tr');
        const submissionDate = submission.submissionDate ? new Date(submission.submissionDate).toLocaleString('zh-CN') : '-';
        const status = submission.graded ? '<span class="badge bg-success">已评分</span>' : '<span class="badge bg-warning">待评分</span>';
        const score = submission.graded ? (submission.score !== null ? submission.score + '分' : '-') : '-';
        const timeTaken = submission.timeTaken !== null ? submission.timeTaken + '分钟' : '-';
        const studentName = submission.studentName || `学生${submission.studentId || '-'}`;
        // 处理提交内容，截取前50个字符显示
        const content = submission.content || submission.answerContent || '';
        const contentPreview = content.length > 50 ? content.substring(0, 50) + '...' : (content || '-');
        
        row.innerHTML = `
            <td>${studentName}</td>
            <td>${submissionDate}</td>
            <td title="${content.replace(/"/g, '&quot;')}">${contentPreview}</td>
            <td>${timeTaken}</td>
            <td>${status}</td>
            <td>${score}</td>
            <td>
                <button class="btn btn-sm btn-primary" onclick="openGradeExamSubmissionModal(${submission.id})">
                    ${submission.graded ? '重新评分' : '评分'}
                </button>
            </td>
        `;
        tbody.appendChild(row);
    });
}

// 打开评分提交模态框
async function openGradeExamSubmissionModal(submissionId) {
    try {
        // 获取提交详情
        const data = await apiService.get(`/api/teacher/exams/submissions/${submissionId}`);
        if (!data || !data.success || !data.data) {
            throw new Error(data.message || '获取提交详情失败');
        }
        
        const submission = data.data;
        
        // 填充表单
        document.getElementById('grade-exam-submission-id').value = submissionId;
        document.getElementById('grade-exam-submission-content').value = submission.content || submission.answerContent || '（无提交内容）';
        document.getElementById('grade-exam-score').value = submission.score || '';
        document.getElementById('grade-exam-comment').value = submission.teacherComment || '';
        
        // 显示模态框
        const modal = new bootstrap.Modal(document.getElementById('gradeExamSubmissionModal'));
        modal.show();
    } catch (error) {
        console.error('打开评分模态框失败:', error);
        alert('打开评分模态框失败: ' + error.message);
    }
}

// 提交评分
async function submitGradeExamSubmission() {
    try {
        const submissionId = document.getElementById('grade-exam-submission-id').value;
        const score = parseInt(document.getElementById('grade-exam-score').value);
        const comment = document.getElementById('grade-exam-comment').value;
        
        if (!submissionId || isNaN(score) || score < 0 || score > 100) {
            alert('请输入有效的分数（0-100）');
            return;
        }
        
        // 调用API评分考试
        const data = await apiService.put(`/api/teacher/exams/grade/${submissionId}`, {
            score: score,
            teacherComment: comment
        });
        
        if (data && (data.success || data.code === 200)) {
            showMessage('评分成功！', 'success');
            // 关闭模态框
            const modal = bootstrap.Modal.getInstance(document.getElementById('gradeExamSubmissionModal'));
            if (modal) {
                modal.hide();
            }
            // 重新加载考试提交列表
            const examModal = document.getElementById('gradeExamModal');
            const examId = examModal?.dataset.examId;
            if (examId) {
                await gradeExam(examId);
            }
            // 重新加载全局提交列表，与作业批改体验保持一致
            loadSubmissions();
        } else {
            throw new Error(data.message || '评分失败');
        }
    } catch (error) {
        console.error('评分失败:', error);
        showMessage('评分失败: ' + error.message, 'error');
    }
}

// 删除考试
async function deleteExam(examId) {
    if (confirm('确定要删除这个考试吗？删除后无法恢复！')) {
        try {
            // 显示加载状态
            const loadingOverlay = document.getElementById('loadingOverlay');
            if (loadingOverlay) {
                loadingOverlay.style.display = 'flex';
            }
            
            // 使用封装好的API服务
            const apiService = new APIService();
            const teacherAPI = new TeacherAPI(apiService);
            
            // 调用API删除考试
            await teacherAPI.deleteExam(examId);
            
            // 显示成功消息
            showMessage('考试删除成功！', 'success');
            
            // 重新加载考试列表
            loadExams();
            
            // 刷新统计卡片数据
            loadDashboardStats();
        } catch (error) {
            console.error('Failed to delete exam:', error);
            showMessage('考试删除失败：' + error.message, 'error');
        } finally {
            // 隐藏加载状态
            const loadingOverlay = document.getElementById('loadingOverlay');
            if (loadingOverlay) {
                loadingOverlay.style.display = 'none';
            }
        }
    }
}

// 保存当前查看的提交ID
let currentSubmissionId = null;

// 查看提交记录
async function viewSubmission(submissionId) {
    try {
        const submission = await fetchAPI(`/api/teacher/submissions/${submissionId}`);
        console.log('查看提交详情:', submission);
        
        // 更新模态框标题
        const modalTitle = document.querySelector('#viewSubmissionModal .modal-title');
        if (modalTitle) {
            modalTitle.textContent = `${submission.studentName}的提交详情`;
        }
        
        // 构建提交详情HTML
        const submissionHTML = `
            <div class="card mb-3">
                <div class="card-body">
                    <h6 class="card-title">基本信息</h6>
                    <div class="row">
                        <div class="col-md-6">
                            <p><strong>学生姓名：</strong>${submission.studentName}</p>
                            <p><strong>学号：</strong>${submission.studentId}</p>
                            <p><strong>作业/考试：</strong>${submission.title}</p>
                        </div>
                        <div class="col-md-6">
                            <p><strong>提交时间：</strong>${new Date(submission.submissionDate).toLocaleString()}</p>
                            <p><strong>状态：</strong>${submission.status === 'graded' ? '<span class="badge badge-success">已批改</span>' : '<span class="badge badge-warning">未批改</span>'}</p>
                            <p><strong>分数：</strong>${submission.score !== null ? submission.score : '-'}</p>
                        </div>
                    </div>
                </div>
            </div>
            <div class="card mb-3">
                <div class="card-body">
                    <h6 class="card-title">提交内容</h6>
                    <div class="submission-content">
                        ${submission.content || '<p class="text-muted">暂无提交内容</p>'}
                    </div>
                </div>
            </div>
            ${submission.teacherComment ? `
            <div class="card">
                <div class="card-body">
                    <h6 class="card-title">评语</h6>
                    <p>${submission.teacherComment}</p>
                </div>
            </div>
            ` : ''}
        `;
        
        // 更新模态框内容
        const modalBody = document.getElementById('viewSubmissionBody');
        if (modalBody) {
            modalBody.innerHTML = submissionHTML;
        }
        
        // 保存当前提交ID
        currentSubmissionId = submissionId;
        
        // 显示批改按钮（如果未批改）
        const gradeBtn = document.getElementById('gradeSubmissionBtn');
        if (gradeBtn) {
            gradeBtn.style.display = submission.status === 'graded' ? 'none' : 'block';
        }
        
        // 显示模态框
        const modal = new bootstrap.Modal(document.getElementById('viewSubmissionModal'));
        modal.show();
    } catch (error) {
        console.error('Failed to view submission:', error);
        showMessage('查看提交详情失败: ' + error.message, 'error');
    }
}

// 批改提交
async function gradeSubmission(submissionId) {
    try {
        // 保存当前提交ID
        currentSubmissionId = submissionId;
        
        // 清空表单
        document.getElementById('gradeSubmissionForm').reset();
        document.getElementById('grade-submission-id').value = submissionId;
        
        // 显示模态框
        const modal = new bootstrap.Modal(document.getElementById('gradeSubmissionModal'));
        modal.show();
    } catch (error) {
        console.error('Failed to grade submission:', error);
        showMessage('打开批改模态框失败: ' + error.message, 'error');
    }
}

// 重新批改提交
async function regradeSubmission(submissionId) {
    try {
        const submission = await fetchAPI(`/api/teacher/submissions/${submissionId}`);
        
        // 填充表单数据
        document.getElementById('grade-submission-id').value = submissionId;
        document.getElementById('grade-score').value = submission.score || '';
        document.getElementById('grade-comment').value = submission.teacherComment || '';
        
        // 显示模态框
        const modal = new bootstrap.Modal(document.getElementById('gradeSubmissionModal'));
        modal.show();
    } catch (error) {
        console.error('Failed to regrade submission:', error);
        showMessage('打开重新批改模态框失败: ' + error.message, 'error');
    }
}

// 提交批改成绩
async function submitGradeSubmission() {
    try {
        const form = document.getElementById('gradeSubmissionForm');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }
        
        const submissionId = parseInt(document.getElementById('grade-submission-id').value);
        const score = parseFloat(document.getElementById('grade-score').value);
        const teacherComment = document.getElementById('grade-comment').value;
        
        const result = await teacherAPI.gradeSubmission({
            submissionId,
            score,
            teacherComment,
            graded: true
        });
        
        if (!result || result.success === false) {
            const message = result?.message || '批改失败';
            throw new Error(message);
        }
        
        showMessage('批改成功！', 'success');
        
        // 关闭模态框
        const modal = bootstrap.Modal.getInstance(document.getElementById('gradeSubmissionModal'));
        modal.hide();
        
        // 刷新当前批改作业弹窗列表（若存在）
        const assignmentModal = document.getElementById('gradeAssignmentModal');
        const assignmentId = assignmentModal?.dataset.assignmentId;
        if (assignmentId) {
            await gradeAssignment(assignmentId);
        }
        
        // 重新加载提交记录（全局列表）
        loadSubmissions();
        
        // 如果查看模态框打开，更新其内容
        const viewModal = bootstrap.Modal.getInstance(document.getElementById('viewSubmissionModal'));
        if (viewModal) {
            await viewSubmission(submissionId);
        }
    } catch (error) {
        console.error('Failed to submit grade:', error);
        showMessage('提交批改成绩失败: ' + error.message, 'error');
    }
}

// 为查看提交模态框的批改按钮添加事件监听
document.addEventListener('DOMContentLoaded', function() {
    const gradeBtn = document.getElementById('gradeSubmissionBtn');
    if (gradeBtn) {
        gradeBtn.addEventListener('click', function() {
            if (currentSubmissionId) {
                gradeSubmission(currentSubmissionId);
            }
        });
    }
});

// 提交发布作业
async function submitAddAssignment() {
    const form = document.getElementById('addAssignmentForm');
    if (!form) return;
    
    // 验证表单
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    
    try {
        // 显示加载状态
        const loadingOverlay = document.getElementById('loadingOverlay');
        if (loadingOverlay) {
            loadingOverlay.style.display = 'flex';
        }
        
        // 获取表单数据
        const title = document.getElementById('assignment-title').value;
        const courseIdStr = document.getElementById('add-assignment-course').value;
        const description = document.getElementById('assignment-description').value;
        const startDate = document.getElementById('assignment-start').value;
        const endDate = document.getElementById('assignment-end').value;
        const maxScore = parseInt(document.getElementById('assignment-score').value);
        
        // 转换并验证课程ID
        console.log('课程选择器原始值(courseIdStr):', courseIdStr, '类型:', typeof courseIdStr);
        const courseId = parseInt(courseIdStr);
        console.log('转换后的课程ID(courseId):', courseId, '是否为NaN:', isNaN(courseId));
        if (isNaN(courseId)) {
            showMessage('请选择有效的所属课程！', 'error');
            return;
        }
        
        // 表单验证 - 暂时不验证班级信息，因为后端还没有处理班级关联
        if (!title) {
            showMessage('请填写作业标题！', 'error');
            return;
        }
        if (!courseIdStr) {
            showMessage('请选择所属课程！', 'error');
            return;
        }
        if (!description) {
            showMessage('请填写作业描述！', 'error');
            return;
        }
        if (!startDate) {
            showMessage('请选择作业发布时间！', 'error');
            return;
        }
        if (!endDate) {
            showMessage('请选择作业截止时间！', 'error');
            return;
        }
        if (!maxScore || isNaN(maxScore)) {
            showMessage('请输入有效的满分值！', 'error');
            return;
        }
        
        // 暂时不处理班级信息，因为后端还没有处理班级关联的逻辑
        
        // 构建请求数据，确保日期格式符合后端期望
        const formatDate = (dateString) => {
            // 将datetime-local格式转换为yyyy-MM-dd HH:mm:ss格式
            const date = new Date(dateString);
            const year = date.getFullYear();
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const day = String(date.getDate()).padStart(2, '0');
            const hours = String(date.getHours()).padStart(2, '0');
            const minutes = String(date.getMinutes()).padStart(2, '0');
            const seconds = String(date.getSeconds()).padStart(2, '0');
            return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
        };
        
        const assignmentData = {
            title: title,
            courseId: courseId, // 已转换为数字类型
            description: description,
            publishDate: formatDate(startDate),
            dueDate: formatDate(endDate),
            isActive: true,
            maxScore: maxScore
        };
        
        // 获取选中的知识点ID
        const knowledgePointsSelect = document.getElementById('assignment-knowledge-points');
        if (knowledgePointsSelect) {
            const selectedKnowledgePoints = Array.from(knowledgePointsSelect.selectedOptions).map(opt => parseInt(opt.value));
            if (selectedKnowledgePoints.length > 0) {
                assignmentData.knowledgePointIds = selectedKnowledgePoints;
                console.log('选中的知识点ID:', selectedKnowledgePoints);
            }
        }
        
        console.log('提交的作业数据:', assignmentData);
        console.log('publishDate格式:', assignmentData.publishDate);
        console.log('dueDate格式:', assignmentData.dueDate);
        
        // 使用封装好的API服务
        const apiService = new APIService();
        const teacherAPI = new TeacherAPI(apiService);
        
        console.log('调用teacherAPI.createAssignment开始');
        // 调用API创建作业
        const result = await teacherAPI.createAssignment(assignmentData);
        console.log('调用teacherAPI.createAssignment结束，返回结果:', result);
        
        // 检查结果
        if (!result) {
            console.error('teacherAPI.createAssignment返回null');
            showMessage('作业发布失败：API返回空结果', 'error');
            return;
        }
        
        // 检查result.success字段
        if (result.success === false) {
            console.error('作业发布失败:', result.message);
            showMessage('作业发布失败：' + result.message, 'error');
            return;
        }
        
        console.log('作业发布成功，准备显示消息');
        // 显示成功消息
        showMessage('作业发布成功！', 'success');
        
        // 关闭模态框
        const modal = bootstrap.Modal.getInstance(document.getElementById('addAssignmentModal'));
        if (modal) {
            modal.hide();
        }
        
        // 重置表单
        form.reset();
        
        console.log('准备调用loadAssignments');
        // 重新加载作业列表
        loadAssignments();
        console.log('调用loadAssignments结束');
        
        // 移除对loadDashboardStats的调用，避免可能的冲突
        // loadDashboardStats();
    } catch (error) {
        console.error('Failed to add assignment:', error);
        showMessage('作业发布失败：' + error.message, 'error');
    } finally {
        // 隐藏加载状态
        const loadingOverlay = document.getElementById('loadingOverlay');
        if (loadingOverlay) {
            loadingOverlay.style.display = 'none';
        }
    }
}

// 提交发布考试功能已移至teacher-assignments.html文件中

// 提交编辑作业
async function submitEditAssignment() {
    const form = document.getElementById('editAssignmentForm');
    if (!form) return;
    
    // 验证表单
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    
    try {
        // 显示加载状态
        const loadingOverlay = document.getElementById('loadingOverlay');
        if (loadingOverlay) {
            loadingOverlay.style.display = 'flex';
        }
        
        // 获取表单数据
        const assignmentId = parseInt(document.getElementById('edit-assignment-id').value);
        const title = document.getElementById('edit-assignment-title').value;
        const courseId = parseInt(document.getElementById('edit-assignment-course').value);
        const description = document.getElementById('edit-assignment-description').value;
        const publishDate = document.getElementById('edit-assignment-start').value;
        const dueDate = document.getElementById('edit-assignment-end').value;
        const maxScore = parseInt(document.getElementById('edit-assignment-score').value);
        
        // 表单验证
        if (!assignmentId || isNaN(assignmentId)) {
            showMessage('作业ID无效！', 'error');
            return;
        }
        if (!title) {
            showMessage('请填写作业标题！', 'error');
            return;
        }
        if (!courseId || isNaN(courseId)) {
            showMessage('请选择有效的所属课程！', 'error');
            return;
        }
        if (!dueDate) {
            showMessage('请选择作业截止时间！', 'error');
            return;
        }
        if (!maxScore || isNaN(maxScore)) {
            showMessage('请输入有效的满分值！', 'error');
            return;
        }
        
        // 构建请求数据
        // 日期需要转换为 ISO 字符串格式
        const publishDateISO = publishDate ? new Date(publishDate).toISOString() : null;
        const dueDateISO = dueDate ? new Date(dueDate).toISOString() : null;
        
        const assignmentData = {
            title: title,
            courseId: courseId,
            description: description,
            publishDate: publishDateISO,
            dueDate: dueDateISO,
            isActive: true,
            maxScore: maxScore
        };
        
        // 使用封装好的API服务
        const apiService = new APIService();
        const teacherAPI = new TeacherAPI(apiService);
        
        // 调用API更新作业 - 需要传递 assignmentId 和 data 两个参数
        const response = await teacherAPI.updateAssignment(assignmentId, assignmentData);
        
        // 检查响应
        if (!response || (response.success === false)) {
            throw new Error(response?.message || '更新作业失败');
        }
        
        // 显示成功消息
        showMessage('作业更新成功！', 'success');
        
        // 关闭模态框
        const modal = bootstrap.Modal.getInstance(document.getElementById('editAssignmentModal'));
        if (modal) {
            modal.hide();
        }
        
        // 重新加载作业列表
        loadAssignments();
        
        // 刷新统计卡片数据
        loadDashboardStats();
    } catch (error) {
        console.error('Failed to edit assignment:', error);
        showMessage('作业更新失败：' + error.message, 'error');
    } finally {
        // 隐藏加载状态
        const loadingOverlay = document.getElementById('loadingOverlay');
        if (loadingOverlay) {
            loadingOverlay.style.display = 'none';
        }
    }
}

// 提交编辑考试
async function submitEditExam() {
    const form = document.getElementById('editExamForm');
    if (!form) return;
    
    // 验证表单
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    
    try {
        // 显示加载状态
        const loadingOverlay = document.getElementById('loadingOverlay');
        if (loadingOverlay) {
            loadingOverlay.style.display = 'flex';
        }
        
        // 获取表单数据
        const examId = parseInt(document.getElementById('edit-exam-id').value);
        const title = document.getElementById('edit-exam-title').value;
        const courseId = parseInt(document.getElementById('edit-exam-course').value);
        const description = document.getElementById('edit-exam-description').value;
        const startTime = document.getElementById('edit-exam-start').value;
        const duration = parseInt(document.getElementById('edit-exam-duration').value);
        const maxScore = parseInt(document.getElementById('edit-exam-score').value);
        const examType = document.getElementById('edit-exam-type').value;
        
        // 表单验证
        if (!examId) {
            showMessage('考试ID无效！', 'error');
            return;
        }
        if (!title) {
            showMessage('请填写考试标题！', 'error');
            return;
        }
        if (!courseId || isNaN(courseId)) {
            showMessage('请选择有效的所属课程！', 'error');
            return;
        }
        if (!startTime) {
            showMessage('请选择考试时间！', 'error');
            return;
        }
        if (!duration || isNaN(duration)) {
            showMessage('请输入有效的考试时长！', 'error');
            return;
        }
        if (!maxScore || isNaN(maxScore)) {
            showMessage('请输入有效的满分值！', 'error');
            return;
        }
        if (!examType || examType === '') {
            showMessage('请选择考试类型！', 'error');
            return;
        }
        
        // 处理时间：datetime-local输入框返回的是本地时间格式（YYYY-MM-DDTHH:mm）
        // 需要将其转换为正确的ISO格式（考虑时区）
        // 方法：将本地时间字符串转换为Date对象，然后转换为ISO字符串
        let startDateTime;
        if (startTime.includes('T')) {
            // 如果包含T，说明是datetime-local格式
            // 直接使用，JavaScript会自动处理为本地时间
            startDateTime = new Date(startTime);
        } else {
            // 否则尝试解析
            startDateTime = new Date(startTime);
        }
        
        // 计算结束时间
        const endDateTime = new Date(startDateTime.getTime() + duration * 60000);
        
        // 构建请求数据
        const examData = {
            title: title,
            courseId: courseId,
            description: description,
            startTime: startDateTime.toISOString(),
            endTime: endDateTime.toISOString(),
            duration: duration,
            isOnline: true, // 默认为线上考试
            isActive: true,
            location: '' // 默认为空
        };
        
        // 使用封装好的API服务
        const apiService = new APIService();
        const teacherAPI = new TeacherAPI(apiService);
        
        // 调用API更新考试，传入examId和examData
        await teacherAPI.updateExam(examId, examData);
        
        // 显示成功消息
        showMessage('考试更新成功！', 'success');
        
        // 关闭模态框
        const modal = bootstrap.Modal.getInstance(document.getElementById('editExamModal'));
        if (modal) {
            modal.hide();
        }
        
        // 重新加载考试列表
        loadExams();
        
        // 刷新统计卡片数据
        loadDashboardStats();
    } catch (error) {
        console.error('Failed to edit exam:', error);
        showMessage('考试更新失败：' + error.message, 'error');
    } finally {
        // 隐藏加载状态
        const loadingOverlay = document.getElementById('loadingOverlay');
        if (loadingOverlay) {
            loadingOverlay.style.display = 'none';
        }
    }
}

// 加载统计数据
async function loadDashboardStats() {
    try {
        // 使用fetchAPI函数获取统计数据
        const response = await fetchAPI('/api/teacher/dashboard');
        const stats = response?.data ?? response ?? {};
        console.log('Dashboard stats:', stats);
        
        // 使用data-stat属性统一更新统计卡片，避免依赖DOM顺序
        const fallbackMap = {
            pendingAssignments: ['pendingAssignments', 'pendingGrades'],
            pendingExams: ['pendingExams', 'upcomingExams'],
            missingSubmissions: ['missingSubmissions'],
            upcomingDeadlines: ['upcomingDeadlines']
        };
        
        document.querySelectorAll('[data-stat]').forEach(element => {
            const statKey = element.getAttribute('data-stat');
            const candidates = fallbackMap[statKey] || [statKey];
            const value = candidates
                .map(key => stats?.[key])
                .find(v => v !== undefined && v !== null);
            
            element.textContent = value ?? 0;
        });
    } catch (error) {
        console.error('Failed to load dashboard stats:', error);
    }
}

// 初始化函数
async function init() {
    // 检查当前页面是否是登录页面，如果是，则不调用需要授权的API
    const currentPage = window.location.pathname.split('/').pop();
    const isLoginPage = currentPage.includes('login') || currentPage === 'index.html';
    
    // 检查是否是教师仪表盘相关页面，如果是，跳过自动加载，因为该页面有自己的初始化逻辑
    const isTeacherDashboardPage = currentPage === 'teacher-student-dashboard.html' || currentPage === 'teacher-dashboard.html';
    
    // 仅对教师相关页面（文件名以 teacher- 开头）执行自动加载，避免学生页面触发教师接口导致 403
    const isTeacherPage = currentPage.startsWith('teacher-');
    
    if (!isLoginPage && isTeacherPage && !isTeacherDashboardPage) {
        // 加载教师端所需数据（会调用 /api/teacher/**）
        await Promise.all([
            loadCourses(),
            loadClasses(),
            loadAssignments(),
            loadExams(),
            loadSubmissions(),
            loadDashboardStats() // 为教师页面加载统计数据
        ]);
        
        // 注释掉废弃的搜索功能初始化，这些函数已不再使用
        // initAssignmentSearch();
        // initExamSearch();
        // initSubmissionSearch();
        
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

// 标签页切换
function initTabs() {
    const tabItems = document.querySelectorAll('.tab-item');
    tabItems.forEach(item => {
        item.addEventListener('click', () => {
            const tab = item.dataset.tab;
            
            // 移除所有活跃状态
            tabItems.forEach(i => i.classList.remove('active'));
            document.querySelectorAll('.tab-content').forEach(content => {
                content.style.display = 'none';
            });
            
            // 添加当前活跃状态
            item.classList.add('active');
            document.getElementById(`${tab}-content`).style.display = 'block';
        });
    });
}

// 初始化标签页
initTabs();

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

// 作业搜索功能
function searchAssignments() {
    // 获取搜索条件
    const title = document.getElementById('assignment-search')?.value || '';
    const courseId = document.getElementById('assignment-course')?.value || '';
    const status = document.getElementById('assignment-status')?.value || '';
    const submitted = document.getElementById('assignment-submitted')?.value || '';
    
    // 构建搜索参数
    const params = {
        keyword: title,
        courseId: courseId,
        status: status,
        submitted: submitted
    };
    
    // 加载作业列表
    loadAssignments(1, params);
}

// 重置作业搜索条件
function resetAssignmentSearch() {
    // 清空搜索输入框
    const searchInput = document.getElementById('assignment-search');
    if (searchInput) searchInput.value = '';
    
    const courseSelect = document.getElementById('assignment-course');
    if (courseSelect) courseSelect.value = '';
    
    const statusSelect = document.getElementById('assignment-status');
    if (statusSelect) statusSelect.value = '';
    
    const submittedSelect = document.getElementById('assignment-submitted');
    if (submittedSelect) submittedSelect.value = '';
    
    // 重新加载作业列表
    loadAssignments(1);
}

// 考试搜索功能
function searchExams() {
    // 获取搜索条件
    const title = document.getElementById('exam-search')?.value || '';
    const courseId = document.getElementById('exam-course')?.value || '';
    const status = document.getElementById('exam-status')?.value || '';
    const isOnline = document.getElementById('exam-online')?.value || '';
    
    // 构建搜索参数
    const params = {
        keyword: title,
        courseId: courseId,
        status: status,
        isOnline: isOnline
    };
    
    // 加载考试列表
    loadExams(1, params);
}

// 重置考试搜索条件
function resetExamSearch() {
    // 清空搜索输入框
    const searchInput = document.getElementById('exam-search');
    if (searchInput) searchInput.value = '';
    
    const courseSelect = document.getElementById('exam-course');
    if (courseSelect) courseSelect.value = '';
    
    const statusSelect = document.getElementById('exam-status');
    if (statusSelect) statusSelect.value = '';
    
    const onlineSelect = document.getElementById('exam-online');
    if (onlineSelect) onlineSelect.value = '';
    
    // 重新加载考试列表
    loadExams(1);
}

// 提交记录搜索功能
function searchSubmissions() {
    // 获取搜索条件
    const studentId = document.getElementById('submission-student')?.value || '';
    const submissionType = document.getElementById('submission-type')?.value || '';
    const assignmentId = document.getElementById('submission-assignment')?.value || '';
    const status = document.getElementById('submission-status')?.value || '';
    const startDate = document.getElementById('submission-date-start')?.value || '';
    const endDate = document.getElementById('submission-date-end')?.value || '';
    
    // 构建搜索参数
    const params = {
        studentId: studentId,
        submissionType: submissionType,
        assignmentId: assignmentId,
        status: status,
        startDate: startDate,
        endDate: endDate
    };
    
    // 加载提交记录列表
    loadSubmissions(1, params);
}

// 重置提交记录搜索条件
function resetSubmissionSearch() {
    // 清空搜索输入框
    const studentSelect = document.getElementById('submission-student');
    if (studentSelect) studentSelect.value = '';
    
    const typeSelect = document.getElementById('submission-type');
    if (typeSelect) typeSelect.value = '';
    
    const assignmentSelect = document.getElementById('submission-assignment');
    if (assignmentSelect) assignmentSelect.value = '';
    
    const statusSelect = document.getElementById('submission-status');
    if (statusSelect) statusSelect.value = '';
    
    const dateStartInput = document.getElementById('submission-date-start');
    if (dateStartInput) dateStartInput.value = '';
    
    const dateEndInput = document.getElementById('submission-date-end');
    if (dateEndInput) dateEndInput.value = '';
    
    // 重新加载提交记录列表
    loadSubmissions(1);
}

// 导出提交记录
function exportSubmissions() {
    // 实现导出功能
    console.log('Export submissions');
}
