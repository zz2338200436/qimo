(function attachTeacherCoursesAssignments(global) {
    let isLoadingAssignmentClasses = false;
    let assignmentFilterParams = {
        courseId: null,
        classId: null
    };
    let assignmentCurrentPage = 1;
    const assignmentPageSize = 10;

    async function loadClassesForAssignments() {
        if (isLoadingAssignmentClasses) {
            console.log('班级数据正在加载中，跳过重复调用');
            return;
        }

        isLoadingAssignmentClasses = true;
        try {
            const response = await fetch('/api/teacher/classes', {
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`获取班级数据失败: ${response.status}`);
            }

            const data = await response.json();
            if (!data.success) {
                throw new Error(data.message || '获取班级数据失败');
            }

            const classes = typeof extractTeacherApiList === 'function'
                ? extractTeacherApiList(data, ['classes'])
                : [];

            const uniqueClassesMap = new Map();
            classes.forEach((cls) => {
                const id = cls.id;
                if (id) {
                    const idStr = String(id);
                    if (!uniqueClassesMap.has(idStr)) {
                        uniqueClassesMap.set(idStr, cls);
                    } else {
                        console.warn('发现重复的班级ID:', id, cls.className || cls.name);
                    }
                }
            });

            const uniqueClasses = Array.from(uniqueClassesMap.values());
            const classSelects = [
                document.getElementById('assign-class'),
                document.getElementById('assignments-class-filter')
            ];

            classSelects.forEach((select) => {
                if (!select) {
                    return;
                }

                const currentValue = select.value;
                const firstOption = select.options[0];
                select.innerHTML = '';
                if (firstOption && firstOption.value === '') {
                    select.appendChild(firstOption.cloneNode(true));
                } else {
                    const defaultOption = document.createElement('option');
                    defaultOption.value = '';
                    defaultOption.textContent = '请选择班级';
                    select.appendChild(defaultOption);
                }

                const addedIds = new Set();
                uniqueClasses.forEach((cls) => {
                    const id = cls.id;
                    const name = cls.className || cls.name;
                    if (!id || !name) {
                        return;
                    }

                    const idStr = String(id);
                    if (addedIds.has(idStr)) {
                        return;
                    }

                    const existingOption = Array.from(select.options).find((opt) => opt.value === idStr);
                    if (existingOption) {
                        return;
                    }

                    const option = document.createElement('option');
                    option.value = idStr;
                    option.textContent = `${name}（${cls.year || ''}级）`;
                    select.appendChild(option);
                    addedIds.add(idStr);
                });

                if (currentValue) {
                    select.value = currentValue;
                }
            });
        } catch (error) {
            console.error('加载班级下拉数据失败:', error);
        } finally {
            isLoadingAssignmentClasses = false;
        }
    }

    function openAssignCourseModal() {
        if (typeof bootstrap === 'undefined') {
            reportResourceLoadFailure('Bootstrap 未加载，请检查网络连接或刷新页面');
            return;
        }

        const form = document.getElementById('assignCourseForm');
        if (form) {
            form.reset();
        }
        const classSelect = document.getElementById('assign-class');
        if (classSelect) {
            classSelect.value = '';
        }

        const classFeedback = document.getElementById('assign-class-feedback');
        const courseFeedback = document.getElementById('assign-course-feedback');
        if (classFeedback) {
            classFeedback.innerHTML = '';
        }
        if (courseFeedback) {
            courseFeedback.innerHTML = '';
        }

        const modalElement = document.getElementById('assignCourseModal');
        if (!modalElement) {
            console.error('找不到课程分配模态框元素');
            return;
        }

        const modal = new bootstrap.Modal(modalElement);
        modal.show();
    }

    function openAssignCourseModalForClass(classId) {
        if (typeof bootstrap === 'undefined') {
            reportResourceLoadFailure('Bootstrap 未加载，请检查网络连接或刷新页面');
            return;
        }

        const classSelect = document.getElementById('assign-class');
        if (classSelect) {
            const trySelect = () => {
                const option = Array.from(classSelect.options).find((opt) => opt.value === String(classId));
                if (option) {
                    classSelect.value = String(classId);
                }
            };

            if (classSelect.options.length <= 1) {
                loadClassesForAssignments().then(trySelect).catch(console.error);
            } else {
                trySelect();
            }
        }

        const classFeedback = document.getElementById('assign-class-feedback');
        if (classFeedback) {
            classFeedback.innerHTML = '';
        }

        const modalElement = document.getElementById('assignCourseModal');
        if (!modalElement) {
            console.error('找不到课程分配模态框元素');
            return;
        }

        const modal = new bootstrap.Modal(modalElement);
        modal.show();
    }

    async function submitAssignCourse() {
        const classId = document.getElementById('assign-class').value;
        const courseId = document.getElementById('assign-course').value;
        const classFeedback = document.getElementById('assign-class-feedback');
        const courseFeedback = document.getElementById('assign-course-feedback');

        let hasError = false;
        if (!classId) {
            hasError = true;
            if (classFeedback) {
                classFeedback.className = 'form-control-feedback invalid';
                classFeedback.innerHTML = '<i class="fa fa-exclamation-circle"></i> 请选择要分配课程的班级。';
            }
        } else if (classFeedback) {
            classFeedback.innerHTML = '';
        }

        if (!courseId) {
            hasError = true;
            if (courseFeedback) {
                courseFeedback.className = 'form-control-feedback invalid';
                courseFeedback.innerHTML = '<i class="fa fa-exclamation-circle"></i> 请选择要分配的课程。';
            }
        } else if (courseFeedback) {
            courseFeedback.innerHTML = '';
        }

        if (hasError) {
            return;
        }

        const user = getCurrentUser();
        if (!user || !user.id) {
            showNotification('用户信息获取失败，请重新登录后再试', 'danger');
            return;
        }

        const assignData = {
            classId: parseInt(classId, 10),
            courseId: parseInt(courseId, 10),
            teacherId: user.id
        };

        try {
            const submitBtn = document.querySelector('#assignCourseModal .btn-primary');
            if (submitBtn) {
                showLoading(submitBtn);
            }

            const response = await teacherAPI.assignCourse(assignData);
            if (!response || response.success === false) {
                const message = response && response.message ? response.message : '课程分配失败，请稍后重试';
                throw new Error(message);
            }

            showNotification('课程分配成功', 'success');

            const modal = bootstrap.Modal.getInstance(document.getElementById('assignCourseModal'));
            if (modal) {
                modal.hide();
            }

            await Promise.all([
                loadCourseAssignments(),
                loadClassesForAssignments(),
                performClassSearch()
            ]);
        } catch (error) {
            console.error('课程分配失败:', error);
            handleApiError(error);
        } finally {
            const submitBtn = document.querySelector('#assignCourseModal .btn-primary');
            if (submitBtn) {
                hideLoading(submitBtn);
            }
        }
    }

    async function loadCourseAssignments(page = 1) {
        assignmentCurrentPage = page;
        const loadingEl = document.getElementById('assignments-loading');
        const noResultsEl = document.getElementById('assignments-no-results');
        const errorEl = document.getElementById('assignments-error');
        const errorMsgEl = document.getElementById('assignments-error-message');
        const tableBody = document.getElementById('assignments-table-body');
        const paginationEl = document.getElementById('assignments-pagination');

        if (loadingEl) {
            loadingEl.style.display = 'block';
        }
        if (noResultsEl) {
            noResultsEl.style.display = 'none';
        }
        if (errorEl) {
            errorEl.style.display = 'none';
        }
        if (tableBody) {
            tableBody.innerHTML = '';
        }
        if (paginationEl) {
            paginationEl.style.display = 'none';
        }

        try {
            const params = {
                page,
                size: assignmentPageSize
            };
            if (assignmentFilterParams.courseId) {
                params.courseId = assignmentFilterParams.courseId;
            }
            if (assignmentFilterParams.classId) {
                params.classId = assignmentFilterParams.classId;
            }

            const response = await teacherAPI.getClassAssignments(params);
            if (!response || response.success === false) {
                const message = response && response.message ? response.message : '获取课程分配列表失败';
                throw new Error(message);
            }

            let assignments = typeof extractTeacherApiList === 'function'
                ? extractTeacherApiList(response, ['assignments', 'courseAssignments'])
                : [];
            let totalElements = 0;
            let totalPages = 1;

            if (response.data) {
                if (Array.isArray(response.data)) {
                    totalElements = assignments.length;
                    totalPages = Math.ceil(totalElements / assignmentPageSize);
                } else if (response.data.content && Array.isArray(response.data.content)) {
                    totalElements = response.data.totalElements || assignments.length;
                    totalPages = response.data.totalPages || Math.ceil(totalElements / assignmentPageSize);
                } else {
                    assignments = [];
                }
            }

            if (!assignments || assignments.length === 0) {
                if (noResultsEl) {
                    noResultsEl.style.display = 'block';
                }
                if (paginationEl) {
                    paginationEl.style.display = 'none';
                }
                return;
            }

            if (tableBody) {
                tableBody.innerHTML = assignments.map((item) => {
                    const assignmentId = item.assignmentId || item.id;
                    const courseName = item.courseName || '-';
                    const className = item.className || '-';
                    const teacherName = item.teacherName || '-';
                    const semester = item.semester || '-';
                    const weeklyHours = item.weeklyHours != null ? item.weeklyHours : '-';

                    return `
                        <tr>
                            <td>${courseName}</td>
                            <td>${className}</td>
                            <td>${teacherName}</td>
                            <td>${semester}</td>
                            <td>${weeklyHours}</td>
                            <td>
                                <button class="btn btn-sm btn-danger" onclick="confirmUnassignCourse(${assignmentId}, ${item.classId || 'null'}, ${item.courseId || 'null'})">
                                    <i class="fa fa-trash"></i> 取消分配
                                </button>
                            </td>
                        </tr>
                    `;
                }).join('');
            }

            if (paginationEl && totalPages > 1) {
                updateAssignmentsPagination(page, totalPages, totalElements);
                paginationEl.style.display = 'flex';
            } else if (paginationEl) {
                paginationEl.style.display = 'none';
                const infoEl = document.getElementById('assignments-pagination-info');
                if (infoEl) {
                    infoEl.style.display = 'none';
                }
            }
        } catch (error) {
            console.error('加载课程分配列表失败:', error);
            if (errorEl && errorMsgEl) {
                errorEl.style.display = 'block';
                errorMsgEl.textContent = error.message || '加载课程分配列表失败，请稍后重试';
            }
        } finally {
            if (loadingEl) {
                loadingEl.style.display = 'none';
            }
        }
    }

    function updateAssignmentsPagination(currentPage, totalPages, totalElements) {
        const paginationEl = document.getElementById('assignments-pagination');
        if (!paginationEl) {
            return;
        }

        let html = `
            <li class="page-item ${currentPage === 1 ? 'disabled' : ''}">
                <a class="page-link" href="javascript:void(0)" onclick="loadCourseAssignments(${currentPage - 1})" ${currentPage === 1 ? 'tabindex="-1" aria-disabled="true"' : ''}>
                    <i class="fa fa-chevron-left"></i> 上一页
                </a>
            </li>
        `;

        const maxVisiblePages = 5;
        let startPage = Math.max(1, currentPage - Math.floor(maxVisiblePages / 2));
        let endPage = Math.min(totalPages, startPage + maxVisiblePages - 1);

        if (endPage - startPage < maxVisiblePages - 1) {
            startPage = Math.max(1, endPage - maxVisiblePages + 1);
        }

        if (startPage > 1) {
            html += `<li class="page-item"><a class="page-link" href="javascript:void(0)" onclick="loadCourseAssignments(1)">1</a></li>`;
            if (startPage > 2) {
                html += '<li class="page-item disabled"><span class="page-link">...</span></li>';
            }
        }

        for (let i = startPage; i <= endPage; i += 1) {
            html += `
                <li class="page-item ${i === currentPage ? 'active' : ''}">
                    <a class="page-link" href="javascript:void(0)" onclick="loadCourseAssignments(${i})">${i}</a>
                </li>
            `;
        }

        if (endPage < totalPages) {
            if (endPage < totalPages - 1) {
                html += '<li class="page-item disabled"><span class="page-link">...</span></li>';
            }
            html += `<li class="page-item"><a class="page-link" href="javascript:void(0)" onclick="loadCourseAssignments(${totalPages})">${totalPages}</a></li>`;
        }

        html += `
            <li class="page-item ${currentPage === totalPages ? 'disabled' : ''}">
                <a class="page-link" href="javascript:void(0)" onclick="loadCourseAssignments(${currentPage + 1})" ${currentPage === totalPages ? 'tabindex="-1" aria-disabled="true"' : ''}>
                    下一页 <i class="fa fa-chevron-right"></i>
                </a>
            </li>
        `;

        paginationEl.innerHTML = html;

        const infoEl = document.getElementById('assignments-pagination-info');
        if (infoEl) {
            infoEl.textContent = `共 ${totalElements} 条记录，第 ${currentPage}/${totalPages} 页`;
            infoEl.style.display = totalPages > 1 ? 'block' : 'none';
        }
    }

    async function confirmUnassignCourse(assignmentId, classId, courseId) {
        if (!assignmentId && (!classId || !courseId)) {
            showNotification('缺少必要参数，无法取消分配', 'danger');
            return;
        }

        if (!confirm('确定要取消该课程与班级的分配关系吗？')) {
            return;
        }

        try {
            let result;
            if (assignmentId) {
                result = await teacherAPI.unassignCourse(assignmentId);
            } else {
                result = await fetch(`/api/teacher/class-courses/unassign?classId=${classId}&courseId=${courseId}`, {
                    method: 'DELETE',
                    credentials: 'include',
                    headers: {
                        'Content-Type': 'application/json'
                    }
                }).then((res) => res.json());
            }

            if (!result || result.success === false) {
                const message = result && result.message ? result.message : '取消课程分配失败';
                throw new Error(message);
            }

            showNotification('课程分配已取消', 'success');
            await Promise.all([
                loadCourseAssignments(assignmentCurrentPage),
                performClassSearch()
            ]);
        } catch (error) {
            console.error('取消课程分配失败:', error);
            handleApiError(error);
        }
    }

    function handleAssignmentsSearch() {
        const courseSelect = document.getElementById('assignments-course-filter');
        const classSelect = document.getElementById('assignments-class-filter');

        assignmentFilterParams.courseId = courseSelect && courseSelect.value ? parseInt(courseSelect.value, 10) : null;
        assignmentFilterParams.classId = classSelect && classSelect.value ? parseInt(classSelect.value, 10) : null;
        loadCourseAssignments(1);
    }

    function handleAssignmentsClear() {
        const courseSelect = document.getElementById('assignments-course-filter');
        const classSelect = document.getElementById('assignments-class-filter');
        if (courseSelect) {
            courseSelect.value = '';
        }
        if (classSelect) {
            classSelect.value = '';
        }
        assignmentFilterParams.courseId = null;
        assignmentFilterParams.classId = null;
        loadCourseAssignments(1);
    }

    document.addEventListener('DOMContentLoaded', () => {
        const searchBtn = document.getElementById('assignments-search-btn');
        const clearBtn = document.getElementById('assignments-clear-btn');
        if (searchBtn) {
            searchBtn.addEventListener('click', handleAssignmentsSearch);
        }
        if (clearBtn) {
            clearBtn.addEventListener('click', handleAssignmentsClear);
        }
    });

    global.loadClassesForAssignments = loadClassesForAssignments;
    global.openAssignCourseModal = openAssignCourseModal;
    global.openAssignCourseModalForClass = openAssignCourseModalForClass;
    global.submitAssignCourse = submitAssignCourse;
    global.loadCourseAssignments = loadCourseAssignments;
    global.updateAssignmentsPagination = updateAssignmentsPagination;
    global.confirmUnassignCourse = confirmUnassignCourse;
    global.handleAssignmentsSearch = handleAssignmentsSearch;
    global.handleAssignmentsClear = handleAssignmentsClear;
})(window);
