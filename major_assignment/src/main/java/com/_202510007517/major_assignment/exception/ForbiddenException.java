package com._202510007517.major_assignment.exception;

import com._202510007517.major_assignment.constants.ErrorMessages;

/**
 * 禁止访问异常
 * 当用户没有权限访问资源时抛出
 */
public class ForbiddenException extends RuntimeException {
    
    private final int code;
    
    public ForbiddenException() {
        super(ErrorMessages.FORBIDDEN);
        this.code = 403;
    }
    
    public ForbiddenException(String message) {
        super(message);
        this.code = 403;
    }
    
    public int getCode() {
        return code;
    }
}
