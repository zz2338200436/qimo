package com._202510007517.platform.course.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CourseUpsertRequestDTO {
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
    private String courseCategory;
    @NotNull(message = "总学时不能为空")
    @Min(value = 0, message = "总学时不能小于0")
    @Max(value = 200, message = "总学时不能大于200")
    private Integer totalHours;
    private Long courseDirector;
    private String assessmentMethod;
    private String courseStatus;
    private String semester;
    private String startDate;
    private String endDate;
    private Integer maxStudents;

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCredit() {
        return credit;
    }

    public void setCredit(Integer credit) {
        this.credit = credit;
    }

    public String getCourseCategory() {
        return courseCategory;
    }

    public void setCourseCategory(String courseCategory) {
        this.courseCategory = courseCategory;
    }

    public Integer getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(Integer totalHours) {
        this.totalHours = totalHours;
    }

    public Long getCourseDirector() {
        return courseDirector;
    }

    public void setCourseDirector(Long courseDirector) {
        this.courseDirector = courseDirector;
    }

    public String getAssessmentMethod() {
        return assessmentMethod;
    }

    public void setAssessmentMethod(String assessmentMethod) {
        this.assessmentMethod = assessmentMethod;
    }

    public String getCourseStatus() {
        return courseStatus;
    }

    public void setCourseStatus(String courseStatus) {
        this.courseStatus = courseStatus;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Integer getMaxStudents() {
        return maxStudents;
    }

    public void setMaxStudents(Integer maxStudents) {
        this.maxStudents = maxStudents;
    }
}
