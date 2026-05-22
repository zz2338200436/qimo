package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.entity.Course;
import com._202510007517.major_assignment.entity.Exam;
import com._202510007517.major_assignment.entity.ExamSubmission;
import com._202510007517.major_assignment.entity.Notification;
import com._202510007517.major_assignment.entity.dto.PageResult;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.service.CourseService;
import com._202510007517.major_assignment.service.ExamService;
import com._202510007517.major_assignment.service.ExamSubmissionService;
import com._202510007517.major_assignment.service.NotificationService;
import com._202510007517.major_assignment.service.EarlyWarningAnalysisService;
import com._202510007517.major_assignment.service.KnowledgePointService;
import com._202510007517.major_assignment.utils.LogUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/exams")
public class ExamController extends BaseController {
    
    private static final Logger logger = LogUtil.getLogger(ExamController.class);
    
    @Autowired
    private ExamService examService;
    
    @Autowired
    private ExamSubmissionService examSubmissionService;
    
    @Autowired
    private CourseService courseService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private EarlyWarningAnalysisService earlyWarningAnalysisService;
    
    @Autowired
    private KnowledgePointService knowledgePointService;
    
    @Autowired
    private com._202510007517.major_assignment.mapper.ExamMapper examMapper;
    
    @GetMapping
    public ResponseResult<Map<String, Object>> getExams(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(value = "order", defaultValue = "DESC") String order,
            @RequestParam(value = "courseId", required = false) String courseIdStr,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "isOnline", required = false) Boolean isOnline,
            HttpServletRequest requestContext) {
        LogUtil.logRequest(logger, "GET", "/api/teacher/exams", null, getCurrentUserId(requestContext));
        
        if (!isLoggedIn(requestContext)) {
            LogUtil.logWarning(logger, "未授权访问考试列表", getCurrentUserId(requestContext));
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        // 转换courseIdStr为Long类型，支持字符串ID和数字ID
        Long courseId = null;
        if (courseIdStr != null && !courseIdStr.isEmpty()) {
            try {
                // 尝试直接转换为Long
                courseId = Long.parseLong(courseIdStr);
            } catch (NumberFormatException e) {
                // 如果转换失败，尝试将其作为课程代码处理
                LogUtil.logDebug(logger, "尝试将课程代码 " + courseIdStr + " 转换为课程ID", getCurrentUserId(requestContext));
                // 获取当前教师的所有课程
                List<Course> allCourses = courseService.findByTeacherId(getCurrentUserId(requestContext));
                // 根据课程代码查找匹配的课程
                for (Course course : allCourses) {
                    if (course.getCourseCode() != null && course.getCourseCode().equals(courseIdStr)) {
                        courseId = course.getId();
                        LogUtil.logDebug(logger, "找到课程代码 " + courseIdStr + " 对应的课程ID: " + courseId, getCurrentUserId(requestContext));
                        break;
                    }
                }
                if (courseId == null) {
                    LogUtil.logWarning(logger, "无效的课程ID或课程代码: " + courseIdStr, getCurrentUserId(requestContext));
                }
            }
        }
        
        // 将courseId赋值给final变量，用于lambda表达式
        final Long finalCourseId = courseId;
        
        // 获取所有考试
        List<Exam> allExams = examService.getAllExams();
        
        // 获取当前时间
        final Date now = new Date();
        
        // 获取当前教师ID
        Long teacherId = getCurrentUserId(requestContext);
        
        // 统计每个考试的提交数量和预期参与学生数
        Map<Long, Integer> submissionCountMap = new java.util.HashMap<>();
        Map<Long, Integer> totalStudentsMap = new java.util.HashMap<>();
        
        for (Exam exam : allExams) {
            // 统计提交数量
            Integer submittedCount = examMapper.countSubmissionsByExamId(exam.getId());
            submissionCountMap.put(exam.getId(), submittedCount != null ? submittedCount : 0);
            
            // 统计预期参与学生数
            List<Long> classIds = examMapper.getExamClasses(exam.getId());
            if (classIds != null && !classIds.isEmpty()) {
                int totalStudents = 0;
                for (Long classId : classIds) {
                    Integer classStudentCount = examMapper.countStudentsByClassId(classId);
                    if (classStudentCount != null) {
                        totalStudents += classStudentCount;
                    }
                }
                totalStudentsMap.put(exam.getId(), totalStudents);
            } else {
                // 如果没有分配班级，则通过课程关联的班级来统计
                Integer courseStudentCount = examMapper.countStudentsByCourseId(exam.getCourseId());
                totalStudentsMap.put(exam.getId(), courseStudentCount != null ? courseStudentCount : 0);
            }
        }
        
        // 为每个考试设置统计信息
        for (Exam exam : allExams) {
            exam.setSubmittedCount(submissionCountMap.getOrDefault(exam.getId(), 0));
            exam.setTotalStudents(totalStudentsMap.getOrDefault(exam.getId(), 0));
        }
        
        // 应用筛选条件
        List<Exam> filteredExams = allExams.stream()
                .filter(exam -> {
                    // 课程ID筛选
                    if (finalCourseId != null) {
                        if (!exam.getCourseId().equals(finalCourseId)) {
                            return false;
                        }
                    }
                    // 在线状态筛选
                    if (isOnline != null) {
                        if (exam.getIsOnline() != isOnline) {
                            return false;
                        }
                    }
                    // 考试状态筛选
                    if (status != null) {
                        // 使用方法外部定义的now变量，无需重新定义
                        
                        switch (status) {
                            case "即将开始":
                            case "upcoming":
                                // 即将开始：开始时间 > 当前时间
                                if (exam.getStartTime() != null && exam.getStartTime().after(now)) {
                                    return true;
                                }
                                return false;
                            case "进行中":
                            case "ongoing":
                                // 进行中：开始时间 <= 当前时间 && 结束时间 >= 当前时间
                                if (exam.getStartTime() != null && exam.getEndTime() != null && 
                                    exam.getStartTime().before(now) && exam.getEndTime().after(now)) {
                                    return true;
                                }
                                return false;
                            case "已结束":
                            case "completed":
                                // 已结束：结束时间 < 当前时间
                                if (exam.getEndTime() != null && exam.getEndTime().before(now)) {
                                    return true;
                                }
                                return false;
                            case "已评分":
                            case "graded":
                                // 已评分：这里简化处理，实际需要查询评分记录
                                // 暂时按已结束处理，因为只有结束的考试才能评分
                                if (exam.getEndTime() != null && exam.getEndTime().before(now)) {
                                    return true;
                                }
                                return false;
                            default:
                                // 未知状态，记录日志但不筛选
                                LogUtil.logWarning(logger, "未知的考试状态: " + status, getCurrentUserId(requestContext));
                                return true;
                        }
                    }
                    return true;
                })
                .collect(java.util.stream.Collectors.toList());
        
        // 计算总数
        int totalElements = filteredExams.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        // 应用分页
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, totalElements);
        // 将SubList转换为普通ArrayList，避免Redis反序列化错误
        List<Exam> pagedExams = new ArrayList<>(filteredExams.subList(startIndex, endIndex));
        
        // 构建课程ID到课程名称的映射
        Map<Long, String> courseNameMap = new java.util.HashMap<>();
        for (Exam exam : pagedExams) {
            if (exam.getCourseId() != null && !courseNameMap.containsKey(exam.getCourseId())) {
                Course course = courseService.findById(exam.getCourseId());
                if (course != null) {
                    courseNameMap.put(exam.getCourseId(), course.getCourseName());
                }
            }
        }
        
        // 将考试列表转换为包含课程名称的Map列表
        List<Map<String, Object>> examMaps = new ArrayList<>();
        for (Exam exam : pagedExams) {
            Map<String, Object> examMap = new java.util.HashMap<>();
            examMap.put("id", exam.getId());
            examMap.put("title", exam.getTitle());
            examMap.put("description", exam.getDescription());
            examMap.put("courseId", exam.getCourseId());
            examMap.put("courseName", courseNameMap.getOrDefault(exam.getCourseId(), "未知课程"));
            examMap.put("startTime", exam.getStartTime());
            examMap.put("endTime", exam.getEndTime());
            examMap.put("publishDate", exam.getPublishDate());
            examMap.put("isActive", exam.getIsActive());
            examMap.put("isOnline", exam.getIsOnline());
            examMap.put("location", exam.getLocation());
            examMap.put("duration", exam.getDuration());
            examMap.put("teacherId", exam.getTeacherId());
            examMap.put("submittedCount", exam.getSubmittedCount());
            examMap.put("totalStudents", exam.getTotalStudents());
            examMaps.add(examMap);
        }
        
        // 构建分页响应
        Map<String, Object> result = new java.util.HashMap<>();
        
        // 构建content
        result.put("content", examMaps);
        
        // 构建pageable
        Map<String, Object> pageable = new java.util.HashMap<>();
        pageable.put("pageNumber", page - 1); // 前端从1开始，后端从0开始
        pageable.put("pageSize", size);
        
        // 构建sort
        Map<String, Object> sort = new java.util.HashMap<>();
        sort.put("empty", false);
        sort.put("sorted", true);
        sort.put("unsorted", false);
        pageable.put("sort", sort);
        
        pageable.put("offset", startIndex);
        pageable.put("paged", true);
        pageable.put("unpaged", false);
        
        result.put("pageable", pageable);
        
        // 其他分页字段
        result.put("totalPages", totalPages);
        result.put("totalElements", totalElements);
        result.put("last", page >= totalPages);
        result.put("size", size);
        result.put("number", page - 1); // 前端从1开始，后端从0开始
        result.put("sort", sort);
        result.put("first", page == 1);
        result.put("numberOfElements", pagedExams.size());
        result.put("empty", pagedExams.isEmpty());
        
        LogUtil.logResponse(logger, "GET", "/api/teacher/exams", 200, result, getCurrentUserId(requestContext));
        return ResponseResult.success(result, "获取考试列表成功", 200);
    }
    
    @PostMapping
    public ResponseResult<Exam> createExam(@RequestBody Map<String, Object> requestBody, HttpServletRequest requestContext) {
        LogUtil.logRequest(logger, "POST", "/api/teacher/exams", requestBody, getCurrentUserId(requestContext));
        
        if (!isLoggedIn(requestContext)) {
            LogUtil.logWarning(logger, "未授权创建考试", getCurrentUserId(requestContext));
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        try {
            // 使用session中的teacherId覆盖前端传入的teacherId，确保安全
            Long teacherId = getCurrentUserId(requestContext);
            
            // 获取当前教师的所有课程，用于课程代码转换
            List<Course> allCourses = courseService.findByTeacherId(teacherId);
            
            // 处理课程ID，支持字符串课程代码
            Object courseIdObj = requestBody.getOrDefault("courseId", requestBody.get("course_id"));
            Long courseId = null;
            
            if (courseIdObj != null) {
                if (courseIdObj instanceof String) {
                    // 如果是字符串，尝试转换为Long，或者查找对应的课程ID
                    String courseIdStr = (String) courseIdObj;
                    try {
                        // 尝试直接转换为Long
                        courseId = Long.parseLong(courseIdStr);
                    } catch (NumberFormatException e) {
                        // 如果转换失败，尝试作为课程代码查找对应的课程ID
                        for (Course course : allCourses) {
                            if (course.getCourseCode() != null && course.getCourseCode().equals(courseIdStr)) {
                                courseId = course.getId();
                                break;
                            }
                        }
                        
                        if (courseId == null) {
                            LogUtil.logError(logger, "创建考试失败 - 无效的课程代码: " + courseIdStr, null);
                            return ResponseResult.failure("创建考试失败 - 无效的课程代码", 400);
                        }
                    }
                } else if (courseIdObj instanceof Number) {
                    // 如果是数字，直接转换为Long
                    courseId = ((Number) courseIdObj).longValue();
                }
            }
            
            if (courseId == null) {
                LogUtil.logError(logger, "创建考试失败 - 课程ID不能为空", null);
                return ResponseResult.failure("创建考试失败 - 课程ID不能为空", 400);
            }
            
            // 创建Exam对象
            Exam exam = new Exam();
            exam.setTitle((String) requestBody.get("title"));
            exam.setDescription((String) requestBody.get("description"));
            exam.setCourseId(courseId);
            
            // 处理日期字符串，转换为Date对象
            // ISO格式字符串（如：2025-12-24T08:00:00.000Z）需要设置为UTC时区
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            
            String startTimeStr = (String) requestBody.getOrDefault("startTime", requestBody.get("start_time"));
            if (startTimeStr != null) {
                exam.setStartTime(sdf.parse(startTimeStr));
            }
            
            String endTimeStr = (String) requestBody.getOrDefault("endTime", requestBody.get("end_time"));
            if (endTimeStr != null) {
                exam.setEndTime(sdf.parse(endTimeStr));
            }
            
            String publishDateStr = (String) requestBody.getOrDefault("publishDate", requestBody.get("publish_date"));
            if (publishDateStr != null) {
                exam.setPublishDate(sdf.parse(publishDateStr));
            }
            
            // 处理布尔值
            Boolean isActive = (Boolean) requestBody.getOrDefault("isActive", requestBody.get("is_active"));
            exam.setIsActive(isActive != null ? isActive : true);
            
            Boolean isOnline = (Boolean) requestBody.getOrDefault("isOnline", requestBody.get("is_online"));
            exam.setIsOnline(isOnline != null ? isOnline : true);
            
            exam.setLocation((String) requestBody.getOrDefault("location", ""));
            
            // 处理时长
            Object durationObj = requestBody.get("duration");
            if (durationObj instanceof Number) {
                exam.setDuration(((Number) durationObj).longValue());
            } else if (durationObj instanceof String) {
                exam.setDuration(Long.parseLong((String) durationObj));
            }
            
            exam.setTeacherId(teacherId);
            
            // 保存考试
            examService.create(exam);
            LogUtil.logOperation(logger, "创建考试", "考试标题: " + exam.getTitle(), getCurrentUserId(requestContext), true);
            
            // 处理知识点关联
            @SuppressWarnings("unchecked")
            List<Number> knowledgePointIdsRaw = (List<Number>) requestBody.get("knowledgePointIds");
            if (knowledgePointIdsRaw != null && !knowledgePointIdsRaw.isEmpty()) {
                List<Long> knowledgePointIds = knowledgePointIdsRaw.stream()
                    .map(Number::longValue)
                    .toList();
                knowledgePointService.setExamKnowledgePoints(exam.getId(), knowledgePointIds);
                logger.info("考试 {} 关联了 {} 个知识点: {}", exam.getId(), knowledgePointIds.size(), knowledgePointIds);
            }
            
            // 发送考试通知给所有选修该课程的学生
            try {
                // 获取该课程的所有学生ID
                List<Long> studentIds = courseService.getStudentIdsByCourseId(exam.getCourseId());
                logger.info("获取到课程 {} 的学生ID列表: {}", exam.getCourseId(), studentIds);
                
                // 创建通知列表
                List<Notification> notifications = new ArrayList<>();
                for (Long studentId : studentIds) {
                    Notification notification = new Notification();
                    notification.setStudentId(studentId);
                    notification.setTeacherId(exam.getTeacherId());
                    notification.setType("exam");
                    notification.setTitle("新考试发布");
                    notification.setContent("您有一场新考试：" + exam.getTitle() + "，请及时查看");
                    notification.setRelatedId(exam.getId());
                    notifications.add(notification);
                }
                
                // 批量发送通知
                if (!notifications.isEmpty()) {
                    notificationService.createBatch(notifications);
                    logger.info("成功发送 {} 条考试通知", notifications.size());
                }
            } catch (Exception e) {
                logger.error("发送考试通知失败: {}", e.getMessage(), e);
                // 发送通知失败不影响考试发布，继续执行
            }
            
            LogUtil.logResponse(logger, "POST", "/api/teacher/exams", 201, exam, getCurrentUserId(requestContext));
            return ResponseResult.created(exam);
        } catch (ParseException e) {
            LogUtil.logError(logger, "创建考试失败 - 日期格式错误", e);
            return ResponseResult.failure("创建考试失败 - 日期格式错误", 400);
        } catch (Exception e) {
            LogUtil.logError(logger, "创建考试失败", e);
            return ResponseResult.failure("创建考试失败", 500);
        }
    }
    
    @PutMapping("/{id}")
    public ResponseResult<Exam> updateExam(@PathVariable Long id, @RequestBody Map<String, Object> requestBody, HttpServletRequest requestContext) {
        LogUtil.logRequest(logger, "PUT", "/api/teacher/exams/" + id, requestBody, getCurrentUserId(requestContext));
        
        if (!isLoggedIn(requestContext)) {
            LogUtil.logWarning(logger, "未授权更新考试", getCurrentUserId(requestContext));
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        try {
            // 获取当前教师的所有课程，用于课程代码转换
            Long teacherId = getCurrentUserId(requestContext);
            List<Course> allCourses = courseService.findByTeacherId(teacherId);
            
            // 处理课程ID，支持字符串课程代码
            Object courseIdObj = requestBody.getOrDefault("courseId", requestBody.get("course_id"));
            Long courseId = null;
            
            if (courseIdObj != null) {
                if (courseIdObj instanceof String) {
                    // 如果是字符串，尝试转换为Long，或者查找对应的课程ID
                    String courseIdStr = (String) courseIdObj;
                    try {
                        // 尝试直接转换为Long
                        courseId = Long.parseLong(courseIdStr);
                    } catch (NumberFormatException e) {
                        // 如果转换失败，尝试作为课程代码查找对应的课程ID
                        for (Course course : allCourses) {
                            if (course.getCourseCode() != null && course.getCourseCode().equals(courseIdStr)) {
                                courseId = course.getId();
                                break;
                            }
                        }
                        
                        if (courseId == null) {
                            LogUtil.logError(logger, "更新考试失败 - 无效的课程代码: " + courseIdStr, null);
                            return ResponseResult.failure("更新考试失败 - 无效的课程代码", 400);
                        }
                    }
                } else if (courseIdObj instanceof Number) {
                    // 如果是数字，直接转换为Long
                    courseId = ((Number) courseIdObj).longValue();
                }
            }
            
            if (courseId == null) {
                LogUtil.logError(logger, "更新考试失败 - 课程ID不能为空", null);
                return ResponseResult.failure("更新考试失败 - 课程ID不能为空", 400);
            }
            
            // 获取现有考试
            Exam existingExam = examService.getExamById(id);
            if (existingExam == null) {
                return ResponseResult.failure("考试不存在", 404);
            }
            
            // 更新基本信息
            existingExam.setTitle((String) requestBody.get("title"));
            existingExam.setDescription((String) requestBody.get("description"));
            existingExam.setCourseId(courseId);
            existingExam.setTeacherId(teacherId);
            
            // 处理日期字符串，转换为Date对象
            // ISO格式字符串（如：2025-12-24T08:00:00.000Z）需要设置为UTC时区
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            
            String startTimeStr = (String) requestBody.getOrDefault("startTime", requestBody.get("start_time"));
            if (startTimeStr != null) {
                existingExam.setStartTime(sdf.parse(startTimeStr));
            }
            
            String endTimeStr = (String) requestBody.getOrDefault("endTime", requestBody.get("end_time"));
            if (endTimeStr != null) {
                existingExam.setEndTime(sdf.parse(endTimeStr));
            }
            
            String publishDateStr = (String) requestBody.getOrDefault("publishDate", requestBody.get("publish_date"));
            if (publishDateStr != null) {
                existingExam.setPublishDate(sdf.parse(publishDateStr));
            }
            
            // 处理布尔值
            Boolean isActive = (Boolean) requestBody.getOrDefault("isActive", requestBody.get("is_active"));
            existingExam.setIsActive(isActive != null ? isActive : true);
            
            Boolean isOnline = (Boolean) requestBody.getOrDefault("isOnline", requestBody.get("is_online"));
            existingExam.setIsOnline(isOnline != null ? isOnline : true);
            
            existingExam.setLocation((String) requestBody.getOrDefault("location", ""));
            
            // 处理时长
            Object durationObj = requestBody.get("duration");
            if (durationObj instanceof Number) {
                existingExam.setDuration(((Number) durationObj).longValue());
            } else if (durationObj instanceof String) {
                existingExam.setDuration(Long.parseLong((String) durationObj));
            }
            
            // 保存更新
            examService.update(existingExam);
            LogUtil.logOperation(logger, "更新考试", "考试ID: " + id + ", 考试标题: " + existingExam.getTitle(), getCurrentUserId(requestContext), true);
            LogUtil.logResponse(logger, "PUT", "/api/teacher/exams/" + id, 200, existingExam, getCurrentUserId(requestContext));
            return ResponseResult.success(existingExam);
        } catch (ParseException e) {
            LogUtil.logError(logger, "更新考试失败 - 日期格式错误", e);
            return ResponseResult.failure("更新考试失败 - 日期格式错误", 400);
        } catch (Exception e) {
            LogUtil.logError(logger, "更新考试失败，考试ID: " + id, e);
            return ResponseResult.failure("更新考试失败", 500);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseResult<Void> deleteExam(@PathVariable Long id, HttpServletRequest requestContext) {
        LogUtil.logRequest(logger, "DELETE", "/api/teacher/exams/" + id, null, getCurrentUserId(requestContext));
        
        if (!isLoggedIn(requestContext)) {
            LogUtil.logWarning(logger, "未授权删除考试", getCurrentUserId(requestContext));
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        try {
            examService.delete(id);
            LogUtil.logOperation(logger, "删除考试", "考试ID: " + id, getCurrentUserId(requestContext), true);
            LogUtil.logResponse(logger, "DELETE", "/api/teacher/exams/" + id, 204, null, getCurrentUserId(requestContext));
            return ResponseResult.noContent();
        } catch (Exception e) {
            LogUtil.logError(logger, "删除考试失败，考试ID: " + id, e);
            return ResponseResult.failure("删除考试失败", 500);
        }
    }
    
    @GetMapping("/{id}")
    public ResponseResult<Map<String, Object>> getExamById(@PathVariable Long id, HttpServletRequest requestContext) {
        LogUtil.logRequest(logger, "GET", "/api/teacher/exams/" + id, null, getCurrentUserId(requestContext));
        
        if (!isLoggedIn(requestContext)) {
            LogUtil.logWarning(logger, "未授权访问考试详情", getCurrentUserId(requestContext));
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        Map<String, Object> examDetails = examService.getExamDetailsWithSubmissions(id);
        if (examDetails == null) {
            return ResponseResult.failure("考试不存在", 404);
        }
        
        LogUtil.logResponse(logger, "GET", "/api/teacher/exams/" + id, 200, examDetails, getCurrentUserId(requestContext));
        return ResponseResult.success(examDetails, "获取考试详情成功", 200);
    }
    
    @PutMapping("/grade/{submissionId}")
    public ResponseResult<ExamSubmission> gradeExam(@PathVariable Long submissionId, 
                                                 @RequestBody Map<String, Object> gradeRequest, 
                                                 HttpServletRequest requestContext) {
        LogUtil.logRequest(logger, "PUT", "/api/teacher/exams/grade/" + submissionId, gradeRequest, getCurrentUserId(requestContext));
        
        if (!isLoggedIn(requestContext)) {
            LogUtil.logWarning(logger, "未授权批改考试", getCurrentUserId(requestContext));
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        try {
            // 处理score的类型转换，支持Integer、Long、String等多种类型
            Integer score = null;
            Object scoreObj = gradeRequest.get("score");
            if (scoreObj != null) {
                if (scoreObj instanceof Integer) {
                    score = (Integer) scoreObj;
                } else if (scoreObj instanceof Number) {
                    score = ((Number) scoreObj).intValue();
                } else if (scoreObj instanceof String) {
                    try {
                        score = Integer.parseInt((String) scoreObj);
                    } catch (NumberFormatException e) {
                        LogUtil.logError(logger, "批改考试失败 - 无效的分数格式: " + scoreObj, e);
                        return ResponseResult.failure("批改考试失败 - 无效的分数格式", 400);
                    }
                }
            }
            
            String teacherComment = gradeRequest.get("teacherComment") != null ? 
                gradeRequest.get("teacherComment").toString() : "";
            
            if (score == null) {
                LogUtil.logError(logger, "批改考试失败 - 分数不能为空", null);
                return ResponseResult.failure("批改考试失败 - 分数不能为空", 400);
            }
            
            if (score < 0 || score > 100) {
                LogUtil.logError(logger, "批改考试失败 - 分数必须在0-100之间: " + score, null);
                return ResponseResult.failure("批改考试失败 - 分数必须在0-100之间", 400);
            }
            
            ExamSubmission submission = examSubmissionService.gradeExam(submissionId, score, teacherComment);
            if (submission != null) {
                // 实时触发学情分析
                try {
                    // 异步执行分析，避免阻塞批改操作
                    new Thread(() -> {
                        try {
                            // 获取考试信息
                            Map<String, Object> examDetails = examService.getExamDetailsWithSubmissions(submission.getExamId());
                            if (examDetails != null) {
                                Exam exam = (Exam) examDetails.get("exam");
                                if (exam != null) {
                                    // 触发该学生该课程的实时分析
                                    earlyWarningAnalysisService.analyzeStudentWarningsRealtime(submission.getStudentId(), exam.getCourseId());
                                }
                            }
                        } catch (Exception e) {
                            logger.error("实时学情分析失败", e);
                        }
                    }).start();
                } catch (Exception e) {
                    logger.warn("启动实时学情分析失败，但不影响批改操作", e);
                }
                
                LogUtil.logOperation(logger, "批改考试", "提交ID: " + submissionId, getCurrentUserId(requestContext), true);
                LogUtil.logResponse(logger, "PUT", "/api/teacher/exams/grade/" + submissionId, 200, submission, getCurrentUserId(requestContext));
                return ResponseResult.success(submission, "考试批改成功", 200);
            } else {
                return ResponseResult.failure("批改失败，提交记录不存在", 404);
            }
        } catch (Exception e) {
            LogUtil.logError(logger, "批改考试失败，提交ID: " + submissionId, e);
            return ResponseResult.failure("批改考试失败", 500);
        }
    }
    
    @GetMapping("/{examId}/submissions")
    public ResponseResult<List<ExamSubmission>> getExamSubmissions(@PathVariable Long examId, HttpServletRequest requestContext) {
        LogUtil.logRequest(logger, "GET", "/api/teacher/exams/" + examId + "/submissions", null, getCurrentUserId(requestContext));
        
        if (!isLoggedIn(requestContext)) {
            LogUtil.logWarning(logger, "未授权访问考试提交列表", getCurrentUserId(requestContext));
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        List<ExamSubmission> submissions = examSubmissionService.getSubmissionsByExamId(examId);
        
        LogUtil.logResponse(logger, "GET", "/api/teacher/exams/" + examId + "/submissions", 200, submissions, getCurrentUserId(requestContext));
        return ResponseResult.success(submissions, "获取考试提交列表成功", 200);
    }
    
    // 考试提交记录管理接口
    
    @GetMapping("/submissions")
    public ResponseResult<Map<String, Object>> getAllSubmissions(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(value = "order", defaultValue = "DESC") String order,
            @RequestParam(value = "examId", required = false) Long examId,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestParam(value = "graded", required = false) Boolean graded,
            HttpServletRequest requestContext) {
        LogUtil.logRequest(logger, "GET", "/api/teacher/exams/submissions", null, getCurrentUserId(requestContext));
        
        if (!isLoggedIn(requestContext)) {
            LogUtil.logWarning(logger, "未授权访问考试提交记录", getCurrentUserId(requestContext));
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        List<ExamSubmission> submissions = examSubmissionService.getSubmissionsWithPagination(
                page, size, sortBy, order, examId, studentId, graded);
        Integer total = examSubmissionService.countSubmissions(examId, studentId, graded);
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("submissions", submissions);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("pages", (int) Math.ceil((double) total / size));
        
        LogUtil.logResponse(logger, "GET", "/api/teacher/exams/submissions", 200, result, getCurrentUserId(requestContext));
        return ResponseResult.success(result, "获取考试提交记录成功", 200);
    }
    
    @GetMapping("/submissions/{submissionId}")
    public ResponseResult<ExamSubmission> getSubmissionById(@PathVariable Long submissionId, HttpServletRequest requestContext) {
        LogUtil.logRequest(logger, "GET", "/api/teacher/exams/submissions/" + submissionId, null, getCurrentUserId(requestContext));
        
        if (!isLoggedIn(requestContext)) {
            LogUtil.logWarning(logger, "未授权访问考试提交记录详情", getCurrentUserId(requestContext));
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        ExamSubmission submission = examSubmissionService.getSubmissionById(submissionId);
        if (submission == null) {
            return ResponseResult.failure("考试提交记录不存在", 404);
        }
        
        LogUtil.logResponse(logger, "GET", "/api/teacher/exams/submissions/" + submissionId, 200, submission, getCurrentUserId(requestContext));
        return ResponseResult.success(submission, "获取考试提交记录详情成功", 200);
    }
    
    @PutMapping("/submissions/{submissionId}")
    public ResponseResult<ExamSubmission> updateSubmission(@PathVariable Long submissionId, @RequestBody ExamSubmission submission, HttpServletRequest requestContext) {
        LogUtil.logRequest(logger, "PUT", "/api/teacher/exams/submissions/" + submissionId, submission, getCurrentUserId(requestContext));
        
        if (!isLoggedIn(requestContext)) {
            LogUtil.logWarning(logger, "未授权更新考试提交记录", getCurrentUserId(requestContext));
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        submission.setId(submissionId);
        boolean success = examSubmissionService.updateSubmission(submission);
        if (success) {
            LogUtil.logOperation(logger, "更新考试提交记录", "提交ID: " + submissionId, getCurrentUserId(requestContext), true);
            LogUtil.logResponse(logger, "PUT", "/api/teacher/exams/submissions/" + submissionId, 200, submission, getCurrentUserId(requestContext));
            return ResponseResult.success(submission, "更新考试提交记录成功", 200);
        } else {
            return ResponseResult.failure("更新考试提交记录失败", 500);
        }
    }
    
    @DeleteMapping("/submissions/{submissionId}")
    public ResponseResult<Void> deleteSubmission(@PathVariable Long submissionId, HttpServletRequest requestContext) {
        LogUtil.logRequest(logger, "DELETE", "/api/teacher/exams/submissions/" + submissionId, null, getCurrentUserId(requestContext));
        
        if (!isLoggedIn(requestContext)) {
            LogUtil.logWarning(logger, "未授权删除考试提交记录", getCurrentUserId(requestContext));
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        boolean success = examSubmissionService.deleteSubmission(submissionId);
        if (success) {
            LogUtil.logOperation(logger, "删除考试提交记录", "提交ID: " + submissionId, getCurrentUserId(requestContext), true);
            LogUtil.logResponse(logger, "DELETE", "/api/teacher/exams/submissions/" + submissionId, 204, null, getCurrentUserId(requestContext));
            return ResponseResult.noContent();
        } else {
            return ResponseResult.failure("删除考试提交记录失败", 500);
        }
    }
}
