package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.AgentServiceApplication;
import com._202510007517.platform.agent.assistant.AgentAssistantRequest;
import com._202510007517.platform.agent.assistant.AgentAssistantType;
import com._202510007517.platform.agent.assistant.AssistantConversationService;
import com._202510007517.platform.agent.api.dto.AgentExecutionResultDTO;
import com._202510007517.platform.agent.api.dto.AgentChatResponseDTO;
import com._202510007517.platform.agent.api.dto.AgentSessionDTO;
import com._202510007517.platform.agent.domain.AgentActionEntity;
import com._202510007517.platform.agent.client.AiEdgeClient;
import com._202510007517.platform.agent.client.StudentCourseEdgeClient;
import com._202510007517.platform.agent.questionbank.QuestionRagService;
import com._202510007517.platform.agent.rag.RagKnowledgeService;
import com._202510007517.platform.agent.repository.AgentActionRepository;
import com._202510007517.platform.agent.repository.AgentAuditLogRepository;
import com._202510007517.platform.agent.client.TeacherAssignmentEdgeClient;
import com._202510007517.platform.ai.api.dto.GenerateQuestionsRequestDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentStudentScoreDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentSubmitRequestDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentSubmissionDTO;
import com._202510007517.platform.assignment.api.dto.TeacherAssignmentUpsertRequestDTO;
import com._202510007517.platform.assignment.api.feign.AssignmentFeignClient;
import com._202510007517.platform.course.api.dto.CourseAssignmentDTO;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.dto.ClassUpsertRequestDTO;
import com._202510007517.platform.course.api.dto.CourseUpsertRequestDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.exam.api.dto.ExamDTO;
import com._202510007517.platform.exam.api.feign.ExamFeignClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = AgentServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.profiles.active=test",
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.config.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:agent-orchestrator;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "agent.question-bank.enabled=true",
                "agent.question-bank.document-paths[0]=../docs/question-bank/java/java-basic-sample.md",
                "agent.question-bank.min-score=0.0"
        })
class AgentOrchestratorTest {

    @Autowired
    private AgentOrchestrator orchestrator;

    @Autowired
    private AgentActionRepository actionRepository;

    @Autowired
    private AgentAuditLogRepository auditLogRepository;

    @MockitoBean
    private CourseFeignClient courseFeignClient;

    @MockitoBean
    private TeacherAssignmentEdgeClient teacherAssignmentEdgeClient;

    @MockitoBean
    private AiEdgeClient aiEdgeClient;

    @MockitoBean
    private AssignmentFeignClient assignmentFeignClient;

    @MockitoBean
    private ExamFeignClient examFeignClient;

    @MockitoBean
    private StudentCourseEdgeClient studentCourseEdgeClient;

    @MockitoBean
    private GeneralChatService generalChatService;

    @MockitoBean
    private RagKnowledgeService ragKnowledgeService;

    @MockitoBean
    private AssistantConversationService assistantConversationService;

    @MockitoBean(name = "questionBankEmbeddingClient")
    private QuestionRagService.EmbeddingClient questionBankEmbeddingClient;

    @BeforeEach
    void clearActions() {
        auditLogRepository.deleteAll();
        actionRepository.deleteAll();
        when(questionBankEmbeddingClient.embed(any())).thenReturn(List.of(1.0, 0.0));
        when(generalChatService.reply(any(), any(), any(), any()))
                .thenReturn("我是课程平台里的 AI 助手，可以聊天，也可以帮你处理课程、班级、作业和通知。");
        when(assistantConversationService.reply(any(), any()))
                .thenReturn("暂时还不能处理这个请求。");
    }

    @Test
    void createsPreviewForAssignmentPublishingInsteadOfExecuting() {
        stubJavaCourse();

        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null,
                "给Java课程发布作业，标题是Spring Cloud实验，截止明晚十点，满分100");

        assertThat(response.getResponseType()).isEqualTo("ACTION_PREVIEW");
        assertThat(response.getSessionId()).isNotBlank();
        assertThat(response.getActionPreview()).isNotNull();
        assertThat(response.getActionPreview().getIntent()).isEqualTo("PUBLISH_ASSIGNMENT");
        assertThat(response.getActionPreview().getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(response.getActionPreview().getIdempotencyKey()).isNotBlank();
        assertThat(response.getActionPreview().getPreview()).containsEntry("title", "Spring Cloud实验");
        assertThat(response.getActionPreview().getPreview()).containsEntry("maxScore", 100);

        AgentActionEntity action = actionRepository.findAll().get(0);
        assertThat(action.getStatus()).isEqualTo("PENDING_CONFIRMATION");
        assertThat(action.getIntent()).isEqualTo("PUBLISH_ASSIGNMENT");
        assertThat(action.getIdempotencyKey()).isEqualTo(response.getActionPreview().getIdempotencyKey());
    }

    @Test
    void resolvesCourseNameToCourseIdBeforeCreatingAssignmentPreview() {
        stubJavaCourse();

        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null,
                "给Java课程发布作业，标题是Spring Cloud实验，截止明晚十点，满分100");

