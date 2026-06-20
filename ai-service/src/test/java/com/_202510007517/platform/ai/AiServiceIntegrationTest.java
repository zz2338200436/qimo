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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = AiServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.config.import=optional:classpath:application-common.yml",
                "spring.cloud.config.enabled=false"
        })
@AutoConfigureMockMvc
@ActiveProfiles("test")
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
        ensureQuestionBankSchema();
        jdbcTemplate.update("DELETE FROM ai_generations");
        jdbcTemplate.update("DELETE FROM questions");
        jdbcTemplate.update("DELETE FROM knowledge_points");
    }

    @Test
    void generateQuestionsUsesQuestionBankAndPersistsHistory() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO knowledge_points (id, point_name, description, difficulty, order_index, course_id)
                VALUES (101, 'Java基础', 'Java 语言基础语法', '中等', 1, 10)
                """);
        jdbcTemplate.update("""
                INSERT INTO questions (id, content, correct_answer, difficulty, options, score, type, knowledge_point_id,
                                       created_at, updated_at, creator_id)
                VALUES (201, 'Java 中哪个关键字用于继承类？', 'A', '中等',
                        '["extends","implements","import","package"]', 5, 'SINGLE_CHOICE', 101,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 7)
                """);

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
                .andExpect(jsonPath("$.data.questions[0].id").value(201))
                .andExpect(jsonPath("$.data.questions[0].content").value("Java 中哪个关键字用于继承类？"))
                .andExpect(jsonPath("$.data.questions[0].options[0]").value("extends"))
                .andExpect(jsonPath("$.data.questions[0].answer").value("A"));

        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT user_id, user_role, prompt_key, request_type, model_name, status, request_payload, response_payload
                FROM ai_generations
                """);

        assertThat(row.get("user_id")).isEqualTo(7L);
        assertThat(row.get("user_role")).isEqualTo("TEACHER");
        assertThat(row.get("prompt_key")).isEqualTo("generate-questions");
        assertThat(row.get("request_type")).isEqualTo("generate-questions");
        assertThat(row.get("model_name")).isEqualTo("question-bank");
        assertThat(row.get("status")).isEqualTo("SUCCESS");
        assertThat(asString(row.get("request_payload"))).contains("Java基础");
        assertThat(asString(row.get("response_payload"))).contains("Java 中哪个关键字用于继承类？");
    }

    @Test
    void generateQuestionsReturnsEmptyResultWhenQuestionBankHasNoMatch() throws Exception {
        mockMvc.perform(post("/api/ai/generate-questions")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7")
                        .header(CommonTraceConstants.ACTIVE_ROLE_HEADER, "TEACHER")
                        .contentType("application/json")
                        .content("""
                                {
                                  "topic": "不存在的知识点",
                                  "count": 2,
                                  "difficulty": "中等"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.questions").isArray())
                .andExpect(jsonPath("$.data.questions").isEmpty())
                .andExpect(jsonPath("$.data.message").value("题库暂无匹配题目，请先维护题库或调整主题/难度"));

        String responsePayload = asString(jdbcTemplate.queryForObject(
                "SELECT response_payload FROM ai_generations",
                Object.class));

        assertThat(responsePayload)
                .contains("题库暂无匹配题目")
                .doesNotContain("选项A")
                .doesNotContain("相关题目 1");
    }

    @Test
    void generateQuestionsNormalizesDifficultyAndDoesNotMixQuestionDifficulties() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO knowledge_points (id, point_name, description, difficulty, order_index, course_id)
                VALUES (101, 'Java基础', 'Java 语言基础语法', '中等', 1, 10)
                """);
        jdbcTemplate.update("""
                INSERT INTO questions (id, content, correct_answer, difficulty, options, score, type, knowledge_point_id,
                                       created_at, updated_at, creator_id)
                VALUES (201, 'Java简单题不应返回', 'A', '简单',
                        '["extends","implements","import","package"]', 5, 'SINGLE_CHOICE', 101,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 7)
                """);
        jdbcTemplate.update("""
                INSERT INTO questions (id, content, correct_answer, difficulty, options, score, type, knowledge_point_id,
                                       created_at, updated_at, creator_id)
                VALUES (202, 'Java中等题应该返回', 'B', '中等',
                        '["String是基本数据类型","int默认占用4字节"]', 5, 'SINGLE_CHOICE', 101,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 7)
                """);

        mockMvc.perform(post("/api/ai/generate-questions")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7")
                        .header(CommonTraceConstants.ACTIVE_ROLE_HEADER, "TEACHER")
                        .contentType("application/json")
                        .content("""
                                {
                                  "topic": "Java基础",
                                  "count": 2,
                                  "difficulty": "中等难度"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.difficulty").value("中等"))
                .andExpect(jsonPath("$.data.questions[0].id").value(202))
                .andExpect(jsonPath("$.data.questions[0].content").value("Java中等题应该返回"))
                .andExpect(jsonPath("$.data.questions.length()").value(1));
    }

    @Test
    void questionBankSummaryReturnsAvailableKnowledgePointsAndQuestionCounts() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO knowledge_points (id, point_name, description, difficulty, order_index, course_id)
                VALUES (101, 'Java基础', 'Java 语言基础语法', '中等', 1, 10)
                """);
        jdbcTemplate.update("""
                INSERT INTO knowledge_points (id, point_name, description, difficulty, order_index, course_id)
                VALUES (102, 'Java基础', 'Java 语法复习', '中等', 2, 10)
                """);
        jdbcTemplate.update("""
                INSERT INTO questions (id, content, correct_answer, difficulty, options, score, type, knowledge_point_id,
                                       created_at, updated_at, creator_id)
                VALUES (201, 'Java中等题应该返回', 'B', '中等',
                        '["String是基本数据类型","int默认占用4字节"]', 5, 'SINGLE_CHOICE', 101,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 7)
                """);
        jdbcTemplate.update("""
                INSERT INTO questions (id, content, correct_answer, difficulty, options, score, type, knowledge_point_id,
                                       created_at, updated_at, creator_id)
                VALUES (202, '另一道Java中等题', 'A', '中等',
                        '["A","B"]', 5, 'SINGLE_CHOICE', 102,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 7)
                """);

        mockMvc.perform(get("/api/ai/question-bank/summary")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7")
                        .header(CommonTraceConstants.ACTIVE_ROLE_HEADER, "TEACHER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalKnowledgePoints").value(1))
                .andExpect(jsonPath("$.data.totalQuestions").value(2))
                .andExpect(jsonPath("$.data.topics[0].knowledgePoint").value("Java基础"))
                .andExpect(jsonPath("$.data.topics[0].difficulty").value("中等"))
                .andExpect(jsonPath("$.data.topics[0].questionCount").value(2))
                .andExpect(jsonPath("$.data.topics.length()").value(1));
    }

    private static String asString(Object value) {
        if (value instanceof byte[] bytes) {
            return new String(bytes);
        }
        return value.toString();
    }

    private void ensureQuestionBankSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS knowledge_points (
                    id BIGINT NOT NULL PRIMARY KEY,
                    description TEXT NULL,
                    difficulty VARCHAR(20) NULL,
                    order_index INT NULL,
                    point_name VARCHAR(255) NOT NULL,
                    course_id BIGINT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS questions (
                    id BIGINT NOT NULL PRIMARY KEY,
                    analysis TEXT NULL,
                    content TEXT NOT NULL,
                    correct_answer VARCHAR(255) NOT NULL,
                    difficulty VARCHAR(20) NULL,
                    options TEXT NULL,
                    score INT NULL,
                    type VARCHAR(30) NOT NULL,
                    assignment_id BIGINT NULL,
                    exam_id BIGINT NULL,
                    knowledge_point_id BIGINT NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL,
                    creator_id BIGINT NULL
                )
                """);
    }
}
