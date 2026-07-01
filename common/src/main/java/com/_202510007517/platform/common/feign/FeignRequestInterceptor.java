package com._202510007517.platform.common.feign;

import com._202510007517.platform.common.web.CommonTraceConstants;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;

public class FeignRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        copyMdcHeader(template, CommonTraceConstants.MDC_TRACE_ID, CommonTraceConstants.TRACE_ID_HEADER);
        copyMdcHeader(template, CommonTraceConstants.MDC_USER_ID, CommonTraceConstants.USER_ID_HEADER);
        copyMdcHeader(template, CommonTraceConstants.MDC_ROLES, CommonTraceConstants.ROLES_HEADER);
        copyMdcHeader(template, CommonTraceConstants.MDC_ACTIVE_ROLE, CommonTraceConstants.ACTIVE_ROLE_HEADER);
    }

    private void copyMdcHeader(RequestTemplate template, String mdcKey, String headerName) {
        String value = MDC.get(mdcKey);
        if (value != null && !value.isBlank()) {
            template.header(headerName, value.trim());
        }
    }
}
