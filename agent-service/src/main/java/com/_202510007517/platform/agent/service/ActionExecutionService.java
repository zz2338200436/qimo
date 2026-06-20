package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.api.dto.AgentExecutionResultDTO;
import com._202510007517.platform.agent.domain.AgentActionEntity;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.repository.AgentActionRepository;
import com._202510007517.platform.agent.tool.AgentTool;
import com._202510007517.platform.agent.tool.ToolRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ActionExecutionService {
    private static final String STATUS_EXECUTED = "EXECUTED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String DEFAULT_FAILURE_MESSAGE = "操作执行失败。";

    private final ToolRegistry toolRegistry;
    private final AgentActionRepository actionRepository;
    private final AgentAuditService auditService;
    private final AgentDataMaskingPolicy dataMaskingPolicy;
    private final ObjectMapper objectMapper;

    public ActionExecutionService(ToolRegistry toolRegistry,
                                  AgentActionRepository actionRepository,
                                  AgentAuditService auditService,
                                  AgentDataMaskingPolicy dataMaskingPolicy,
                                  ObjectMapper objectMapper) {
        this.toolRegistry = toolRegistry;
        this.actionRepository = actionRepository;
        this.auditService = auditService;
        this.dataMaskingPolicy = dataMaskingPolicy;
        this.objectMapper = objectMapper;
    }

    public AgentExecutionResultDTO execute(Long userId, String userRole, AgentActionEntity action,
                                           AgentIntent intent, Map<String, Object> request) {
        action.setConfirmedAt(action.getConfirmedAt() == null ? LocalDateTime.now() : action.getConfirmedAt());
        Map<String, Object> result;
        boolean success;
        String message;
        try {
            AgentTool tool = toolRegistry.resolve(intent);
            result = dataMaskingPolicy.maskMetadata(tool.execute(userId, userRole, request));
            success = STATUS_EXECUTED.equals(result.get("status"));
            message = success ? "操作已执行。" : String.valueOf(result.getOrDefault("message", "操作执行失败。"));
        } catch (RuntimeException ex) {
            String maskedMessage = dataMaskingPolicy.maskText(
                    ex.getMessage() == null || ex.getMessage().isBlank() ? DEFAULT_FAILURE_MESSAGE : ex.getMessage()
            );
            result = Map.of("status", STATUS_FAILED, "message", maskedMessage);
            success = false;
            message = maskedMessage;
        }

        action.setStatus(success ? STATUS_EXECUTED : STATUS_FAILED);
        action.setExecutedAt(LocalDateTime.now());
        action.setResultJson(writeJson(result));
        if (!success) {
            action.setErrorMessage(message);
        }
        actionRepository.save(action);
        auditService.recordActionExecution(action.getId(), userId, userRole, intent, success,
                success ? null : message);

        return toExecutionResultDto(action, message, readJsonMap(action.getResultJson()));
    }

    private AgentExecutionResultDTO toExecutionResultDto(AgentActionEntity action, String message,
                                                         Map<String, Object> result) {
        AgentExecutionResultDTO dto = new AgentExecutionResultDTO();
        dto.setActionId(action.getId());
        dto.setIntent(action.getIntent());
        dto.setStatus(action.getStatus());
        dto.setMessage(message);
        dto.setResult(result);
        return dto;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize agent action payload.", ex);
        }
    }

    private Map<String, Object> readJsonMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse agent action payload.", ex);
        }
    }
}
