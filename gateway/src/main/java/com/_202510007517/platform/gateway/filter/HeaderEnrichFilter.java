package com._202510007517.platform.gateway.filter;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.gateway.security.GatewayExchangeAttributes;
import com._202510007517.platform.gateway.security.GatewayJwtAuthContext;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class HeaderEnrichFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        GatewayJwtAuthContext authContext =
                exchange.getAttribute(GatewayExchangeAttributes.JWT_AUTH_CONTEXT);

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(CommonTraceConstants.USER_ID_HEADER);
                    headers.remove(CommonTraceConstants.ROLES_HEADER);
                    headers.remove(CommonTraceConstants.ACTIVE_ROLE_HEADER);
                    if (authContext != null) {
                        headers.set(CommonTraceConstants.USER_ID_HEADER, authContext.userId());
                        headers.set(CommonTraceConstants.ROLES_HEADER, String.join(",", authContext.roles()));
                        headers.set(CommonTraceConstants.ACTIVE_ROLE_HEADER, authContext.activeRole());
                    }
                })
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 30;
    }
}
