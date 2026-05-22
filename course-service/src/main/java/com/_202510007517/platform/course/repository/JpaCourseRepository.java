package com._202510007517.platform.course.repository;

import com._202510007517.platform.course.domain.CourseRecord;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
public class JpaCourseRepository implements CourseRepository {

    private final CourseJpaRepository courseJpaRepository;
    private final CourseClassJpaRepository courseClassJpaRepository;
    private final ClassCourseJpaRepository classCourseJpaRepository;
    private final ClassStudentJpaRepository classStudentJpaRepository;
    private final MajorJpaRepository majorJpaRepository;

    public JpaCourseRepository(CourseJpaRepository courseJpaRepository,
                               CourseClassJpaRepository courseClassJpaRepository,
                               ClassCourseJpaRepository classCourseJpaRepository,
                               ClassStudentJpaRepository classStudentJpaRepository,
                               MajorJpaRepository majorJpaRepository) {
        this.courseJpaRepository = courseJpaRepository;
        this.courseClassJpaRepository = courseClassJpaRepository;
        this.classCourseJpaRepository = classCourseJpaRepository;
        this.classStudentJpaRepository = classStudentJpaRepository;
        this.majorJpaRepository = majorJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseRecord> findByTeacherIdWithSearch(Long teacherId, String name, String courseCode, String category, String status) {
        return courseJpaRepository.findByTeacherIdOrderByIdDesc(teacherId).stream()
                .filter(course -> contains(course.getCourseName(), name))
                .filter(course -> equalsIgnoreBlank(course.getCourseCode(), courseCode))
                .filter(course -> equalsIgnoreBlank(course.getCourseCategory(), category))
                .filter(course -> equalsIgnoreBlank(course.getCourseStatus(), status))
                .map(this::toRecord)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseRecord> findByStudentIdWithSearch(Long studentId, String searchQuery, String category, String status) {
        LinkedHashSet<Long> courseIds = new LinkedHashSet<>();
        for (ClassStudentEntity classStudent : classStudentJpaRepository.findByStudentIdOrderByClassId(studentId)) {
            courseIds.addAll(resolveCourseIdsForClass(classStudent.getClassId()));
        }
        return courseJpaRepository.findByIdIn(courseIds).stream()
                .filter(course -> contains(course.getCourseName(), searchQuery))
                .filter(course -> equalsIgnoreBlank(course.getCourseCategory(), category))
                .filter(course -> equalsIgnoreBlank(course.getCourseStatus(), status))
                .sorted((left, right) -> Long.compare(right.getId(), left.getId()))
                .map(this::toRecord)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseRecord> findAllCourses() {
        return courseJpaRepository.findAll().stream()
                .sorted((left, right) -> Long.compare(right.getId(), left.getId()))
                .map(this::toRecord)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseRecord> findById(Long id) {
        return courseJpaRepository.findById(id).map(this::toRecord);
    }

    @Override
    @Transactional
    public CourseRecord insert(CourseRecord course) {
        CourseEntity entity = toEntity(course);
        CourseEntity saved = courseJpaRepository.save(entity);
        course.setId(saved.getId());
        return course;
    }

    @Override
    @Transactional
    public void update(CourseRecord course) {
        courseJpaRepository.findById(course.getId()).ifPresent(entity -> {
            apply(entity, course);
            courseJpaRepository.save(entity);
        });
    }

    @Override
    @Transactional
    public void delete(Long id) {
        for (ClassCourseEntity assignment : new ArrayList<>(classCourseJpaRepository.findByCourseId(id))) {
            classCourseJpaRepository.delete(assignment);
        }
        for (CourseClassEntity courseClass : courseClassJpaRepository.findAll()) {
            if (Objects.equals(courseClass.getCourseId(), id)) {
                courseClass.setCourseId(null);
                courseClassJpaRepository.save(courseClass);
            }
        }
        courseJpaRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> countStudentsByCourseIds(List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, LinkedHashSet<Long>> studentsByCourse = new LinkedHashMap<>();
        for (Long courseId : courseIds) {
            studentsByCourse.put(courseId, new LinkedHashSet<>());
        }
        for (CourseClassEntity courseClass : courseClassJpaRepository.findAll()) {
            Collection<Long> candidateCourseIds = resolveCourseIdsForClass(courseClass.getId());
            List<ClassStudentEntity> students = classStudentJpaRepository.findByClassIdOrderByStudentId(courseClass.getId());
            for (Long courseId : candidateCourseIds) {
                LinkedHashSet<Long> ids = studentsByCourse.get(courseId);
                if (ids != null) {
                    for (ClassStudentEntity student : students) {
                        ids.add(student.getStudentId());
                    }
                }
            }
        }
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (Long courseId : courseIds) {
            result.put(courseId, studentsByCourse.getOrDefault(courseId, new LinkedHashSet<>()).size());
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> findStudentsByCourseId(Long courseId) {
        LinkedHashSet<Long> classIds = new LinkedHashSet<>();
        for (CourseClassEntity courseClass : courseClassJpaRepository.findAll()) {
            if (resolveCourseIdsForClass(courseClass.getId()).contains(courseId)) {
                classIds.add(courseClass.getId());
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Long classId : classIds) {
            CourseClassEntity courseClass = courseClassJpaRepository.findById(classId).orElse(null);
            if (courseClass == null) {
                continue;
            }
            for (ClassStudentEntity student : classStudentJpaRepository.findByClassIdOrderByStudentId(classId)) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", student.getStudentId());
                row.put("className", courseClass.getClassName());
                rows.add(row);
            }
        }
        rows.sort((left, right) -> Long.compare(toLong(left.get("id")), toLong(right.get("id"))));
        return deduplicateRows(rows, "id");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findClassIdsByStudentId(Long studentId) {
        return classStudentJpaRepository.findByStudentIdOrderByClassId(studentId).stream()
                .map(ClassStudentEntity::getClassId)
                .distinct()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findStudentIdsByTeacherId(Long teacherId) {
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        for (CourseClassEntity courseClass : courseClassJpaRepository.findAll()) {
            if (teacherCanAccessClass(teacherId, courseClass.getId())) {
                for (ClassStudentEntity student : classStudentJpaRepository.findByClassIdOrderByStudentId(courseClass.getId())) {
                    result.add(student.getStudentId());
                }
            }
        }
        return new ArrayList<>(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> findClassesByTeacherId(Long teacherId, String className, String grade, String majorName, Long majorId, Long courseId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<Long, Integer> studentCounts = countStudentsByClassId();
        Map<Long, String> majorNames = loadMajorNames();
        for (CourseClassEntity courseClass : courseClassJpaRepository.findAll()) {
            if (!teacherCanAccessClass(teacherId, courseClass.getId())) {
                continue;
            }
            if (!contains(courseClass.getClassName(), className)) {
                continue;
            }
            if (!equalsIgnoreBlank(courseClass.getYear(), grade)) {
                continue;
            }
            if (majorId != null && !Objects.equals(courseClass.getMajorId(), majorId)) {
                continue;
            }
            String majorLabel = majorNames.get(courseClass.getMajorId());
            if (!contains(majorLabel, majorName)) {
                continue;
            }
            List<ClassCourseEntity> assignments = classCourseJpaRepository.findByClassId(courseClass.getId());
            if (assignments.isEmpty()) {
                Long directCourseId = courseClass.getCourseId();
                if (courseId == null || Objects.equals(directCourseId, courseId)) {
                    rows.add(classRow(courseClass, studentCounts.getOrDefault(courseClass.getId(), 0), majorLabel, directCourseId, courseClass.getClassTime(), courseClass.getClassLocation()));
                }
                continue;
            }
            for (ClassCourseEntity assignment : assignments) {
                if (courseId != null && !Objects.equals(assignment.getCourseId(), courseId)) {
                    continue;
                }
                rows.add(classRow(courseClass, studentCounts.getOrDefault(courseClass.getId(), 0), majorLabel, assignment.getCourseId(), assignment.getClassTime(), assignment.getClassLocation()));
            }
        }
        rows.sort((left, right) -> Long.compare(toLong(right.get("id")), toLong(left.get("id"))));
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> findClassById(Long classId) {
        return courseClassJpaRepository.findById(classId)
                .map(courseClass -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", courseClass.getId());
                    row.put("className", courseClass.getClassName());
                    row.put("year", courseClass.getYear());
                    row.put("capacity", courseClass.getCapacity());
                    row.put("courseId", courseClass.getCourseId());
                    row.put("teacherId", courseClass.getTeacherId());
                    row.put("majorId", courseClass.getMajorId());
                    row.put("majorName", loadMajorNames().get(courseClass.getMajorId()));
                    row.put("classTime", courseClass.getClassTime());
                    row.put("classLocation", courseClass.getClassLocation());
                    row.put("studentCount", classStudentJpaRepository.findByClassIdOrderByStudentId(classId).size());
                    return row;
                })
                .orElse(Map.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> findStudentsByClassId(Long classId) {
        return classStudentJpaRepository.findByClassIdOrderByStudentId(classId).stream()
                .map(student -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", student.getStudentId());
                    return row;
                })
                .toList();
    }

    @Override
    @Transactional
    public Long insertClass(Map<String, Object> classData) {
        CourseClassEntity entity = new CourseClassEntity();
        apply(entity, classData);
        return courseClassJpaRepository.save(entity).getId();
    }

    @Override
    @Transactional
    public void updateClass(Map<String, Object> classData) {
        Long id = toLong(classData.get("id"));
        courseClassJpaRepository.findById(id).ifPresent(entity -> {
            apply(entity, classData);
            courseClassJpaRepository.save(entity);
        });
    }

    @Override
    @Transactional
    public void deleteClass(Long classId) {
        for (ClassStudentEntity student : new ArrayList<>(classStudentJpaRepository.findByClassIdOrderByStudentId(classId))) {
            classStudentJpaRepository.delete(student);
        }
        for (ClassCourseEntity assignment : new ArrayList<>(classCourseJpaRepository.findByClassId(classId))) {
            classCourseJpaRepository.delete(assignment);
        }
        courseClassJpaRepository.deleteById(classId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> findClassAssignments(Long teacherId, Long courseId, Long classId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ClassCourseEntity assignment : classCourseJpaRepository.findAll()) {
            CourseClassEntity courseClass = courseClassJpaRepository.findById(assignment.getClassId()).orElse(null);
            CourseEntity course = courseJpaRepository.findById(assignment.getCourseId()).orElse(null);
            if (courseClass == null || course == null) {
                continue;
            }
            if (!teacherCanAccessClass(teacherId, courseClass.getId())) {
                continue;
            }
            if (courseId != null && !Objects.equals(course.getId(), courseId)) {
                continue;
            }
            if (classId != null && !Objects.equals(courseClass.getId(), classId)) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("assignmentId", assignment.getId());
            row.put("classId", courseClass.getId());
            row.put("courseId", course.getId());
            row.put("courseName", course.getCourseName());
            row.put("className", courseClass.getClassName());
            row.put("teacherName", null);
            row.put("semester", course.getSemester());
            row.put("weeklyHours", assignment.getClassTime());
            row.put("classLocation", assignment.getClassLocation());
            rows.add(row);
        }
        rows.sort((left, right) -> Long.compare(toLong(right.get("assignmentId")), toLong(left.get("assignmentId"))));
        return rows;
    }

    @Override
    @Transactional
    public Long insertClassCourse(Map<String, Object> assignData) {
        ClassCourseEntity entity = new ClassCourseEntity();
        entity.setClassId(toLong(assignData.get("classId")));
        entity.setCourseId(toLong(assignData.get("courseId")));
        entity.setTeacherId(toLong(assignData.get("teacherId")));
        entity.setClassTime(toStringValue(assignData.get("classTime")));
        entity.setClassLocation(toStringValue(assignData.get("classLocation")));
        return classCourseJpaRepository.save(entity).getId();
    }

    @Override
    @Transactional
    public void deleteClassCourse(Long assignmentId) {
        classCourseJpaRepository.deleteById(assignmentId);
    }

    @Override
    @Transactional
    public void deleteClassCourse(Long classId, Long courseId) {
        classCourseJpaRepository.deleteByClassIdAndCourseId(classId, courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean teacherOwnsCourse(Long teacherId, Long courseId) {
        return courseJpaRepository.findById(courseId)
                .map(course -> Objects.equals(course.getTeacherId(), teacherId))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean teacherCanAccessClass(Long teacherId, Long classId) {
        CourseClassEntity courseClass = courseClassJpaRepository.findById(classId).orElse(null);
        if (courseClass == null) {
            return false;
        }
        if (Objects.equals(courseClass.getTeacherId(), teacherId)) {
            return true;
        }
        if (courseClass.getCourseId() != null && teacherOwnsCourse(teacherId, courseClass.getCourseId())) {
            return true;
        }
        for (ClassCourseEntity assignment : classCourseJpaRepository.findByClassId(classId)) {
            if (Objects.equals(assignment.getTeacherId(), teacherId)) {
                return true;
            }
            if (teacherOwnsCourse(teacherId, assignment.getCourseId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean classCourseExists(Long classId, Long courseId) {
        return classCourseJpaRepository.findByClassId(classId).stream()
                .anyMatch(assignment -> Objects.equals(assignment.getCourseId(), courseId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean classNameExists(String className, Long excludedClassId) {
        return courseClassJpaRepository.findAll().stream()
                .filter(courseClass -> Objects.equals(courseClass.getClassName(), className))
                .anyMatch(courseClass -> !Objects.equals(courseClass.getId(), excludedClassId));
    }

    @Override
    @Transactional
    public void replaceStudentClass(Long studentId, Long classId) {
        classStudentJpaRepository.deleteByStudentId(studentId);
        ClassStudentEntity entity = new ClassStudentEntity();
        entity.setClassId(classId);
        entity.setStudentId(studentId);
        classStudentJpaRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> findMajors() {
        return majorJpaRepository.findAll().stream()
                .sorted((left, right) -> Long.compare(left.getId(), right.getId()))
                .map(major -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", major.getId());
                    row.put("majorName", major.getMajorName());
                    return row;
                })
                .toList();
    }

    private Map<Long, Integer> countStudentsByClassId() {
        Map<Long, Integer> counts = new HashMap<>();
        for (CourseClassEntity courseClass : courseClassJpaRepository.findAll()) {
            counts.put(courseClass.getId(), classStudentJpaRepository.findByClassIdOrderByStudentId(courseClass.getId()).size());
        }
        return counts;
    }

    private Map<Long, String> loadMajorNames() {
        Map<Long, String> majors = new HashMap<>();
        for (MajorEntity major : majorJpaRepository.findAll()) {
            majors.put(major.getId(), major.getMajorName());
        }
        return majors;
    }

    private List<Long> resolveCourseIdsForClass(Long classId) {
        LinkedHashSet<Long> courseIds = new LinkedHashSet<>();
        CourseClassEntity courseClass = courseClassJpaRepository.findById(classId).orElse(null);
        if (courseClass != null && courseClass.getCourseId() != null) {
            courseIds.add(courseClass.getCourseId());
        }
        for (ClassCourseEntity assignment : classCourseJpaRepository.findByClassId(classId)) {
            courseIds.add(assignment.getCourseId());
        }
        return new ArrayList<>(courseIds);
    }

    private Map<String, Object> classRow(CourseClassEntity courseClass,
                                         Integer studentCount,
                                         String majorName,
                                         Long courseId,
                                         String classTime,
                                         String classLocation) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", courseClass.getId());
        row.put("className", courseClass.getClassName());
        row.put("year", courseClass.getYear());
        row.put("capacity", courseClass.getCapacity());
        row.put("studentCount", studentCount);
        row.put("teacherId", courseClass.getTeacherId());
        row.put("teacherName", null);
        row.put("majorId", courseClass.getMajorId());
        row.put("majorName", majorName);
        row.put("courseId", courseId);
        row.put("courseName", courseId != null ? courseJpaRepository.findById(courseId).map(CourseEntity::getCourseName).orElse(null) : null);
        row.put("classTime", classTime);
        row.put("classLocation", classLocation);
        return row;
    }

    private List<Map<String, Object>> deduplicateRows(List<Map<String, Object>> rows, String key) {
        LinkedHashMap<Object, Map<String, Object>> deduplicated = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            deduplicated.putIfAbsent(row.get(key), row);
        }
        return new ArrayList<>(deduplicated.values());
    }

    private boolean contains(String candidate, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return candidate != null && candidate.contains(query);
    }

    private boolean equalsIgnoreBlank(String candidate, String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        return Objects.equals(candidate, filter);
    }

    private CourseRecord toRecord(CourseEntity entity) {
        CourseRecord course = new CourseRecord();
        course.setId(entity.getId());
        course.setCourseName(entity.getCourseName());
        course.setCourseCode(entity.getCourseCode());
        course.setDescription(entity.getDescription());
        course.setCredit(entity.getCredit());
        course.setCourseCategory(entity.getCourseCategory());
        course.setTotalHours(entity.getTotalHours());
        course.setTeacherId(entity.getTeacherId());
        course.setCourseDirector(entity.getCourseDirector());
        course.setAssessmentMethod(entity.getAssessmentMethod());
        course.setCourseStatus(entity.getCourseStatus());
        course.setSemester(entity.getSemester());
        course.setStartDate(entity.getStartDate());
        course.setEndDate(entity.getEndDate());
        course.setMaxStudents(entity.getMaxStudents());
        return course;
    }

    private CourseEntity toEntity(CourseRecord course) {
        CourseEntity entity = new CourseEntity();
        apply(entity, course);
        return entity;
    }

    private void apply(CourseEntity entity, CourseRecord course) {
        entity.setId(course.getId());
        entity.setCourseName(course.getCourseName());
        entity.setCourseCode(course.getCourseCode());
        entity.setDescription(course.getDescription());
        entity.setCredit(course.getCredit());
        entity.setCourseCategory(course.getCourseCategory());
        entity.setTotalHours(course.getTotalHours());
        entity.setTeacherId(course.getTeacherId());
        entity.setCourseDirector(course.getCourseDirector());
        entity.setAssessmentMethod(course.getAssessmentMethod());
        entity.setCourseStatus(course.getCourseStatus());
        entity.setSemester(course.getSemester());
        entity.setStartDate(course.getStartDate());
        entity.setEndDate(course.getEndDate());
        entity.setMaxStudents(course.getMaxStudents());
    }

    private void apply(CourseClassEntity entity, Map<String, Object> classData) {
        entity.setClassName(toStringValue(classData.get("className")));
        entity.setYear(toStringValue(classData.get("year")));
        entity.setCapacity(toInteger(classData.get("capacity")));
        entity.setCourseId(toNullableLong(classData.get("courseId")));
        entity.setTeacherId(toNullableLong(classData.get("teacherId")));
        entity.setMajorId(toNullableLong(classData.get("majorId")));
        entity.setClassTime(toStringValue(classData.get("classTime")));
        entity.setClassLocation(toStringValue(classData.get("classLocation")));
    }

    private static Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    private static Long toNullableLong(Object value) {
        if (value == null) {
            return null;
        }
        return toLong(value);
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(value.toString());
    }

    private static String toStringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
