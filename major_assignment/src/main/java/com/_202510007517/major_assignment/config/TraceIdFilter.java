package com._202510007517.major_assignment.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * 链路追踪过滤器（R3.4 / R13.2 / Design §7.2 MDC 字段）。
 *
 * <p>职责（与 {@link MultiRoleSessionFilter} 协作）：</p>
 * <ol>
 *   <li>若请求头 {@code X-Trace-Id} 存在且非空白，透传；否则生成去连字符的 {@link UUID}。</li>
 *   <li>将 {@code traceId} 写入 {@link MDC}，并顺带写入 {@code uri} / {@code method} 的最小基线值，
 *       以便在 {@link MultiRoleSessionFilter} 尚未执行或跳过匿名路径时，日志仍能带出基础字段。</li>
 *   <li>在请求属性 {@value #TRACE_ID_ATTR} 中同步保存 {@code traceId}，供下游 Filter / Controller / Aspect 使用，
 *       避免重复解析 Header。</li>
 *   <li>将 {@code X-Trace-Id} 回写到响应头，方便客户端与日志进行链路关联（R13.2）。</li>
 *   <li>{@code finally} 块中 {@link MDC#clear()}：作为 Filter 链最外层的 MDC 兜底清理点，防止线程池复用污染
 *       （Design §7.2 "Filter 结束前在 finally 块执行 MDC.clear()"）。</li>
 * </ol>
 *
 * <p><b>链路顺序</b>：{@code TraceIdFilter}（本类，{@link Ordered#HIGHEST_PRECEDENCE}）→
 * {@link MultiRoleSessionFilter}（{@code @Order(1)}）→ Spring Security → Controller。</p>
 *
 * <p><b>MDC 写入者</b>：本过滤器与 {@link MultiRoleSessionFilter} 是 MDC 的<b>唯二</b>写入者；切面与
 * {@code GlobalExceptionHandler} 只读，不得在业务链路中覆写 {@code traceId}（Design §7.2）。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter implements Filter {

    /** HTTP 请求头 / 响应头名称。 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /** 请求属性键，供下游组件复用而无需重复解析 Header。 */
    public static final String TRACE_ID_ATTR = "TRACE_ID";

    // MDC 字段名（对齐 Design §7.2 日志 JSON 字段）
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_URI = "uri";
    private static final String MDC_METHOD = "method";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String traceId = resolveTraceId(httpRequest.getHeader(TRACE_ID_HEADER));

        try {
            // 1) 请求属性：下游 Filter / Controller 可复用
            httpRequest.setAttribute(TRACE_ID_ATTR, traceId);

            // 2) MDC：日志 / 切面消费
            MDC.put(MDC_TRACE_ID, traceId);
            if (httpRequest.getRequestURI() != null) {
                MDC.put(MDC_URI, httpRequest.getRequestURI());
            }
            if (httpRequest.getMethod() != null) {
                MDC.put(MDC_METHOD, httpRequest.getMethod());
            }

            // 3) 响应头回写：方便客户端与运维按 traceId 检索日志
            httpResponse.setHeader(TRACE_ID_HEADER, traceId);

            chain.doFilter(request, response);
        } finally {
            // Filter 链最外层的兜底清理，避免线程池复用污染
            MDC.clear();
        }
    }

    /**
     * 解析入站 traceId：存在且非空白则复用；否则生成去连字符的 UUID。
     */
    private String resolveTraceId(String incoming) {
        if (incoming != null && !incoming.isBlank()) {
            return incoming.trim();
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
