package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.api.dto.AgentChatRequestDTO;
import com._202510007517.platform.agent.api.dto.AgentChatResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultAgentChatStreamingServiceTest {

    private final AgentOrchestrator orchestrator = mock(AgentOrchestrator.class);
    private final DefaultAgentChatStreamingService service =
            new DefaultAgentChatStreamingService(orchestrator, new ObjectMapper());

    @Test
    void chatForwardsRequestContextToOrchestrator() {
        AgentChatRequestDTO request = new AgentChatRequestDTO();
        request.setSessionId("session-1");
        request.setMessage("把这个题发布到班级");
        request.setContext(Map.of(
                "page", "teacher-question-bank",
                "selectedQuestionIds", List.of(101, 102)));

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

        AgentChatResponseDTO actual = service.chat(request, 7L, "TEACHER");

        assertThat(actual.getMessage()).isEqualTo("已为你准备发布操作。");
    }

    @Test
    void emitsStartEventBeforeOrchestratorCompletes() throws Exception {
        CountDownLatch chatStarted = new CountDownLatch(1);
        CountDownLatch allowChatReturn = new CountDownLatch(1);
        CountDownLatch firstSend = new CountDownLatch(1);
        List<String> chunks = new CopyOnWriteArrayList<>();

        when(orchestrator.streamChat(eq(7L), eq("STUDENT"), argThat((AgentChatRequestDTO request) ->
                "session-1".equals(request.getSessionId()) && "提交作业".equals(request.getMessage())), any()))
                .thenAnswer(invocation -> {
                    chatStarted.countDown();
                    assertThat(allowChatReturn.await(2, TimeUnit.SECONDS)).isTrue();
                    AgentChatResponseDTO response = new AgentChatResponseDTO();
                    response.setSessionId("284");
                    response.setResponseType("ACTION_PREVIEW");
                    response.setMessage("请确认是否执行该操作。");
                    return response;
                });

        SseEmitter emitter = service.streamChat(7L, "STUDENT", "session-1", "提交作业");
        attachHandler(emitter, chunks, firstSend);

        assertThat(chatStarted.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(firstSend.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(waitFor(() -> joined(chunks).contains("event:start"), 1_000)).isTrue();
        assertThat(joined(chunks)).doesNotContain("event:result");

        allowChatReturn.countDown();

        assertThat(waitFor(() -> joined(chunks).contains("event:result"), 2_000)).isTrue();
        assertThat(waitFor(() -> joined(chunks).contains("event:done"), 2_000)).isTrue();
    }

    @Test
    void emitsDeltaBeforeStreamingOrchestratorCompletes() throws Exception {
        CountDownLatch allowStreamReturn = new CountDownLatch(1);
        CountDownLatch firstSend = new CountDownLatch(1);
        List<String> chunks = new CopyOnWriteArrayList<>();

        when(orchestrator.streamChat(eq(7L), eq("STUDENT"), argThat((AgentChatRequestDTO request) ->
                "session-1".equals(request.getSessionId()) && "你好".equals(request.getMessage())), any()))
                .thenAnswer(invocation -> {
                    AgentStreamingCallback callback = invocation.getArgument(3);
                    callback.onPartialResponse("你");
                    assertThat(allowStreamReturn.await(2, TimeUnit.SECONDS)).isTrue();
                    callback.onPartialResponse("好");

                    AgentChatResponseDTO response = new AgentChatResponseDTO();
                    response.setSessionId("284");
                    response.setResponseType("TEXT");
                    response.setMessage("你好");
                    return response;
                });

        SseEmitter emitter = service.streamChat(7L, "STUDENT", "session-1", "你好");
        attachHandler(emitter, chunks, firstSend);

        assertThat(firstSend.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(waitFor(() -> joined(chunks).contains("event:delta"), 1_000)).isTrue();
        assertThat(joined(chunks)).contains("\"text\":\"你\"");
        assertThat(joined(chunks)).doesNotContain("event:result");

        allowStreamReturn.countDown();

        assertThat(waitFor(() -> joined(chunks).contains("event:result"), 2_000)).isTrue();
        assertThat(joined(chunks)).contains("\"message\":\"你好\"");
    }

    private void attachHandler(SseEmitter emitter, List<String> chunks, CountDownLatch firstSend) throws Exception {
        Class<?> handlerType = Class.forName(
                "org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter$Handler");
        Method initialize = ResponseBodyEmitter.class.getDeclaredMethod("initialize", handlerType);
        initialize.setAccessible(true);

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("send".equals(method.getName()) && args != null && args.length > 0) {
                collectSentData(args[0], chunks, firstSend);
            }
            return null;
        };

        Object handler = Proxy.newProxyInstance(
                handlerType.getClassLoader(),
                new Class<?>[]{handlerType},
                invocationHandler);
        initialize.invoke(emitter, handler);
    }

    private void collectSentData(Object payload, List<String> chunks, CountDownLatch firstSend) throws Exception {
        if (payload instanceof Set<?> batch) {
            for (Object item : batch) {
                chunks.add(extractChunk(item));
                firstSend.countDown();
            }
            return;
        }
        chunks.add(String.valueOf(payload));
        firstSend.countDown();
    }

    private String extractChunk(Object dataWithMediaType) throws Exception {
        Method getData = dataWithMediaType.getClass().getMethod("getData");
        return String.valueOf(getData.invoke(dataWithMediaType));
    }

    private boolean waitFor(Check check, long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (check.evaluate()) {
                return true;
            }
            Thread.sleep(25);
        }
        return check.evaluate();
    }

    private String joined(List<String> chunks) {
        return String.join("", chunks);
    }

    @FunctionalInterface
    private interface Check {
        boolean evaluate() throws Exception;
    }
}
