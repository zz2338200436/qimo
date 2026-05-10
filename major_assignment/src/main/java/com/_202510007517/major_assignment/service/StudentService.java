package com._202510007517.major_assignment.service;

import com._202510007517.major_assignment.entity.Course;
import com._202510007517.major_assignment.entity.dto.StudentDashboardDTO;

import java.util.List;
import java.util.Map;

public interface StudentService {
    List<Course> getStudentCourses(Long studentId);
    Map<String, Object> getStudentCoursesWithPagination(Long studentId, Integer page, Integer size, String sortBy, String order, String courseStatus, String semester, String courseCategory, String searchQuery);
    Map<String, Object> getLearningStats(Long studentId, String semester, Long courseId, String timeRange);
    List<Map<String, Object>> getKnowledgePoints(Long studentId, String semester, Long courseId, String timeRange);
    List<Map<String, Object>> getStudyTimeDistribution(Long studentId, String type, String semester, Long courseId, String timeRange);
    
    Map<String, Object> getKnowledgePointDetail(Long studentId, Long knowledgePointId);

    StudentDashboardDTO getStudentPerformance(Long studentId);

    List<Map<String, Object>> getScores(Long studentId, String semester, Long courseId, String timeRange);
    
    List<Map<String, Object>> getEarlyWarnings(Long studentId);
    
    // 学生设置相关方法
    Map<String, Object> getStudentProfile(Long studentId);
    boolean updateStudentProfile(Long studentId, Map<String, Object> profileData);
    boolean changePassword(Long studentId, String currentPassword, String newPassword, String confirmPassword);
    Map<String, Object> getNotificationSettings(Long studentId);
    boolean updateNotificationSettings(Long studentId, Map<String, Object> settings);
    Map<String, Object> getPrivacySettings(Long studentId);
    boolean updatePrivacySettings(Long studentId, Map<String, Object> settings);
    boolean uploadAvatar(Long studentId, String avatarUrl);
    Map<String, Object> exportStudentData(Long studentId);
}