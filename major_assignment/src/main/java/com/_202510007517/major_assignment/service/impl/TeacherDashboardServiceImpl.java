package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.entity.Course;
import com._202510007517.major_assignment.entity.EarlyWarning;
import com._202510007517.major_assignment.entity.User;
import com._202510007517.major_assignment.entity.dto.ScoreTrendDTO;
import com._202510007517.major_assignment.entity.dto.StudentLearningSummaryDTO;
import com._202510007517.major_assignment.entity.dto.TeacherDashboardDTO;
import com._202510007517.major_assignment.mapper.CourseMapper;
import com._202510007517.major_assignment.mapper.AssignmentMapper;
import com._202510007517.major_assignment.mapper.AssignmentSubmissionMapper;
import com._202510007517.major_assignment.mapper.EarlyWarningMapper;
import com._202510007517.major_assignment.mapper.ExamMapper;
import com._202510007517.major_assignment.mapper.ExamSubmissionMapper;
import com._202510007517.major_assignment.mapper.StudentMapper;
import com._202510007517.major_assignment.mapper.UserMapper;
import com._202510007517.major_assignment.service.TeacherDashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

@Service
public class TeacherDashboardServiceImpl implements TeacherDashboardService {
    
    private static final Logger logger = LoggerFactory.getLogger(TeacherDashboardServiceImpl.class);
    
    @Autowired
    private CourseMapper courseMapper;
    
    @Autowired
    private AssignmentMapper assignmentMapper;
    
    @Autowired
    private AssignmentSubmissionMapper assignmentSubmissionMapper;
    
    @Autowired
    private EarlyWarningMapper earlyWarningMapper;
    
    @Autowired
    private ExamMapper examMapper;
    
    @Autowired
    private ExamSubmissionMapper examSubmissionMapper;
    
