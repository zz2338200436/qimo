(function attachTeacherAssignmentsAssignmentCrud(global) {
    function escapeHtml(value) {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function renderAssignmentDescriptionHtml(value) {
        const content = String(value ?? '').trim() || '暂无描述';
        return `<div class="assignment-description-text border rounded p-3 bg-light" style="white-space: pre-wrap;">${escapeHtml(content)}</div>`;
    }

    function normalizeAssignmentAttachments(rawAssignment) {
        if (!rawAssignment || typeof rawAssignment !== 'object') {
            return [];
        }

        if (Array.isArray(rawAssignment.attachments)) {
            return rawAssignment.attachments;
        }

        if (rawAssignment.assignment && Array.isArray(rawAssignment.assignment.attachments)) {
            return rawAssignment.assignment.attachments;
        }

        return [];
    }

    function normalizeAssignmentDetail(rawData) {
        const assignment = rawData?.assignment || rawData || {};
        return {
            ...assignment,
            attachments: normalizeAssignmentAttachments(rawData)
        };
    }

    function renderAssignmentAttachmentLinksHtml(attachments) {
        if (!Array.isArray(attachments) || attachments.length === 0) {
            return '';
        }

        const items = attachments.map(attachment => {
            const name = attachment.name || attachment.originalFilename || '附件';
            const size = attachment.size ? ` · ${(Number(attachment.size) / 1024).toFixed(1)} KB` : '';
            const path = attachment.downloadUrl || `/api/attachments/${attachment.id}/download`;
            const href = path.startsWith('http') ? path : path;
            return `
                <button type="button" class="btn btn-outline-primary btn-sm me-2 mb-2" data-download-url="${escapeHtml(href)}" data-download-name="${escapeHtml(name)}" onclick="downloadAttachmentFromButton(this)">
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

    function renderEditAssignmentAttachments(attachments) {
        const container = document.getElementById('edit-assignment-existing-files');
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
            const path = attachment.downloadUrl || `/api/attachments/${attachment.id}/download`;
            const href = path.startsWith('http') ? path : path;
            return `
                <div class="d-flex align-items-center justify-content-between gap-3 py-2 border-bottom">
                    <div>
                        <i class="fa fa-file"></i>
                        <button type="button" class="btn btn-link p-0 align-baseline" data-download-url="${escapeHtml(href)}" data-download-name="${escapeHtml(name)}" onclick="downloadAttachmentFromButton(this)">${escapeHtml(name)}</button>
                        <small class="text-muted">${escapeHtml(size)}</small>
                    </div>
                </div>
            `;
        }).join('');

        container.innerHTML = `<div class="card-body">${items}</div>`;
    }

    async function viewAssignment(assignmentId) {
        console.log('查看作业:', assignmentId);
        try {
            showLoading();
            const apiService = new APIService();
            const teacherAPI = new TeacherAPI(apiService);

            const response = await teacherAPI.getAssignmentById(assignmentId);
            if (response.success) {
                const assignment = normalizeAssignmentDetail(response.data);
                const viewExamBody = document.getElementById('viewExamBody');
                if (viewExamBody) {
                    viewExamBody.innerHTML = `
                        <div class="assignment-detail">
                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <strong>作业标题:</strong> ${assignment.title}
                                </div>
                                <div class="col-md-6">
                                    <strong>课程:</strong> ${courseMap.get(assignment.courseId) || '未知课程'}
                                </div>
                            </div>
                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <strong>发布日期:</strong> ${formatDateTime(assignment.publishDate)}
                                </div>
                                <div class="col-md-6">
                                    <strong>截止日期:</strong> ${formatDateTime(assignment.dueDate)}
                                </div>
                            </div>
                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <strong>状态:</strong> ${assignment.isActive ? '激活' : '已关闭'}
                                </div>
                            </div>
                            <div class="mb-3">
                                <strong>作业描述:</strong>
                                <div class="mt-2">${renderAssignmentDescriptionHtml(assignment.description)}</div>
                            </div>
                            ${renderAssignmentAttachmentLinksHtml(assignment.attachments)}
                        </div>
                    `;
                }
                const viewExamTitle = document.getElementById('viewExamModalLabel');
                if (viewExamTitle) {
                    viewExamTitle.textContent = '作业详情';
                }
                const viewExamSubmissionsButton = document.getElementById('view-exam-submissions-btn');
                if (viewExamSubmissionsButton) {
                    viewExamSubmissionsButton.dataset.assignmentId = String(assignmentId);
                    delete viewExamSubmissionsButton.dataset.examId;
                    viewExamSubmissionsButton.textContent = '查看提交列表';
                }
                const modal = new bootstrap.Modal(document.getElementById('viewExamModal'));
                modal.show();
            } else {
                showMessage('获取作业详情失败', 'error');
            }
        } catch (error) {
            console.error('查看作业失败:', error);
            showMessage('查看作业失败', 'error');
        } finally {
            hideLoading();
        }
    }

    async function editAssignment(assignmentId) {
        console.log('编辑作业:', assignmentId);
        try {
            showLoading();
            const apiService = new APIService();
            const teacherAPI = new TeacherAPI(apiService);

            const response = await teacherAPI.getAssignmentById(assignmentId);
            if (response && response.success) {
                const assignment = normalizeAssignmentDetail(response.data);

                document.getElementById('edit-assignment-id').value = assignment.id || '';
                document.getElementById('edit-assignment-title').value = assignment.title || '';
                document.getElementById('edit-assignment-course').value = assignment.courseId || '';
                document.getElementById('edit-assignment-description').value = assignment.description || '';
                renderEditAssignmentAttachments(assignment.attachments);

                if (assignment.publishDate) {
                    document.getElementById('edit-assignment-start').value = formatDateTimeLocal(assignment.publishDate);
                }
                if (assignment.dueDate) {
                    document.getElementById('edit-assignment-end').value = formatDateTimeLocal(assignment.dueDate);
                }

                const statusField = document.getElementById('edit-assignment-status');
                if (statusField) {
                    statusField.value = assignment.isActive ? 'active' : 'inactive';
                }

                const scoreField = document.getElementById('edit-assignment-score');
                if (scoreField && assignment.maxScore) {
                    scoreField.value = assignment.maxScore;
                }

                const modal = new bootstrap.Modal(document.getElementById('editAssignmentModal'));
                modal.show();
            } else {
                showMessage('获取作业详情失败: ' + (response?.message || '未知错误'), 'error');
            }
        } catch (error) {
            console.error('编辑作业失败:', error);
            showMessage('编辑作业失败: ' + error.message, 'error');
        } finally {
            hideLoading();
        }
    }

    async function deleteAssignment(assignmentId) {
        if (!confirm('确定要删除这个作业吗？')) {
            return;
        }

        showLoading();

        try {
            const apiService = new APIService();
            const teacherAPI = new TeacherAPI(apiService);
            const response = await teacherAPI.deleteAssignment(assignmentId);

            hideLoading();

            if (!response || response.success === false) {
                showMessage('删除作业失败：' + (response?.message || '未知错误'), 'error');
            } else {
                showMessage('删除作业成功', 'success');
                await loadAssignments();
                if (typeof refreshTeacherAssignmentsSummary === 'function') {
                    await refreshTeacherAssignmentsSummary();
                }
            }
        } catch (error) {
            hideLoading();
            console.error('删除作业失败:', error);
            showMessage('删除作业失败：' + error.message, 'error');
        }
    }

    async function submitEditAssignment() {
        try {
            const assignmentId = document.getElementById('edit-assignment-id').value;
            const assignmentTitle = document.getElementById('edit-assignment-title').value;
            const assignmentCourse = document.getElementById('edit-assignment-course').value;
            const assignmentDescription = document.getElementById('edit-assignment-description').value;
            const assignmentStart = document.getElementById('edit-assignment-start').value;
            const assignmentEnd = document.getElementById('edit-assignment-end').value;

            if (!assignmentId || !assignmentTitle || !assignmentCourse || !assignmentStart || !assignmentEnd) {
                showMessage('请填写完整的作业信息', 'error');
                return;
            }

            const publishDate = new Date(assignmentStart);
            const dueDate = new Date(assignmentEnd);

            if (isNaN(publishDate.getTime()) || isNaN(dueDate.getTime())) {
                showMessage('日期格式不正确', 'error');
                return;
            }

            if (dueDate <= publishDate) {
                showMessage('截止日期必须晚于发布时间', 'error');
                return;
            }

            showLoading();

            const assignmentData = {
                title: assignmentTitle,
                courseId: parseInt(assignmentCourse),
                description: assignmentDescription,
                publishDate: publishDate.toISOString(),
                dueDate: dueDate.toISOString(),
                isActive: true
            };

            const scoreField = document.getElementById('edit-assignment-score');
            if (scoreField && scoreField.value) {
                assignmentData.maxScore = parseInt(scoreField.value);
            }

            const fileInput = document.getElementById('edit-assignment-files');
            let requestBody = assignmentData;
            if (fileInput && fileInput.files && fileInput.files.length > 0) {
                const formData = new FormData();
                formData.append('payload', new Blob([JSON.stringify(assignmentData)], { type: 'application/json' }));
                Array.from(fileInput.files).forEach(file => formData.append('files', file));
                requestBody = formData;
            }

            const apiService = new APIService();
            const teacherAPI = new TeacherAPI(apiService);
            const response = await teacherAPI.updateAssignment(parseInt(assignmentId), requestBody);

            hideLoading();

            if (response && (response.success || response.code === 200)) {
                showMessage('作业更新成功', 'success');
                const modal = bootstrap.Modal.getInstance(document.getElementById('editAssignmentModal'));
                if (modal) {
                    modal.hide();
                }
                await loadAssignments();
                if (typeof refreshTeacherAssignmentsSummary === 'function') {
                    await refreshTeacherAssignmentsSummary();
                }
            } else {
                showMessage('作业更新失败：' + (response?.message || '未知错误'), 'error');
            }
        } catch (error) {
            hideLoading();
            console.error('更新作业失败:', error);
            showMessage('作业更新失败：' + error.message, 'error');
        }
    }

    global.viewAssignment = viewAssignment;
    global.editAssignment = editAssignment;
    global.deleteAssignment = deleteAssignment;
    global.submitEditAssignment = submitEditAssignment;
})(window);
