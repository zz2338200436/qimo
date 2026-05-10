package com._202510007517.major_assignment.entity.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 作业DTO - 用于API响应，不包含敏感信息
 */
@Data
public class AssignmentDTO {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private LocalDateTime publishDate;
    private Boolean isActive;
    private Long courseId;
    private String courseName;
    private Long teacherId;
    private String teacherName;
}

