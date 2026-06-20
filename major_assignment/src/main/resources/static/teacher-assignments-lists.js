(function attachTeacherAssignmentsLists(global) {
    function resolveTeacherAssignmentsApi() {
        if (global.teacherAPI) {
            return global.teacherAPI;
        }

        const apiService = new APIService();
        return new TeacherAPI(apiService);
    }

    function getSubmissionsTableBody() {
        return document.getElementById('submissions-table-body')
            || document.querySelector('#submissions-content tbody');
    }

    const VIEW_ACTION_BUTTON_CLASS = 'btn teacher-action-view btn-sm';

    function renderTableMessage(tbody, colspan, message, className = 'text-center') {
        if (!tbody) {
            return;
        }

        tbody.innerHTML = `<tr><td colspan="${colspan}" class="${className}">${message}</td></tr>`;
    }

    function normalizeTeacherPageList(data, keys = []) {
        if (Array.isArray(data)) {
            return data;
        }

        if (data && Array.isArray(data.content)) {
            return data.content;
        }

        for (const key of keys) {
            if (Array.isArray(data?.[key])) {
                return data[key];
            }
        }

        return [];
    }

    function buildTeacherPageInfo(responseData, page, fallbackLength, requestedPageSize = 10) {
        const rawPageNumber = responseData?.pageable?.pageNumber
            ?? responseData?.number
            ?? responseData?.pageNumber;

        return {
            pageNumber: (rawPageNumber ?? (page - 1)) + 1,
            totalPages: responseData?.totalPages ?? responseData?.total_pages ?? 1,
            totalElements: responseData?.totalElements ?? responseData?.total_elements ?? fallbackLength,
            pageSize: responseData?.size ?? responseData?.pageSize ?? requestedPageSize
        };
    }

    function parseStoredPaginationParams(container) {
        const savedParams = container?.dataset?.currentParams || '{}';
        try {
            return JSON.parse(savedParams);
        } catch (error) {
            console.warn('解析分页筛选条件失败，已回退为空对象:', error);
            return {};
        }
    }

    function renderPagination(container, pageInfo, loadFunctionName) {
        if (typeof container === 'string') {
            container = document.getElementById(container);
        }

        if (!container || !pageInfo) {
            return;
        }

        const { pageNumber, totalPages } = pageInfo;
        const params = parseStoredPaginationParams(container);
        const paramsStr = JSON.stringify(params).replace(/"/g, '&quot;');

        let paginationHTML = `
            <li class="page-item ${pageNumber === 1 ? 'disabled' : ''}">
                <a class="page-link" onclick="${loadFunctionName}(${pageNumber - 1}, ${paramsStr})" aria-label="Previous">
                    <span aria-hidden="true">&laquo;</span>
                </a>
            </li>
        `;

        let startPage = Math.max(1, pageNumber - 2);
        let endPage = Math.min(totalPages, pageNumber + 2);

        if (endPage - startPage < 4) {
            if (startPage === 1) {
                endPage = Math.min(totalPages, startPage + 4);
            } else if (endPage === totalPages) {
                startPage = Math.max(1, endPage - 4);
            }
        }

        for (let i = startPage; i <= endPage; i += 1) {
            paginationHTML += `
                <li class="page-item ${i === pageNumber ? 'active' : ''}">
                    <a class="page-link" onclick="${loadFunctionName}(${i}, ${paramsStr})">${i}</a>
                </li>
            `;
        }

        paginationHTML += `
            <li class="page-item ${pageNumber === totalPages ? 'disabled' : ''}">
                <a class="page-link" onclick="${loadFunctionName}(${pageNumber + 1}, ${paramsStr})" aria-label="Next">
                    <span aria-hidden="true">&raquo;</span>
                </a>
            </li>
        `;

        container.innerHTML = paginationHTML;
    }

    function resolveCourseName(record) {
        return record.courseName
            || record.course_name
            || courseMap.get(record.courseId || record.course_id)
            || '未知课程';
    }

    function formatDateValue(dateValue, fallback = '未知') {
        if (!dateValue) {
            return fallback;
        }

        try {
            return new Date(dateValue).toLocaleDateString();
        } catch (error) {
            return dateValue;
        }
    }

    function renderAssignments(assignmentsData) {
        const tbody = document.getElementById('assignments-table-body');
        if (!tbody) {
            return;
        }

        const assignments = normalizeTeacherPageList(assignmentsData, ['assignments']);
        if (assignments.length === 0) {
            renderTableMessage(tbody, 7, '暂无作业数据');
            return;
        }

        tbody.innerHTML = assignments.map(assignment => {
            let statusBadge = '';
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

            const publishDate = formatDateValue(assignment.createdAt || assignment.publishDate);
            const dueDate = formatDateValue(assignment.dueDate);
            const submittedCount = assignment.submittedCount || assignment.submitted_count || 0;
            const totalStudents = assignment.totalStudents || assignment.total_students || 0;

            return `
                <tr>
                    <td>${assignment.title}</td>
                    <td>${resolveCourseName(assignment)}</td>
                    <td>${publishDate}</td>
                    <td>${dueDate}</td>
                    <td>${submittedCount}/${totalStudents}</td>
                    <td>${statusBadge}</td>
                    <td>
                        <div class="action-buttons">
                            <button class="${VIEW_ACTION_BUTTON_CLASS}" onclick="viewAssignment(${assignment.id})">
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

    async function loadAssignments(page = 1, params = {}) {
        const tbody = document.getElementById('assignments-table-body');
        if (!tbody) {
            return;
        }

        renderTableMessage(tbody, 7, '加载中...');

        try {
            const requestParams = {
                page,
                size: 10,
                ...params
            };

            const data = await resolveTeacherAssignmentsApi().getAssignments(requestParams);
            if (!data) {
                throw new Error('API请求失败，未返回数据');
            }
            if (data.success === false) {
                throw new Error(data.message || 'API请求失败');
            }

            const responseData = data.data || {};
            const assignments = normalizeTeacherPageList(responseData, ['assignments']);
            const pageInfo = buildTeacherPageInfo(responseData, page, assignments.length, requestParams.size);

            renderAssignments(assignments);

            const paginationContainer = document.getElementById('assignments-pagination');
            if (paginationContainer) {
                paginationContainer.dataset.currentParams = JSON.stringify(params);
                renderPagination(paginationContainer, pageInfo, 'loadAssignments');
            }
        } catch (error) {
            console.error('Failed to load assignments:', error);
            renderTableMessage(tbody, 7, `加载失败: ${error.message}`, 'text-center text-danger');
        }
    }

    function renderExams(examsData) {
        const tbody = document.getElementById('exams-table-body');
        if (!tbody) {
            return;
        }

        const exams = normalizeTeacherPageList(examsData, ['exams']);
        if (exams.length === 0) {
            renderTableMessage(tbody, 7, '暂无考试数据');
            return;
        }

        tbody.innerHTML = exams.map(exam => {
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

            const submittedCount = exam.submittedCount || exam.submitted_count || 0;
            const totalStudents = exam.totalStudents || exam.total_students || 0;

            return `
                <tr>
                    <td>${exam.title}</td>
                    <td>${resolveCourseName(exam)}</td>
                    <td>${formatDateValue(exam.startTime)}</td>
                    <td>${exam.duration}分钟</td>
                    <td>${submittedCount}/${totalStudents}</td>
                    <td>${statusBadge}</td>
                    <td>
                        <div class="action-buttons">
                            <button class="${VIEW_ACTION_BUTTON_CLASS}" onclick="viewExam(${exam.id})">
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

    async function loadExams(page = 1, params = {}) {
        const tbody = document.getElementById('exams-table-body');
        if (!tbody) {
            return;
        }

        renderTableMessage(tbody, 7, '加载中...');

        try {
            const requestParams = {
                page,
                size: 10,
                ...params
            };

            const data = await resolveTeacherAssignmentsApi().getExams(requestParams);
            if (!data) {
                throw new Error('API请求失败，未返回数据');
            }
            if (data.success === false) {
                throw new Error(data.message || '加载考试失败');
            }

            const responseData = data.data || {};
            const exams = normalizeTeacherPageList(responseData, ['exams']);
            const pageInfo = buildTeacherPageInfo(responseData, page, exams.length, requestParams.size);

            renderExams(exams);

            const paginationContainer = document.getElementById('exams-pagination');
            if (paginationContainer) {
                paginationContainer.dataset.currentParams = JSON.stringify(params);
                renderPagination(paginationContainer, pageInfo, 'loadExams');
            }
        } catch (error) {
            console.error('Failed to load exams:', error);
            renderTableMessage(tbody, 7, `加载失败: ${error.message}`, 'text-center text-danger');
        }
    }

    function renderSubmissions(submissionsData) {
        const tbody = getSubmissionsTableBody();
        if (!tbody) {
            return;
        }

        const submissions = normalizeTeacherPageList(submissionsData, ['submissions']);
        if (submissions.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" style="text-align: center; color: #64748b;">暂无提交记录</td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = submissions.map(submission => {
            const submissionId = submission.id ?? submission.submissionId;
            const studentId = submission.studentId
                || submission.student_id
                || submission.student?.studentId
                || submission.userId;
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
            const submitTime = submission.submissionDate
                || submission.submittedAt
                || submission.submitTime
                || submission.createdAt;
            const score = submission.score ?? submission.grade ?? submission.mark ?? null;
            const gradedFlag = submission.graded === true || submission.isGraded === true;
            const lateFlag = submission.isLate === true || submission.late === true;
            const normalizedStatus = (
                submission.status
                || submission.submissionStatus
                || (gradedFlag ? 'graded' : (lateFlag ? 'late' : 'submitted'))
            ).toString().toLowerCase();

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
                <button class="${VIEW_ACTION_BUTTON_CLASS}" onclick="viewSubmission(${submissionId})">
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
        }).join('');
    }

    async function loadSubmissions(page = 1, params = {}) {
        const tbody = getSubmissionsTableBody();
        if (!tbody) {
            return;
        }

        renderTableMessage(tbody, 7, '加载中...');

        try {
            const requestParams = {
                page,
                size: 10,
                ...params
            };

            const data = await resolveTeacherAssignmentsApi().getSubmissions(requestParams);
            if (!data) {
                throw new Error('API请求失败，未返回数据');
            }
            if (data.success === false) {
                throw new Error(data.message || '加载提交记录失败');
            }

            const responseData = data.data || data;
            const submissions = normalizeTeacherPageList(responseData, ['submissions']);
            const pageInfo = buildTeacherPageInfo(responseData, page, submissions.length, requestParams.size);

            renderSubmissions(submissions);

            const paginationContainer = document.getElementById('submissions-pagination');
            if (paginationContainer) {
                paginationContainer.dataset.currentParams = JSON.stringify(params);
                renderPagination(paginationContainer, pageInfo, 'loadSubmissions');
            }
        } catch (error) {
            console.error('Failed to load submissions:', error);
            renderTableMessage(tbody, 7, `加载失败: ${error.message}`, 'text-center text-danger');
            showMessage(`加载提交记录失败，请稍后重试：${error.message}`, 'error');
        }
    }

    global.renderPagination = renderPagination;
    global.renderAssignments = renderAssignments;
    global.loadAssignments = loadAssignments;
    global.renderExams = renderExams;
    global.loadExams = loadExams;
    global.renderSubmissions = renderSubmissions;
    global.loadSubmissions = loadSubmissions;
})(window);
