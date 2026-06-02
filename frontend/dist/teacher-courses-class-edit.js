(function attachTeacherCoursesClassEdit(global) {
    async function submitEditClass() {
        const form = document.getElementById('editClassForm');

        if (!await validateForm('editClassForm')) {
            return;
        }

        const formData = new FormData(form);
        const classId = parseInt(formData.get('id'));
        const user = getCurrentUser();
        if (!user) {
            showNotification('用户信息获取失败，请重新登录', 'danger');
            return;
        }

        const originalCourseId = document.getElementById('edit-class-course-id').value;
        const originalMajorId = document.getElementById('edit-class-major-id').value;

        console.log('表单数据:', Object.fromEntries(formData));
        console.log('从DOM获取的隐藏字段值 - courseId:', originalCourseId, 'majorId:', originalMajorId);

        const majorSelect = document.getElementById('edit-class-major');
        console.log('专业下拉框元素:', majorSelect);
        if (majorSelect) {
            console.log('专业下拉框当前选中值:', majorSelect.value);
            console.log('专业下拉框选项数量:', majorSelect.options.length);
            console.log('专业下拉框所有选项:');
            for (let i = 0; i < majorSelect.options.length; i++) {
                const option = majorSelect.options[i];
                console.log(`  选项${i}: value=${option.value}, text=${option.textContent}, selected=${option.selected}`);
            }
        }

        const majorValue = formData.get('classMajor');
        console.log('从formData获取的classMajor值:', majorValue, '类型:', typeof majorValue);

        const majorSelectValue = majorSelect ? majorSelect.value : null;
        console.log('直接从DOM获取的专业值:', majorSelectValue, '类型:', typeof majorSelectValue);

        const majorId = majorSelectValue ? parseInt(majorSelectValue) : null;
        console.log('转换后的majorId:', majorId, '类型:', typeof majorId);

        if (isNaN(majorId)) {
            console.error('错误：majorId不是有效数字:', majorId);
            showNotification('专业选择无效，请重新选择', 'danger');
            return;
        }

        const classData = {
            className: formData.get('className'),
            year: parseInt(formData.get('classGrade')),
            capacity: parseInt(formData.get('classStudents')),
            majorId,
            teacherId: user.id,
            courseId: originalCourseId ? parseInt(originalCourseId) : null
        };

        console.log('最终提交的数据:', JSON.stringify(classData, null, 2));
        console.log('最终提交的数据:', classData);

        try {
            const saveBtn = form.closest('.modal').querySelector('.btn-primary');
            showLoading(saveBtn);

            const apiUrl = `/api/teacher/classes/${classId}`;
            console.log('调用API更新班级:', apiUrl);
            console.log('请求数据:', classData);

            const response = await fetch(apiUrl, {
                method: 'PUT',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(classData)
            });

            console.log('API响应状态:', response.status);
            console.log('API响应状态文本:', response.statusText);

            if (!response.ok) {
                const errorText = await response.text();
                console.error('API响应错误内容:', errorText);
                throw new Error(`更新班级失败: ${response.status} - ${errorText}`);
            }

            const data = await response.json();
            console.log('API响应数据:', JSON.stringify(data, null, 2));

            if (data.success) {
                const modal = bootstrap.Modal.getInstance(document.getElementById('editClassModal'));
                modal.hide();
                showNotification('班级信息更新成功，已保存修改', 'success');
                performClassSearch();
            } else {
                console.error('更新班级失败:', data.message);
                showNotification(`更新班级失败: ${data.message}`, 'danger');
            }
        } catch (error) {
            showNotification(error.message || '更新班级失败', 'danger');
            console.error('更新班级错误:', error);
        } finally {
            const saveBtn = form.closest('.modal').querySelector('.btn-primary');
            hideLoading(saveBtn);
        }
    }

    async function validateFieldEvent(event) {
        const field = event.target;
        const feedbackId = `${field.id}-feedback`;
        await validateField(field, feedbackId);
    }

    async function editClass(classId) {
        try {
            const response = await fetch(`/api/teacher/classes/${classId}`, {
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const responseData = await response.json();

            if (!responseData.success) {
                throw new Error(responseData.message || '获取班级详情失败');
            }

            const cls = typeof extractTeacherSingleRecord === 'function'
                ? extractTeacherSingleRecord(responseData)
                : responseData.data;

            if (!cls) {
                showNotification('班级不存在', 'warning');
                return;
            }

            const idField = document.getElementById('edit-class-id');
            const nameField = document.getElementById('edit-class-name');
            const gradeField = document.getElementById('edit-class-grade');
            const majorField = document.getElementById('edit-class-major');
            const advisorField = document.getElementById('edit-class-advisor');
            const studentsField = document.getElementById('edit-class-students');

            idField.value = cls.id;
            nameField.value = cls.className;
            gradeField.value = cls.grade || cls.classGrade || cls.year || '';

            console.log('班级详情中的专业信息 - majorId:', cls.majorId, 'majorName:', cls.majorName, 'classMajor:', cls.classMajor);

            const classMajorId = cls.majorId || cls.major_id || 1;
            majorField.value = classMajorId;
            console.log('设置专业字段的值为:', classMajorId);

            advisorField.value = cls.teacherName || cls.classAdvisor;
            studentsField.value = cls.capacity || cls.studentCount || cls.classStudents;
            nameField.dataset.originalValue = cls.className;

            const user = getCurrentUser();
            if (user) {
                advisorField.value = user.name || user.username;
                advisorField.readOnly = true;
                console.log('当前登录用户:', user);
            }

            const courseIdValue = cls.courseId || cls.course_id;
            const majorIdValue = cls.majorId || cls.major_id;

            console.log('保存的courseId:', courseIdValue, 'majorId:', majorIdValue);

            document.getElementById('edit-class-course-id').value = courseIdValue;
            document.getElementById('edit-class-major-id').value = majorIdValue;

            const modal = new bootstrap.Modal(document.getElementById('editClassModal'));
            modal.show();

            const editForm = document.getElementById('editClassForm');
            const inputs = editForm.querySelectorAll('input, select');
            inputs.forEach(input => {
                input.removeEventListener('input', validateFieldEvent);
                input.removeEventListener('change', validateFieldEvent);
                input.addEventListener('input', validateFieldEvent);
                input.addEventListener('change', validateFieldEvent);
            });
        } catch (error) {
            handleApiError(error);
        }
    }

    global.submitEditClass = submitEditClass;
    global.editClass = editClass;
})(window);
