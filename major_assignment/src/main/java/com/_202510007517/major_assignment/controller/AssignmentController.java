package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.constants.CacheConstants;
import com._202510007517.major_assignment.entity.Assignment;
import com._202510007517.major_assignment.entity.AssignmentSubmission;
import com._202510007517.major_assignment.entity.Course;
import com._202510007517.major_assignment.entity.Notification;
import com._202510007517.major_assignment.entity.dto.PageResult;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.mapper.AssignmentMapper;
import com._202510007517.major_assignment.service.AssignmentService;
import com._202510007517.major_assignment.service.AssignmentSubmissionService;
import com._202510007517.major_assignment.service.CourseService;
import com._202510007517.major_assignment.service.NotificationService;
import com._202510007517.major_assignment.service.EarlyWarningAnalysisService;
import com._202510007517.major_assignment.service.KnowledgePointService;
import com._202510007517.major_assignment.utils.LogUtil;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/teacher/assignments")
public class AssignmentController extends BaseController {
    
    private static final Logger logger = LogUtil.getLogger(AssignmentController.class);
    
    @Autowired
    private AssignmentService assignmentService;
    
    @Autowired
    private AssignmentSubmissionService assignmentSubmissionService;
    
    @Autowired
    private AssignmentMapper assignmentMapper;
    
    @Autowired
    private CourseService courseService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private EarlyWarningAnalysisService earlyWarningAnalysisService;
    
    @Autowired
    private KnowledgePointService knowledgePointService;
    
