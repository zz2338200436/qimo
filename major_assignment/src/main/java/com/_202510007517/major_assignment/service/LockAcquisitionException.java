package com._202510007517.major_assignment.service;

/**
 * 分布式锁获取失败异常（Task 8.5 / R5.4）。
 *
 * <p>当 {@link DistributedLock#withLock(String, String, java.time.Duration, java.util.function.Supplier)}
 * 在非阻塞 {@code tryLock} 失败时抛出。非受检异常，调用方可按需捕获或让其传播到
 * 全局异常处理器转换为标准 {@code ErrorResponse}。</p>
 */
public class LockAcquisitionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LockAcquisitionException(String message) {
        super(message);
    }

    public LockAcquisitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
