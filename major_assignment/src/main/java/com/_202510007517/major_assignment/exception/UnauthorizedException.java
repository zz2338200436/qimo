package com._202510007517.major_assignment.exception;

import com._202510007517.major_assignment.constants.ErrorMessages;

/**
 * 未授权异常
 * 当用户未登录或登录已过期时抛出
 */
public class UnauthorizedException extends RuntimeException {
    
    private final int code;
    
    public UnauthorizedException() {
        super(ErrorMessages.UNAUTHORIZED);
        this.code = 401;
    }
    
    public UnauthorizedException(String message) {
        super(message);
        this.code = 401;
    }
    
    public UnauthorizedException(String message, int code) {
        super(message);
        this.code = code;
    }
    
    public int getCode() {
        return code;
    }
}
