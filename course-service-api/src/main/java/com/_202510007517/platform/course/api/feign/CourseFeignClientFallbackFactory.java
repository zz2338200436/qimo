package com._202510007517.platform.course.api.feign;

import com._202510007517.platform.course.api.dto.ClassUpsertRequestDTO;
import com._202510007517.platform.course.api.dto.CourseAssignmentDTO;
import com._202510007517.platform.course.api.dto.CourseAssignmentRequestDTO;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.dto.CourseUpsertRequestDTO;
import com._202510007517.platform.course.api.dto.MajorDTO;
import com._202510007517.platform.course.api.dto.TeacherClassDTO;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CourseFeignClientFallbackFactory implements FallbackFactory<CourseFeignClient> {

    @Override
    public CourseFeignClient create(Throwable cause) {
        return new CourseFeignClient() {
            @Override
            public List<CourseDTO> listTeacherCourses(Long teacherId, String courseName, String courseCode, String category, String courseStatus) {
                return List.of();
            }

            @Override
            public CourseDTO getCourse(Long courseId) {
                return null;
            }

            @Override
            public CourseDTO createCourse(Long teacherId, CourseUpsertRequestDTO request) {
                return null;
            }

            @Override
            public CourseDTO updateCourse(Long courseId, Long teacherId, CourseUpsertRequestDTO request) {
                return null;
            }

            @Override
            public void deleteCourse(Long courseId) {
            }

            @Override
            public List<Map<String, Object>> listCourseStudents(Long courseId) {
                return List.of();
            }

            @Override
            public List<Long> listStudentClassIds(Long studentId) {
                return List.of();
            }

            @Override
            public List<TeacherClassDTO> listTeacherClasses(Long teacherId, String className, String grade, String majorName, Long majorId, Long courseId) {
                return List.of();
            }

            @Override
            public Map<String, Object> getClass(Long teacherId, Long classId) {
                return Map.of();
            }

            @Override
            public Long createClass(Long teacherId, ClassUpsertRequestDTO request) {
                return null;
            }

            @Override
            public void updateClass(Long teacherId, Long classId, ClassUpsertRequestDTO request) {
            }

            @Override
            public void deleteClass(Long teacherId, Long classId) {
            }

            @Override
            public List<CourseAssignmentDTO> listCourseAssignments(Long teacherId, Long courseId, Long classId) {
                return List.of();
            }

            @Override
            public Long assignCourse(Long teacherId, CourseAssignmentRequestDTO request) {
                return null;
            }

            @Override
            public void unassignCourse(Long teacherId, Long assignmentId) {
            }

            @Override
            public void unassignClassCourse(Long teacherId, Long classId, Long courseId) {
            }

            @Override
            public List<MajorDTO> listMajors() {
                return List.of();
            }
        };
    }
}
