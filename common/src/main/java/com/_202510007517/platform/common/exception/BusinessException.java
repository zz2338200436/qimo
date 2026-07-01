package com._202510007517.platform.common.exception;

public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(message, 400);
    }

    public BusinessException(String message, int code) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
