package com._202510007517.platform.gateway.web;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/errors")
public class BrowserErrorEdgeController {

    private final BrowserErrorEdgeStore store;

    public BrowserErrorEdgeController(BrowserErrorEdgeStore store) {
        this.store = store;
    }

    @PostMapping("/browser")
    public ResponseResult<Long> reportBrowserError(
            @RequestBody BrowserErrorEdgeStore.BrowserErrorPayload payload,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            HttpServletRequest request) {
        enrichPayload(payload, userIdHeader, request);
        return ResponseResult.success(store.save(payload), "错误日志上报成功", 200);
    }

    @PostMapping("/browser/batch")
    public ResponseResult<Integer> batchReportBrowserErrors(
            @RequestBody List<BrowserErrorEdgeStore.BrowserErrorPayload> payloads,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            HttpServletRequest request) {
        payloads.forEach(payload -> enrichPayload(payload, userIdHeader, request));
        return ResponseResult.success(store.batchSave(payloads), "错误日志批量上报成功", 200);
    }

    @GetMapping("/browser")
    public ResponseResult<Map<String, Object>> getBrowserErrorList(
            @RequestParam Map<String, String> params,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<BrowserErrorEdgeStore.BrowserErrorPayload> rows = store.list(params, page, size);
        int total = store.count(params);
        return ResponseResult.success(Map.of(
                "content", rows,
                "totalElements", total,
                "pageNumber", page,
                "pageSize", size,
                "totalPages", (total + size - 1) / size
        ), "获取错误日志列表成功", 200);
    }

    @GetMapping("/browser/{id}")
    public ResponseResult<BrowserErrorEdgeStore.BrowserErrorPayload> getBrowserErrorById(@PathVariable Long id) {
        BrowserErrorEdgeStore.BrowserErrorPayload payload = store.findById(id);
        if (payload == null) {
            return ResponseResult.failure("错误日志不存在");
        }
        return ResponseResult.success(payload, "获取错误日志详情成功", 200);
    }

    private static void enrichPayload(BrowserErrorEdgeStore.BrowserErrorPayload payload,
                                      String userIdHeader,
                                      HttpServletRequest request) {
        payload.setClientIp(resolveClientIp(request));
        payload.setSessionId(request.getSession(true).getId());
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            payload.setUserId(Long.valueOf(userIdHeader));
        }
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
