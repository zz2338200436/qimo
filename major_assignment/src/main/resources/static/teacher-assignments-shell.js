(function attachTeacherAssignmentsShell(global) {
    const assignmentPageState = {
        activeTab: 'assignments',
        examsLoaded: false,
        studentDropdownLoaded: false
    };

    function syncCourseMap(courses, valueResolver) {
        if (typeof courseMap === 'undefined') {
            return;
        }

        courseMap.clear();
        courses.forEach(course => {
            const courseId = valueResolver(course);
            const courseName = course.courseName;
            courseMap.set(courseId, courseName);
        });
    }

    function populateCourseOptions(selectElement, courseList) {
        if (!selectElement || !selectElement.firstElementChild) {
            return;
        }

        const currentValue = selectElement.value;
        const firstOption = selectElement.firstElementChild.cloneNode(true);

        selectElement.innerHTML = '';
        selectElement.appendChild(firstOption);

        courseList.forEach(course => {
            const option = document.createElement('option');
            option.value = course.id;
            option.textContent = course.courseName;
            selectElement.appendChild(option);
        });

        if (currentValue) {
            selectElement.value = currentValue;
        }
    }

    function populateAllCourseDropdowns(courses) {
        const assignmentSearchSelect = document.getElementById('assignment-course');
        if (assignmentSearchSelect) {
            populateCourseOptions(assignmentSearchSelect, courses);
        }

        const examSearchSelect = document.getElementById('exam-course-search');
        if (examSearchSelect) {
            populateCourseOptions(examSearchSelect, courses);
        }

        const addAssignmentModal = document.getElementById('addAssignmentModal');
        if (addAssignmentModal) {
            const assignmentCourseSelect = addAssignmentModal.querySelector('#add-assignment-course');
            if (assignmentCourseSelect) {
                populateCourseOptions(assignmentCourseSelect, courses);
            }
        }

        const editAssignmentModal = document.getElementById('editAssignmentModal');
        if (editAssignmentModal) {
            const editAssignmentCourseSelect = editAssignmentModal.querySelector('#edit-assignment-course');
            if (editAssignmentCourseSelect) {
                populateCourseOptions(editAssignmentCourseSelect, courses);
            }
        }

        const addExamModal = document.getElementById('addExamModal');
        if (addExamModal) {
            const examCourseSelect = addExamModal.querySelector('#exam-course');
            if (examCourseSelect) {
                populateCourseOptions(examCourseSelect, courses);
            }
        }

        const editExamModal = document.getElementById('editExamModal');
        if (editExamModal) {
            const editExamCourseSelect = editExamModal.querySelector('#edit-exam-course');
            if (editExamCourseSelect) {
                populateCourseOptions(editExamCourseSelect, courses);
            }
        }
    }

    async function loadTeacherAssignmentCourses() {
        try {
            const apiService = new APIService();
            const teacherAPI = new TeacherAPI(apiService);
            const response = await teacherAPI.getCourses({ page: 1, size: 100 });

            if (response && response.success) {
                if (response.data && response.data.content) {
                    const courseList = response.data.content;
                    populateAllCourseDropdowns(courseList);
                    syncCourseMap(courseList, course => course.id || course.courseCode);
                    return;
                }

                if (response.data && Array.isArray(response.data)) {
                    populateAllCourseDropdowns(response.data);
                    syncCourseMap(response.data, course => course.id);
                    return;
                }
            }

            console.log('课程接口返回不符合预期，保留页面已有课程选项');
        } catch (error) {
            console.error('加载课程数据失败:', error);
            console.log('保留页面已有课程选项作为兜底');
        }
    }

    async function populateStudentDropdown() {
        try {
            const studentSelect = document.getElementById('submission-student');
            if (!studentSelect) {
                return;
            }

            studentSelect.innerHTML = '<option value="">加载中学生数据...</option>';

            const apiService = new APIService();
            const teacherAPI = new TeacherAPI(apiService);
            const response = await teacherAPI.getStudents();

            let students = [];
            if (response && response.success) {
                if (response.data && response.data.content) {
                    students = response.data.content;
                } else if (Array.isArray(response.data)) {
                    students = response.data;
                }
            }

            const allStudentsOption = document.createElement('option');
            allStudentsOption.value = '';
            allStudentsOption.textContent = '全部学生';

            studentSelect.innerHTML = '';
            studentSelect.appendChild(allStudentsOption);

            students.forEach(student => {
                const option = document.createElement('option');
                option.value = student.id || student.studentId;
                option.textContent = `${student.name || student.studentName} (${student.studentId || student.id})`;
                studentSelect.appendChild(option);
            });
        } catch (error) {
            console.error('填充学生下拉菜单失败:', error);
            const studentSelect = document.getElementById('submission-student');
            if (studentSelect) {
                studentSelect.innerHTML = '<option value="">获取学生数据失败</option>';
            }
        }
    }

    function activateTab(tabId) {
        assignmentPageState.activeTab = tabId;

        document.querySelectorAll('.tab-item').forEach(tab => {
            tab.classList.toggle('active', tab.getAttribute('data-tab') === tabId);
        });

        ['assignments', 'exams', 'submissions'].forEach(contentTabId => {
            const content = document.getElementById(`${contentTabId}-content`);
            if (content) {
                content.classList.toggle('active', contentTabId === tabId);
                content.style.display = contentTabId === tabId ? 'block' : 'none';
            }
        });
    }

    async function switchTeacherAssignmentsTab(tabId) {
        activateTab(tabId);

        if (tabId === 'assignments') {
            await loadAssignments();
            return;
        }

        if (tabId === 'exams') {
            await loadExams();
            assignmentPageState.examsLoaded = true;
            return;
        }

        if (tabId === 'submissions') {
            if (!assignmentPageState.studentDropdownLoaded) {
                await populateStudentDropdown();
                assignmentPageState.studentDropdownLoaded = true;
            }
            await populateAssignmentExamDropdown({ includeExams: true });
            await loadSubmissions();
        }
    }

    function getTeacherAssignmentsEntryState() {
        const params = new URLSearchParams(window.location.search);
        const requestedTab = params.get('tab');
        const assignmentId = params.get('assignmentId');
        const examId = params.get('examId');
        const action = params.get('action');
        const supportedTabs = ['assignments', 'exams', 'submissions'];
        const initialTab = supportedTabs.includes(requestedTab)
            ? requestedTab
            : (assignmentId ? 'assignments' : (examId ? 'exams' : 'assignments'));

        return {
            initialTab,
            assignmentId,
            examId,
            action
        };
    }

    async function handleTeacherAssignmentsEntryAction(entryState) {
        if (!entryState) {
            return;
        }

        if (entryState.initialTab === 'assignments' && entryState.assignmentId) {
            if (entryState.action === 'edit') {
                await editAssignment(entryState.assignmentId);
                return;
            }

            await viewAssignment(entryState.assignmentId);
            return;
        }

        if (entryState.initialTab !== 'exams' || !entryState.examId) {
            return;
        }

        if (entryState.action === 'edit') {
            await editExam(entryState.examId);
            return;
        }

        await viewExam(entryState.examId);

        if (entryState.action === 'start') {
            showMessage('已打开考试详情，可继续查看提交记录或调整考试信息。', 'info');
        }
    }

    function buildAssignmentSearchParams() {
        const params = {};
        const keyword = document.getElementById('assignment-search')?.value || '';
        const courseId = document.getElementById('assignment-course')?.value || '';
        const status = document.getElementById('assignment-status')?.value || '';

        if (keyword.trim() !== '') {
            params.keyword = keyword;
        }
        if (courseId !== '') {
            params.courseId = courseId;
        }
        if (status !== '') {
            params.status = status;
        }

        return params;
    }

    function openTeacherAssignmentWorkspace(tab, assignmentId, action) {
        const params = new URLSearchParams({ tab });
        if (assignmentId) {
            params.set('assignmentId', String(assignmentId));
        }
        if (action) {
            params.set('action', action);
        }
        window.location.href = `teacher-assignments.html?${params.toString()}&v=20260523-1`;
    }

    function buildExamSearchParams() {
        const params = {};
        const keyword = document.getElementById('exam-search')?.value || '';
        const courseId = document.getElementById('exam-course-search')?.value || '';
        const status = document.getElementById('exam-status')?.value || '';

        if (keyword.trim() !== '') {
            params.keyword = keyword;
        }
        if (courseId !== '') {
            params.courseId = courseId;
        }
        if (status !== '') {
            params.status = status;
        }

        return params;
    }

    function buildSubmissionSearchParams() {
        const params = {};
        const studentId = document.getElementById('submission-student')?.value || '';
        const assignmentId = document.getElementById('submission-assignment')?.value || '';
        const status = document.getElementById('submission-status')?.value || '';

        if (studentId.trim() !== '') {
            params.studentId = studentId;
        }
        if (assignmentId !== '') {
            params.assignmentId = assignmentId;
        }
        if (status !== '') {
            params.status = status;
        }

        return params;
    }

    async function refreshTeacherAssignmentsSummary() {
        const statElements = document.querySelectorAll('#mainContent [data-stat]');
        if (!statElements.length || typeof buildTeacherDashboardSnapshot !== 'function') {
            return;
        }

        try {
            const stats = await buildTeacherDashboardSnapshot();
            const fallbackMap = {
                pendingAssignments: ['pendingAssignments', 'pendingGrades'],
                pendingExams: ['pendingExams', 'upcomingExams'],
                missingSubmissions: ['missingSubmissions'],
                upcomingDeadlines: ['upcomingDeadlines']
            };

            statElements.forEach(element => {
                const statKey = element.getAttribute('data-stat');
                const candidates = fallbackMap[statKey] || [statKey];
                const value = candidates
                    .map(key => stats?.[key])
                    .find(candidate => candidate !== undefined && candidate !== null);

                element.textContent = value ?? 0;
                element.classList.remove('loading-skeleton');
            });
        } catch (error) {
            console.warn('刷新作业与考试统计卡片失败:', error?.message || error);
        }
    }

    async function runAssignmentSearch() {
        showLoading();
        try {
            await loadAssignments(1, buildAssignmentSearchParams());
        } finally {
            hideLoading();
        }
    }

    async function runExamSearch() {
        showLoading();
        try {
            await loadExams(1, buildExamSearchParams());
        } finally {
            hideLoading();
        }
    }

    async function runSubmissionSearch() {
        showLoading();
        try {
            await loadSubmissions(1, buildSubmissionSearchParams());
        } finally {
            hideLoading();
        }
    }

    async function resetAssignmentSearch() {
        const searchInput = document.getElementById('assignment-search');
        if (searchInput) {
            searchInput.value = '';
        }

        const courseSelect = document.getElementById('assignment-course');
        if (courseSelect) {
            courseSelect.value = '';
        }

        const statusSelect = document.getElementById('assignment-status');
        if (statusSelect) {
            statusSelect.value = '';
        }

        const submittedSelect = document.getElementById('assignment-submitted');
        if (submittedSelect) {
            submittedSelect.value = '';
        }

        await loadAssignments(1);
    }

    async function resetExamSearch() {
        const searchInput = document.getElementById('exam-search');
        if (searchInput) {
            searchInput.value = '';
        }

        const courseSearchSelect = document.getElementById('exam-course-search') || document.getElementById('exam-course');
        if (courseSearchSelect) {
            courseSearchSelect.value = '';
        }

        const statusSelect = document.getElementById('exam-status');
        if (statusSelect) {
            statusSelect.value = '';
        }

        const onlineSelect = document.getElementById('exam-online');
        if (onlineSelect) {
            onlineSelect.value = '';
        }

        await loadExams(1);
    }

    async function resetSubmissionSearch() {
        const studentSelect = document.getElementById('submission-student');
        if (studentSelect) {
            studentSelect.value = '';
        }

        const typeSelect = document.getElementById('submission-type');
        if (typeSelect) {
            typeSelect.value = '';
        }

        const assignmentSelect = document.getElementById('submission-assignment');
        if (assignmentSelect) {
            assignmentSelect.value = '';
        }

        const statusSelect = document.getElementById('submission-status');
        if (statusSelect) {
            statusSelect.value = '';
        }

        const dateStartInput = document.getElementById('submission-date-start');
        if (dateStartInput) {
            dateStartInput.value = '';
        }

        const dateEndInput = document.getElementById('submission-date-end');
        if (dateEndInput) {
            dateEndInput.value = '';
        }

        await loadSubmissions(1);
    }

    function bindSearchButton(selector, handler) {
        const button = document.querySelector(selector);
        if (!button || button.dataset.codexBound === 'true') {
            return;
        }

        button.dataset.codexBound = 'true';
        button.addEventListener('click', handler);
    }

    function bindSearchFieldEnter(selectors, handler) {
        selectors.forEach(selector => {
            const field = document.querySelector(selector);
            if (!field || field.dataset.codexEnterBound === 'true') {
                return;
            }

            field.dataset.codexEnterBound = 'true';
            field.addEventListener('keydown', async event => {
                if (event.key !== 'Enter') {
                    return;
                }

                event.preventDefault();
                await handler();
            });
        });
    }

    function bindTabItems() {
        document.querySelectorAll('.tab-item').forEach(item => {
            if (item.dataset.codexBound === 'true') {
                return;
            }

            item.dataset.codexBound = 'true';
            item.addEventListener('click', async () => {
                const tabId = item.getAttribute('data-tab');
                await switchTeacherAssignmentsTab(tabId);
            });
        });
    }

    function initTeacherAssignmentsShellModule() {
        bindSearchButton('#assignments-content .search-filter .btn-primary', runAssignmentSearch);
        bindSearchButton('#exams-content .search-filter .btn-primary', runExamSearch);
        bindSearchButton('#submissions-content .search-filter .btn-primary', runSubmissionSearch);
        bindSearchFieldEnter([
            '#assignment-search',
            '#exam-search'
        ], async () => {
            if (assignmentPageState.activeTab === 'assignments') {
                await runAssignmentSearch();
                return;
            }

            if (assignmentPageState.activeTab === 'exams') {
                await runExamSearch();
            }
        });
        bindTabItems();
    }

    async function bootstrapTeacherAssignmentsPage(options = {}) {
        const { loadUserDropdownForTeacher } = options;

        const entryState = getTeacherAssignmentsEntryState();

        if (typeof loadUserDropdownForTeacher === 'function') {
            await loadUserDropdownForTeacher('userDropdownContainer');
        }

        showLoading();
        try {
            initTeacherAssignmentsShellModule();
            await loadTeacherAssignmentCourses();
            if (typeof global.loadTeacherAssignmentClasses === 'function') {
                await global.loadTeacherAssignmentClasses();
            }
            if (typeof global.bindCourseClassFilters === 'function') {
                global.bindCourseClassFilters();
            }
            if (typeof global.bindAssignmentClassDropdownRefresh === 'function') {
                global.bindAssignmentClassDropdownRefresh();
            }
            await switchTeacherAssignmentsTab(entryState.initialTab);
            await refreshTeacherAssignmentsSummary();

            if (global.initTeacherAssignmentsExamPublishModule) {
                global.initTeacherAssignmentsExamPublishModule();
            }
            if (global.initTeacherAssignmentsGradingModule) {
                global.initTeacherAssignmentsGradingModule();
            }

            await handleTeacherAssignmentsEntryAction(entryState);
        } finally {
            hideLoading();
        }
    }

    global.assignmentPageState = assignmentPageState;
    global.populateCourseOptions = populateCourseOptions;
    global.populateAllCourseDropdowns = populateAllCourseDropdowns;
    global.loadTeacherAssignmentCourses = loadTeacherAssignmentCourses;
    global.populateStudentDropdown = populateStudentDropdown;
    global.activateTeacherAssignmentsTab = activateTab;
    global.switchTeacherAssignmentsTab = switchTeacherAssignmentsTab;
    global.getTeacherAssignmentsEntryState = getTeacherAssignmentsEntryState;
    global.handleTeacherAssignmentsEntryAction = handleTeacherAssignmentsEntryAction;
    global.initTeacherAssignmentsShellModule = initTeacherAssignmentsShellModule;
    global.bootstrapTeacherAssignmentsPage = bootstrapTeacherAssignmentsPage;
    global.buildTeacherAssignmentSearchParams = buildAssignmentSearchParams;
    global.buildTeacherExamSearchParams = buildExamSearchParams;
    global.buildTeacherSubmissionSearchParams = buildSubmissionSearchParams;
    global.refreshTeacherAssignmentsSummary = refreshTeacherAssignmentsSummary;
    global.searchAssignments = runAssignmentSearch;
    global.searchExams = runExamSearch;
    global.searchSubmissions = runSubmissionSearch;
    global.resetAssignmentSearch = resetAssignmentSearch;
    global.resetExamSearch = resetExamSearch;
    global.resetSubmissionSearch = resetSubmissionSearch;
    global.openTeacherAssignmentWorkspace = openTeacherAssignmentWorkspace;
})(window);
