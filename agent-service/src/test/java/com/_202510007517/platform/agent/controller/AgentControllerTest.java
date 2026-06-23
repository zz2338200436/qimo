package com._202510007517.platform.agent.controller;

import com._202510007517.platform.agent.api.dto.AgentActionPreviewDTO;
import com._202510007517.platform.agent.api.dto.AgentActionDTO;
import com._202510007517.platform.agent.api.dto.AgentChatResponseDTO;
import com._202510007517.platform.agent.api.dto.AgentExecutionResultDTO;
import com._202510007517.platform.agent.api.dto.AgentMessageDTO;
import com._202510007517.platform.agent.api.dto.AgentSessionDTO;
import com._202510007517.platform.agent.config.AgentServiceExceptionHandler;
import com._202510007517.platform.agent.service.AgentOrchestrator;
import com._202510007517.platform.agent.service.AgentChatStreamingService;
import com._202510007517.platform.common.web.CommonTraceConstants;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

class AgentControllerTest {

    private final AgentOrchestrator orchestrator = mock(AgentOrchestrator.class);
    private final AgentChatStreamingService streamingService = mock(AgentChatStreamingService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new AgentController(orchestrator, streamingService))
            .setControllerAdvice(new AgentServiceExceptionHandler(new MockEnvironment()))
            .build();

