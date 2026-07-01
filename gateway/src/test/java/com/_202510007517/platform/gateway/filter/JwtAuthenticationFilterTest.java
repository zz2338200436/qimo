package com._202510007517.platform.gateway.filter;

import com._202510007517.platform.gateway.config.GatewaySecurityProperties;
import com._202510007517.platform.gateway.security.JwtAuthenticationException;
import com._202510007517.platform.gateway.security.JwtAuthenticationWriter;
import com._202510007517.platform.gateway.security.JwtTokenValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @Test
    void returnsUnauthorizedWhenValidatorThrowsSynchronously() {
        GatewaySecurityProperties properties = new GatewaySecurityProperties();
        JwtTokenValidator validator = mock(JwtTokenValidator.class);
        when(validator.validate("bad-token"))
                .thenThrow(new JwtAuthenticationException("JWT kid 未受信任"));

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                properties,
                validator,
                new JwtAuthenticationWriter(new ObjectMapper())
        );

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer bad-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        GatewayFilterChain chain = serverWebExchange -> {
            chainInvoked.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(chainInvoked).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("JWT kid 未受信任")
                .contains("\"code\":401");
    }
}
