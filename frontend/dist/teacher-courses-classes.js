(function attachTeacherCoursesClasses(global) {
    let currentClassPage = 1;
    const classesPerPage = 9;
    let totalClassPages = 1;
    let allClasses = [];

    function getClassSearchHistory() {
        if (!global.searchHistory) {
            global.searchHistory = { classes: [] };
        } else if (!Array.isArray(global.searchHistory.classes)) {
            global.searchHistory.classes = [];
        }
        return global.searchHistory.classes;
    }

    function syncAllClasses() {
        global.allClasses = allClasses;
    }

    function getClassSearchValues() {
        return {
            className: document.getElementById('class-name').value.trim(),
            classGrade: document.getElementById('class-grade').value,
            classMajor: document.getElementById('class-major').value,
            classTeacher: document.getElementById('class-teacher').value.trim()
        };
    }

    function showClassLoadingState() {
        document.getElementById('class-loading').style.display = 'block';
        document.getElementById('class-no-results').style.display = 'none';
        document.getElementById('class-error').style.display = 'none';
        document.getElementById('class-list').innerHTML = '';
    }

    function showClassError(message) {
        const errorContainer = document.getElementById('class-error');
        const errorMessage = document.getElementById('class-error-message');

        errorMessage.textContent = message;
        errorContainer.style.display = 'flex';
        document.getElementById('class-list').innerHTML = '';
        document.getElementById('class-no-results').style.display = 'none';
    }

    function handleClassSearchError(error) {
        document.getElementById('class-loading').style.display = 'none';
        showClassError(`获取班级数据失败: ${error.message}`);
    }

    function getCurrentPageClasses() {
        const startIndex = (currentClassPage - 1) * classesPerPage;
        const endIndex = startIndex + classesPerPage;
        return allClasses.slice(startIndex, endIndex);
    }

    function renderClassList(classes) {
        const classListContainer = document.getElementById('class-list');
        let html = '';

        const uniqueClassesMap = new Map();
        classes.forEach(cls => {
            const classId = cls.id;
            if (!uniqueClassesMap.has(classId)) {
                uniqueClassesMap.set(classId, {
                    id: cls.id,
                    className: cls.className,
                    year: cls.year,
                    capacity: cls.capacity,
                    studentCount: cls.studentCount,
                    teacherId: cls.teacherId,
                    teacherName: cls.teacherName,
                    majorId: cls.majorId,
                    majorName: cls.majorName,
                    courses: cls.courses || [],
                    courseName: cls.courseName || '未分配课程'
                });
                return;
            }

            const existingClass = uniqueClassesMap.get(classId);
            if (cls.courses && Array.isArray(cls.courses)) {
                const existingCourseIds = new Set((existingClass.courses || []).map(course => course.courseId));
                cls.courses.forEach(course => {
                    if (!existingCourseIds.has(course.courseId)) {
                        existingClass.courses.push(course);
                        existingCourseIds.add(course.courseId);
                    }
                });
                const courseNames = existingClass.courses.map(course => course.courseName || '').filter(Boolean);
                existingClass.courseName = courseNames.length > 0 ? courseNames.join('、') : '未分配课程';
            }
        });

        uniqueClassesMap.forEach(cls => {
            const courseCount = cls.courses ? cls.courses.length : 0;
            const courseDisplay = courseCount > 1
                ? `${cls.courseName} <span style="color: #667eea; font-weight: bold;">(${courseCount}门课程)</span>`
                : cls.courseName;
            const majorDisplay = cls.majorName && cls.majorName !== 'null' ? cls.majorName : '未设置';
            const teacherDisplay = cls.teacherName && cls.teacherName !== 'null' ? cls.teacherName : '未设置';
            const escapedClassName = String(cls.className || '').replace(/'/g, "\\'");

            html += `
                <div class="card">
                    <div class="card-header">
                        <h3 class="card-title">${cls.className}</h3>
                        <span class="badge" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 4px 12px; border-radius: 20px; font-size: 12px;">${cls.year}级</span>
                    </div>
                    <div class="card-meta">
                        <p><i class="fa fa-book" style="margin-right: 8px; color: #667eea;"></i>课程：${courseDisplay}</p>
                        <p><i class="fa fa-graduation-cap" style="margin-right: 8px; color: #667eea;"></i>专业：${majorDisplay}</p>
                        <p><i class="fa fa-user" style="margin-right: 8px; color: #667eea;"></i>班主任：${teacherDisplay}</p>
                    </div>
                    <div class="card-stats">
                        <div class="stat-item">
                            <div class="stat-number">${cls.studentCount || 0}</div>
                            <div class="stat-label">学生人数</div>
                        </div>
                        <div class="stat-item">
                            <div class="stat-number">${cls.capacity || 0}</div>
                            <div class="stat-label">学生容量</div>
                        </div>
                    </div>
                    <div style="margin-top: 16px; display: flex; gap: 8px;">
                        <button class="btn btn-sm btn-primary" onclick="viewClassStudents(${cls.id})">
                            <i class="fa fa-users"></i> 查看学生
                        </button>
                        <button class="btn btn-sm btn-secondary" onclick="editClass(${cls.id})">
                            <i class="fa fa-edit"></i> 编辑
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="deleteClass(${cls.id})">
                            <i class="fa fa-trash"></i> 删除
                        </button>
                        <button class="btn btn-sm btn-secondary" onclick="openAssignCourseModalForClass(${cls.id}, '${escapedClassName}')">
                            <i class="fa fa-link"></i> 分配课程
                        </button>
                    </div>
                </div>
            `;
        });

        classListContainer.innerHTML = html;
    }

    function updateClassPaginationUI() {
        const pagination = document.getElementById('class-pagination');
        if (totalClassPages <= 1) {
            pagination.style.display = 'none';
            return;
        }

        pagination.style.display = 'flex';

        const prevItem = pagination.querySelector('.page-item:nth-child(1)');
        const nextItem = pagination.querySelector('.page-item:last-child');

        while (pagination.children.length > 2) {
            pagination.removeChild(pagination.children[1]);
        }

        pagination.insertBefore(prevItem, pagination.firstChild);
        pagination.appendChild(nextItem);

        for (let i = 1; i <= totalClassPages; i += 1) {
            const li = document.createElement('li');
            li.className = 'page-item';
            li.innerHTML = `<a class="page-link" data-page="${i}" href="javascript:void(0);">${i}</a>`;
            pagination.insertBefore(li, nextItem);
        }

        pagination.querySelectorAll('.page-link').forEach(link => {
            link.classList.remove('active');
        });

        const currentPageLink = pagination.querySelector(`.page-link[data-page="${currentClassPage}"]`);
        if (currentPageLink) {
            currentPageLink.classList.add('active');
        }

        const prevLink = pagination.querySelector('.page-link[data-page="prev"]');
        const nextLink = pagination.querySelector('.page-link[data-page="next"]');
        if (prevLink && prevLink.parentElement) {
            prevLink.parentElement.classList.toggle('disabled', currentClassPage === 1);
        }
        if (nextLink && nextLink.parentElement) {
            nextLink.parentElement.classList.toggle('disabled', currentClassPage >= totalClassPages);
        }
    }

    async function performClassSearch() {
        const { className, classGrade, classMajor, classTeacher } = getClassSearchValues();
        showClassLoadingState();

        const params = new URLSearchParams();
        if (className) {
            params.append('className', className);
        }
        if (classGrade) {
            params.append('grade', classGrade);
        }
        if (classMajor) {
            params.append('majorId', classMajor);
        }
        if (classTeacher) {
            params.append('teacherName', classTeacher);
        }

        try {
            const response = await fetch(`/api/teacher/classes?${params.toString()}`, {
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();
            document.getElementById('class-loading').style.display = 'none';

            if (!data.success) {
                throw new Error(data.message || '获取班级数据失败');
            }

            allClasses = typeof extractTeacherApiList === 'function'
                ? extractTeacherApiList(data, ['classes'])
                : [];
            syncAllClasses();

            if (!global.teacherCoursesPageState.classMetadataLoaded && typeof global.areClassSearchFiltersEmpty === 'function' && global.areClassSearchFiltersEmpty()) {
                global.applyGradeAndMajorMetadata(allClasses);
            }

            totalClassPages = Math.ceil(allClasses.length / classesPerPage);
            currentClassPage = 1;

            if (allClasses.length > 0) {
                renderClassList(getCurrentPageClasses());
                updateClassPaginationUI();
                return;
            }

            document.getElementById('class-no-results').style.display = 'block';
            document.getElementById('class-pagination').style.display = 'none';
        } catch (error) {
            handleClassSearchError(error);
        }
    }

    function recordClassSearchHistory(searchParams) {
        const history = getClassSearchHistory();
        history.unshift(searchParams);
        if (history.length > 10) {
            history.pop();
        }
    }

    async function handleClassSearch() {
        recordClassSearchHistory(getClassSearchValues());
        await performClassSearch();
    }

    function resetClassSearchInputs() {
        document.getElementById('class-name').value = '';
        document.getElementById('class-grade').value = '';
        document.getElementById('class-major').value = '';
        document.getElementById('class-teacher').value = '';
    }

    function handleClassClear() {
        resetClassSearchInputs();
        performClassSearch();
    }

    function handleClassPaginationClick(pageType) {
        let newPage = currentClassPage;

        if (pageType === 'prev') {
            newPage = Math.max(1, currentClassPage - 1);
        } else if (pageType === 'next') {
            newPage = currentClassPage + 1;
        } else {
            newPage = parseInt(pageType, 10);
        }

        if (Number.isNaN(newPage) || newPage < 1 || newPage > totalClassPages) {
            return;
        }

        currentClassPage = newPage;
        updateClassPaginationUI();
        renderClassList(getCurrentPageClasses());
    }

    function initClassPagination() {
        const classPagination = document.getElementById('class-pagination');
        if (!classPagination || classPagination.dataset.bound === 'true') {
            return;
        }

        classPagination.addEventListener('click', event => {
            event.preventDefault();
            const target = event.target.closest('.page-link');
            if (!target) {
                return;
            }
            handleClassPaginationClick(target.getAttribute('data-page'));
        });
        classPagination.dataset.bound = 'true';
    }

    function initClassSearch() {
        const searchBtn = document.getElementById('class-search-btn');
        const clearBtn = document.getElementById('class-clear-btn');
        const classNameInput = document.getElementById('class-name');
        const classGradeSelect = document.getElementById('class-grade');
        const classMajorSelect = document.getElementById('class-major');
        const classTeacherInput = document.getElementById('class-teacher');

        if (!searchBtn || searchBtn.dataset.bound === 'true') {
            initClassPagination();
            return;
        }

        let searchTimer = null;

        searchBtn.addEventListener('click', () => {
            handleClassSearch();
        });

        clearBtn.addEventListener('click', () => {
            handleClassClear();
        });

        [classNameInput, classTeacherInput].forEach(input => {
            input.addEventListener('keypress', event => {
                if (event.key === 'Enter') {
                    handleClassSearch();
                }
            });

            input.addEventListener('input', () => {
                clearTimeout(searchTimer);
                searchTimer = setTimeout(() => {
                    handleClassSearch();
                }, 500);
            });
        });

        [classGradeSelect, classMajorSelect].forEach(select => {
            select.addEventListener('change', () => {
                handleClassSearch();
            });
        });

        searchBtn.dataset.bound = 'true';
        initClassPagination();
    }

    function getAllClasses() {
        return allClasses;
    }

    global.initClassSearch = initClassSearch;
    global.initPagination = initClassPagination;
    global.handleClassSearch = handleClassSearch;
    global.handleClassClear = handleClassClear;
    global.performClassSearch = performClassSearch;
    global.renderClassList = renderClassList;
    global.handleClassPaginationClick = handleClassPaginationClick;
    global.getTeacherCoursesAllClasses = getAllClasses;
    syncAllClasses();
})(window);
