package com._202510007517.major_assignment.entity.dto;

import lombok.Data;
import java.util.List;

@Data
public class StudentLearningSummaryDTO {
    // 汇总统计数据
    private Integer totalStudents;
    private Double averageScore;
    private Integer totalPendingAssignments;
    private Double overallProgress;
    
    // 学生详细列表
    private List<StudentPerformanceDTO> studentPerformances;
    
    @Data
    public static class StudentPerformanceDTO {
        private Long studentId;
        private Long courseId;
        private String realName;
        private String className;
        private String courseName;
        private Integer averageScore;
        private Integer pendingAssignments;
        private Integer overallProgress;
        private String status;
    }
}
