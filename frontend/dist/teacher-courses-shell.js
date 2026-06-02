(function attachTeacherCoursesShell(global) {
    const COURSE_CATEGORY_OPTIONS = [
        { value: 'required', label: '必修' },
        { value: 'elective', label: '选修' },
        { value: 'public', label: '公共课' },
        { value: 'major', label: '专业课' },
        { value: 'practice', label: '实践课' }
    ];

    const COURSE_STATUS_OPTIONS = [
        { value: 'inactive', label: '未开始' },
        { value: 'active', label: '进行中' },
        { value: 'closed', label: '已结束' }
    ];

    const teacherCoursesPageState = {
        classesInitialized: false,
        assignmentsInitialized: false,
        classCourseOptionsLoaded: false,
        assignmentCourseOptionsLoaded: false,
        classMetadataLoaded: false
    };

    function initCurrentUser() {
        try {
            const user = sessionStorage.getItem('user');
            if (user) {
                global.currentUser = JSON.parse(user);
                console.log('初始化当前用户:', global.currentUser);
            }
        } catch (error) {
            console.error('解析用户信息失败:', error);
        }
    }

    function initializeSearchHistory() {
        if (!global.searchHistory) {
            global.searchHistory = { classes: [] };
            return;
        }

        if (!Array.isArray(global.searchHistory.classes)) {
            global.searchHistory.classes = [];
        }
    }

    function showNotification(message, type = 'info') {
        CommonUI.showToast(message, type);
    }

    function reportResourceLoadFailure(message) {
        CommonUI.reportResourceLoadFailure(message);
    }

    function handleApiError(error) {
        CommonUI.handleApiError(error);
    }

    function showLoading(element) {
        CommonUI.showButtonLoading(element);
    }

    function hideLoading(element) {
        CommonUI.hideButtonLoading(element);
    }

    function verifyBootstrapLoaded() {
        if (typeof global.bootstrap === 'undefined') {
            console.error('Bootstrap 未加载，请检查网络连接或刷新页面');
            return false;
        }
        return true;
    }

    async function loadAssessmentMethods() {
        try {
            const localAssessmentMethods = ['闭卷考试', '开卷考试', '课程论文', '实验报告', '平时成绩', '项目实践', '综合评价'];
            const assessmentSelects = [
                document.getElementById('add-course-assessment-method'),
                document.getElementById('edit-course-assessment-method')
            ];

            console.log('加载本地考核方式选项:', localAssessmentMethods);

            assessmentSelects.forEach(select => {
                if (!select) {
                    return;
                }

                while (select.children.length > 1) {
                    select.removeChild(select.lastChild);
                }

                localAssessmentMethods.forEach(method => {
                    const option = document.createElement('option');
                    option.value = method;
                    option.textContent = method;
                    select.appendChild(option);
                });
            });
        } catch (error) {
            console.error('加载本地考核方式选项失败:', error);

            const defaultMethods = ['闭卷考试', '开卷考试', '课程论文', '实验报告', '平时成绩'];
            const assessmentSelects = [
                document.getElementById('add-course-assessment-method'),
                document.getElementById('edit-course-assessment-method')
            ];

            assessmentSelects.forEach(select => {
                if (!select) {
                    return;
                }

                while (select.children.length > 1) {
                    select.removeChild(select.lastChild);
                }

                defaultMethods.forEach(method => {
                    const option = document.createElement('option');
                    option.value = method;
                    option.textContent = method;
                    select.appendChild(option);
                });
            });
        }
    }

    async function loadUserDropdownForTeacher(containerId) {
        return CommonUI.loadTeacherUserDropdown(containerId);
    }

    function bindSidebarToggle() {
        CommonUI.bindSidebarToggle('toggleBtn', {
            sidebarId: 'sidebar',
            mainContentId: 'mainContent'
        });
    }

    function showBrowserError(error) {
        const errorContainer = document.getElementById('browser-error-container');
        const errorMessage = document.getElementById('browser-error-message');
        const errorStack = document.getElementById('browser-error-stack');

        if (!(errorContainer && errorMessage && errorStack)) {
            return;
        }

        errorMessage.innerHTML = `<strong>${error.errorType || '错误'}:</strong> ${error.message || '未知错误'}`;

        if (error.stack) {
            errorStack.textContent = error.stack;
        }

        errorContainer.style.display = 'block';

        global.setTimeout(() => {
            errorContainer.style.display = 'none';
        }, 10000);
    }

    function bindBrowserErrorHandlers() {
        if (global.__teacherCoursesBrowserErrorHandlersBound) {
            return;
        }

        global.addEventListener('error', function onTeacherCoursesError(errorEvent) {
            const error = {
                errorType: 'JavaScript Error',
                message: errorEvent.message,
                stack: errorEvent.error ? errorEvent.error.stack : '',
                lineNumber: errorEvent.lineno,
                columnNumber: errorEvent.colno,
                fileUrl: errorEvent.filename
            };

            showBrowserError(error);
            return false;
        });

        global.addEventListener('unhandledrejection', function onTeacherCoursesUnhandledRejection(promiseRejectionEvent) {
            const error = {
                errorType: 'Unhandled Promise Rejection',
                message: promiseRejectionEvent.reason
                    ? (promiseRejectionEvent.reason.message || String(promiseRejectionEvent.reason))
                    : '未知Promise拒绝',
                stack: promiseRejectionEvent.reason
                    ? (promiseRejectionEvent.reason.stack || '')
                    : ''
            };

            showBrowserError(error);
        });

        global.__teacherCoursesBrowserErrorHandlersBound = true;
    }

    function bindSidebarShortcuts() {
        const menuItems = document.querySelectorAll('.sidebar-menu .menu-item');
        menuItems.forEach(item => {
            if (item.dataset.teacherCoursesShortcutBound === 'true') {
                return;
            }

            item.addEventListener('click', function onSidebarShortcutClick(event) {
                const label = this.querySelector('span')?.textContent?.trim();
                if (label !== '班级管理') {
                    return;
                }

                event.preventDefault();
                document.querySelector('.tab-item[data-tab="classes"]')?.click();
            });

            item.dataset.teacherCoursesShortcutBound = 'true';
        });
    }

    function bindAddCourseModalValidationReset() {
        const modalElement = document.getElementById('addCourseModal');
        const form = document.getElementById('addCourseForm');
        if (!modalElement || !form || modalElement.dataset.validationResetBound === 'true') {
            return;
        }

        const clearAddCourseFormState = () => {
            form.reset();
            if (typeof global.resetFormValidation === 'function') {
                global.resetFormValidation('addCourseForm');
            }
        };

        modalElement.addEventListener('show.bs.modal', clearAddCourseFormState);
        modalElement.addEventListener('hidden.bs.modal', clearAddCourseFormState);
        modalElement.dataset.validationResetBound = 'true';
    }

    function getCurrentUser() {
        if (!global.currentUser) {
            initCurrentUser();
        }
        return global.currentUser;
    }

    function ensureSelectHasOption(select, value, label = value) {
        if (!select || value === undefined || value === null || value === '') {
            return;
        }

        const stringValue = String(value);
        const existingOption = Array.from(select.options || []).find(option => option.value === stringValue);
        if (existingOption) {
            return;
        }

        const option = document.createElement('option');
        option.value = stringValue;
        option.textContent = label;
        select.appendChild(option);
    }

    function populateCourseEnumSelects(selects, options) {
        selects.forEach(select => {
            if (!select) {
                return;
            }

            const currentValue = select.value;

            while (select.children.length > 1) {
                select.removeChild(select.lastChild);
            }

            options.forEach(({ value, label }) => {
                const option = document.createElement('option');
                option.value = value;
                option.textContent = label;
                select.appendChild(option);
            });

            if (currentValue) {
                ensureSelectHasOption(select, currentValue);
                select.value = currentValue;
            }
        });
    }

    async function loadCourseCategories() {
        try {
            const categorySelects = [
                document.getElementById('course-category'),
                document.getElementById('add-course-category'),
                document.getElementById('edit-course-category')
            ];

            console.log('加载课程类别数据:', COURSE_CATEGORY_OPTIONS);
            populateCourseEnumSelects(categorySelects, COURSE_CATEGORY_OPTIONS);
        } catch (error) {
            console.error('加载课程类别数据失败:', error);
            populateCourseEnumSelects([
                document.getElementById('course-category'),
                document.getElementById('add-course-category'),
                document.getElementById('edit-course-category')
            ], COURSE_CATEGORY_OPTIONS);
        }
    }

    async function loadCourseStatuses() {
        try {
            const statusSelects = [
                document.getElementById('course-status'),
                document.getElementById('add-course-status'),
                document.getElementById('edit-course-status')
            ];

            console.log('加载课程状态数据:', COURSE_STATUS_OPTIONS);
            populateCourseEnumSelects(statusSelects, COURSE_STATUS_OPTIONS);
        } catch (error) {
            console.error('加载课程状态数据失败:', error);
            populateCourseEnumSelects([
                document.getElementById('course-status'),
                document.getElementById('add-course-status'),
                document.getElementById('edit-course-status')
            ], COURSE_STATUS_OPTIONS);
        }
    }

    function activateTeacherCoursesTab(targetTab) {
        const tabItems = document.querySelectorAll('.tab-item');
        const tabContents = document.querySelectorAll('.tab-content');
        const targetContent = document.getElementById(`${targetTab}-content`);

        tabItems.forEach(tabItem => {
            tabItem.classList.toggle('active', tabItem.getAttribute('data-tab') === targetTab);
        });

        tabContents.forEach(tabContent => {
            tabContent.style.display = tabContent === targetContent ? 'block' : 'none';
        });
    }

    function initTabs() {
        const tabItems = document.querySelectorAll('.tab-item');

        tabItems.forEach(item => {
            item.addEventListener('click', function onTabClick() {
                const targetTab = this.getAttribute('data-tab');

                activateTeacherCoursesTab(targetTab);

                if (targetTab === 'classes') {
                    if (!teacherCoursesPageState.classesInitialized) {
                        Promise.all([
                            global.ensureClassMetadataLoaded
                                ? global.ensureClassMetadataLoaded()
                                : Promise.resolve(),
                            performClassSearch()
                        ]).catch(console.error);
                        teacherCoursesPageState.classesInitialized = true;
                    }
                    return;
                }

                if (targetTab === 'courses') {
                    loadCoursePageData(1);
                    return;
                }

                if (targetTab === 'assignments' && !teacherCoursesPageState.assignmentsInitialized) {
                    Promise.all([
                        loadCourseAssignments(),
                        loadClassesForAssignments(),
                        ensureAssignmentCourseOptionsLoaded()
                    ]).catch(console.error);
                    teacherCoursesPageState.assignmentsInitialized = true;
                }
            });
        });
    }

    async function bootstrapTeacherCoursesPage() {
        if (!CommonUI.requireTeacherSession({ redirectUrl: 'teacher-login.html?v=20260523-1' })) {
            return;
        }
        if (global.loadFrontendCapabilities) {
            global.loadFrontendCapabilities();
        }

        verifyBootstrapLoaded();
        initializeSearchHistory();
        bindBrowserErrorHandlers();
        bindSidebarToggle();
        await loadUserDropdownForTeacher('userDropdownContainer');

        initTabs();
        activateTeacherCoursesTab('courses');
        bindSidebarShortcuts();
        initClassSearch();
        await initCourseSearch();
        if (global.bindTeacherCoursesMetadata) {
            global.bindTeacherCoursesMetadata();
        }
        if (global.ensureClassMetadataLoaded) {
            await global.ensureClassMetadataLoaded();
        }
        initPagination();
        await loadAssessmentMethods();
        bindAddCourseModalValidationReset();
        loadCoursePageData(1);
    }

    global.teacherCoursesPageState = teacherCoursesPageState;
    global.currentUser = null;
    global.initCurrentUser = initCurrentUser;
    global.getCurrentUser = getCurrentUser;
    global.loadCourseCategories = loadCourseCategories;
    global.loadCourseStatuses = loadCourseStatuses;
    global.ensureSelectHasOption = ensureSelectHasOption;
    global.initTabs = initTabs;
    global.activateTeacherCoursesTab = activateTeacherCoursesTab;
    global.showNotification = showNotification;
    global.reportResourceLoadFailure = reportResourceLoadFailure;
    global.handleApiError = handleApiError;
    global.showLoading = showLoading;
    global.hideLoading = hideLoading;
    global.loadAssessmentMethods = loadAssessmentMethods;
    global.loadUserDropdownForTeacher = loadUserDropdownForTeacher;
    global.showBrowserError = showBrowserError;
    global.bindAddCourseModalValidationReset = bindAddCourseModalValidationReset;
    global.bootstrapTeacherCoursesPage = bootstrapTeacherCoursesPage;
})(window);
