package com._202510007517.platform.agent.internet;

public record WebPageReadResponse(
        InternetResultStatus status,
        String url,
        String title,
        String content,
        String message
) {
}
