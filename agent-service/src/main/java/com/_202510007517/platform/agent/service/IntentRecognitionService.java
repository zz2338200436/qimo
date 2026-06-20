package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.model.RecognizedIntent;

public interface IntentRecognitionService {
    RecognizedIntent recognize(String message);
}
