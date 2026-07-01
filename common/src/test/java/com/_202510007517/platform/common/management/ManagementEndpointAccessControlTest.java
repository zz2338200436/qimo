package com._202510007517.platform.common.management;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManagementEndpointAccessControlTest {

    private final ManagementEndpointAccessProperties properties = new ManagementEndpointAccessProperties();
    private final ManagementEndpointAccessMatcher matcher = new ManagementEndpointAccessMatcher(properties);

    @Test
    void servlet_filter_allows_private_network_access_to_actuator() throws Exception {
        CommonServletManagementAccessFilter filter = new CommonServletManagementAccessFilter(matcher);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/prometheus");
        request.setRemoteAddr("192.168.65.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }

    @Test
    void servlet_filter_blocks_public_network_access_to_actuator() throws Exception {
        CommonServletManagementAccessFilter filter = new CommonServletManagementAccessFilter(matcher);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/prometheus");
        request.setRemoteAddr("8.8.8.8");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(403, response.getStatus());
    }

    @Test
    void matcher_only_protects_actuator_paths() throws IOException {
        assertEquals(true, matcher.isProtectedPath("/actuator/health"));
        assertEquals(true, matcher.isProtectedPath("/actuator/prometheus"));
        assertEquals(false, matcher.isProtectedPath("/api/users/1"));
        assertEquals(true, matcher.isAllowed("127.0.0.1"));
        assertEquals(true, matcher.isAllowed("10.10.10.10"));
        assertEquals(false, matcher.isAllowed("8.8.8.8"));
    }

    @Test
    void reactive_filter_blocks_public_network_access_to_actuator() {
        CommonReactiveManagementAccessFilter filter = new CommonReactiveManagementAccessFilter(matcher);
        MockServerHttpRequest request = MockServerHttpRequest.get("/actuator/prometheus")
                .remoteAddress(new java.net.InetSocketAddress("8.8.8.8", 12345))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        WebFilterChain chain = value -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertEquals(403, exchange.getResponse().getStatusCode().value());
        assertEquals(false, chainCalled.get());
    }
}
