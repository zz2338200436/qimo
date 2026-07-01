package com._202510007517.platform.agent.service;

@FunctionalInterface
public interface AgentStreamingCallback {
    void onPartialResponse(String partialResponse) throws Exception;
}
