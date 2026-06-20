package com._202510007517.platform.agent.config;

import com._202510007517.platform.common.exception.BaseGlobalExceptionHandler;
import com._202510007517.platform.common.web.ResponseResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AgentServiceExceptionHandler extends BaseGlobalExceptionHandler {

    public AgentServiceExceptionHandler(Environment environment) {
        super(environment);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ResponseResult<Object>> handleSecurityException(SecurityException ex,
                                                                          HttpServletRequest request) {
        try {
            putMdc(request, ex);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseResult.failure(ex.getMessage(), 403));
        } finally {
            cleanupMdc();
        }
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ResponseResult<Object>> handleIllegalStateException(IllegalStateException ex,
                                                                              HttpServletRequest request) {
        try {
            putMdc(request, ex);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ResponseResult.failure(ex.getMessage(), 409));
        } finally {
            cleanupMdc();
        }
    }
}
