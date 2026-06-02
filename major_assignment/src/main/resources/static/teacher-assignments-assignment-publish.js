(function attachTeacherAssignmentsAssignmentPublish(global) {
    let teacherAssignmentClasses = [];

    function formatAssignmentDate(dateString) {
        const date = new Date(dateString);
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const seconds = String(date.getSeconds()).padStart(2, '0');
        return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
    }

    function resolveClassId(classItem) {
        return classItem?.id || classItem?.classId || classItem?.class_id;
    }

    function resolveClassName(classItem) {
        return classItem?.className || classItem?.name || classItem?.class_name;
    }

    function resolveClassCourseIds(classItem) {
        const directCourseId = classItem?.courseId || classItem?.course_id;
        const nestedCourses = Array.isArray(classItem?.courses) ? classItem.courses : [];
        const nestedCourseIds = nestedCourses
            .map(course => course?.courseId || course?.id || course?.course_id)
            .filter(Boolean);

        return [directCourseId, ...nestedCourseIds].filter(Boolean).map(String);
    }

    function filterClassesByCourse(classes, courseId) {
        if (!courseId) {
            return [];
        }

        const targetCourseId = String(courseId);
        return classes.filter(classItem => resolveClassCourseIds(classItem).includes(targetCourseId));
    }

    function showClassDropdownPlaceholder(select, text) {
        if (!select) {
            return;
        }

        select.innerHTML = '';
        const option = document.createElement('option');
        option.value = '';
        option.textContent = text;
        option.disabled = true;
        select.appendChild(option);
    }

    function populateSingleClassDropdown(select, classes, emptyText = '该课程暂无可发布班级') {
        if (!select) {
            return;
        }

        const selectedValues = new Set(Array.from(select.selectedOptions || []).map(option => option.value));
        select.innerHTML = '';

        classes.forEach(classItem => {
            const classId = resolveClassId(classItem);
            const className = resolveClassName(classItem);
            if (!classId || !className) {
                return;
            }

            const option = document.createElement('option');
            option.value = classId;
            option.textContent = className;
            option.selected = selectedValues.has(String(classId));
            select.appendChild(option);
        });

        if (!select.options.length) {
            showClassDropdownPlaceholder(select, emptyText);
        }
    }

    function refreshClassDropdownForCourse(courseSelectId, classSelectId) {
        const courseSelect = document.getElementById(courseSelectId);
        const classSelect = document.getElementById(classSelectId);
        if (!classSelect) {
            return;
        }

        const courseId = courseSelect ? courseSelect.value : '';
        if (!courseId) {
            showClassDropdownPlaceholder(classSelect, '请先选择课程');
            return;
        }

        populateSingleClassDropdown(classSelect, filterClassesByCourse(teacherAssignmentClasses, courseId));
    }

    function populateAssignmentClassDropdowns(classes) {
        teacherAssignmentClasses = Array.isArray(classes) ? classes : [];
        refreshClassDropdownForCourse('add-assignment-course', 'assignment-class');
        refreshClassDropdownForCourse('edit-assignment-course', 'edit-assignment-class');
        refreshClassDropdownForCourse('exam-course', 'exam-class');
        refreshClassDropdownForCourse('edit-exam-course', 'edit-exam-class');
    }

    async function loadTeacherAssignmentClasses() {
        try {
            const apiService = new APIService();
            const teacherAPI = new TeacherAPI(apiService);
            const response = await teacherAPI.getClasses({ page: 1, size: 100 });

            let classes = [];
            if (response && response.success) {
                if (response.data && Array.isArray(response.data.content)) {
                    classes = response.data.content;
                } else if (Array.isArray(response.data)) {
                    classes = response.data;
                } else if (response.data && Array.isArray(response.data.classes)) {
                    classes = response.data.classes;
                }
            }

            populateAssignmentClassDropdowns(classes);
        } catch (error) {
            console.error('加载发布班级数据失败:', error);
            populateAssignmentClassDropdowns([]);
        }
    }

    function bindCourseClassFilters() {
        [
            ['add-assignment-course', 'assignment-class'],
            ['edit-assignment-course', 'edit-assignment-class'],
            ['exam-course', 'exam-class'],
            ['edit-exam-course', 'edit-exam-class']
        ].forEach(([courseSelectId, classSelectId]) => {
            const courseSelect = document.getElementById(courseSelectId);
            if (!courseSelect || courseSelect.dataset.classFilterBound === 'true') {
                return;
            }

            courseSelect.addEventListener('change', () => {
                refreshClassDropdownForCourse(courseSelectId, classSelectId);
            });
            courseSelect.dataset.classFilterBound = 'true';
        });
    }

    function bindAssignmentClassDropdownRefresh() {
        [
            [document.getElementById('addAssignmentModal'), 'add-assignment-course', 'assignment-class'],
            [document.getElementById('editAssignmentModal'), 'edit-assignment-course', 'edit-assignment-class'],
            [document.getElementById('addExamModal'), 'exam-course', 'exam-class'],
            [document.getElementById('editExamModal'), 'edit-exam-course', 'edit-exam-class']
        ].forEach(([modalElement, courseSelectId, classSelectId]) => {
            if (!modalElement || modalElement.dataset.assignmentClassRefreshBound === 'true') {
                return;
            }

            modalElement.addEventListener('show.bs.modal', async () => {
                await loadTeacherAssignmentClasses();
                refreshClassDropdownForCourse(courseSelectId, classSelectId);
            });
            modalElement.dataset.assignmentClassRefreshBound = 'true';
        });
    }

    async function submitAddAssignment() {
        const form = document.getElementById('addAssignmentForm');
        if (!form) {
            return;
        }

        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }

        try {
            showLoading();

            const title = document.getElementById('assignment-title').value;
            const courseIdStr = document.getElementById('add-assignment-course').value;
            const description = document.getElementById('assignment-description').value;
            const startDate = document.getElementById('assignment-start').value;
            const endDate = document.getElementById('assignment-end').value;
            const maxScore = parseInt(document.getElementById('assignment-score').value, 10);

            console.log('课程选择器原始值(courseIdStr):', courseIdStr, '类型:', typeof courseIdStr);
            const courseId = parseInt(courseIdStr, 10);
            console.log('转换后的课程ID(courseId):', courseId, '是否为NaN:', isNaN(courseId));

            if (isNaN(courseId)) {
                showMessage('请选择有效的所属课程！', 'error');
                return;
            }
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

            const assignmentData = {
                title,
                courseId,
                description,
                publishDate: formatAssignmentDate(startDate),
                dueDate: formatAssignmentDate(endDate),
                isActive: true,
                maxScore
            };

            const knowledgePointsSelect = document.getElementById('assignment-knowledge-points');
            if (knowledgePointsSelect) {
                const selectedKnowledgePoints = Array.from(knowledgePointsSelect.selectedOptions)
                    .map(opt => parseInt(opt.value, 10));
                if (selectedKnowledgePoints.length > 0) {
                    assignmentData.knowledgePointIds = selectedKnowledgePoints;
                    console.log('选中的知识点ID:', selectedKnowledgePoints);
                }
            }

            console.log('提交的作业数据:', assignmentData);
            console.log('publishDate格式:', assignmentData.publishDate);
            console.log('dueDate格式:', assignmentData.dueDate);

            const apiService = new APIService();
            const teacherAPI = new TeacherAPI(apiService);

            console.log('调用teacherAPI.createAssignment开始');
            const result = await teacherAPI.createAssignment(assignmentData);
            console.log('调用teacherAPI.createAssignment结束，返回结果:', result);

            if (!result) {
                console.error('teacherAPI.createAssignment返回null');
                showMessage('作业发布失败：API返回空结果', 'error');
                return;
            }

            if (result.success === false) {
                console.error('作业发布失败:', result.message);
                showMessage('作业发布失败：' + result.message, 'error');
                return;
            }

            console.log('作业发布成功，准备显示消息');
            showMessage('作业发布成功！', 'success');

            const modal = bootstrap.Modal.getInstance(document.getElementById('addAssignmentModal'));
            if (modal) {
                modal.hide();
            }

            form.reset();

            console.log('准备调用loadAssignments');
            if (typeof loadAssignments === 'function') {
                await loadAssignments();
            }
            console.log('调用loadAssignments结束');

            if (typeof refreshTeacherAssignmentsSummary === 'function') {
                await refreshTeacherAssignmentsSummary();
            }
        } catch (error) {
            console.error('Failed to add assignment:', error);
            showMessage('作业发布失败：' + error.message, 'error');
        } finally {
            hideLoading();
        }
    }

    global.populateAssignmentClassDropdowns = populateAssignmentClassDropdowns;
    global.bindAssignmentClassDropdownRefresh = bindAssignmentClassDropdownRefresh;
    global.bindCourseClassFilters = bindCourseClassFilters;
    global.refreshClassDropdownForCourse = refreshClassDropdownForCourse;
    global.loadTeacherAssignmentClasses = loadTeacherAssignmentClasses;
    global.submitAddAssignment = submitAddAssignment;
})(window);
