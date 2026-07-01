package com._202510007517.major_assignment.entity;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class Course {
    private Long id;
    
    @NotBlank(message = "课程名称不能为空")
    @Size(min = 2, max = 50, message = "课程名称长度必须在2到50个字符之间")
    private String courseName;
    
    @NotBlank(message = "课程代码不能为空")
    @Size(min = 2, max = 20, message = "课程代码长度必须在2到20个字符之间")
    private String courseCode;
    
    @Size(max = 200, message = "课程描述长度不能超过200个字符")
    private String description;
    
    @NotNull(message = "学分不能为空")
    @Min(value = 0, message = "学分不能小于0")
    @Max(value = 10, message = "学分不能大于10")
    private Integer credit;
    
    @NotNull(message = "总学时不能为空")
    @Min(value = 0, message = "总学时不能小于0")
    @Max(value = 200, message = "总学时不能大于200")
    private Integer totalHours;
    
    private Long teacherId;
    
    private String courseCategory;
    
    private Integer studentCount;
    
    // 添加课程状态和学期属性，以支持API文档中要求的筛选功能
    private String courseStatus;
    private String semester;
    private String assessmentMethod;
    private Long courseDirector;
    private String startDate;
    private String endDate;
    private Integer maxStudents;
    
    // 教师姓名（通过JOIN查询获取，非数据库表字段）
    private String teacherName;

    // 学生视角课程学习进度（通过服务层动态计算，非数据库表字段）
    private Integer progress;
}
