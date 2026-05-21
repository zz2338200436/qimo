package com._202510007517.platform.course.repository;

import com._202510007517.platform.course.domain.CourseRecord;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CourseRepository {
    List<CourseRecord> findByTeacherIdWithSearch(Long teacherId, String name, String courseCode, String category, String status);

    List<CourseRecord> findByStudentIdWithSearch(Long studentId, String searchQuery, String category, String status);

    List<CourseRecord> findAllCourses();

    Optional<CourseRecord> findById(Long id);

    CourseRecord insert(CourseRecord course);

    void update(CourseRecord course);

    void delete(Long id);

    Map<Long, Integer> countStudentsByCourseIds(List<Long> courseIds);

    List<Map<String, Object>> findStudentsByCourseId(Long courseId);

    List<Long> findClassIdsByStudentId(Long studentId);

    List<Long> findStudentIdsByTeacherId(Long teacherId);

    List<Map<String, Object>> findClassesByTeacherId(Long teacherId, String className, String grade, String majorName, Long majorId, Long courseId);

    Map<String, Object> findClassById(Long classId);

    List<Map<String, Object>> findStudentsByClassId(Long classId);

    Long insertClass(Map<String, Object> classData);

    void updateClass(Map<String, Object> classData);

    void deleteClass(Long classId);

    List<Map<String, Object>> findClassAssignments(Long teacherId, Long courseId, Long classId);

    Long insertClassCourse(Map<String, Object> assignData);

    void deleteClassCourse(Long assignmentId);

    void deleteClassCourse(Long classId, Long courseId);

    boolean teacherOwnsCourse(Long teacherId, Long courseId);

    boolean teacherCanAccessClass(Long teacherId, Long classId);

    boolean classCourseExists(Long classId, Long courseId);

    boolean classNameExists(String className, Long excludedClassId);

    void replaceStudentClass(Long studentId, Long classId);

    List<Map<String, Object>> findMajors();
}
