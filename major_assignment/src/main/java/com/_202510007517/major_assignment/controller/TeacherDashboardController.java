package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.entity.User;
import com._202510007517.major_assignment.entity.AssignmentSubmission;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
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
    private AssignmentSubmissionService assignmentSubmissionService;
    
    @GetMapping("/dashboard")
    public ResponseResult<TeacherDashboardDTO> getDashboard(
            @RequestParam(value = "classId", required = false) String classId,
            @RequestParam(value = "courseId", required = false) String courseId,
            @RequestParam(value = "timeRange", required = false) String timeRange,
            HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        // 处理参数，将"all"转换为null
        Long classIdLong = classId != null && !"all".equals(classId) ? Long.parseLong(classId) : null;
        Long courseIdLong = courseId != null && !"all".equals(courseId) ? Long.parseLong(courseId) : null;
        
        Long teacherId = getCurrentUserId(session);
        // 传递timeRange参数给service层
        TeacherDashboardDTO dashboardData = teacherDashboardService.getDashboardData(teacherId, classIdLong, courseIdLong, timeRange);
        
        return ResponseResult.success(dashboardData, "获取教师仪表盘数据成功", 200);
    }
    
    @GetMapping("/learning-summary")
    public ResponseResult<StudentLearningSummaryDTO> getStudentLearningSummary(
            @RequestParam(value = "classId", required = false) String classId,
            @RequestParam(value = "courseId", required = false) String courseId,
            @RequestParam(value = "timeRange", required = false) String timeRange,
            HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        // 处理参数，将"all"转换为null
        Long classIdLong = classId != null && !"all".equals(classId) ? Long.parseLong(classId) : null;
        Long courseIdLong = courseId != null && !"all".equals(courseId) ? Long.parseLong(courseId) : null;
        
        Long teacherId = getCurrentUserId(session);
        // 传递timeRange参数给service层
        StudentLearningSummaryDTO summaryData = teacherDashboardService.getStudentLearningSummary(teacherId, classIdLong, courseIdLong, timeRange);
        
        return ResponseResult.success(summaryData, "获取学生学习汇总数据成功", 200);
    }

    @GetMapping("/score-trend")
    public ResponseResult<List<ScoreTrendDTO>> getScoreTrend(
            @RequestParam(value = "classId", required = false) String classId,
            @RequestParam(value = "courseId", required = false) String courseId,
            @RequestParam(value = "timeRange", required = false) String timeRange,
            HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }

        Long classIdLong = classId != null && !"all".equals(classId) ? Long.parseLong(classId) : null;
        Long courseIdLong = courseId != null && !"all".equals(courseId) ? Long.parseLong(courseId) : null;
        Long teacherId = getCurrentUserId(session);

        List<ScoreTrendDTO> trend = teacherDashboardService.getScoreTrend(teacherId, classIdLong, courseIdLong, timeRange);
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
            HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        Long teacherId = getCurrentUserId(session);
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
    
    @PostMapping("/classes")
    public ResponseResult<Map<String, Object>> createClass(@RequestBody Map<String, Object> classData, HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        try {
            // 实现创建班级逻辑
            courseMapper.createClass(classData);
            
            return ResponseResult.success(null, "班级创建成功", 201);
        } catch (Exception e) {
            logger.error("创建班级失败", e);
            return ResponseResult.failure("创建班级失败: " + e.getMessage(), 500);
        }
    }
    
    @PutMapping("/classes/{classId}")
    public ResponseResult<Map<String, Object>> updateClass(@PathVariable Long classId, @RequestBody Map<String, Object> classData, HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        try {
            // 添加classId到更新数据中
            classData.put("id", classId);
            
            // 确保所有数字字段都是正确的类型
            if (classData.containsKey("year")) {
                classData.put("year", Integer.parseInt(classData.get("year").toString()));
            }
            if (classData.containsKey("capacity")) {
                classData.put("capacity", Integer.parseInt(classData.get("capacity").toString()));
            }
            if (classData.containsKey("majorId")) {
                classData.put("majorId", Long.parseLong(classData.get("majorId").toString()));
            }
            if (classData.containsKey("teacherId")) {
                classData.put("teacherId", Long.parseLong(classData.get("teacherId").toString()));
            }
            if (classData.containsKey("courseId")) {
                Object courseIdValue = classData.get("courseId");
                if (courseIdValue != null && !"-".equals(courseIdValue) && !"".equals(courseIdValue)) {
                    classData.put("courseId", Long.parseLong(courseIdValue.toString()));
                } else {
                    classData.put("courseId", null);
                }
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
    public ResponseResult<Void> deleteClass(@PathVariable Long classId, HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        try {
            // 通过 Service 层删除班级，会自动处理外键约束
            courseService.deleteClass(classId);
            return ResponseResult.success(null, "班级删除成功", 204);
        } catch (Exception e) {
            logger.error("删除班级失败", e);
            return ResponseResult.failure("删除班级失败: " + e.getMessage(), 500);
        }
    }
    
    @GetMapping("/classes/{classId}")
    public ResponseResult<Map<String, Object>> getClassById(@PathVariable Long classId, HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        try {
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
    public ResponseResult<List<Map<String, Object>>> getClassStudents(@PathVariable Long classId, HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        List<Map<String, Object>> students = studentMapper.getStudentsByClassId(classId);
        
        return ResponseResult.success(students, "获取班级学生列表成功", 200);
    }
    
    @GetMapping("/students/{studentId}")
    public ResponseResult<Map<String, Object>> getStudentById(@PathVariable Long studentId, HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }

        try {
            // 获取学生基本信息
            User student = userService.findById(studentId);
            if (student == null) {
                return ResponseResult.failure("学生不存在", 404);
            }

            // 构建完整的学生信息
            Map<String, Object> studentInfo = new HashMap<>();
            studentInfo.put("studentId", student.getId());
            studentInfo.put("realName", student.getName());
            // 从数据库查询学生所在班级名称，避免前端总是显示“未知班级”
            String className = userService.getStudentClassName(studentId);
            if (className == null || className.trim().isEmpty()) {
                className = "未知班级";
            }
            studentInfo.put("className", className);
            
            // 尝试获取学生学习表现数据
            Map<String, Object> performanceData = studentMapper.getStudentPerformance(studentId);
            studentInfo.putAll(performanceData);
            
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
            HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        Long teacherId = getCurrentUserId(session);
        
        // 计算分页参数
        int safePage = (page == null || page < 1) ? 1 : page;
        int safeSize = (size == null || size < 1) ? 10 : size;
        int offset = (safePage - 1) * safeSize;
        
        // 获取总数
        Integer total = courseMapper.countClassAssignments(teacherId, courseId, classId);
        if (total == null) total = 0;
        
        // 获取所有数据（当前SQL不支持LIMIT，需要在内存中分页）
        List<Map<String, Object>> allAssignments = courseMapper.getClassAssignments(teacherId, courseId, classId);
        
        // 内存分页
        int totalPages = (int) Math.ceil((double) total / safeSize);
        totalPages = Math.max(totalPages, 1);
        safePage = Math.min(safePage, totalPages);
        
        List<Map<String, Object>> pagedAssignments = allAssignments.stream()
            .skip(offset)
            .limit(safeSize)
            .collect(java.util.stream.Collectors.toList());
        
        // 构建分页响应
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("content", pagedAssignments);
        result.put("pageNumber", safePage - 1);
        result.put("pageSize", safeSize);
        result.put("totalElements", total);
        result.put("totalPages", totalPages);
        result.put("first", safePage == 1);
        result.put("last", safePage >= totalPages);
        result.put("numberOfElements", pagedAssignments.size());
        result.put("empty", pagedAssignments.isEmpty());
        
        return ResponseResult.success(result, "获取课程分配列表成功", 200);
    }
    
    @GetMapping("/check-class-name")
    public ResponseResult<Map<String, Boolean>> checkClassNameExists(
            @RequestParam String className,
            @RequestParam(required = false) Long classId,
            HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
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
    public ResponseResult<Map<String, Object>> assignCourse(@RequestBody Map<String, Object> assignData, HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        try {
            // 关联已有班级模式（仅支持此模式）
            Long classId = Long.parseLong(assignData.get("classId").toString());
            Long courseId = Long.parseLong(assignData.get("courseId").toString());
            Long teacherId = Long.parseLong(assignData.get("teacherId").toString());
            
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
                assignDataMap.put("teacherId", teacherId);
                assignDataMap.put("classTime", assignData.getOrDefault("classTime", ""));
                assignDataMap.put("classLocation", assignData.getOrDefault("classLocation", ""));
                
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
    public ResponseResult<Void> unassignCourse(@PathVariable Long assignmentId, HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        try {
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
            HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        try {
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
    public ResponseResult<Object> updateStudent(@PathVariable Long studentId, @RequestBody Map<String, Object> studentData, HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        try {
            // 更新学生基本信息
            User student = userService.findById(studentId);
            if (student == null) {
                return ResponseResult.failure("学生不存在", 404);
            }
            
            // 更新学生信息
            if (studentData.containsKey("realName")) {
                student.setName((String) studentData.get("realName"));
            }
            
            // 如果有班级ID，更新学生的班级信息
            if (studentData.containsKey("classId")) {
                Object classIdObj = studentData.get("classId");
                if (classIdObj != null) {
                    String classIdStr = classIdObj.toString().trim();
                    if (!"all".equals(classIdStr) && !classIdStr.isEmpty()) {
                        try {
                            Long classId = Long.parseLong(classIdStr);
                            
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
            
            // 保存更新
            userService.update(student);
            
            // 更新学生学习表现数据
            if (studentData.containsKey("averageScore") || studentData.containsKey("pendingAssignments") || studentData.containsKey("overallProgress")) {
                Double averageScore = studentData.containsKey("averageScore") ? Double.parseDouble(studentData.get("averageScore").toString()) : null;
                Integer pendingAssignments = studentData.containsKey("pendingAssignments") ? Integer.parseInt(studentData.get("pendingAssignments").toString()) : null;
                Integer overallProgress = studentData.containsKey("overallProgress") ? Integer.parseInt(studentData.get("overallProgress").toString()) : null;
                
                // 调用StudentMapper的updateStudentPerformance方法来更新学生学习表现数据
                studentMapper.updateStudentPerformance(studentId, averageScore, pendingAssignments, overallProgress);
            }
            
            return ResponseResult.success(student, "学生信息更新成功", 200);
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
            HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        List<AssignmentSubmission> submissions = assignmentSubmissionService.getSubmissionsWithPagination(
                page, size, sortBy, order, assignmentId, studentId, graded);
        Integer total = assignmentSubmissionService.countSubmissions(assignmentId, studentId, graded);
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("content", submissions);
        result.put("totalElements", total);
        result.put("pageNumber", page);
        result.put("totalPages", (int) Math.ceil((double) total / size));
        result.put("pageSize", size);
        
        return ResponseResult.success(result, "获取作业提交记录成功", 200);
    }
    
    @GetMapping("/submissions/{submissionId}")
    public ResponseResult<AssignmentSubmission> getSubmissionById(@PathVariable Long submissionId, HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        AssignmentSubmission submission = assignmentSubmissionService.getSubmissionById(submissionId);
        if (submission == null) {
            return ResponseResult.failure("作业提交记录不存在", 404);
        }
        
        return ResponseResult.success(submission, "获取作业提交记录详情成功", 200);
    }
    
    @PutMapping("/submissions/{submissionId}/grade")
    public ResponseResult<AssignmentSubmission> gradeSubmission(@PathVariable Long submissionId, @RequestBody Map<String, Object> gradeData, HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
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
}