package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.constants.ErrorMessages;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.exception.BusinessException;
import com._202510007517.major_assignment.exception.ForbiddenException;
import com._202510007517.major_assignment.exception.RateLimitExceededException;
import com._202510007517.major_assignment.exception.RemoteClientException;
import com._202510007517.major_assignment.exception.RemoteServerException;
import com._202510007517.major_assignment.exception.ResourceNotFoundException;
import com._202510007517.major_assignment.exception.UnauthorizedException;
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
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * 全局异常处理器（单体阶段）。
 * <p>
 * 对齐 Design §Error Handling §3 的异常—HTTP 状态码映射表，为<b>唯一</b>异常出口。
 * Controller / Service / Aspect 均禁止在 catch 块里返回 {@link ResponseResult} 后
 * 吞掉异常，否则 MDC 上下文将缺失。
 * </p>
 * <p>
 * MDC 规则（对齐 Design §Error Handling §1）：
 * <ul>
 *   <li>本处理器只在<b>处理器方法内部</b>临时追加 {@code uri}/{@code method}/{@code exceptionType}，
 *       并在 {@code finally} 中精确 {@link MDC#remove(String) remove} 本处理器追加的键。</li>
 *   <li>不得调用 {@link MDC#clear()}——该操作归 Filter 层负责（{@code TraceIdFilter} /
 *       {@code MultiRoleSessionFilter}）。</li>
 * </ul>
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String MDC_URI = "uri";
    private static final String MDC_METHOD = "method";
    private static final String MDC_EXCEPTION_TYPE = "exceptionType";

    private final Environment environment;

    public GlobalExceptionHandler(Environment environment) {
        this.environment = environment;
    }

    // ===================================================================
    // 业务与鉴权异常
    // ===================================================================

    /** 401：未授权。 */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ResponseResult<Object>> handleUnauthorizedException(UnauthorizedException ex,
                                                                              HttpServletRequest request) {
        try {
            putMdc(request, ex);
            logger.warn("未授权访问: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseResult.failure(ex.getMessage(), ex.getCode()));
        } finally {
            cleanupMdc();
        }
    }

    /** 403：禁止访问。 */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ResponseResult<Object>> handleForbiddenException(ForbiddenException ex,
                                                                           HttpServletRequest request) {
        try {
            putMdc(request, ex);
            logger.warn("禁止访问: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseResult.failure(ex.getMessage(), ex.getCode()));
        } finally {
            cleanupMdc();
        }
    }

    /** 404：资源不存在。 */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ResponseResult<Object>> handleResourceNotFoundException(ResourceNotFoundException ex,
                                                                                  HttpServletRequest request) {
        try {
            putMdc(request, ex);
            logger.warn("资源未找到: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseResult.failure(ex.getMessage(), ex.getCode()));
        } finally {
            cleanupMdc();
        }
    }

    /** 400：业务异常；若 code 不属于 {@code [400,499]} 则视为自定义业务子码，回填 {@code data.bizCode}。 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ResponseResult<Map<String, Object>>> handleBusinessException(BusinessException ex,
                                                                                       HttpServletRequest request) {
        try {
            putMdc(request, ex);
            logger.warn("业务异常: code={}, message={}", ex.getCode(), ex.getMessage());
            ResponseResult<Map<String, Object>> body =
                    ResponseResult.<Map<String, Object>>failure(ex.getMessage(), ex.getCode());
            int code = ex.getCode();
            if (code < 400 || code > 499) {
                // 业务子码：原文带回以便前端精准识别
                body.data(Map.of("bizCode", code));
            }
            // 无论 code 是通用 400 还是自定义子码，HTTP 状态固定 400（Design §3 约束）
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        } finally {
            cleanupMdc();
        }
    }

    // ===================================================================
    // 参数校验异常
    // ===================================================================

    /** 400：@RequestBody @Valid 失败。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseResult<Map<String, Object>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        try {
            putMdc(request, ex);
            Map<String, String> errors = new HashMap<>();
            ex.getBindingResult().getAllErrors().forEach(error -> {
                String fieldName = ((FieldError) error).getField();
                String errorMessage = error.getDefaultMessage();
                errors.put(fieldName, errorMessage);
            });
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseResult.<Map<String, Object>>failure(ErrorMessages.PARAM_INVALID, 400)
                            .data(Map.of("fields", errors)));
        } finally {
            cleanupMdc();
        }
    }

    /** 400：表单绑定失败。 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ResponseResult<Map<String, Object>>> handleBindException(BindException ex,
                                                                                   HttpServletRequest request) {
        try {
            putMdc(request, ex);
            Map<String, String> errors = new HashMap<>();
            ex.getBindingResult().getAllErrors().forEach(error -> {
                String fieldName = ((FieldError) error).getField();
                String errorMessage = error.getDefaultMessage();
                errors.put(fieldName, errorMessage);
            });
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseResult.<Map<String, Object>>failure(ErrorMessages.PARAM_BINDING_FAILED, 400)
                            .data(Map.of("fields", errors)));
        } finally {
            cleanupMdc();
        }
    }

    /** 400：jakarta.validation 约束违反（常见于 @Validated + @RequestParam）。 */
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

    /** 400：非法参数。 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseResult<Object>> handleIllegalArgumentException(IllegalArgumentException ex,
                                                                                 HttpServletRequest request) {
        try {
            putMdc(request, ex);
            logger.warn("非法参数: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseResult.failure(ex.getMessage(), 400));
        } finally {
            cleanupMdc();
        }
    }

    /** 400：请求体无法解析（JSON 格式错误、类型不匹配等）。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseResult<Object>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        try {
            putMdc(request, ex);
            logger.warn("请求体格式错误: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseResult.failure(ErrorMessages.REQUEST_BODY_INVALID, 400));
        } finally {
            cleanupMdc();
        }
    }

    /** 405：请求方法不支持。 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResponseResult<Object>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        try {
            putMdc(request, ex);
            logger.warn("方法不支持: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                    .body(ResponseResult.failure(ErrorMessages.METHOD_NOT_ALLOWED, 405));
        } finally {
            cleanupMdc();
        }
    }

    /** 404：未匹配到任何处理器。 */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ResponseResult<Object>> handleNoHandlerFoundException(NoHandlerFoundException ex,
                                                                                HttpServletRequest request) {
        try {
            putMdc(request, ex);
            String message = "No endpoint " + ex.getHttpMethod() + " " + ex.getRequestURL() + ".";
            logger.warn("未匹配到处理器: {}", message);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseResult.failure(message, 404));
        } finally {
            cleanupMdc();
        }
    }

    // ===================================================================
    // 远程调用 / 熔断 / 限流 / 超时（阶段 2 后续补齐；阶段 1 先登记类型）
    // ===================================================================

    /** 透传下游 4xx：HTTP 状态与响应 code 均来自下游。 */
    @ExceptionHandler(RemoteClientException.class)
    public ResponseEntity<ResponseResult<Object>> handleRemoteClientException(RemoteClientException ex,
                                                                              HttpServletRequest request) {
        try {
            putMdc(request, ex);
            logger.warn("下游 4xx 透传: code={}, message={}", ex.getCode(), ex.getMessage());
            int code = ex.getCode();
            HttpStatus status = resolveStatus(code, HttpStatus.BAD_REQUEST);
            return ResponseEntity.status(status)
                    .body(ResponseResult.failure(ex.getMessage(), code));
        } finally {
            cleanupMdc();
        }
    }

    /** 502：下游 5xx 聚合。 */
    @ExceptionHandler(RemoteServerException.class)
    public ResponseEntity<ResponseResult<Object>> handleRemoteServerException(RemoteServerException ex,
                                                                              HttpServletRequest request) {
        try {
            putMdc(request, ex);
            logger.error("下游 5xx: code={}, message={}", ex.getCode(), ex.getMessage());
            String message = isProduction() ? ErrorMessages.DOWNSTREAM_ERROR : ex.getMessage();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(ResponseResult.failure(message, 502));
        } finally {
            cleanupMdc();
        }
    }

    // TODO(task 14.x): Add CallNotPermittedException handler when Resilience4j is introduced
    // 依 Design §Error Handling §3，CallNotPermittedException → HTTP 503 + data.circuit = "OPEN"。
    // 由于阶段 1 尚未引入 io.github.resilience4j 依赖，此处暂不声明 @ExceptionHandler；阶段 2
    // Gateway 搭建（tasks.md 14.7）完成后再补齐。

    /** 504：下游超时（{@link java.util.concurrent.TimeoutException}）。 */
    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ResponseResult<Object>> handleTimeoutException(TimeoutException ex,
                                                                         HttpServletRequest request) {
        try {
            putMdc(request, ex);
            logger.error("请求超时: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                    .body(ResponseResult.failure(ErrorMessages.TIMEOUT, 504));
        } finally {
            cleanupMdc();
        }
    }

    /** 429：限流命中，附带 {@code data.retryAfter}。 */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ResponseResult<Map<String, Object>>> handleRateLimitExceededException(
            RateLimitExceededException ex, HttpServletRequest request) {
        try {
            putMdc(request, ex);
            logger.warn("限流命中: retryAfter={}s, message={}", ex.getRetryAfterSeconds(), ex.getMessage());
            String message = ex.getMessage() != null ? ex.getMessage() : ErrorMessages.RATE_LIMITED;
            ResponseResult<Map<String, Object>> body = ResponseResult.<Map<String, Object>>failure(message, 429)
                    .data(Map.of("retryAfter", ex.getRetryAfterSeconds()));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
        } finally {
            cleanupMdc();
        }
    }

    // ===================================================================
    // 兜底
    // ===================================================================

    /** 500：未捕获的异常。prod 下隐藏堆栈与内部消息（R3.3）。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseResult<Object>> handleException(Exception ex, HttpServletRequest request) {
        try {
            putMdc(request, ex);
            logger.error("服务器内部错误 - URI: {}, Method: {}", request.getRequestURI(), request.getMethod(), ex);
            String message = isProduction() ? ErrorMessages.INTERNAL_ERROR : ex.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseResult.<Object>failure(message, 500));
        } finally {
            cleanupMdc();
        }
    }

    // ===================================================================
    // Helpers
    // ===================================================================

    /**
     * 将本次请求的 {@code uri} / {@code method} / {@code exceptionType} 写入 MDC，
     * 供日志 Appender 组装结构化字段使用（Design §7.2）。
     */
    private void putMdc(HttpServletRequest request, Throwable ex) {
        if (request != null) {
            if (request.getRequestURI() != null) {
                MDC.put(MDC_URI, request.getRequestURI());
            }
            if (request.getMethod() != null) {
                MDC.put(MDC_METHOD, request.getMethod());
            }
        }
        if (ex != null) {
            MDC.put(MDC_EXCEPTION_TYPE, ex.getClass().getSimpleName());
        }
    }

    /**
     * 精确移除本处理器追加的 MDC 键（<b>不使用</b> {@link MDC#clear()}，
     * 避免抹除上游 Filter 写入的 {@code traceId / userId / role}）。
     */
    private void cleanupMdc() {
        MDC.remove(MDC_URI);
        MDC.remove(MDC_METHOD);
        MDC.remove(MDC_EXCEPTION_TYPE);
    }

    private boolean isProduction() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
    }

    /** 将任意整数 code 解析为 {@link HttpStatus}；非 [100, 599] 时回退到 {@code fallback}。 */
    private HttpStatus resolveStatus(int code, HttpStatus fallback) {
        HttpStatus status = HttpStatus.resolve(code);
        return status != null ? status : fallback;
    }

    /**
     * 留给后续扩展：标准化字段级错误表的组装顺序（当前使用普通 HashMap 已足够，
     * 显式声明一处便于保持顺序一致的单元测试断言）。
     */
    @SuppressWarnings("unused")
    private static Map<String, String> orderedFieldMap() {
        return new LinkedHashMap<>();
    }
}
