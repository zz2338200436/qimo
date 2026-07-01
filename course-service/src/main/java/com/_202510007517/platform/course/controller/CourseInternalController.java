package com._202510007517.platform.course.controller;

import com._202510007517.platform.course.api.dto.ClassUpsertRequestDTO;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.dto.CourseAssignmentDTO;
import com._202510007517.platform.course.api.dto.CourseAssignmentRequestDTO;
import com._202510007517.platform.course.api.dto.CourseUpsertRequestDTO;
import com._202510007517.platform.course.api.dto.MajorDTO;
import com._202510007517.platform.course.api.dto.TeacherClassDTO;
import com._202510007517.platform.course.service.CourseApplicationService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/courses")
public class CourseInternalController {

    private final CourseApplicationService courseApplicationService;

    public CourseInternalController(CourseApplicationService courseApplicationService) {
        this.courseApplicationService = courseApplicationService;
    }

    @GetMapping
    public List<CourseDTO> listTeacherCourses(@RequestParam("teacherId") Long teacherId,
                                              @RequestParam(value = "courseName", required = false) String courseName,
                                              @RequestParam(value = "courseCode", required = false) String courseCode,
                                              @RequestParam(value = "category", required = false) String category,
                                              @RequestParam(value = "courseStatus", required = false) String courseStatus) {
        return courseApplicationService.listTeacherCourses(teacherId, courseName, courseCode, category, courseStatus);
    }

    @GetMapping("/{courseId}")
    public CourseDTO getCourse(@PathVariable Long courseId) {
        return courseApplicationService.getCourse(courseId);
    }

    @PostMapping
    public CourseDTO createCourse(@RequestParam("teacherId") Long teacherId,
                                  @RequestBody CourseUpsertRequestDTO request) {
        return courseApplicationService.createCourse(teacherId, request);
    }

    @PutMapping("/{courseId}")
    public CourseDTO updateCourse(@PathVariable Long courseId,
                                  @RequestParam("teacherId") Long teacherId,
                                  @RequestBody CourseUpsertRequestDTO request) {
        return courseApplicationService.updateCourse(courseId, teacherId, request);
    }

    @DeleteMapping("/{courseId}")
    public void deleteCourse(@PathVariable Long courseId) {
        courseApplicationService.deleteCourse(courseId);
    }

    @GetMapping("/{courseId}/students")
    public List<Map<String, Object>> listCourseStudents(@PathVariable Long courseId) {
        return courseApplicationService.listCourseStudents(courseId);
    }

    @GetMapping("/students/{studentId}/class-ids")
    public List<Long> listStudentClassIds(@PathVariable Long studentId) {
        return courseApplicationService.listStudentClassIds(studentId);
    }

    @GetMapping("/classes")
    public List<TeacherClassDTO> listTeacherClasses(@RequestParam("teacherId") Long teacherId,
                                                    @RequestParam(value = "className", required = false) String className,
                                                    @RequestParam(value = "grade", required = false) String grade,
                                                    @RequestParam(value = "majorName", required = false) String majorName,
                                                    @RequestParam(value = "majorId", required = false) Long majorId,
                                                    @RequestParam(value = "courseId", required = false) Long courseId) {
        return courseApplicationService.listTeacherClasses(teacherId, className, grade, majorName, majorId, courseId);
    }

    @GetMapping("/classes/{classId}")
    public Map<String, Object> getClass(@RequestParam("teacherId") Long teacherId,
                                        @PathVariable Long classId) {
        return courseApplicationService.getClass(teacherId, classId);
    }

    @PostMapping("/classes")
    public Long createClass(@RequestParam("teacherId") Long teacherId,
                            @RequestBody ClassUpsertRequestDTO request) {
        return courseApplicationService.createClass(teacherId, request);
    }

    @PutMapping("/classes/{classId}")
    public void updateClass(@RequestParam("teacherId") Long teacherId,
                            @PathVariable Long classId,
                            @RequestBody ClassUpsertRequestDTO request) {
        courseApplicationService.updateClass(teacherId, classId, request);
    }

    @DeleteMapping("/classes/{classId}")
    public void deleteClass(@RequestParam("teacherId") Long teacherId,
                            @PathVariable Long classId) {
        courseApplicationService.deleteClass(teacherId, classId);
    }

    @GetMapping("/course-assignments")
    public List<CourseAssignmentDTO> listCourseAssignments(@RequestParam("teacherId") Long teacherId,
                                                           @RequestParam(value = "courseId", required = false) Long courseId,
                                                           @RequestParam(value = "classId", required = false) Long classId) {
        return courseApplicationService.listCourseAssignments(teacherId, courseId, classId);
    }

    @PostMapping("/course-assignments")
    public Long assignCourse(@RequestParam("teacherId") Long teacherId,
                             @RequestBody CourseAssignmentRequestDTO request) {
        return courseApplicationService.assignCourse(teacherId, request);
    }

    @DeleteMapping("/course-assignments/{assignmentId}")
    public void unassignCourse(@RequestParam("teacherId") Long teacherId,
                               @PathVariable Long assignmentId) {
        courseApplicationService.unassignCourse(teacherId, assignmentId);
    }

    @DeleteMapping("/class-courses/unassign")
    public void unassignClassCourse(@RequestParam("teacherId") Long teacherId,
                                    @RequestParam Long classId,
                                    @RequestParam Long courseId) {
        courseApplicationService.unassignClassCourse(teacherId, classId, courseId);
    }

    @GetMapping("/majors")
    public List<MajorDTO> listMajors() {
        return courseApplicationService.listMajors();
    }
}
