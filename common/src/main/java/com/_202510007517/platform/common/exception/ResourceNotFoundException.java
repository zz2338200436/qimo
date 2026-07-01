package com._202510007517.platform.common.exception;

public class ResourceNotFoundException extends RuntimeException {

    private final int code;

    public ResourceNotFoundException() {
        this("资源不存在", 404);
    }

    public ResourceNotFoundException(String message) {
        this(message, 404);
    }

    public ResourceNotFoundException(String message, int code) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
