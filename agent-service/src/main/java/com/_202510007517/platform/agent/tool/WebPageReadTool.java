package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.internet.WebPageReadResponse;
import com._202510007517.platform.agent.internet.WebPageReadService;
import com._202510007517.platform.agent.model.AgentIntent;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class WebPageReadTool implements AgentTool {
    private final WebPageReadService pageReadService;

    public WebPageReadTool(WebPageReadService pageReadService) {
        this.pageReadService = pageReadService;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.READ_WEB_PAGE;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        String url = readText(request, "url", "uri", "link");
        if (url == null) {
            return Map.of(
                    "status", "VALIDATION_FAILED",
                    "message", "请提供需要读取的网页地址。"
            );
        }
        WebPageReadResponse response = pageReadService.read(url);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", response.status().name());
        result.put("url", response.url());
        result.put("title", response.title());
        result.put("content", response.content());
        result.put("message", response.message());
        return result;
    }

    private String readText(Map<String, Object> request, String... keys) {
        if (request == null) {
            return null;
        }
        for (String key : keys) {
            Object value = request.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value).trim();
            }
        }
        return null;
    }
}
