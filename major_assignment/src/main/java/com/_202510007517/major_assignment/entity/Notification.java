package com._202510007517.major_assignment.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Date;

@Data
public class Notification {
    private Long id;
    
    @NotNull(message = "学生ID不能为空")
    private Long studentId;
    
    @NotNull(message = "教师ID不能为空")
    private Long teacherId;
    
    @NotBlank(message = "通知类型不能为空")
    @Size(min = 2, max = 20, message = "通知类型长度必须在2到20个字符之间")
    private String type;
    
    @NotBlank(message = "通知标题不能为空")
    @Size(min = 2, max = 100, message = "通知标题长度必须在2到100个字符之间")
    private String title;
    
    @NotBlank(message = "通知内容不能为空")
    private String content;
    
    private Long relatedId;
    
    private Boolean isRead;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8", shape = JsonFormat.Shape.STRING)
    private Date createdAt;
}