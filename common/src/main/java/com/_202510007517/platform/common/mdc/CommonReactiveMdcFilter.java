package com._202510007517.platform.common.mdc;

import com._202510007517.platform.common.web.CommonTraceConstants;
import org.slf4j.MDC;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

public class CommonReactiveMdcFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String traceId = CommonServletMdcFilter.resolveTraceId(
                exchange.getRequest().getHeaders().getFirst(CommonTraceConstants.TRACE_ID_HEADER));
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(CommonTraceConstants.TRACE_ID_HEADER, traceId)
                .build();
        ServerWebExchange mutatedExchange = exchange.mutate().request(request).build();

        mutatedExchange.getAttributes().put(CommonTraceConstants.TRACE_ID_ATTR, traceId);
        copyHeaderAttribute(mutatedExchange, CommonTraceConstants.USER_ID_HEADER, CommonTraceConstants.USER_ID_ATTR);
        copyHeaderAttribute(mutatedExchange, CommonTraceConstants.ROLES_HEADER, CommonTraceConstants.ROLES_ATTR);
        copyHeaderAttribute(mutatedExchange, CommonTraceConstants.ACTIVE_ROLE_HEADER, CommonTraceConstants.ACTIVE_ROLE_ATTR);
        mutatedExchange.getResponse().getHeaders().set(CommonTraceConstants.TRACE_ID_HEADER, traceId);

        CommonServletMdcFilter.putIfPresent(CommonTraceConstants.MDC_TRACE_ID, traceId);
        CommonServletMdcFilter.putIfPresent(CommonTraceConstants.MDC_USER_ID,
                request.getHeaders().getFirst(CommonTraceConstants.USER_ID_HEADER));
        CommonServletMdcFilter.putIfPresent(CommonTraceConstants.MDC_ROLES,
                request.getHeaders().getFirst(CommonTraceConstants.ROLES_HEADER));
        CommonServletMdcFilter.putIfPresent(CommonTraceConstants.MDC_ACTIVE_ROLE,
                request.getHeaders().getFirst(CommonTraceConstants.ACTIVE_ROLE_HEADER));
        CommonServletMdcFilter.putIfPresent(CommonTraceConstants.MDC_URI, request.getURI().getPath());
        CommonServletMdcFilter.putIfPresent(CommonTraceConstants.MDC_METHOD,
                request.getMethod() != null ? request.getMethod().name() : null);

        return chain.filter(mutatedExchange)
                .contextWrite(context -> enrichContext(context, traceId, request))
                .doFinally(signalType -> MDC.clear());
    }

    private static void copyHeaderAttribute(ServerWebExchange exchange, String header, String attr) {
        String value = exchange.getRequest().getHeaders().getFirst(header);
        if (value != null && !value.isBlank()) {
            exchange.getAttributes().put(attr, value.trim());
        }
    }

    private static Context enrichContext(Context context, String traceId, ServerHttpRequest request) {
        Context next = context.put(CommonTraceConstants.MDC_TRACE_ID, traceId);
        next = putContextValue(next, CommonTraceConstants.MDC_USER_ID,
                request.getHeaders().getFirst(CommonTraceConstants.USER_ID_HEADER));
        next = putContextValue(next, CommonTraceConstants.MDC_ROLES,
                request.getHeaders().getFirst(CommonTraceConstants.ROLES_HEADER));
        next = putContextValue(next, CommonTraceConstants.MDC_ACTIVE_ROLE,
                request.getHeaders().getFirst(CommonTraceConstants.ACTIVE_ROLE_HEADER));
        next = putContextValue(next, CommonTraceConstants.MDC_URI, request.getURI().getPath());
        next = putContextValue(next, CommonTraceConstants.MDC_METHOD,
                request.getMethod() != null ? request.getMethod().name() : null);
        return next;
    }

    private static Context putContextValue(Context context, String key, String value) {
        if (value == null || value.isBlank()) {
            return context;
        }
        return context.put(key, value.trim());
    }
}
