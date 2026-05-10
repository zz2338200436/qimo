package com._202510007517.major_assignment.service;

import com._202510007517.major_assignment.entity.Course;
import java.util.List;
import java.util.Map;

public interface CourseService {
    List<Course> findByTeacherId(Long teacherId);
    List<Course> findByTeacherIdWithSearch(Long teacherId, String name, String courseCode, String category, String status);
    List<Course> getAllCourses();
    List<Long> findCourseIdsByTeacherId(Long teacherId);
    void create(Course course);
    void update(Course course);
    void delete(Long id);
    Course findById(Long id);
    Integer getStudentCountByCourseId(Long courseId);
    
    /**
     * 批量获取多个课程的学生数量
     * @param courseIds 课程ID列表
     * @return Map，key为课程ID，value为学生数量
     */
    Map<Long, Integer> batchGetStudentCountByCourseIds(List<Long> courseIds);
    
    List<Map<String, Object>> getClassesByTeacherId(Long teacherId, String className, String grade, String majorName, Long majorId, String teacherName, Long courseId);
    String getTeacherNameByCourseId(Long courseId);
    Map<String, Object> getLearningProgressByCourseIdAndStudentId(Long courseId, Long studentId);
    List<Long> getStudentIdsByCourseId(Long courseId);
    // List<Long> getStudentIdsByClassId(Long classId);
    List<Map<String, Object>> getStudentsByCourseId(Long courseId);
    
    // 课程分配相关方法
    void createClass(Map<String, Object> classData);
    void updateClass(Map<String, Object> classData);
    void deleteClass(Long classId);
    
    // 课程分配相关方法
    List<Map<String, Object>> getClassAssignments(Long teacherId, Long courseId, Long classId);
    void assignCourse(Map<String, Object> assignData);
    void unassignCourse(Long assignmentId);
    
    // 学生课程相关方法
    List<Course> findStudentCourses(Long studentId);
}