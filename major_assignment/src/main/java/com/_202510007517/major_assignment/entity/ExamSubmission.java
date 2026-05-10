package com._202510007517.major_assignment.entity;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Date;

@Data
public class ExamSubmission {
    private Long id;
    
    private Boolean graded;
    
    /**
     * 学生姓名（联表查询 users 表时返回）
     * 非必填，仅用于前端展示
     */
    private String studentName;
    
    /**
     * 考试标题（联表查询 exams 表时返回）
     * 非必填，仅用于前端展示
     */
    private String examTitle;
    
    @Min(value = 0, message = "分数不能小于0")
    @Max(value = 100, message = "分数不能大于100")
    private Integer score;
    
    @NotNull(message = "提交日期不能为空")
    private Date submissionDate;
    
    @Size(max = 500, message = "教师评语长度不能超过500个字符")
    private String teacherComment;
    
    /**
     * 学生提交的考试答案内容
     */
    private String content;
    
    @Min(value = 0, message = "用时不能小于0分钟")
    private Integer timeTaken;
    
    @NotNull(message = "考试ID不能为空")
    private Long examId;
    
    @NotNull(message = "学生ID不能为空")
    private Long studentId;
}