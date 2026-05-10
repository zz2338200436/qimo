package com._202510007517.major_assignment.entity.dto;

import org.slf4j.MDC;

/**
 * 统一响应结果类
 * 用于规范前后端数据交互的响应格式。
 * <p>
 * 契约（对齐 Design §Components §7.2 / R3.1）：
 * <ul>
 *   <li>{@code success} 与 {@code code} 满足不变量 {@code success == (200 <= code < 300)}。</li>
 *   <li>{@code message} 非空。</li>
 *   <li>{@code data} 可为空。</li>
 *   <li>{@code traceId} 于构造时从 {@link MDC#get(String)} 读取（null-safe）。</li>
 *   <li>{@code timestamp} 于构造时初始化为 {@link System#currentTimeMillis()}。</li>
 * </ul>
 */
public class ResponseResult<T> {
    /** MDC 中存放 traceId 的键名，与 {@code TraceIdFilter} / {@code MultiRoleSessionFilter} 约定一致。 */
    private static final String MDC_TRACE_ID = "traceId";

    // 请求是否成功
    private boolean success;
    // 响应数据
    private T data;
    // 响应消息
    private String message;
    // 响应状态码
    private int code;
    // 链路追踪 ID（阶段 1 由 MDC 写入；MDC 缺失时为 null）
    private String traceId;
    // 服务端生成响应体时的时间戳（毫秒）
    private long timestamp;

    /**
     * 私有构造方法：所有静态工厂路径均走此构造器，保证每个 {@link ResponseResult} 实例
     * 都带上 {@code traceId} 与 {@code timestamp} 字段（R3.1）。
     */
    private ResponseResult() {
        this.timestamp = System.currentTimeMillis();
        this.traceId = MDC.get(MDC_TRACE_ID);
    }

    // 成功响应 - 带数据
    public static <T> ResponseResult<T> success(T data, String message, int code) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setSuccess(true);
        result.setData(data);
        result.setMessage(message);
        result.setCode(code);
        return result;
    }

    // 成功响应 - 默认消息和状态码
    public static <T> ResponseResult<T> success(T data) {
        return success(data, "操作成功", 200);
    }

    // 成功响应 - 无数据
    public static ResponseResult<Void> success() {
        return success(null, "操作成功", 200);
    }

    // 成功响应 - 创建成功
    public static <T> ResponseResult<T> created(T data) {
        return success(data, "创建成功", 201);
    }

    // 成功响应 - 无内容
    public static ResponseResult<Void> noContent() {
        return success(null, "操作成功", 204);
    }

    // 失败响应
    public static <T> ResponseResult<T> failure(String message, int code) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setSuccess(false);
        result.setData(null);
        result.setMessage(message);
        result.setCode(code);
        return result;
    }

    // 失败响应 - 默认状态码
    public static <T> ResponseResult<T> failure(String message) {
        return failure(message, 400);
    }

    // 失败响应 - 无消息
    public static <T> ResponseResult<T> failure(int code) {
        return failure("操作失败", code);
    }

    // Getter和Setter方法
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

    // 设置数据 - 支持链式调用
    public ResponseResult<T> data(T data) {
        this.setData(data);
        return this;
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
