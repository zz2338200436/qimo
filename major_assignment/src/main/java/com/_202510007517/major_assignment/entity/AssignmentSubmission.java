package com._202510007517.major_assignment.entity;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Date;

@Data
public class AssignmentSubmission {
    private Long id;
    
    private Boolean graded;
    private Boolean isLate;
    
    /**
     * 学生姓名（联表查询 users 表时返回）
     * 非必填，仅用于前端展示
     */
    private String studentName;
    
    /**
     * 作业标题（联表查询 assignments 表时返回）
     * 非必填，仅用于前端展示
     */
    private String assignmentTitle;
    
    @Min(value = 0, message = "迟交惩罚不能小于0")
    @Max(value = 100, message = "迟交惩罚不能大于100")
    private Integer latePenalty;
    
    @Min(value = 0, message = "分数不能小于0")
    @Max(value = 100, message = "分数不能大于100")
    private Integer score;
    
    @NotNull(message = "提交日期不能为空")
    private Date submissionDate;
    
    @Size(max = 500, message = "教师评语长度不能超过500个字符")
    private String teacherComment;
    
    @NotNull(message = "作业ID不能为空")
    private Long assignmentId;
    
    @NotNull(message = "学生ID不能为空")
    private Long studentId;
    
    @Size(max = 2000, message = "作业内容长度不能超过2000个字符")
    private String content;
}