package com._202510007517.major_assignment.exception;

/**
 * 限流命中时抛出的异常。
 * <p>
 * 对齐 Design §Error Handling §3：映射到 HTTP 429；响应体中 {@code data.retryAfter}
 * 为建议重试秒数（参考 Property 4 响应契约的 Fallback code 集 {429, 503, 504}）。
 * </p>
 */
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = Math.max(0L, retryAfterSeconds);
    }

    /** 建议客户端在指定秒数后重试；非负整数。 */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
