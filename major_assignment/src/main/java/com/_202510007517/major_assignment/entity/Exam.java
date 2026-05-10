package com._202510007517.major_assignment.entity;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Date;

@Data
public class Exam {
    private Long id;
    
    @NotBlank(message = "考试标题不能为空")
    @Size(min = 2, max = 100, message = "考试标题长度必须在2到100个字符之间")
    private String title;
    
    @NotBlank(message = "考试描述不能为空")
    private String description;
    
    @NotNull(message = "课程ID不能为空")
    private Long courseId;
    
    @NotNull(message = "开始时间不能为空")
    private Date startTime;
    
    @NotNull(message = "结束时间不能为空")
    @FutureOrPresent(message = "结束时间必须是当前时间或未来时间")
    private Date endTime;
    
    @NotNull(message = "发布日期不能为空")
    private Date publishDate;
    
    private Boolean isActive;
    private Boolean isOnline;
    private String location;
    
    @NotNull(message = "考试时长不能为空")
    @Min(value = 1, message = "考试时长不能小于1分钟")
    @Max(value = 480, message = "考试时长不能超过480分钟")
    private Long duration;
    
    @NotNull(message = "教师ID不能为空")
    private Long teacherId;
    
    // 新增字段：提交数量
    private Integer submittedCount;
    
    // 新增字段：预期参与学生数（总人数）
    private Integer totalStudents;
}
