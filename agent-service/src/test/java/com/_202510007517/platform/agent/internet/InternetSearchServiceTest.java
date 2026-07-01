package com._202510007517.platform.agent.internet;

import com._202510007517.platform.agent.config.AgentInternetProperties;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InternetSearchServiceTest {

    @Test
    void parsesDuckDuckGoHtmlResultsIntoStructuredSources() {
        AgentInternetProperties properties = new AgentInternetProperties();
        properties.setMaxResults(2);
        properties.setBlockPrivateNetwork(false);
        properties.setSearchBaseUrl("https://duckduckgo.com/html/");
        InternetSearchService service = new InternetSearchService(
                properties,
                new InternetAccessPolicy(properties),
                new FakeInternetHttpClient("""
                        <html><body>
                        <div class="result">
                          <a rel="nofollow" class="result__a" href="/l/?uddg=https%3A%2F%2Fspring.io%2Fprojects%2Fspring-cloud-gateway">Spring Cloud Gateway</a>
                          <a class="result__snippet">API gateway built on Spring.</a>
                        </div>
                        <div class="result">
                          <a rel="nofollow" class="result__a" href="https://docs.spring.io/spring-cloud-gateway/reference/">Reference Docs</a>
                          <a class="result__snippet">Official reference documentation.</a>
                        </div>
                        </body></html>
                        """));

        InternetSearchResponse response = service.search("Spring Cloud Gateway");

        assertThat(response.status()).isEqualTo(InternetResultStatus.EXECUTED);
        assertThat(response.results()).hasSize(2);
        assertThat(response.results().get(0).title()).isEqualTo("Spring Cloud Gateway");
        assertThat(response.results().get(0).url()).isEqualTo("https://spring.io/projects/spring-cloud-gateway");
        assertThat(response.results().get(0).snippet()).contains("API gateway");
        assertThat(response.results().get(1).url()).isEqualTo("https://docs.spring.io/spring-cloud-gateway/reference/");
    }

    @Test
    void parsesBingHtmlResultsIntoStructuredSources() {
        AgentInternetProperties properties = new AgentInternetProperties();
        properties.setMaxResults(2);
        properties.setBlockPrivateNetwork(false);
        InternetSearchService service = new InternetSearchService(
                properties,
                new InternetAccessPolicy(properties),
                new FakeInternetHttpClient("""
                        <html><body>
                        <ol id="b_results">
                          <li class="b_algo">
                            <h2><a href="https://spring.io/projects/spring-cloud-gateway">Spring Cloud Gateway</a></h2>
                            <div class="b_caption"><p>Spring Cloud Gateway provides a library for building API gateways.</p></div>
                          </li>
                          <li class="b_algo">
                            <h2><a href="https://docs.spring.io/spring-cloud-gateway/reference/">Reference Docs</a></h2>
                            <div class="b_caption"><p>Official reference documentation.</p></div>
                          </li>
                        </ol>
                        </body></html>
                        """));

        InternetSearchResponse response = service.search("Spring Cloud Gateway");

        assertThat(response.status()).isEqualTo(InternetResultStatus.EXECUTED);
        assertThat(response.results()).hasSize(2);
        assertThat(response.results().get(0).title()).isEqualTo("Spring Cloud Gateway");
        assertThat(response.results().get(0).url()).isEqualTo("https://spring.io/projects/spring-cloud-gateway");
        assertThat(response.results().get(0).snippet()).contains("API gateways");
        assertThat(response.results().get(1).url()).isEqualTo("https://docs.spring.io/spring-cloud-gateway/reference/");
    }

    @Test
    void parsesBingTitleLinkInsteadOfBreadcrumbLinks() {
        AgentInternetProperties properties = new AgentInternetProperties();
        properties.setMaxResults(1);
        properties.setBlockPrivateNetwork(false);
        InternetSearchService service = new InternetSearchService(
                properties,
                new InternetAccessPolicy(properties),
                new FakeInternetHttpClient("""
                        <html><body>
                        <ol id="b_results">
                          <li class="b_algo">
                            <div class="tptt">
                              <a href="https://spring.io/">spring.io https:// spring.io › projects › spring-cloud-gateway</a>
                            </div>
                            <h2><a href="https://spring.io/projects/spring-cloud-gateway">Spring Cloud Gateway</a></h2>
                            <div class="b_caption">
                              <p>This project provides a library for building an API Gateway on top of Spring.</p>
                            </div>
                          </li>
                        </ol>
                        </body></html>
                        """));

        InternetSearchResponse response = service.search("Spring Cloud Gateway");

        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).title()).isEqualTo("Spring Cloud Gateway");
        assertThat(response.results().get(0).url()).isEqualTo("https://spring.io/projects/spring-cloud-gateway");
        assertThat(response.results().get(0).snippet()).contains("API Gateway");
    }

    @Test
    void prioritizesOfficialLookingSourcesForOfficialDocumentQueries() {
        AgentInternetProperties properties = new AgentInternetProperties();
        properties.setMaxResults(3);
        properties.setBlockPrivateNetwork(false);
        InternetSearchService service = new InternetSearchService(
                properties,
                new InternetAccessPolicy(properties),
                new FakeInternetHttpClient("""
                        <html><body>
                        <ol id="b_results">
                          <li class="b_algo">
                            <h2><a href="https://blog.csdn.net/demo/article/details/1">Spring Cloud Gateway 实战</a></h2>
                            <p>一篇实践文章。</p>
                          </li>
                          <li class="b_algo">
                            <h2><a href="https://spring.io/projects/spring-cloud-gateway">Spring Cloud Gateway</a></h2>
                            <p>Official project page for Spring Cloud Gateway.</p>
                          </li>
                          <li class="b_algo">
                            <h2><a href="https://docs.spring.io/spring-cloud-gateway/reference/">Spring Cloud Gateway Reference Documentation</a></h2>
                            <p>Official reference documentation.</p>
                          </li>
                        </ol>
                        </body></html>
                        """));

        InternetSearchResponse response = service.search("Spring Cloud Gateway 官方资料");

        assertThat(response.results()).extracting(InternetSearchResult::url)
                .containsExactly(
                        "https://docs.spring.io/spring-cloud-gateway/reference/",
                        "https://spring.io/projects/spring-cloud-gateway",
                        "https://blog.csdn.net/demo/article/details/1"
                );
    }

    @Test
    void expandsOfficialSpringQueriesToSiteSearchBeforeFallbackQuery() {
        AgentInternetProperties properties = new AgentInternetProperties();
        properties.setMaxResults(3);
        properties.setBlockPrivateNetwork(false);
        RecordingInternetHttpClient httpClient = new RecordingInternetHttpClient(Map.of(
                "site:spring.io Spring Cloud Gateway", """
                        <html><body>
                        <ol id="b_results">
                          <li class="b_algo">
                            <h2><a href="https://spring.io/projects/spring-cloud-gateway/">Spring Cloud Gateway</a></h2>
                            <p>Official project page for Spring Cloud Gateway.</p>
                          </li>
                          <li class="b_algo">
                            <h2><a href="https://spring.io/projects/spring-cloud/">Spring Cloud</a></h2>
                            <p>Spring Cloud provides tools for common distributed system patterns.</p>
                          </li>
                        </ol>
                        </body></html>
                        """,
                "Spring Cloud Gateway official documentation", """
                        <html><body>
                        <ol id="b_results">
                          <li class="b_algo">
                            <h2><a href="https://blog.csdn.net/demo/article/details/1">Spring Cloud Gateway 实战</a></h2>
                            <p>一篇实践文章。</p>
                          </li>
                        </ol>
                        </body></html>
                        """,
                "Spring Cloud Gateway 官方资料", """
                        <html><body>
                        <ol id="b_results">
                          <li class="b_algo">
                            <h2><a href="https://spring.io/">Spring | Home</a></h2>
                            <p>Spring home page.</p>
                          </li>
                        </ol>
                        </body></html>
                        """
        ));
        InternetSearchService service = new InternetSearchService(
                properties,
                new InternetAccessPolicy(properties),
                httpClient);

        InternetSearchResponse response = service.search("Spring Cloud Gateway 官方资料");

        assertThat(httpClient.queries()).contains("site:spring.io Spring Cloud Gateway");
        assertThat(response.results()).extracting(InternetSearchResult::url)
                .containsExactly(
                        "https://spring.io/projects/spring-cloud-gateway/",
                        "https://spring.io/projects/spring-cloud/",
                        "https://blog.csdn.net/demo/article/details/1"
                );
    }

    @Test
    void returnsDisabledStatusWhenInternetAccessIsOff() {
        AgentInternetProperties properties = new AgentInternetProperties();
        properties.setEnabled(false);
        InternetSearchService service = new InternetSearchService(
                properties,
                new InternetAccessPolicy(properties),
                new FakeInternetHttpClient("should not be called"));

        InternetSearchResponse response = service.search("Spring Cloud Gateway");

        assertThat(response.status()).isEqualTo(InternetResultStatus.DISABLED);
        assertThat(response.results()).isEmpty();
    }

    private record FakeInternetHttpClient(String body) implements InternetHttpClient {
        @Override
        public String get(URI uri, Duration timeout, int maxBytes, String userAgent) {
            assertThat(queryFrom(uri)).contains("Spring Cloud Gateway");
            return body;
        }

        private String queryFrom(URI uri) {
            String rawQuery = uri.getRawQuery();
            assertThat(rawQuery).isNotBlank();
            for (String part : rawQuery.split("&")) {
                int separator = part.indexOf('=');
                if (separator < 0) {
                    continue;
                }
                String key = URLDecoder.decode(part.substring(0, separator), StandardCharsets.UTF_8);
                if ("q".equals(key)) {
                    return URLDecoder.decode(part.substring(separator + 1), StandardCharsets.UTF_8);
                }
            }
            return "";
        }
    }

    private static final class RecordingInternetHttpClient implements InternetHttpClient {
        private final Map<String, String> bodiesByQuery;
        private final List<String> queries = new ArrayList<>();

        private RecordingInternetHttpClient(Map<String, String> bodiesByQuery) {
            this.bodiesByQuery = new LinkedHashMap<>(bodiesByQuery);
        }

        @Override
        public String get(URI uri, Duration timeout, int maxBytes, String userAgent) {
            String query = queryFrom(uri);
            queries.add(query);
            return bodiesByQuery.getOrDefault(query, "<html><body></body></html>");
        }

        private List<String> queries() {
            return queries;
        }

        private String queryFrom(URI uri) {
            String rawQuery = uri.getRawQuery();
            assertThat(rawQuery).isNotBlank();
            for (String part : rawQuery.split("&")) {
                int separator = part.indexOf('=');
                if (separator < 0) {
                    continue;
                }
                String key = URLDecoder.decode(part.substring(0, separator), StandardCharsets.UTF_8);
                if ("q".equals(key)) {
                    return URLDecoder.decode(part.substring(separator + 1), StandardCharsets.UTF_8);
                }
            }
            return "";
        }
    }
}
