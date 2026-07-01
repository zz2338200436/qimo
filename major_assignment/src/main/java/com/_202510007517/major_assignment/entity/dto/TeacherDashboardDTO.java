package com._202510007517.major_assignment.entity.dto;

import lombok.Data;
import java.util.List;

@Data
public class TeacherDashboardDTO {
    private Integer totalCourses;
    private Integer totalCoursesChange;
    private Integer totalStudents;
    private Integer totalStudentsChange;
    private Integer pendingAssignments;
    private Integer pendingAssignmentsChange;
    private Integer pendingExams;
    private Integer pendingExamsChange;
    private Integer missingSubmissions;
    private Integer missingSubmissionsChange;
    private Integer upcomingDeadlines;
    private Integer upcomingDeadlinesChange;
    private Integer warningCount;
    private Integer warningCountChange;
    private List<String> courseNames;
    private List<Double> averageScores;
    private Double overallProgress;
    private List<String> submissionRateDays;
    private List<Integer> submissionRates;
    private List<RecentActivityDTO> recentActivities;
    
    @Data
    public static class RecentActivityDTO {
        private String activityType;
        private String studentName;
        private Long studentId;
        private String activityDate;
        private String details;
    }
}
