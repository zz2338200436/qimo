package com._202510007517.platform.agent.internet;

import com._202510007517.platform.agent.config.AgentInternetProperties;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class WebPageReadServiceTest {

    @Test
    void readsAndCleansHtmlPageContent() {
        AgentInternetProperties properties = new AgentInternetProperties();
        properties.setBlockPrivateNetwork(false);
        properties.setMaxContentChars(120);
        WebPageReadService service = new WebPageReadService(
                properties,
                new InternetAccessPolicy(properties),
                new FakeInternetHttpClient("""
                        <html>
                          <head><title>Spring Cloud Gateway</title><script>alert('x')</script></head>
                          <body>
                            <h1>Spring Cloud Gateway</h1>
                            <p>Provides a simple yet effective way to route to APIs.</p>
                            <style>.hidden{display:none}</style>
                          </body>
                        </html>
                        """));

        WebPageReadResponse response = service.read("https://spring.io/projects/spring-cloud-gateway");

        assertThat(response.status()).isEqualTo(InternetResultStatus.EXECUTED);
        assertThat(response.title()).isEqualTo("Spring Cloud Gateway");
        assertThat(response.content())
                .contains("Spring Cloud Gateway")
                .contains("route to APIs")
                .doesNotContain("alert")
                .doesNotContain("hidden");
        assertThat(response.content().length()).isLessThanOrEqualTo(120);
    }

    private record FakeInternetHttpClient(String body) implements InternetHttpClient {
        @Override
        public String get(URI uri, Duration timeout, int maxBytes, String userAgent) {
            assertThat(uri.toString()).isEqualTo("https://spring.io/projects/spring-cloud-gateway");
            return body;
        }
    }
}
