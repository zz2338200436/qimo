package com._202510007517.major_assignment.exception;

import com._202510007517.major_assignment.constants.ErrorMessages;

/**
 * 资源未找到异常
 * 当请求的资源不存在时抛出
 */
public class ResourceNotFoundException extends RuntimeException {
    
    private final int code;
    
    public ResourceNotFoundException() {
        super(ErrorMessages.NOT_FOUND);
        this.code = 404;
    }
    
    public ResourceNotFoundException(String message) {
        super(message);
        this.code = 404;
    }
    
    public int getCode() {
        return code;
    }
}
