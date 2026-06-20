package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.api.dto.AgentActionPreviewDTO;
import com._202510007517.platform.agent.api.dto.AgentChatResponseDTO;
import com._202510007517.platform.agent.domain.AgentActionEntity;
import com._202510007517.platform.agent.domain.AgentSessionEntity;
import com._202510007517.platform.agent.model.RecognizedIntent;
import com._202510007517.platform.agent.repository.AgentActionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ActionPlanningService {
    private static final String STATUS_PENDING_CONFIRMATION = "PENDING_CONFIRMATION";

    private final AgentActionMapper actionMapper;
    private final AgentActionRepository actionRepository;
    private final AgentIdempotencyService idempotencyService;
    private final AgentDataMaskingPolicy dataMaskingPolicy;
    private final ObjectMapper objectMapper;

    public ActionPlanningService(AgentActionMapper actionMapper,
                                 AgentActionRepository actionRepository,
                                 AgentIdempotencyService idempotencyService,
                                 AgentDataMaskingPolicy dataMaskingPolicy,
                                 ObjectMapper objectMapper) {
        this.actionMapper = actionMapper;
        this.actionRepository = actionRepository;
        this.idempotencyService = idempotencyService;
        this.dataMaskingPolicy = dataMaskingPolicy;
        this.objectMapper = objectMapper;
    }

    public AgentChatResponseDTO createPreviewResponse(AgentSessionEntity session, RecognizedIntent recognizedIntent) {
        AgentActionPreviewDTO preview = actionMapper.toPreview(recognizedIntent);
        String idempotencyKey = idempotencyService.newActionKey();
        preview.setIdempotencyKey(idempotencyKey);

        AgentActionEntity action = new AgentActionEntity();
        action.setSessionId(session.getId());
        action.setIntent(recognizedIntent.intent().name());
        action.setStatus(STATUS_PENDING_CONFIRMATION);
        action.setRiskLevel(preview.getRiskLevel());
        action.setPreviewJson(writeJson(dataMaskingPolicy.maskMetadata(preview.getPreview())));
        action.setRequestJson(writeJson(Map.of(
                "intent", recognizedIntent.intent().name(),
                "slots", dataMaskingPolicy.maskMetadata(recognizedIntent.slots())
        )));
        action.setIdempotencyKey(idempotencyKey);
        action.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        AgentActionEntity savedAction = actionRepository.save(action);
        preview.setActionId(savedAction.getId());

        AgentChatResponseDTO response = new AgentChatResponseDTO();
        response.setSessionId(String.valueOf(session.getId()));
        response.setResponseType("ACTION_PREVIEW");
        response.setMessage("请确认是否执行该操作。");
        response.setActionPreview(preview);
        return response;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize agent action payload.", ex);
        }
    }
}
