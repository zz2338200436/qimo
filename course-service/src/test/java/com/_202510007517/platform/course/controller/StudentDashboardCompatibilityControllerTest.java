package com._202510007517.platform.course.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentDashboardCompatibilityControllerTest {

    @Test
    void currentStudentPerformanceEndpointReturnsUnsupportedCompatibilityPayload() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StudentDashboardCompatibilityController()).build();

        mockMvc.perform(get("/api/dashboard/student-performance")
                        .header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.unsupported").value(true))
                .andExpect(jsonPath("$.code").value(501))
                .andExpect(jsonPath("$.message").value("当前 JWT 微服务环境暂未提供学生综合表现接口，页面将改用已接通的数据源进行统计。"));
    }

    @Test
    void namedStudentPerformanceEndpointReturnsUnsupportedCompatibilityPayload() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StudentDashboardCompatibilityController()).build();

        mockMvc.perform(get("/api/dashboard/student-performance/{studentId}", 42L)
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.unsupported").value(true))
                .andExpect(jsonPath("$.code").value(501))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
