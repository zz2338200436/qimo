package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.annotation.RequireLogin;
import com._202510007517.major_assignment.client.UserServiceProfileClient;
import com._202510007517.major_assignment.constants.ErrorMessages;
import com._202510007517.major_assignment.constants.SuccessMessages;
import com._202510007517.major_assignment.entity.Assignment;
import com._202510007517.major_assignment.entity.AssignmentSubmission;
import com._202510007517.major_assignment.entity.Course;
import com._202510007517.major_assignment.entity.Exam;
import com._202510007517.major_assignment.entity.ExamSubmission;
import com._202510007517.major_assignment.entity.User;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.exception.ResourceNotFoundException;
import com._202510007517.major_assignment.exception.UnauthorizedException;
import com._202510007517.major_assignment.service.*;
import com._202510007517.platform.user.api.dto.UserProfileDTO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Validated
public class StudentController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(StudentController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UserServiceProfileClient userServiceProfileClient;
    
    @Autowired
    private StudentService studentService;
    
    @Autowired
    private CourseService courseService;
    
    @Autowired
    private AssignmentService assignmentService;
    
    @Autowired
    private ExamService examService;
    
    @Autowired
    private AssignmentSubmissionService assignmentSubmissionService;
    
    @Autowired
    private ExamSubmissionService examSubmissionService;

    @Autowired
    private AssessmentAttachmentService assessmentAttachmentService;

    @GetMapping("/student/courses")
    @RequireLogin
    public ResponseResult<Map<String, Object>> getStudentCourses(HttpServletRequest requestContext,
                                                               @RequestParam(value = "page", defaultValue = "1") @Min(1) Integer page,
                                                               @RequestParam(value = "size", defaultValue = "10") @Min(1) @Max(100) Integer size,
                                                               @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
                                                               @RequestParam(value = "order", defaultValue = "DESC") String order,
                                                               @RequestParam(value = "courseStatus", required = false) String courseStatus,
                                                               @RequestParam(value = "semester", required = false) String semester,
                                                               @RequestParam(value = "courseCategory", required = false) String courseCategory,
                                                               @RequestParam(value = "searchQuery", required = false) String searchQuery) {
        Long currentUserId = getCurrentUserId(requestContext);
        
        // 调用服务层获取分页数据
        Map<String, Object> result = studentService.getStudentCoursesWithPagination(currentUserId, page, size, sortBy, order, courseStatus, semester, courseCategory, searchQuery);
        return ResponseResult.success(result, SuccessMessages.GET_COURSE_LIST_SUCCESS, 200);
    }
    
    @GetMapping("/student/courses/{courseId}")
    @RequireLogin
    public ResponseResult<Course> getCourseDetail(@PathVariable Long courseId, HttpServletRequest requestContext) {
        Course course = courseService.findById(courseId);
        if (course == null) {
            throw new ResourceNotFoundException(ErrorMessages.COURSE_NOT_FOUND);
        }
        return ResponseResult.success(course, SuccessMessages.GET_COURSE_DETAIL_SUCCESS, 200);
    }
    
    @GetMapping("/student/assignments")
    @RequireLogin
    public ResponseResult<Map<String, Object>> getAssignments(HttpServletRequest requestContext,
                                                            @RequestParam(value = "page", defaultValue = "1") @Min(1) Integer page,
                                                            @RequestParam(value = "size", defaultValue = "10") @Min(1) @Max(100) Integer size,
                                                            @RequestParam(value = "sortBy", defaultValue = "dueDate") String sortBy,
                                                            @RequestParam(value = "order", defaultValue = "DESC") String order,
                                                            @RequestParam(value = "courseId", required = false) Long courseId,
                                                            @RequestParam(value = "submitted", required = false) Boolean submitted,
                                                            @RequestParam(value = "isActive", required = false) Boolean isActive) {
        Long userId = getCurrentUserId(requestContext);
        
        // 调用服务层获取分页数据
        Map<String, Object> result = assignmentService.getAssignmentsWithPagination(userId, page, size, sortBy, order, courseId, submitted, isActive);
        return ResponseResult.success(result, SuccessMessages.GET_ASSIGNMENT_LIST_SUCCESS, 200);
    }
    
    @GetMapping("/student/assignments/{assignmentId}")
    @RequireLogin
    public ResponseResult<Map<String, Object>> getAssignmentDetail(@PathVariable Long assignmentId, HttpServletRequest requestContext) {
        Long userId = getCurrentUserId(requestContext);
        
        Assignment assignment = assignmentService.getAssignmentById(assignmentId);
        if (assignment == null) {
            throw new ResourceNotFoundException(ErrorMessages.ASSIGNMENT_NOT_FOUND);
        }
        
        // 构建作业详情Map
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", assignment.getId());
        result.put("title", assignment.getTitle());
        result.put("description", assignment.getDescription());
        result.put("dueDate", assignment.getDueDate());
        result.put("publishDate", assignment.getPublishDate());
        result.put("isActive", assignment.getIsActive());
        result.put("courseId", assignment.getCourseId());
        result.put("attachments", assessmentAttachmentService.getAttachmentDtos(
                AssessmentAttachmentService.ASSIGNMENT_TYPE, assignmentId));
        
        // 获取课程名称
        Course course = courseService.findById(assignment.getCourseId());
        if (course != null) {
            result.put("courseName", course.getCourseName());
        }
        
        enrichTeacherInfo(result, assignment.getTeacherId());
        
        // 获取学生的提交信息
        AssignmentSubmission submission = assignmentSubmissionService.getSubmissionByAssignmentAndStudent(assignmentId, userId);
        if (submission != null) {
            Map<String, Object> submissionMap = new java.util.HashMap<>();
            submissionMap.put("id", submission.getId());
            submissionMap.put("submissionDate", submission.getSubmissionDate());
            submissionMap.put("isLate", submission.getIsLate());
            submissionMap.put("latePenalty", submission.getLatePenalty());
            submissionMap.put("score", submission.getScore());
            submissionMap.put("teacherComment", submission.getTeacherComment());
            submissionMap.put("graded", submission.getGraded());
            submissionMap.put("content", submission.getContent());
            result.put("submission", submissionMap);
        }
        
        return ResponseResult.success(result, SuccessMessages.GET_ASSIGNMENT_DETAIL_SUCCESS, 200);
    }
    
    @GetMapping("/student/exams")
    @RequireLogin
    public ResponseResult<Map<String, Object>> getExams(HttpServletRequest requestContext,
                                                      @RequestParam(value = "page", defaultValue = "1") @Min(1) Integer page,
                                                      @RequestParam(value = "size", defaultValue = "10") @Min(1) @Max(100) Integer size,
                                                      @RequestParam(value = "sortBy", defaultValue = "startTime") String sortBy,
                                                      @RequestParam(value = "order", defaultValue = "DESC") String order,
                                                      @RequestParam(value = "courseId", required = false) Long courseId,
                                                      @RequestParam(value = "isActive", required = false) Boolean isActive,
                                                      @RequestParam(value = "submitted", required = false) Boolean submitted) {
        Long userId = getCurrentUserId(requestContext);
        
        // 调用服务层获取分页数据
        Map<String, Object> result = examService.getExamsWithPagination(userId, page, size, sortBy, order, courseId, isActive, submitted);
        return ResponseResult.success(result, SuccessMessages.GET_EXAM_LIST_SUCCESS, 200);
    }
    
    @GetMapping("/student/exams/{examId}")
    @RequireLogin
    public ResponseResult<Map<String, Object>> getExamDetail(@PathVariable Long examId, HttpServletRequest requestContext) {
        Long userId = getCurrentUserId(requestContext);
        
        Exam exam = examService.getExamById(examId);
        if (exam == null) {
            throw new ResourceNotFoundException(ErrorMessages.EXAM_NOT_FOUND);
        }
        
        // 构建考试详情Map
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", exam.getId());
        result.put("title", exam.getTitle());
        result.put("description", exam.getDescription());
        result.put("startTime", exam.getStartTime());
        result.put("endTime", exam.getEndTime());
        result.put("duration", exam.getDuration());
        result.put("isActive", exam.getIsActive());
        result.put("isOnline", exam.getIsOnline());
        result.put("location", exam.getLocation());
        result.put("publishDate", exam.getPublishDate());
        result.put("courseId", exam.getCourseId());
        result.put("attachments", assessmentAttachmentService.getAttachmentDtos(
                AssessmentAttachmentService.EXAM_TYPE, examId));
        
        // 获取课程名称
        Course course = courseService.findById(exam.getCourseId());
        if (course != null) {
            result.put("courseName", course.getCourseName());
        }
        
        enrichTeacherInfo(result, exam.getTeacherId());
        
        // 获取学生的提交信息
        ExamSubmission submission = examSubmissionService.getSubmissionByExamAndStudent(examId, userId);
        if (submission != null) {
            Map<String, Object> submissionMap = new java.util.HashMap<>();
            submissionMap.put("id", submission.getId());
            submissionMap.put("submissionDate", submission.getSubmissionDate());
            submissionMap.put("timeTaken", submission.getTimeTaken());
            submissionMap.put("score", submission.getScore());
            submissionMap.put("teacherComment", submission.getTeacherComment());
            submissionMap.put("graded", submission.getGraded());
            submissionMap.put("content", submission.getContent());
            result.put("submission", submissionMap);
        }
        
        return ResponseResult.success(result, SuccessMessages.GET_EXAM_DETAIL_SUCCESS, 200);
    }
    
    @PostMapping("/student/assignments/{assignmentId}/submit")
    @RequireLogin
    public ResponseResult<Map<String, Object>> submitAssignment(@PathVariable Long assignmentId, @RequestBody Map<String, String> submission, HttpServletRequest requestContext) {
        try {
            Long userId = getCurrentUserId(requestContext);
            String content = submission.get("content");
            
            // 验证提交内容
            if (content == null || content.trim().isEmpty()) {
                return ResponseResult.failure(ErrorMessages.CONTENT_EMPTY, 400);
            }
            
            AssignmentSubmission submissionResult = assignmentSubmissionService.submitAssignment(assignmentId, userId, content);
            if (submissionResult == null) {
                return ResponseResult.failure(ErrorMessages.SUBMIT_FAILED, 500);
            }
            
            // 构建符合文档要求的响应
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("id", submissionResult.getId());
            result.put("submissionDate", submissionResult.getSubmissionDate());
            result.put("isLate", submissionResult.getIsLate());
            result.put("latePenalty", submissionResult.getLatePenalty());
            result.put("graded", submissionResult.getGraded());
            
            return ResponseResult.success(result, SuccessMessages.ASSIGNMENT_SUBMIT_SUCCESS, 200);
        } catch (IllegalArgumentException e) {
            return ResponseResult.failure(e.getMessage(), 400);
        } catch (Exception e) {
            logger.error("提交作业时发生内部错误", e);
            return ResponseResult.failure(ErrorMessages.INTERNAL_ERROR, 500);
        }
    }
    
    @PostMapping("/student/exams/{examId}/submit")
    @RequireLogin
    public ResponseResult<Map<String, Object>> submitExam(@PathVariable Long examId, @RequestBody Map<String, Object> submission, HttpServletRequest requestContext) {
        Long userId = getCurrentUserId(requestContext);
        
        // 安全转换 timeTaken
        Integer timeTaken = 0;
        if (submission.get("timeTaken") instanceof Number) {
            timeTaken = ((Number) submission.get("timeTaken")).intValue();
        }
        
        // 安全转换 answers
        Map<String, String> answers = new java.util.HashMap<>();
        if (submission.get("answers") instanceof Map) {
            Map<?, ?> rawAnswers = (Map<?, ?>) submission.get("answers");
            for (Map.Entry<?, ?> entry : rawAnswers.entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                    answers.put((String) entry.getKey(), (String) entry.getValue());
                }
            }
        }
        
        ExamSubmission submissionResult = examSubmissionService.submitExam(examId, userId, timeTaken, answers);
        if (submissionResult == null) {
            return ResponseResult.failure(ErrorMessages.SUBMIT_FAILED, 500);
        }
        
        // 构建符合文档要求的响应
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", submissionResult.getId());
        result.put("submissionDate", submissionResult.getSubmissionDate());
        result.put("timeTaken", submissionResult.getTimeTaken());
        result.put("graded", submissionResult.getGraded());
        
        return ResponseResult.success(result, SuccessMessages.EXAM_SUBMIT_SUCCESS, 200);
    }
    
    @GetMapping("/student/stats")
    @RequireLogin
    public ResponseResult<Map<String, Object>> getLearningStats(
            @RequestParam(value = "semester", required = false) String semester,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "timeRange", required = false) String timeRange,
            HttpServletRequest requestContext) {
        Long userId = getCurrentUserId(requestContext);
        Map<String, Object> stats = studentService.getLearningStats(userId, semester, courseId, timeRange);
        return ResponseResult.success(stats, SuccessMessages.GET_STATS_SUCCESS, 200);
    }
    
    @GetMapping("/student/knowledge-points")
    @RequireLogin
    public ResponseResult<List<Map<String, Object>>> getKnowledgePoints(
            @RequestParam(value = "semester", required = false) String semester,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "timeRange", required = false) String timeRange,
            HttpServletRequest requestContext) {
        Long userId = getCurrentUserId(requestContext);
        List<Map<String, Object>> knowledgePoints = studentService.getKnowledgePoints(userId, semester, courseId, timeRange);
        return ResponseResult.success(knowledgePoints, SuccessMessages.GET_KNOWLEDGE_POINTS_SUCCESS, 200);
    }
    
    @GetMapping("/student/scores")
    @RequireLogin
    public ResponseResult<List<Map<String, Object>>> getScores(
            @RequestParam(value = "semester", required = false) String semester,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "timeRange", required = false) String timeRange,
            HttpServletRequest requestContext) {
        Long userId = getCurrentUserId(requestContext);
        List<Map<String, Object>> scores = studentService.getScores(userId, semester, courseId, timeRange);
        return ResponseResult.success(scores, SuccessMessages.GET_SCORES_SUCCESS, 200);
    }
    
    @GetMapping("/student/study-time-distribution")
    @RequireLogin
    public ResponseResult<List<Map<String, Object>>> getStudyTimeDistribution(
            @RequestParam(value = "type", defaultValue = "daily") String type,
            @RequestParam(value = "semester", required = false) String semester,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "timeRange", required = false) String timeRange,
            HttpServletRequest requestContext) {
        Long userId = getCurrentUserId(requestContext);
        List<Map<String, Object>> studyTimeDistribution = studentService.getStudyTimeDistribution(userId, type, semester, courseId, timeRange);

        // 将数据库中的 study_time 字段转换为前端期望的 hours/label 字段
        List<Map<String, Object>> transformed = new ArrayList<>();
        for (Map<String, Object> row : studyTimeDistribution) {
            Map<String, Object> item = new HashMap<>(row);

            Object studyTimeObj = row.get("study_time");
            double hours = 0.0;
            if (studyTimeObj instanceof Number) {
                hours = ((Number) studyTimeObj).doubleValue();
            } else if (studyTimeObj != null) {
                try {
                    hours = Double.parseDouble(studyTimeObj.toString());
                } catch (NumberFormatException ignored) {
                    hours = 0.0;
                }
            }
            item.put("hours", hours);

            // label 默认使用 date 字段，便于前端直接展示
            Object dateObj = row.get("date");
            if (dateObj != null && !item.containsKey("label")) {
                item.put("label", dateObj.toString());
            }

            transformed.add(item);
        }

        return ResponseResult.success(transformed, SuccessMessages.GET_STUDY_TIME_SUCCESS, 200);
    }
    
    @GetMapping("/student/knowledge-points/{knowledgePointId}")
    @RequireLogin
    public ResponseResult<Map<String, Object>> getKnowledgePointDetail(@PathVariable Long knowledgePointId, HttpServletRequest requestContext) {
        Long userId = getCurrentUserId(requestContext);
        Map<String, Object> knowledgePointDetail = studentService.getKnowledgePointDetail(userId, knowledgePointId);
        if (knowledgePointDetail == null) {
            throw new ResourceNotFoundException(ErrorMessages.KNOWLEDGE_POINT_NOT_FOUND);
        }
        return ResponseResult.success(knowledgePointDetail, SuccessMessages.GET_KNOWLEDGE_POINT_DETAIL_SUCCESS, 200);
    }
    
    @GetMapping("/student/early-warnings")
    @RequireLogin
    public ResponseResult<List<Map<String, Object>>> getEarlyWarnings(HttpServletRequest requestContext) {
        Long userId = getCurrentUserId(requestContext);
        List<Map<String, Object>> earlyWarnings = studentService.getEarlyWarnings(userId);
        return ResponseResult.success(earlyWarnings, SuccessMessages.GET_EARLY_WARNINGS_SUCCESS, 200);
    }
    
    // 获取学生班级名称
    @GetMapping("/students/{studentId}/class")
    @RequireLogin
    public ResponseResult<String> getStudentClassName(@PathVariable Long studentId, HttpServletRequest requestContext) {
        String className = userService.getStudentClassName(studentId);
        if (className == null || className.isEmpty()) {
            throw new ResourceNotFoundException(ErrorMessages.STUDENT_NOT_FOUND);
        }
        return ResponseResult.success(className, SuccessMessages.GET_CLASS_NAME_SUCCESS, 200);
    }
    
    // 学生设置相关API端点
    
    @GetMapping("/student/profile")
    @RequireLogin
    public ResponseResult<Map<String, Object>> getStudentProfile(HttpServletRequest requestContext) {
        Long studentId = getCurrentUserId(requestContext);
        Map<String, Object> profile = studentService.getStudentProfile(studentId);
        if (profile == null) {
            throw new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND);
        }
        return ResponseResult.success(profile, SuccessMessages.GET_USER_INFO_SUCCESS, 200);
    }
    
    @PutMapping("/student/profile")
    @RequireLogin
    public ResponseResult<Boolean> updateStudentProfile(@RequestBody Map<String, Object> profileData, HttpServletRequest requestContext) {
        Long studentId = getCurrentUserId(requestContext);
        
        // 验证姓名不能为空
        if (profileData.containsKey("name")) {
            String name = (String) profileData.get("name");
            if (name == null || name.trim().isEmpty()) {
                return ResponseResult.failure(ErrorMessages.NAME_EMPTY, 400);
            }
        }
        
        // 验证邮箱格式
        if (profileData.containsKey("email")) {
            String email = (String) profileData.get("email");
            if (email != null && !email.trim().isEmpty()) {
                if (!email.contains("@") || !email.contains(".")) {
                    return ResponseResult.failure(ErrorMessages.EMAIL_INVALID, 400);
                }
            }
        }
        
        boolean result = studentService.updateStudentProfile(studentId, profileData);
        if (result) {
            return ResponseResult.success(true, SuccessMessages.PROFILE_UPDATE_SUCCESS, 200);
        } else {
            return ResponseResult.failure(ErrorMessages.UPDATE_FAILED, 400);
        }
    }
    
    @PostMapping("/student/change-password")
    @RequireLogin
    public ResponseResult<Boolean> changePassword(@RequestBody Map<String, String> passwordData, HttpServletRequest requestContext) {
        Long studentId = getCurrentUserId(requestContext);
        String currentPassword = passwordData.get("currentPassword");
        String newPassword = passwordData.get("newPassword");
        String confirmPassword = passwordData.get("confirmPassword");
        
        // 前端验证
        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            return ResponseResult.failure(ErrorMessages.PASSWORD_EMPTY, 400);
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseResult.failure(ErrorMessages.PASSWORD_EMPTY, 400);
        }
        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            return ResponseResult.failure(ErrorMessages.PASSWORD_EMPTY, 400);
        }
        if (!newPassword.equals(confirmPassword)) {
            return ResponseResult.failure(ErrorMessages.PASSWORD_MISMATCH, 400);
        }
        if (newPassword.length() < 8) {
            return ResponseResult.failure(ErrorMessages.PASSWORD_TOO_SHORT, 400);
        }
        
        boolean result = studentService.changePassword(studentId, currentPassword, newPassword, confirmPassword);
        if (result) {
            return ResponseResult.success(true, SuccessMessages.PASSWORD_CHANGED, 200);
        } else {
            return ResponseResult.failure(ErrorMessages.CURRENT_PASSWORD_ERROR, 400);
        }
    }
    
    @GetMapping("/student/notification-settings")
    @RequireLogin
    public ResponseResult<Map<String, Object>> getNotificationSettings(HttpServletRequest requestContext) {
        Long studentId = getCurrentUserId(requestContext);
        Map<String, Object> settings = studentService.getNotificationSettings(studentId);
        return ResponseResult.success(settings, SuccessMessages.NOTIFICATION_SETTINGS_SUCCESS, 200);
    }
    
    @PutMapping("/student/notification-settings")
    @RequireLogin
    public ResponseResult<Boolean> updateNotificationSettings(@RequestBody Map<String, Object> settings, HttpServletRequest requestContext) {
        Long studentId = getCurrentUserId(requestContext);
        boolean result = studentService.updateNotificationSettings(studentId, settings);
        if (result) {
            return ResponseResult.success(true, SuccessMessages.NOTIFICATION_SETTINGS_SUCCESS, 200);
        } else {
            return ResponseResult.failure(ErrorMessages.UPDATE_FAILED, 500);
        }
    }
    
    @GetMapping("/student/privacy-settings")
    @RequireLogin
    public ResponseResult<Map<String, Object>> getPrivacySettings(HttpServletRequest requestContext) {
        Long studentId = getCurrentUserId(requestContext);
        Map<String, Object> settings = studentService.getPrivacySettings(studentId);
        return ResponseResult.success(settings, SuccessMessages.PRIVACY_SETTINGS_SUCCESS, 200);
    }
    
    @PutMapping("/student/privacy-settings")
    @RequireLogin
    public ResponseResult<Boolean> updatePrivacySettings(@RequestBody Map<String, Object> settings, HttpServletRequest requestContext) {
        Long studentId = getCurrentUserId(requestContext);
        boolean result = studentService.updatePrivacySettings(studentId, settings);
        if (result) {
            return ResponseResult.success(true, SuccessMessages.PRIVACY_SETTINGS_SUCCESS, 200);
        } else {
            return ResponseResult.failure(ErrorMessages.UPDATE_FAILED, 500);
        }
    }
    
    @PostMapping("/student/upload-avatar")
    @RequireLogin
    public ResponseResult<Boolean> uploadAvatar(@RequestBody Map<String, String> avatarData, HttpServletRequest requestContext) {
        Long studentId = getCurrentUserId(requestContext);
        String avatarUrl = avatarData.get("avatar");
        boolean result = studentService.uploadAvatar(studentId, avatarUrl);
        if (result) {
            return ResponseResult.success(true, SuccessMessages.AVATAR_UPLOAD_SUCCESS, 200);
        } else {
            return ResponseResult.failure(ErrorMessages.UPDATE_FAILED, 500);
        }
    }
    
    @GetMapping("/student/export-data")
    @RequireLogin
    public ResponseResult<Map<String, Object>> exportStudentData(HttpServletRequest requestContext) {
        Long studentId = getCurrentUserId(requestContext);
        Map<String, Object> data = studentService.exportStudentData(studentId);
        return ResponseResult.success(data, SuccessMessages.EXPORT_DATA_SUCCESS, 200);
    }

    private void enrichTeacherInfo(Map<String, Object> result, Long teacherId) {
        if (teacherId == null) {
            return;
        }
        UserProfileDTO teacherProfile = userServiceProfileClient.getUserProfile(teacherId).orElse(null);
        if (teacherProfile != null) {
            result.put("teacherId", teacherProfile.getId());
            result.put("teacherName", teacherProfile.getName());
            return;
        }
        User teacher = userService.findById(teacherId);
        if (teacher != null) {
            result.put("teacherId", teacher.getId());
            result.put("teacherName", teacher.getName());
        }
    }
}

