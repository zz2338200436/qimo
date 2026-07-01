package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.annotation.RequireLogin;
import com._202510007517.major_assignment.constants.RoleConstants;
import com._202510007517.major_assignment.entity.Course;
import com._202510007517.major_assignment.entity.Exam;
import com._202510007517.major_assignment.entity.ExamSubmission;
import com._202510007517.major_assignment.entity.Notification;
import com._202510007517.major_assignment.entity.dto.PageResult;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.service.CourseService;
import com._202510007517.major_assignment.service.AssessmentAttachmentService;
import com._202510007517.major_assignment.service.ExamService;
import com._202510007517.major_assignment.service.ExamSubmissionService;
import com._202510007517.major_assignment.service.NotificationService;
import com._202510007517.major_assignment.service.EarlyWarningAnalysisService;
import com._202510007517.major_assignment.service.KnowledgePointService;
import com._202510007517.major_assignment.utils.LogUtil;
import com._202510007517.major_assignment.utils.PageUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/exams")
@RequireLogin(roles = {RoleConstants.TEACHER})
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
    private AssessmentAttachmentService assessmentAttachmentService;
    
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

        Long teacherId = getCurrentUserId(requestContext);
        List<Course> allCourses = courseService.findByTeacherId(teacherId);
        Long courseId = resolveTeacherCourseId(courseIdStr, allCourses);
        if (courseId == null && courseIdStr != null && !courseIdStr.isEmpty()) {
            LogUtil.logWarning(logger, "无效的课程ID或课程代码: " + courseIdStr, teacherId);
        }
        
        // 将courseId赋值给final变量，用于lambda表达式
        final Long finalCourseId = courseId;
        
        final String normalizedStatus = normalizeExamStatusFilter(status);

        // 获取所有考试
        List<Exam> allExams = examService.getAllExams();
        
        // 获取当前时间
        final Date now = new Date();
        
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
                    if (normalizedStatus != null) {
                        // 使用方法外部定义的now变量，无需重新定义
                        
                        switch (normalizedStatus) {
                            case "upcoming":
                                // 即将开始：开始时间 > 当前时间
                                if (exam.getStartTime() != null && exam.getStartTime().after(now)) {
                                    return true;
                                }
                                return false;
                            case "ongoing":
                                // 进行中：开始时间 <= 当前时间 && 结束时间 >= 当前时间
                                if (exam.getStartTime() != null && exam.getEndTime() != null && 
                                    exam.getStartTime().before(now) && exam.getEndTime().after(now)) {
                                    return true;
                                }
                                return false;
                            case "completed":
                                // 已结束：结束时间 < 当前时间
                                if (exam.getEndTime() != null && exam.getEndTime().before(now)) {
                                    return true;
                                }
                                return false;
                            case "graded":
                                // 已评分：这里简化处理，实际需要查询评分记录
                                // 暂时按已结束处理，因为只有结束的考试才能评分
                                if (exam.getEndTime() != null && exam.getEndTime().before(now)) {
                                    return true;
                                }
                                return false;
                            default:
                                // 未知状态，记录日志但不筛选
                                LogUtil.logWarning(logger, "未知的考试状态: " + normalizedStatus, getCurrentUserId(requestContext));
                                return true;
                        }
                    }
                    return true;
                })
                .collect(java.util.stream.Collectors.toList());
        
        // 计算总数
        Map<String, Object> pageMetadata = buildSpringPageResponseFromInMemoryList(filteredExams, page, size);
        @SuppressWarnings("unchecked")
        List<Exam> pagedExams = (List<Exam>) pageMetadata.get("content");
        
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
        
        int safeSize = (Integer) pageMetadata.get("size");
        int safePage = ((Integer) pageMetadata.get("number")) + 1;
        int totalElements = ((Number) pageMetadata.get("totalElements")).intValue();
        Map<String, Object> result = buildSpringPageResponse(examMaps, safePage, safeSize, totalElements);
        
        LogUtil.logResponse(logger, "GET", "/api/teacher/exams", 200, result, getCurrentUserId(requestContext));
        return ResponseResult.success(result, "获取考试列表成功", 200);
    }
    
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResult<Exam> createExam(@RequestBody Map<String, Object> requestBody, HttpServletRequest requestContext) {
        return createExamInternal(requestBody, null, requestContext);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseResult<Exam> createExamWithFiles(
            @RequestPart("payload") Map<String, Object> requestBody,
            @RequestPart(value = "files", required = false) MultipartFile[] files,
            HttpServletRequest requestContext) {
        return createExamInternal(requestBody, files, requestContext);
    }

    private ResponseResult<Exam> createExamInternal(Map<String, Object> requestBody,
                                                   MultipartFile[] files,
                                                   HttpServletRequest requestContext) {
        LogUtil.logRequest(logger, "POST", "/api/teacher/exams", requestBody, getCurrentUserId(requestContext));
        
        try {
            // 使用session中的teacherId覆盖前端传入的teacherId，确保安全
            Long teacherId = getCurrentUserId(requestContext);
            
            List<Course> allCourses = courseService.findByTeacherId(teacherId);
            
            Object rawCourseValue = requestBody.containsKey("courseId")
                    ? requestBody.get("courseId")
                    : requestBody.get("course_id");
            Long courseId = resolveTeacherCourseIdFromPayload(requestBody, allCourses);
            if (courseId == null && rawCourseValue instanceof String && !((String) rawCourseValue).isBlank()) {
                LogUtil.logError(logger, "创建考试失败 - 无效的课程代码: " + rawCourseValue, null);
                return ResponseResult.failure("创建考试失败 - 无效的课程代码", 400);
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
            
            String startTimeStr = (String) requestBody.getOrDefault("startTime", requestBody.get("start_time"));
            if (startTimeStr != null) {
                exam.setStartTime(parseExamDateTime(startTimeStr));
            }
            
            String endTimeStr = (String) requestBody.getOrDefault("endTime", requestBody.get("end_time"));
            if (endTimeStr != null) {
                exam.setEndTime(parseExamDateTime(endTimeStr));
            }
            
            String publishDateStr = (String) requestBody.getOrDefault("publishDate", requestBody.get("publish_date"));
            if (publishDateStr != null) {
                exam.setPublishDate(parseExamDateTime(publishDateStr));
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
            assessmentAttachmentService.saveAttachments(
                    AssessmentAttachmentService.EXAM_TYPE,
                    exam.getId(),
                    teacherId,
                    files);
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
        
        try {
            Long teacherId = getCurrentUserId(requestContext);
            List<Course> allCourses = courseService.findByTeacherId(teacherId);
            
            Object rawCourseValue = requestBody.containsKey("courseId")
                    ? requestBody.get("courseId")
                    : requestBody.get("course_id");
            Long courseId = resolveTeacherCourseIdFromPayload(requestBody, allCourses);
            if (courseId == null && rawCourseValue instanceof String && !((String) rawCourseValue).isBlank()) {
                LogUtil.logError(logger, "更新考试失败 - 无效的课程代码: " + rawCourseValue, null);
                return ResponseResult.failure("更新考试失败 - 无效的课程代码", 400);
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
            
            String startTimeStr = (String) requestBody.getOrDefault("startTime", requestBody.get("start_time"));
            if (startTimeStr != null) {
                existingExam.setStartTime(parseExamDateTime(startTimeStr));
            }
            
            String endTimeStr = (String) requestBody.getOrDefault("endTime", requestBody.get("end_time"));
            if (endTimeStr != null) {
                existingExam.setEndTime(parseExamDateTime(endTimeStr));
            }
            
            String publishDateStr = (String) requestBody.getOrDefault("publishDate", requestBody.get("publish_date"));
            if (publishDateStr != null) {
                existingExam.setPublishDate(parseExamDateTime(publishDateStr));
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

    private Date parseExamDateTime(String value) throws ParseException {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.endsWith("Z") || normalized.matches(".*[+-]\\d{2}:?\\d{2}$")) {
            ParseException lastFailure = null;
            for (String pattern : List.of("yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssX", "yyyy-MM-dd'T'HH:mm:ssXXX")) {
                try {
                    SimpleDateFormat isoFormat = new SimpleDateFormat(pattern);
                    isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    return isoFormat.parse(normalized);
                } catch (ParseException e) {
                    lastFailure = e;
                }
            }
            throw lastFailure;
        }

        String localDateTime = normalized.replace('T', ' ');
        if (localDateTime.length() == 16) {
            localDateTime += ":00";
        }
        SimpleDateFormat localFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return localFormat.parse(localDateTime);
    }
    
    @GetMapping("/{id}")
    public ResponseResult<Map<String, Object>> getExamById(@PathVariable Long id, HttpServletRequest requestContext) {
        LogUtil.logRequest(logger, "GET", "/api/teacher/exams/" + id, null, getCurrentUserId(requestContext));
        
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
        
        Integer total = examSubmissionService.countSubmissions(examId, studentId, graded);
        PageUtils.PageWindow window = resolvePageWindow(page, size, total);
        List<ExamSubmission> submissions = examSubmissionService.getSubmissionsWithPagination(
                window.page(), window.size(), total, sortBy, order, examId, studentId, graded);

        Map<String, Object> result = buildSpringPageResponse(submissions, window.page(), window.size(), total);

        LogUtil.logResponse(logger, "GET", "/api/teacher/exams/submissions", 200, result, getCurrentUserId(requestContext));
        return ResponseResult.success(result, "获取考试提交记录成功", 200);
    }
    
    @GetMapping("/submissions/{submissionId}")
    public ResponseResult<ExamSubmission> getSubmissionById(@PathVariable Long submissionId, HttpServletRequest requestContext) {
        LogUtil.logRequest(logger, "GET", "/api/teacher/exams/submissions/" + submissionId, null, getCurrentUserId(requestContext));
        
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