        assertThat(response.getResponseType()).isEqualTo("ACTION_PREVIEW");
        assertThat(response.getActionPreview().getPreview())
                .containsEntry("courseName", "Java")
                .containsEntry("courseId", 12L);
    }

    @Test
    void asksForClearerCourseWhenCourseNameMatchesMultipleCourses() {
        CourseDTO first = new CourseDTO();
        first.setId(12L);
        first.setCourseName("Java 分布式框架");
        first.setCourseCode("JAVA-001");
        CourseDTO second = new CourseDTO();
        second.setId(13L);
        second.setCourseName("Java 企业开发");
        second.setCourseCode("JAVA-002");
        when(courseFeignClient.listTeacherCourses(7L, null, null, null, null)).thenReturn(List.of(first, second));

        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null,
                "给Java课程发布作业，标题是Spring Cloud实验，截止明晚十点，满分100");

        assertThat(response.getResponseType()).isEqualTo("TEXT");
        assertThat(response.getActionPreview()).isNull();
        assertThat(response.getMessage())
                .contains("课程")
                .contains("课程ID")
                .contains("学期")
                .contains("班级")
                .doesNotContain("更明确的课程");
        assertThat(actionRepository.findAll()).isEmpty();
    }

    @Test
    void resolvesCourseFromSemesterAndClassPhraseBeforeCreatingAssignmentPreview() {
        stubAmbiguousCloudCourse();

        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null,
                "请在 2025-2026-2 学期《云计算技术》软件工程23级1班 这门课下发布作业：标题“Java基础”，中等难度，2道题，满分100分，截止时间 2026-06-17 23:59。");

        assertThat(response.getResponseType()).isEqualTo("ACTION_PREVIEW");
        assertThat(response.getActionPreview()).isNotNull();
        assertThat(response.getActionPreview().getPreview())
                .containsEntry("courseName", "云计算技术")
                .containsEntry("className", "软件工程23级1班")
                .containsEntry("semester", "2025-2026-2")
                .containsEntry("courseId", 91005L)
                .containsEntry("title", "Java基础")
                .containsEntry("maxScore", 100)
                .containsEntry("dueDate", "2026-06-17 23:59");
    }

    @Test
    void resolvesAssignmentTitleToAssignmentIdBeforeCreatingSubmitPreview() {
        AssignmentStudentScoreDTO assignment = new AssignmentStudentScoreDTO();
        assignment.setRelatedId(100L);
        assignment.setTitle("数据库作业");
        when(assignmentFeignClient.listStudentScores(7L)).thenReturn(List.of(assignment));

        AgentChatResponseDTO response = orchestrator.chat(7L, "STUDENT", null,
                "提交数据库作业，内容是实验报告已完成");

        assertThat(response.getResponseType()).isEqualTo("ACTION_PREVIEW");
        assertThat(response.getActionPreview().getPreview())
                .containsEntry("assignmentTitle", "数据库作业")
                .containsEntry("assignmentId", 100L)
                .containsEntry("content", "实验报告已完成");
    }

    @Test
    void asksForClearerAssignmentWhenTitleMatchesMultipleAssignments() {
        AssignmentStudentScoreDTO first = new AssignmentStudentScoreDTO();
        first.setRelatedId(100L);
        first.setTitle("数据库作业");
        AssignmentStudentScoreDTO second = new AssignmentStudentScoreDTO();
        second.setRelatedId(101L);
        second.setTitle("数据库作业补交");
        when(assignmentFeignClient.listStudentScores(7L)).thenReturn(List.of(first, second));

        AgentChatResponseDTO response = orchestrator.chat(7L, "STUDENT", null,
                "提交数据库作业，内容是实验报告已完成");

        assertThat(response.getResponseType()).isEqualTo("TEXT");
        assertThat(response.getActionPreview()).isNull();
        assertThat(response.getMessage()).contains("作业");
        assertThat(actionRepository.findAll()).isEmpty();
    }

    @Test
    void returnsTextForUnknownMessageWithoutCreatingAction() {
        AgentChatResponseDTO response = orchestrator.chat(7L, "STUDENT", null, "今天食堂吃什么");

        assertThat(response.getResponseType()).isEqualTo("TEXT");
        assertThat(response.getActionPreview()).isNull();
        assertThat(response.getMessage()).contains("AI 助手");
        assertThat(actionRepository.findAll()).isEmpty();
    }

    @Test
    void routesGeneralConversationToChatModelWithoutCreatingAction() {
        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null, "你是什么模型");

        assertThat(response.getResponseType()).isEqualTo("TEXT");
        assertThat(response.getActionPreview()).isNull();
        assertThat(response.getMessage()).contains("AI 助手");
        assertThat(actionRepository.findAll()).isEmpty();
        verify(generalChatService).reply(7L, "TEACHER", response.getSessionId(), "你是什么模型");
    }

    @Test
    void unknownIntentUsesGeneralAssistantBoundary() {
        when(generalChatService.reply(eq(7L), eq("TEACHER"), anyString(), eq("你是谁")))
                .thenReturn("我是教学管理平台内置的 AI 助手。");

        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null, "你是谁");

        assertThat(response.getResponseType()).isEqualTo("TEXT");
        assertThat(response.getMessage()).isEqualTo("我是教学管理平台内置的 AI 助手。");
        assertThat(response.getSessionId()).isNotBlank();
        verify(generalChatService).reply(7L, "TEACHER", response.getSessionId(), "你是谁");
    }

    @Test
    void unknownIntentStillReturnsFallbackWhenAssistantCannotAnswer() {
        when(generalChatService.reply(eq(7L), eq("TEACHER"), anyString(), eq("你好")))
                .thenReturn("暂时还不能处理这个请求。");

        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null, "你好");

        assertThat(response.getMessage()).isEqualTo("暂时还不能处理这个请求。");
        assertThat(response.getResponseType()).isEqualTo("TEXT");
    }

    @Test
    void routesRagKnowledgeIntentToRagServiceWithoutCreatingAction() {
        when(assistantConversationService.reply(eq(AgentAssistantType.KNOWLEDGE), any(AgentAssistantRequest.class)))
                .thenReturn("服务注册与发现用于让服务实例动态登记并被调用方发现。");

        AgentChatResponseDTO response = orchestrator.chat(7L, "STUDENT", null, "什么是服务注册与发现？");

        assertThat(response.getResponseType()).isEqualTo("TEXT");
        assertThat(response.getActionPreview()).isNull();
        assertThat(response.getMessage()).contains("服务注册与发现");
        assertThat(actionRepository.findAll()).isEmpty();
        verify(assistantConversationService).reply(eq(AgentAssistantType.KNOWLEDGE), argThat(request ->
                request.userId().equals(7L)
                        && "STUDENT".equals(request.userRole())
                        && response.getSessionId().equals(request.sessionId())
                        && "什么是服务注册与发现？".equals(request.message())));
        verify(generalChatService, never()).reply(any(), any(), any(), any());
    }

    @Test
    void ragPathKeepsSessionScopedResponseShape() {
        when(assistantConversationService.reply(eq(AgentAssistantType.KNOWLEDGE), any(AgentAssistantRequest.class)))
                .thenReturn("服务注册与发现用于让微服务实例彼此定位。");

        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null, "什么是服务注册与发现");

        assertThat(response.getResponseType()).isEqualTo("TEXT");
        assertThat(response.getMessage()).contains("服务注册与发现");
        assertThat(response.getSessionId()).isNotBlank();
        verify(assistantConversationService).reply(eq(AgentAssistantType.KNOWLEDGE), argThat(request ->
                request.userId().equals(7L)
                        && "TEACHER".equals(request.userRole())
                        && response.getSessionId().equals(request.sessionId())
                        && "什么是服务注册与发现".equals(request.message())));
    }

    @Test
    void asksForMissingSlotsBeforeCreatingAssignmentPublishPreview() {
        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null, "帮我发布作业");

        assertThat(response.getResponseType()).isEqualTo("TEXT");
        assertThat(response.getActionPreview()).isNull();
        assertThat(response.getMessage()).contains("课程", "标题", "截止时间", "满分");
        assertThat(actionRepository.findAll()).isEmpty();
    }

    @Test
    void executesRandomQuestionGenerationWhenRequestIsGenericButExplicitlyRandom() {
        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null, "随机生成五道课堂练习题");

        assertThat(response.getResponseType()).isEqualTo("DATA");
        assertThat(response.getMessage()).isNotEqualTo("null");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertThat(data)
                .containsEntry("status", "EXECUTED")
                .containsEntry("count", 5)
                .containsEntry("topic", null)
                .containsKey("questions");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) data.get("questions");
        assertThat(questions).isNotEmpty();
        verify(aiEdgeClient, never()).generateQuestions(any(), any(), any(), any(GenerateQuestionsRequestDTO.class));
        assertThat(actionRepository.findAll()).isEmpty();
    }

    @Test
    void mergesFollowUpSlotsIntoPendingAssignmentPublishIntent() {
        stubJavaCourse();

        AgentChatResponseDTO first = orchestrator.chat(7L, "TEACHER", null, "帮我发布作业");

        AgentChatResponseDTO second = orchestrator.chat(7L, "TEACHER", first.getSessionId(),
                "给Java课程，标题是微服务实验，截止明晚，满分100");

        assertThat(second.getResponseType()).isEqualTo("ACTION_PREVIEW");
        assertThat(second.getActionPreview()).isNotNull();
        assertThat(second.getActionPreview().getIntent()).isEqualTo("PUBLISH_ASSIGNMENT");
        assertThat(second.getActionPreview().getPreview())
                .containsEntry("courseName", "Java")
                .containsEntry("title", "微服务实验")
                .containsEntry("dueDate", "明晚")
                .containsEntry("maxScore", 100);
        assertThat(actionRepository.findAll()).hasSize(1);
    }

    @Test
    void reusesGeneratedQuestionsAsAssignmentContentInFollowUpPublish() {
        stubJavaCourse();
        AssignmentDTO assignment = new AssignmentDTO();
        assignment.setId(99L);
        assignment.setTitle("Java课堂练习");
        when(teacherAssignmentEdgeClient.createAssignment(eq("7"), any()))
                .thenReturn(ResponseResult.created(assignment));

        AgentChatResponseDTO generated = orchestrator.chat(7L, "TEACHER", null,
                "生成2道Java基础中等难度题");
        AgentSessionDTO generatedSession = orchestrator.getSession(7L, "TEACHER", Long.valueOf(generated.getSessionId()));
        assertThat(generatedSession.getPendingIntent()).isEqualTo("PUBLISH_ASSIGNMENT");
        assertThat(String.valueOf(generatedSession.getPendingSlots().get("content")))
                .contains("Java中用于表示一个类继承另一个类的关键字是哪个？")
                .contains("char类型在Java中可以直接表示Unicode字符。");
        AgentChatResponseDTO preview = orchestrator.chat(7L, "TEACHER", generated.getSessionId(),
                "给Java课程发布作业，标题是Java课堂练习，截止明晚，满分100");

        assertThat(preview.getResponseType()).isEqualTo("ACTION_PREVIEW");
        assertThat(preview.getActionPreview().getPreview())
                .containsEntry("title", "Java课堂练习")
                .containsEntry("courseId", 12L);
        assertThat(String.valueOf(preview.getActionPreview().getPreview().get("content")))
                .contains("Java中用于表示一个类继承另一个类的关键字是哪个？")
                .contains("char类型在Java中可以直接表示Unicode字符。");

        AgentExecutionResultDTO result = orchestrator.confirm(
                7L,
                "TEACHER",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey());

        assertThat(result.getStatus()).isEqualTo("EXECUTED");
        var requestCaptor = forClass(TeacherAssignmentUpsertRequestDTO.class);
        verify(teacherAssignmentEdgeClient).createAssignment(eq("7"), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getDescription())
                .contains("Java中用于表示一个类继承另一个类的关键字是哪个？")
                .contains("char类型在Java中可以直接表示Unicode字符。");
    }

    @Test
    void generatedQuestionsCanBePublishedToResolvedClassWithDefaultScoreAndDueDate() {
        stubCloudClassAssignment();

        AgentChatResponseDTO generated = orchestrator.chat(7L, "TEACHER", null,
                "生成2道Java基础中等难度题");
        AgentChatResponseDTO preview = orchestrator.chat(7L, "TEACHER", generated.getSessionId(),
                "发布到云计算技术1班");

        assertThat(preview.getResponseType()).isEqualTo("ACTION_PREVIEW");
        assertThat(preview.getActionPreview().getPreview())
                .containsEntry("courseId", 91005L)
                .containsEntry("className", "云计算技术1班")
                .containsEntry("maxScore", 100);
        assertThat(String.valueOf(preview.getActionPreview().getPreview().get("dueDate")))
                .matches("\\d{4}-\\d{2}-\\d{2} 23:59:59");
        assertThat(String.valueOf(preview.getActionPreview().getPreview().get("content")))
                .contains("Java中用于表示一个类继承另一个类的关键字是哪个？");
    }

    @Test
    void generatedQuestionsCanUseFirstAvailableClassForTestRandomPublishTarget() {
        stubCloudClassAssignment();

        AgentChatResponseDTO generated = orchestrator.chat(7L, "TEACHER", null,
                "生成2道Java基础中等难度题");
        AgentChatResponseDTO preview = orchestrator.chat(7L, "TEACHER", generated.getSessionId(),
                "帮我随机找个班级发布，我测试一下能否正确发布");

        assertThat(preview.getResponseType()).isEqualTo("ACTION_PREVIEW");
        assertThat(preview.getActionPreview().getPreview())
                .containsEntry("targetSelectionMode", "FIRST_AVAILABLE_CLASS_FOR_TEST")
                .containsEntry("courseId", 91005L)
                .containsEntry("className", "云计算技术1班")
                .containsEntry("maxScore", 100);
        assertThat(String.valueOf(preview.getActionPreview().getPreview().get("dueDate")))
                .matches("\\d{4}-\\d{2}-\\d{2} 23:59:59");
    }

    @Test
    void greetingDoesNotReusePendingQuestionGenerationIntent() {
        AgentChatResponseDTO first = orchestrator.chat(7L, "TEACHER", null, "生成题目");

        assertThat(first.getResponseType()).isEqualTo("TEXT");

        AgentChatResponseDTO second = orchestrator.chat(7L, "TEACHER", first.getSessionId(), "HI");

        assertThat(second.getResponseType()).isEqualTo("TEXT");
        assertThat(second.getMessage()).contains("AI 助手");
        verify(generalChatService).reply(7L, "TEACHER", second.getSessionId(), "HI");

        AgentSessionDTO session = orchestrator.getSession(7L, "TEACHER", Long.valueOf(second.getSessionId()));
        assertThat(session.getPendingIntent()).isNull();
        assertThat(session.getPendingSlots()).isEmpty();
    }

    @Test
    void topicFollowUpStillContinuesPendingQuestionGenerationIntent() {
        AgentChatResponseDTO first = orchestrator.chat(7L, "TEACHER", null, "生成题目");

        AgentChatResponseDTO second = orchestrator.chat(7L, "TEACHER", first.getSessionId(), "Java基础");

        assertThat(second.getResponseType()).isEqualTo("DATA");
        assertThat(second.getData()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) second.getData();
        assertThat(data)
                .containsEntry("status", "EXECUTED")
                .containsKey("questions");
    }

    @Test
    void greetingDoesNotReusePendingAssignmentPublishIntentAfterQuestionGeneration() {
        AgentChatResponseDTO generated = orchestrator.chat(7L, "TEACHER", null,
                "生成2道Java基础中等难度题");

        AgentSessionDTO generatedSession = orchestrator.getSession(7L, "TEACHER", Long.valueOf(generated.getSessionId()));
        assertThat(generatedSession.getPendingIntent()).isEqualTo("PUBLISH_ASSIGNMENT");

        AgentChatResponseDTO followUp = orchestrator.chat(7L, "TEACHER", generated.getSessionId(), "你好");

        assertThat(followUp.getResponseType()).isEqualTo("TEXT");
        assertThat(followUp.getMessage()).contains("AI 助手");
        verify(generalChatService).reply(7L, "TEACHER", followUp.getSessionId(), "你好");

        AgentSessionDTO clearedSession = orchestrator.getSession(7L, "TEACHER", Long.valueOf(followUp.getSessionId()));
        assertThat(clearedSession.getPendingIntent()).isNull();
        assertThat(clearedSession.getPendingSlots()).isEmpty();
    }

    @Test
    void knowledgeQuestionDoesNotReusePendingAssignmentPublishIntentAfterQuestionGeneration() {
        AgentChatResponseDTO generated = orchestrator.chat(7L, "TEACHER", null,
                "生成2道Java基础中等难度题");

        AgentChatResponseDTO followUp = orchestrator.chat(
                7L,
                "TEACHER",
                generated.getSessionId(),
                "什么是服务注册与发现");

        assertThat(followUp.getResponseType()).isEqualTo("TEXT");
        assertThat(followUp.getMessage()).doesNotContain("发布课程或班级");
        verify(assistantConversationService).reply(eq(AgentAssistantType.KNOWLEDGE), any(AgentAssistantRequest.class));

        AgentSessionDTO clearedSession = orchestrator.getSession(7L, "TEACHER", Long.valueOf(followUp.getSessionId()));
        assertThat(clearedSession.getPendingIntent()).isNull();
        assertThat(clearedSession.getPendingSlots()).isEmpty();
    }

    @Test
    void usesPageContextToCreateSelectedQuestionAssignmentPreview() {
        stubJavaCourse();

        AgentChatResponseDTO response = orchestrator.chat(
                7L,
                "TEACHER",
                null,
                "把这个题发布到班级",
                Map.of(
                        "page", "teacher-question-bank",
                        "selectedQuestionIds", List.of(91022),
                        "selectedQuestionScore", 2,
                        "selectedQuestionType", "TRUE_FALSE",
                        "selectedQuestionContent", "服务注册中心通常保存服务实例的地址、端口和健康状态等信息。",
                        "currentCourseId", 12
                ));

        assertThat(response.getResponseType()).isEqualTo("ACTION_PREVIEW");
        assertThat(response.getActionPreview().getIntent()).isEqualTo("PUBLISH_ASSIGNMENT");
        assertThat(response.getActionPreview().getPreview())
                .containsEntry("courseId", 12L)
                .containsEntry("questionIds", List.of(91022L))
                .containsEntry("maxScore", 2)
                .containsEntry("selectionMode", "SELECTED_QUESTIONS");
        assertThat(String.valueOf(response.getActionPreview().getPreview().get("title"))).contains("服务注册中心");
    }

    @Test
    void selectedQuestionPublishWithoutCourseOnlyAsksForPublishTarget() {
        AgentChatResponseDTO response = orchestrator.chat(
                7L,
                "TEACHER",
                null,
                "把这个题发布到班级",
                Map.of(
                        "selectedQuestionIds", List.of(91022),
                        "selectedQuestionScore", 2,
                        "selectedQuestionType", "TRUE_FALSE",
                        "selectedQuestionContent", "服务注册中心通常保存服务实例的地址、端口和健康状态等信息。"
                ));

        assertThat(response.getResponseType()).isEqualTo("TEXT");
        assertThat(response.getMessage())
                .isEqualTo("我已准备好作业内容、标题、截止时间和满分。还需要选择发布课程或班级。");
        assertThat(actionRepository.findAll()).isEmpty();
    }

    @Test
    void asksForMissingSlotsBeforeExecutingReadOnlyDetailQuery() {
        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null, "查看课程详情");

        assertThat(response.getResponseType()).isEqualTo("TEXT");
        assertThat(response.getActionPreview()).isNull();
        assertThat(response.getMessage()).contains("课程ID");
        assertThat(actionRepository.findAll()).isEmpty();
    }

    @Test
    void reusesSingleCourseContextForCourseDetailFollowUp() {
        CourseDTO course = new CourseDTO();
        course.setId(12L);
        course.setCourseName("Java 分布式框架");
        when(courseFeignClient.listTeacherCourses(7L, null, null, null, null)).thenReturn(List.of(course));
        when(courseFeignClient.getCourse(12L)).thenReturn(course);

        AgentChatResponseDTO first = orchestrator.chat(7L, "TEACHER", null, "查看我的课程");
        AgentChatResponseDTO second = orchestrator.chat(7L, "TEACHER", first.getSessionId(), "查看课程详情");

        assertThat(second.getResponseType()).isEqualTo("DATA");
        assertThat(second.getData()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) second.getData();
        assertThat(data).containsEntry("status", "EXECUTED");
        assertThat(data).containsKey("course");
    }

    @Test
    void keepsPromptingForCourseIdWhenFollowUpContextIsAmbiguous() {
        CourseDTO firstCourse = new CourseDTO();
        firstCourse.setId(12L);
        firstCourse.setCourseName("Java 分布式框架");
        CourseDTO secondCourse = new CourseDTO();
        secondCourse.setId(13L);
        secondCourse.setCourseName("Java 企业开发");
        when(courseFeignClient.listTeacherCourses(7L, null, null, null, null))
                .thenReturn(List.of(firstCourse, secondCourse));

        AgentChatResponseDTO first = orchestrator.chat(7L, "TEACHER", null, "查看我的课程");
        AgentChatResponseDTO second = orchestrator.chat(7L, "TEACHER", first.getSessionId(), "查看课程详情");

        assertThat(second.getResponseType()).isEqualTo("TEXT");
        assertThat(second.getActionPreview()).isNull();
        assertThat(second.getMessage()).contains("课程ID");
    }

    @Test
    void confirmationExecutesToolAndWritesAuditLog() {
        stubJavaCourse();
        AssignmentDTO assignment = new AssignmentDTO();
        assignment.setId(99L);
        assignment.setTitle("Spring Cloud实验");
        when(teacherAssignmentEdgeClient.createAssignment(eq("7"), any()))
                .thenReturn(ResponseResult.created(assignment));

        AgentChatResponseDTO preview = orchestrator.chat(7L, "TEACHER", null,
                "给Java课程发布作业，标题是Spring Cloud实验，截止明晚十点，满分100");

        AgentExecutionResultDTO result = orchestrator.confirm(
                7L,
                "TEACHER",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey());

        assertThat(result.getStatus()).isEqualTo("EXECUTED");
        assertThat(result.getMessage()).isEqualTo("操作已执行。");

        AgentActionEntity action = actionRepository.findById(preview.getActionPreview().getActionId()).orElseThrow();
        assertThat(action.getStatus()).isEqualTo("EXECUTED");
        assertThat(action.getConfirmedAt()).isNotNull();
        assertThat(action.getExecutedAt()).isNotNull();
        assertThat(auditLogRepository.findAll()).hasSize(1);
        assertThat(auditLogRepository.findAll().get(0).getOperation()).isEqualTo("PUBLISH_ASSIGNMENT");
        assertThat(auditLogRepository.findAll().get(0).getSuccess()).isTrue();
    }

    @Test
    void reusesRecentlyPublishedAssignmentForFollowUpDetailQuery() {
        stubJavaCourse();
        AssignmentDTO publishedAssignment = new AssignmentDTO();
        publishedAssignment.setId(99L);
        publishedAssignment.setTitle("Spring Cloud实验");
        when(teacherAssignmentEdgeClient.createAssignment(eq("7"), any()))
                .thenReturn(ResponseResult.created(publishedAssignment));
        when(teacherAssignmentEdgeClient.getAssignment("7", 99L))
                .thenReturn(ResponseResult.success(publishedAssignment));

        AgentChatResponseDTO preview = orchestrator.chat(7L, "TEACHER", null,
                "给Java课程发布作业，标题是Spring Cloud实验，截止明晚十点，满分100");

        AgentExecutionResultDTO publishResult = orchestrator.confirm(
                7L,
                "TEACHER",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey());

        AgentChatResponseDTO detailResponse = orchestrator.chat(
                7L,
                "TEACHER",
                preview.getSessionId(),
                "查看我刚才发布的作业详情");

        assertThat(publishResult.getStatus()).isEqualTo("EXECUTED");
        assertThat(detailResponse.getResponseType()).isEqualTo("DATA");
        assertThat(detailResponse.getData()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) detailResponse.getData();
        assertThat(data).containsEntry("status", "EXECUTED");
        assertThat(data).containsKey("assignment");
        verify(teacherAssignmentEdgeClient).getAssignment("7", 99L);
    }

    @Test
    void reusesRecentlyPublishedExamForFollowUpDetailQuery() {
        stubJavaCourse();
        ExamDTO publishedExam = new ExamDTO();
        publishedExam.setId(88L);
        publishedExam.setTitle("期末考试");
        when(examFeignClient.createTeacherExam(eq(7L), any()))
                .thenReturn(publishedExam);
        when(examFeignClient.getTeacherExam(88L, 7L))
                .thenReturn(publishedExam);

        AgentChatResponseDTO preview = orchestrator.chat(7L, "TEACHER", null,
                "给Java课程发布考试，标题是期末考试，开始时间2026-12-30 09:00:00，结束时间2026-12-30 10:30:00，时长90分钟");

        AgentExecutionResultDTO publishResult = orchestrator.confirm(
                7L,
                "TEACHER",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey());

        AgentChatResponseDTO detailResponse = orchestrator.chat(
                7L,
                "TEACHER",
                preview.getSessionId(),
                "查看我刚才发布的考试");

        assertThat(publishResult.getStatus()).isEqualTo("EXECUTED");
        assertThat(detailResponse.getResponseType()).isEqualTo("DATA");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) detailResponse.getData();
        assertThat(data).containsEntry("status", "EXECUTED");
        assertThat(data).containsKey("exam");
        verify(examFeignClient).getTeacherExam(88L, 7L);
    }

    @Test
    void reusesRecentlyCreatedCourseForFollowUpDetailQuery() {
        CourseDTO createdCourse = new CourseDTO();
        createdCourse.setId(66L);
        createdCourse.setCourseName("分布式框架技术");
        createdCourse.setCourseCode("DFT101");
        when(courseFeignClient.createCourse(eq(7L), any(CourseUpsertRequestDTO.class)))
                .thenReturn(createdCourse);
        when(courseFeignClient.getCourse(66L)).thenReturn(createdCourse);

        AgentChatResponseDTO preview = orchestrator.chat(7L, "TEACHER", null,
                "创建课程，课程名称是分布式框架技术，课程代码是DFT101，学分3，总学时48");

        AgentExecutionResultDTO createResult = orchestrator.confirm(
                7L,
                "TEACHER",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey());

        AgentChatResponseDTO detailResponse = orchestrator.chat(
                7L,
                "TEACHER",
                preview.getSessionId(),
                "查看我刚创建的课程");

        assertThat(createResult.getStatus()).isEqualTo("EXECUTED");
        assertThat(detailResponse.getResponseType()).isEqualTo("DATA");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) detailResponse.getData();
        assertThat(data).containsEntry("status", "EXECUTED");
        assertThat(data).containsKey("course");
        verify(courseFeignClient).getCourse(66L);
    }

    @Test
    void reusesRecentlyCreatedClassForFollowUpDetailQuery() {
        when(courseFeignClient.createClass(eq(7L), any(ClassUpsertRequestDTO.class)))
                .thenReturn(2301L);
        when(courseFeignClient.getClass(7L, 2301L))
                .thenReturn(Map.of("id", 2301L, "className", "软件2301"));

        AgentChatResponseDTO preview = orchestrator.chat(7L, "TEACHER", null,
                "创建班级，班级名称是软件2301，年级2023，容量40，课程ID 12，专业ID 2");

        AgentExecutionResultDTO createResult = orchestrator.confirm(
                7L,
                "TEACHER",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey());

        AgentChatResponseDTO detailResponse = orchestrator.chat(
                7L,
                "TEACHER",
                preview.getSessionId(),
                "查看我刚创建的班级");

        assertThat(createResult.getStatus()).isEqualTo("EXECUTED");
        assertThat(detailResponse.getResponseType()).isEqualTo("DATA");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) detailResponse.getData();
        assertThat(data).containsEntry("status", "EXECUTED");
        assertThat(data).containsKey("class");
        verify(courseFeignClient).getClass(7L, 2301L);
    }

    @Test
    void confirmationRejectsExpiredPendingActionWithoutExecutingTool() {
        stubJavaCourse();
        AgentChatResponseDTO preview = orchestrator.chat(7L, "TEACHER", null,
                "给Java课程发布作业，标题是Spring Cloud实验，截止明晚十点，满分100");
        AgentActionEntity action = actionRepository.findById(preview.getActionPreview().getActionId()).orElseThrow();
        action.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        actionRepository.save(action);

        assertThatThrownBy(() -> orchestrator.confirm(
                7L,
                "TEACHER",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");

        AgentActionEntity expiredAction = actionRepository.findById(preview.getActionPreview().getActionId()).orElseThrow();
        assertThat(expiredAction.getStatus()).isEqualTo("EXPIRED");
        assertThat(expiredAction.getExecutedAt()).isNull();
        assertThat(auditLogRepository.findAll()).isEmpty();
    }

    @Test
    void criticalActionRepeatedFirstConfirmationReturnsSecondConfirmationStage() {
        stubJavaCourse();
        AgentChatResponseDTO preview = orchestrator.chat(7L, "TEACHER", null,
                "删除作业ID 88");

        AgentExecutionResultDTO first = orchestrator.confirm(
                7L,
                "TEACHER",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey());
        AgentExecutionResultDTO second = orchestrator.confirm(
                7L,
                "TEACHER",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey());

        assertThat(first.getStatus()).isEqualTo("PENDING_SECOND_CONFIRMATION");
        assertThat(second.getStatus()).isEqualTo("PENDING_SECOND_CONFIRMATION");
        assertThat(second.getResult()).containsEntry("secondConfirmationPhrase", "确认执行");
        AgentActionEntity action = actionRepository.findById(preview.getActionPreview().getActionId()).orElseThrow();
        assertThat(action.getStatus()).isEqualTo("PENDING_SECOND_CONFIRMATION");
        assertThat(action.getExecutedAt()).isNull();
        assertThat(auditLogRepository.findAll()).isEmpty();
    }

    @Test
    void canCancelCriticalActionWaitingForSecondConfirmation() {
        stubJavaCourse();
        AgentChatResponseDTO preview = orchestrator.chat(7L, "TEACHER", null,
                "删除作业ID 88");
        AgentExecutionResultDTO staged = orchestrator.confirm(
                7L,
                "TEACHER",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey());

        AgentExecutionResultDTO cancelled = orchestrator.cancel(
                7L,
                "TEACHER",
                preview.getActionPreview().getActionId());

        assertThat(staged.getStatus()).isEqualTo("PENDING_SECOND_CONFIRMATION");
        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
        assertThat(actionRepository.findById(preview.getActionPreview().getActionId()).orElseThrow().getStatus())
                .isEqualTo("CANCELLED");
    }

    @Test
    void secondConfirmationStageStillExpiresBeforeExecution() {
        stubJavaCourse();
        AgentChatResponseDTO preview = orchestrator.chat(7L, "TEACHER", null,
                "删除作业ID 88");
        AgentExecutionResultDTO staged = orchestrator.confirm(
                7L,
                "TEACHER",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey());
        AgentActionEntity action = actionRepository.findById(preview.getActionPreview().getActionId()).orElseThrow();
        action.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        actionRepository.save(action);

        assertThat(staged.getStatus()).isEqualTo("PENDING_SECOND_CONFIRMATION");
        assertThatThrownBy(() -> orchestrator.confirm(
                7L,
                "TEACHER",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey(),
                "确认执行"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");
        assertThat(actionRepository.findById(preview.getActionPreview().getActionId()).orElseThrow().getStatus())
                .isEqualTo("EXPIRED");
    }

    @Test
    void repeatedConfirmationWithSameIdempotencyKeyReturnsExecutedResultWithoutReExecutingTool() {
        stubJavaCourse();
        AssignmentDTO assignment = new AssignmentDTO();
        assignment.setId(99L);
        assignment.setTitle("Spring Cloud实验");
        when(teacherAssignmentEdgeClient.createAssignment(eq("7"), any()))
                .thenReturn(ResponseResult.created(assignment));
        AgentChatResponseDTO preview = orchestrator.chat(7L, "TEACHER", null,
                "给Java课程发布作业，标题是Spring Cloud实验，截止明晚十点，满分100");

        AgentExecutionResultDTO first = orchestrator.confirm(
                7L,
                "TEACHER",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey());
        AgentExecutionResultDTO second = orchestrator.confirm(
                7L,
                "TEACHER",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey());

        assertThat(first.getStatus()).isEqualTo("EXECUTED");
        assertThat(second.getStatus()).isEqualTo("EXECUTED");
        assertThat(second.getResult()).isEqualTo(first.getResult());
        assertThat(auditLogRepository.findAll()).hasSize(1);
        verify(teacherAssignmentEdgeClient).createAssignment(eq("7"), any());
    }

    @Test
    void confirmationExecutesAssignmentSubmitToolAndWritesAuditLog() {
        AssignmentStudentScoreDTO assignment = new AssignmentStudentScoreDTO();
        assignment.setRelatedId(100L);
        assignment.setTitle("数据库作业");
        when(assignmentFeignClient.listStudentScores(7L)).thenReturn(List.of(assignment));
        AssignmentSubmissionDTO submission = new AssignmentSubmissionDTO();
        submission.setId(300L);
        when(assignmentFeignClient.submit(eq(100L), any())).thenReturn(submission);

        AgentChatResponseDTO preview = orchestrator.chat(7L, "STUDENT", null,
                "提交数据库作业，内容是实验报告已完成");

        AgentExecutionResultDTO result = orchestrator.confirm(
                7L,
                "STUDENT",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey());

        assertThat(result.getStatus()).isEqualTo("EXECUTED");
        assertThat(result.getMessage()).isEqualTo("操作已执行。");
        var submitRequestCaptor = forClass(AssignmentSubmitRequestDTO.class);
        verify(assignmentFeignClient).submit(eq(100L), submitRequestCaptor.capture());
        assertThat(submitRequestCaptor.getValue().getStudentId()).isEqualTo(7L);
        assertThat(submitRequestCaptor.getValue().getContent()).isEqualTo("实验报告已完成");
        AgentActionEntity action = actionRepository.findById(preview.getActionPreview().getActionId()).orElseThrow();
        assertThat(action.getStatus()).isEqualTo("EXECUTED");
        assertThat(action.getConfirmedAt()).isNotNull();
        assertThat(action.getExecutedAt()).isNotNull();
        assertThat(auditLogRepository.findAll()).hasSize(1);
        assertThat(auditLogRepository.findAll().get(0).getOperation()).isEqualTo("SUBMIT_ASSIGNMENT");
        assertThat(auditLogRepository.findAll().get(0).getTargetService()).isEqualTo("assignment-service");
        assertThat(auditLogRepository.findAll().get(0).getSuccess()).isTrue();
    }

    private void stubJavaCourse() {
        CourseDTO course = new CourseDTO();
        course.setId(12L);
        course.setCourseName("Java 分布式框架");
        course.setCourseCode("JAVA-001");
        when(courseFeignClient.listTeacherCourses(7L, null, null, null, null)).thenReturn(List.of(course));
    }

    private void stubAmbiguousCloudCourse() {
        CourseDTO first = new CourseDTO();
        first.setId(91005L);
        first.setCourseName("云计算技术");
        first.setCourseCode("CLOUD-001");
        first.setSemester("2025-2026-2");
        CourseDTO second = new CourseDTO();
        second.setId(91006L);
        second.setCourseName("云计算技术");
        second.setCourseCode("CLOUD-002");
        second.setSemester("2024-2025-2");
        when(courseFeignClient.listTeacherCourses(7L, null, null, null, null)).thenReturn(List.of(first, second));

        CourseAssignmentDTO primaryAssignment = new CourseAssignmentDTO();
        primaryAssignment.setAssignmentId(501L);
        primaryAssignment.setCourseId(91005L);
        primaryAssignment.setCourseName("云计算技术");
        primaryAssignment.setClassName("软件工程23级1班");
        primaryAssignment.setSemester("2025-2026-2");
        CourseAssignmentDTO secondaryAssignment = new CourseAssignmentDTO();
        secondaryAssignment.setAssignmentId(502L);
        secondaryAssignment.setCourseId(91006L);
        secondaryAssignment.setCourseName("云计算技术");
        secondaryAssignment.setClassName("软件工程23级2班");
        secondaryAssignment.setSemester("2024-2025-2");
        when(courseFeignClient.listCourseAssignments(7L, null, null))
                .thenReturn(List.of(primaryAssignment, secondaryAssignment));
    }

    private void stubCloudClassAssignment() {
        CourseAssignmentDTO assignment = new CourseAssignmentDTO();
        assignment.setAssignmentId(501L);
        assignment.setCourseId(91005L);
        assignment.setCourseName("云计算技术");
        assignment.setClassName("云计算技术1班");
        assignment.setSemester("2025-2026-2");
        when(courseFeignClient.listCourseAssignments(7L, null, null))
                .thenReturn(List.of(assignment));
    }
}
