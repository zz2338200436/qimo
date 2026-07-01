package com._202510007517.platform.gateway.controller;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class BrowserErrorEdgeStore {

    private final AtomicLong idSequence = new AtomicLong(1L);
    private final ConcurrentHashMap<Long, BrowserErrorPayload> entries = new ConcurrentHashMap<>();

    public long save(BrowserErrorPayload payload) {
        long id = idSequence.getAndIncrement();
        payload.setId(id);
        payload.setStatus(payload.getStatus() == null ? 0 : payload.getStatus());
        payload.setErrorTime(payload.getErrorTime() == null ? Instant.now().toString() : payload.getErrorTime());
        payload.setCreatedTime(Instant.now().toString());
        payload.setUpdatedTime(payload.getCreatedTime());
        entries.put(id, payload);
        return id;
    }

    public int batchSave(List<BrowserErrorPayload> payloads) {
        for (BrowserErrorPayload payload : payloads) {
            save(payload);
        }
        return payloads.size();
    }

    public BrowserErrorPayload findById(Long id) {
        return entries.get(id);
    }

    public List<BrowserErrorPayload> list(Map<String, String> params, int page, int size) {
        return filtered(params).stream()
                .sorted(Comparator.comparing(BrowserErrorPayload::getId).reversed())
                .skip((long) Math.max(page - 1, 0) * size)
                .limit(size)
                .toList();
    }

    public int count(Map<String, String> params) {
        return filtered(params).size();
    }

    private List<BrowserErrorPayload> filtered(Map<String, String> params) {
        List<BrowserErrorPayload> rows = new ArrayList<>(entries.values());
        if (params == null || params.isEmpty()) {
            return rows;
        }
        return rows.stream()
                .filter(row -> matches(row.getErrorType(), params.get("errorType")))
                .filter(row -> matches(row.getPageUrl(), params.get("pageUrl")))
                .filter(row -> matches(row.getClientIp(), params.get("clientIp")))
                .filter(row -> matchesStatus(row.getStatus(), params.get("status")))
                .toList();
    }

    private static boolean matches(String actual, String expected) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        return actual != null && actual.contains(expected);
    }

    private static boolean matchesStatus(Integer actual, String expected) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        return Objects.equals(actual, Integer.valueOf(expected));
    }

    public static class BrowserErrorPayload {
        private Long id;
        private String errorType;
        private String errorMessage;
        private String errorStack;
        private String pageUrl;
        private Integer lineNumber;
        private Integer columnNumber;
        private String fileUrl;
        private String userAgent;
        private String clientIp;
        private String sessionId;
        private Long userId;
        private String errorTime;
        private Integer status;
        private String createdTime;
        private String updatedTime;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getErrorType() {
            return errorType;
        }

        public void setErrorType(String errorType) {
            this.errorType = errorType;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public String getErrorStack() {
            return errorStack;
        }

        public void setErrorStack(String errorStack) {
            this.errorStack = errorStack;
        }

        public String getPageUrl() {
            return pageUrl;
        }

        public void setPageUrl(String pageUrl) {
            this.pageUrl = pageUrl;
        }

        public Integer getLineNumber() {
            return lineNumber;
        }

        public void setLineNumber(Integer lineNumber) {
            this.lineNumber = lineNumber;
        }

        public Integer getColumnNumber() {
            return columnNumber;
        }

        public void setColumnNumber(Integer columnNumber) {
            this.columnNumber = columnNumber;
        }

        public String getFileUrl() {
            return fileUrl;
        }

        public void setFileUrl(String fileUrl) {
            this.fileUrl = fileUrl;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }

        public String getClientIp() {
            return clientIp;
        }

        public void setClientIp(String clientIp) {
            this.clientIp = clientIp;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getErrorTime() {
            return errorTime;
        }

        public void setErrorTime(String errorTime) {
            this.errorTime = errorTime;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public String getCreatedTime() {
            return createdTime;
        }

        public void setCreatedTime(String createdTime) {
            this.createdTime = createdTime;
        }

        public String getUpdatedTime() {
            return updatedTime;
        }

        public void setUpdatedTime(String updatedTime) {
            this.updatedTime = updatedTime;
        }
    }
}
