package com._202510007517.platform.course.api.feign;

import com._202510007517.platform.course.api.dto.ClassUpsertRequestDTO;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.dto.CourseAssignmentDTO;
import com._202510007517.platform.course.api.dto.CourseAssignmentRequestDTO;
import com._202510007517.platform.course.api.dto.CourseUpsertRequestDTO;
import com._202510007517.platform.course.api.dto.MajorDTO;
import com._202510007517.platform.course.api.dto.TeacherClassDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "course-service", path = "/internal/courses", fallbackFactory = CourseFeignClientFallbackFactory.class)
public interface CourseFeignClient {

    @GetMapping
    List<CourseDTO> listTeacherCourses(@RequestParam("teacherId") Long teacherId,
                                       @RequestParam(value = "courseName", required = false) String courseName,
                                       @RequestParam(value = "courseCode", required = false) String courseCode,
                                       @RequestParam(value = "category", required = false) String category,
                                       @RequestParam(value = "courseStatus", required = false) String courseStatus);

    @GetMapping("/{courseId}")
    CourseDTO getCourse(@PathVariable("courseId") Long courseId);

    @PostMapping
    CourseDTO createCourse(@RequestParam("teacherId") Long teacherId,
                           @RequestBody CourseUpsertRequestDTO request);

    @PutMapping("/{courseId}")
    CourseDTO updateCourse(@PathVariable("courseId") Long courseId,
                           @RequestParam("teacherId") Long teacherId,
                           @RequestBody CourseUpsertRequestDTO request);

    @DeleteMapping("/{courseId}")
    void deleteCourse(@PathVariable("courseId") Long courseId);

    @GetMapping("/{courseId}/students")
    List<Map<String, Object>> listCourseStudents(@PathVariable("courseId") Long courseId);

    @GetMapping("/students/{studentId}/class-ids")
    List<Long> listStudentClassIds(@PathVariable("studentId") Long studentId);

    @GetMapping("/classes")
    List<TeacherClassDTO> listTeacherClasses(@RequestParam("teacherId") Long teacherId,
                                             @RequestParam(value = "className", required = false) String className,
                                             @RequestParam(value = "grade", required = false) String grade,
                                             @RequestParam(value = "majorName", required = false) String majorName,
                                             @RequestParam(value = "majorId", required = false) Long majorId,
                                             @RequestParam(value = "courseId", required = false) Long courseId);

    @GetMapping("/classes/{classId}")
    Map<String, Object> getClass(@RequestParam("teacherId") Long teacherId,
                                 @PathVariable("classId") Long classId);

    @PostMapping("/classes")
    Long createClass(@RequestParam("teacherId") Long teacherId,
                     @RequestBody ClassUpsertRequestDTO request);

    @PutMapping("/classes/{classId}")
    void updateClass(@RequestParam("teacherId") Long teacherId,
                     @PathVariable("classId") Long classId,
                     @RequestBody ClassUpsertRequestDTO request);

    @DeleteMapping("/classes/{classId}")
    void deleteClass(@RequestParam("teacherId") Long teacherId,
                     @PathVariable("classId") Long classId);

    @GetMapping("/course-assignments")
    List<CourseAssignmentDTO> listCourseAssignments(@RequestParam("teacherId") Long teacherId,
                                                    @RequestParam(value = "courseId", required = false) Long courseId,
                                                    @RequestParam(value = "classId", required = false) Long classId);

    @PostMapping("/course-assignments")
    Long assignCourse(@RequestParam("teacherId") Long teacherId,
                      @RequestBody CourseAssignmentRequestDTO request);

    @DeleteMapping("/course-assignments/{assignmentId}")
    void unassignCourse(@RequestParam("teacherId") Long teacherId,
                        @PathVariable("assignmentId") Long assignmentId);

    @DeleteMapping("/class-courses/unassign")
    void unassignClassCourse(@RequestParam("teacherId") Long teacherId,
                             @RequestParam("classId") Long classId,
                             @RequestParam("courseId") Long courseId);

    @GetMapping("/majors")
    List<MajorDTO> listMajors();
}
