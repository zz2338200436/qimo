package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.config.AgentInternetProperties;
import com._202510007517.platform.agent.internet.InternetAccessPolicy;
import com._202510007517.platform.agent.internet.InternetHttpClient;
import com._202510007517.platform.agent.internet.InternetResultStatus;
import com._202510007517.platform.agent.internet.InternetSearchService;
import com._202510007517.platform.agent.internet.WebPageReadService;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InternetToolTest {

    @Test
    @SuppressWarnings("unchecked")
    void searchToolReturnsStructuredSearchResults() {
        AgentInternetProperties properties = testProperties();
        InternetSearchService searchService = new InternetSearchService(
                properties,
                new InternetAccessPolicy(properties),
                new FakeInternetHttpClient("""
                        <a class="result__a" href="https://spring.io/projects/spring-cloud-gateway">Spring Cloud Gateway</a>
                        <a class="result__snippet">API gateway</a>
                        """));

        InternetSearchTool tool = new InternetSearchTool(searchService);
        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of("query", "Spring Cloud Gateway"));

        assertThat(result).containsEntry("status", "EXECUTED");
        assertThat(result).containsEntry("query", "Spring Cloud Gateway");
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.get("results");
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).containsEntry("url", "https://spring.io/projects/spring-cloud-gateway");
    }

    @Test
    void searchToolValidatesQueryBeforeCallingService() {
        AgentInternetProperties properties = testProperties();
        InternetSearchTool tool = new InternetSearchTool(new InternetSearchService(
                properties,
                new InternetAccessPolicy(properties),
                new FakeInternetHttpClient("should not be called")));

        Map<String, Object> result = tool.execute(7L, "STUDENT", Map.of());

        assertThat(result)
                .containsEntry("status", "VALIDATION_FAILED")
                .containsEntry("message", "请提供需要联网搜索的关键词。");
    }

    @Test
    void webPageToolReturnsCleanPageContent() {
        AgentInternetProperties properties = testProperties();
        WebPageReadService pageReadService = new WebPageReadService(
                properties,
                new InternetAccessPolicy(properties),
                new FakeInternetHttpClient("""
                        <html><head><title>Spring</title></head>
                        <body><p>Spring makes Java productive.</p></body></html>
                        """));

        WebPageReadTool tool = new WebPageReadTool(pageReadService);
        Map<String, Object> result = tool.execute(7L, "STUDENT", Map.of("url", "https://spring.io"));

        assertThat(result)
                .containsEntry("status", "EXECUTED")
                .containsEntry("url", "https://spring.io")
                .containsEntry("title", "Spring")
                .containsEntry("content", "Spring makes Java productive.");
    }

    private AgentInternetProperties testProperties() {
        AgentInternetProperties properties = new AgentInternetProperties();
        properties.setBlockPrivateNetwork(false);
        return properties;
    }

    private record FakeInternetHttpClient(String body) implements InternetHttpClient {
        @Override
        public String get(URI uri, Duration timeout, int maxBytes, String userAgent) {
            return body;
        }
    }
}
