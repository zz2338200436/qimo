package com._202510007517.platform.agent.internet;

import com._202510007517.platform.agent.config.AgentInternetProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestClientInternetHttpClientTest {

    @Test
    void validatesRedirectTargetBeforeFollowingIt() throws Exception {
        AtomicInteger protectedEndpointHits = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/start", exchange -> {
            exchange.getResponseHeaders().add("Location", "/internal");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/internal", exchange -> {
            protectedEndpointHits.incrementAndGet();
            byte[] body = "secret".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            InternetAccessPolicy accessPolicy = new InternetAccessPolicy(new AgentInternetProperties()) {
                @Override
                public void validate(URI uri) {
                    if (uri.getPath().contains("internal")) {
                        throw new InternetAccessDeniedException("redirect blocked");
                    }
                }
            };
            RestClientInternetHttpClient client = new RestClientInternetHttpClient(accessPolicy);

            URI startUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/start");
            assertThatThrownBy(() -> client.get(startUri, Duration.ofSeconds(2), 1024, "test-agent"))
                    .isInstanceOf(InternetAccessDeniedException.class)
                    .hasMessageContaining("redirect blocked");
            assertThat(protectedEndpointHits).hasValue(0);
        } finally {
            server.stop(0);
        }
    }
}
