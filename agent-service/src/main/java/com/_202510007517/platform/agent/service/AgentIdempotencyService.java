package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.domain.AgentActionEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AgentIdempotencyService {

    public String newActionKey() {
        return UUID.randomUUID().toString();
    }

    public void assertMatches(AgentActionEntity action, String idempotencyKey) {
        if (!action.getIdempotencyKey().equals(idempotencyKey)) {
            throw new IllegalArgumentException("Agent action idempotency key mismatch.");
        }
    }
}
