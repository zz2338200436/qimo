package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.client.AuthServiceClient;
import com._202510007517.major_assignment.client.UserServiceProfileClient;
import com._202510007517.major_assignment.entity.Course;
import com._202510007517.major_assignment.entity.User;
import com._202510007517.major_assignment.entity.dto.StudentDashboardDTO;
import com._202510007517.major_assignment.mapper.StudentMapper;
import com._202510007517.major_assignment.service.CourseService;
import com._202510007517.major_assignment.service.StudentService;
import com._202510007517.major_assignment.service.UserService;
import com._202510007517.major_assignment.utils.PageUtils;
import com._202510007517.major_assignment.utils.TypeUtils;
import com._202510007517.platform.user.api.dto.UpdateUserProfileDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private CourseService courseService;
    
    @Autowired
    private UserService userService;

    @Autowired
    private UserServiceProfileClient userServiceProfileClient;

    @Autowired
    private AuthServiceClient authServiceClient;

    @Override
    @Transactional(readOnly = true)
    public List<Course> getStudentCourses(Long studentId) {
        return studentMapper.getStudentCourses(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStudentCoursesWithPagination(Long studentId, Integer page, Integer size, String sortBy, String order, String courseStatus, String semester, String courseCategory, String searchQuery) {
        int safePage = PageUtils.safePage(page, 1);
        int safeSize = PageUtils.safeSize(size, 10, 100);
        String safeSortBy = sanitizeSortBy(sortBy);
        String safeOrder = sanitizeOrder(order);
        String trimmedSearch = searchQuery == null ? null : searchQuery.trim();

        int totalElements = studentMapper.countStudentCoursesWithFilters(
                studentId, courseStatus, semester, courseCategory, trimmedSearch);

        int totalPages = (int) Math.ceil((double) totalElements / safeSize);
        totalPages = Math.max(totalPages, 1);

        safePage = Math.min(safePage, totalPages);
        int offset = (safePage - 1) * safeSize;

        List<Course> pagedCourses = studentMapper.getStudentCoursesWithFilters(
                studentId,
                offset,
                safeSize,
                safeSortBy,
                safeOrder,
                courseStatus,
                semester,
                courseCategory,
                trimmedSearch
        );

        Map<String, Object> result = new java.util.HashMap<>();

        result.put("content", pagedCourses);

        Map<String, Object> pageable = new java.util.HashMap<>();
        pageable.put("pageNumber", safePage - 1); // 前端从1开始，后端从0开始
        pageable.put("pageSize", safeSize);

        Map<String, Object> sort = new java.util.HashMap<>();
        sort.put("empty", false);
        sort.put("sorted", true);
        sort.put("unsorted", false);
        pageable.put("sort", sort);

        pageable.put("offset", offset);
        pageable.put("paged", true);
        pageable.put("unpaged", false);

        result.put("pageable", pageable);

        result.put("totalPages", totalPages);
        result.put("totalElements", totalElements);
        result.put("last", safePage >= totalPages);
        result.put("size", safeSize);
        result.put("number", safePage - 1); // 前端从1开始，后端从0开始
        result.put("sort", sort);
        result.put("first", safePage == 1);
        result.put("numberOfElements", pagedCourses.size());
        result.put("empty", pagedCourses.isEmpty());

        return result;
    }

    private String sanitizeSortBy(String sortBy) {
        if (sortBy == null) {
            return "c.id";
        }
        return switch (sortBy) {
            case "courseName" -> "c.course_name";
            case "courseStatus" -> "c.course_status";
            case "semester" -> "c.semester";
            case "courseCategory" -> "c.course_category";
            case "startDate" -> "c.start_date";
            case "endDate" -> "c.end_date";
            case "credit" -> "c.credit";
            default -> "c.id";
        };
    }

    private String sanitizeOrder(String order) {
        if (order == null) {
            return "DESC";
        }
        String upper = order.toUpperCase();
        return ("ASC".equals(upper) || "DESC".equals(upper)) ? upper : "DESC";
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getLearningStats(Long studentId, String semester, Long courseId, String timeRange) {
        // 获取原始学习统计数据
        Map<String, Object> rawStats = studentMapper.getLearningStats(studentId, null, null, null);
        
        // 构建符合前端需求的数据结构
        Map<String, Object> response = new HashMap<>();
        
        // 计算学习时长（基于数据库返回的分布数据，默认使用每日视图）
        List<Map<String, Object>> studyTimeRows = studentMapper.getStudyTimeDistribution(studentId, "daily", semester, courseId, timeRange);
        List<Double> studyTimeDistribution = studyTimeRows.stream()
                .map(row -> TypeUtils.safeDouble(row.get("study_time"), 0.0))
                .collect(Collectors.toList());
        double totalStudyTime = studyTimeDistribution.stream().mapToDouble(Double::doubleValue).sum();
        response.put("studyTime", totalStudyTime);
        response.put("studyTimeChange", 0);
        response.put("studyTimeDistribution", studyTimeDistribution);
        
        // 任务完成情况
        Object completedAssignmentsObj = rawStats.getOrDefault("completedAssignments", 0);
        Integer completedAssignments = TypeUtils.safeInt(completedAssignmentsObj, 0);
        response.put("completedTasks", completedAssignments);
        response.put("completedTasksChange", 0);
        
        // 平均成绩：从数据库获取真实成绩平均值
        Object averageScoreObj = rawStats.getOrDefault("averageScore", 0.0);
        Double averageScore = TypeUtils.safeDouble(averageScoreObj, 0.0);
        
        // 如果averageScore为0或看起来像完成率（<=100且是整数且>0），则从成绩表重新计算真实平均分
        if (averageScore == 0.0 || (averageScore > 0 && averageScore <= 100 && averageScore == averageScore.intValue())) {
            // 从成绩表获取真实平均分
            List<Map<String, Object>> scores = studentMapper.getScores(studentId, semester, courseId, timeRange);
            if (!scores.isEmpty()) {
                double totalScore = 0.0;
                int count = 0;
                for (Map<String, Object> score : scores) {
                    Object scoreObj = score.get("score");
                    if (scoreObj instanceof Number) {
                        double scoreValue = ((Number) scoreObj).doubleValue();
                        if (scoreValue > 0) { // 只计算有效的成绩
                            totalScore += scoreValue;
                            count++;
                        }
                    }
                }
                if (count > 0) {
                    averageScore = totalScore / count;
                }
            }
        }
        
        response.put("averageScore", Math.round(averageScore * 10.0) / 10.0); // 保留一位小数
        response.put("averageScoreChange", 0.0);
        
        // 获取知识点列表
        List<Map<String, Object>> knowledgePoints = studentMapper.getKnowledgePoints(studentId, semester, courseId, timeRange);
        
        // 知识点掌握情况 - 计算所有知识点的平均掌握度（0-100）
        double knowledgeMastery = 0.0;
        if (!knowledgePoints.isEmpty()) {
            // 过滤出有掌握度记录的知识点
            List<Double> masteryScores = knowledgePoints.stream()
                    .map(point -> {
                        Object masteryObj = point.get("mastery");
                        if (masteryObj instanceof Number) {
                            double score = ((Number) masteryObj).doubleValue();
                            // 确保掌握度在0-100范围内
                            return Math.max(0.0, Math.min(100.0, score));
                        }
                        return null;
                    })
                    .filter(score -> score != null && score > 0)
                    .collect(Collectors.toList());
            
            if (!masteryScores.isEmpty()) {
                knowledgeMastery = masteryScores.stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);
            }
        }
        response.put("knowledgeMastery", Math.round(knowledgeMastery * 10.0) / 10.0); // 保留一位小数
        response.put("knowledgeMasteryChange", 0.0);
        
        // 转换知识点数据格式，添加practiceCount字段
        List<Map<String, Object>> formattedKnowledgePoints = knowledgePoints.stream().map(point -> {
            Map<String, Object> formatted = new java.util.HashMap<>(point);
            // 添加practiceCount字段（保持0，后续可扩展为真实练习次数）
            formatted.put("practiceCount", 0);
            return formatted;
        }).collect(Collectors.toList());
        response.put("knowledgePoints", formattedKnowledgePoints);
        
        // 学习计划数据（当前无真实计划，返回空结构）
        Map<String, Object> studyPlan = new java.util.HashMap<>();
        studyPlan.put("completionPercentage", 0);
        studyPlan.put("items", List.of());
        response.put("studyPlan", studyPlan);
        
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getKnowledgePoints(Long studentId, String semester, Long courseId, String timeRange) {
        return studentMapper.getKnowledgePoints(studentId, semester, courseId, timeRange);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentDashboardDTO getStudentPerformance(Long studentId) {
        // 创建StudentDashboardDTO对象
        StudentDashboardDTO result = new StudentDashboardDTO();
        
        // 从数据库获取真实数据（getStudentPerformance不需要筛选，传递null）
        Map<String, Object> rawStats = studentMapper.getLearningStats(studentId, null, null, null);
        Map<String, Object> performanceStats = studentMapper.getStudentPerformance(studentId);
        
        // 设置课程相关数据
        List<Course> courses = studentMapper.getStudentCourses(studentId);
        // 直接使用实际课程列表的数量，避免统计表或聚合 SQL 出错导致数量不一致
        int courseCount = courses != null ? courses.size() : 0;
        result.setCourseCount(courseCount);
        result.setCourseCountChange(0);
        
        // 转换Course为CourseDTO并设置到result中
        List<StudentDashboardDTO.CourseDTO> courseDTOs = courses.stream()
            .map(course -> {
                StudentDashboardDTO.CourseDTO courseDTO = new StudentDashboardDTO.CourseDTO();
                courseDTO.setId(course.getId());
                courseDTO.setName(course.getCourseName());
                courseDTO.setTeacherName(course.getTeacherName());
                // 从数据库计算课程进度：基于作业和考试的完成率
                Integer progress = studentMapper.getCourseProgress(studentId, course.getId());
                courseDTO.setProgress(progress != null ? progress : 0);
                return courseDTO;
            })
            .collect(Collectors.toList());
        result.setCourses(courseDTOs);
        
        // 设置作业和考试数据
        int pendingAssignments = TypeUtils.safeInt(performanceStats.get("pendingAssignments"),
                TypeUtils.safeInt(rawStats.getOrDefault("pendingAssignments", 0), 0));
        result.setPendingAssignments(Math.max(pendingAssignments, 0));
        result.setPendingAssignmentsChange(0);
        
        int upcomingExams = TypeUtils.safeInt(performanceStats.getOrDefault("upcomingExams", 0), 0);
        result.setUpcomingExams(Math.max(upcomingExams, 0));
        result.setUpcomingExamsChange(0);
        
        // 设置整体进度：基于完成率，而不是分数
        double progress = TypeUtils.safeDouble(performanceStats.getOrDefault("overallProgress",
                rawStats.getOrDefault("averageScore", 0.0)), 0.0);
        // 确保进度不超过100%
        progress = Math.max(0.0, Math.min(progress, 100.0));
        
        result.setOverallProgress(progress);
        result.setOverallProgressChange(0.0);
        
        // 从数据库获取学习进度数据
        List<Map<String, Object>> progressTrend = studentMapper.getLearningProgressTrend(studentId);
        List<String> weeks = new ArrayList<>();
        List<Integer> progressData = new ArrayList<>();
        for (Map<String, Object> item : progressTrend) {
            weeks.add(TypeUtils.safeString(item.getOrDefault("week", ""), ""));
            progressData.add(TypeUtils.safeInt(item.getOrDefault("progress", 0), 0));
        }
        
        result.setLearningProgressWeeks(weeks);
        result.setLearningProgressData(progressData);
        
        // 设置最近活动数据（取最近的成绩记录）
        List<Map<String, Object>> scores = studentMapper.getScores(studentId, null, null, null);
        List<StudentDashboardDTO.RecentActivityDTO> recentActivities = scores.stream()
                .limit(5)
                .map(score -> {
                    String type = String.valueOf(score.getOrDefault("type", "unknown"));
                    String name = String.valueOf(score.getOrDefault("name", "未命名"));
                    String courseName = String.valueOf(score.getOrDefault("course", "未知课程"));
                    String submitDate = score.get("submitDate") != null ? score.get("submitDate").toString() : "";
                    return createRecentActivity(type, courseName + " - " + name, submitDate);
                })
                .collect(Collectors.toList());
        result.setRecentActivities(recentActivities);
        
        // 设置成绩分布数据（基于真实成绩）
        List<StudentDashboardDTO.GradesDistributionDTO> gradesDistribution = buildGradesDistribution(scores);
        result.setGradesDistribution(gradesDistribution);
        
        return result;
    }
    
    // 辅助方法：创建模拟课程DTO
    private StudentDashboardDTO.CourseDTO createMockCourseDTO(Long id, String name, String teacherName, int progress) {
        StudentDashboardDTO.CourseDTO courseDTO = new StudentDashboardDTO.CourseDTO();
        courseDTO.setId(id);
        courseDTO.setName(name);
        courseDTO.setTeacherName(teacherName);
        courseDTO.setProgress(progress);
        return courseDTO;
    }
    
    // 辅助方法：创建最近活动对象
    private StudentDashboardDTO.RecentActivityDTO createRecentActivity(String type, String description, String timestamp) {
        StudentDashboardDTO.RecentActivityDTO activity = new StudentDashboardDTO.RecentActivityDTO();
        activity.setType(type);
        activity.setDescription(description);
        activity.setTimestamp(timestamp);
        return activity;
    }
    
    // 辅助方法：创建成绩分布对象
    private StudentDashboardDTO.GradesDistributionDTO createGradesDistribution(String name, int value) {
        StudentDashboardDTO.GradesDistributionDTO distribution = new StudentDashboardDTO.GradesDistributionDTO();
        distribution.setName(name);
        distribution.setValue(value);
        return distribution;
    }

    // 根据成绩记录构建成绩分布
    private List<StudentDashboardDTO.GradesDistributionDTO> buildGradesDistribution(List<Map<String, Object>> scores) {
        Map<String, Integer> counter = new HashMap<>();
        counter.put("优秀", 0);
        counter.put("良好", 0);
        counter.put("中等", 0);
        counter.put("及格", 0);
        counter.put("不及格", 0);

        for (Map<String, Object> score : scores) {
            double value = TypeUtils.safeDouble(score.getOrDefault("score", 0), 0.0);
            if (value >= 90) {
                counter.computeIfPresent("优秀", (k, v) -> v + 1);
            } else if (value >= 80) {
                counter.computeIfPresent("良好", (k, v) -> v + 1);
            } else if (value >= 70) {
                counter.computeIfPresent("中等", (k, v) -> v + 1);
            } else if (value >= 60) {
                counter.computeIfPresent("及格", (k, v) -> v + 1);
            } else {
                counter.computeIfPresent("不及格", (k, v) -> v + 1);
            }
        }

        List<StudentDashboardDTO.GradesDistributionDTO> result = new ArrayList<>();
        counter.forEach((name, value) -> result.add(createGradesDistribution(name, value)));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getScores(Long studentId, String semester, Long courseId, String timeRange) {
        return studentMapper.getScores(studentId, semester, courseId, timeRange);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getStudyTimeDistribution(Long studentId, String type, String semester, Long courseId, String timeRange) {
        return studentMapper.getStudyTimeDistribution(studentId, type, semester, courseId, timeRange);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEarlyWarnings(Long studentId) {
        // 实现getEarlyWarnings方法
        return new ArrayList<>();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getKnowledgePointDetail(Long studentId, Long knowledgePointId) {
        // 实现getKnowledgePointDetail方法
        return new HashMap<>();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStudentProfile(Long studentId) {
        // 获取用户基本信息
        User user = userService.findById(studentId);
        if (user == null) {
            return null;
        }
        
        // 构建返回的Map
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("name", user.getName());
        profile.put("email", user.getEmail());
        profile.put("phone", user.getPhone());
        profile.put("avatar", user.getAvatar());
        profile.put("studentId", user.getId()); // studentId就是用户ID
        
        // 获取学生的班级信息（包括专业、年级、班级名称）
        Map<String, Object> classInfo = studentMapper.getStudentClassInfo(studentId);
        if (classInfo != null) {
            profile.put("className", classInfo.get("className"));
            profile.put("grade", classInfo.get("grade"));
            profile.put("major", classInfo.get("major"));
        } else {
            // 如果没有班级信息，设置默认值
            profile.put("className", "");
            profile.put("grade", "");
            profile.put("major", "");
        }
        
        return profile;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStudentProfile(Long studentId, Map<String, Object> profileData) {
        // 获取当前用户
        User user = userService.findById(studentId);
        if (user == null) {
            return false;
        }

        // 更新用户信息
        boolean hasUpdate = false;
        
        if (profileData.containsKey("name")) {
            String name = (String) profileData.get("name");
            if (name != null && !name.trim().isEmpty()) {
                user.setName(name.trim());
                hasUpdate = true;
            }
        }
        
        if (profileData.containsKey("email")) {
            String email = (String) profileData.get("email");
            if (email != null && !email.trim().isEmpty()) {
                // 简单的邮箱格式验证
                if (email.contains("@") && email.contains(".")) {
                    user.setEmail(email.trim());
                    hasUpdate = true;
                }
            }
        }
        
        if (profileData.containsKey("phone")) {
            String phone = (String) profileData.get("phone");
            if (phone != null && !phone.trim().isEmpty()) {
                user.setPhone(phone.trim());
                hasUpdate = true;
            }
        }
        
        // 注意：User实体类没有major、grade、className字段，这些信息存储在关联表中
        // major信息在majors表中，通过course_classes关联
        // grade（年级）在course_classes表的year字段
        // className在course_classes表的class_name字段
        // 这些信息通常由管理员或系统设置，学生不能直接修改
        
        if (!hasUpdate) {
            return false;
        }

        UpdateUserProfileDTO request = new UpdateUserProfileDTO();
        request.setName(user.getName());
        request.setEmail(user.getEmail());
        request.setPhone(user.getPhone());
        return userServiceProfileClient.updateUserProfile(studentId, request).isPresent();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changePassword(Long studentId, String currentPassword, String newPassword, String confirmPassword) {
        // 验证新密码和确认密码是否一致
        if (!newPassword.equals(confirmPassword)) {
            return false;
        }

        // 验证新密码长度
        if (newPassword == null || newPassword.length() < 8) {
            return false;
        }

        if (currentPassword.equals(newPassword)) {
            return false;
        }

        // 验证密码强度：至少包含字母和数字
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : newPassword.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            }
            if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        if (!hasLetter || !hasDigit) {
            return false;
        }

        return authServiceClient.changePassword(studentId, currentPassword, newPassword);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationSettings(Long studentId) {
        Map<String, Object> settings = new HashMap<>();
        // 默认全部为 false，表示未开启任何通知开关
        settings.put("emailNotifications", false);
        settings.put("smsNotifications", false);
        settings.put("webNotifications", false);
        settings.put("assignmentNotifications", false);
        settings.put("gradeNotifications", false);
        settings.put("courseNotifications", false);
        settings.put("systemNotifications", false);
        settings.put("reminderNotifications", false);
        return settings;
    }

    @Override
    public boolean updateNotificationSettings(Long studentId, Map<String, Object> settings) {
        // 这里简单返回true，实际应该保存到数据库
        // 后续可以添加数据库操作来持久化设置
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPrivacySettings(Long studentId) {
        Map<String, Object> settings = new HashMap<>();
        // 默认全部为 false，表示不开启任何隐私相关分享/采集
        settings.put("shareProfile", false);
        settings.put("shareAchievements", false);
        settings.put("dataCollection", false);
        return settings;
    }

    @Override
    public boolean updatePrivacySettings(Long studentId, Map<String, Object> settings) {
        // 这里简单返回true，实际应该保存到数据库
        // 后续可以添加数据库操作来持久化设置
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean uploadAvatar(Long studentId, String avatarUrl) {
        UpdateUserProfileDTO request = new UpdateUserProfileDTO();
        request.setAvatar(avatarUrl);
        return userServiceProfileClient.updateUserProfile(studentId, request).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> exportStudentData(Long studentId) {
        Map<String, Object> data = new HashMap<>();
        
        // 添加学生基本信息
        User user = userService.findById(studentId);
        if (user != null) {
            Map<String, Object> basicInfo = new HashMap<>();
            basicInfo.put("id", user.getId());
            basicInfo.put("username", user.getUsername());
            basicInfo.put("name", user.getName());
            basicInfo.put("email", user.getEmail());
            basicInfo.put("phone", user.getPhone());
            // 注意：User实体类没有major、grade和classId字段，这些信息可能存储在其他表中
            data.put("basicInfo", basicInfo);
        }
        
        // 添加课程信息
        List<Course> courses = getStudentCourses(studentId);
        data.put("courses", courses);
        
        // 添加学习统计
        data.put("learningStats", getLearningStats(studentId, null, null, null));
        
        // 添加成绩信息
        data.put("scores", getScores(studentId, null, null, null));
        
        return data;
    }
}
