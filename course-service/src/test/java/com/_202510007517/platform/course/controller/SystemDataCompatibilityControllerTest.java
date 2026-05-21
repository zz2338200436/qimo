package com._202510007517.platform.course.controller;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.service.CourseApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SystemDataCompatibilityControllerTest {

    @Test
    void semestersEndpointReturnsSortedDistinctLegacyEnvelope() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        when(service.listSemesters()).thenReturn(List.of("2025秋", "2026春"));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new SystemDataCompatibilityController(service)).build();

        mockMvc.perform(get("/api/system/semesters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取学期列表成功"))
                .andExpect(jsonPath("$.data[0]").value("2025秋"))
                .andExpect(jsonPath("$.data[1]").value("2026春"));
    }

    @Test
    void studentCoursesEndpointReturnsLegacyEnvelope() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        CourseDTO course = new CourseDTO();
        course.setId(301L);
        course.setCourseName("高等数学");
        when(service.listStudentCourses(42L, null, null, null)).thenReturn(List.of(course));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new SystemDataCompatibilityController(service)).build();

        mockMvc.perform(get("/api/system/student/courses")
                        .header(CommonTraceConstants.USER_ID_HEADER, "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取课程列表成功"))
                .andExpect(jsonPath("$.data[0].id").value(301))
                .andExpect(jsonPath("$.data[0].courseName").value("高等数学"));
    }

    @Test
    void teacherCoursesEndpointReturnsLegacyEnvelope() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        CourseDTO course = new CourseDTO();
        course.setId(401L);
        course.setCourseName("Java程序设计");
        when(service.listTeacherCourses(7L, null, null, null, null)).thenReturn(List.of(course));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new SystemDataCompatibilityController(service)).build();

        mockMvc.perform(get("/api/system/teacher/courses")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取课程列表成功"))
                .andExpect(jsonPath("$.data[0].id").value(401))
                .andExpect(jsonPath("$.data[0].courseName").value("Java程序设计"));
    }

    @Test
    void timeRangesEndpointReturnsLegacyStaticOptions() throws Exception {
        CourseApplicationService service = mock(CourseApplicationService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new SystemDataCompatibilityController(service)).build();

        mockMvc.perform(get("/api/system/time-ranges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取时间范围列表成功"))
                .andExpect(jsonPath("$.data[0]").value("week"))
                .andExpect(jsonPath("$.data[3]").value("semester"));
    }
}