    @GetMapping
    // 暂时移除缓存，确保作业列表实时更新
    // @Cacheable(value = "assignments", key = "#session.getAttribute('userId') + '_' + #page + '_' + #size + '_' + #keyword + '_' + #courseIdStr + '_' + #status + '_' + #classId")
    public ResponseResult<Map<String, Object>> getAssignments(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(value = "order", defaultValue = "DESC") String order,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "courseId", required = false) String courseIdStr,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "submitted", required = false) Boolean submitted,
            @RequestParam(value = "classId", required = false) Long classId,
            HttpSession session) {
        LogUtil.logRequest(logger, "GET", "/api/teacher/assignments", null, getCurrentUserId(session));
        
        if (!isLoggedIn(session)) {
            LogUtil.logWarning(logger, "未授权访问作业列表", getCurrentUserId(session));
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        // 获取当前用户ID，教师ID
        Long teacherId = getCurrentUserId(session);
        
        // 根据前端传递的status参数计算对应的isActive值
        Boolean statusBasedIsActive = null;
        if (status != null) {
            switch (status) {
                case "pending":
                case "submitted":
                case "graded":
                    statusBasedIsActive = true;
                    break;
                case "closed":
                    statusBasedIsActive = false;
                    break;
                default:
                    // 对于其他status值，不设置statusBasedIsActive
                    break;
            }
        }
        
        // 确定最终的isActive过滤条件：如果status参数提供了值，则使用statusBasedIsActive，否则使用原有的isActive参数
        final Boolean finalIsActive = statusBasedIsActive != null ? statusBasedIsActive : isActive;
        
        // 转换courseIdStr为Long类型，支持字符串ID和数字ID
        Long courseId = null;
        if (courseIdStr != null && !courseIdStr.isEmpty()) {
            try {
                // 尝试直接转换为Long
                courseId = Long.parseLong(courseIdStr);
            } catch (NumberFormatException e) {
                // 如果转换失败，尝试将其作为课程代码处理
                LogUtil.logDebug(logger, "尝试将课程代码 " + courseIdStr + " 转换为课程ID", getCurrentUserId(session));
                // 获取当前教师的所有课程
                List<Course> allCourses = courseService.findByTeacherId(getCurrentUserId(session));
                // 根据课程代码查找匹配的课程
                for (Course course : allCourses) {
                    if (course.getCourseCode() != null && course.getCourseCode().equals(courseIdStr)) {
                        courseId = course.getId();
                        LogUtil.logDebug(logger, "找到课程代码 " + courseIdStr + " 对应的课程ID: " + courseId, getCurrentUserId(session));
                        break;
                    }
                }
                if (courseId == null) {
                    LogUtil.logWarning(logger, "无效的课程ID或课程代码: " + courseIdStr, getCurrentUserId(session));
                }
            }
        }
        
        // 将courseId赋值给final变量，用于lambda表达式
        final Long finalCourseId = courseId;
        
        // 获取当前时间
        final Date now = new Date();
        
        LogUtil.logDebug(logger, "开始作业筛选 - 教师ID: " + teacherId + ", 关键词: " + keyword + ", 课程ID: " + finalCourseId + ", 状态: " + status + ", 当前时间: " + now, getCurrentUserId(session));
        
        // 预计算每个作业的提交数量、已批改数量和状态
        // 优化：使用更高效的方式获取提交统计信息
        List<Map<String, Object>> submissionStats = assignmentMapper.getSubmissionStats();
        
        // 按作业ID分组统计提交数量和已批改数量
        Map<Long, Integer> submissionCountMap = new java.util.HashMap<>();
        Map<Long, Integer> gradedCountMap = new java.util.HashMap<>();
        
        for (Map<String, Object> stat : submissionStats) {
            Long assignmentId = (Long) stat.get("assignmentId");
            Integer total = ((Number) stat.get("total")).intValue();
            Integer graded = ((Number) stat.get("graded")).intValue();
            
            // 统计提交数量和已批改数量
            submissionCountMap.put(assignmentId, total);
            gradedCountMap.put(assignmentId, graded);
        }
        
        // 获取所有作业
        List<Assignment> allAssignments = assignmentMapper.getAllAssignmentsByTeacher(teacherId);
        
        // 获取每个作业的预期参与学生数
        Map<Long, Integer> totalStudentsMap = new java.util.HashMap<>();
        for (Assignment assignment : allAssignments) {
            // 查询该作业分配的班级，然后统计这些班级的学生总数
            List<Long> classIds = assignmentMapper.getAssignmentClasses(assignment.getId());
            if (classIds != null && !classIds.isEmpty()) {
                int totalStudents = 0;
                for (Long assignmentClassId : classIds) {
                    // 统计该班级的学生数
                    Integer classStudentCount = assignmentMapper.countStudentsByClassId(assignmentClassId);
                    if (classStudentCount != null) {
                        totalStudents += classStudentCount;
                    }
                }
                totalStudentsMap.put(assignment.getId(), totalStudents);
            } else {
                // 如果没有分配班级，则通过课程关联的班级来统计
                // 查询该课程关联的所有班级的学生总数
                Integer courseStudentCount = assignmentMapper.countStudentsByCourseId(assignment.getCourseId());
                totalStudentsMap.put(assignment.getId(), courseStudentCount != null ? courseStudentCount : 0);
            }
        }
        
        // 为每个作业设置预计算的字段
        for (Assignment assignment : allAssignments) {
            // 设置提交数量
            Integer submissionCount = submissionCountMap.getOrDefault(assignment.getId(), 0);
            assignment.setSubmissionCount(submissionCount);
            
            // 设置已批改数量
            Integer gradedCount = gradedCountMap.getOrDefault(assignment.getId(), 0);
            assignment.setGradedCount(gradedCount);
            
            // 设置预期参与学生数
            Integer totalStudents = totalStudentsMap.getOrDefault(assignment.getId(), 0);
            assignment.setTotalStudents(totalStudents);
            
            // 计算并设置作业状态
            boolean isClosed = assignment.getDueDate() != null && assignment.getDueDate().before(now);
            String assignmentStatus = "pending"; // 默认状态：待提交
            
            if (isClosed) {
                assignmentStatus = "closed"; // 已截止
            } else if (submissionCount > 0) {
                if (gradedCount > 0) {
                    assignmentStatus = "graded"; // 已批改
                } else {
                    assignmentStatus = "submitted"; // 已提交
                }
            }
            
            assignment.setStatus(assignmentStatus);
        }
        
        // 应用筛选条件
        List<Assignment> filteredAssignments = allAssignments.stream()
                .filter(assignment -> {
                    // 教师ID筛选，只显示当前教师的作业
                    if (!assignment.getTeacherId().equals(teacherId)) {
                        return false;
                    }
                    
                    // 关键词搜索，搜索作业标题
                    if (keyword != null && !keyword.isEmpty()) {
                        if (!assignment.getTitle().toLowerCase().contains(keyword.toLowerCase())) {
                            return false;
                        }
                    }
                    
                    // 课程ID筛选
                    if (finalCourseId != null) {
                        if (!assignment.getCourseId().equals(finalCourseId)) {
                            return false;
                        }
                    }
                    
                    // 作业状态筛选
                    if (status != null) {
                        // 直接使用预计算的状态进行筛选，无需再次查询数据库
                        String assignmentStatus = assignment.getStatus();
                        LogUtil.logDebug(logger, "作业ID: " + assignment.getId() + ", 标题: " + assignment.getTitle() + ", 预计算状态: " + assignmentStatus + ", 筛选状态: " + status, getCurrentUserId(session));
                        
                        boolean result = true;
                        
                        // 根据前端状态值进行筛选
                        if (!status.equals(assignmentStatus)) {
                            result = false;
                        }
                        
                        LogUtil.logDebug(logger, "作业ID: " + assignment.getId() + ", 状态: " + status + ", 筛选结果: " + result, getCurrentUserId(session));
                        return result;
                    }
                    
                    // 作业激活状态筛选（如果没有提供status参数）
                    if (finalIsActive != null) {
                        if (assignment.getIsActive() != finalIsActive) {
                            return false;
                        }
                    }
                    
                    // 班级ID筛选
                    if (classId != null) {
                        // 检查作业是否分配给指定班级
                        List<Long> assignmentClassIds = assignmentMapper.getAssignmentClasses(assignment.getId());
                        if (!assignmentClassIds.contains(classId)) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .collect(java.util.stream.Collectors.toList());
        
        LogUtil.logDebug(logger, "作业筛选完成 - 筛选后作业数: " + filteredAssignments.size(), getCurrentUserId(session));
        
        // 计算总数
        int totalElements = filteredAssignments.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        // 应用分页
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, totalElements);
        // 将SubList转换为普通ArrayList，避免Redis反序列化错误
        List<Assignment> pagedAssignments = new ArrayList<>(filteredAssignments.subList(startIndex, endIndex));
        
        // 构建分页响应
        Map<String, Object> result = new java.util.HashMap<>();
        
        // 构建content
        result.put("content", pagedAssignments);
        
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
        result.put("numberOfElements", pagedAssignments.size());
        result.put("empty", pagedAssignments.isEmpty());
        
        LogUtil.logResponse(logger, "GET", "/api/teacher/assignments", 200, result, getCurrentUserId(session));
        return ResponseResult.success(result, "获取作业列表成功", 200);
    }
    
    @PostMapping
    @CacheEvict(value = CacheConstants.ASSIGNMENTS, allEntries = true)
    @Transactional
    public ResponseResult<Assignment> createAssignment(@RequestBody Map<String, Object> requestBody, HttpSession session) {
        LogUtil.logRequest(logger, "POST", "/api/teacher/assignments", requestBody, getCurrentUserId(session));
        
        if (!isLoggedIn(session)) {
            LogUtil.logWarning(logger, "未授权创建作业", getCurrentUserId(session));
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        try {
            // 使用session中的teacherId覆盖前端传入的teacherId，确保安全
            Long teacherId = getCurrentUserId(session);
            
            // 从请求体中提取作业信息
            Assignment assignment = new Assignment();
            assignment.setTitle((String) requestBody.get("title"));
            assignment.setDescription((String) requestBody.get("description"));
            
            // 处理courseId
            Object courseIdObj = requestBody.get("courseId");
            if (courseIdObj instanceof Number) {
                assignment.setCourseId(((Number) courseIdObj).longValue());
            } else if (courseIdObj instanceof String) {
                assignment.setCourseId(Long.parseLong((String) courseIdObj));
            }
            
            // 处理日期
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            SimpleDateFormat standardFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            Object dueDateObj = requestBody.get("dueDate");
            if (dueDateObj instanceof String) {
                String dueDateStr = (String) dueDateObj;
                if (dueDateStr.contains("T")) {
                    assignment.setDueDate(sdf.parse(dueDateStr));
                } else {
                    assignment.setDueDate(standardFormat.parse(dueDateStr));
                }
            }
            
            Object publishDateObj = requestBody.get("publishDate");
            if (publishDateObj instanceof String) {
                String publishDateStr = (String) publishDateObj;
                if (publishDateStr.contains("T")) {
                    assignment.setPublishDate(sdf.parse(publishDateStr));
                } else {
                    assignment.setPublishDate(standardFormat.parse(publishDateStr));
                }
            }
            
            // 处理maxScore
            Object maxScoreObj = requestBody.get("maxScore");
            if (maxScoreObj instanceof Number) {
                assignment.setMaxScore(((Number) maxScoreObj).intValue());
            }
            
            assignment.setTeacherId(teacherId);
            
            // 设置默认值
            if (assignment.getIsActive() == null) {
                assignment.setIsActive(true);
            }
            if (assignment.getPublishDate() == null) {
                assignment.setPublishDate(new Date());
            }
            
            // 打印详细日志，检查assignment对象的所有字段
            logger.info("准备保存作业，作业对象详细信息: [id={}, title={}, description={}, courseId={}, dueDate={}, publishDate={}, isActive={}, teacherId={}]", 
                assignment.getId(), assignment.getTitle(), assignment.getDescription(), assignment.getCourseId(), 
                assignment.getDueDate(), assignment.getPublishDate(), assignment.getIsActive(), 
                assignment.getTeacherId());
            
            // 保存作业
            assignmentService.create(assignment);
            
            // 保存后打印作业ID
            logger.info("作业保存成功，生成的作业ID: {}", assignment.getId());
            
            // 处理知识点关联
            @SuppressWarnings("unchecked")
            List<Number> knowledgePointIdsRaw = (List<Number>) requestBody.get("knowledgePointIds");
            if (knowledgePointIdsRaw != null && !knowledgePointIdsRaw.isEmpty()) {
                List<Long> knowledgePointIds = knowledgePointIdsRaw.stream()
                    .map(Number::longValue)
                    .toList();
                knowledgePointService.setAssignmentKnowledgePoints(assignment.getId(), knowledgePointIds);
                logger.info("作业 {} 关联了 {} 个知识点: {}", assignment.getId(), knowledgePointIds.size(), knowledgePointIds);
            }
            
            // 发送作业通知给所有选修该课程的学生
            try {
                // 获取该课程的所有学生ID
                List<Long> studentIds = courseService.getStudentIdsByCourseId(assignment.getCourseId());
                logger.info("获取到课程 {} 的学生ID列表: {}", assignment.getCourseId(), studentIds);
                
                // 创建通知列表
                List<Notification> notifications = new ArrayList<>();
                for (Long studentId : studentIds) {
                    Notification notification = new Notification();
                    notification.setStudentId(studentId);
                    notification.setTeacherId(assignment.getTeacherId());
                    notification.setType("assignment");
                    notification.setTitle("新作业发布");
                    notification.setContent("您有一份新作业：" + assignment.getTitle() + "，请及时完成");
                    notification.setRelatedId(assignment.getId());
                    notifications.add(notification);
                }
                
                // 批量发送通知
                if (!notifications.isEmpty()) {
                    notificationService.createBatch(notifications);
                    logger.info("成功发送 {} 条作业通知", notifications.size());
                }
            } catch (Exception e) {
                logger.error("发送作业通知失败: {}", e.getMessage(), e);
                // 发送通知失败不影响作业发布，继续执行
            }
            
            LogUtil.logOperation(logger, "创建作业", "作业标题: " + assignment.getTitle(), getCurrentUserId(session), true);
            LogUtil.logResponse(logger, "POST", "/api/teacher/assignments", 201, assignment, getCurrentUserId(session));
            return ResponseResult.created(assignment);
        } catch (Exception e) {
            LogUtil.logError(logger, "创建作业失败", e);
            // 打印完整的异常堆栈信息
            logger.error("创建作业失败，完整异常信息: ", e);
            return ResponseResult.failure("创建作业失败: " + e.getMessage(), 500);
        }
    }
    
    @PutMapping("/{id}")
    @CacheEvict(value = CacheConstants.ASSIGNMENTS, allEntries = true)
    public ResponseResult<Assignment> updateAssignment(@PathVariable Long id, @RequestBody Map<String, Object> requestBody, HttpSession session) {
        LogUtil.logRequest(logger, "PUT", "/api/teacher/assignments/" + id, requestBody, getCurrentUserId(session));
        
        if (!isLoggedIn(session)) {
            LogUtil.logWarning(logger, "未授权更新作业", getCurrentUserId(session));
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        try {
            // 获取当前教师的所有课程，用于课程代码转换
            Long teacherId = getCurrentUserId(session);
            List<Course> allCourses = courseService.findByTeacherId(teacherId);
            
            // 处理课程ID，支持字符串课程代码
            Object courseIdObj = requestBody.get("courseId");
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
                            LogUtil.logError(logger, "更新作业失败 - 无效的课程代码: " + courseIdStr, null);
                            return ResponseResult.failure("更新作业失败 - 无效的课程代码", 400);
                        }
                    }
                } else if (courseIdObj instanceof Number) {
                    // 如果是数字，直接转换为Long
                    courseId = ((Number) courseIdObj).longValue();
                }
            }
            
            if (courseId == null) {
                LogUtil.logError(logger, "更新作业失败 - 课程ID不能为空", null);
                return ResponseResult.failure("更新作业失败 - 课程ID不能为空", 400);
            }
            
            // 获取现有作业
            Assignment existingAssignment = assignmentService.getAssignmentById(id);
            if (existingAssignment == null) {
                return ResponseResult.failure("作业不存在", 404);
            }
            
            // 更新作业信息
            existingAssignment.setTitle((String) requestBody.get("title"));
            existingAssignment.setDescription((String) requestBody.get("description"));
            existingAssignment.setCourseId(courseId);
            
            // 处理日期字符串，转换为Date对象
            // 支持多种日期格式：ISO格式（如：2025-12-24T08:00:00.000Z）和标准格式（如：2025-12-24 08:00:00）
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            SimpleDateFormat standardFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            Object dueDateObj = requestBody.get("dueDate");
            if (dueDateObj != null) {
                if (dueDateObj instanceof Date) {
                    existingAssignment.setDueDate((Date) dueDateObj);
                } else if (dueDateObj instanceof String) {
                    String dueDateStr = (String) dueDateObj;
                    try {
                        // 尝试ISO格式
                        if (dueDateStr.contains("T") && dueDateStr.contains("Z")) {
                            existingAssignment.setDueDate(isoFormat.parse(dueDateStr));
                        } else {
                            // 尝试标准格式
                            existingAssignment.setDueDate(standardFormat.parse(dueDateStr));
                        }
                    } catch (Exception e) {
                        logger.error("解析截止日期失败: " + dueDateStr, e);
                        throw new IllegalArgumentException("无效的截止日期格式: " + dueDateStr);
                    }
                }
            }
            
            Object publishDateObj = requestBody.get("publishDate");
            if (publishDateObj != null) {
                if (publishDateObj instanceof Date) {
                    existingAssignment.setPublishDate((Date) publishDateObj);
                } else if (publishDateObj instanceof String) {
                    String publishDateStr = (String) publishDateObj;
                    try {
                        // 尝试ISO格式
                        if (publishDateStr.contains("T") && publishDateStr.contains("Z")) {
                            existingAssignment.setPublishDate(isoFormat.parse(publishDateStr));
                        } else {
                            // 尝试标准格式
                            existingAssignment.setPublishDate(standardFormat.parse(publishDateStr));
                        }
                    } catch (Exception e) {
                        logger.error("解析发布日期失败: " + publishDateStr, e);
                        throw new IllegalArgumentException("无效的发布日期格式: " + publishDateStr);
                    }
                }
            }
            
            // 处理maxScore字段（如果存在）
            Object maxScoreObj = requestBody.get("maxScore");
            if (maxScoreObj != null) {
                if (maxScoreObj instanceof Number) {
                    existingAssignment.setMaxScore(((Number) maxScoreObj).intValue());
                } else if (maxScoreObj instanceof String) {
                    try {
                        existingAssignment.setMaxScore(Integer.parseInt((String) maxScoreObj));
                    } catch (NumberFormatException e) {
                        logger.warn("无效的满分值: " + maxScoreObj);
                    }
                }
            }
            
            existingAssignment.setIsActive((Boolean) requestBody.get("isActive"));
            existingAssignment.setTeacherId(teacherId);
            
            // 保存更新
            assignmentService.update(existingAssignment);
            LogUtil.logOperation(logger, "更新作业", "作业ID: " + id + ", 作业标题: " + existingAssignment.getTitle(), getCurrentUserId(session), true);
            LogUtil.logResponse(logger, "PUT", "/api/teacher/assignments/" + id, 200, existingAssignment, getCurrentUserId(session));
            return ResponseResult.success(existingAssignment);
        } catch (Exception e) {
            LogUtil.logError(logger, "更新作业失败，作业ID: " + id, e);
            return ResponseResult.failure("更新作业失败", 500);
        }
    }
    
    @DeleteMapping("/{id}")
    @CacheEvict(value = CacheConstants.ASSIGNMENTS, allEntries = true)
    public ResponseResult<Void> deleteAssignment(@PathVariable Long id, HttpSession session) {
        LogUtil.logRequest(logger, "DELETE", "/api/teacher/assignments/" + id, null, getCurrentUserId(session));
        
        if (!isLoggedIn(session)) {
            LogUtil.logWarning(logger, "未授权删除作业", getCurrentUserId(session));
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        try {
            assignmentService.delete(id);
            LogUtil.logOperation(logger, "删除作业", "作业ID: " + id, getCurrentUserId(session), true);
            LogUtil.logResponse(logger, "DELETE", "/api/teacher/assignments/" + id, 204, null, getCurrentUserId(session));
            return ResponseResult.noContent();
        } catch (Exception e) {
            LogUtil.logError(logger, "删除作业失败，作业ID: " + id, e);
            return ResponseResult.failure("删除作业失败", 500);
        }
    }
    
    @GetMapping("/{id}")
    public ResponseResult<Map<String, Object>> getAssignmentById(@PathVariable Long id, HttpSession session) {
        LogUtil.logRequest(logger, "GET", "/api/teacher/assignments/" + id, null, getCurrentUserId(session));
        
        if (!isLoggedIn(session)) {
            LogUtil.logWarning(logger, "未授权访问作业详情", getCurrentUserId(session));
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        Map<String, Object> assignmentDetails = assignmentService.getAssignmentDetailsWithSubmissions(id);
        if (assignmentDetails == null) {
            return ResponseResult.failure("作业不存在", 404);
        }
        
        LogUtil.logResponse(logger, "GET", "/api/teacher/assignments/" + id, 200, assignmentDetails, getCurrentUserId(session));
        return ResponseResult.success(assignmentDetails, "获取作业详情成功", 200);
    }
    
    @PutMapping("/grade/{submissionId}")
    @CacheEvict(value = CacheConstants.ASSIGNMENTS, allEntries = true)
    public ResponseResult<Map<String, Object>> gradeAssignment(@PathVariable Long submissionId, 
                                               @RequestBody Map<String, Object> gradeRequest, 
                                               HttpSession session) {
        LogUtil.logRequest(logger, "PUT", "/api/teacher/assignments/grade/" + submissionId, gradeRequest, getCurrentUserId(session));
        
        if (!isLoggedIn(session)) {
            LogUtil.logWarning(logger, "未授权批改作业", getCurrentUserId(session));
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
                        LogUtil.logError(logger, "批改作业失败 - 无效的分数格式: " + scoreObj, e);
                        return ResponseResult.failure("批改作业失败 - 无效的分数格式", 400);
                    }
                }
            }
            
            String teacherComment = gradeRequest.get("teacherComment") != null ? 
                gradeRequest.get("teacherComment").toString() : "";
            
            if (score == null) {
                LogUtil.logError(logger, "批改作业失败 - 分数不能为空", null);
                return ResponseResult.failure("批改作业失败 - 分数不能为空", 400);
            }
            
            if (score < 0 || score > 100) {
                LogUtil.logError(logger, "批改作业失败 - 分数必须在0-100之间: " + score, null);
                return ResponseResult.failure("批改作业失败 - 分数必须在0-100之间", 400);
            }
            
            boolean success = assignmentSubmissionService.gradeAssignment(submissionId, score, teacherComment);
            if (success) {
                // 获取批改后的提交记录
                AssignmentSubmission submission = assignmentSubmissionService.getSubmissionById(submissionId);
                if (submission != null) {
                    // 实时触发学情分析
                    try {
                        // 异步执行分析，避免阻塞批改操作
                        new Thread(() -> {
                            try {
                                // 获取作业信息
                                Map<String, Object> assignmentDetails = assignmentService.getAssignmentDetailsWithSubmissions(submission.getAssignmentId());
                                if (assignmentDetails != null) {
                                    Assignment assignment = (Assignment) assignmentDetails.get("assignment");
                                    if (assignment != null) {
                                        // 触发该学生该课程的实时分析
                                        earlyWarningAnalysisService.analyzeStudentWarningsRealtime(submission.getStudentId(), assignment.getCourseId());
                                    }
                                }
                            } catch (Exception e) {
                                logger.error("实时学情分析失败", e);
                            }
                        }).start();
                    } catch (Exception e) {
                        logger.warn("启动实时学情分析失败，但不影响批改操作", e);
                    }
                    
                    // 构建返回结果
                    Map<String, Object> result = new java.util.HashMap<>();
                    result.put("id", submission.getId());
                    result.put("studentId", submission.getStudentId());
                    // 获取学生姓名
                    // 这里需要添加获取学生姓名的逻辑
                    // result.put("studentName", studentName);
                    // result.put("assignmentId", submission.getAssignmentId());
                    // 获取作业标题
                    // Assignment assignment = assignmentService.getAssignmentById(submission.getAssignmentId());
                    // result.put("assignmentTitle", assignment.getTitle());
                    result.put("submissionDate", submission.getSubmissionDate());
                    result.put("isLate", submission.getIsLate());
                    result.put("latePenalty", submission.getLatePenalty());
                    result.put("score", submission.getScore());
                    result.put("teacherComment", submission.getTeacherComment());
                    result.put("graded", submission.getGraded());
                    
                    LogUtil.logOperation(logger, "批改作业", "提交ID: " + submissionId, getCurrentUserId(session), true);
                    LogUtil.logResponse(logger, "PUT", "/api/teacher/assignments/grade/" + submissionId, 200, result, getCurrentUserId(session));
                    return ResponseResult.success(result, "作业批改成功", 200);
                }
                return ResponseResult.failure("获取批改后的提交记录失败", 500);
            } else {
                return ResponseResult.failure("批改作业失败", 500);
            }
        } catch (Exception e) {
            LogUtil.logError(logger, "批改作业失败，提交ID: " + submissionId, e);
            return ResponseResult.failure("批改作业失败", 500);
        }
    }
    
    // 作业提交记录管理接口
    
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
        LogUtil.logRequest(logger, "GET", "/api/teacher/assignments/submissions", null, getCurrentUserId(session));
        
        if (!isLoggedIn(session)) {
            LogUtil.logWarning(logger, "未授权访问作业提交记录", getCurrentUserId(session));
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        List<AssignmentSubmission> submissions = assignmentSubmissionService.getSubmissionsWithPagination(
                page, size, sortBy, order, assignmentId, studentId, graded);
        Integer total = assignmentSubmissionService.countSubmissions(assignmentId, studentId, graded);
        
        // 构建符合前端预期的分页响应格式
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("content", submissions);
        
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
        
        pageable.put("offset", (page - 1) * size);
        pageable.put("paged", true);
        pageable.put("unpaged", false);
        
        result.put("pageable", pageable);
        
        // 其他分页字段
        result.put("totalPages", (int) Math.ceil((double) total / size));
        result.put("totalElements", total);
        result.put("last", page >= (int) Math.ceil((double) total / size));
        result.put("size", size);
        result.put("number", page - 1); // 前端从1开始，后端从0开始
        result.put("sort", sort);
        result.put("first", page == 1);
        result.put("numberOfElements", submissions.size());
        result.put("empty", submissions.isEmpty());
        
        LogUtil.logResponse(logger, "GET", "/api/teacher/assignments/submissions", 200, result, getCurrentUserId(session));
        return ResponseResult.success(result, "获取作业提交记录成功", 200);
    }
    
    @GetMapping("/submissions/{submissionId}")
    public ResponseResult<AssignmentSubmission> getSubmissionById(@PathVariable Long submissionId, HttpSession session) {
        LogUtil.logRequest(logger, "GET", "/api/teacher/assignments/submissions/" + submissionId, null, getCurrentUserId(session));
        
        if (!isLoggedIn(session)) {
            LogUtil.logWarning(logger, "未授权访问作业提交记录详情", getCurrentUserId(session));
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        AssignmentSubmission submission = assignmentSubmissionService.getSubmissionById(submissionId);
        if (submission == null) {
            return ResponseResult.failure("作业提交记录不存在", 404);
        }
        
        LogUtil.logResponse(logger, "GET", "/api/teacher/assignments/submissions/" + submissionId, 200, submission, getCurrentUserId(session));
        return ResponseResult.success(submission, "获取作业提交记录详情成功", 200);
    }
    
    @GetMapping("/{assignmentId}/submissions")
    public ResponseResult<List<AssignmentSubmission>> getSubmissionsByAssignmentId(@PathVariable Long assignmentId, HttpSession session) {
        LogUtil.logRequest(logger, "GET", "/api/teacher/assignments/" + assignmentId + "/submissions", null, getCurrentUserId(session));
        
        if (!isLoggedIn(session)) {
            LogUtil.logWarning(logger, "未授权访问作业提交记录", getCurrentUserId(session));
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        List<AssignmentSubmission> submissions = assignmentSubmissionService.getSubmissionsByAssignmentId(assignmentId);
        LogUtil.logResponse(logger, "GET", "/api/teacher/assignments/" + assignmentId + "/submissions", 200, submissions, getCurrentUserId(session));
        return ResponseResult.success(submissions, "获取作业提交记录成功", 200);
    }
    
    @PutMapping("/submissions/{submissionId}")
    public ResponseResult<AssignmentSubmission> updateSubmission(@PathVariable Long submissionId, @RequestBody AssignmentSubmission submission, HttpSession session) {
        LogUtil.logRequest(logger, "PUT", "/api/teacher/assignments/submissions/" + submissionId, submission, getCurrentUserId(session));
        
        if (!isLoggedIn(session)) {
            LogUtil.logWarning(logger, "未授权更新作业提交记录", getCurrentUserId(session));
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        submission.setId(submissionId);
        boolean success = assignmentSubmissionService.updateSubmission(submission);
        if (success) {
            LogUtil.logOperation(logger, "更新作业提交记录", "提交ID: " + submissionId, getCurrentUserId(session), true);
            LogUtil.logResponse(logger, "PUT", "/api/teacher/assignments/submissions/" + submissionId, 200, submission, getCurrentUserId(session));
            return ResponseResult.success(submission, "更新作业提交记录成功", 200);
        } else {
            return ResponseResult.failure("更新作业提交记录失败", 500);
        }
    }
    
    @DeleteMapping("/submissions/{submissionId}")
    public ResponseResult<Void> deleteSubmission(@PathVariable Long submissionId, HttpSession session) {
        LogUtil.logRequest(logger, "DELETE", "/api/teacher/assignments/submissions/" + submissionId, null, getCurrentUserId(session));
        
        if (!isLoggedIn(session)) {
            LogUtil.logWarning(logger, "未授权删除作业提交记录", getCurrentUserId(session));
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        boolean success = assignmentSubmissionService.deleteSubmission(submissionId);
        if (success) {
            LogUtil.logOperation(logger, "删除作业提交记录", "提交ID: " + submissionId, getCurrentUserId(session), true);
            LogUtil.logResponse(logger, "DELETE", "/api/teacher/assignments/submissions/" + submissionId, 204, null, getCurrentUserId(session));
            return ResponseResult.noContent();
        } else {
            return ResponseResult.failure("删除作业提交记录失败", 500);
        }
    }
}

