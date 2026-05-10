package com._202510007517.major_assignment.entity.dto;

import lombok.Data;
import java.util.List;

@Data
public class StudentDashboardDTO {
    private Integer courseCount;
    private Integer courseCountChange;
    private Integer pendingAssignments;
    private Integer pendingAssignmentsChange;
    private Integer upcomingExams;
    private Integer upcomingExamsChange;
    private Double overallProgress;
    private Double overallProgressChange;
    private List<CourseDTO> courses;
    private List<String> learningProgressWeeks;
    private List<Integer> learningProgressData;
    private List<GradesDistributionDTO> gradesDistribution;
    private List<RecentActivityDTO> recentActivities;

    @Data
    public static class CourseDTO {
        private Long id;
        private String name;
        private String teacherName;
        private Integer progress;
    }

    @Data
    public static class GradesDistributionDTO {
        private Integer value;
        private String name;
    }

    @Data
    public static class RecentActivityDTO {
        private String type;
        private String description;
        private String timestamp;
    }
}