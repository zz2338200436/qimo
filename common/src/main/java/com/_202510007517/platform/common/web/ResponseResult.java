package com._202510007517.platform.common.web;

import org.slf4j.MDC;

/**
 * 统一响应体。
 */
public class ResponseResult<T> {

    private boolean success;
    private T data;
    private String message;
    private int code;
    private String traceId;
    private long timestamp;

    private ResponseResult() {
        this.timestamp = System.currentTimeMillis();
        this.traceId = MDC.get(CommonTraceConstants.MDC_TRACE_ID);
    }

    public static <T> ResponseResult<T> success(T data, String message, int code) {
        ResponseResult<T> result = new ResponseResult<>();
        result.success = true;
        result.data = data;
        result.message = message;
        result.code = code;
        return result;
    }

    public static <T> ResponseResult<T> success(T data) {
        return success(data, "操作成功", 200);
    }

    public static ResponseResult<Void> success() {
        return success(null, "操作成功", 200);
    }

    public static <T> ResponseResult<T> created(T data) {
        return success(data, "创建成功", 201);
    }

    public static ResponseResult<Void> noContent() {
        return success(null, "操作成功", 204);
    }

    public static <T> ResponseResult<T> failure(String message, int code) {
        ResponseResult<T> result = new ResponseResult<>();
        result.success = false;
        result.message = message;
        result.code = code;
        return result;
    }

    public static <T> ResponseResult<T> failure(String message) {
        return failure(message, 400);
    }

    public static <T> ResponseResult<T> failure(int code) {
        return failure("操作失败", code);
    }

    public ResponseResult<T> data(T data) {
        this.data = data;
        return this;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