    @Autowired
    private StudentMapper studentMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    
    @Override
    public TeacherDashboardDTO getDashboardData(Long teacherId, Long classId, Long courseId, String timeRange) {
        TeacherDashboardDTO dashboard = new TeacherDashboardDTO();
        
        // 从数据库获取教师的课程列表
        List<Course> courses;
        if (courseId != null) {
            // 根据课程ID获取特定课程
            Course course = courseMapper.findById(courseId);
            courses = course != null ? Collections.singletonList(course) : new ArrayList<>();
        } else {
            // 获取教师的所有课程
            courses = courseMapper.findByTeacherId(teacherId);
        }
        int totalCourses = courses.size();
        dashboard.setTotalCourses(totalCourses);
        
        // 模拟课程数量变化（较上周）
        int courseCountLastWeek = totalCourses > 0 ? totalCourses - 1 : 0;
        dashboard.setTotalCoursesChange(totalCourses - courseCountLastWeek);
        
        // 计算总学生数
        int totalStudentCount = 0;
        List<String> courseNames = new ArrayList<>();
        List<Integer> studentCountsPerCourse = new ArrayList<>();
        List<Double> averageScores = new ArrayList<>();
        
        // 获取学生ID列表
        Set<Long> uniqueStudentIds = new HashSet<>();
        try {
            if (courseId != null && classId != null) {
                // 根据课程ID和班级ID的组合获取学生ID列表
                List<Long> studentIds = courseMapper.getStudentIdsByCourseIdAndClassId(courseId, classId);
                if (studentIds != null) {
                    uniqueStudentIds.addAll(studentIds);
                }
            } else if (courseId != null) {
                // 根据课程ID获取学生ID列表
                List<Long> studentIds = courseMapper.getStudentIdsByCourseId(courseId);
                if (studentIds != null) {
                    uniqueStudentIds.addAll(studentIds);
                }
            } else if (classId != null) {
                // 根据班级ID获取学生ID列表
                List<Map<String, Object>> students = studentMapper.getStudentsByClassId(classId);
                if (students != null) {
                    for (Map<String, Object> student : students) {
                        Object studentIdObj = student.get("id");
                        if (studentIdObj != null) {
                            Long studentId = Long.parseLong(studentIdObj.toString());
                            uniqueStudentIds.add(studentId);
                        }
                    }
                }
            } else {
                // 获取教师所有课程的所有学生ID，使用更高效的getStudentIdsByClassTeacherId方法
                List<Long> studentIds = courseMapper.getStudentIdsByClassTeacherId(teacherId);
                if (studentIds != null) {
                    uniqueStudentIds.addAll(studentIds);
                }
            }
        } catch (Exception e) {
            logger.error("获取学生ID列表失败", e);
        }
        totalStudentCount = uniqueStudentIds.size();
        
        for (Course course : courses) {
            courseNames.add(course.getCourseName());
            
            // 获取该课程的学生数，考虑班级筛选
            Integer studentCount;
            if (classId != null) {
                studentCount = courseMapper.getStudentCountByCourseIdAndClassId(course.getId(), classId);
            } else {
                studentCount = courseMapper.getStudentCountByCourseId(course.getId());
            }
            if (studentCount != null) {
                studentCountsPerCourse.add(studentCount);
            } else {
                studentCountsPerCourse.add(0);
            }
            
            // 计算每门课程的平均成绩，考虑班级筛选
            Double courseAverageScore;
            if (classId != null) {
                courseAverageScore = courseMapper.getCourseAverageScoreByClassId(course.getId(), classId);
            } else {
                courseAverageScore = courseMapper.getCourseAverageScore(course.getId());
            }
            if (courseAverageScore != null) {
                averageScores.add(courseAverageScore);
            } else {
                averageScores.add(0.0);
            }
        }
        
        dashboard.setTotalStudents(totalStudentCount);
        dashboard.setAverageScores(averageScores);
        
        // 模拟学生数量变化（较上周）
        int studentCountLastWeek = totalStudentCount > 10 ? totalStudentCount - 12 : totalStudentCount;
        dashboard.setTotalStudentsChange(totalStudentCount - studentCountLastWeek);
        
        dashboard.setCourseNames(courseNames);
        
        // 获取未批改作业数量
        Integer ungradedSubmissions = assignmentSubmissionMapper.countUngradedSubmissionsByTeacher(teacherId);
        int pendingAssignments = ungradedSubmissions != null ? ungradedSubmissions : 0;
        dashboard.setPendingAssignments(pendingAssignments);
        
        // 模拟待批改作业变化（较昨日）
        int pendingAssignmentsYesterday = pendingAssignments > 10 ? pendingAssignments + 8 : pendingAssignments;
        dashboard.setPendingAssignmentsChange(pendingAssignments - pendingAssignmentsYesterday);
        
        // 获取待批改考试数量
        Integer pendingExamsFromDb = examSubmissionMapper.countUngradedExamsByTeacher(teacherId);
        int examsCount = pendingExamsFromDb != null ? pendingExamsFromDb : 0;
        dashboard.setPendingExams(examsCount);
        
        // 模拟即将到来的考试变化（较昨日）
        int pendingExamsYesterday = examsCount > 5 ? examsCount - 2 : examsCount;
        dashboard.setPendingExamsChange(examsCount - pendingExamsYesterday);
        
        // 获取预警数量（可选：如果需要预警数量，可以添加一个新的字段）
        List<EarlyWarning> unresolvedWarnings = earlyWarningMapper.findUnresolvedByTeacherId(teacherId);
        int warningCount = unresolvedWarnings != null ? unresolvedWarnings.size() : 0;
        dashboard.setWarningCount(warningCount);
        
        // 模拟预警数量变化（较昨日）
        int warningCountYesterday = warningCount > 5 ? warningCount - 3 : warningCount;
        dashboard.setWarningCountChange(warningCount - warningCountYesterday);
        
        // 计算即将截止的作业数量
        Integer upcomingAssignments = assignmentMapper.countUpcomingAssignmentsByTeacher(teacherId);
        
        // 计算即将截止的考试数量
        Integer upcomingExams = examMapper.countUpcomingExamsByTeacher(teacherId);
        
        // 计算即将截止的任务总数（作业+考试）
        int upcomingDeadlines = (upcomingAssignments != null ? upcomingAssignments : 0) + (upcomingExams != null ? upcomingExams : 0);
        dashboard.setUpcomingDeadlines(upcomingDeadlines);
        
        // 模拟即将截止任务变化（较昨日）
        int upcomingDeadlinesYesterday = upcomingDeadlines > 5 ? upcomingDeadlines - 3 : upcomingDeadlines;
        dashboard.setUpcomingDeadlinesChange(upcomingDeadlines - upcomingDeadlinesYesterday);
        
        // 计算未交作业数量（使用数据库真实数据）
        Integer missingCountFromDb = assignmentMapper.countMissingSubmissionsByTeacher(teacherId, classId, courseId);
        int missingCount = missingCountFromDb != null ? missingCountFromDb : 0;
        dashboard.setMissingSubmissions(missingCount);
        
        // 模拟未交作业学生变化（较昨日）
        int missingSubmissionsYesterday = missingCount > 10 ? missingCount - 5 : missingCount;
        dashboard.setMissingSubmissionsChange(missingCount - missingSubmissionsYesterday);
        
        // 设置ECharts所需的平均分数据（从数据库获取真实数据）
        List<Double> averageScoresList = new ArrayList<>();
        for (Course course : courses) {
            // 获取课程的具体成绩列表
            List<Double> scores = courseMapper.getCourseScores(course.getId());
            
            // 计算平均分
            Double avgScore = courseMapper.getCourseAverageScore(course.getId());
            
            // 如果avgScore为null，手动计算
            if (avgScore == null) {
                if (scores != null && !scores.isEmpty()) {
                    double sum = 0.0;
                    int validScoreCount = 0;
                    for (Double score : scores) {
                        if (score != null) {
                            sum += score;
                            validScoreCount++;
                        }
                    }
                    if (validScoreCount > 0) {
                        avgScore = sum / validScoreCount;
                    } else {
                        avgScore = 0.0;
                    }
                } else {
                    avgScore = 0.0;
                }
            } else {
                // 输出详细计算过程
                if (scores != null && !scores.isEmpty()) {
                    double sum = 0.0;
                    int validScoreCount = 0;
                    for (Double score : scores) {
                        if (score != null) {
                            sum += score;
                            validScoreCount++;
                        }
                    }
                }
            }
            
            averageScoresList.add(avgScore != null ? avgScore : 0.0);
        }
        
        // 计算总体平均成绩
        if (averageScoresList.size() > 0) {
            double totalSum = 0.0;
            for (Double avg : averageScoresList) {
                totalSum += avg;
            }
            double overallAvg = totalSum / averageScoresList.size();
        }
        
        dashboard.setAverageScores(averageScoresList);
        
        // 设置提交率数据（从数据库获取真实数据）
        // 改进：显示每天的实际提交数量，而不是百分比（因为每天的预期提交数不固定）
        List<String> submissionRateDays = new ArrayList<>();
        List<Integer> submissionRates = new ArrayList<>();
        
        // 获取过去7天的日期名称
        List<String> dayNames = Arrays.asList("周日", "周一", "周二", "周三", "周四", "周五", "周六");
        
        // 构建日期到提交数的映射
        Map<String, Integer> submissionCountMap = new HashMap<>();
        int maxSubmissions = 0; // 记录最大提交数，用于计算相对百分比
        try {
            List<Map<String, Object>> submissionCounts = assignmentSubmissionMapper.getSubmissionCountsByDay(teacherId);
            if (submissionCounts != null) {
                for (Map<String, Object> count : submissionCounts) {
                    String date = (String) count.get("submission_date");
                    Integer submissionCount = ((Number) count.get("submission_count")).intValue();
                    submissionCountMap.put(date, submissionCount);
                    if (submissionCount > maxSubmissions) {
                        maxSubmissions = submissionCount;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("获取提交统计数据失败", e);
        }
        
        // 如果最近7天没有提交，查询历史最大提交数作为参考
        if (maxSubmissions == 0) {
            try {
                // 查询历史上单日最大提交数
                Integer historicalMax = assignmentSubmissionMapper.getMaxDailySubmissionsByTeacher(teacherId);
                if (historicalMax != null && historicalMax > 0) {
                    maxSubmissions = historicalMax;
                } else {
                    maxSubmissions = totalStudentCount > 0 ? totalStudentCount : 10; // 兜底值
                }
            } catch (Exception e) {
                maxSubmissions = totalStudentCount > 0 ? totalStudentCount : 10;
            }
        }
        
        // 生成过去7天的提交率数据
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar today = Calendar.getInstance();
        today.setTime(new Date());
        
        for (int i = 6; i >= 0; i--) {
            Calendar currentDay = Calendar.getInstance();
            currentDay.setTime(today.getTime());
            currentDay.add(Calendar.DAY_OF_YEAR, -i);
            
            String dateStr = dateFormat.format(currentDay.getTime());
            int dayOfWeek = currentDay.get(Calendar.DAY_OF_WEEK) - 1;
            // 确保dayOfWeek在有效范围内
            dayOfWeek = Math.min(dayOfWeek, dayNames.size() - 1);
            String dayName = dayNames.get(dayOfWeek);
            submissionRateDays.add(dayName);
            
            // 计算相对提交率：当天提交数 / 最大提交数 * 100
            // 这样可以更好地展示提交趋势的相对变化
            Integer submissions = submissionCountMap.getOrDefault(dateStr, 0);
            int submissionRate;
            if (maxSubmissions > 0) {
                submissionRate = (int) Math.round((submissions / (double) maxSubmissions) * 100);
                submissionRate = Math.max(0, Math.min(submissionRate, 100)); // 0-100 保护
            } else {
                submissionRate = 0;
            }
            submissionRates.add(submissionRate);
        }
        
        // 确保始终有数据返回，即使数据库查询失败
        if (submissionRateDays.isEmpty()) {
            submissionRateDays = Arrays.asList("周日", "周一", "周二", "周三", "周四", "周五", "周六");
            submissionRates = Arrays.asList(0, 0, 0, 0, 0, 0, 0);
        }
        
        dashboard.setSubmissionRateDays(submissionRateDays);
        dashboard.setSubmissionRates(submissionRates);
        
        // 从业务表动态获取最近活动数据
        List<TeacherDashboardDTO.RecentActivityDTO> recentActivities = new ArrayList<>();
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        try {
            // 1. 获取最近的作业提交
            List<Map<String, Object>> recentAssignmentSubmissions = assignmentSubmissionMapper.getRecentSubmissionsByTeacher(teacherId, 5);
            if (recentAssignmentSubmissions != null) {
                for (Map<String, Object> sub : recentAssignmentSubmissions) {
                    TeacherDashboardDTO.RecentActivityDTO activity = new TeacherDashboardDTO.RecentActivityDTO();
                    activity.setActivityType("作业提交");
                    
                    Object studentIdObj = sub.get("student_id");
                    if (studentIdObj instanceof Number) {
                        activity.setStudentId(((Number) studentIdObj).longValue());
                    }
                    
                    String studentName = sub.get("student_name") != null ? sub.get("student_name").toString() : "未知学生";
                    activity.setStudentName(studentName);
                    
                    String assignmentTitle = sub.get("assignment_title") != null ? sub.get("assignment_title").toString() : "作业";
                    activity.setDetails(studentName + " 提交了作业《" + assignmentTitle + "》");
                    
                    Object submissionDate = sub.get("submission_date");
                    if (submissionDate instanceof Date) {
                        activity.setActivityDate(isoFormat.format((Date) submissionDate));
                    } else {
                        activity.setActivityDate(isoFormat.format(new Date()));
                    }
                    
                    recentActivities.add(activity);
                }
            }
            
            // 2. 获取最近的考试提交
            List<Map<String, Object>> recentExamSubmissions = examSubmissionMapper.getRecentSubmissionsByTeacher(teacherId, 3);
            if (recentExamSubmissions != null) {
                for (Map<String, Object> sub : recentExamSubmissions) {
                    TeacherDashboardDTO.RecentActivityDTO activity = new TeacherDashboardDTO.RecentActivityDTO();
                    activity.setActivityType("考试提交");
                    
                    Object studentIdObj = sub.get("student_id");
                    if (studentIdObj instanceof Number) {
                        activity.setStudentId(((Number) studentIdObj).longValue());
                    }
                    
                    String studentName = sub.get("student_name") != null ? sub.get("student_name").toString() : "未知学生";
                    activity.setStudentName(studentName);
                    
                    String examTitle = sub.get("exam_title") != null ? sub.get("exam_title").toString() : "考试";
                    activity.setDetails(studentName + " 提交了考试《" + examTitle + "》");
                    
                    Object submissionDate = sub.get("submission_date");
                    if (submissionDate instanceof Date) {
                        activity.setActivityDate(isoFormat.format((Date) submissionDate));
                    } else {
                        activity.setActivityDate(isoFormat.format(new Date()));
                    }
                    
                    recentActivities.add(activity);
                }
            }
            
            // 3. 获取最近的学情预警
            List<EarlyWarning> recentWarnings = earlyWarningMapper.findRecentByTeacherId(teacherId, 3);
            if (recentWarnings != null) {
                for (EarlyWarning warning : recentWarnings) {
                    TeacherDashboardDTO.RecentActivityDTO activity = new TeacherDashboardDTO.RecentActivityDTO();
                    activity.setActivityType("学情预警");
                    activity.setStudentId(warning.getStudentId());
                    
                    // 获取学生姓名
                    String studentName = "未知学生";
                    try {
                        User user = userMapper.findById(warning.getStudentId());
                        if (user != null) {
                            studentName = user.getName();
                        }
                    } catch (Exception e) {
                        // 忽略
                    }
                    activity.setStudentName(studentName);
                    
                    String warningType = warning.getWarningType() != null ? warning.getWarningType() : "学习";
                    activity.setDetails(studentName + " 触发了" + warningType + "预警");
                    
                    if (warning.getTriggerDate() != null) {
                        activity.setActivityDate(isoFormat.format(warning.getTriggerDate()));
                    } else {
                        activity.setActivityDate(isoFormat.format(new Date()));
                    }
                    
                    recentActivities.add(activity);
                }
            }
            
            // 4. 获取最近批改的作业
            List<Map<String, Object>> recentGraded = assignmentSubmissionMapper.getRecentGradedByTeacher(teacherId, 3);
            if (recentGraded != null) {
                for (Map<String, Object> graded : recentGraded) {
                    TeacherDashboardDTO.RecentActivityDTO activity = new TeacherDashboardDTO.RecentActivityDTO();
                    activity.setActivityType("作业批改");
                    
                    Object studentIdObj = graded.get("student_id");
                    if (studentIdObj instanceof Number) {
                        activity.setStudentId(((Number) studentIdObj).longValue());
                    }
                    
                    String studentName = graded.get("student_name") != null ? graded.get("student_name").toString() : "未知学生";
                    activity.setStudentName(studentName);
                    
                    String assignmentTitle = graded.get("assignment_title") != null ? graded.get("assignment_title").toString() : "作业";
                    Object scoreObj = graded.get("score");
                    String scoreStr = scoreObj != null ? scoreObj.toString() : "未评分";
                    activity.setDetails(studentName + " 的作业《" + assignmentTitle + "》已批改，得分: " + scoreStr);
                    
                    Object updatedAt = graded.get("updated_at");
                    if (updatedAt instanceof Date) {
                        activity.setActivityDate(isoFormat.format((Date) updatedAt));
                    } else {
                        activity.setActivityDate(isoFormat.format(new Date()));
                    }
                    
                    recentActivities.add(activity);
                }
            }
            
            // 按时间倒序排序，取前10条
            recentActivities.sort((a, b) -> {
                try {
                    Date dateA = isoFormat.parse(a.getActivityDate());
                    Date dateB = isoFormat.parse(b.getActivityDate());
                    return dateB.compareTo(dateA);
                } catch (Exception e) {
                    return 0;
                }
            });
            
            if (recentActivities.size() > 10) {
                recentActivities = new ArrayList<>(recentActivities.subList(0, 10));
            }
            
        } catch (Exception e) {
            logger.error("获取最近活动数据失败", e);
        }
        
        // 如果没有数据，显示提示信息
        if (recentActivities.isEmpty()) {
            TeacherDashboardDTO.RecentActivityDTO activity = new TeacherDashboardDTO.RecentActivityDTO();
            activity.setActivityType("系统提示");
            activity.setStudentName("系统");
            activity.setActivityDate(isoFormat.format(new Date()));
            activity.setDetails("暂无最近活动记录");
            recentActivities.add(activity);
        }
        
        // 将最近活动添加到dashboard对象中
        dashboard.setRecentActivities(recentActivities);
        
        // 模拟待批改作业数量变化（较上周）
        int pendingAssignmentsLastWeek = pendingAssignments > 0 ? pendingAssignments - 2 : 0;
        dashboard.setPendingAssignmentsChange(pendingAssignments - pendingAssignmentsLastWeek);
        
        // 模拟待发布考试数量变化（较上周）
        int pendingExamsLastWeek = examsCount > 0 ? examsCount - 1 : 0;
        dashboard.setPendingExamsChange(examsCount - pendingExamsLastWeek);
        
        // 模拟未交作业学生数量变化（较上周）
        int missingSubmissionsLastWeek = missingCount > 0 ? missingCount - 1 : 0;
        dashboard.setMissingSubmissionsChange(missingCount - missingSubmissionsLastWeek);
        
        // 模拟即将截止任务数量变化（较上周）
        int upcomingDeadlinesLastWeek = upcomingDeadlines > 0 ? upcomingDeadlines - 1 : 0;
        dashboard.setUpcomingDeadlinesChange(upcomingDeadlines - upcomingDeadlinesLastWeek);
        
        // 模拟预警数量变化（较上周）
        int warningCountLastWeek = warningCount > 0 ? warningCount - 1 : 0;
        dashboard.setWarningCountChange(warningCount - warningCountLastWeek);
        
        return dashboard;
    }

    @Override
    public StudentLearningSummaryDTO getStudentLearningSummary(Long teacherId, Long classId, Long courseId, String timeRange) {
        StudentLearningSummaryDTO summary = new StudentLearningSummaryDTO();
        List<StudentLearningSummaryDTO.StudentPerformanceDTO> studentPerformances = new ArrayList<>();
        
        // 初始化课程名称
        String selectedCourseName = null;
        if (courseId != null) {
            // 根据课程ID获取课程名称
            Course course = courseMapper.findById(courseId);
            if (course != null) {
                selectedCourseName = course.getCourseName();
            }
        }
        
        // 学生ID到课程名称的映射，用于当courseId为null时
        Map<Long, List<String>> studentCourseMap = new HashMap<>();
        
        // 获取学生ID列表和课程信息
        if (courseId != null) {
            // 单课程模式：获取该课程下的所有学生
            List<Long> studentIds = new ArrayList<>();
            if (classId != null) {
                // 根据课程ID和班级ID的组合获取学生ID列表
                studentIds = courseMapper.getStudentIdsByCourseIdAndClassId(courseId, classId);
            } else {
                // 根据课程ID获取学生ID列表
                studentIds = courseMapper.getStudentIdsByCourseId(courseId);
            }
            
            // 为每个学生添加对应的课程
            for (Long studentId : studentIds) {
                studentCourseMap.put(studentId, Collections.singletonList(selectedCourseName));
            }
        } else {
            // 多课程模式：获取所有课程下的学生，并记录每个学生的课程
            List<Course> courses = courseMapper.findByTeacherId(teacherId);
            
            for (Course course : courses) {
                List<Long> studentIds;
                if (classId != null) {
                    // 根据课程ID和班级ID的组合获取学生ID列表
                    studentIds = courseMapper.getStudentIdsByCourseIdAndClassId(course.getId(), classId);
                } else {
                    // 根据课程ID获取学生ID列表
                    studentIds = courseMapper.getStudentIdsByCourseId(course.getId());
                }
                
                for (Long studentId : studentIds) {
                    studentCourseMap.computeIfAbsent(studentId, k -> new ArrayList<>()).add(course.getCourseName());
                }
            }
        }
        
        // 汇总统计数据
        int totalStudents = studentCourseMap.size();
        double totalScore = 0;
        int totalPendingAssignments = 0;
        double totalProgress = 0;
        
        // 获取每个学生的学习数据
        for (Map.Entry<Long, List<String>> entry : studentCourseMap.entrySet()) {
            Long studentId = entry.getKey();
            List<String> studentCourses = entry.getValue();
            
            // 获取学生基本信息
            User user = userMapper.findById(studentId);
            if (user == null) {
                continue;
            }
            
            // 直接从数据库获取学生的平均成绩和进度，不依赖StudentService.getStudentPerformance()
            Map<String, Object> resultMap = studentMapper.getStudentPerformance(studentId);
            // 手工维护或动态计算的平均成绩
            double averageScoreValue = resultMap != null && resultMap.get("averageScore") instanceof Number ? ((Number) resultMap.get("averageScore")).doubleValue() : 0.0;
            // 手工维护或动态计算的整体进度
            double overallProgressValue = resultMap != null && resultMap.get("overallProgress") instanceof Number ? ((Number) resultMap.get("overallProgress")).doubleValue() : 0.0;
            // 获取待交作业数量，添加null检查
            Integer pendingAssignments = resultMap != null && resultMap.get("pendingAssignments") instanceof Number ? ((Number) resultMap.get("pendingAssignments")).intValue() : 0;
            
            int averageScore = (int) Math.round(averageScoreValue);
            int overallProgress = (int) Math.round(overallProgressValue);
            
            // 为每个课程创建一个学生表现DTO
            for (String courseName : studentCourses) {
                // 创建学生表现DTO
                StudentLearningSummaryDTO.StudentPerformanceDTO studentPerformance = new StudentLearningSummaryDTO.StudentPerformanceDTO();
                studentPerformance.setStudentId(studentId);
                studentPerformance.setRealName(user.getName());
                // 获取真实班级名称
                String className = userMapper.getStudentClassName(studentId);
                studentPerformance.setClassName(className != null ? className : "未知");
                
                // 设置课程名称
                studentPerformance.setCourseName(courseName != null ? courseName : "未知课程");
                
                // 设置学生表现数据
                studentPerformance.setAverageScore(averageScore);
                studentPerformance.setPendingAssignments(pendingAssignments);
                studentPerformance.setOverallProgress(overallProgress);
                studentPerformance.setStatus(averageScore < 60 ? "⚠️ 预警" : averageScore < 75 ? "⚠️ 关注" : "正常");
                
                // 添加到学生表现列表
                studentPerformances.add(studentPerformance);
                
                // 更新汇总统计数据
                totalScore += studentPerformance.getAverageScore();
                totalPendingAssignments += studentPerformance.getPendingAssignments();
                totalProgress += studentPerformance.getOverallProgress();
            }
        }
        
        // 设置汇总统计数据
        summary.setTotalStudents(totalStudents);
        summary.setAverageScore(studentPerformances.size() > 0 ? totalScore / studentPerformances.size() : 0);
        summary.setTotalPendingAssignments(totalPendingAssignments);
        summary.setOverallProgress(studentPerformances.size() > 0 ? totalProgress / studentPerformances.size() : 0);
        summary.setStudentPerformances(studentPerformances);
        
        return summary;
    }

    @Override
    public List<ScoreTrendDTO> getScoreTrend(Long teacherId, Long classId, Long courseId, String timeRange) {
        Date endDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(endDate);

        // 根据时间范围确定开始时间
        if ("week".equalsIgnoreCase(timeRange)) {
            calendar.add(Calendar.DAY_OF_YEAR, -6);
        } else if ("month".equalsIgnoreCase(timeRange)) {
            calendar.add(Calendar.DAY_OF_YEAR, -29);
        } else if ("semester".equalsIgnoreCase(timeRange)) {
            calendar.add(Calendar.DAY_OF_YEAR, -119);
        } else {
            // 默认一个月
            calendar.add(Calendar.DAY_OF_YEAR, -29);
            timeRange = "month";
        }
        Date startDate = calendar.getTime();

        List<Map<String, Object>> trendRows = assignmentSubmissionMapper.getAssignmentScoreTrend(
                teacherId,
                classId,
                courseId,
                startDate,
                endDate
        );

        List<ScoreTrendDTO> trendList = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        if (trendRows != null) {
            for (Map<String, Object> row : trendRows) {
                ScoreTrendDTO dto = new ScoreTrendDTO();
                Object dayObj = row.get("day");
                dto.setDate(dayObj != null ? dayObj.toString() : dateFormat.format(startDate));

                Object avgScoreObj = row.get("avgScore");
                double avgScore = 0.0;
                if (avgScoreObj instanceof Number) {
                    avgScore = ((Number) avgScoreObj).doubleValue();
                } else if (avgScoreObj != null) {
                    try {
                        avgScore = Double.parseDouble(avgScoreObj.toString());
                    } catch (NumberFormatException ignored) {
                        // 保持默认值0
                    }
                }
                dto.setAverageScore(avgScore);
                trendList.add(dto);
            }
        }

        // 按日期排序，确保前端渲染有序
        trendList.sort(Comparator.comparing(ScoreTrendDTO::getDate));
        return trendList;
    }
}