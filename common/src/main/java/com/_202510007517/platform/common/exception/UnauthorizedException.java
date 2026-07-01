package com._202510007517.platform.common.exception;

public class UnauthorizedException extends RuntimeException {

    private final int code;

    public UnauthorizedException() {
        this("未授权访问", 401);
    }

    public UnauthorizedException(String message) {
        this(message, 401);
    }

    public UnauthorizedException(String message, int code) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
