(function attachTeacherCoursesCourseCrud(global) {
    async function submitAddCourse() {
        const isValid = await validateForm('addCourseForm');
        if (!isValid) {
            return;
        }

        try {
            const formData = {
                courseCode: document.getElementById('add-course-code').value,
                courseName: document.getElementById('add-course-name').value,
                courseCategory: document.getElementById('add-course-category').value,
                credit: parseFloat(document.getElementById('add-course-credits').value),
                totalHours: parseInt(document.getElementById('add-course-total-hours').value),
                teacherId: parseInt(document.getElementById('add-course-teacher-id').value),
                courseDirector: parseInt(document.getElementById('add-course-director').value),
                assessmentMethod: document.getElementById('add-course-assessment-method').value,
                courseStatus: document.getElementById('add-course-status').value,
                semester: document.getElementById('add-course-semester').value,
                startDate: document.getElementById('add-course-start-date').value,
                endDate: document.getElementById('add-course-end-date').value,
                maxStudents: parseInt(document.getElementById('add-course-max-students').value),
                description: document.getElementById('add-course-description').value
            };

            const result = await teacherAPI.createCourse(formData);
            if (!result || result.success === false) {
                throw new Error(result?.message || '创建课程失败');
            }

            showNotification('课程创建成功', 'success');

            const modal = bootstrap.Modal.getInstance(document.getElementById('addCourseModal'));
            modal.hide();

            const form = document.getElementById('addCourseForm');
            form.reset();
            if (typeof resetFormValidation === 'function') {
                resetFormValidation('addCourseForm');
            }

            await loadCourses();
        } catch (error) {
            handleApiError(error);
        }
    }

    async function editCourse(courseId, options = {}) {
        try {
            const response = await teacherAPI.getCourseById(courseId);
            const course = typeof extractTeacherSingleRecord === 'function'
                ? extractTeacherSingleRecord(response)
                : response?.data;

            if (!course) {
                showNotification('课程不存在', 'warning');
                return;
            }

            document.getElementById('edit-course-id').value = course.id;
            document.getElementById('edit-course-code').value = course.courseCode;

            const courseCodeInput = document.getElementById('edit-course-code');
            courseCodeInput.dataset.originalValue = course.courseCode;

            document.getElementById('edit-course-name').value = course.courseName;
            const categorySelect = document.getElementById('edit-course-category');
            const statusSelect = document.getElementById('edit-course-status');
            if (typeof ensureSelectHasOption === 'function') {
                ensureSelectHasOption(categorySelect, course.courseCategory, course.courseCategory);
                ensureSelectHasOption(statusSelect, course.courseStatus, course.courseStatus);
            }
            categorySelect.value = course.courseCategory;
            document.getElementById('edit-course-credits').value = course.credit;
            document.getElementById('edit-course-total-hours').value = course.totalHours;
            document.getElementById('edit-course-director').value = course.courseDirector;
            document.getElementById('edit-course-assessment-method').value = course.assessmentMethod;
            statusSelect.value = course.courseStatus;
            document.getElementById('edit-course-semester').value = course.semester;
            document.getElementById('edit-course-start-date').value = course.startDate ? course.startDate.split('T')[0] : '';
            document.getElementById('edit-course-end-date').value = course.endDate ? course.endDate.split('T')[0] : '';
            document.getElementById('edit-course-max-students').value = course.maxStudents;
            document.getElementById('edit-course-description').value = course.description || '';

            const isReadOnly = !!options.readOnly;
            const form = document.getElementById('editCourseForm');
            if (form) {
                form.querySelectorAll('input, select, textarea').forEach(field => {
                    if (field.id === 'edit-course-id') {
                        field.readOnly = true;
                        return;
                    }
                    if (field.tagName === 'SELECT') {
                        field.disabled = isReadOnly;
                    } else {
                        field.readOnly = isReadOnly;
                    }
                });
            }

            const modalTitle = document.getElementById('editCourseModalLabel');
            if (modalTitle) {
                modalTitle.textContent = isReadOnly ? `课程详情 - ${course.courseName || course.courseCode || course.id}` : '编辑课程';
            }

            const saveButton = document.querySelector('#editCourseModal .modal-footer .btn-primary');
            if (saveButton) {
                saveButton.style.display = isReadOnly ? 'none' : '';
            }

            const modal = new bootstrap.Modal(document.getElementById('editCourseModal'));
            modal.show();
        } catch (error) {
            handleApiError(error);
        }
    }

    async function submitEditCourse() {
        const isValid = await validateForm('editCourseForm');
        if (!isValid) {
            return;
        }

        const courseId = parseInt(document.getElementById('edit-course-id').value);
        const formData = {
            courseCode: document.getElementById('edit-course-code').value,
            courseName: document.getElementById('edit-course-name').value,
            courseCategory: document.getElementById('edit-course-category').value,
            credit: parseInt(document.getElementById('edit-course-credits').value),
            totalHours: parseInt(document.getElementById('edit-course-total-hours').value),
            teacherId: parseInt(document.getElementById('edit-course-director').value),
            courseDirector: parseInt(document.getElementById('edit-course-director').value),
            assessmentMethod: document.getElementById('edit-course-assessment-method').value,
            courseStatus: document.getElementById('edit-course-status').value,
            semester: document.getElementById('edit-course-semester').value,
            startDate: document.getElementById('edit-course-start-date').value,
            endDate: document.getElementById('edit-course-end-date').value,
            maxStudents: parseInt(document.getElementById('edit-course-max-students').value),
            description: document.getElementById('edit-course-description').value
        };

        try {
            await teacherAPI.updateCourse(courseId, formData);
            showNotification('课程更新成功', 'success');

            const modal = bootstrap.Modal.getInstance(document.getElementById('editCourseModal'));
            modal.hide();

            loadCourses();
        } catch (error) {
            handleApiError(error);
        }
    }

    async function deleteCourse(courseId) {
        if (!confirm('确定要删除该课程吗？')) {
            return;
        }

        try {
            await teacherAPI.deleteCourse(courseId);
            showNotification('课程删除成功', 'success');
            loadCourses();
        } catch (error) {
            handleApiError(error);
        }
    }

    global.submitAddCourse = submitAddCourse;
    global.editCourse = editCourse;
    global.submitEditCourse = submitEditCourse;
    global.deleteCourse = deleteCourse;
})(window);
