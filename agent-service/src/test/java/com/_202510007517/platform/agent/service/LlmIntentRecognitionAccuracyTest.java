package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.config.AgentLlmProperties;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.RecognizedIntent;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "agent.llm.accuracy.enabled", matches = "true")
@EnabledIfEnvironmentVariable(named = "API-KEY", matches = ".+")
class LlmIntentRecognitionAccuracyTest {

    private static final double MINIMUM_INTENT_ACCURACY = 0.90;
    private static final double MINIMUM_SLOT_ACCURACY = 0.80;

    @Test
    void recognizesRepresentativeChineseAgentCommandsAccurately() {
        LlmIntentRecognitionService service = buildService();
        List<AccuracyCase> cases = List.of(
                new AccuracyCase(
                        "给Java企业开发课程发布作业，标题是Spring Cloud注册中心实验，截止明晚十点，满分100",
                        AgentIntent.PUBLISH_ASSIGNMENT,
                        Map.of("title", "Spring Cloud注册中心实验", "courseName", "Java企业开发", "maxScore", 100),
                        List.of()
                ),
                new AccuracyCase(
                        "帮我提交数据库作业，内容是实验报告和截图都已经上传",
                        AgentIntent.SUBMIT_ASSIGNMENT,
                        Map.of("assignmentTitle", "数据库作业", "content", "实验报告和截图都已经上传"),
                        List.of()
                ),
                new AccuracyCase(
                        "我还有哪些待提交作业",
                        AgentIntent.QUERY_PENDING_ASSIGNMENTS,
                        Map.of(),
                        List.of()
                ),
                new AccuracyCase(
                        "查看我的课程列表",
                        AgentIntent.QUERY_COURSES,
                        Map.of(),
                        List.of()
                ),
                new AccuracyCase(
                        "查询学生ID 12 在课程ID 3 的知识点掌握情况",
                        AgentIntent.QUERY_KNOWLEDGE_MASTERY,
                        Map.of("studentId", 12, "courseId", 3),
                        List.of()
                ),
                new AccuracyCase(
                        "生成五道Java集合选择题",
                        AgentIntent.GENERATE_QUESTIONS,
                        Map.of(),
                        List.of()
                ),
                new AccuracyCase(
                        "把所有通知标为已读",
                        AgentIntent.MARK_ALL_NOTIFICATIONS_READ,
                        Map.of(),
                        List.of()
                ),
                new AccuracyCase(
                        "把通知ID 9标为已读",
                        AgentIntent.MARK_NOTIFICATION_READ,
                        Map.of("notificationId", 9),
                        List.of()
                ),
                new AccuracyCase(
                        "给学生ID 42发送通知，标题是开课通知，内容是请按时上课，类型是course，相关ID是8",
                        AgentIntent.SEND_NOTIFICATION,
                        Map.of("studentId", 42, "title", "开课通知", "content", "请按时上课", "type", "course", "relatedId", 8),
                        List.of()
                ),
                new AccuracyCase(
                        "给学生ID 42,43批量发送通知，标题是开课通知，内容是请按时上课，类型是course",
                        AgentIntent.SEND_BATCH_NOTIFICATION,
                        Map.of("studentIds", List.of(42, 43), "title", "开课通知", "content", "请按时上课", "type", "course"),
                        List.of()
                ),
                new AccuracyCase(
                        "发布考试，标题是Spring Boot阶段测验，课程ID 8，满分100，考试时长60分钟",
                        AgentIntent.PUBLISH_EXAM,
                        Map.of("title", "Spring Boot阶段测验", "courseId", 8, "maxScore", 100, "duration", 60),
                        List.of()
                ),
                new AccuracyCase(
                        "帮我提交Java期末考试，用时45分钟",
                        AgentIntent.SUBMIT_EXAM,
                        Map.of("examTitle", "Java期末考试", "timeTaken", 45),
                        List.of()
                ),
                new AccuracyCase(
                        "今天食堂有什么菜",
                        AgentIntent.UNKNOWN,
                        Map.of(),
                        List.of()
                )
        );

        List<String> intentFailures = new ArrayList<>();
        int slotExpectations = 0;
        int slotMatches = 0;

        for (AccuracyCase testCase : cases) {
            RecognizedIntent result = service.recognize(testCase.message());
            if (result.intent() != testCase.expectedIntent()) {
                intentFailures.add("""
                        message=%s
                        expectedIntent=%s actualIntent=%s confidence=%s slots=%s missingSlots=%s
                        """.formatted(
                        testCase.message(),
                        testCase.expectedIntent(),
                        result.intent(),
                        result.confidence(),
                        result.slots(),
                        result.missingSlots()
                ));
            }
            for (Map.Entry<String, Object> expectedSlot : testCase.expectedSlots().entrySet()) {
                slotExpectations++;
                Object actualValue = result.slots().get(expectedSlot.getKey());
                if (slotMatches(expectedSlot.getValue(), actualValue)) {
                    slotMatches++;
                }
            }
        }

        double intentAccuracy = (cases.size() - intentFailures.size()) / (double) cases.size();
        double slotAccuracy = slotExpectations == 0 ? 1.0 : slotMatches / (double) slotExpectations;

        assertThat(intentFailures)
                .withFailMessage("Intent accuracy %.2f is below %.2f. Failures:%n%s",
                        intentAccuracy, MINIMUM_INTENT_ACCURACY, String.join("\n", intentFailures))
                .isEmpty();
        assertThat(intentAccuracy).isGreaterThanOrEqualTo(MINIMUM_INTENT_ACCURACY);
        assertThat(slotAccuracy)
                .withFailMessage("Slot accuracy %.2f is below %.2f", slotAccuracy, MINIMUM_SLOT_ACCURACY)
                .isGreaterThanOrEqualTo(MINIMUM_SLOT_ACCURACY);
    }

    private LlmIntentRecognitionService buildService() {
        AgentLlmProperties properties = new AgentLlmProperties();
        properties.setEnabled(true);
        properties.setApiKey(System.getenv("API-KEY"));
        ChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .modelName(properties.getModelName())
                .temperature(properties.getTemperature())
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
        return new LlmIntentRecognitionService(
                chatModel,
                new RuleBasedIntentRecognitionService(),
                new ObjectMapper(),
                properties
        );
    }

    private boolean slotMatches(Object expectedValue, Object actualValue) {
        if (actualValue == null) {
            return false;
        }
        if (expectedValue instanceof Number expectedNumber) {
            if (actualValue instanceof Number actualNumber) {
                return expectedNumber.longValue() == actualNumber.longValue();
            }
            return expectedValue.toString().equals(actualValue.toString());
        }
        if (expectedValue instanceof List<?> expectedList) {
            if (actualValue instanceof List<?> actualList) {
                return actualList.stream().map(String::valueOf).toList()
                        .containsAll(expectedList.stream().map(String::valueOf).toList());
            }
            return false;
        }
        return actualValue.toString().contains(expectedValue.toString());
    }

    private record AccuracyCase(
            String message,
            AgentIntent expectedIntent,
            Map<String, Object> expectedSlots,
            List<String> expectedMissingSlots
    ) {
    }
}
