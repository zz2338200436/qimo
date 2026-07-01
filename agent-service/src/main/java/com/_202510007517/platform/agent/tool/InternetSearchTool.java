package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.internet.InternetSearchResponse;
import com._202510007517.platform.agent.internet.InternetSearchResult;
import com._202510007517.platform.agent.internet.InternetSearchService;
import com._202510007517.platform.agent.model.AgentIntent;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class InternetSearchTool implements AgentTool {
    private final InternetSearchService searchService;

    public InternetSearchTool(InternetSearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.INTERNET_SEARCH;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        String query = readText(request, "query", "keyword", "topic");
        if (query == null) {
            return Map.of(
                    "status", "VALIDATION_FAILED",
                    "message", "请提供需要联网搜索的关键词。"
            );
        }
        InternetSearchResponse response = searchService.search(query);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", response.status().name());
        result.put("query", response.query());
        result.put("results", response.results().stream().map(this::toMap).toList());
        result.put("message", response.message());
        return result;
    }

    private Map<String, Object> toMap(InternetSearchResult result) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", result.title());
        map.put("url", result.url());
        map.put("snippet", result.snippet());
        return map;
    }

    private String readText(Map<String, Object> request, String... keys) {
        if (request == null) {
            return null;
        }
        for (String key : keys) {
            Object value = request.get(key);
            if (value instanceof List<?> values && !values.isEmpty()) {
                value = values.get(0);
            }
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value).trim();
            }
        }
        return null;
    }
}
