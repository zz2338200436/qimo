(function attachTeacherAssignmentsExamPublish(global) {
    async function submitAddExam() {
        try {
            console.log('=== 开始执行 submitAddExam 函数 ===');
            const form = document.getElementById('addExamForm');
            if (!form) {
                console.error('表单 addExamForm 未找到');
                return;
            }

            const examTitle = document.getElementById('exam-title').value;
            const examCourseElement = document.getElementById('exam-course');
            const examDescription = document.getElementById('exam-description').value;
            const examStart = document.getElementById('exam-start').value;
            const examDuration = document.getElementById('exam-duration').value;

            const examCourse = examCourseElement.value;
            const selectedOption = examCourseElement.options[examCourseElement.selectedIndex];
            const selectedCourseText = selectedOption ? selectedOption.textContent : '无';

            console.log('表单字段值:', {
                examTitle,
                examCourse,
                selectedCourseText,
                examDescription,
                examStart,
                examDuration
            });

            if (!examTitle) {
                console.error('验证失败：考试标题为空');
                showMessage('请填写考试标题', 'error');
                return;
            }
            if (!examCourse || examCourse === '') {
                console.error('验证失败：所属课程为空');
                showMessage('请选择所属课程', 'error');
                return;
            }
            if (!examDescription) {
                console.error('验证失败：考试说明为空');
                showMessage('请填写考试说明', 'error');
                return;
            }
            if (!examStart) {
                console.error('验证失败：考试时间为空');
                showMessage('请选择考试时间', 'error');
                return;
            }
            if (!examDuration) {
                console.error('验证失败：考试时长为空');
                showMessage('请填写考试时长', 'error');
                return;
            }
            console.log('所有表单验证通过');

            showLoading();

            const apiService = new APIService();
            const teacherAPI = new TeacherAPI(apiService);

            const examData = {
                title: examTitle,
                courseId: parseInt(examCourse),
                description: examDescription,
                startTime: serializeExamLocalDateTime(examStart),
                endTime: buildExamEndLocalDateTime(examStart, examDuration),
                publishDate: serializeExamLocalDateTime(new Date().toLocaleString('sv-SE').replace(' ', 'T')),
                duration: parseInt(examDuration),
                isActive: true,
                isOnline: true,
                location: ''
            };

            const knowledgePointsSelect = document.getElementById('exam-knowledge-points');
            if (knowledgePointsSelect) {
                const selectedKnowledgePoints = Array.from(knowledgePointsSelect.selectedOptions).map(opt => parseInt(opt.value));
                if (selectedKnowledgePoints.length > 0) {
                    examData.knowledgePointIds = selectedKnowledgePoints;
                    console.log('选中的知识点ID:', selectedKnowledgePoints);
                }
            }

            const result = await teacherAPI.createExam(examData);

            if (!result || result.success === false) {
                hideLoading();
                showMessage('考试发布失败：' + (result?.message || 'API返回空结果'), 'error');
                return;
            }

            hideLoading();
            showMessage('考试发布成功', 'success');

            const modal = bootstrap.Modal.getInstance(document.getElementById('addExamModal'));
            modal.hide();
            form.reset();

            if (typeof loadExams === 'function') {
                await loadExams();
            } else {
                console.error('loadExams 函数未定义');
            }

            if (typeof refreshTeacherAssignmentsSummary === 'function') {
                await refreshTeacherAssignmentsSummary();
            }
        } catch (error) {
            hideLoading();
            console.error('发布考试失败:', error);
            showMessage('考试发布失败：' + error.message, 'error');
        }
    }

    async function loadCourseOptions() {
        try {
            console.log('开始加载课程列表');
            const apiService = new APIService();
            const teacherAPI = new TeacherAPI(apiService);

            console.log('调用API获取课程列表');
            const response = await teacherAPI.getCourses({ page: 1, size: 100 });
            console.log('课程列表API响应:', response);

            if (!response) {
                console.error('API未返回响应');
                showMessage('加载课程列表失败：服务器未返回响应', 'error');
                return;
            }

            if (!response.success) {
                console.error('API响应失败:', response.message);
                showMessage('加载课程列表失败：' + response.message, 'error');
                return;
            }

            console.log('API响应成功');
            const pageResult = response.data || {};
            const courses = pageResult.content || [];
            console.log('课程列表数据:', courses);

            const assignmentCourseSelect = document.getElementById('add-assignment-course');
            const examCourseSelects = document.querySelectorAll('#exam-course');
            const editExamCourseSelect = document.getElementById('edit-exam-course');
            const editAssignmentCourseSelect = document.getElementById('edit-assignment-course');

            console.log('作业课程选择器:', assignmentCourseSelect);
            console.log('考试课程选择器:', examCourseSelects);
            console.log('编辑考试课程选择器:', editExamCourseSelect);
            console.log('编辑作业课程选择器:', editAssignmentCourseSelect);

            function processSelector(selector) {
                if (selector) {
                    selector.innerHTML = '<option value="">请选择所属课程</option>';
                    courses.forEach(course => {
                        const option = document.createElement('option');
                        option.value = course.id;
                        option.textContent = course.courseName || course.name;
                        selector.appendChild(option);
                    });
                }
            }

            processSelector(assignmentCourseSelect);
            processSelector(editExamCourseSelect);
            processSelector(editAssignmentCourseSelect);

            if (examCourseSelects && examCourseSelects.length > 0) {
                examCourseSelects.forEach(selector => {
                    processSelector(selector);
                });
            }

            console.log('课程列表加载完成，共添加' + courses.length + '门课程到所有选择器');
        } catch (error) {
            console.error('加载课程列表失败:', error);
            showMessage('加载课程列表失败：' + error.message, 'error');
        }
    }

    async function loadKnowledgePoints(courseId, selectElementId) {
        const selectElement = document.getElementById(selectElementId);
        if (!selectElement) {
            console.error('知识点选择器未找到:', selectElementId);
            return;
        }

        selectElement.innerHTML = '';

        if (!courseId) {
            selectElement.innerHTML = '<option value="" disabled>请先选择课程</option>';
            return;
        }

        try {
            console.log('加载课程知识点，课程ID:', courseId);
            const apiService = new APIService();
            const teacherAPI = new TeacherAPI(apiService);
            const result = await teacherAPI.getKnowledgePointsByCourse(courseId);

            if (!result || result.success === false) {
                console.warn('知识点接口返回失败:', result);
                selectElement.innerHTML = '<option value="" disabled>知识点暂不可用，请稍后重试</option>';
                return;
            }

            console.log('知识点API响应:', result);

            if (result.success && result.data && result.data.length > 0) {
                result.data.forEach(kp => {
                    const option = document.createElement('option');
                    option.value = kp.id;
                    option.textContent = `${kp.pointName} (${kp.difficulty || '未设置难度'})`;
                    selectElement.appendChild(option);
                });
                console.log('加载了', result.data.length, '个知识点');
            } else {
                selectElement.innerHTML = '<option value="" disabled>该课程暂无知识点</option>';
                console.log('该课程暂无知识点');
            }
        } catch (error) {
            console.error('加载知识点失败:', error);
            selectElement.innerHTML = '<option value="" disabled>加载知识点失败，请稍后重试</option>';
        }
    }

    function bindKnowledgePointLoaders() {
        const assignmentCourseSelect = document.getElementById('add-assignment-course');
        if (assignmentCourseSelect) {
            assignmentCourseSelect.addEventListener('change', function onAssignmentCourseChange() {
                loadKnowledgePoints(this.value, 'assignment-knowledge-points');
            });
        }

        const examCourseSelect = document.getElementById('exam-course');
        if (examCourseSelect) {
            examCourseSelect.addEventListener('change', function onExamCourseChange() {
                loadKnowledgePoints(this.value, 'exam-knowledge-points');
            });
        }
    }

    function bindCourseOptionRefresh() {
        const assignmentModal = document.getElementById('addAssignmentModal');
        const examModal = document.getElementById('addExamModal');

        if (assignmentModal) {
            assignmentModal.addEventListener('shown.bs.modal', loadCourseOptions);
        }

        if (examModal) {
            examModal.addEventListener('shown.bs.modal', loadCourseOptions);
        }
    }

    function initTeacherAssignmentsExamPublishModule() {
        bindKnowledgePointLoaders();
        bindCourseOptionRefresh();
    }

    global.submitAddExam = submitAddExam;
    global.loadCourseOptions = loadCourseOptions;
    global.loadKnowledgePoints = loadKnowledgePoints;
    global.initTeacherAssignmentsExamPublishModule = initTeacherAssignmentsExamPublishModule;
})(window);
