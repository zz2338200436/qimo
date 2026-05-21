package com._202510007517.platform.course.contract;

import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.dto.MajorDTO;
import com._202510007517.platform.course.api.dto.TeacherClassDTO;
import com._202510007517.platform.course.service.CourseApplicationService;
import com._202510007517.platform.course.web.CourseController;
import com._202510007517.platform.course.web.CourseInternalController;
import com._202510007517.platform.course.web.TeacherCourseAdminController;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class CourseServiceContractBase {

    private CourseApplicationService courseApplicationService;

    @BeforeEach
    void setup() {
        courseApplicationService = mock(CourseApplicationService.class);
        stubContracts();
        RestAssuredMockMvc.standaloneSetup(
                new CourseController(courseApplicationService),
                new TeacherCourseAdminController(courseApplicationService),
                new CourseInternalController(courseApplicationService)
        );
    }

    private void stubContracts() {
        CourseDTO course = new CourseDTO();
        course.setId(101L);
        course.setCourseName("Distributed Systems");
        course.setCourseCode("DS101");
        course.setCredit(3);
        course.setTotalHours(48);
        course.setTeacherId(7L);
        course.setCourseStatus("ACTIVE");
        course.setSemester("2026-Fall");
        course.setStudentCount(36);

        when(courseApplicationService.listTeacherCourses(7L, null, null, null, null)).thenReturn(List.of(course));
        when(courseApplicationService.getCourse(101L)).thenReturn(course);

        TeacherClassDTO teacherClass = new TeacherClassDTO();
        teacherClass.setId(501L);
        teacherClass.setClassName("软件 2301");
        teacherClass.setYear("2023");
        teacherClass.setCapacity(40);
        teacherClass.setStudentCount(36);
        teacherClass.setTeacherId(7L);
        teacherClass.setMajorId(2L);
        teacherClass.setMajorName("软件工程");
        teacherClass.setCourseName("Distributed Systems、Algorithms");
        teacherClass.setCourseCount(2);
        when(courseApplicationService.listTeacherClasses(7L, null, null, null, null, null)).thenReturn(List.of(teacherClass));

        MajorDTO major = new MajorDTO();
        major.setId(2L);
        major.setMajorName("软件工程");
        when(courseApplicationService.listMajors()).thenReturn(List.of(major));

        when(courseApplicationService.listCourseStudents(101L))
                .thenReturn(List.of(Map.of("id", 3001L, "className", "软件 2301")));
    }
}
