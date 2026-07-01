package com._202510007517.platform.gateway.filter;

import com._202510007517.platform.common.web.CommonTraceConstants;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Component("ipUserRouteKeyResolver")
public class IpUserRouteKeyResolver implements KeyResolver {

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        return Mono.just(resolveKey(exchange));
    }

    public String resolveKey(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String ip = resolveIp(request);
        String userId = firstPresent(
                exchange.getAttribute(CommonTraceConstants.USER_ID_ATTR),
                request.getHeaders().getFirst(CommonTraceConstants.USER_ID_HEADER),
                "anonymous"
        );
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String routeId = route != null ? route.getId() : "unmatched";
        return ip + ":" + userId + ":" + routeId;
    }

    private String resolveIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        return remoteAddress != null && remoteAddress.getAddress() != null
                ? remoteAddress.getAddress().getHostAddress()
                : "unknown";
    }

    @SafeVarargs
    private final <T> String firstPresent(T... values) {
        for (T value : values) {
            if (value != null && !value.toString().isBlank()) {
                return value.toString().trim();
            }
        }
        return "unknown";
    }
}
