package com._202510007517.platform.gateway.filter;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.gateway.config.GatewaySecurityProperties;
import com._202510007517.platform.gateway.security.GatewayExchangeAttributes;
import com._202510007517.platform.gateway.security.JwtAuthenticationException;
import com._202510007517.platform.gateway.security.JwtAuthenticationWriter;
import com._202510007517.platform.gateway.security.JwtTokenValidator;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";

    private final GatewaySecurityProperties securityProperties;
    private final JwtTokenValidator jwtTokenValidator;
    private final JwtAuthenticationWriter authenticationWriter;

    public JwtAuthenticationFilter(GatewaySecurityProperties securityProperties,
                                   JwtTokenValidator jwtTokenValidator,
                                   JwtAuthenticationWriter authenticationWriter) {
        this.securityProperties = securityProperties;
        this.jwtTokenValidator = jwtTokenValidator;
        this.authenticationWriter = authenticationWriter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (HttpMethod.OPTIONS.equals(request.getMethod()) || isWhitelisted(request.getPath().value())) {
            return chain.filter(exchange);
        }

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return authenticationWriter.writeUnauthorized(exchange.getResponse(), "缺少 Bearer Token");
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            return authenticationWriter.writeUnauthorized(exchange.getResponse(), "Bearer Token 不能为空");
        }

        try {
            return jwtTokenValidator.validate(token)
                    .flatMap(authContext -> {
                        exchange.getAttributes().put(GatewayExchangeAttributes.JWT_AUTH_CONTEXT, authContext);
                        putIfPresent(exchange, CommonTraceConstants.USER_ID_ATTR, authContext.userId());
                        putIfPresent(exchange, CommonTraceConstants.ROLES_ATTR, String.join(",", authContext.roles()));
                        putIfPresent(exchange, CommonTraceConstants.ACTIVE_ROLE_ATTR, authContext.activeRole());
                        return chain.filter(exchange);
                    })
                    .onErrorResume(JwtAuthenticationException.class,
                            ex -> authenticationWriter.writeUnauthorized(exchange.getResponse(), ex.getMessage()));
        } catch (JwtAuthenticationException ex) {
            return authenticationWriter.writeUnauthorized(exchange.getResponse(), ex.getMessage());
        }
    }

    private boolean isWhitelisted(String path) {
        return securityProperties.getWhitelistPaths().stream()
                .anyMatch(whitelistPath -> "/".equals(whitelistPath)
                        ? "/".equals(path)
                        : path.startsWith(whitelistPath));
    }

    private void putIfPresent(ServerWebExchange exchange, String key, String value) {
        if (value != null && !value.isBlank()) {
            exchange.getAttributes().put(key, value);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
