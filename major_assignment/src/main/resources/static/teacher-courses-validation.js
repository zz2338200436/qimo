(function attachTeacherCoursesValidation(global) {
    function getFieldLabel(field) {
        const label = field.closest('.form-group')?.querySelector('label');
        return label ? label.textContent.trim().replace('*', '') : '该字段';
    }

    async function checkCourseCodeExists(courseCode, currentCourseId = null) {
        try {
            const response = await teacherAPI.getCourses({ page: 1, size: 100 });
            const courses = typeof extractTeacherApiList === 'function'
                ? extractTeacherApiList(response, ['courses'])
                : [];

            return courses.some(course => course.courseCode === courseCode && course.id !== currentCourseId);
        } catch (error) {
            console.error('检查课程代码失败:', error);
            return false;
        }
    }

    async function checkClassNameExists(className, currentClassId = null) {
        const normalizedClassName = (className || '').trim().toLowerCase();

        const fallbackExistsCheck = () => {
            const classes = typeof getTeacherCoursesAllClasses === 'function'
                ? getTeacherCoursesAllClasses()
                : global.allClasses;
            if (!normalizedClassName || !Array.isArray(classes)) {
                return false;
            }

            return classes.some(cls => {
                const existingName = String(cls.className || cls.name || '').trim().toLowerCase();
                const existingId = cls.id != null ? Number(cls.id) : null;
                return existingName === normalizedClassName && existingId !== currentClassId;
            });
        };

        const shouldUseLocalFallbackOnly = window.location.hostname === 'localhost'
            && window.location.port === '5500'
            && !!sessionStorage.getItem('token')
            && !document.cookie.includes('JSESSIONID');

        if (shouldUseLocalFallbackOnly) {
            return fallbackExistsCheck();
        }

        try {
            const response = await fetch(`/api/teacher/check-class-name?className=${encodeURIComponent(className)}${currentClassId ? `&classId=${currentClassId}` : ''}`, {
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                return fallbackExistsCheck();
            }

            const data = await response.json();
            if (data.success === false) {
                return fallbackExistsCheck();
            }

            let existsValue;
            if (data.data && typeof data.data === 'object') {
                existsValue = data.data.exists;
            } else {
                existsValue = data.exists;
            }

            return existsValue === true || existsValue === 'true';
        } catch (error) {
            return fallbackExistsCheck();
        }
    }

    async function validateField(field, feedbackId) {
        const value = field.value ? field.value.trim() : '';
        const feedback = document.getElementById(feedbackId);
        const isRequired = field.hasAttribute('required');
        const minLength = field.getAttribute('minlength');
        const maxLength = field.getAttribute('maxlength');
        const min = field.getAttribute('min');
        const max = field.getAttribute('max');

        if (field.type === 'hidden' || !feedback) {
            return true;
        }

        field.classList.remove('is-valid', 'is-invalid');
        feedback.innerHTML = '<i class="fa fa-spinner fa-spin"></i> 验证中...';
        feedback.className = 'form-control-feedback';

        let isValid = true;
        let errorMessage = '';

        if (isRequired && value === '') {
            isValid = false;
            errorMessage = `${getFieldLabel(field)}为必填项`;
        } else if (minLength && value.length < parseInt(minLength, 10)) {
            isValid = false;
            errorMessage = `${getFieldLabel(field)}至少需要${minLength}个字符`;
        } else if (maxLength && value.length > parseInt(maxLength, 10)) {
            isValid = false;
            errorMessage = `${getFieldLabel(field)}最多允许${maxLength}个字符`;
        } else if (min && !isNaN(value) && parseFloat(value) < parseFloat(min)) {
            isValid = false;
            errorMessage = `${getFieldLabel(field)}请输入大于或等于${min}的数字`;
        } else if (max && !isNaN(value) && parseFloat(value) > parseFloat(max)) {
            isValid = false;
            errorMessage = `${getFieldLabel(field)}请输入小于或等于${max}的数字`;
        } else if ((field.id.includes('credits') || field.name === 'credit') && value !== '') {
            const credit = parseFloat(value);
            if (isNaN(credit) || credit < 1 || credit > 10) {
                isValid = false;
                errorMessage = `${getFieldLabel(field)}必须在1-10之间`;
            }
        } else if ((field.id.includes('total-hours') || field.name === 'totalHours') && value !== '') {
            const totalHours = parseInt(value, 10);
            if (isNaN(totalHours) || totalHours <= 0) {
                isValid = false;
                errorMessage = `${getFieldLabel(field)}必须大于0`;
            }
        } else if ((field.id.includes('max-students') || field.name === 'maxStudents') && value !== '') {
            const maxStudents = parseInt(value, 10);
            if (isNaN(maxStudents) || maxStudents < 10 || maxStudents > 200) {
                isValid = false;
                errorMessage = `${getFieldLabel(field)}必须在10-200之间`;
            }
        } else if ((field.type === 'number' || field.hasAttribute('min') || field.hasAttribute('max')) && value !== '' && isNaN(value)) {
            isValid = false;
            errorMessage = `${getFieldLabel(field)}请输入有效的数字`;
        } else if (field.id.includes('start-date') && value !== '') {
            const formId = field.closest('form').id;
            const endDateFieldId = formId === 'addCourseForm' ? 'add-course-end-date' : 'edit-course-end-date';
            const endDateField = document.getElementById(endDateFieldId);
            const endDateValue = endDateField.value;

            if (endDateValue && value > endDateValue) {
                isValid = false;
                errorMessage = '开始日期不能晚于结束日期';
            }

            if (endDateValue && endDateField.classList.contains('is-invalid')) {
                await validateField(endDateField, `${endDateField.id}-feedback`);
            }
        } else if (field.id.includes('end-date') && value !== '') {
            const formId = field.closest('form').id;
            const startDateFieldId = formId === 'addCourseForm' ? 'add-course-start-date' : 'edit-course-start-date';
            const startDateField = document.getElementById(startDateFieldId);
            const startDateValue = startDateField.value;

            if (startDateValue && value < startDateValue) {
                isValid = false;
                errorMessage = '结束日期不能早于开始日期';
            }

            if (startDateValue && startDateField.classList.contains('is-invalid')) {
                await validateField(startDateField, `${startDateField.id}-feedback`);
            }
        } else if ((field.id.includes('course-code') || field.name === 'courseCode') && value !== '') {
            if (value.length > 20) {
                isValid = false;
                errorMessage = '课程代码长度不能超过20个字符';
            } else {
                const formId = field.closest('form').id;
                let currentCourseId = null;

                if (formId === 'editCourseForm') {
                    const idField = document.getElementById('edit-course-id');
                    currentCourseId = idField ? parseInt(idField.value, 10) : null;
                    const originalCode = field.dataset.originalValue;
                    if (value !== originalCode) {
                        const exists = await checkCourseCodeExists(value, currentCourseId);
                        if (exists) {
                            isValid = false;
                            errorMessage = '该课程代码已被其他课程使用';
                        }
                    }
                } else if (formId === 'addCourseForm') {
                    const exists = await checkCourseCodeExists(value);
                    if (exists) {
                        isValid = false;
                        errorMessage = '该课程代码已存在';
                    }
                }
            }
        } else if ((field.id.includes('class-name') || field.name === 'className') && value !== '') {
            if (value.length < 2 || value.length > 100) {
                isValid = false;
                errorMessage = '班级名称长度必须在2-100个字符之间';
            } else {
                const formId = field.closest('form').id;
                let currentClassId = null;

                if (formId === 'editClassForm') {
                    const idField = document.getElementById('edit-class-id');
                    currentClassId = idField ? parseInt(idField.value, 10) : null;
                    const originalName = field.dataset.originalValue;
                    if (value !== originalName) {
                        const exists = await checkClassNameExists(value, currentClassId);
                        if (exists) {
                            isValid = false;
                            errorMessage = '该班级名称已被其他班级使用';
                        }
                    }
                } else if (formId === 'addClassForm') {
                    const exists = await checkClassNameExists(value);
                    if (exists) {
                        isValid = false;
                        errorMessage = '该班级名称已存在';
                    }
                }
            }
        } else if ((field.id.includes('class-advisor') || field.name === 'classAdvisor') && value !== '') {
            if (value.length < 2 || value.length > 50) {
                isValid = false;
                errorMessage = '班主任姓名长度必须在2-50个字符之间';
            } else {
                const teacherNamePattern = /^[\u4e00-\u9fa5a-zA-Z\s·.]+$/;
                if (!teacherNamePattern.test(value)) {
                    isValid = false;
                    errorMessage = '班主任姓名只能包含中文、英文、空格、·和.等字符';
                }
            }
        }

        if (isValid && value !== '') {
            field.classList.add('is-valid');
            feedback.innerHTML = '<i class="fa fa-check"></i> 输入格式正确';
            feedback.classList.add('valid');
        } else if (!isValid) {
            field.classList.add('is-invalid');
            feedback.innerHTML = `<i class="fa fa-times"></i> ${errorMessage}`;
            feedback.classList.add('invalid');
        } else {
            feedback.innerHTML = '';
        }

        return isValid;
    }

    function initFormValidation(formId) {
        const form = document.getElementById(formId);
        if (!form || form.dataset.validationBound === 'true') {
            return;
        }

        form.querySelectorAll('input, select, textarea').forEach(input => {
            if (input.hasAttribute('readonly')) {
                return;
            }

            input.addEventListener('input', async function onInput() {
                await validateField(this, `${this.id}-feedback`);
            });

            input.addEventListener('blur', async function onBlur() {
                await validateField(this, `${this.id}-feedback`);
            });

            if (input.tagName === 'SELECT') {
                input.addEventListener('change', async function onChange() {
                    await validateField(this, `${this.id}-feedback`);
                });
            }
        });

        form.dataset.validationBound = 'true';
    }

    function focusFirstInvalidField(form, isValid) {
        if (isValid) {
            return true;
        }

        const firstInvalid = form.querySelector('.is-invalid');
        if (firstInvalid) {
            firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
            firstInvalid.focus();
        }
        return false;
    }

    async function validateCourseForm(formId) {
        const form = document.getElementById(formId);
        let isValid = true;

        for (const input of form.querySelectorAll('input, select, textarea')) {
            if (input.hasAttribute('readonly')) {
                continue;
            }
            const fieldValid = await validateField(input, `${input.id}-feedback`);
            isValid = isValid && fieldValid;
        }

        const startDateField = form.querySelector('[id*="start-date"]');
        const endDateField = form.querySelector('[id*="end-date"]');
        if (startDateField && endDateField) {
            const startDateValue = startDateField.value;
            const endDateValue = endDateField.value;

            if (startDateValue && endDateValue && startDateValue > endDateValue) {
                isValid = false;
                const endDateFeedback = document.getElementById(`${endDateField.id}-feedback`);
                endDateField.classList.add('is-invalid');
                if (endDateFeedback) {
                    endDateFeedback.innerHTML = '<i class="fa fa-times"></i> 结束日期不能早于开始日期';
                    endDateFeedback.className = 'form-control-feedback invalid';
                }
                startDateField.classList.remove('is-valid');
            }
        }

        return focusFirstInvalidField(form, isValid);
    }

    async function validateClassForm(formId) {
        const form = document.getElementById(formId);
        let isValid = true;

        for (const input of form.querySelectorAll('input, select, textarea')) {
            if (input.hasAttribute('readonly')) {
                continue;
            }
            const fieldValid = await validateField(input, `${input.id}-feedback`);
            isValid = isValid && fieldValid;
        }

        return focusFirstInvalidField(form, isValid);
    }

    async function validateForm(formId) {
        if (!formId) {
            console.error('Form ID is required for validateForm');
            return false;
        }

        if (formId === 'addCourseForm' || formId === 'editCourseForm') {
            return validateCourseForm(formId);
        }
        if (formId === 'addClassForm' || formId === 'editClassForm') {
            return validateClassForm(formId);
        }

        console.error(`No validation function found for form ID: ${formId}`);
        return false;
    }

    function resetFormValidation(formId) {
        const form = document.getElementById(formId);
        if (!form) {
            return;
        }

        form.querySelectorAll('input, select, textarea').forEach(field => {
            field.classList.remove('is-valid', 'is-invalid');
        });

        form.querySelectorAll('.form-control-feedback').forEach(feedback => {
            feedback.innerHTML = '';
            feedback.classList.remove('valid', 'invalid');
        });
    }

    global.checkCourseCodeExists = checkCourseCodeExists;
    global.checkClassNameExists = checkClassNameExists;
    global.validateField = validateField;
    global.initFormValidation = initFormValidation;
    global.validateCourseForm = validateCourseForm;
    global.validateClassForm = validateClassForm;
    global.validateForm = validateForm;
    global.resetFormValidation = resetFormValidation;
})(window);
