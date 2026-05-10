package com._202510007517.major_assignment.aspect;

import com._202510007517.major_assignment.utils.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 日志切面
 * <p>
 * 统一记录 Controller 层的请求和响应日志。
 * </p>
 * <p>
 * 对齐 R3.4 / Design §7.2：切面<b>只读 MDC</b>，不再自行查 Session / Redis。
 * {@code traceId / userId / role / uri / method} 由
 * {@code MultiRoleSessionFilter}（以及阶段 1 后续任务的 {@code TraceIdFilter}）写入，
 * 本切面仅消费这些字段（R2.2 一致性不变量）。
 * </p>
 */
@Aspect
@Component
@Order(2)
public class LoggingAspect {

    private static final Logger logger = LogUtil.getLogger(LoggingAspect.class);

    private static final String MDC_USER_ID = "userId";
    private static final String MDC_STATUS = "status";
    private static final String MDC_ELAPSED_MS = "elapsed_ms";

    /**
     * 定义切点：所有Controller类的公共方法
     */
    @Pointcut("execution(public * com._202510007517.major_assignment.controller..*Controller.*(..))")
    public void controllerMethods() {
    }

    /**
     * 排除全局异常处理器
     */
    @Pointcut("!execution(* com._202510007517.major_assignment.controller.GlobalExceptionHandler.*(..))")
    public void excludeExceptionHandler() {
    }

    /**
     * 环绕通知：记录请求和响应日志
     */
    @Around("controllerMethods() && excludeExceptionHandler()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 只读 MDC 与 request 的基础元数据；不再访问 Session。
        String userId = MDC.get(MDC_USER_ID);
        String requestUri = "unknown";
        String method = "unknown";

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            requestUri = request.getRequestURI();
            method = request.getMethod();
        }

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        // 记录请求日志
        logger.debug("[{}] {} {} - {}.{} - userId: {}",
                method, requestUri, "开始处理", className, methodName, userId);

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            // 将访问日志的关键字段临时写入 MDC，供 JSON encoder 透出 elapsed_ms / status（Design §7.2）。
            MDC.put(MDC_ELAPSED_MS, String.valueOf(duration));
            MDC.put(MDC_STATUS, "200");
            try {
                logger.info("[{}] {} - {}.{} - userId: {} - 耗时: {}ms",
                        method, requestUri, className, methodName, userId, duration);
            } finally {
                MDC.remove(MDC_ELAPSED_MS);
                MDC.remove(MDC_STATUS);
            }
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            MDC.put(MDC_ELAPSED_MS, String.valueOf(duration));
            MDC.put(MDC_STATUS, "500");
            try {
                logger.error("[{}] {} - {}.{} - userId: {} - 耗时: {}ms - 异常: {}",
                        method, requestUri, className, methodName, userId, duration, e.getMessage());
            } finally {
                MDC.remove(MDC_ELAPSED_MS);
                MDC.remove(MDC_STATUS);
            }
            throw e;
        }
    }
}
