(function attachTeacherAssignmentsSubmissionFilters(global) {
    const submissionItemCache = global.teacherAssignmentsSubmissionCache || {
        assignments: [],
        assignmentsLoaded: false,
        exams: [],
        examsLoaded: false
    };

    async function populateAssignmentExamDropdown(options = {}) {
        const { includeExams = false, force = false } = options;
        try {
            const assignmentSelect = document.getElementById('submission-assignment');
            if (!assignmentSelect) {
                return;
            }

            assignmentSelect.innerHTML = '<option value="">加载中作业/考试数据...</option>';

            const apiService = new APIService();
            const teacherAPI = new TeacherAPI(apiService);

            if (force || !submissionItemCache.assignmentsLoaded) {
                const assignmentsResponse = await teacherAPI.getAssignments({ page: 1, size: 100 });
                let assignments = [];
                if (assignmentsResponse && assignmentsResponse.success) {
                    if (assignmentsResponse.data && assignmentsResponse.data.content) {
                        assignments = assignmentsResponse.data.content;
                    } else if (Array.isArray(assignmentsResponse.data)) {
                        assignments = assignmentsResponse.data;
                    }
                }
                submissionItemCache.assignments = assignments;
                submissionItemCache.assignmentsLoaded = true;
            }

            if (includeExams && (force || !submissionItemCache.examsLoaded)) {
                const examsResponse = await teacherAPI.getExams({ page: 1, size: 100 });
                let exams = [];
                if (examsResponse && examsResponse.success) {
                    if (examsResponse.data && examsResponse.data.content) {
                        exams = examsResponse.data.content;
                    } else if (Array.isArray(examsResponse.data)) {
                        exams = examsResponse.data;
                    }
                }
                submissionItemCache.exams = exams;
                submissionItemCache.examsLoaded = true;
            }

            const allItems = [
                ...submissionItemCache.assignments.map(item => ({
                    id: item.id,
                    title: item.title,
                    type: 'assignment'
                })),
                ...(includeExams
                    ? submissionItemCache.exams.map(item => ({
                        id: item.id,
                        title: item.title,
                        type: 'exam'
                    }))
                    : [])
            ];

            allItems.sort((a, b) => a.title.localeCompare(b.title));

            const allOption = document.createElement('option');
            allOption.value = '';
            allOption.textContent = '全部作业/考试';

            assignmentSelect.innerHTML = '';
            assignmentSelect.appendChild(allOption);

            allItems.forEach(item => {
                const option = document.createElement('option');
                option.value = item.id;
                option.textContent = `${item.title} (${item.type === 'assignment' ? '作业' : '考试'})`;
                assignmentSelect.appendChild(option);
            });
        } catch (error) {
            console.error('填充作业/考试下拉菜单失败:', error);
            const assignmentSelect = document.getElementById('submission-assignment');
            if (assignmentSelect) {
                assignmentSelect.innerHTML = '<option value="">获取作业/考试数据失败</option>';
            }
        }
    }

    function initTeacherAssignmentsSubmissionFiltersModule() {
        // 提交记录搜索按钮已由 teacher-assignments-shell.js 接管，
        // 这里仅保留作业/考试下拉缓存与填充能力。
    }

    global.teacherAssignmentsSubmissionCache = submissionItemCache;
    global.populateAssignmentExamDropdown = populateAssignmentExamDropdown;
    global.initTeacherAssignmentsSubmissionFiltersModule = initTeacherAssignmentsSubmissionFiltersModule;
})(window);
