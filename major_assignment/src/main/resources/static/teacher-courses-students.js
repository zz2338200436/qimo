(function attachTeacherCoursesStudents(global) {
    let currentViewedClassId = null;

    function getCsrfToken() {
        const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
        return match ? decodeURIComponent(match[1]) : null;
    }

    async function viewClassStudents(classId) {
        currentViewedClassId = classId;

        const modal = new bootstrap.Modal(document.getElementById('viewStudentsModal'));
        modal.show();

        const modalTitle = document.getElementById('viewStudentsModalLabel');
        if (modalTitle) {
            modalTitle.textContent = '班级学生列表';
        }

        await loadClassStudents();
    }

    async function loadClassStudents() {
        if (!currentViewedClassId) {
            return;
        }

        document.getElementById('students-loading').style.display = 'none';
        document.getElementById('students-error').style.display = 'none';
        document.getElementById('students-no-results').style.display = 'none';
        document.getElementById('students-list-container').style.display = 'none';
        document.getElementById('students-loading').style.display = 'block';

        try {
            const response = await fetch(`/api/teacher/classes/${currentViewedClassId}/students`, {
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();
            document.getElementById('students-loading').style.display = 'none';

            if (data.success && data.data) {
                const students = data.data;
                if (students.length === 0) {
                    document.getElementById('students-no-results').style.display = 'block';
                } else {
                    renderStudentsList(students);
                    document.getElementById('students-list-container').style.display = 'block';
                }
            } else {
                throw new Error(data.message || '获取学生列表失败');
            }
        } catch (error) {
            document.getElementById('students-loading').style.display = 'none';

            const errorContainer = document.getElementById('students-error');
            const errorMessage = document.getElementById('students-error-message');
            errorMessage.textContent = `获取学生列表失败: ${error.message}`;
            errorContainer.style.display = 'block';
            console.error('获取学生列表错误:', error);
        }
    }

    async function addStudentToCurrentClass() {
        const classId = currentViewedClassId;
        const studentIdInput = document.getElementById('add-student-id');
        const addBtn = document.getElementById('add-student-btn');

        if (!classId) {
            showNotification('请先选择要查看的班级', 'warning');
            return;
        }

        const studentIdentifier = studentIdInput.value.trim();
        if (!studentIdentifier) {
            showNotification('请输入学生ID或用户名', 'warning');
            studentIdInput.focus();
            return;
        }

        try {
            showLoading(addBtn);
            const csrfToken = getCsrfToken();
            const requestBody = { studentIdentifier };

            const response = await fetch(`/api/teacher/classes/${classId}/students`, {
                method: 'POST',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json',
                    ...(csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : {})
                },
                body: JSON.stringify(requestBody)
            });

            const data = await response.json();
            if (data.success && data.data && data.data.needConfirm) {
                const confirmed = await confirmReplaceStudentClass(studentIdentifier, classId, data.data.message);
                if (!confirmed) {
                    return;
                }
                showNotification('学生已添加到班级', 'success');
                studentIdInput.value = '';
                await loadClassStudents();
                return;
            }

            if (!response.ok || !data.success) {
                throw new Error(data.message || '添加学生失败');
            }

            showNotification('学生已添加到班级', 'success');
            studentIdInput.value = '';
            await loadClassStudents();
        } catch (error) {
            showNotification(error.message || '添加学生失败', 'danger');
            console.error('添加学生到班级失败:', error);
        } finally {
            hideLoading(addBtn);
        }
    }

    async function confirmReplaceStudentClass(studentIdentifier, classId, message) {
        const confirmed = window.confirm(message || '该学生已在其他班级中，是否要移动到当前班级？');
        if (!confirmed) {
            return false;
        }

        const csrfToken = getCsrfToken();
        const response = await fetch(`/api/teacher/classes/${classId}/students`, {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
                ...(csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : {})
            },
            body: JSON.stringify({
                studentIdentifier,
                forceReplace: true
            })
        });

        const data = await response.json();
        if (!response.ok || !data.success) {
            throw new Error(data.message || '添加学生失败');
        }
        return true;
    }

    function renderStudentsList(students) {
        const tbody = document.getElementById('students-table-body');
        tbody.innerHTML = '';

        students.forEach((student) => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${student.id || '-'}</td>
                <td>${student.username || '-'}</td>
                <td>${student.name || '-'}</td>
                <td>${student.email || '-'}</td>
                <td>${student.phone || '-'}</td>
            `;
            tbody.appendChild(row);
        });
    }

    global.viewClassStudents = viewClassStudents;
    global.loadClassStudents = loadClassStudents;
    global.addStudentToCurrentClass = addStudentToCurrentClass;
    global.confirmReplaceStudentClass = confirmReplaceStudentClass;
    global.renderStudentsList = renderStudentsList;
    global.getCsrfToken = getCsrfToken;
})(window);
