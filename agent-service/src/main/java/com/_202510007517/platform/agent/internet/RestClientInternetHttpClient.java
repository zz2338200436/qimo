package com._202510007517.platform.agent.internet;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Duration;

public class RestClientInternetHttpClient implements InternetHttpClient {
    private static final int MAX_REDIRECTS = 5;

    private final InternetAccessPolicy accessPolicy;

    public RestClientInternetHttpClient(InternetAccessPolicy accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    @Override
    public String get(URI uri, Duration timeout, int maxBytes, String userAgent) {
        URI currentUri = uri;
        for (int redirectCount = 0; redirectCount <= MAX_REDIRECTS; redirectCount++) {
            HttpResult result = fetchOnce(currentUri, timeout, userAgent);
            if (!result.isRedirect()) {
                return truncate(result.body(), maxBytes);
            }
            currentUri = resolveRedirect(currentUri, result.location());
            accessPolicy.validate(currentUri);
        }
        throw new InternetAccessDeniedException("联网请求重定向次数过多。");
    }

    private HttpResult fetchOnce(URI uri, Duration timeout, String userAgent) {
        SimpleClientHttpRequestFactory requestFactory = new NoRedirectClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build()
                .get()
                .uri(uri)
                .header(HttpHeaders.USER_AGENT, userAgent)
                .exchange((request, response) -> new HttpResult(
                        response.getStatusCode(),
                        response.getHeaders().getFirst(HttpHeaders.LOCATION),
                        response.getBody().readAllBytes()));
    }

    private URI resolveRedirect(URI currentUri, String location) {
        if (location == null || location.isBlank()) {
            throw new InternetAccessDeniedException("联网请求收到重定向响应，但缺少 Location。");
        }
        return currentUri.resolve(location.trim());
    }

    private String truncate(String body, int maxBytes) {
        if (body == null) {
            return "";
        }
        if (body.length() <= maxBytes) {
            return body;
        }
        return body.substring(0, maxBytes);
    }

    private record HttpResult(HttpStatusCode statusCode, String location, byte[] bodyBytes) {
        boolean isRedirect() {
            return statusCode.is3xxRedirection();
        }

        String body() {
            return bodyBytes == null ? "" : new String(bodyBytes);
        }
    }

    private static class NoRedirectClientHttpRequestFactory extends SimpleClientHttpRequestFactory {
        @Override
        protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
            super.prepareConnection(connection, httpMethod);
            if (HttpMethod.GET.matches(httpMethod)) {
                connection.setInstanceFollowRedirects(false);
            }
        }
    }
}
