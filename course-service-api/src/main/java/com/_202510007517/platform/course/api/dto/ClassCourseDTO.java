package com._202510007517.platform.course.api.dto;

public class ClassCourseDTO {
    private Long courseId;
    private String courseName;
    private String classTime;
    private String classLocation;

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getClassTime() {
        return classTime;
    }

    public void setClassTime(String classTime) {
        this.classTime = classTime;
    }

    public String getClassLocation() {
        return classLocation;
    }

    public void setClassLocation(String classLocation) {
        this.classLocation = classLocation;
    }
}
