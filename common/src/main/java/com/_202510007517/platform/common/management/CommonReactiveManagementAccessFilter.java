package com._202510007517.platform.common.management;

import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

public class CommonReactiveManagementAccessFilter implements WebFilter, Ordered {

    private final ManagementEndpointAccessMatcher matcher;

    public CommonReactiveManagementAccessFilter(ManagementEndpointAccessMatcher matcher) {
        this.matcher = matcher;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        if (!matcher.isProtectedPath(path)) {
            return chain.filter(exchange);
        }

        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        String candidate = remoteAddress != null && remoteAddress.getAddress() != null
                ? remoteAddress.getAddress().getHostAddress()
                : null;

        if (matcher.isAllowed(candidate)) {
            return chain.filter(exchange);
        }

        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
