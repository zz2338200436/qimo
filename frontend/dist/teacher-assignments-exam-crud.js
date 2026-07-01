(function attachTeacherAssignmentsExamCrud(global) {
    function escapeHtml(value) {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function normalizeExamDateTimeText(value) {
        return String(value || '').trim().replace(' ', 'T');
    }

    function serializeExamLocalDateTime(value) {
        const normalized = normalizeExamDateTimeText(value);
        if (!normalized) {
            return '';
        }
        const withoutOffset = normalized.replace(/([+-]\d{2}:?\d{2}|Z)$/i, '');
        const withoutMillis = withoutOffset.split('.')[0];
        return withoutMillis.length === 16 ? `${withoutMillis}:00` : withoutMillis;
    }

    function parseExamDateTimeForInput(value) {
        const normalized = serializeExamLocalDateTime(value);
        return normalized ? normalized.slice(0, 16) : '';
    }

    function buildExamEndLocalDateTime(startValue, durationMinutes) {
        const normalizedStart = parseExamDateTimeForInput(startValue);
        const duration = parseInt(durationMinutes, 10);
        if (!normalizedStart || !Number.isFinite(duration)) {
            return '';
        }

        const [datePart, timePart] = normalizedStart.split('T');
        const [year, month, day] = datePart.split('-').map(Number);
        const [hours, minutes] = timePart.split(':').map(Number);
        const endDateTime = new Date(year, month - 1, day, hours, minutes + duration, 0);
        const endYear = endDateTime.getFullYear();
        const endMonth = String(endDateTime.getMonth() + 1).padStart(2, '0');
        const endDay = String(endDateTime.getDate()).padStart(2, '0');
        const endHours = String(endDateTime.getHours()).padStart(2, '0');
        const endMinutes = String(endDateTime.getMinutes()).padStart(2, '0');
        return `${endYear}-${endMonth}-${endDay}T${endHours}:${endMinutes}:00`;
    }

    function formatDateTime(dateTimeString) {
        if (!dateTimeString) {
            return '';
        }
        const date = new Date(dateTimeString);
        return date.toLocaleString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
    }

    function formatDateTimeLocal(dateTimeString) {
        return parseExamDateTimeForInput(dateTimeString);
    }

    function normalizeExamAttachments(rawExam) {
        if (!rawExam || typeof rawExam !== 'object') {
            return [];
        }

        if (Array.isArray(rawExam.attachments)) {
            return rawExam.attachments;
        }

        if (rawExam.exam && Array.isArray(rawExam.exam.attachments)) {
            return rawExam.exam.attachments;
        }

        return [];
    }

    function renderExamAttachmentLinksHtml(attachments) {
        if (!Array.isArray(attachments) || attachments.length === 0) {
            return '';
        }

        const items = attachments.map(attachment => {
            const name = attachment.name || attachment.originalFilename || '附件';
            const size = attachment.size ? ` · ${(Number(attachment.size) / 1024).toFixed(1)} KB` : '';
            const path = attachment.downloadUrl || `/api/attachments/exam/${attachment.id}/download`;
            return `
                <button type="button" class="btn btn-outline-primary btn-sm me-2 mb-2" data-download-url="${escapeHtml(path)}" data-download-name="${escapeHtml(name)}" onclick="downloadAttachmentFromButton(this)">
                    <i class="fa fa-download"></i> ${escapeHtml(name)}${escapeHtml(size)}
                </button>
            `;
        }).join('');

        return `
            <div class="mb-3">
                <strong>附件:</strong>
                <div class="mt-2">${items}</div>
            </div>
        `;
    }

    function renderEditExamAttachments(attachments) {
        const container = document.getElementById('edit-exam-existing-files');
        if (!container) {
            return;
        }

        if (!Array.isArray(attachments) || attachments.length === 0) {
            container.innerHTML = `
                <div class="card-body text-muted">
                    <i class="fa fa-paperclip"></i> 暂无已上传附件
                </div>
            `;
            return;
        }

        const items = attachments.map(attachment => {
            const name = attachment.name || attachment.originalFilename || '附件';
            const size = attachment.size ? ` · ${(Number(attachment.size) / 1024).toFixed(1)} KB` : '';
            const path = attachment.downloadUrl || `/api/attachments/exam/${attachment.id}/download`;
            return `
                <div class="d-flex align-items-center justify-content-between gap-3 py-2 border-bottom">
                    <div>
                        <i class="fa fa-file"></i>
                        <button type="button" class="btn btn-link p-0 align-baseline" data-download-url="${escapeHtml(path)}" data-download-name="${escapeHtml(name)}" onclick="downloadAttachmentFromButton(this)">${escapeHtml(name)}</button>
                        <small class="text-muted">${escapeHtml(size)}</small>
                    </div>
                </div>
            `;
        }).join('');

        container.innerHTML = `<div class="card-body">${items}</div>`;
    }

    async function viewExam(examId) {
        console.log('查看考试:', examId);
        try {
            showLoading();
            const apiService = new APIService();
            const teacherAPI = new TeacherAPI(apiService);

            const response = await teacherAPI.getExamById(examId);
            if (response.success) {
                const exam = {
                    ...response.data,
                    attachments: normalizeExamAttachments(response.data)
                };
                const viewExamBody = document.getElementById('viewExamBody');
                if (viewExamBody) {
                    viewExamBody.innerHTML = `
                        <div class="exam-detail">
                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <strong>考试标题:</strong> ${exam.title}
                                </div>
                                <div class="col-md-6">
                                    <strong>课程:</strong> ${courseMap.get(exam.courseId) || '未知课程'}
                                </div>
                            </div>
                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <strong>开始时间:</strong> ${formatDateTime(exam.startTime)}
                                </div>
                                <div class="col-md-6">
                                    <strong>结束时间:</strong> ${formatDateTime(exam.endTime)}
                                </div>
                            </div>
                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <strong>考试时长:</strong> ${exam.duration} 分钟
                                </div>
                                <div class="col-md-6">
                                    <strong>状态:</strong> ${exam.isActive ? '激活' : '已关闭'}
                                </div>
                            </div>
                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <strong>考试类型:</strong> ${exam.isOnline ? '线上考试' : '线下考试'}
                                </div>
                                <div class="col-md-6">
                                    <strong>考试地点:</strong> ${exam.location || '线上'}
                                </div>
                            </div>
                            <div class="mb-3">
                                <strong>考试说明:</strong>
                                <div class="mt-2">${escapeHtml(exam.description)}</div>
                            </div>
                            ${renderExamAttachmentLinksHtml(exam.attachments)}
                        </div>
                    `;
                }
                const viewExamTitle = document.getElementById('viewExamModalLabel');
                if (viewExamTitle) {
                    viewExamTitle.textContent = '考试详情';
                }
                const viewExamSubmissionsButton = document.getElementById('view-exam-submissions-btn');
                if (viewExamSubmissionsButton) {
                    viewExamSubmissionsButton.dataset.examId = String(examId);
                    delete viewExamSubmissionsButton.dataset.assignmentId;
                    viewExamSubmissionsButton.textContent = '查看提交列表';
                }
                const modal = new bootstrap.Modal(document.getElementById('viewExamModal'));
                modal.show();
            } else {
                showMessage('获取考试详情失败', 'error');
            }
        } catch (error) {
            console.error('查看考试失败:', error);
            showMessage('查看考试失败', 'error');
        } finally {
            hideLoading();
        }
    }

    async function editExam(examId) {
        console.log('编辑考试:', examId);
        try {
            showLoading();
            const apiService = new APIService();
            const teacherAPI = new TeacherAPI(apiService);

            const response = await teacherAPI.getExamById(examId);
            if (response.success) {
                const exam = {
                    ...response.data,
                    attachments: normalizeExamAttachments(response.data)
                };
                document.getElementById('edit-exam-id').value = exam.id;
                document.getElementById('edit-exam-title').value = exam.title;
                document.getElementById('edit-exam-course').value = exam.courseId;
                document.getElementById('edit-exam-description').value = exam.description;
                document.getElementById('edit-exam-start').value = parseExamDateTimeForInput(exam.startTime);
                document.getElementById('edit-exam-duration').value = exam.duration;
                renderEditExamAttachments(exam.attachments);

                const modal = new bootstrap.Modal(document.getElementById('editExamModal'));
                modal.show();
            } else {
                showMessage('获取考试详情失败', 'error');
            }
        } catch (error) {
            console.error('编辑考试失败:', error);
            showMessage('编辑考试失败', 'error');
        } finally {
            hideLoading();
        }
    }

    async function deleteExam(examId) {
        if (!confirm('确定要删除这个考试吗？')) {
            return;
        }

        showLoading();

        try {
            const apiService = new APIService();
            const teacherAPI = new TeacherAPI(apiService);
            const response = await teacherAPI.deleteExam(examId);

            hideLoading();

            if (response && response.success) {
                showMessage('删除考试成功', 'success');
                await loadExams();
                if (typeof refreshTeacherAssignmentsSummary === 'function') {
                    await refreshTeacherAssignmentsSummary();
                }
            } else {
                showMessage('删除考试失败：' + (response.message || '未知错误'), 'error');
            }
        } catch (error) {
            hideLoading();
            console.error('删除考试失败:', error);
            showMessage('删除考试失败：' + error.message, 'error');
        }
    }

    async function submitEditExam() {
        try {
            const examId = document.getElementById('edit-exam-id').value;
            const examTitle = document.getElementById('edit-exam-title').value;
            const examCourse = document.getElementById('edit-exam-course').value;
            const examDescription = document.getElementById('edit-exam-description').value;
            const examStart = document.getElementById('edit-exam-start').value;
            const examDuration = document.getElementById('edit-exam-duration').value;

            if (!examId || !examTitle || !examCourse || !examStart || !examDuration) {
                showMessage('请填写完整的考试信息', 'error');
                return;
            }

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

            const fileInput = document.getElementById('edit-exam-papers');
            let requestBody = examData;
            if (fileInput && fileInput.files && fileInput.files.length > 0) {
                const formData = new FormData();
                formData.append('payload', new Blob([JSON.stringify(examData)], { type: 'application/json' }));
                Array.from(fileInput.files).forEach(file => formData.append('files', file));
                requestBody = formData;
            }

            const response = await teacherAPI.updateExam(parseInt(examId), requestBody);

            hideLoading();

            if (response && response.success) {
                showMessage('考试更新成功', 'success');
                const modal = bootstrap.Modal.getInstance(document.getElementById('editExamModal'));
                modal.hide();
                await loadExams();
                if (typeof refreshTeacherAssignmentsSummary === 'function') {
                    await refreshTeacherAssignmentsSummary();
                }
            } else {
                showMessage('考试更新失败：' + (response.message || '未知错误'), 'error');
            }
        } catch (error) {
            hideLoading();
            console.error('更新考试失败:', error);
            showMessage('考试更新失败：' + error.message, 'error');
        }
    }

    global.formatDateTime = formatDateTime;
    global.formatDateTimeLocal = formatDateTimeLocal;
    global.serializeExamLocalDateTime = serializeExamLocalDateTime;
    global.parseExamDateTimeForInput = parseExamDateTimeForInput;
    global.buildExamEndLocalDateTime = buildExamEndLocalDateTime;
    global.viewExam = viewExam;
    global.editExam = editExam;
    global.deleteExam = deleteExam;
    global.submitEditExam = submitEditExam;
})(window);
