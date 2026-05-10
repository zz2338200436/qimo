package com._202510007517.major_assignment.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Date;

@Data
public class Assignment {
    private Long id;
    
    @NotBlank(message = "作业标题不能为空")
    @Size(min = 2, max = 100, message = "作业标题长度必须在2到100个字符之间")
    private String title;
    
    @NotBlank(message = "作业描述不能为空")
    private String description;
    
    @NotNull(message = "课程ID不能为空")
    private Long courseId;
    
    @NotNull(message = "截止日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8", shape = JsonFormat.Shape.STRING)
    private Date dueDate;
    
    @NotNull(message = "发布日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8", shape = JsonFormat.Shape.STRING)
    private Date publishDate;
    
    private Boolean isActive;
    
    @NotNull(message = "教师ID不能为空")
    private Long teacherId;
    
    // 新增字段：满分
    private Integer maxScore;
    

    
    // 新增字段：作业提交数量
    private Integer submissionCount;
    
    // 新增字段：已批改数量
    private Integer gradedCount;
    
    // 新增字段：作业状态
    private String status;
    
    // 新增字段：预期参与学生数（总人数）
    private Integer totalStudents;
}
