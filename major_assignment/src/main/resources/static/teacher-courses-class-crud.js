(function attachTeacherCoursesClassCrud(global) {
    function clearClassFormState(form) {
        if (!form) {
            return;
        }

        form.reset();
        form.querySelectorAll('input, select').forEach(input => {
            input.classList.remove('is-valid', 'is-invalid');
        });
        form.querySelectorAll('.form-control-feedback').forEach(feedback => {
            feedback.innerHTML = '';
            feedback.className = 'form-control-feedback';
        });
    }

    function showDuplicateClassNameFeedback(message) {
        const classNameInput = document.getElementById('add-class-name');
        if (!classNameInput) {
            return;
        }

        classNameInput.focus();
        classNameInput.classList.add('is-invalid');

        const feedback = document.getElementById('add-class-name-feedback');
        if (feedback) {
            feedback.innerHTML = `<i class="fa fa-times"></i> ${message}`;
            feedback.className = 'form-control-feedback invalid';
        }
    }

    function resolveClassCreateErrorMessage(data) {
        let errorMessage = '添加班级失败';
        if (!data || !data.message) {
            return errorMessage;
        }

        errorMessage = data.message;
        if (errorMessage.includes('unique_constraint') || errorMessage.includes('Duplicate entry') || errorMessage.includes('唯一约束')) {
            const duplicateMessage = '该班级名称已存在，请使用其他名称';
            showDuplicateClassNameFeedback(duplicateMessage);
            return duplicateMessage;
        }
        if (errorMessage.includes('外键')) {
            return '选择的关联数据不存在，请检查输入';
        }
        if (errorMessage.includes('非空')) {
            return '请填写所有必填字段';
        }
        return errorMessage;
    }

    async function deleteClass(classId) {
        if (!confirm('确定要删除这个班级吗？删除后将无法恢复。')) {
            return;
        }

        try {
            const csrfToken = typeof getCsrfToken === 'function' ? getCsrfToken() : null;
            const response = await fetch(`/api/teacher/classes/${classId}`, {
                method: 'DELETE',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json',
                    ...(csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : {})
                }
            });

            if (!response.ok) {
                throw new Error(`删除班级失败: ${response.status}`);
            }

            const data = await response.json();
            if (!data.success) {
                throw new Error(data.message || '删除班级失败');
            }

            showNotification('班级删除成功', 'success');
            await performClassSearch();
        } catch (error) {
            showNotification(error.message || '删除班级失败', 'danger');
            console.error('删除班级错误:', error);
        }
    }

    async function submitAddClass() {
        const form = document.getElementById('addClassForm');
        if (!await validateForm('addClassForm')) {
            return;
        }

        const formData = new FormData(form);
        const user = getCurrentUser();
        if (!user) {
            showNotification('用户信息获取失败，请重新登录', 'danger');
            return;
        }

        const classData = {
            className: formData.get('className'),
            year: parseInt(formData.get('classGrade'), 10),
            capacity: parseInt(formData.get('classStudents'), 10),
            majorId: parseInt(formData.get('classMajor'), 10),
            teacherId: user.id,
            courseId: parseInt(formData.get('classCourse'), 10)
        };

        const saveBtn = form.closest('.modal').querySelector('.btn-primary');
        try {
            showLoading(saveBtn);
            const csrfToken = typeof getCsrfToken === 'function' ? getCsrfToken() : null;
            const response = await fetch('/api/teacher/classes', {
                method: 'POST',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json',
                    ...(csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : {})
                },
                body: JSON.stringify(classData)
            });

            const data = await response.json();
            if (!response.ok || !data.success) {
                throw new Error(resolveClassCreateErrorMessage(data));
            }

            const modal = bootstrap.Modal.getInstance(document.getElementById('addClassModal'));
            if (modal) {
                modal.hide();
            }

            showNotification('班级添加成功，已创建新的班级', 'success');
            await performClassSearch();
            clearClassFormState(form);
        } catch (error) {
            showNotification(error.message || '添加班级失败，请检查输入并重试', 'danger');
            console.error('添加班级错误:', error);
        } finally {
            hideLoading(saveBtn);
        }
    }

    global.deleteClass = deleteClass;
    global.submitAddClass = submitAddClass;
})(window);
