package com._202510007517.platform.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class IpUserRouteKeyResolverTest {

    @Test
    void keyContainsClientIpUserIdAndAiRouteId() {
        IpUserRouteKeyResolver resolver = new IpUserRouteKeyResolver();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/ai/generate-questions")
                        .remoteAddress(new InetSocketAddress("127.0.0.1", 12345))
                        .header("X-User-Id", "7")
                        .build());
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR,
                Route.async()
                        .id("ai-route")
                        .uri(URI.create("lb://ai-service"))
                        .predicate(serverWebExchange -> true)
                        .build());

        String key = resolver.resolveKey(exchange);

        assertThat(key).isEqualTo("127.0.0.1:7:ai-route");
    }
}
