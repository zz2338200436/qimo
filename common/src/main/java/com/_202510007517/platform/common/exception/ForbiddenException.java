package com._202510007517.platform.common.exception;

public class ForbiddenException extends RuntimeException {

    private final int code;

    public ForbiddenException() {
        this("禁止访问", 403);
    }

    public ForbiddenException(String message) {
        this(message, 403);
    }

    public ForbiddenException(String message, int code) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
