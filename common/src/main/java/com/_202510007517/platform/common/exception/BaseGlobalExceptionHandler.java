package com._202510007517.platform.common.exception;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * 各微服务可复用的全局异常处理基类。
 */
public abstract class BaseGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(BaseGlobalExceptionHandler.class);

    private final Environment environment;

    protected BaseGlobalExceptionHandler(Environment environment) {
        this.environment = environment;
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ResponseResult<Object>> handleUnauthorizedException(UnauthorizedException ex,
                                                                              HttpServletRequest request) {
        try {
            putMdc(request, ex);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseResult.failure(ex.getMessage(), ex.getCode()));
        } finally {
            cleanupMdc();
        }
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ResponseResult<Object>> handleForbiddenException(ForbiddenException ex,
                                                                           HttpServletRequest request) {
        try {
            putMdc(request, ex);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseResult.failure(ex.getMessage(), ex.getCode()));
        } finally {
            cleanupMdc();
        }
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ResponseResult<Object>> handleResourceNotFoundException(ResourceNotFoundException ex,
                                                                                  HttpServletRequest request) {
        try {
            putMdc(request, ex);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseResult.failure(ex.getMessage(), ex.getCode()));
        } finally {
            cleanupMdc();
        }
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ResponseResult<Map<String, Object>>> handleBusinessException(BusinessException ex,
                                                                                       HttpServletRequest request) {
        try {
            putMdc(request, ex);
            ResponseResult<Map<String, Object>> body =
                    ResponseResult.<Map<String, Object>>failure(ex.getMessage(), ex.getCode());
            if (ex.getCode() < 400 || ex.getCode() > 499) {
                body.data(Map.of("bizCode", ex.getCode()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        } finally {
            cleanupMdc();
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseResult<Map<String, Object>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        try {
            putMdc(request, ex);
            Map<String, String> errors = new HashMap<>();
            ex.getBindingResult().getAllErrors().forEach(error -> {
                String fieldName = ((FieldError) error).getField();
                errors.put(fieldName, error.getDefaultMessage());
            });
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseResult.<Map<String, Object>>failure("参数校验失败", 400)
                            .data(Map.of("fields", errors)));
        } finally {
            cleanupMdc();
        }
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ResponseResult<Map<String, Object>>> handleBindException(BindException ex,
                                                                                   HttpServletRequest request) {
        try {
            putMdc(request, ex);
            Map<String, String> errors = new HashMap<>();
            ex.getBindingResult().getAllErrors().forEach(error -> {
                String fieldName = ((FieldError) error).getField();
                errors.put(fieldName, error.getDefaultMessage());
            });
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseResult.<Map<String, Object>>failure("参数绑定失败", 400)
                            .data(Map.of("fields", errors)));
        } finally {
            cleanupMdc();
        }
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResponseResult<Map<String, Object>>> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        try {
            putMdc(request, ex);
            Map<String, String> errors = new HashMap<>();
            Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
            for (ConstraintViolation<?> violation : violations) {
                errors.put(violation.getPropertyPath().toString(), violation.getMessage());
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseResult.<Map<String, Object>>failure("约束违反异常", 400)
                            .data(Map.of("fields", errors)));
        } finally {
            cleanupMdc();
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseResult<Object>> handleIllegalArgumentException(IllegalArgumentException ex,
                                                                                 HttpServletRequest request) {
        try {
            putMdc(request, ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseResult.failure(ex.getMessage(), 400));
        } finally {
            cleanupMdc();
        }
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseResult<Object>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        try {
            putMdc(request, ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseResult.failure("请求体格式错误", 400));
        } finally {
            cleanupMdc();
        }
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResponseResult<Object>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        try {
            putMdc(request, ex);
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                    .body(ResponseResult.failure("请求方法不支持", 405));
        } finally {
            cleanupMdc();
        }
    }

    @ExceptionHandler(RemoteClientException.class)
    public ResponseEntity<ResponseResult<Object>> handleRemoteClientException(RemoteClientException ex,
                                                                              HttpServletRequest request) {
        try {
            putMdc(request, ex);
            HttpStatus status = resolveStatus(ex.getCode(), HttpStatus.BAD_REQUEST);
            return ResponseEntity.status(status)
                    .body(ResponseResult.failure(ex.getMessage(), ex.getCode()));
        } finally {
            cleanupMdc();
        }
    }

    @ExceptionHandler(RemoteServerException.class)
    public ResponseEntity<ResponseResult<Object>> handleRemoteServerException(RemoteServerException ex,
                                                                              HttpServletRequest request) {
        try {
            putMdc(request, ex);
            String message = isProduction() ? "下游服务暂不可用" : ex.getMessage();
            HttpStatus status = resolveStatus(ex.getCode(), HttpStatus.BAD_GATEWAY);
            return ResponseEntity.status(status)
                    .body(ResponseResult.failure(message, status.value()));
        } finally {
            cleanupMdc();
        }
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ResponseResult<Object>> handleTimeoutException(TimeoutException ex,
                                                                         HttpServletRequest request) {
        try {
            putMdc(request, ex);
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                    .body(ResponseResult.failure("请求超时", 504));
        } finally {
            cleanupMdc();
        }
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ResponseResult<Map<String, Object>>> handleRateLimitExceededException(
            RateLimitExceededException ex, HttpServletRequest request) {
        try {
            putMdc(request, ex);
            String message = ex.getMessage() != null ? ex.getMessage() : "请求过于频繁";
            ResponseResult<Map<String, Object>> body = ResponseResult.<Map<String, Object>>failure(message, 429)
                    .data(Map.of("retryAfter", ex.getRetryAfterSeconds()));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
        } finally {
            cleanupMdc();
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseResult<Object>> handleException(Exception ex, HttpServletRequest request) {
        try {
            putMdc(request, ex);
            log.error("Unhandled exception on {} {}", request != null ? request.getMethod() : "UNKNOWN",
                    request != null ? request.getRequestURI() : "UNKNOWN", ex);
            String message = isProduction() ? "服务器内部错误" : ex.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseResult.failure(message, 500));
        } finally {
            cleanupMdc();
        }
    }

    protected void putMdc(HttpServletRequest request, Throwable ex) {
        if (request != null) {
            if (request.getRequestURI() != null) {
                MDC.put(CommonTraceConstants.MDC_URI, request.getRequestURI());
            }
            if (request.getMethod() != null) {
                MDC.put(CommonTraceConstants.MDC_METHOD, request.getMethod());
            }
        }
        if (ex != null) {
            MDC.put(CommonTraceConstants.MDC_EXCEPTION_TYPE, ex.getClass().getSimpleName());
        }
    }

    protected void cleanupMdc() {
        MDC.remove(CommonTraceConstants.MDC_URI);
        MDC.remove(CommonTraceConstants.MDC_METHOD);
        MDC.remove(CommonTraceConstants.MDC_EXCEPTION_TYPE);
    }

    protected boolean isProduction() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
    }

    protected HttpStatus resolveStatus(int code, HttpStatus fallback) {
        HttpStatus status = HttpStatus.resolve(code);
        return status != null ? status : fallback;
    }
}
