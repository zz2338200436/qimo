package com._202510007517.platform.legacy.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LegacyKnowledgeReadControllerTest {

    @Test
    void teacherKnowledgeListEndpointReturnsLegacyCompatibleEnvelope() throws Exception {
        LegacyKnowledgeReadService service = mock(LegacyKnowledgeReadService.class);
        when(service.listKnowledgePoints(7L, null)).thenReturn(List.of(
                Map.of(
                        "id", 11L,
                        "pointName", "面向对象基础",
                        "courseId", 101L,
                        "difficulty", "中等",
                        "masteryRate", 82.5
                )
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new LegacyKnowledgeReadController(service)).build();

        mockMvc.perform(get("/api/teacher/knowledge-points")
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取知识点列表成功"))
                .andExpect(jsonPath("$.data[0].id").value(11))
                .andExpect(jsonPath("$.data[0].pointName").value("面向对象基础"));
    }
}
