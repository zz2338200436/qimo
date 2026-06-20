package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.config.AgentLlmProperties;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.RecognizedIntent;
import com.fasterxml.jackson.databind.ObjectMapper;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LlmIntentRecognitionServiceTest {

    private final RuleBasedIntentRecognitionService fallback = new RuleBasedIntentRecognitionService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesStructuredLlmIntent() {
        LlmIntentRecognitionService service = serviceWithResponse("""
                {
                  "intent": "PUBLISH_ASSIGNMENT",
                  "confidence": 0.91,
                  "slots": {
                    "title": "Spring Cloud实验",
                    "courseName": "Java",
                    "maxScore": 100
                  },
                  "missingSlots": []
                }
                """);

        RecognizedIntent result = service.recognize("帮我给Java课发布Spring Cloud实验作业");

        assertThat(result.intent()).isEqualTo(AgentIntent.PUBLISH_ASSIGNMENT);
        assertThat(result.confidence()).isEqualTo(0.91);
        assertThat(result.slots())
                .containsEntry("title", "Spring Cloud实验")
                .containsEntry("courseName", "Java")
                .containsEntry("maxScore", 100);
        assertThat(result.missingSlots()).isEmpty();
    }

    @Test
    void fallsBackWhenLlmReturnsInvalidJson() {
        LlmIntentRecognitionService service = serviceWithResponse("我觉得这是发布作业");

        RecognizedIntent result = service.recognize("给Java课程发布作业，标题是Spring Cloud实验，满分100");

        assertThat(result.intent()).isEqualTo(AgentIntent.PUBLISH_ASSIGNMENT);
        assertThat(result.slots()).containsEntry("title", "Spring Cloud实验");
    }

    @Test
    void fallsBackWhenLlmConfidenceIsTooLow() {
        LlmIntentRecognitionService service = serviceWithResponse("""
                {"intent":"DELETE_ASSIGNMENT","confidence":0.2,"slots":{},"missingSlots":[]}
                """);

        RecognizedIntent result = service.recognize("查看我的课程");

        assertThat(result.intent()).isEqualTo(AgentIntent.QUERY_COURSES);
    }

    @Test
    void usesRuleBasedIntentWhenLlmMisclassifiesReadOnlyLookupAsWriteAction() {
        LlmIntentRecognitionService service = serviceWithResponse("""
                {"intent":"SUBMIT_ASSIGNMENT","confidence":0.96,"slots":{"assignmentId":9940521},"missingSlots":[]}
                """);

        RecognizedIntent result = service.recognize("查看作业ID 9940521提交记录");

        assertThat(result.intent()).isEqualTo(AgentIntent.QUERY_ASSIGNMENT_SUBMISSIONS);
        assertThat(result.slots()).containsEntry("assignmentId", 9940521L);
    }

    @Test
    void removesMissingSlotsWhenRuleBasedEnrichmentProvidesThem() {
        LlmIntentRecognitionService service = serviceWithResponse("""
                {
                  "intent": "PUBLISH_ASSIGNMENT",
                  "confidence": 0.93,
                  "slots": {
                    "title": "Spring Cloud实验",
                    "courseName": "Java企业开发"
                  },
                  "missingSlots": ["maxScore"]
                }
                """);

        RecognizedIntent result = service.recognize("给Java企业开发课程发布作业，标题是Spring Cloud实验，满分100");

        assertThat(result.intent()).isEqualTo(AgentIntent.PUBLISH_ASSIGNMENT);
        assertThat(result.slots()).containsEntry("maxScore", 100);
        assertThat(result.missingSlots()).doesNotContain("maxScore");
    }

    @Test
    void enrichesClassNameFromRuleParserWhenLlmOmitsCourseDisambiguationSlot() {
        LlmIntentRecognitionService service = serviceWithResponse("""
                {
                  "intent": "PUBLISH_ASSIGNMENT",
                  "confidence": 0.95,
                  "slots": {
                    "title": "Java基础",
                    "courseName": "云计算技术",
                    "semester": "第二学期",
                    "maxScore": 100,
                    "dueDate": "2026-06-17 23:59"
                  },
                  "missingSlots": []
                }
                """);

        RecognizedIntent result = service.recognize(
                "请在 第二学期《云计算技术》云计算技术1班 这门课下发布作业：标题“Java基础”，中等难度，2道题，满分100分，截止时间 2026-06-17 23:59。");

        assertThat(result.intent()).isEqualTo(AgentIntent.PUBLISH_ASSIGNMENT);
        assertThat(result.slots()).containsEntry("className", "云计算技术1班");
    }

    @Test
    @SuppressWarnings("unchecked")
    void parsesBatchNotificationIntentAndEnrichesStudentIds() {
        LlmIntentRecognitionService service = serviceWithResponse("""
                {
                  "intent": "SEND_BATCH_NOTIFICATION",
                  "confidence": 0.95,
                  "slots": {
                    "title": "开课通知",
                    "content": "请按时上课",
                    "type": "course"
                  },
                  "missingSlots": ["studentIds"]
                }
                """);

        RecognizedIntent result = service.recognize("给学生ID 42,43批量发送通知，标题是开课通知，内容是请按时上课，类型是course");

        assertThat(result.intent()).isEqualTo(AgentIntent.SEND_BATCH_NOTIFICATION);
        assertThat((List<Number>) result.slots().get("studentIds")).extracting(Number::longValue)
                .containsExactly(42L, 43L);
        assertThat(result.missingSlots()).doesNotContain("studentIds");
    }

    @Test
    void dropsUnknownLlmSlotKeysAndMissingSlotNames() {
        LlmIntentRecognitionService service = serviceWithResponse("""
                {
                  "intent": "PUBLISH_ASSIGNMENT",
                  "confidence": 0.95,
                  "slots": {
                    "title": "Spring Cloud实验",
                    "courseName": "Java企业开发",
                    "maxScore": 100,
                    "directSql": "delete from users",
                    "targetService": "auth-service",
                    "admin": true
                  },
                  "missingSlots": ["dueDate", "adminPassword", "targetService"]
                }
                """);

        RecognizedIntent result = service.recognize("给Java企业开发课程发布作业，标题是Spring Cloud实验，满分100");

        assertThat(result.intent()).isEqualTo(AgentIntent.PUBLISH_ASSIGNMENT);
        assertThat(result.slots())
                .containsEntry("title", "Spring Cloud实验")
                .containsEntry("courseName", "Java企业开发")
                .containsEntry("maxScore", 100)
                .doesNotContainKeys("directSql", "targetService", "admin");
        assertThat(result.missingSlots()).containsExactly("dueDate");
    }

    @Test
    void preservesImplementedPublishExamBusinessSlots() {
        LlmIntentRecognitionService service = serviceWithResponse("""
                {
                  "intent": "PUBLISH_EXAM",
                  "confidence": 0.95,
                  "slots": {
                    "title": "Spring Boot阶段测验",
                    "courseId": 8,
                    "description": "覆盖微服务基础",
                    "startTime": "2026-12-30 09:00:00",
                    "endTime": "2026-12-30 10:00:00",
                    "publishDate": "2026-12-20 08:00:00",
                    "isActive": true,
                    "isOnline": false,
                    "location": "教学楼A101",
                    "duration": 60,
                    "targetService": "exam-service"
                  },
                  "missingSlots": []
                }
                """);

        RecognizedIntent result = service.recognize("发布线下考试，标题是Spring Boot阶段测验，课程ID 8，地点教学楼A101");

        assertThat(result.intent()).isEqualTo(AgentIntent.PUBLISH_EXAM);
        assertThat(result.slots())
                .containsEntry("description", "覆盖微服务基础")
                .containsEntry("publishDate", "2026-12-20 08:00:00")
                .containsEntry("isActive", true)
                .containsEntry("isOnline", false)
                .containsEntry("location", "教学楼A101")
                .doesNotContainKey("targetService");
    }

    @Test
    void preservesImplementedCreateCourseBusinessSlots() {
        LlmIntentRecognitionService service = serviceWithResponse("""
                {
                  "intent": "CREATE_COURSE",
                  "confidence": 0.95,
                  "slots": {
                    "courseName": "分布式框架技术",
                    "courseCode": "DFT101",
                    "description": "Spring Cloud 与 Agent 实践",
                    "credit": 3,
                    "totalHours": 48,
                    "courseCategory": "必修",
                    "courseStatus": "active",
                    "semester": "2026春",
                    "targetService": "course-service"
                  },
                  "missingSlots": []
                }
                """);

        RecognizedIntent result = service.recognize("创建课程，课程名称是分布式框架技术，课程代码是DFT101，学分3，总学时48");

        assertThat(result.intent()).isEqualTo(AgentIntent.CREATE_COURSE);
        assertThat(result.slots())
                .containsEntry("courseName", "分布式框架技术")
                .containsEntry("courseCode", "DFT101")
                .containsEntry("description", "Spring Cloud 与 Agent 实践")
                .containsEntry("credit", 3)
                .containsEntry("totalHours", 48)
                .containsEntry("courseCategory", "必修")
                .containsEntry("courseStatus", "active")
                .containsEntry("semester", "2026春")
                .doesNotContainKey("targetService");
    }

    @Test
    void preservesImplementedUpdateCourseBusinessSlots() {
        LlmIntentRecognitionService service = serviceWithResponse("""
                {
                  "intent": "UPDATE_COURSE",
                  "confidence": 0.95,
                  "slots": {
                    "courseId": 101,
                    "courseName": "高级分布式框架技术",
                    "courseCode": "DFT201",
                    "description": "进阶 Spring Cloud 与 Agent 实践",
                    "credit": 4,
                    "totalHours": 64,
                    "courseCategory": "专业核心",
                    "courseStatus": "active",
                    "semester": "2026秋",
                    "targetService": "course-service"
                  },
                  "missingSlots": []
                }
                """);

        RecognizedIntent result = service.recognize("更新课程ID 101，课程名称是高级分布式框架技术，课程代码是DFT201，学分4，总学时64");

        assertThat(result.intent()).isEqualTo(AgentIntent.UPDATE_COURSE);
        assertThat(result.slots())
                .containsEntry("courseId", 101)
                .containsEntry("courseName", "高级分布式框架技术")
                .containsEntry("courseCode", "DFT201")
                .containsEntry("description", "进阶 Spring Cloud 与 Agent 实践")
                .containsEntry("credit", 4)
                .containsEntry("totalHours", 64)
                .containsEntry("courseCategory", "专业核心")
                .containsEntry("courseStatus", "active")
                .containsEntry("semester", "2026秋")
                .doesNotContainKey("targetService");
    }

    @Test
    void preservesImplementedCreateClassBusinessSlots() {
        LlmIntentRecognitionService service = serviceWithResponse("""
                {
                  "intent": "CREATE_CLASS",
                  "confidence": 0.95,
                  "slots": {
                    "className": "软件2301",
                    "year": "2023",
                    "capacity": 40,
                    "courseId": 101,
                    "majorId": 2,
                    "classTime": "周一 1-2节",
                    "classLocation": "教学楼A101",
                    "targetService": "course-service"
                  },
                  "missingSlots": []
                }
                """);

        RecognizedIntent result = service.recognize("创建班级，班级名称是软件2301，年级2023，容量40，课程ID 101，专业ID 2");

        assertThat(result.intent()).isEqualTo(AgentIntent.CREATE_CLASS);
        assertThat(result.slots())
                .containsEntry("className", "软件2301")
                .containsEntry("year", "2023")
                .containsEntry("capacity", 40)
                .containsEntry("courseId", 101)
                .containsEntry("majorId", 2)
                .containsEntry("classTime", "周一 1-2节")
                .containsEntry("classLocation", "教学楼A101")
                .doesNotContainKey("targetService");
    }

    @Test
    void preservesImplementedGradeAndAnalysisBusinessSlots() {
        LlmIntentRecognitionService service = serviceWithResponse("""
                {
                  "intent": "GRADE_ASSIGNMENT",
                  "confidence": 0.95,
                  "slots": {
                    "submissionId": 33,
                    "score": 92,
                    "teacherComment": "结构清晰",
                    "classId": 7,
                    "timeRange": "本周",
                    "directToolCall": {"service":"assignment-service"}
                  },
                  "missingSlots": ["teacherComment", "directToolCall"]
                }
                """);

        RecognizedIntent result = service.recognize("给提交记录ID 33批改作业，分数92，评语结构清晰");

        assertThat(result.intent()).isEqualTo(AgentIntent.GRADE_ASSIGNMENT);
        assertThat(result.slots())
                .containsEntry("submissionId", 33)
                .containsEntry("score", 92)
                .containsEntry("teacherComment", "结构清晰")
                .containsEntry("classId", 7)
                .containsEntry("timeRange", "本周")
                .doesNotContainKey("directToolCall");
        assertThat(result.missingSlots()).doesNotContain("teacherComment", "directToolCall");
    }

    @Test
    @SuppressWarnings("unchecked")
    void preservesAllowedNestedAnswerPayloads() {
        LlmIntentRecognitionService service = serviceWithResponse("""
                {
                  "intent": "SUBMIT_EXAM",
                  "confidence": 0.95,
                  "slots": {
                    "examTitle": "Java期末考试",
                    "timeTaken": 45,
                    "answers": {"1":"A","2":"B"},
                    "directToolCall": {"service":"exam-service"}
                  },
                  "missingSlots": []
                }
                """);

        RecognizedIntent result = service.recognize("提交Java期末考试，用时45分钟，答案是1:A,2:B");

        assertThat(result.intent()).isEqualTo(AgentIntent.SUBMIT_EXAM);
        assertThat((Map<String, Object>) result.slots().get("answers"))
                .containsEntry("1", "A")
                .containsEntry("2", "B");
        assertThat(result.slots()).doesNotContainKey("directToolCall");
    }

    @Test
    void logsLlmPromptOutputAndRecognizedIntentWithSensitiveDataMasked() {
        Logger logger = (Logger) LoggerFactory.getLogger(LlmIntentRecognitionService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        Level originalLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        try {
            LlmIntentRecognitionService service = serviceWithResponse("""
                    {
                      "intent": "PUBLISH_ASSIGNMENT",
                      "confidence": 0.91,
                      "slots": {
                        "title": "Spring Cloud实验",
                        "content": "联系我 13912345678，邮箱 alice@example.com，apiKey=abc123"
                      },
                      "missingSlots": []
                    }
                    """);

            service.recognize("给Java课发布作业，手机号13912345678，邮箱alice@example.com，apiKey=abc123");

            String logs = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(java.util.stream.Collectors.joining("\n"));
            assertThat(logs)
                    .contains("agent llm intent prompt")
                    .contains("agent llm intent output")
                    .contains("agent llm intent recognized")
                    .contains("139****5678")
                    .contains("a***@example.com")
                    .contains("apiKey=***")
                    .doesNotContain("13912345678")
                    .doesNotContain("alice@example.com")
                    .doesNotContain("apiKey=abc123");
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(originalLevel);
        }
    }

    private LlmIntentRecognitionService serviceWithResponse(String response) {
        AgentLlmProperties properties = new AgentLlmProperties();
        properties.setMinimumConfidence(0.7);
        ChatModel chatModel = new ChatModel() {
            @Override
            public String chat(String userMessage) {
                return response;
            }
        };
        return new LlmIntentRecognitionService(chatModel, fallback, objectMapper, properties);
    }
}
