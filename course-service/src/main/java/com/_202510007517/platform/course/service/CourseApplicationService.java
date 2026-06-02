package com._202510007517.platform.course.service;

import com._202510007517.platform.common.exception.ResourceNotFoundException;
import com._202510007517.platform.common.exception.ForbiddenException;
import com._202510007517.platform.common.exception.RemoteClientException;
import com._202510007517.platform.course.api.dto.ClassCourseDTO;
import com._202510007517.platform.course.api.dto.ClassUpsertRequestDTO;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.dto.CourseAssignmentDTO;
import com._202510007517.platform.course.api.dto.CourseAssignmentRequestDTO;
import com._202510007517.platform.course.api.dto.CourseUpsertRequestDTO;
import com._202510007517.platform.course.api.dto.MajorDTO;
import com._202510007517.platform.course.api.dto.TeacherClassDTO;
import com._202510007517.platform.course.config.CourseCacheNames;
import com._202510007517.platform.course.domain.CourseRecord;
import com._202510007517.platform.course.repository.CourseRepository;
import com._202510007517.platform.user.api.dto.StudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateStudentProfileDTO;
import com._202510007517.platform.user.api.dto.UserProfileDTO;
import com._202510007517.platform.user.api.feign.UserFeignClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CourseApplicationService {

    private final CourseRepository courseRepository;
    private final UserFeignClient userFeignClient;

    public CourseApplicationService(CourseRepository courseRepository, UserFeignClient userFeignClient) {
        this.courseRepository = courseRepository;
        this.userFeignClient = userFeignClient;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CourseCacheNames.COURSE_LIST,
            key = "#teacherId + ':' + (#name == null ? '' : #name) + ':' + (#courseCode == null ? '' : #courseCode) + ':' + (#category == null ? '' : #category) + ':' + (#status == null ? '' : #status)")
    public List<CourseDTO> listTeacherCourses(Long teacherId, String name, String courseCode, String category, String status) {
        List<CourseRecord> courses = courseRepository.findByTeacherIdWithSearch(teacherId, name, courseCode, category, status);
        Map<Long, Integer> studentCounts = courseRepository.countStudentsByCourseIds(courses.stream().map(CourseRecord::getId).toList());
        return courses.stream()
                .map(course -> toDto(course, studentCounts.getOrDefault(course.getId(), 0), null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseDTO> listStudentCourses(Long studentId, String searchQuery, String category, String status) {
        List<CourseRecord> courses = courseRepository.findByStudentIdWithSearch(studentId, searchQuery, category, status);
        Map<Long, Integer> studentCounts = courseRepository.countStudentsByCourseIds(courses.stream().map(CourseRecord::getId).toList());
        Map<Long, String> teacherNames = loadTeacherNames(courses.stream().map(CourseRecord::getTeacherId).filter(Objects::nonNull).distinct().toList());
        return courses.stream()
                .map(course -> toDto(course, studentCounts.getOrDefault(course.getId(), 0), teacherNames.get(course.getTeacherId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> listSemesters() {
        return courseRepository.findAllCourses().stream()
                .map(CourseRecord::getSemester)
                .filter(semester -> semester != null && !semester.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CourseCacheNames.COURSE_DETAIL, key = "#courseId")
    public CourseDTO getCourse(Long courseId) {
        CourseRecord course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("课程不存在"));
        Integer studentCount = courseRepository.countStudentsByCourseIds(List.of(courseId)).getOrDefault(courseId, 0);
        return toDto(course, studentCount, null);
    }

    @Transactional(readOnly = true)
    public CourseDTO getStudentCourse(Long studentId, Long courseId) {
        List<CourseDTO> courses = listStudentCourses(studentId, null, null, null);
        return courses.stream()
                .filter(course -> Objects.equals(course.getId(), courseId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("无权访问该课程"));
    }

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = CourseCacheNames.COURSE_LIST, allEntries = true),
            @CacheEvict(cacheNames = CourseCacheNames.COURSE_DETAIL, allEntries = true)
    })
    public CourseDTO createCourse(Long teacherId, CourseUpsertRequestDTO request) {
        validateCourseDates(request);
        CourseRecord course = toRecord(request);
        course.setTeacherId(teacherId);
        return toDto(courseRepository.insert(course), 0, null);
    }

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = CourseCacheNames.COURSE_LIST, allEntries = true),
            @CacheEvict(cacheNames = CourseCacheNames.COURSE_DETAIL, allEntries = true)
    })
    public CourseDTO updateCourse(Long courseId, Long teacherId, CourseUpsertRequestDTO request) {
        validateCourseDates(request);
        CourseRecord course = toRecord(request);
        course.setId(courseId);
        course.setTeacherId(teacherId);
        courseRepository.update(course);
        return toDto(course, courseRepository.countStudentsByCourseIds(List.of(courseId)).getOrDefault(courseId, 0), null);
    }

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = CourseCacheNames.COURSE_LIST, allEntries = true),
            @CacheEvict(cacheNames = CourseCacheNames.COURSE_DETAIL, allEntries = true)
    })
    public void deleteCourse(Long courseId) {
        courseRepository.delete(courseId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listCourseStudents(Long courseId) {
        return courseRepository.findStudentsByCourseId(courseId);
    }

    @Transactional(readOnly = true)
    public List<Long> listStudentClassIds(Long studentId) {
        return new ArrayList<>(new LinkedHashSet<>(courseRepository.findClassIdsByStudentId(studentId)));
    }

    @Transactional(readOnly = true)
    public List<TeacherClassDTO> listTeacherClasses(Long teacherId, String className, String grade, String majorName, Long majorId, Long courseId) {
        List<Map<String, Object>> rows = courseRepository.findClassesByTeacherId(teacherId, className, grade, majorName, majorId, courseId);
        Map<Long, TeacherClassDTO> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Long classId = toLong(row.get("id"));
            if (classId == null) {
                continue;
            }
            TeacherClassDTO dto = grouped.computeIfAbsent(classId, id -> toClassDto(row));
            Long rowCourseId = toLong(row.get("courseId"));
            if (rowCourseId != null && dto.getCourses().stream().noneMatch(course -> Objects.equals(course.getCourseId(), rowCourseId))) {
                ClassCourseDTO course = new ClassCourseDTO();
                course.setCourseId(rowCourseId);
                course.setCourseName(toStringValue(row.get("courseName")));
                course.setClassTime(toStringValue(row.get("classTime")));
                course.setClassLocation(toStringValue(row.get("classLocation")));
                dto.getCourses().add(course);
            }
        }
        for (TeacherClassDTO dto : grouped.values()) {
            List<String> names = dto.getCourses().stream()
                    .map(ClassCourseDTO::getCourseName)
                    .filter(name -> name != null && !name.isBlank())
                    .toList();
            dto.setCourseName(names.isEmpty() ? "未分配课程" : String.join("、", names));
            dto.setCourseCount(names.size() > 1 ? names.size() : null);
        }
        return new ArrayList<>(grouped.values());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getClass(Long teacherId, Long classId) {
        ensureTeacherCanAccessClass(teacherId, classId);
        Map<String, Object> row = courseRepository.findClassById(classId);
        if (row == null || row.isEmpty()) {
            throw new ResourceNotFoundException("班级不存在");
        }
        return row;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listClassStudents(Long teacherId, Long classId) {
        ensureTeacherCanAccessClass(teacherId, classId);
        List<Map<String, Object>> rows = courseRepository.findStudentsByClassId(classId);
        List<Long> userIds = rows.stream()
                .map(row -> toLong(row.get("id")))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return List.of();
        }
        Map<Long, UserProfileDTO> profiles = userFeignClient.listByIds(userIds).stream()
                .filter(profile -> profile.getId() != null)
                .collect(LinkedHashMap::new, (map, profile) -> map.put(profile.getId(), profile), LinkedHashMap::putAll);
        return userIds.stream()
                .map(userId -> toStudentRow(userId, profiles.get(userId)))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean classNameExists(String className, Long excludedClassId) {
        return courseRepository.classNameExists(className, excludedClassId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTeacherStudent(Long teacherId, Long studentId) {
        ensureTeacherCanAccessStudent(teacherId, studentId);
        return toTeacherStudentRow(userFeignClient.getStudentProfile(studentId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateTeacherStudent(Long teacherId, Long studentId, Map<String, Object> studentData) {
        ensureTeacherCanAccessStudent(teacherId, studentId);
        Long classId = parseOptionalClassId(studentData.get("classId"));
        if (classId != null) {
            ensureTeacherCanAccessClass(teacherId, classId);
        }

        UpdateStudentProfileDTO request = new UpdateStudentProfileDTO();
        request.setRealName(toNullableString(studentData.get("realName")));
        request.setEmail(toNullableString(studentData.get("email")));
        request.setPhone(toNullableString(studentData.get("phone")));
        request.setAvatar(toNullableString(studentData.get("avatar")));
        StudentProfileDTO updated = userFeignClient.updateStudentProfile(studentId, request);
        if (classId != null) {
            courseRepository.replaceStudentClass(studentId, classId);
            updated.setClassName(classDisplayName(classId));
        }
        return toTeacherStudentRow(updated);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> addStudentToClass(Long teacherId, Long classId, Map<String, Object> requestData) {
        ensureTeacherCanAccessClass(teacherId, classId);

        UserProfileDTO student = resolveStudentForClassAssignment(requestData);
        Long studentId = student.getId();
        if (studentId == null) {
            throw new ResourceNotFoundException("学生不存在");
        }
        if (!isStudentUser(student)) {
            throw new IllegalArgumentException("目标用户不是学生");
        }

        List<Long> existingClassIds = new ArrayList<>(new LinkedHashSet<>(courseRepository.findClassIdsByStudentId(studentId)));
        if (existingClassIds.contains(classId)) {
            return Map.of(
                    "studentId", studentId,
                    "classId", classId
            );
        }

        boolean forceReplace = Boolean.TRUE.equals(requestData.get("forceReplace"));
        if (!existingClassIds.isEmpty()) {
            boolean hasUnmanagedClass = existingClassIds.stream()
                    .anyMatch(existingClassId -> !courseRepository.teacherCanAccessClass(teacherId, existingClassId));
            if (hasUnmanagedClass) {
                throw new ForbiddenException("无权移动该学生所在班级");
            }

            if (!forceReplace) {
                return Map.of(
                        "needConfirm", true,
                        "message", "该学生已在其他班级中，是否要移动到当前班级？",
                        "studentId", studentId,
                        "classId", classId
                );
            }
        }

        courseRepository.replaceStudentClass(studentId, classId);
        return Map.of(
                "studentId", studentId,
                "classId", classId
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createClass(Long teacherId, ClassUpsertRequestDTO request) {
        ensureCourseOwnershipWhenPresent(teacherId, request.getCourseId());
        Map<String, Object> values = classValues(teacherId, request);
        return courseRepository.insertClass(values);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateClass(Long teacherId, Long classId, ClassUpsertRequestDTO request) {
        ensureTeacherCanAccessClass(teacherId, classId);
        ensureCourseOwnershipWhenPresent(teacherId, request.getCourseId());
        Map<String, Object> values = classValues(teacherId, request);
        values.put("id", classId);
        courseRepository.updateClass(values);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteClass(Long teacherId, Long classId) {
        ensureTeacherCanAccessClass(teacherId, classId);
        courseRepository.deleteClass(classId);
    }

    @Transactional(readOnly = true)
    public List<CourseAssignmentDTO> listCourseAssignments(Long teacherId, Long courseId, Long classId) {
        return courseRepository.findClassAssignments(teacherId, courseId, classId).stream()
                .map(CourseApplicationService::toAssignmentDto)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long assignCourse(Long teacherId, CourseAssignmentRequestDTO request) {
        if (request.getClassId() == null || request.getCourseId() == null) {
            throw new IllegalArgumentException("缺少必要参数：classId, courseId");
        }
        ensureTeacherCanAccessClass(teacherId, request.getClassId());
        ensureCourseOwnershipWhenPresent(teacherId, request.getCourseId());
        if (courseRepository.classCourseExists(request.getClassId(), request.getCourseId())) {
            throw new IllegalArgumentException("该班级已经分配了该课程，无需重复分配");
        }
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("classId", request.getClassId());
        values.put("courseId", request.getCourseId());
        values.put("teacherId", teacherId);
        values.put("classTime", request.getClassTime() != null ? request.getClassTime() : "");
        values.put("classLocation", request.getClassLocation() != null ? request.getClassLocation() : "");
        return courseRepository.insertClassCourse(values);
    }

    @Transactional(rollbackFor = Exception.class)
    public void unassignCourse(Long teacherId, Long assignmentId) {
        List<CourseAssignmentDTO> managed = listCourseAssignments(teacherId, null, null);
        boolean exists = managed.stream().anyMatch(item -> Objects.equals(item.getAssignmentId(), assignmentId));
        if (!exists) {
            throw new IllegalArgumentException("无权取消该课程分配");
        }
        courseRepository.deleteClassCourse(assignmentId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void unassignClassCourse(Long teacherId, Long classId, Long courseId) {
        ensureTeacherCanAccessClass(teacherId, classId);
        ensureCourseOwnershipWhenPresent(teacherId, courseId);
        courseRepository.deleteClassCourse(classId, courseId);
    }

    @Transactional(readOnly = true)
    public List<MajorDTO> listMajors() {
        return courseRepository.findMajors().stream()
                .map(row -> {
                    MajorDTO dto = new MajorDTO();
                    dto.setId(toLong(row.get("id")));
                    dto.setMajorName(toStringValue(row.get("majorName")));
                    return dto;
                })
                .toList();
    }

    private static void validateCourseDates(CourseUpsertRequestDTO request) {
        if (request == null) {
            return;
        }
        String startDate = request.getStartDate();
        String endDate = request.getEndDate();
        if (startDate != null && !startDate.isBlank() && endDate != null && !endDate.isBlank()
                && startDate.compareTo(endDate) > 0) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }
    }

    private static CourseRecord toRecord(CourseUpsertRequestDTO request) {
        CourseRecord course = new CourseRecord();
        course.setCourseName(request.getCourseName());
        course.setCourseCode(request.getCourseCode());
        course.setDescription(request.getDescription());
        course.setCredit(request.getCredit());
        course.setCourseCategory(request.getCourseCategory());
        course.setTotalHours(request.getTotalHours());
        course.setCourseDirector(request.getCourseDirector());
        course.setAssessmentMethod(request.getAssessmentMethod());
        course.setCourseStatus(request.getCourseStatus());
        course.setSemester(request.getSemester());
        course.setStartDate(request.getStartDate());
        course.setEndDate(request.getEndDate());
        course.setMaxStudents(request.getMaxStudents());
        return course;
    }

    private Map<Long, String> loadTeacherNames(List<Long> teacherIds) {
        if (teacherIds == null || teacherIds.isEmpty()) {
            return Map.of();
        }
        return userFeignClient.listByIds(teacherIds).stream()
                .filter(profile -> profile.getId() != null)
                .collect(LinkedHashMap::new,
                        (map, profile) -> map.put(profile.getId(), resolveTeacherName(profile)),
                        LinkedHashMap::putAll);
    }

    private static String resolveTeacherName(UserProfileDTO profile) {
        if (profile == null) {
            return null;
        }
        if (profile.getName() != null && !profile.getName().isBlank()) {
            return profile.getName();
        }
        return profile.getUsername();
    }

    private static CourseDTO toDto(CourseRecord course, Integer studentCount, String teacherName) {
        CourseDTO dto = new CourseDTO();
        dto.setId(course.getId());
        dto.setCourseName(course.getCourseName());
        dto.setCourseCode(course.getCourseCode());
        dto.setDescription(course.getDescription());
        dto.setCredit(course.getCredit());
        dto.setCourseCategory(course.getCourseCategory());
        dto.setTotalHours(course.getTotalHours());
        dto.setTeacherId(course.getTeacherId());
        dto.setCourseDirector(course.getCourseDirector());
        dto.setAssessmentMethod(course.getAssessmentMethod());
        dto.setCourseStatus(course.getCourseStatus());
        dto.setSemester(course.getSemester());
        dto.setStartDate(course.getStartDate());
        dto.setEndDate(course.getEndDate());
        dto.setMaxStudents(course.getMaxStudents());
        dto.setStudentCount(studentCount);
        dto.setTeacherName(teacherName);
        return dto;
    }

    private void ensureCourseOwnershipWhenPresent(Long teacherId, Long courseId) {
        if (courseId != null && !courseRepository.teacherOwnsCourse(teacherId, courseId)) {
            throw new IllegalArgumentException("无权关联其他教师的课程");
        }
    }

    private void ensureTeacherCanAccessClass(Long teacherId, Long classId) {
        if (!courseRepository.teacherCanAccessClass(teacherId, classId)) {
            throw new IllegalArgumentException("无权操作该班级");
        }
    }

    private void ensureTeacherCanAccessStudent(Long teacherId, Long studentId) {
        if (!courseRepository.findStudentIdsByTeacherId(teacherId).contains(studentId)) {
            throw new IllegalArgumentException("无权操作该学生");
        }
    }

    private UserProfileDTO resolveStudentForClassAssignment(Map<String, Object> requestData) {
        Object identifierObj = requestData.get("studentIdentifier");
        if (identifierObj == null || identifierObj.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("缺少学生标识");
        }

        String identifier = identifierObj.toString().trim();
        if (identifier.matches("\\d+")) {
            try {
                return userFeignClient.getProfile(Long.parseLong(identifier));
            } catch (RemoteClientException ex) {
                if (ex.getCode() != 404) {
                    throw ex;
                }
            }
        }

        try {
            return userFeignClient.getProfileByUsername(identifier);
        } catch (RemoteClientException ex) {
            if (ex.getCode() == 404) {
                throw new ResourceNotFoundException("学生不存在");
            }
            throw ex;
        }
    }

    private boolean isStudentUser(UserProfileDTO profile) {
        List<String> roles = profile != null ? profile.getRoles() : null;
        return roles != null && roles.stream().anyMatch(role ->
                "STUDENT".equalsIgnoreCase(role) || "ROLE_STUDENT".equalsIgnoreCase(role));
    }

    private static Map<String, Object> classValues(Long teacherId, ClassUpsertRequestDTO request) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("className", request.getClassName());
        values.put("year", request.getYear());
        values.put("capacity", request.getCapacity());
        values.put("courseId", request.getCourseId());
        values.put("teacherId", teacherId);
        values.put("majorId", request.getMajorId());
        values.put("classTime", request.getClassTime());
        values.put("classLocation", request.getClassLocation());
        return values;
    }

    private static TeacherClassDTO toClassDto(Map<String, Object> row) {
        TeacherClassDTO dto = new TeacherClassDTO();
        dto.setId(toLong(row.get("id")));
        dto.setClassName(toStringValue(row.get("className")));
        dto.setYear(toStringValue(row.get("year")));
        dto.setCapacity(toInteger(row.get("capacity")));
        dto.setStudentCount(toInteger(row.get("studentCount")));
        dto.setTeacherId(toLong(row.get("teacherId")));
        dto.setTeacherName(toStringValue(row.get("teacherName")));
        dto.setMajorId(toLong(row.get("majorId")));
        dto.setMajorName(toStringValue(row.get("majorName")));
        return dto;
    }

    private static CourseAssignmentDTO toAssignmentDto(Map<String, Object> row) {
        CourseAssignmentDTO dto = new CourseAssignmentDTO();
        dto.setAssignmentId(toLong(row.get("assignmentId")));
        dto.setClassId(toLong(row.get("classId")));
        dto.setCourseId(toLong(row.get("courseId")));
        dto.setCourseName(toStringValue(row.get("courseName")));
        dto.setClassName(toStringValue(row.get("className")));
        dto.setTeacherName(toStringValue(row.get("teacherName")));
        dto.setSemester(toStringValue(row.get("semester")));
        dto.setWeeklyHours(toStringValue(row.get("weeklyHours")));
        dto.setClassLocation(toStringValue(row.get("classLocation")));
        return dto;
    }

    private static Map<String, Object> toStudentRow(Long userId, UserProfileDTO profile) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", userId);
        row.put("username", profile != null ? profile.getUsername() : null);
        row.put("name", profile != null ? profile.getName() : null);
        row.put("email", profile != null ? profile.getEmail() : null);
        row.put("phone", profile != null ? profile.getPhone() : null);
        return row;
    }

    private static Map<String, Object> toTeacherStudentRow(StudentProfileDTO profile) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("studentId", profile.getStudentId());
        row.put("id", profile.getStudentId());
        row.put("username", profile.getUsername());
        row.put("realName", profile.getRealName());
        row.put("name", profile.getRealName());
        row.put("email", profile.getEmail());
        row.put("phone", profile.getPhone());
        row.put("avatar", profile.getAvatar());
        row.put("roles", profile.getRoles());
        row.put("className", normalizeClassName(profile.getClassName()));
        return row;
    }

    private static String normalizeClassName(String className) {
        return className == null || className.isBlank() ? "未知班级" : className;
    }

    private static String classDisplayName(Long classId) {
        return classId == null ? "未知班级" : "班级 " + classId;
    }

    private static String toNullableString(Object value) {
        return value != null ? value.toString() : null;
    }

    private static Long parseOptionalClassId(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = value.toString().trim();
        if (text.isEmpty() || "-".equals(text) || "all".equalsIgnoreCase(text)) {
            return null;
        }
        return Long.valueOf(text);
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
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
        return value != null ? value.toString() : null;
    }
}
