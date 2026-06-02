(function attachTeacherCoursesCourses(global) {
    let currentCourseSearchParams = {};
    const courseSearchHistory = [];
    const COURSE_CATEGORY_API_ALIASES = {
        required: ['required', '必修'],
        elective: ['elective', '选修'],
        public: ['public', '公共课'],
        major: ['major', '专业课'],
        practice: ['practice', '实践课']
    };
    const COURSE_STATUS_API_ALIASES = {
        inactive: ['inactive', '未开始'],
        active: ['active', '进行中'],
        closed: ['closed', '已结束']
    };

    function buildCourseCategoryApiParams(courseCategory) {
        if (!courseCategory) {
            return {};
        }

        const aliases = COURSE_CATEGORY_API_ALIASES[courseCategory];
        if (!aliases || aliases.length === 0) {
            return { category: courseCategory };
        }

        return { category: aliases[0], categoryAliases: aliases };
    }

    function buildCourseStatusApiParams(courseStatus) {
        if (!courseStatus) {
            return {};
        }

        const aliases = COURSE_STATUS_API_ALIASES[courseStatus];
        if (!aliases || aliases.length === 0) {
            return { courseStatus };
        }

        return { courseStatus: aliases[0], courseStatusAliases: aliases };
    }

    function getCourseSearchParams(page) {
        const courseName = document.getElementById('course-name').value.trim();
        const courseCode = document.getElementById('course-code').value.trim();
        const courseCategory = document.getElementById('course-category').value;
        const courseStatus = document.getElementById('course-status').value;
        const categoryParams = buildCourseCategoryApiParams(courseCategory);
        const statusParams = buildCourseStatusApiParams(courseStatus);

        return {
            page,
            filters: {
                courseName,
                courseCode,
                courseCategory,
                courseStatus
            },
            apiParams: {
                courseName,
                courseCode,
                ...categoryParams,
                ...statusParams
            }
        };
    }

    function resolveCourseCategory(course) {
        const categoryFieldNames = ['courseCategory', 'category', 'course_category', 'courseCategoryValue'];
        const fieldName = categoryFieldNames.find(field => course[field] !== undefined && course[field] !== null);
        return fieldName ? course[fieldName] : '';
    }

    function resolveCourseStatus(course) {
        const statusFieldNames = ['courseStatus', 'status', 'course_status', 'courseStatusValue', 'state', 'statusCode'];
        const fieldName = statusFieldNames.find(field => course[field] !== undefined && course[field] !== null && course[field] !== '');
        return fieldName ? course[fieldName] : '未知';
    }

    function renderCourseStatusBadge(course) {
        const courseStatus = resolveCourseStatus(course);
        const normalizedStatus = (courseStatus || '').toString().replace(/\s+/g, '').trim();
        const displayStatus = normalizedStatus || '未知';

        switch (displayStatus) {
            case '未开始':
            case 'not_started':
            case 'NOT_STARTED':
            case '0':
            case 'pending':
            case 'PENDING':
            case 'inactive':
            case 'INACTIVE':
            case 'wait':
            case 'WAIT':
                return '<span class="badge bg-warning">未开始</span>';
            case '进行中':
            case 'in_progress':
            case 'IN_PROGRESS':
            case '1':
            case 'active':
            case 'ACTIVE':
            case 'ongoing':
            case 'ONGOING':
            case 'running':
            case 'RUNNING':
                return '<span class="badge bg-primary">进行中</span>';
            case '已结束':
            case 'completed':
            case 'COMPLETED':
            case '2':
            case 'finished':
            case 'FINISHED':
            case 'done':
            case 'DONE':
            case 'closed':
            case 'CLOSED':
                return '<span class="badge bg-success">已结束</span>';
            default:
                return '<span class="badge bg-secondary">' + displayStatus + '</span>';
        }
    }

    function renderCourseRow(course) {
        return `
            <tr>
                <td>${course.courseCode}</td>
                <td>${course.courseName}</td>
                <td>${course.courseCategory}</td>
                <td>${course.credit}</td>
                <td>${course.studentCount || 0}</td>
                <td>${renderCourseStatusBadge(course)}</td>
                <td>
                    <div class="action-buttons">
                        <button class="btn btn-primary btn-sm" onclick="editCourse(${course.id})"><i class="fa fa-edit"></i> 编辑</button>
                        <button class="btn btn-danger btn-sm" onclick="deleteCourse(${course.id})"><i class="fa fa-trash"></i> 删除</button>
                    </div>
                </td>
            </tr>
        `;
    }

    function renderCoursePagination(currentPage, totalPages, paginationElement) {
        paginationElement.innerHTML = '';

        const prevLi = document.createElement('li');
        prevLi.className = `page-item ${currentPage === 1 ? 'disabled' : ''}`;
        prevLi.innerHTML = `
            <a class="page-link" href="#" onclick="handleCoursePageClick(${currentPage - 1}); return false;">&laquo; 上一页</a>
        `;
        paginationElement.appendChild(prevLi);

        let startPage = Math.max(1, currentPage - 2);
        let endPage = Math.min(totalPages, currentPage + 2);

        if (endPage - startPage < 4) {
            if (startPage === 1) {
                endPage = Math.min(totalPages, startPage + 4);
            } else if (endPage === totalPages) {
                startPage = Math.max(1, endPage - 4);
            }
        }

        for (let i = startPage; i <= endPage; i++) {
            const li = document.createElement('li');
            li.className = `page-item ${i === currentPage ? 'active' : ''}`;
            li.innerHTML = `
                <a class="page-link" href="#" onclick="handleCoursePageClick(${i}); return false;">&nbsp;${i}&nbsp;</a>
            `;
            paginationElement.appendChild(li);
        }

        const nextLi = document.createElement('li');
        nextLi.className = `page-item ${currentPage === totalPages ? 'disabled' : ''}`;
        nextLi.innerHTML = `
            <a class="page-link" href="#" onclick="handleCoursePageClick(${currentPage + 1}); return false;">下一页 &raquo;</a>
        `;
        paginationElement.appendChild(nextLi);
    }

    async function loadCourses(page = 1, params = {}) {
        const courseTableBody = document.querySelector('#courses-content .table tbody');
        const searchBtn = document.getElementById('course-search-btn');
        const pagination = document.querySelector('#courses-content .pagination');

        try {
            showLoading(searchBtn);
            courseTableBody.innerHTML = '<tr><td colspan="7" class="text-center"><i class="fa fa-spinner fa-spin"></i> 加载中...</td></tr>';

            const apiParams = {
                page,
                size: 10,
                ...params
            };

            const urlWithParams = new URL('/api/teacher/courses', window.location.origin);
            const categoryAliases = Array.isArray(apiParams.categoryAliases) ? apiParams.categoryAliases.filter(Boolean) : [];
            const statusAliases = Array.isArray(apiParams.courseStatusAliases) ? apiParams.courseStatusAliases.filter(Boolean) : [];
            const categoryCandidates = categoryAliases.length > 0
                ? categoryAliases
                : (apiParams.category ? [apiParams.category] : ['']);
            const statusCandidates = statusAliases.length > 0
                ? statusAliases
                : (apiParams.courseStatus ? [apiParams.courseStatus] : ['']);

            const mergedCourses = [];
            const seenCourseIds = new Set();
            let finalResponse = null;

            for (const categoryValue of categoryCandidates) {
                for (const statusValue of statusCandidates) {
                    const requestUrl = new URL(urlWithParams);
                    requestUrl.searchParams.delete('category');
                    requestUrl.searchParams.delete('courseStatus');

                    Object.entries(apiParams).forEach(([key, value]) => {
                        if (key === 'categoryAliases' || key === 'courseStatusAliases') {
                            return;
                        }
                        if (key === 'category' || key === 'courseStatus') {
                            return;
                        }
                        if (value !== undefined && value !== null && value !== '') {
                            requestUrl.searchParams.append(key, value);
                        }
                    });

                    if (categoryValue) {
                        requestUrl.searchParams.append('category', categoryValue);
                    }
                    if (statusValue) {
                        requestUrl.searchParams.append('courseStatus', statusValue);
                    }

                    const fetchResponse = await fetch(requestUrl, {
                        credentials: 'include',
                        headers: {
                            'Content-Type': 'application/json'
                        }
                    });

                    const responseData = await fetchResponse.json();
                    if (!responseData.success) {
                        throw new Error(responseData.message || 'API请求失败');
                    }

                    if (!(responseData && responseData.success === true)) {
                        throw new Error(responseData ? (responseData.message || '获取课程数据失败') : '获取课程数据失败：空响应');
                    }

                    finalResponse = responseData;
                    const pageResult = responseData.data || {};
                    const content = Array.isArray(pageResult.content) ? pageResult.content : [];
                    content.forEach(course => {
                        const courseId = course?.id ?? JSON.stringify(course);
                        if (seenCourseIds.has(courseId)) {
                            return;
                        }
                        seenCourseIds.add(courseId);
                        mergedCourses.push(course);
                    });
                }
            }

            const pageResult = finalResponse?.data || {};
            const courses = mergedCourses;
            const totalPagesValue = Number(pageResult.totalPages) > 0 ? Number(pageResult.totalPages) : 1;
            const currentPageValue = Number(pageResult.pageNumber) > 0
                ? Number(pageResult.pageNumber)
                : (pageResult.pageable && typeof pageResult.pageable.pageNumber === 'number'
                    ? Number(pageResult.pageable.pageNumber) + 1
                    : page);

            currentCourseSearchParams = params;

            if (courses.length === 0) {
                courseTableBody.innerHTML = '<tr><td colspan="7" class="text-center">暂无课程数据</td></tr>';
                pagination.innerHTML = '';
            } else {
                courseTableBody.innerHTML = courses.map(renderCourseRow).join('');
                renderCoursePagination(currentPageValue, totalPagesValue, pagination);
            }
        } catch (error) {
            console.error('Failed to load courses:', error);
            courseTableBody.innerHTML = `<tr><td colspan="7" class="text-center text-danger">加载失败：${error.message || '网络异常，请稍后重试'}</td></tr>`;
            pagination.innerHTML = '';
        } finally {
            hideLoading(searchBtn);
        }
    }

    async function handleCourseSearch() {
        const { apiParams, filters } = getCourseSearchParams(1);
        courseSearchHistory.unshift(filters);
        if (courseSearchHistory.length > 10) {
            courseSearchHistory.pop();
        }
        await loadCourses(1, apiParams);
    }

    function handleCourseClear() {
        document.getElementById('course-name').value = '';
        document.getElementById('course-code').value = '';
        document.getElementById('course-category').value = '';
        document.getElementById('course-status').value = '';
        loadCourses();
    }

    async function handleCoursePageClick(page) {
        await loadCourses(page, currentCourseSearchParams);
    }

    function loadCoursePageData(page) {
        const { apiParams } = getCourseSearchParams(page);
        loadCourses(page, apiParams);
    }

    async function initCourseSearch() {
        await loadCourseCategories();
        await loadCourseStatuses();

        document.getElementById('course-search-btn').addEventListener('click', handleCourseSearch);
        document.getElementById('course-clear-btn').addEventListener('click', handleCourseClear);

        const courseInputs = document.querySelectorAll('#courses-content .search-filter input, #courses-content .search-filter select');
        courseInputs.forEach(input => {
            input.addEventListener('keypress', function(e) {
                if (e.key === 'Enter') {
                    handleCourseSearch();
                }
            });
        });
    }

    global.loadCourses = loadCourses;
    global.handleCourseSearch = handleCourseSearch;
    global.handleCourseClear = handleCourseClear;
    global.handleCoursePageClick = handleCoursePageClick;
    global.handlePageClick = handleCoursePageClick;
    global.renderCoursePagination = renderCoursePagination;
    global.loadCoursePageData = loadCoursePageData;
    global.initCourseSearch = initCourseSearch;
    global.getCourseSearchParams = getCourseSearchParams;
})(window);
