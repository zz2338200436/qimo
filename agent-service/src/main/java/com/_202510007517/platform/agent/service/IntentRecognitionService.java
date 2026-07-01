package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.api.dto.AgentMessageDTO;
import com._202510007517.platform.agent.model.RecognizedIntent;

import java.util.List;

public interface IntentRecognitionService {
    RecognizedIntent recognize(String message);

    default RecognizedIntent recognize(String message, List<AgentMessageDTO> recentMessages) {
        return recognize(message);
    }
}
