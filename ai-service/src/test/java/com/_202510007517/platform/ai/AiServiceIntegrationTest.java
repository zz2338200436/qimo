package com._202510007517.platform.ai;

import com._202510007517.platform.common.web.CommonTraceConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = AiServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AiServiceIntegrationTest {

    private static final String DATABASE_NAME = "ai-integration-" + UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
        registry.add("spring.datasource.url", () ->
                "jdbc:h2:mem:" + DATABASE_NAME + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
    }

    @BeforeEach
    void cleanHistory() {
        jdbcTemplate.update("DELETE FROM ai_generations");
    }

    @Test
    void generateQuestionsUsesMockModelAndPersistsHistory() throws Exception {
        mockMvc.perform(post("/api/ai/generate-questions")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7")
                        .header(CommonTraceConstants.ACTIVE_ROLE_HEADER, "TEACHER")
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
                .andExpect(jsonPath("$.data.count").value(2))
                .andExpect(jsonPath("$.data.questions[0].content").value("Java基础相关题目 1"));

        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT user_id, user_role, prompt_key, request_type, model_name, status, request_payload, response_payload
                FROM ai_generations
                """);

        assertThat(row.get("user_id")).isEqualTo(7L);
        assertThat(row.get("user_role")).isEqualTo("TEACHER");
        assertThat(row.get("prompt_key")).isEqualTo("generate-questions");
        assertThat(row.get("request_type")).isEqualTo("generate-questions");
        assertThat(row.get("model_name")).isEqualTo("local-mock-model");
        assertThat(row.get("status")).isEqualTo("SUCCESS");
        assertThat(asString(row.get("request_payload"))).contains("Java基础");
        assertThat(asString(row.get("response_payload"))).contains("Java基础相关题目 1");
    }

    private static String asString(Object value) {
        if (value instanceof byte[] bytes) {
            return new String(bytes);
        }
        return value.toString();
    }
}
