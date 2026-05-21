package com._202510007517.platform.common.mdc;

import com._202510007517.platform.common.web.CommonTraceConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class CommonServletMdcFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = resolveTraceId(request.getHeader(CommonTraceConstants.TRACE_ID_HEADER));
        try {
            request.setAttribute(CommonTraceConstants.TRACE_ID_ATTR, traceId);
            response.setHeader(CommonTraceConstants.TRACE_ID_HEADER, traceId);
            putBaseMdc(traceId, request.getHeader(CommonTraceConstants.USER_ID_HEADER),
                    request.getHeader(CommonTraceConstants.ROLES_HEADER),
                    request.getHeader(CommonTraceConstants.ACTIVE_ROLE_HEADER),
                    request.getRequestURI(), request.getMethod());
            request.setAttribute(CommonTraceConstants.USER_ID_ATTR, headerValue(request.getHeader(CommonTraceConstants.USER_ID_HEADER)));
            request.setAttribute(CommonTraceConstants.ROLES_ATTR, headerValue(request.getHeader(CommonTraceConstants.ROLES_HEADER)));
            request.setAttribute(CommonTraceConstants.ACTIVE_ROLE_ATTR, headerValue(request.getHeader(CommonTraceConstants.ACTIVE_ROLE_HEADER)));
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private static void putBaseMdc(String traceId, String userId, String roles, String activeRole, String uri, String method) {
        MDC.put(CommonTraceConstants.MDC_TRACE_ID, traceId);
        putIfPresent(CommonTraceConstants.MDC_USER_ID, userId);
        putIfPresent(CommonTraceConstants.MDC_ROLES, roles);
        putIfPresent(CommonTraceConstants.MDC_ACTIVE_ROLE, activeRole);
        putIfPresent(CommonTraceConstants.MDC_URI, uri);
        putIfPresent(CommonTraceConstants.MDC_METHOD, method);
    }

    static void putIfPresent(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value.trim());
        }
    }

    static String resolveTraceId(String incoming) {
        if (incoming != null && !incoming.isBlank()) {
            return incoming.trim();
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String headerValue(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