    @Test
    void chatReturnsActionPreviewEnvelope() throws Exception {
        AgentChatResponseDTO response = new AgentChatResponseDTO();
        response.setSessionId("1");
        response.setResponseType("ACTION_PREVIEW");
        response.setMessage("请确认是否执行该操作。");
        AgentActionPreviewDTO preview = new AgentActionPreviewDTO();
        preview.setActionId(11L);
        preview.setIntent("PUBLISH_ASSIGNMENT");
        preview.setRiskLevel("MEDIUM");
        preview.setPreview(Map.of("title", "Spring Cloud实验"));
        response.setActionPreview(preview);
        when(orchestrator.chat(
                eq(7L),
                eq("TEACHER"),
                eq(null),
                eq("发布作业，标题是Spring Cloud实验，满分100"),
                eq(Map.of())))
                .thenReturn(response);

        mockMvc.perform(post("/api/agent/chat")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7")
                        .header(CommonTraceConstants.ACTIVE_ROLE_HEADER, "TEACHER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"发布作业，标题是Spring Cloud实验，满分100\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.responseType").value("ACTION_PREVIEW"))
                .andExpect(jsonPath("$.data.actionPreview.intent").value("PUBLISH_ASSIGNMENT"));
    }

    @Test
    void chatForwardsRequestContextToOrchestrator() throws Exception {
        AgentChatResponseDTO response = new AgentChatResponseDTO();
        response.setSessionId("session-1");
        response.setResponseType("TEXT");
        response.setMessage("已为你准备发布操作。");

        when(orchestrator.chat(
                eq(7L),
                eq("TEACHER"),
                eq("session-1"),
                eq("把这个题发布到班级"),
                argThat(context -> "teacher-question-bank".equals(context.get("page"))
                        && List.of(101, 102).equals(context.get("selectedQuestionIds")))))
                .thenReturn(response);

        mockMvc.perform(post("/api/agent/chat")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7")
                        .header(CommonTraceConstants.ACTIVE_ROLE_HEADER, "TEACHER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "session-1",
                                  "message": "把这个题发布到班级",
                                  "context": {
                                    "page": "teacher-question-bank",
                                    "selectedQuestionIds": [101, 102]
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("已为你准备发布操作。"));
    }

    @Test
    void streamChatEmitsSseEventsForTextResponse() throws Exception {
        SseEmitter emitter = new SseEmitter();
        when(streamingService.streamChat(
                eq(7L),
                eq("TEACHER"),
                argThat(request -> request.getContext().isEmpty()
                        && request.getSessionId() == null
                        && "什么是服务注册与发现？".equals(request.getMessage()))))
                .thenReturn(emitter);

        var result = mockMvc.perform(post("/api/agent/chat/stream")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7")
                        .header(CommonTraceConstants.ACTIVE_ROLE_HEADER, "TEACHER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"message\":\"什么是服务注册与发现？\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        emitter.send(SseEmitter.event().name("session").data("{\"sessionId\":\"1\"}"));
        emitter.send(SseEmitter.event().name("delta").data("{\"text\":\"服务注册与发现用于\"}"));
        emitter.send(SseEmitter.event().name("result").data("{\"sessionId\":\"1\",\"responseType\":\"TEXT\",\"message\":\"服务注册与发现用于让微服务实例彼此定位。\"}"));
        emitter.complete();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("event:session")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("event:delta")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("event:result")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"responseType\":\"TEXT\"")));

        verify(streamingService).streamChat(
                eq(7L),
                eq("TEACHER"),
                argThat(request -> request.getContext().isEmpty()
                        && request.getSessionId() == null
                        && "什么是服务注册与发现？".equals(request.getMessage())));
    }

    @Test
    void streamChatForwardsRequestContextToStreamingService() throws Exception {
        SseEmitter emitter = new SseEmitter();
        when(streamingService.streamChat(
                eq(7L),
                eq("TEACHER"),
                argThat(request -> "session-1".equals(request.getSessionId())
                        && "把这个题发布到班级".equals(request.getMessage())
                        && "teacher-question-bank".equals(request.getContext().get("page"))
                        && List.of(101, 102).equals(request.getContext().get("selectedQuestionIds")))))
                .thenReturn(emitter);

        mockMvc.perform(post("/api/agent/chat/stream")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7")
                        .header(CommonTraceConstants.ACTIVE_ROLE_HEADER, "TEACHER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("""
                                {
                                  "sessionId": "session-1",
                                  "message": "把这个题发布到班级",
                                  "context": {
                                    "page": "teacher-question-bank",
                                    "selectedQuestionIds": [101, 102]
                                  }
                                }
                                """))
                .andExpect(request().asyncStarted());

        verify(streamingService).streamChat(
                eq(7L),
                eq("TEACHER"),
                argThat(request -> "session-1".equals(request.getSessionId())
                        && "把这个题发布到班级".equals(request.getMessage())
                        && "teacher-question-bank".equals(request.getContext().get("page"))
                        && List.of(101, 102).equals(request.getContext().get("selectedQuestionIds"))));
    }

    @Test
    void missingUserIdentityReturnsFailureEnvelope() throws Exception {
        mockMvc.perform(post("/api/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"查看我的课程\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void getActionReturnsOwnedActionEnvelope() throws Exception {
        AgentActionDTO action = new AgentActionDTO();
        action.setActionId(11L);
        action.setSessionId("3");
        action.setIntent("PUBLISH_ASSIGNMENT");
        action.setStatus("PENDING_CONFIRMATION");
        action.setRiskLevel("MEDIUM");
        action.setPreview(Map.of("title", "Spring Cloud实验"));
        when(orchestrator.getAction(eq(7L), eq("TEACHER"), eq(11L))).thenReturn(action);

        mockMvc.perform(get("/api/agent/actions/11")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7")
                        .header(CommonTraceConstants.ACTIVE_ROLE_HEADER, "TEACHER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.actionId").value(11))
                .andExpect(jsonPath("$.data.sessionId").value("3"))
                .andExpect(jsonPath("$.data.intent").value("PUBLISH_ASSIGNMENT"))
                .andExpect(jsonPath("$.data.preview.title").value("Spring Cloud实验"));
    }

    @Test
    void listSessionsReturnsOwnedSessionEnvelope() throws Exception {
        AgentSessionDTO session = new AgentSessionDTO();
        session.setSessionId("3");
        session.setUserRole("TEACHER");
        session.setStatus("ACTIVE");
        session.setPendingIntent("PUBLISH_ASSIGNMENT");
        when(orchestrator.listSessions(eq(7L), eq("TEACHER"))).thenReturn(List.of(session));

        mockMvc.perform(get("/api/agent/sessions")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7")
                        .header(CommonTraceConstants.ACTIVE_ROLE_HEADER, "TEACHER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].sessionId").value("3"))
                .andExpect(jsonPath("$.data[0].userRole").value("TEACHER"))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data[0].pendingIntent").value("PUBLISH_ASSIGNMENT"));
    }

    @Test
    void getSessionReturnsOwnedSessionWithActionsEnvelope() throws Exception {
        AgentMessageDTO userMessage = new AgentMessageDTO();
        userMessage.setRole("USER");
        userMessage.setContent("发布作业");
        AgentMessageDTO assistantMessage = new AgentMessageDTO();
        assistantMessage.setRole("ASSISTANT");
        assistantMessage.setContent("请确认是否执行该操作。");

        AgentActionDTO action = new AgentActionDTO();
        action.setActionId(11L);
        action.setIntent("PUBLISH_ASSIGNMENT");

        AgentSessionDTO session = new AgentSessionDTO();
        session.setSessionId("3");
        session.setUserRole("TEACHER");
        session.setStatus("ACTIVE");
        session.setMessages(List.of(userMessage, assistantMessage));
        session.setActions(List.of(action));
        when(orchestrator.getSession(eq(7L), eq("TEACHER"), eq(3L))).thenReturn(session);

        mockMvc.perform(get("/api/agent/sessions/3")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7")
                        .header(CommonTraceConstants.ACTIVE_ROLE_HEADER, "TEACHER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionId").value("3"))
                .andExpect(jsonPath("$.data.userRole").value("TEACHER"))
                .andExpect(jsonPath("$.data.messages[0].role").value("USER"))
                .andExpect(jsonPath("$.data.messages[0].content").value("发布作业"))
                .andExpect(jsonPath("$.data.messages[1].role").value("ASSISTANT"))
                .andExpect(jsonPath("$.data.messages[1].content").value("请确认是否执行该操作。"))
                .andExpect(jsonPath("$.data.actions[0].actionId").value(11))
                .andExpect(jsonPath("$.data.actions[0].intent").value("PUBLISH_ASSIGNMENT"));
    }

    @Test
    void confirmActionReturnsExecutionEnvelope() throws Exception {
        AgentExecutionResultDTO result = new AgentExecutionResultDTO();
        result.setActionId(11L);
        result.setIntent("PUBLISH_ASSIGNMENT");
        result.setStatus("EXECUTED");
        result.setMessage("操作已执行。");
        result.setResult(Map.of("status", "EXECUTED", "assignmentId", 99));
        when(orchestrator.confirm(eq(7L), eq("TEACHER"), eq(11L), eq("idem-1"), eq("确认执行"))).thenReturn(result);

        mockMvc.perform(post("/api/agent/actions/11/confirm")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7")
                        .header(CommonTraceConstants.ACTIVE_ROLE_HEADER, "TEACHER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"idem-1\",\"secondConfirmationText\":\"确认执行\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.actionId").value(11))
                .andExpect(jsonPath("$.data.status").value("EXECUTED"))
                .andExpect(jsonPath("$.data.message").value("操作已执行。"))
                .andExpect(jsonPath("$.data.result.assignmentId").value(99));
    }

    @Test
    void confirmRejectsActionOwnedByAnotherUserEnvelope() throws Exception {
        doThrow(new SecurityException("无权确认该 Agent 操作。"))
                .when(orchestrator).confirm(eq(8L), eq("TEACHER"), eq(11L), eq("idem-1"), eq(null));

        mockMvc.perform(post("/api/agent/actions/11/confirm")
                        .header(CommonTraceConstants.USER_ID_HEADER, "8")
                        .header(CommonTraceConstants.ACTIVE_ROLE_HEADER, "TEACHER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"idem-1\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权确认该 Agent 操作。"));
    }

    @Test
    void confirmRejectsExpiredActionEnvelope() throws Exception {
        doThrow(new IllegalStateException("Agent action has expired."))
                .when(orchestrator).confirm(eq(7L), eq("TEACHER"), eq(11L), eq("idem-1"), eq(null));

        mockMvc.perform(post("/api/agent/actions/11/confirm")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7")
                        .header(CommonTraceConstants.ACTIVE_ROLE_HEADER, "TEACHER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"idem-1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("Agent action has expired."));
    }

    @Test
    void confirmRejectsMismatchedIdempotencyKeyEnvelope() throws Exception {
        doThrow(new IllegalArgumentException("Agent action idempotency key mismatch."))
                .when(orchestrator).confirm(eq(7L), eq("TEACHER"), eq(11L), eq("wrong-key"), eq(null));

        mockMvc.perform(post("/api/agent/actions/11/confirm")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7")
                        .header(CommonTraceConstants.ACTIVE_ROLE_HEADER, "TEACHER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"wrong-key\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Agent action idempotency key mismatch."));
    }

    @Test
    void cancelActionReturnsCancellationEnvelope() throws Exception {
        AgentExecutionResultDTO result = new AgentExecutionResultDTO();
        result.setActionId(11L);
        result.setIntent("PUBLISH_ASSIGNMENT");
        result.setStatus("CANCELLED");
        result.setMessage("操作已取消。");
        when(orchestrator.cancel(eq(7L), eq("TEACHER"), eq(11L))).thenReturn(result);

        mockMvc.perform(post("/api/agent/actions/11/cancel")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7")
                        .header(CommonTraceConstants.ACTIVE_ROLE_HEADER, "TEACHER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.actionId").value(11))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.message").value("操作已取消。"));
    }
}
