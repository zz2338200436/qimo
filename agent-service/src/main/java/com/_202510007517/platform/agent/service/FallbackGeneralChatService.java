package com._202510007517.platform.agent.service;

public class FallbackGeneralChatService implements GeneralChatService {
    private static final String FALLBACK_MESSAGE = "暂时还不能处理这个请求。";

    @Override
    public String reply(Long userId, String userRole, String message) {
        return FALLBACK_MESSAGE;
    }
}
