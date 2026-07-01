package com._202510007517.platform.agent.internet;

import java.util.List;

public record InternetSearchResponse(
        InternetResultStatus status,
        String query,
        List<InternetSearchResult> results,
        String message
) {
}
