package com._202510007517.major_assignment.utils;

/**
 * 类型转换工具类
 * 提供安全的类型转换方法
 */
public final class TypeUtils {
    
    private TypeUtils() {
        // 防止实例化
    }
    
    /**
     * 安全转换为Integer
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 转换后的Integer值
     */
    public static int safeInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 安全转换为Long
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 转换后的Long值
     */
    public static long safeLong(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 安全转换为Double
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 转换后的Double值
     */
    public static double safeDouble(Object value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 安全转换为Boolean
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 转换后的Boolean值
     */
    public static boolean safeBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String str = value.toString().trim().toLowerCase();
        if ("true".equals(str) || "1".equals(str) || "yes".equals(str)) {
            return true;
        }
        if ("false".equals(str) || "0".equals(str) || "no".equals(str)) {
            return false;
        }
        return defaultValue;
    }
    
    /**
     * 安全转换为String
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 转换后的String值
     */
    public static String safeString(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }
    
    /**
     * 判断字符串是否为空或空白
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * 判断字符串是否不为空且不为空白
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
}
