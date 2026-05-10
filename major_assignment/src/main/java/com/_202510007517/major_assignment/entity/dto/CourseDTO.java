package com._202510007517.major_assignment.entity.dto;

import lombok.Data;
import java.time.LocalDate;

/**
 * 课程DTO - 用于API响应
 */
@Data
public class CourseDTO {
    private Long id;
    private String courseCode;
    private String courseName;
    private Integer credit;
    private String courseCategory;
    private String description;
    private Integer totalHours;
    private Long teacherId;
    private String teacherName;
    private String courseStatus;
    private String semester;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxStudents;
    private Integer studentCount;
}

