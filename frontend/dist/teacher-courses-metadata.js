(function attachTeacherCoursesMetadata(global) {
    let teacherMajorMetadata = [];

    async function loadCourseOptions() {
        console.log('开始加载课程选项...');
        try {
            const apiUrl = '/api/teacher/courses?size=100';
            console.log('请求URL:', apiUrl);
            const response = await fetch(apiUrl, {
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            console.log('API响应状态:', response.status);

            if (!response.ok) {
                const errorText = await response.text();
                console.error('API响应错误:', errorText);
                throw new Error(`获取课程数据失败: ${response.status} - ${errorText}`);
            }

            const data = await response.json();
            console.log('课程数据API返回:', data);

            if (!data.success) {
                throw new Error(data.message || '获取课程数据失败');
            }

            const courses = typeof extractTeacherApiList === 'function'
                ? extractTeacherApiList(data, ['courses'])
                : [];

            console.log('处理后的课程列表:', courses);
            if (courses.length === 0) {
                console.warn('当前教师没有课程数据');
            }

            populateCourseDropdown(courses);
        } catch (error) {
            console.error('加载课程数据失败:', error);
            showCourseLoadFailureState();
        }
    }

    function populateCourseDropdown(courses) {
        console.log('=== populateCourseDropdown 开始 ===');
        console.log('传入的课程数据:', courses);

        const courseSelects = [
            document.getElementById('add-class-course'),
            document.getElementById('edit-class-course'),
            document.getElementById('assign-course'),
            document.getElementById('assignments-course-filter')
        ];

        console.log('找到的下拉列表元素:', courseSelects.map(select => select ? select.id : 'null'));

        courseSelects.forEach(select => {
            if (!select) {
                console.log('下拉列表元素不存在');
                return;
            }

            console.log(`处理下拉列表: ${select.id}`);
            const currentValue = select.value;

            while (select.children.length > 1) {
                select.removeChild(select.lastChild);
            }

            let addedCount = 0;
            courses.forEach(course => {
                const courseId = course.id || course.courseId || course.course_id;
                const courseName = course.courseName || course.course_name || course.name;
                const courseCode = course.courseCode || course.course_code || '';

                if (!courseId || !courseName) {
                    return;
                }

                const option = document.createElement('option');
                option.value = courseId;
                option.textContent = `${courseName} (${courseCode})`;
                select.appendChild(option);
                addedCount += 1;
            });

            console.log(`下拉列表 ${select.id} 添加了 ${addedCount} 个选项`);
            if (currentValue) {
                select.value = currentValue;
            }
        });
    }

    function showCourseLoadFailureState() {
        console.error('课程API调用失败，请检查：1.是否已登录 2.后端服务是否运行 3.浏览器控制台错误信息');
        const errorContainer = document.getElementById('browser-error-container');
        const errorMessage = document.getElementById('browser-error-message');
        if (errorContainer && errorMessage) {
            errorMessage.textContent = '课程数据加载失败，请刷新页面或重新登录';
            errorContainer.style.display = 'block';
        }
    }

    function normalizeMajorList(rawMajors) {
        return rawMajors
            .map(major => {
                const majorName = major?.majorName || major?.name;
                const majorId = major?.id || major?.majorId || major?.major_id;
                return majorName && majorId ? [majorName, majorId] : null;
            })
            .filter(Boolean)
            .sort((a, b) => a[0].localeCompare(b[0]));
    }

    async function loadTeacherMajors() {
        const response = await fetch('/api/teacher/majors', {
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`获取专业数据失败: ${response.status}`);
        }

        const data = await response.json();
        console.log('专业数据API返回:', data);

        if (!data.success) {
            throw new Error(data.message || '获取专业数据失败');
        }

        const majors = typeof extractTeacherApiList === 'function'
            ? extractTeacherApiList(data, ['majors'])
            : [];

        teacherMajorMetadata = normalizeMajorList(majors);
        return teacherMajorMetadata;
    }

    function buildGradeAndMajorMetadata(classes) {
        const currentYear = new Date().getFullYear();
        const gradeSet = new Set(Array.from({ length: 6 }, (_, index) => (currentYear - 5 + index).toString()));

        const majorMap = new Map();
        teacherMajorMetadata.forEach(([name, id]) => {
            majorMap.set(name, id);
        });

        classes.forEach(cls => {
            const gradeValue = cls.year || cls.grade;
            if (gradeValue) {
                gradeSet.add(String(gradeValue));
            }
            const majorName = cls.majorName || cls.major;
            const majorId = cls.majorId || cls.major_id;
            if (majorName && majorId) {
                majorMap.set(majorName, majorId);
            }
        });

        return {
            grades: Array.from(gradeSet).sort((a, b) => Number(a) - Number(b)),
            majorList: Array.from(majorMap.entries()).sort((a, b) => a[0].localeCompare(b[0]))
        };
    }

    function populateGradeDropdown(grades) {
        [
            document.getElementById('class-grade'),
            document.getElementById('add-class-grade'),
            document.getElementById('edit-class-grade')
        ].forEach(select => {
            if (!select) {
                return;
            }

            const currentValue = select.value;
            while (select.children.length > 1) {
                select.removeChild(select.lastChild);
            }

            grades.forEach(grade => {
                const option = document.createElement('option');
                option.value = grade;
                option.textContent = `${grade}级`;
                select.appendChild(option);
            });

            if (currentValue) {
                select.value = currentValue;
            }
        });
    }

    function populateMajorDropdown(majorList) {
        [
            document.getElementById('class-major'),
            document.getElementById('add-class-major'),
            document.getElementById('edit-class-major')
        ].forEach(select => {
            if (!select) {
                return;
            }

            const currentValue = select.value;
            while (select.children.length > 1) {
                select.removeChild(select.lastChild);
            }

            majorList.forEach(([majorName, majorId]) => {
                const option = document.createElement('option');
                option.value = majorId;
                option.textContent = majorName;
                select.appendChild(option);
            });

            if (currentValue) {
                select.value = currentValue;
            }
        });
    }

    function applyGradeAndMajorMetadata(classes) {
        if (classes.length > 0) {
            console.log('班级数据示例:', classes[0]);
        }

        const { grades, majorList } = buildGradeAndMajorMetadata(classes);
        console.log('生成的年级数据:', grades);
        console.log('提取的专业数据:', majorList);

        populateGradeDropdown(grades);
        populateMajorDropdown(majorList);
        global.teacherCoursesPageState.classMetadataLoaded = true;
    }

    function areClassSearchFiltersEmpty() {
        return !document.getElementById('class-name').value.trim()
            && !document.getElementById('class-grade').value
            && !document.getElementById('class-major').value
            && !document.getElementById('class-teacher').value.trim();
    }

    async function loadGradeAndMajorData(force = false) {
        if (global.teacherCoursesPageState.classMetadataLoaded && !force) {
            return;
        }

        let classes = [];
        let classesLoaded = false;
        let majorsLoaded = false;

        try {
            const majorList = await loadTeacherMajors();
            majorsLoaded = majorList.length > 0;
        } catch (error) {
            teacherMajorMetadata = [];
            console.error('加载专业数据失败:', error);
        }

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
            console.log('班级数据API返回:', data);

            if (!data.success) {
                throw new Error(data.message || '获取班级数据失败');
            }

            const classes = typeof extractTeacherApiList === 'function'
                ? extractTeacherApiList(data, ['classes'])
                : [];
            classesLoaded = true;
            applyGradeAndMajorMetadata(classes);
        } catch (error) {
            console.error('加载班级元数据失败:', error);
            if (majorsLoaded) {
                applyGradeAndMajorMetadata(classes);
                return;
            }

            const { grades } = buildGradeAndMajorMetadata([]);
            populateGradeDropdown(grades);
            populateMajorDropdown([]);
            global.teacherCoursesPageState.classMetadataLoaded = classesLoaded || majorsLoaded;
        }
    }

    async function ensureClassMetadataLoaded(force = false) {
        await loadGradeAndMajorData(force);
    }

    async function ensureClassCourseOptionsLoaded(force = false) {
        if (global.teacherCoursesPageState.classCourseOptionsLoaded && !force) {
            return;
        }

        await loadCourseOptions();
        global.teacherCoursesPageState.classCourseOptionsLoaded = true;
    }

    async function ensureAssignmentCourseOptionsLoaded(force = false) {
        if (global.teacherCoursesPageState.assignmentCourseOptionsLoaded && !force) {
            return;
        }

        await loadCourseOptions();
        global.teacherCoursesPageState.assignmentCourseOptionsLoaded = true;
    }

    function bindModalMetadataPrefetch() {
        const addClassModalElement = document.getElementById('addClassModal');
        if (addClassModalElement && addClassModalElement.dataset.metadataBound !== 'true') {
            addClassModalElement.addEventListener('shown.bs.modal', () => {
                Promise.all([
                    ensureClassMetadataLoaded(),
                    ensureClassCourseOptionsLoaded()
                ]).catch(console.error);
            });
            addClassModalElement.dataset.metadataBound = 'true';
        }

        const editClassModalElement = document.getElementById('editClassModal');
        if (editClassModalElement && editClassModalElement.dataset.metadataBound !== 'true') {
            editClassModalElement.addEventListener('shown.bs.modal', () => {
                Promise.all([
                    ensureClassMetadataLoaded(),
                    ensureClassCourseOptionsLoaded()
                ]).catch(console.error);
            });
            editClassModalElement.dataset.metadataBound = 'true';
        }

        const assignCourseModalElement = document.getElementById('assignCourseModal');
        if (assignCourseModalElement && assignCourseModalElement.dataset.metadataBound !== 'true') {
            assignCourseModalElement.addEventListener('shown.bs.modal', () => {
                ensureAssignmentCourseOptionsLoaded().catch(console.error);
            });
            assignCourseModalElement.dataset.metadataBound = 'true';
        }
    }

    global.loadCourseOptions = loadCourseOptions;
    global.populateCourseDropdown = populateCourseDropdown;
    global.showCourseLoadFailureState = showCourseLoadFailureState;
    global.buildGradeAndMajorMetadata = buildGradeAndMajorMetadata;
    global.applyGradeAndMajorMetadata = applyGradeAndMajorMetadata;
    global.areClassSearchFiltersEmpty = areClassSearchFiltersEmpty;
    global.loadTeacherMajors = loadTeacherMajors;
    global.loadGradeAndMajorData = loadGradeAndMajorData;
    global.ensureClassMetadataLoaded = ensureClassMetadataLoaded;
    global.populateGradeDropdown = populateGradeDropdown;
    global.populateMajorDropdown = populateMajorDropdown;
    global.ensureClassCourseOptionsLoaded = ensureClassCourseOptionsLoaded;
    global.ensureAssignmentCourseOptionsLoaded = ensureAssignmentCourseOptionsLoaded;
    global.bindTeacherCoursesMetadata = bindModalMetadataPrefetch;
})(window);
