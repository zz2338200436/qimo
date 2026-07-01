(function attachTeacherAssignmentsGrading(global) {
    let currentSubmissionId = null;

    function setDisplay(elementId, display) {
        const element = document.getElementById(elementId);
        if (element) {
            element.style.display = display;
        }
    }

    function hideModalById(modalId) {
        const modalElement = document.getElementById(modalId);
        const modal = modalElement ? bootstrap.Modal.getInstance(modalElement) : null;
        if (modal && modalElement.classList.contains('show')) {
            modal.hide();
        }
    }

    function formatSubmissionContent(value) {
        if (value === null || value === undefined || value === '') {
            return '';
        }

        const text = String(value);
        try {
            const parsed = JSON.parse(text);
            if (parsed && typeof parsed === 'object' && parsed.content !== undefined && parsed.content !== null) {
                return String(parsed.content);
            }
        } catch (error) {
            // Plain text submissions are displayed as-is.
        }

        return text;
    }

    function renderSubmissionAttachmentLinksHtml(attachments) {
        if (!Array.isArray(attachments) || attachments.length === 0) {
            return '<span class="text-muted">无附件</span>';
        }

        return attachments.map(attachment => {
            const name = escapeSubmissionText(
                attachment.name
                    || attachment.originalFilename
                    || attachment.fileName
                    || `附件${attachment.id || ''}`
            );
            const inferredDownloadUrl = attachment.id
                ? `/api/attachments/${attachment.assessmentType === 'exam_submission' || attachment.type === 'exam' ? 'exam' : 'assignment'}/${attachment.id}/download`
                : '';
            const downloadUrl = escapeSubmissionText(
                attachment.downloadUrl
                    || attachment.url
                    || inferredDownloadUrl
            );
            return `
                <button type="button"
                        class="btn btn-sm btn-outline-primary me-2 mb-2"
                        data-download-url="${downloadUrl}"
                        data-download-name="${name}"
                        onclick="downloadAttachmentFromButton(this)">
                    <i class="fa fa-download"></i> ${name}
                </button>
            `;
        }).join('');
    }

    function setSubmissionAttachmentContainer(containerId, attachments) {
        const container = document.getElementById(containerId);
        if (container) {
            container.innerHTML = renderSubmissionAttachmentLinksHtml(attachments);
        }
    }

    async function gradeAssignment(assignmentId) {
        try {
            const modalElement = document.getElementById('gradeAssignmentModal');
            if (modalElement) {
                modalElement.dataset.assignmentId = assignmentId;
            }

            const modal = bootstrap.Modal.getOrCreateInstance(modalElement);
            if (!modalElement.classList.contains('show')) {
                modal.show();
            }

            setDisplay('grade-assignment-loading', 'block');
            setDisplay('grade-assignment-error', 'none');
            setDisplay('grade-assignment-content', 'none');

            const response = await fetch(`/api/teacher/assignments/${assignmentId}/submissions`, {
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();
            setDisplay('grade-assignment-loading', 'none');

            if (data.success && data.data) {
                const submissions = data.data;
                if (submissions.length === 0) {
                    const errorMessage = document.getElementById('grade-assignment-error-message');
                    if (errorMessage) {
                        errorMessage.textContent = '该作业暂无提交记录';
                    }
                    setDisplay('grade-assignment-error', 'block');
                } else {
                    renderAssignmentSubmissions(submissions);
                    setDisplay('grade-assignment-content', 'block');
                }
                return;
            }

            throw new Error(data.message || '获取提交记录失败');
        } catch (error) {
            setDisplay('grade-assignment-loading', 'none');
            const errorMessage = document.getElementById('grade-assignment-error-message');
            if (errorMessage) {
                errorMessage.textContent = `获取提交记录失败: ${error.message}`;
            }
            setDisplay('grade-assignment-error', 'block');
            console.error('获取提交记录错误:', error);
        }
    }

    function renderAssignmentSubmissions(submissions) {
        const tbody = document.getElementById('grade-assignment-table-body');
        if (!tbody) {
            return;
        }

        tbody.innerHTML = '';

        submissions.forEach(submission => {
            const row = document.createElement('tr');
            const submissionDate = submission.submissionDate
                ? new Date(submission.submissionDate).toLocaleString('zh-CN')
                : '-';
            const status = submission.graded
                ? '<span class="badge bg-success">已批改</span>'
                : '<span class="badge bg-warning">待批改</span>';
            const score = submission.graded
                ? (submission.score !== null ? `${submission.score}分` : '-')
                : '-';
            const content = formatSubmissionContent(submission.content) || '-';
            const contentPreview = content.length > 50 ? `${content.substring(0, 50)}...` : content;
            const studentName = submission.studentName || `学生${submission.studentId || '-'}`;
            const attachmentsHtml = renderSubmissionAttachmentLinksHtml(submission.attachments);

            row.innerHTML = `
                <td>${studentName}</td>
                <td>${submissionDate}</td>
                <td>${contentPreview}</td>
                <td>${attachmentsHtml}</td>
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

    async function openGradeSubmissionModal(submissionId) {
        try {
            const response = await fetch(`/api/teacher/assignments/submissions/${submissionId}`, {
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
            document.getElementById('grade-submission-id').value = submissionId;
            document.getElementById('grade-submission-content').value = formatSubmissionContent(submission.content);
            document.getElementById('grade-score').value = submission.score || '';
            document.getElementById('grade-comment').value = submission.teacherComment || '';
            setSubmissionAttachmentContainer('grade-submission-attachments', submission.attachments);

            hideModalById('viewSubmissionModal');

            const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById('gradeSubmissionModal'));
            modal.show();
        } catch (error) {
            console.error('打开批改模态框失败:', error);
            showMessage(`打开批改模态框失败: ${error.message}`, 'error');
        }
    }

    function openExamSubmissionListFromViewModal() {
        const viewExamSubmissionsButton = document.getElementById('view-exam-submissions-btn');
        const assignmentId = viewExamSubmissionsButton?.dataset?.assignmentId;
        const examId = viewExamSubmissionsButton?.dataset?.examId;
        if (!assignmentId && !examId) {
            showMessage('缺少作业或考试信息，无法查看提交列表', 'error');
            return;
        }

        const viewExamModal = document.getElementById('viewExamModal');
        const modal = bootstrap.Modal.getInstance(viewExamModal);
        if (modal) {
            modal.hide();
        }

        if (assignmentId) {
            gradeAssignment(parseInt(assignmentId, 10));
            return;
        }

        gradeExam(parseInt(examId, 10));
    }

    async function gradeExam(examId) {
        try {
            const modalElement = document.getElementById('gradeExamModal');
            if (modalElement) {
                modalElement.dataset.examId = examId;
            }

            const modal = bootstrap.Modal.getOrCreateInstance(modalElement);
            if (!modalElement.classList.contains('show')) {
                modal.show();
            }

            setDisplay('grade-exam-loading', 'block');
            setDisplay('grade-exam-error', 'none');
            setDisplay('grade-exam-content', 'none');

            const data = await apiService.get(`/api/teacher/exams/${examId}/submissions`);
            setDisplay('grade-exam-loading', 'none');

            if (data && data.success && data.data) {
                const submissions = data.data;
                if (submissions.length === 0) {
                    const errorMessage = document.getElementById('grade-exam-error-message');
                    if (errorMessage) {
                        errorMessage.textContent = '该考试暂无提交记录';
                    }
                    setDisplay('grade-exam-error', 'block');
                } else {
                    renderExamSubmissions(submissions);
                    setDisplay('grade-exam-content', 'block');
                }
                return;
            }

            throw new Error(data.message || '获取提交记录失败');
        } catch (error) {
            setDisplay('grade-exam-loading', 'none');
            const errorMessage = document.getElementById('grade-exam-error-message');
            if (errorMessage) {
                errorMessage.textContent = `获取提交记录失败: ${error.message}`;
            }
            setDisplay('grade-exam-error', 'block');
            console.error('获取提交记录错误:', error);
        }
    }

    function renderExamSubmissions(submissions) {
        const tbody = document.getElementById('grade-exam-table-body');
        if (!tbody) {
            return;
        }

        tbody.innerHTML = '';

        submissions.forEach(submission => {
            const row = document.createElement('tr');
            const submissionDate = submission.submissionDate
                ? new Date(submission.submissionDate).toLocaleString('zh-CN')
                : '-';
            const status = submission.graded
                ? '<span class="badge bg-success">已评分</span>'
                : '<span class="badge bg-warning">待评分</span>';
            const score = submission.graded
                ? (submission.score !== null ? `${submission.score}分` : '-')
                : '-';
            const timeTaken = submission.timeTaken !== null ? `${submission.timeTaken}分钟` : '-';
            const studentName = submission.studentName || `学生${submission.studentId || '-'}`;
            const content = formatSubmissionContent(submission.content || submission.answerContent || '');
            const contentPreview = content.length > 50 ? `${content.substring(0, 50)}...` : (content || '-');
            const attachmentsHtml = renderSubmissionAttachmentLinksHtml(submission.attachments);

            row.innerHTML = `
                <td>${studentName}</td>
                <td>${submissionDate}</td>
                <td title="${content.replace(/"/g, '&quot;')}">${contentPreview}</td>
                <td>${attachmentsHtml}</td>
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

    async function openGradeExamSubmissionModal(submissionId) {
        try {
            const data = await apiService.get(`/api/teacher/exams/submissions/${submissionId}`);
            if (!data || !data.success || !data.data) {
                throw new Error(data.message || '获取提交详情失败');
            }

            const submission = data.data;
            document.getElementById('grade-exam-submission-id').value = submissionId;
            document.getElementById('grade-exam-submission-content').value =
                formatSubmissionContent(submission.content || submission.answerContent || '') || '（无提交内容）';
            document.getElementById('grade-exam-score').value = submission.score || '';
            document.getElementById('grade-exam-comment').value = submission.teacherComment || '';
            setSubmissionAttachmentContainer('grade-exam-submission-attachments', submission.attachments);

            const modal = new bootstrap.Modal(document.getElementById('gradeExamSubmissionModal'));
            modal.show();
        } catch (error) {
            console.error('打开评分模态框失败:', error);
            showMessage(`打开评分模态框失败: ${error.message}`, 'error');
        }
    }

    async function submitGradeExamSubmission() {
        try {
            const submissionId = document.getElementById('grade-exam-submission-id').value;
            const score = parseInt(document.getElementById('grade-exam-score').value, 10);
            const comment = document.getElementById('grade-exam-comment').value;

            if (!submissionId || Number.isNaN(score) || score < 0 || score > 100) {
                showMessage('请输入有效的分数（0-100）', 'error');
                return;
            }

            const data = await apiService.put(`/api/teacher/exams/grade/${submissionId}`, {
                score,
                teacherComment: comment
            });

            if (!data || (!data.success && data.code !== 200)) {
                throw new Error(data?.message || '评分失败');
            }

            showMessage('评分成功！', 'success');

            const modal = bootstrap.Modal.getInstance(document.getElementById('gradeExamSubmissionModal'));
            if (modal) {
                modal.hide();
            }

            const examModal = document.getElementById('gradeExamModal');
            const examId = examModal?.dataset.examId;
            if (examId) {
                await gradeExam(examId);
            }

            loadSubmissions();
        } catch (error) {
            console.error('评分失败:', error);
            showMessage(`评分失败: ${error.message}`, 'error');
        }
    }

    function escapeSubmissionText(value) {
        if (value === null || value === undefined || value === '') {
            return '';
        }

        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function formatSubmissionDate(value) {
        if (!value) {
            return '未知';
        }

        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return escapeSubmissionText(value);
        }

        return date.toLocaleString();
    }

    function isSubmissionGraded(submission) {
        return submission?.graded === true || submission?.status === 'graded' || submission?.status === '已批改';
    }

    async function viewSubmission(submissionId) {
        try {
            const modalElement = document.getElementById('viewSubmissionModal');
            const modalBody = document.getElementById('viewSubmissionBody');
            if (!modalElement || !modalBody) {
                throw new Error('页面缺少提交详情弹窗，请刷新后重试');
            }

            const submission = await fetchAPI(`/api/teacher/submissions/${submissionId}`);
            const studentName = submission.studentName || `学生${submission.studentId || ''}`.trim() || '学生';
            const assignmentTitle = submission.title || submission.assignmentTitle || `作业${submission.assignmentId || ''}`.trim() || '未知作业';
            const graded = isSubmissionGraded(submission);
            const modalTitle = document.querySelector('#viewSubmissionModal .modal-title');
            if (modalTitle) {
                modalTitle.textContent = `${studentName}的提交详情`;
            }

            const displayContent = formatSubmissionContent(submission.content);
            const contentHtml = displayContent
                ? `<div class="submission-content" style="white-space: pre-wrap; word-break: break-word;">${escapeSubmissionText(displayContent)}</div>`
                : '<p class="text-muted">暂无提交内容</p>';
            const attachmentsHtml = renderSubmissionAttachmentLinksHtml(submission.attachments);

            const submissionHTML = `
                <div class="card mb-3">
                    <div class="card-body">
                        <h6 class="card-title">基本信息</h6>
                        <div class="row">
                            <div class="col-md-6">
                                <p><strong>学生姓名：</strong>${escapeSubmissionText(studentName)}</p>
                                <p><strong>学号：</strong>${escapeSubmissionText(submission.studentId || '-')}</p>
                                <p><strong>作业/考试：</strong>${escapeSubmissionText(assignmentTitle)}</p>
                            </div>
                            <div class="col-md-6">
                                <p><strong>提交时间：</strong>${formatSubmissionDate(submission.submissionDate)}</p>
                                <p><strong>状态：</strong>${graded ? '<span class="badge badge-success">已批改</span>' : '<span class="badge badge-warning">未批改</span>'}</p>
                                <p><strong>分数：</strong>${submission.score !== null && submission.score !== undefined ? escapeSubmissionText(submission.score) : '-'}</p>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="card mb-3">
                    <div class="card-body">
                        <h6 class="card-title">提交内容</h6>
                        ${contentHtml}
                    </div>
                </div>
                <div class="card mb-3">
                    <div class="card-body">
                        <h6 class="card-title">提交附件</h6>
                        ${attachmentsHtml}
                    </div>
                </div>
                ${submission.teacherComment ? `
                <div class="card">
                    <div class="card-body">
                        <h6 class="card-title">评语</h6>
                        <p style="white-space: pre-wrap; word-break: break-word;">${escapeSubmissionText(submission.teacherComment)}</p>
                    </div>
                </div>
                ` : ''}
            `;

            modalBody.innerHTML = submissionHTML;

            currentSubmissionId = submissionId;

            const gradeBtn = document.getElementById('gradeSubmissionBtn');
            if (gradeBtn) {
                gradeBtn.style.display = graded ? 'none' : 'block';
            }

            const modal = bootstrap.Modal.getOrCreateInstance(modalElement);
            modal.show();
        } catch (error) {
            console.error('Failed to view submission:', error);
            showMessage(`查看提交详情失败: ${error.message}`, 'error');
        }
    }

    async function gradeSubmission(submissionId) {
        try {
            currentSubmissionId = submissionId;
            document.getElementById('gradeSubmissionForm').reset();
            document.getElementById('grade-submission-id').value = submissionId;
            const submission = await fetchAPI(`/api/teacher/submissions/${submissionId}`);
            document.getElementById('grade-submission-content').value = formatSubmissionContent(submission.content);
            document.getElementById('grade-score').value = submission.score || '';
            document.getElementById('grade-comment').value = submission.teacherComment || '';
            setSubmissionAttachmentContainer('grade-submission-attachments', submission.attachments);

            hideModalById('viewSubmissionModal');

            const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById('gradeSubmissionModal'));
            modal.show();
        } catch (error) {
            console.error('Failed to grade submission:', error);
            showMessage(`打开批改模态框失败: ${error.message}`, 'error');
        }
    }

    async function regradeSubmission(submissionId) {
        try {
            const submission = await fetchAPI(`/api/teacher/submissions/${submissionId}`);
            document.getElementById('grade-submission-id').value = submissionId;
            document.getElementById('grade-submission-content').value = formatSubmissionContent(submission.content);
            document.getElementById('grade-score').value = submission.score || '';
            document.getElementById('grade-comment').value = submission.teacherComment || '';
            setSubmissionAttachmentContainer('grade-submission-attachments', submission.attachments);
            currentSubmissionId = submissionId;

            hideModalById('viewSubmissionModal');

            const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById('gradeSubmissionModal'));
            modal.show();
        } catch (error) {
            console.error('Failed to regrade submission:', error);
            showMessage(`打开重新批改模态框失败: ${error.message}`, 'error');
        }
    }

    async function submitGradeSubmission() {
        try {
            const form = document.getElementById('gradeSubmissionForm');
            if (!form.checkValidity()) {
                form.reportValidity();
                return;
            }

            const submissionId = parseInt(document.getElementById('grade-submission-id').value, 10);
            const score = parseFloat(document.getElementById('grade-score').value);
            const teacherComment = document.getElementById('grade-comment').value;

            const result = await teacherAPI.gradeSubmission({
                submissionId,
                score,
                teacherComment,
                graded: true
            });

            if (!result || result.success === false) {
                throw new Error(result?.message || '批改失败');
            }

            showMessage('批改成功！', 'success');

            const modal = bootstrap.Modal.getInstance(document.getElementById('gradeSubmissionModal'));
            if (modal) {
                modal.hide();
            }

            const assignmentModal = document.getElementById('gradeAssignmentModal');
            const assignmentId = assignmentModal?.dataset.assignmentId;
            if (assignmentId) {
                await gradeAssignment(assignmentId);
            }

            await loadAssignments();
            loadSubmissions();

            hideModalById('viewSubmissionModal');
        } catch (error) {
            console.error('Failed to submit grade:', error);
            showMessage(`提交批改成绩失败: ${error.message}`, 'error');
        }
    }

    function bindGradeSubmissionButton() {
        const gradeBtn = document.getElementById('gradeSubmissionBtn');
        if (!gradeBtn || gradeBtn.dataset.codexBound === 'true') {
            return;
        }

        gradeBtn.dataset.codexBound = 'true';
        gradeBtn.addEventListener('click', () => {
            if (currentSubmissionId) {
                gradeSubmission(currentSubmissionId);
            }
        });
    }

    function bindViewExamSubmissionsButton() {
        const button = document.getElementById('view-exam-submissions-btn');
        if (!button || button.dataset.codexBound === 'true') {
            return;
        }

        button.dataset.codexBound = 'true';
        button.addEventListener('click', openExamSubmissionListFromViewModal);
    }

    function initTeacherAssignmentsGradingModule() {
        bindGradeSubmissionButton();
        bindViewExamSubmissionsButton();
    }

    global.gradeAssignment = gradeAssignment;
    global.renderAssignmentSubmissions = renderAssignmentSubmissions;
    global.openGradeSubmissionModal = openGradeSubmissionModal;
    global.openExamSubmissionListFromViewModal = openExamSubmissionListFromViewModal;
    global.gradeExam = gradeExam;
    global.renderExamSubmissions = renderExamSubmissions;
    global.openGradeExamSubmissionModal = openGradeExamSubmissionModal;
    global.submitGradeExamSubmission = submitGradeExamSubmission;
    global.viewSubmission = viewSubmission;
    global.gradeSubmission = gradeSubmission;
    global.regradeSubmission = regradeSubmission;
    global.submitGradeSubmission = submitGradeSubmission;
    global.initTeacherAssignmentsGradingModule = initTeacherAssignmentsGradingModule;
})(window);
