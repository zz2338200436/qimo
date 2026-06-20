package com._202510007517.platform.ai.controller;

import com._202510007517.platform.ai.api.dto.GenerateExamRequestDTO;
import com._202510007517.platform.ai.api.dto.GenerateQuestionsRequestDTO;
import com._202510007517.platform.ai.api.dto.LearningSuggestionRequestDTO;
import com._202510007517.platform.ai.service.AiGenerationService;
import com._202510007517.platform.ai.config.AiServiceExceptionHandler;
import com._202510007517.platform.ai.service.AiQuestionBankQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiControllerTest {

    @Test
    void generateQuestionsReturnsLegacyCompatibleEnvelope() throws Exception {
        AiGenerationService service = mock(AiGenerationService.class);
        when(service.generateQuestions(eq(7L), eq("TEACHER"), any(GenerateQuestionsRequestDTO.class)))
                .thenReturn(Map.of(
                        "topic", "Java基础",
                        "count", 2,
                        "difficulty", "中等",
                        "questions", List.of(
                                Map.of("id", 1, "content", "Java基础相关题目 1", "difficulty", "中等",
                                        "type", "选择题", "options", List.of("选项A", "选项B"), "answer", "A"))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller(service)).build();

        mockMvc.perform(post("/api/ai/generate-questions")
                        .header("X-User-Id", "7")
                        .header("X-Active-Role", "TEACHER")
                        .contentType("application/json")
                        .content("""
                                {
                                  "topic": "Java基础",
                                  "count": 2,
                                  "difficulty": "中等"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("生成题目成功"))
                .andExpect(jsonPath("$.data.topic").value("Java基础"))
                .andExpect(jsonPath("$.data.questions[0].content").value("Java基础相关题目 1"));
    }

    @Test
    void generateExamReturnsLegacyCompatibleEnvelope() throws Exception {
        AiGenerationService service = mock(AiGenerationService.class);
        when(service.generateExam(eq(7L), eq("TEACHER"), any(GenerateExamRequestDTO.class)))
                .thenReturn(Map.of(
                        "title", "Java模拟试卷",
                        "courseName", "Java",
                        "totalScore", 100,
                        "duration", 90,
                        "difficulty", "中等",
                        "questions", List.of(Map.of("id", 1, "type", "选择题", "score", 10))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller(service)).build();

        mockMvc.perform(post("/api/ai/generate-exam")
                        .header("X-User-Id", "7")
                        .header("X-Active-Role", "TEACHER")
                        .contentType("application/json")
                        .content("""
                                {
                                  "courseName": "Java",
                                  "totalScore": 100,
                                  "duration": 90,
                                  "difficulty": "中等"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("生成试卷成功"))
                .andExpect(jsonPath("$.data.title").value("Java模拟试卷"))
                .andExpect(jsonPath("$.data.questions[0].score").value(10));
    }

    @Test
    void learningSuggestionsUsesCurrentStudentWhenStudentRole() throws Exception {
        AiGenerationService service = mock(AiGenerationService.class);
        when(service.generateLearningSuggestions(eq(42L), eq("STUDENT"), any(LearningSuggestionRequestDTO.class)))
                .thenReturn(Map.of(
                        "studentId", 42L,
                        "suggestions", List.of("建议加强函数概念的理解")));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller(service)).build();

        mockMvc.perform(post("/api/ai/learning-suggestions")
                        .header("X-User-Id", "42")
                        .header("X-Active-Role", "STUDENT")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取学习建议成功"))
                .andExpect(jsonPath("$.data.studentId").value(42))
                .andExpect(jsonPath("$.data.suggestions[0]").value("建议加强函数概念的理解"));
    }

    @Test
    void missingUserIdentityReturnsBadRequest() throws Exception {
        AiGenerationService service = mock(AiGenerationService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller(service)).build();

        mockMvc.perform(post("/api/ai/generate-questions")
                        .contentType("application/json")
                        .content("""
                                {
                                  "topic": "Java基础",
                                  "count": 2,
                                  "difficulty": "中等"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("缺少用户身份"));
    }

    @Test
    void invalidGenerateQuestionsRequestUsesResponseResultEnvelope() throws Exception {
        AiGenerationService service = mock(AiGenerationService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller(service))
                .setControllerAdvice(new AiServiceExceptionHandler(new MockEnvironment()))
                .build();

        mockMvc.perform(post("/api/ai/generate-questions")
                        .header("X-User-Id", "7")
                        .header("X-Active-Role", "TEACHER")
                        .contentType("application/json")
                        .content("""
                                {
                                  "topic": "",
                                  "count": 0,
                                  "difficulty": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("参数校验失败"));
    }

    private static AiController controller(AiGenerationService service) {
        return new AiController(service, mock(AiQuestionBankQueryService.class));
    }
}
