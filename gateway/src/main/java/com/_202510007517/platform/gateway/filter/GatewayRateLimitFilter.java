package com._202510007517.platform.gateway.filter;

import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.gateway.config.GatewayRateLimitProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class GatewayRateLimitFilter implements GlobalFilter, Ordered {

    private final RedisRateLimiter redisRateLimiter;
    private final IpUserRouteKeyResolver keyResolver;
    private final GatewayRateLimitProperties properties;
    private final ObjectMapper objectMapper;

    public GatewayRateLimitFilter(RedisRateLimiter redisRateLimiter,
                                  IpUserRouteKeyResolver keyResolver,
                                  GatewayRateLimitProperties properties,
                                  ObjectMapper objectMapper) {
        this.redisRateLimiter = redisRateLimiter;
        this.keyResolver = keyResolver;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String routeId = route != null ? route.getId() : "unmatched";
        String key = keyResolver.resolveKey(exchange);
        GatewayRateLimitProperties.RouteLimit limit = applyRouteLimit(routeId);
        return redisRateLimiter.isAllowed(routeId, key)
                .flatMap(response -> {
                    response.getHeaders().forEach((name, values) ->
                            exchange.getResponse().getHeaders().add(name, values));
                    if (response.isAllowed()) {
                        return chain.filter(exchange);
                    }
                    return writeRateLimited(exchange, limit);
                });
    }

    private Mono<Void> writeRateLimited(ServerWebExchange exchange, GatewayRateLimitProperties.RouteLimit limit) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set("Retry-After", String.valueOf(limit.getRetryAfterSeconds()));
        ResponseResult<Map<String, Object>> body = ResponseResult.<Map<String, Object>>failure("请求过于频繁", 429)
                .data(Map.of("retryAfter", limit.getRetryAfterSeconds()));
        byte[] bytes = toJsonBytes(body);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private byte[] toJsonBytes(ResponseResult<Map<String, Object>> body) {
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException ex) {
            return "{\"success\":false,\"code\":429,\"message\":\"请求过于频繁\"}"
                    .getBytes(StandardCharsets.UTF_8);
        }
    }

    GatewayRateLimitProperties.RouteLimit applyRouteLimit(String routeId) {
        GatewayRateLimitProperties.RouteLimit limit = properties.resolve(routeId);
        redisRateLimiter.getConfig().put(routeId, new RedisRateLimiter.Config()
                .setReplenishRate(limit.getReplenishRate())
                .setBurstCapacity(limit.getBurstCapacity())
                .setRequestedTokens(limit.getRequestedTokens()));
        return limit;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 40;
    }
}
