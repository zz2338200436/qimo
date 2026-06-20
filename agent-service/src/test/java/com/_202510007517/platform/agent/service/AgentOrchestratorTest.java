package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.AgentServiceApplication;
import com._202510007517.platform.agent.api.dto.AgentExecutionResultDTO;
import com._202510007517.platform.agent.api.dto.AgentChatResponseDTO;
import com._202510007517.platform.agent.domain.AgentActionEntity;
import com._202510007517.platform.agent.rag.RagKnowledgeService;
import com._202510007517.platform.agent.repository.AgentActionRepository;
import com._202510007517.platform.agent.repository.AgentAuditLogRepository;
import com._202510007517.platform.agent.client.TeacherAssignmentEdgeClient;
import com._202510007517.platform.assignment.api.dto.AssignmentDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentStudentScoreDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentSubmitRequestDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentSubmissionDTO;
import com._202510007517.platform.assignment.api.feign.AssignmentFeignClient;
import com._202510007517.platform.course.api.dto.CourseAssignmentDTO;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import com._202510007517.platform.common.web.ResponseResult;
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
                "spring.datasource.driver-class-name=org.h2.Driver"
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
    private AssignmentFeignClient assignmentFeignClient;

    @MockitoBean
    private GeneralChatService generalChatService;

    @MockitoBean
    private RagKnowledgeService ragKnowledgeService;

    @BeforeEach
    void clearActions() {
        auditLogRepository.deleteAll();
        actionRepository.deleteAll();
        when(generalChatService.reply(any(), any(), any()))
                .thenReturn("我是课程平台里的 AI 助手，可以聊天，也可以帮你处理课程、班级、作业和通知。");
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
        verify(generalChatService).reply(7L, "TEACHER", "你是什么模型");
    }

    @Test
    void routesRagKnowledgeIntentToRagServiceWithoutCreatingAction() {
        when(ragKnowledgeService.answer(7L, "STUDENT", "什么是服务注册与发现？"))
                .thenReturn("服务注册与发现用于让服务实例动态登记并被调用方发现。");

        AgentChatResponseDTO response = orchestrator.chat(7L, "STUDENT", null, "什么是服务注册与发现？");

        assertThat(response.getResponseType()).isEqualTo("TEXT");
        assertThat(response.getActionPreview()).isNull();
        assertThat(response.getMessage()).contains("服务注册与发现");
        assertThat(actionRepository.findAll()).isEmpty();
        verify(ragKnowledgeService).answer(7L, "STUDENT", "什么是服务注册与发现？");
        verify(generalChatService, never()).reply(any(), any(), any());
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
}
