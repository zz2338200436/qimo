package com._202510007517.platform.common.web;

/**
 * 公共链路追踪与身份透传常量。
 */
public final class CommonTraceConstants {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String ROLES_HEADER = "X-Roles";
    public static final String ACTIVE_ROLE_HEADER = "X-Active-Role";

    public static final String TRACE_ID_ATTR = "TRACE_ID";
    public static final String USER_ID_ATTR = "CURRENT_USER_ID";
    public static final String ROLES_ATTR = "CURRENT_ROLES";
    public static final String ACTIVE_ROLE_ATTR = "CURRENT_ACTIVE_ROLE";

    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_USER_ID = "userId";
    public static final String MDC_ROLES = "roles";
    public static final String MDC_ACTIVE_ROLE = "activeRole";
    public static final String MDC_URI = "uri";
    public static final String MDC_METHOD = "method";
    public static final String MDC_EXCEPTION_TYPE = "exceptionType";

    private CommonTraceConstants() {
    }
}
