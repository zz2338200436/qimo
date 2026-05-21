package com._202510007517.platform.common.exception;

public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = Math.max(0L, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
