package com._202510007517.major_assignment.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志工具类
 * 提供更简洁的日志记录方法
 */
public class LogUtil {
    
    /**
     * 获取Logger实例
     * @param clazz 类对象
     * @return Logger实例
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
    
    /**
     * 获取Logger实例
     * @param name 日志名称
     * @return Logger实例
     */
    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }
    
    /**
     * 记录请求日志
     * @param logger Logger实例
     * @param method 请求方法
     * @param url 请求URL
     * @param params 请求参数
     * @param userId 用户ID
     */
    public static void logRequest(Logger logger, String method, String url, Object params, Long userId) {
        logger.info("Request - Method: {}, URL: {}, Params: {}, UserId: {}", method, url, params, userId);
    }
    
    /**
     * 记录响应日志
     * @param logger Logger实例
     * @param method 请求方法
     * @param url 请求URL
     * @param statusCode 状态码
     * @param response 响应数据
     * @param userId 用户ID
     */
    public static void logResponse(Logger logger, String method, String url, int statusCode, Object response, Long userId) {
        logger.info("Response - Method: {}, URL: {}, StatusCode: {}, Response: {}, UserId: {}", method, url, statusCode, response, userId);
    }
    
    /**
     * 记录业务操作日志
     * @param logger Logger实例
     * @param operation 操作描述
     * @param detail 详细信息
     * @param userId 用户ID
     * @param result 操作结果
     */
    public static void logOperation(Logger logger, String operation, String detail, Long userId, boolean result) {
        logger.info("Operation - {}, Detail: {}, UserId: {}, Result: {}", operation, detail, userId, result ? "SUCCESS" : "FAILED");
    }
    
    /**
     * 记录错误日志
     * @param logger Logger实例
     * @param message 错误消息
     * @param e 异常对象
     */
    public static void logError(Logger logger, String message, Exception e) {
        logger.error("Error - {}, Exception: {}", message, e.getMessage(), e);
    }
    
    /**
     * 记录警告日志
     * @param logger Logger实例
     * @param message 警告消息
     * @param params 附加参数
     */
    public static void logWarning(Logger logger, String message, Object... params) {
        logger.warn("Warning - {}", message, params);
    }
    
    /**
     * 记录调试日志
     * @param logger Logger实例
     * @param message 调试消息
     * @param params 附加参数
     */
    public static void logDebug(Logger logger, String message, Object... params) {
        logger.debug("Debug - {}", message, params);
    }
}