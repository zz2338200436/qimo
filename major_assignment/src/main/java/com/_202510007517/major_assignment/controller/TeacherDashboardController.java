package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.annotation.RequireLogin;
import com._202510007517.major_assignment.constants.RoleConstants;
import com._202510007517.major_assignment.client.UserServiceProfileClient;
import com._202510007517.major_assignment.entity.User;
import com._202510007517.major_assignment.entity.AssignmentSubmission;
import com._202510007517.major_assignment.entity.Course;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.entity.dto.ScoreTrendDTO;
import com._202510007517.major_assignment.entity.dto.StudentLearningSummaryDTO;
import com._202510007517.major_assignment.entity.dto.TeacherDashboardDTO;
import com._202510007517.major_assignment.mapper.CourseMapper;
import com._202510007517.major_assignment.mapper.StudentMapper;
import com._202510007517.major_assignment.service.TeacherDashboardService;
import com._202510007517.major_assignment.service.UserService;
import com._202510007517.major_assignment.service.AssignmentSubmissionService;
import com._202510007517.major_assignment.service.CourseService;
import com._202510007517.platform.user.api.dto.StudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateStudentProfileDTO;
import com._202510007517.major_assignment.utils.PageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/teacher")
@RequireLogin(roles = {RoleConstants.TEACHER})
public class TeacherDashboardController extends BaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(TeacherDashboardController.class);
    
    @Autowired
    private TeacherDashboardService teacherDashboardService;
    
    @Autowired
    private CourseMapper courseMapper;
    
    @Autowired
    private CourseService courseService;
    
    @Autowired
    private StudentMapper studentMapper;
    
    @Autowired
    private UserService userService;

    @Autowired
    private UserServiceProfileClient userServiceProfileClient;
    
    @Autowired
    private AssignmentSubmissionService assignmentSubmissionService;
    
    @GetMapping("/dashboard")
    public ResponseResult<TeacherDashboardDTO> getDashboard(
            @RequestParam(value = "classId", required = false) String classId,
            @RequestParam(value = "courseId", required = false) String courseId,
            @RequestParam(value = "timeRange", required = false) String timeRange,
            HttpServletRequest requestContext) {
        Long classIdLong = parseOptionalFilterId(classId);
        Long courseIdLong = parseOptionalFilterId(courseId);
        
        Long teacherId = getCurrentUserId(requestContext);
        TeacherDashboardDTO dashboardData = teacherDashboardService.getDashboardData(teacherId, classIdLong, courseIdLong, timeRange);
        
        return ResponseResult.success(dashboardData, "获取教师仪表盘数据成功", 200);
    }
    
    @GetMapping("/learning-summary")
    public ResponseResult<StudentLearningSummaryDTO> getStudentLearningSummary(
            @RequestParam(value = "classId", required = false) String classId,
            @RequestParam(value = "courseId", required = false) String courseId,
            @RequestParam(value = "timeRange", required = false) String timeRange,
            HttpServletRequest requestContext) {
        Long classIdLong = parseOptionalFilterId(classId);
        Long courseIdLong = parseOptionalFilterId(courseId);
        
        Long teacherId = getCurrentUserId(requestContext);
        StudentLearningSummaryDTO summaryData = teacherDashboardService.getStudentLearningSummary(teacherId, classIdLong, courseIdLong, timeRange);
        
        return ResponseResult.success(summaryData, "获取学生学习汇总数据成功", 200);
    }

    @GetMapping("/score-trend")
    public ResponseResult<List<ScoreTrendDTO>> getScoreTrend(
            @RequestParam(value = "classId", required = false) String classId,
            @RequestParam(value = "courseId", required = false) String courseId,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestParam(value = "timeRange", required = false) String timeRange,
            HttpServletRequest requestContext) {
        Long classIdLong = parseOptionalFilterId(classId);
        Long courseIdLong = parseOptionalFilterId(courseId);
        Long teacherId = getCurrentUserId(requestContext);

        List<ScoreTrendDTO> trend = teacherDashboardService.getScoreTrend(teacherId, classIdLong, courseIdLong, studentId, timeRange);
        return ResponseResult.success(trend, "获取成绩趋势数据成功", 200);
    }
    
    @GetMapping("/classes")
    public ResponseResult<List<Map<String, Object>>> getClasses(
            @RequestParam(value = "className", required = false) String className,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "majorName", required = false) String majorName,
            @RequestParam(value = "majorId", required = false) Long majorId,
            @RequestParam(value = "teacherName", required = false) String teacherName,
            @RequestParam(value = "courseId", required = false) Long courseId,
            HttpServletRequest requestContext) {
        Long teacherId = getCurrentUserId(requestContext);
        List<Map<String, Object>> classes = courseMapper.getClassesByTeacherId(teacherId, className, grade, majorName, majorId, teacherName, courseId);
        
        // 按班级ID分组，每个班级包含多个课程
        Map<Long, Map<String, Object>> uniqueClassesMap = new LinkedHashMap<>();
        for (Map<String, Object> cls : classes) {
            Object idObj = cls.get("id");
            if (idObj != null) {
                Long id = idObj instanceof Long ? (Long) idObj : Long.valueOf(idObj.toString());
                if (!uniqueClassesMap.containsKey(id)) {
                    // 创建班级对象，包含课程列表
                    Map<String, Object> classInfo = new java.util.HashMap<>();
                    classInfo.put("id", id);
                    classInfo.put("className", cls.get("className"));
                    classInfo.put("year", cls.get("year"));
                    classInfo.put("capacity", cls.get("capacity"));
                    classInfo.put("studentCount", cls.get("studentCount"));
                    classInfo.put("teacherId", cls.get("teacherId"));
                    classInfo.put("teacherName", cls.get("teacherName"));
                    classInfo.put("majorId", cls.get("majorId"));
                    classInfo.put("majorName", cls.get("majorName"));
                    
                    // 初始化课程列表
                    List<Map<String, Object>> courses = new ArrayList<>();
                    if (cls.get("courseId") != null) {
                        Map<String, Object> course = new java.util.HashMap<>();
                        course.put("courseId", cls.get("courseId"));
                        course.put("courseName", cls.get("courseName"));
                        course.put("classTime", cls.get("classTime"));
                        course.put("classLocation", cls.get("classLocation"));
                        courses.add(course);
                    }
                    classInfo.put("courses", courses);
                    
                    uniqueClassesMap.put(id, classInfo);
                } else {
                    // 如果班级已存在，添加课程信息（如果该课程尚未添加）
                    Map<String, Object> classInfo = uniqueClassesMap.get(id);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> courses = (List<Map<String, Object>>) classInfo.get("courses");
                    if (cls.get("courseId") != null) {
                        Long courseIdValue = cls.get("courseId") instanceof Long ? 
                            (Long) cls.get("courseId") : Long.valueOf(cls.get("courseId").toString());
                        // 检查课程是否已存在
                        boolean courseExists = courses.stream().anyMatch(c -> {
                            Object cid = c.get("courseId");
                            return cid != null && (cid.equals(courseIdValue) || cid.toString().equals(courseIdValue.toString()));
                        });
                        if (!courseExists) {
                            Map<String, Object> course = new java.util.HashMap<>();
                            course.put("courseId", cls.get("courseId"));
                            course.put("courseName", cls.get("courseName"));
                            course.put("classTime", cls.get("classTime"));
                            course.put("classLocation", cls.get("classLocation"));
                            courses.add(course);
                        }
                    }
                }
            }
        }
        
        // 返回分组后的数据，每个班级只返回一次，但包含所有课程信息
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> classInfo : uniqueClassesMap.values()) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> courses = (List<Map<String, Object>>) classInfo.get("courses");
            
            // 构建课程名称列表（用于显示）
            List<String> courseNames = new ArrayList<>();
            if (courses != null && !courses.isEmpty()) {
                for (Map<String, Object> course : courses) {
                    String courseName = (String) course.get("courseName");
                    if (courseName != null && !courseName.isEmpty()) {
                        courseNames.add(courseName);
                    }
                }
            }
            
            // 设置课程名称（多个课程用逗号分隔）
            if (!courseNames.isEmpty()) {
                classInfo.put("courseName", String.join("、", courseNames));
                // 如果有多个课程，添加课程数量信息
                if (courseNames.size() > 1) {
                    classInfo.put("courseCount", courseNames.size());
                }
            } else {
                classInfo.put("courseName", "未分配课程");
            }
            
            // 保留课程列表供前端使用
            classInfo.put("courses", courses);
            
            result.add(classInfo);
        }
        
        return ResponseResult.success(result, "获取班级列表成功", 200);
    }

    private Long parseOptionalFilterId(String rawValue) {
        if (rawValue == null || rawValue.isBlank() || "all".equalsIgnoreCase(rawValue)) {
            return null;
        }
        return Long.parseLong(rawValue);
    }
    
    @PostMapping("/classes")
    public ResponseResult<Map<String, Object>> createClass(@RequestBody Map<String, Object> classData, HttpServletRequest requestContext) {
        
        try {
            Long teacherId = getCurrentUserId(requestContext);
            classData.put("teacherId", teacherId);

            Long courseId = parseNullableLong(classData.get("courseId"));
            if (courseId != null && !teacherOwnsCourse(teacherId, courseId)) {
                return ResponseResult.failure("无权关联其他教师的课程", 403);
            }
            classData.put("courseId", courseId);

            // 实现创建班级逻辑
            courseMapper.createClass(classData);
            
            return ResponseResult.success(null, "班级创建成功", 201);
        } catch (Exception e) {
            logger.error("创建班级失败", e);
            return ResponseResult.failure("创建班级失败: " + e.getMessage(), 500);
        }
    }
    
    @PutMapping("/classes/{classId}")
    public ResponseResult<Map<String, Object>> updateClass(@PathVariable Long classId, @RequestBody Map<String, Object> classData, HttpServletRequest requestContext) {
        
        try {
            Long teacherId = getCurrentUserId(requestContext);
            if (!teacherCanAccessClass(teacherId, classId)) {
                return ResponseResult.failure("无权操作该班级", 403);
            }

            // 添加classId到更新数据中
            classData.put("id", classId);
            classData.put("teacherId", teacherId);
            
            // 确保所有数字字段都是正确的类型
            if (classData.containsKey("year") && classData.get("year") != null) {
                classData.put("year", Integer.parseInt(classData.get("year").toString()));
            }
            if (classData.containsKey("capacity") && classData.get("capacity") != null) {
                classData.put("capacity", Integer.parseInt(classData.get("capacity").toString()));
            }
            if (classData.containsKey("majorId") && classData.get("majorId") != null) {
                classData.put("majorId", Long.parseLong(classData.get("majorId").toString()));
            }
            if (classData.containsKey("courseId")) {
                Long courseId = parseNullableLong(classData.get("courseId"));
                if (courseId != null && !teacherOwnsCourse(teacherId, courseId)) {
                    return ResponseResult.failure("无权关联其他教师的课程", 403);
                }
                classData.put("courseId", courseId);
            }
            
            // 实现更新班级逻辑
            courseMapper.updateClass(classData);
            
            return ResponseResult.success(null, "班级更新成功", 200);
        } catch (Exception e) {
            logger.error("更新班级失败", e);
            return ResponseResult.failure("更新班级失败: " + e.getMessage(), 500);
        }
    }
    
    @DeleteMapping("/classes/{classId}")
    public ResponseResult<Void> deleteClass(@PathVariable Long classId, HttpServletRequest requestContext) {
        
        try {
            Long teacherId = getCurrentUserId(requestContext);
            if (!teacherCanAccessClass(teacherId, classId)) {
                return ResponseResult.failure("无权操作该班级", 403);
            }
            // 通过 Service 层删除班级，会自动处理外键约束
            courseService.deleteClass(classId);
            return ResponseResult.success(null, "班级删除成功", 204);
        } catch (Exception e) {
            logger.error("删除班级失败", e);
            return ResponseResult.failure("删除班级失败: " + e.getMessage(), 500);
        }
    }
    
    @GetMapping("/classes/{classId}")
    public ResponseResult<Map<String, Object>> getClassById(@PathVariable Long classId, HttpServletRequest requestContext) {
        
        try {
            Long teacherId = getCurrentUserId(requestContext);
            if (!teacherCanAccessClass(teacherId, classId)) {
                return ResponseResult.failure("无权查看该班级", 403);
            }
            // 实现获取单个班级详情逻辑
            Map<String, Object> cls = courseMapper.getClassById(classId);
            
            if (cls == null) {
                return ResponseResult.failure("班级不存在", 404);
            }
            
            return ResponseResult.success(cls, "获取班级详情成功", 200);
        } catch (Exception e) {
            logger.error("获取班级详情失败", e);
            return ResponseResult.failure("服务器内部错误", 500);
        }
    }
    
    @GetMapping("/classes/{classId}/students")
    public ResponseResult<List<Map<String, Object>>> getClassStudents(@PathVariable Long classId, HttpServletRequest requestContext) {

        Long teacherId = getCurrentUserId(requestContext);
        if (!teacherCanAccessClass(teacherId, classId)) {
            return ResponseResult.failure("无权查看该班级学生", 403);
        }
        
        List<Map<String, Object>> students = studentMapper.getStudentsByClassId(classId);
        
        return ResponseResult.success(students, "获取班级学生列表成功", 200);
    }

    @PostMapping("/classes/{classId}/students")
    public ResponseResult<Object> addStudentToClass(
            @PathVariable Long classId,
            @RequestBody Map<String, Object> requestData,
            HttpServletRequest requestContext) {

        try {
            Long teacherId = getCurrentUserId(requestContext);
            if (!teacherCanAccessClass(teacherId, classId)) {
                return ResponseResult.failure("无权操作该班级", 403);
            }

            User student = resolveStudentForClassAssignment(requestData);
            if (student == null) {
                return ResponseResult.failure("学生不存在", 404);
            }
            if (!isStudentUser(student.getId())) {
                return ResponseResult.failure("目标用户不是学生", 400);
            }

            List<Map<String, Object>> existingClasses = courseMapper.getClassesByStudentId(student.getId());
            if (existingClasses != null && !existingClasses.isEmpty()) {
                boolean alreadyInTargetClass = existingClasses.stream()
                        .anyMatch(c -> classId.equals(parseNullableLong(c.get("id"))));
                if (alreadyInTargetClass) {
                    return ResponseResult.failure("学生已经在该班级中", 400);
                }

                boolean allClassesManagedByTeacher = existingClasses.stream()
                        .allMatch(c -> teacherCanAccessClass(teacherId, parseNullableLong(c.get("id"))));
                if (!allClassesManagedByTeacher) {
                    return ResponseResult.failure("无权移动该学生所在班级", 403);
                }

                boolean forceReplace = Boolean.parseBoolean(String.valueOf(requestData.getOrDefault("forceReplace", false)));
                if (!forceReplace) {
                    StringBuilder classNames = new StringBuilder();
                    for (Map<String, Object> cls : existingClasses) {
                        if (classNames.length() > 0) {
                            classNames.append("、");
                        }
                        classNames.append(String.valueOf(cls.getOrDefault("className", "未知班级")));
                    }
                    Map<String, Object> warningData = new HashMap<>();
                    warningData.put("needConfirm", true);
                    warningData.put("message", "该学生已在班级【" + classNames + "】中，是否要将其移动到当前班级？");
                    warningData.put("existingClasses", existingClasses);
                    return ResponseResult.success(warningData, "需要确认操作", 200);
                }
            }

            studentMapper.deleteStudentClass(student.getId());
            studentMapper.insertStudentClass(classId, student.getId());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("studentId", student.getId());
            responseData.put("username", student.getUsername());
            responseData.put("realName", student.getName());
            responseData.put("classId", classId);
            return ResponseResult.success(responseData, "学生已添加到班级", 200);
        } catch (IllegalArgumentException e) {
            return ResponseResult.failure(e.getMessage(), 400);
        } catch (Exception e) {
            logger.error("添加班级学生失败 classId={}", classId, e);
            return ResponseResult.failure("添加学生到班级失败", 500);
        }
    }
    
    @GetMapping("/students/{studentId}")
    public ResponseResult<Map<String, Object>> getStudentById(@PathVariable Long studentId, HttpServletRequest requestContext) {

        try {
            Long teacherId = getCurrentUserId(requestContext);
            if (!teacherCanAccessStudent(teacherId, studentId)) {
                return ResponseResult.failure("无权查看该学生", 403);
            }

            Map<String, Object> studentInfo = userServiceProfileClient.getStudentProfile(studentId)
                    .map(this::toStudentInfo)
                    .orElseGet(() -> buildLegacyStudentInfo(studentId));
            if (studentInfo == null) {
                return ResponseResult.failure("学生不存在", 404);
            }
            
            // 尝试获取学生学习表现数据
            Map<String, Object> performanceData = studentMapper.getStudentPerformance(studentId);
            if (performanceData != null) {
                studentInfo.putAll(performanceData);
            }
            
            return ResponseResult.success(studentInfo, "获取学生详情成功", 200);
        } catch (Exception e) {
            logger.error("获取学生详情失败", e);
            return ResponseResult.failure("服务器内部错误", 500);
        }
    }
    
    // 课程分配相关接口
    @GetMapping("/course-assignments")
    public ResponseResult<Map<String, Object>> getClassAssignments(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "classId", required = false) Long classId,
            HttpServletRequest requestContext) {
        
        Long teacherId = getCurrentUserId(requestContext);

        // 当前 SQL 不支持 LIMIT，先全量取回后再用统一分页规则裁切。
        List<Map<String, Object>> allAssignments = courseMapper.getClassAssignments(teacherId, courseId, classId);

        return ResponseResult.success(
                buildSpringPageResponseFromInMemoryList(allAssignments, page == null ? 1 : page, size == null ? DEFAULT_PAGE_SIZE : size),
                "获取课程分配列表成功",
                200);
    }
    
    @GetMapping("/check-class-name")
    public ResponseResult<Map<String, Boolean>> checkClassNameExists(
            @RequestParam String className,
            @RequestParam(required = false) Long classId,
            HttpServletRequest requestContext) {
        
        try {
            // 查询班级名称是否存在
            Integer count = courseMapper.checkClassNameExists(className, classId);
            
            Map<String, Boolean> result = new java.util.HashMap<>();
            result.put("exists", count != null && count > 0);
            
            return ResponseResult.success(result, "班级名称检查成功", 200);
        } catch (Exception e) {
            logger.error("班级名称检查失败", e);
            return ResponseResult.failure("班级名称检查失败: " + e.getMessage(), 500);
        }
    }
    
    @PostMapping("/course-assignments")
    public ResponseResult<Map<String, Object>> assignCourse(@RequestBody Map<String, Object> assignData, HttpServletRequest requestContext) {
        
        try {
            Long currentTeacherId = getCurrentUserId(requestContext);
            // 关联已有班级模式（仅支持此模式）
            Object classIdObj = assignData.get("classId");
            Object courseIdObj = assignData.get("courseId");

            if (classIdObj == null || courseIdObj == null) {
                return ResponseResult.failure("缺少必要参数：classId, courseId", 400);
            }

            Long classId = Long.parseLong(classIdObj.toString());
            Long courseId = Long.parseLong(courseIdObj.toString());

            if (!teacherCanAccessClass(currentTeacherId, classId)) {
                return ResponseResult.failure("无权操作该班级", 403);
            }
            if (!teacherOwnsCourse(currentTeacherId, courseId)) {
                return ResponseResult.failure("无权分配其他教师的课程", 403);
            }
            
            // 查询已有班级的详细信息
            Map<String, Object> existingClass = courseMapper.getClassById(classId);
            
            if (existingClass == null) {
                return ResponseResult.failure("班级不存在", 404);
            }
            
            // 检查该班级是否已经分配了该课程（使用 class_courses 中间表）
            Map<String, Object> existingAssignment = courseMapper.checkClassCourseAssignment(classId, courseId);
            
            if (existingAssignment != null && !existingAssignment.isEmpty()) {
                // 已分配，返回提示信息
                return ResponseResult.failure("该班级已经分配了该课程，无需重复分配", 400);
            } else {
                // 未分配，在 class_courses 表中添加关联记录（不创建新的 course_classes 记录）
                Map<String, Object> assignDataMap = new java.util.HashMap<>();
                assignDataMap.put("classId", classId);  // 使用现有的班级ID
                assignDataMap.put("courseId", courseId);
                assignDataMap.put("teacherId", currentTeacherId);
                Object classTimeObj = assignData.get("classTime");
                Object classLocationObj = assignData.get("classLocation");
                assignDataMap.put("classTime", classTimeObj != null ? classTimeObj.toString() : "");
                assignDataMap.put("classLocation", classLocationObj != null ? classLocationObj.toString() : "");
                
                // 在 class_courses 表中添加关联记录
                courseMapper.assignCourse(assignDataMap);
                
                // 学生已经关联到班级，通过 class_students 表可以自动看到新分配的课程
            }
            
            return ResponseResult.success(null, "课程分配成功", 201);
        } catch (Exception e) {
            return ResponseResult.failure("创建课程分配失败: " + e.getMessage(), 500);
        }
    }
    
    @DeleteMapping("/course-assignments/{assignmentId}")
    public ResponseResult<Void> unassignCourse(@PathVariable Long assignmentId, HttpServletRequest requestContext) {
        
        try {
            Long teacherId = getCurrentUserId(requestContext);
            Integer managedCount = courseMapper.countManagedAssignments(teacherId, assignmentId);
            if (managedCount == null || managedCount <= 0) {
                return ResponseResult.failure("无权取消该课程分配", 403);
            }
            // 实现取消课程分配逻辑（通过Service层，会自动处理关联记录的删除）
            courseService.unassignCourse(assignmentId);
            
            return ResponseResult.success(null, "课程分配已取消", 204);
        } catch (Exception e) {
            logger.error("取消课程分配失败", e);
            return ResponseResult.failure("取消课程分配失败: " + e.getMessage(), 500);
        }
    }
    
    @DeleteMapping("/class-courses/unassign")
    public ResponseResult<Void> unassignClassCourse(
            @RequestParam Long classId,
            @RequestParam Long courseId,
            HttpServletRequest requestContext) {
        
        try {
            Long teacherId = getCurrentUserId(requestContext);
            if (!teacherCanAccessClass(teacherId, classId)) {
                return ResponseResult.failure("无权操作该班级", 403);
            }
            if (!teacherOwnsCourse(teacherId, courseId)) {
                return ResponseResult.failure("无权取消其他教师的课程分配", 403);
            }
            // 通过classId和courseId取消课程分配
            Map<String, Object> params = new java.util.HashMap<>();
            params.put("classId", classId);
            params.put("courseId", courseId);
            courseMapper.unassignClassCourse(params);
            
            return ResponseResult.success(null, "课程分配已取消", 204);
        } catch (Exception e) {
            logger.error("取消课程分配失败", e);
            return ResponseResult.failure("取消课程分配失败: " + e.getMessage(), 500);
        }
    }
    
    @PutMapping("/students/{studentId}")
    public ResponseResult<Object> updateStudent(@PathVariable Long studentId, @RequestBody Map<String, Object> studentData, HttpServletRequest requestContext) {
        
        try {
            Long teacherId = getCurrentUserId(requestContext);
            if (!teacherCanAccessStudent(teacherId, studentId)) {
                return ResponseResult.failure("无权修改该学生", 403);
            }

            Object updatedStudent = updateStudentProfileThroughUserService(studentId, studentData);
            if (updatedStudent == null) {
                return ResponseResult.failure("学生不存在", 404);
            }
            
            // 如果有班级ID，更新学生的班级信息
            if (studentData.containsKey("classId")) {
                Object classIdObj = studentData.get("classId");
                if (classIdObj != null) {
                    String classIdStr = classIdObj.toString().trim();
                    if (!"all".equals(classIdStr) && !classIdStr.isEmpty()) {
                        try {
                            Long classId = Long.parseLong(classIdStr);
                            if (!teacherCanAccessClass(teacherId, classId)) {
                                return ResponseResult.failure("无权将学生移动到该班级", 403);
                            }
                            
                            // 检查学生是否已经在其他班级
                            List<Map<String, Object>> existingClasses = courseMapper.getClassesByStudentId(studentId);
                            
                            if (existingClasses != null && !existingClasses.isEmpty()) {
                                // 检查是否已经在目标班级
                                boolean alreadyInTargetClass = existingClasses.stream()
                                    .anyMatch(c -> classId.equals(c.get("id")));
                                
                                if (alreadyInTargetClass) {
                                    return ResponseResult.failure("学生已经在该班级中", 400);
                                }
                                
                                // 学生已在其他班级，返回警告信息
                                StringBuilder classNames = new StringBuilder();
                                for (Map<String, Object> cls : existingClasses) {
                                    if (classNames.length() > 0) {
                                        classNames.append("、");
                                    }
                                    classNames.append(cls.get("className"));
                                }
                                
                                // 检查是否有强制替换标志
                                Boolean forceReplace = studentData.containsKey("forceReplace") 
                                    ? (Boolean) studentData.get("forceReplace") 
                                    : false;
                                
                                if (!forceReplace) {
                                    // 返回警告，需要用户确认
                                    Map<String, Object> warningData = new HashMap<>();
                                    warningData.put("needConfirm", true);
                                    warningData.put("message", "该学生已在班级【" + classNames.toString() + "】中，是否要将其移动到新班级？");
                                    warningData.put("existingClasses", existingClasses);
                                    return ResponseResult.success(warningData, "需要确认操作", 200);
                                }
                            }
                            
                            // 先删除学生旧的班级关联
                            studentMapper.deleteStudentClass(studentId);
                            // 再插入新的班级关联
                            studentMapper.insertStudentClass(classId, studentId);
                        } catch (NumberFormatException e) {
                            return ResponseResult.failure("无效的班级ID格式", 400);
                        } catch (Exception e) {
                            return ResponseResult.failure("更新学生班级信息失败: " + e.getMessage(), 500);
                        }
                    }
                }
            }
            
            // 更新学生学习表现数据
            if (studentData.containsKey("averageScore") || studentData.containsKey("pendingAssignments") || studentData.containsKey("overallProgress")) {
                Double averageScore = studentData.containsKey("averageScore") && studentData.get("averageScore") != null ? Double.parseDouble(studentData.get("averageScore").toString()) : null;
                Integer pendingAssignments = studentData.containsKey("pendingAssignments") && studentData.get("pendingAssignments") != null ? Integer.parseInt(studentData.get("pendingAssignments").toString()) : null;
                Integer overallProgress = studentData.containsKey("overallProgress") && studentData.get("overallProgress") != null ? Integer.parseInt(studentData.get("overallProgress").toString()) : null;
                
                // 调用StudentMapper的updateStudentPerformance方法来更新学生学习表现数据
                studentMapper.updateStudentPerformance(studentId, averageScore, pendingAssignments, overallProgress);
            }
            
            return ResponseResult.success(updatedStudent, "学生信息更新成功", 200);
        } catch (Exception e) {
            logger.error("更新学生信息失败", e);
            return ResponseResult.failure("服务器内部错误", 500);
        }
    }
    
    // 提交记录相关接口
    @GetMapping("/submissions")
    public ResponseResult<Map<String, Object>> getAllSubmissions(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(value = "order", defaultValue = "DESC") String order,
            @RequestParam(value = "assignmentId", required = false) Long assignmentId,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestParam(value = "graded", required = false) Boolean graded,
            HttpServletRequest requestContext) {
        int total = assignmentSubmissionService.countSubmissions(assignmentId, studentId, graded);
        PageUtils.PageWindow window = resolvePageWindow(page == null ? 1 : page, size == null ? DEFAULT_PAGE_SIZE : size, total);

        List<AssignmentSubmission> submissions = assignmentSubmissionService.getSubmissionsWithPagination(
                window.page(), window.size(), total, sortBy, order, assignmentId, studentId, graded);

        return ResponseResult.success(
                buildSpringPageResponse(submissions, window.page(), window.size(), total),
                "获取作业提交记录成功",
                200);
    }
    
    @GetMapping("/submissions/{submissionId}")
    public ResponseResult<AssignmentSubmission> getSubmissionById(@PathVariable Long submissionId, HttpServletRequest requestContext) {
        
        AssignmentSubmission submission = assignmentSubmissionService.getSubmissionById(submissionId);
        if (submission == null) {
            return ResponseResult.failure("作业提交记录不存在", 404);
        }
        
        return ResponseResult.success(submission, "获取作业提交记录详情成功", 200);
    }
    
    @PutMapping("/submissions/{submissionId}/grade")
    public ResponseResult<AssignmentSubmission> gradeSubmission(@PathVariable Long submissionId, @RequestBody Map<String, Object> gradeData, HttpServletRequest requestContext) {
        
        try {
            Integer score = gradeData.get("score") != null ? Integer.parseInt(gradeData.get("score").toString()) : null;
            String teacherComment = gradeData.get("teacherComment") != null ? (String) gradeData.get("teacherComment") : "";
            
            boolean success = assignmentSubmissionService.gradeAssignment(submissionId, score, teacherComment);
            if (success) {
                AssignmentSubmission submission = assignmentSubmissionService.getSubmissionById(submissionId);
                return ResponseResult.success(submission, "作业批改成功", 200);
            } else {
                return ResponseResult.failure("作业批改失败", 500);
            }
        } catch (Exception e) {
            logger.error("作业批改失败", e);
            return ResponseResult.failure("作业批改失败: " + e.getMessage(), 500);
        }
    }

    private boolean teacherCanAccessClass(Long teacherId, Long classId) {
        Integer count = courseMapper.countManagedClasses(teacherId, classId);
        return count != null && count > 0;
    }

    private boolean teacherCanAccessStudent(Long teacherId, Long studentId) {
        List<Long> studentIds = courseMapper.getStudentIdsByClassTeacherId(teacherId);
        if (studentIds == null || studentIds.isEmpty()) {
            return false;
        }
        Set<Long> visibleStudentIds = new HashSet<>(studentIds);
        return visibleStudentIds.contains(studentId);
    }

    private User resolveStudentForClassAssignment(Map<String, Object> requestData) {
        Object identifierObj = requestData.get("studentIdentifier");
        if (identifierObj == null || identifierObj.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("缺少学生标识");
        }

        String identifier = identifierObj.toString().trim();
        if (identifier.matches("\\d+")) {
            User studentById = userService.findById(Long.parseLong(identifier));
            if (studentById != null) {
                return studentById;
            }
        }
        return userService.findByUsername(identifier);
    }

    private boolean isStudentUser(Long userId) {
        List<String> roles = userService.getRolesByUserId(userId);
        return roles != null && roles.stream().anyMatch(role ->
                RoleConstants.STUDENT.equalsIgnoreCase(role)
                        || RoleConstants.ROLE_STUDENT.equalsIgnoreCase(role));
    }

    private Map<String, Object> toStudentInfo(StudentProfileDTO profile) {
        Map<String, Object> studentInfo = new HashMap<>();
        studentInfo.put("studentId", profile.getStudentId());
        studentInfo.put("realName", profile.getRealName());
        studentInfo.put("username", profile.getUsername());
        studentInfo.put("email", profile.getEmail());
        studentInfo.put("phone", profile.getPhone());
        studentInfo.put("avatar", profile.getAvatar());
        studentInfo.put("roles", profile.getRoles());
        studentInfo.put("className", normalizeClassName(profile.getClassName()));
        return studentInfo;
    }

    private Map<String, Object> buildLegacyStudentInfo(Long studentId) {
        User student = userService.findById(studentId);
        if (student == null) {
            return null;
        }
        Map<String, Object> studentInfo = new HashMap<>();
        studentInfo.put("studentId", student.getId());
        studentInfo.put("realName", student.getName());
        studentInfo.put("className", normalizeClassName(userService.getStudentClassName(studentId)));
        return studentInfo;
    }

    private Object updateStudentProfileThroughUserService(Long studentId, Map<String, Object> studentData) {
        UpdateStudentProfileDTO request = buildStudentProfileUpdate(studentData);
        if (hasStudentProfileUpdate(request)) {
            return userServiceProfileClient.updateStudentProfile(studentId, request)
                    .map(profile -> (Object) profile)
                    .orElse(null);
        }
        return userServiceProfileClient.getStudentProfile(studentId)
                .map(profile -> (Object) profile)
                .orElseGet(() -> userService.findById(studentId));
    }

    private UpdateStudentProfileDTO buildStudentProfileUpdate(Map<String, Object> studentData) {
        UpdateStudentProfileDTO request = new UpdateStudentProfileDTO();
        if (studentData.containsKey("realName")) {
            request.setRealName(toNullableString(studentData.get("realName")));
        }
        if (studentData.containsKey("email")) {
            request.setEmail(toNullableString(studentData.get("email")));
        }
        if (studentData.containsKey("phone")) {
            request.setPhone(toNullableString(studentData.get("phone")));
        }
        if (studentData.containsKey("avatar")) {
            request.setAvatar(toNullableString(studentData.get("avatar")));
        }
        return request;
    }

    private boolean hasStudentProfileUpdate(UpdateStudentProfileDTO request) {
        return request.getRealName() != null
                || request.getEmail() != null
                || request.getPhone() != null
                || request.getAvatar() != null;
    }

    private String toNullableString(Object value) {
        return value != null ? value.toString() : null;
    }

    private String normalizeClassName(String className) {
        return className == null || className.trim().isEmpty() ? "未知班级" : className;
    }

    private boolean teacherOwnsCourse(Long teacherId, Long courseId) {
        Course course = courseService.findById(courseId);
        return course != null && teacherId.equals(course.getTeacherId());
    }

    private Long parseNullableLong(Object value) {
        if (value == null) {
            return null;
        }
        String stringValue = value.toString().trim();
        if (stringValue.isEmpty() || "-".equals(stringValue) || "all".equalsIgnoreCase(stringValue)) {
            return null;
        }
        return Long.parseLong(stringValue);
    }
}

