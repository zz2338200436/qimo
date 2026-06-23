package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.api.dto.AgentChatRequestDTO;
import com._202510007517.platform.agent.api.dto.AgentChatResponseDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class DefaultAgentChatStreamingService implements AgentChatStreamingService {
    private static final Logger log = LoggerFactory.getLogger(DefaultAgentChatStreamingService.class);
    private static final long STREAM_TIMEOUT_MS = Duration.ofMinutes(5).toMillis();
    private static final ExecutorService EXECUTOR = new ThreadPoolExecutor(2, 8, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(128), runnable -> {
        Thread thread = new Thread(runnable, "agent-chat-stream");
        thread.setDaemon(true);
        return thread;
    }, new ThreadPoolExecutor.AbortPolicy());

    private final AgentOrchestrator orchestrator;
    private final ObjectMapper objectMapper;

    public DefaultAgentChatStreamingService(AgentOrchestrator orchestrator, ObjectMapper objectMapper) {
        this.orchestrator = orchestrator;
        this.objectMapper = objectMapper;
    }

    @Override
    public SseEmitter streamChat(Long userId, String userRole, String sessionId, String message) {
        AgentChatRequestDTO request = new AgentChatRequestDTO();
        request.setSessionId(sessionId);
        request.setMessage(message);
        return streamChat(userId, userRole, request);
    }

    @Override
    public SseEmitter streamChat(Long userId, String userRole, AgentChatRequestDTO request) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        try {
            EXECUTOR.execute(() -> {
                try {
                    sendEvent(emitter, "start", startPayload(request.getSessionId()));
                    AgentChatResponseDTO response = orchestrator.chat(
                            userId,
                            userRole,
                            request.getSessionId(),
                            request.getMessage(),
                            request.getContext());
                    sendEvent(emitter, "session", Map.of("sessionId", response.getSessionId()));
                    emitResponse(emitter, response);
                    sendEvent(emitter, "done", Map.of());
                    emitter.complete();
                } catch (Exception ex) {
                    completeWithError(emitter, ex);
                }
            });
        } catch (RejectedExecutionException ex) {
            completeWithError(emitter, new IllegalStateException("Agent stream is busy. Please retry later.", ex));
        }
        return emitter;
    }

    private void completeWithError(SseEmitter emitter, Exception ex) {
        try {
            sendEvent(emitter, "error", Map.of("message", safeMessage(ex)));
            sendEvent(emitter, "done", Map.of());
        } catch (Exception ignored) {
            // ignore secondary failures while closing the stream
        }
        emitter.completeWithError(ex);
        log.warn("agent stream chat failed: {}", ex.getClass().getSimpleName());
    }

    @Override
    public AgentChatResponseDTO chat(Long userId, String userRole, String sessionId, String message) {
        return orchestrator.chat(userId, userRole, sessionId, message);
    }

    @Override
    public AgentChatResponseDTO chat(AgentChatRequestDTO request, Long userId, String userRole) {
        return orchestrator.chat(userId, userRole, request.getSessionId(), request.getMessage(), request.getContext());
    }

    private void emitResponse(SseEmitter emitter, AgentChatResponseDTO response) throws IOException {
        String message = response.getMessage();
        if (message != null && !message.isBlank()) {
            sendEvent(emitter, "delta", Map.of("text", message));
        }
        sendEvent(emitter, "result", normalizeResponse(response));
    }

    private Map<String, Object> normalizeResponse(AgentChatResponseDTO response) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", response.getSessionId());
        payload.put("responseType", response.getResponseType());
        payload.put("message", response.getMessage());
        payload.put("actionPreview", response.getActionPreview());
        payload.put("data", response.getData());
        return payload;
    }

    private Map<String, Object> startPayload(String sessionId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", "PROCESSING");
        if (sessionId != null && !sessionId.isBlank()) {
            payload.put("sessionId", sessionId);
        }
        return payload;
    }

    private void sendEvent(SseEmitter emitter, String name, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(name).data(json(data)));
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize SSE payload.", ex);
        }
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? "请求失败" : message;
    }
}
