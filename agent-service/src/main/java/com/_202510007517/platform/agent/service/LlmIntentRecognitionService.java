package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.api.dto.AgentMessageDTO;
import com._202510007517.platform.agent.config.AgentLlmProperties;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.RecognizedIntent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LlmIntentRecognitionService implements IntentRecognitionService {
    private static final Logger log = LoggerFactory.getLogger(LlmIntentRecognitionService.class);
    private static final Set<String> ALLOWED_SLOT_NAMES = Set.of(
            "title",
            "courseName",
            "courseCode",
            "courseId",
            "credit",
            "totalHours",
            "assignmentTitle",
            "assignmentId",
            "examTitle",
            "examId",
            "studentId",
            "studentIds",
            "notificationId",
            "content",
            "answers",
            "maxScore",
            "score",
            "submissionId",
            "teacherComment",
            "duration",
            "startTime",
            "endTime",
            "publishDate",
            "timeTaken",
            "dueDate",
            "description",
            "courseCategory",
            "courseStatus",
            "semester",
            "startDate",
            "endDate",
            "maxStudents",
            "isActive",
            "isOnline",
            "location",
            "type",
            "relatedId",
            "classId",
            "className",
            "grade",
            "year",
            "capacity",
            "classTime",
            "classLocation",
            "majorName",
            "majorId",
            "timeRange",
            "topic",
            "query",
            "url",
            "count",
            "difficulty",
            "totalScore"
    );

    private final ChatModel chatModel;
    private final IntentRecognitionService fallback;
    private final ObjectMapper objectMapper;
    private final AgentLlmProperties properties;
    private final AgentDataMaskingPolicy dataMaskingPolicy;

    public LlmIntentRecognitionService(ChatModel chatModel,
                                       IntentRecognitionService fallback,
                                       ObjectMapper objectMapper,
                                       AgentLlmProperties properties) {
        this(chatModel, fallback, objectMapper, properties, new AgentDataMaskingPolicy());
    }

    public LlmIntentRecognitionService(ChatModel chatModel,
                                       IntentRecognitionService fallback,
                                       ObjectMapper objectMapper,
                                       AgentLlmProperties properties,
                                       AgentDataMaskingPolicy dataMaskingPolicy) {
        this.chatModel = chatModel;
        this.fallback = fallback;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.dataMaskingPolicy = dataMaskingPolicy;
    }

    @Override
    public RecognizedIntent recognize(String message) {
        return recognize(message, List.of());
    }

    @Override
    public RecognizedIntent recognize(String message, List<AgentMessageDTO> recentMessages) {
        String prompt = buildPrompt(message, recentMessages);
        log.info("agent llm intent prompt: {}", dataMaskingPolicy.maskText(prompt));
        try {
            String output = chatModel.chat(prompt);
            log.info("agent llm intent output: {}", dataMaskingPolicy.maskText(output));
            RecognizedIntent recognizedIntent = parse(output);
            if (recognizedIntent.intent() == AgentIntent.UNKNOWN
                    || recognizedIntent.confidence() < properties.getMinimumConfidence()) {
                log.info("agent llm intent fallback: intent={}, confidence={}",
                        recognizedIntent.intent(), recognizedIntent.confidence());
                return fallback.recognize(message);
            }
            RecognizedIntent enrichedIntent = enrichWithRuleBasedSlots(message, recognizedIntent);
            log.info("agent llm intent recognized: intent={}, confidence={}, slots={}, missingSlots={}",
                    enrichedIntent.intent(),
                    enrichedIntent.confidence(),
                    dataMaskingPolicy.maskMetadata(enrichedIntent.slots()),
                    enrichedIntent.missingSlots());
            return enrichedIntent;
        } catch (RuntimeException ex) {
            log.info("agent llm intent fallback: reason={}", ex.getClass().getSimpleName());
            return fallback.recognize(message);
        }
    }

    private RecognizedIntent enrichWithRuleBasedSlots(String message, RecognizedIntent recognizedIntent) {
        RecognizedIntent ruleBasedIntent = fallback.recognize(message);
        if (isReadOnlyWriteConflict(ruleBasedIntent.intent(), recognizedIntent.intent())
                && ruleBasedIntent.confidence() >= properties.getMinimumConfidence()) {
            return ruleBasedIntent;
        }
        if (ruleBasedIntent.intent() != recognizedIntent.intent()) {
            return recognizedIntent;
        }
        Map<String, Object> slots = new LinkedHashMap<>(recognizedIntent.slots());
        ruleBasedIntent.slots().forEach(slots::putIfAbsent);
        List<String> missingSlots = recognizedIntent.missingSlots().stream()
                .filter(slot -> !hasSlot(slots, slot))
                .toList();
        return new RecognizedIntent(
                recognizedIntent.intent(),
                recognizedIntent.confidence(),
                slots,
                missingSlots
        );
    }

    private boolean hasSlot(Map<String, Object> slots, String key) {
        Object value = slots.get(key);
        return value != null && !String.valueOf(value).isBlank();
    }

    private boolean isReadOnlyWriteConflict(AgentIntent ruleIntent, AgentIntent llmIntent) {
        return isReadOnlyIntent(ruleIntent) && isWriteIntent(llmIntent);
    }

    private boolean isReadOnlyIntent(AgentIntent intent) {
        return switch (intent) {
            case QUERY_COURSES, QUERY_COURSE_DETAIL, QUERY_CLASSES, QUERY_CLASS_DETAIL,
                    QUERY_ASSIGNMENTS, QUERY_ASSIGNMENT_DETAIL, QUERY_ASSIGNMENT_SUBMISSIONS,
                    QUERY_PENDING_ASSIGNMENTS, QUERY_EXAMS, QUERY_EXAM_DETAIL, QUERY_EXAM_SUBMISSIONS,
                    QUERY_SCORES, QUERY_NOTIFICATIONS, QUERY_UNREAD_NOTIFICATION_COUNT,
                    QUERY_STUDENT_STATS, QUERY_STUDY_TIME_DISTRIBUTION,
                    QUERY_TEACHER_DASHBOARD, QUERY_LEARNING_SUMMARY,
                    QUERY_SCORE_TREND, QUERY_EARLY_WARNINGS, QUERY_KNOWLEDGE_POINTS, QUERY_KNOWLEDGE_MASTERY, QUERY_QUESTION_BANK,
                    QUERY_RAG_KNOWLEDGE, INTERNET_SEARCH, READ_WEB_PAGE -> true;
            default -> false;
        };
    }

    private boolean isWriteIntent(AgentIntent intent) {
        return switch (intent) {
            case PUBLISH_ASSIGNMENT, UPDATE_ASSIGNMENT, DELETE_ASSIGNMENT, GRADE_ASSIGNMENT,
                    SUBMIT_ASSIGNMENT, PUBLISH_EXAM, CREATE_COURSE, UPDATE_COURSE, DELETE_COURSE, CREATE_CLASS,
                    UPDATE_EXAM, DELETE_EXAM,
                    GRADE_EXAM, SUBMIT_EXAM, MARK_ALL_NOTIFICATIONS_READ, MARK_NOTIFICATION_READ, DELETE_NOTIFICATION,
                    DELETE_ALL_READ_NOTIFICATIONS, SEND_NOTIFICATION, SEND_BATCH_NOTIFICATION,
                    GENERATE_QUESTIONS, GENERATE_EXAM, GENERATE_LEARNING_SUGGESTIONS -> true;
            default -> false;
        };
    }

    private RecognizedIntent parse(String output) {
        try {
            LlmIntentResponse response = objectMapper.readValue(extractJson(output), LlmIntentResponse.class);
            AgentIntent intent = AgentIntent.valueOf(response.intent());
            double confidence = response.confidence();
            Map<String, Object> slots = sanitizeSlots(response.slots());
            List<String> missingSlots = sanitizeMissingSlots(response.missingSlots());
            return new RecognizedIntent(intent, confidence, slots, missingSlots);
        } catch (IllegalArgumentException | JsonProcessingException ex) {
            throw new IllegalStateException("Invalid LLM intent response.", ex);
        }
    }

    private Map<String, Object> sanitizeSlots(Map<String, Object> slots) {
        if (slots == null || slots.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        slots.forEach((key, value) -> {
            if (ALLOWED_SLOT_NAMES.contains(key)) {
                sanitized.put(key, value);
            }
        });
        return sanitized;
    }

    private List<String> sanitizeMissingSlots(List<String> missingSlots) {
        if (missingSlots == null || missingSlots.isEmpty()) {
            return List.of();
        }
        return missingSlots.stream()
                .filter(ALLOWED_SLOT_NAMES::contains)
                .toList();
    }

    private String buildPrompt(String message, List<AgentMessageDTO> recentMessages) {
        return """
                You are the intent recognition layer for an education platform Agent.
                Return strict JSON only, without markdown.
                The Agent can only execute these intents:
                %s

                JSON schema:
                {"intent":"INTENT_NAME","confidence":0.0,"slots":{},"missingSlots":[]}

                Use UNKNOWN when the user request is outside these intents.
                Do not execute operations. Only classify intent and extract slots.
                Extract concrete business slots when they are present in the user message.
                Use these slot names exactly:
                - title: assignment, exam, or generated-content title.
                - courseName: course name text, without the suffix "课程".
                - courseCode: course code.
                - courseId: numeric course id.
                - credit: numeric course credit.
                - totalHours: numeric total course hours.
                - assignmentTitle: assignment name when the user submits or refers to an assignment.
                - assignmentId: numeric assignment id.
                - examTitle: exam or paper name when the user submits or refers to an exam.
                - examId: numeric exam id.
                - studentId: numeric student id.
                - studentIds: array of numeric student ids for batch notification sending.
                - notificationId: numeric notification id.
                - content: submission content after phrases like "内容是".
                - answers: submitted exam answers as an object keyed by question id, for example {"1":"A","2":"B"}.
                - maxScore: numeric full score.
                - score: numeric awarded score for grading a submission.
                - submissionId: numeric assignment or exam submission id for grading.
                - teacherComment: teacher grading comment text.
                - duration: exam duration in minutes.
                - startTime: exam start time text exactly as stated when present.
                - endTime: exam end time text exactly as stated when present.
                - publishDate: publish time text exactly as stated when present.
                - timeTaken: submitted exam time in minutes.
                - dueDate: deadline text exactly as stated when a deadline is present.
                - description: exam or course description text.
                - courseCategory: course category text.
                - courseStatus: course status text.
                - semester: course semester text.
                - startDate: course start date text.
                - endDate: course end date text.
                - maxStudents: course max student count.
                - isActive: boolean active/published status when explicitly stated.
                - isOnline: boolean exam online/offline status when explicitly stated.
                - location: offline exam location text.
                - type: notification type, for example course, assignment, exam, system, or announcement.
                - relatedId: optional numeric related business id for notifications.
                - classId: numeric class id.
                - className: class name text.
                - grade: class grade text.
                - year: class enrollment year text.
                - capacity: numeric class capacity.
                - classTime: class schedule text.
                - classLocation: class location text.
                - majorName: major name text.
                - majorId: numeric major id.
                - timeRange: analysis time range text such as 本周, 本月, or 最近30天.
                - topic: generated question topic text.
                - query: internet search keyword when the user explicitly asks to search online, browse the web, check latest information, or find official online sources.
                - url: full http or https URL when the user asks to read or summarize a web page.
                - count: generated question count.
                - difficulty: generated question or exam difficulty text.
                - totalScore: generated exam total score.
                Put absent required values in missingSlots only when the intent clearly requires them.

                Examples:
                User: 给Java企业开发课程发布作业，标题是Spring Cloud实验，满分100
                JSON: {"intent":"PUBLISH_ASSIGNMENT","confidence":0.95,"slots":{"courseName":"Java企业开发","title":"Spring Cloud实验","maxScore":100},"missingSlots":[]}
                User: 帮我提交数据库作业，内容是实验报告已完成
                JSON: {"intent":"SUBMIT_ASSIGNMENT","confidence":0.95,"slots":{"assignmentTitle":"数据库作业","content":"实验报告已完成"},"missingSlots":[]}
                User: 发布考试，标题是Spring Boot阶段测验，课程ID 8，开始时间2026-12-30 09:00:00，结束时间2026-12-30 10:00:00，考试时长60分钟
                JSON: {"intent":"PUBLISH_EXAM","confidence":0.95,"slots":{"title":"Spring Boot阶段测验","courseId":8,"startTime":"2026-12-30 09:00:00","endTime":"2026-12-30 10:00:00","duration":60},"missingSlots":[]}
                User: 创建课程，课程名称是分布式框架技术，课程代码是DFT101，学分3，总学时48
                JSON: {"intent":"CREATE_COURSE","confidence":0.95,"slots":{"courseName":"分布式框架技术","courseCode":"DFT101","credit":3,"totalHours":48},"missingSlots":[]}
                User: 更新课程ID 101，课程名称是高级分布式框架技术，课程代码是DFT201，学分4，总学时64
                JSON: {"intent":"UPDATE_COURSE","confidence":0.95,"slots":{"courseId":101,"courseName":"高级分布式框架技术","courseCode":"DFT201","credit":4,"totalHours":64},"missingSlots":[]}
                User: 删除课程ID 101
                JSON: {"intent":"DELETE_COURSE","confidence":0.95,"slots":{"courseId":101},"missingSlots":[]}
                User: 创建班级，班级名称是软件2301，年级2023，容量40，课程ID 101，专业ID 2
                JSON: {"intent":"CREATE_CLASS","confidence":0.95,"slots":{"className":"软件2301","year":"2023","capacity":40,"courseId":101,"majorId":2},"missingSlots":[]}
                User: 给提交记录ID 33批改作业，分数92，评语结构清晰
                JSON: {"intent":"GRADE_ASSIGNMENT","confidence":0.95,"slots":{"submissionId":33,"score":92,"teacherComment":"结构清晰"},"missingSlots":[]}
                User: 查询班级ID 7 本周学习概览
                JSON: {"intent":"QUERY_LEARNING_SUMMARY","confidence":0.95,"slots":{"classId":7,"timeRange":"本周"},"missingSlots":[]}
                User: 提交Java期末考试，用时45分钟，答案是1:A,2:B
                JSON: {"intent":"SUBMIT_EXAM","confidence":0.95,"slots":{"examTitle":"Java期末考试","timeTaken":45,"answers":{"1":"A","2":"B"}},"missingSlots":[]}
                User: 查询学生ID 12 在课程ID 3 的知识点掌握情况
                JSON: {"intent":"QUERY_KNOWLEDGE_MASTERY","confidence":0.95,"slots":{"studentId":12,"courseId":3},"missingSlots":[]}
                User: 学情预警
                JSON: {"intent":"QUERY_EARLY_WARNINGS","confidence":0.95,"slots":{},"missingSlots":[]}
                User: 分析全部学生的知识点掌握情况
                JSON: {"intent":"QUERY_KNOWLEDGE_MASTERY","confidence":0.95,"slots":{},"missingSlots":[]}
                User: 查看课程ID 3 的知识点
                JSON: {"intent":"QUERY_KNOWLEDGE_POINTS","confidence":0.95,"slots":{"courseId":3},"missingSlots":[]}
                User: 把通知ID 9标为已读
                JSON: {"intent":"MARK_NOTIFICATION_READ","confidence":0.95,"slots":{"notificationId":9},"missingSlots":[]}
                User: 删除通知ID 9
                JSON: {"intent":"DELETE_NOTIFICATION","confidence":0.95,"slots":{"notificationId":9},"missingSlots":[]}
                User: 删除所有已读通知
                JSON: {"intent":"DELETE_ALL_READ_NOTIFICATIONS","confidence":0.95,"slots":{},"missingSlots":[]}
                User: 给学生ID 42发送通知，标题是开课通知，内容是请按时上课，类型是course，相关ID是8
                JSON: {"intent":"SEND_NOTIFICATION","confidence":0.95,"slots":{"studentId":42,"title":"开课通知","content":"请按时上课","type":"course","relatedId":8},"missingSlots":[]}
                User: 给学生ID 42,43批量发送通知，标题是开课通知，内容是请按时上课，类型是course
                JSON: {"intent":"SEND_BATCH_NOTIFICATION","confidence":0.95,"slots":{"studentIds":[42,43],"title":"开课通知","content":"请按时上课","type":"course"},"missingSlots":[]}

                Recent conversation:
                %s

                User message:
                %s
                """.formatted(
                availableIntentNames(),
                formatRecentConversation(recentMessages),
                message == null ? "" : message
        );
    }

    private String formatRecentConversation(List<AgentMessageDTO> recentMessages) {
        if (recentMessages == null || recentMessages.isEmpty()) {
            return "(none)";
        }
        return recentMessages.stream()
                .map(message -> {
                    String role = message.getRole() == null || message.getRole().isBlank()
                            ? "UNKNOWN"
                            : message.getRole();
                    String content = message.getContent() == null ? "" : message.getContent();
                    return role + ": " + content;
                })
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String availableIntentNames() {
        return Arrays.stream(AgentIntent.values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }

    private String extractJson(String output) {
        if (output == null) {
            return "";
        }
        String text = output.trim();
        if (text.startsWith("```")) {
            int firstLineEnd = text.indexOf('\n');
            int fenceEnd = text.lastIndexOf("```");
            if (firstLineEnd >= 0 && fenceEnd > firstLineEnd) {
                return text.substring(firstLineEnd + 1, fenceEnd).trim();
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private record LlmIntentResponse(
            String intent,
            double confidence,
            Map<String, Object> slots,
            List<String> missingSlots
    ) {
    }
}
