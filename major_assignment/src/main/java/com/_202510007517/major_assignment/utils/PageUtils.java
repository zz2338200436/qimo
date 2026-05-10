package com._202510007517.major_assignment.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分页工具类
 * 统一构建分页响应结构
 */
public final class PageUtils {
    
    private PageUtils() {
        // 防止实例化
    }
    
    /**
     * 构建分页响应
     * @param content 分页内容
     * @param page 当前页码（从1开始）
     * @param size 每页大小
     * @param totalElements 总元素数
     * @return 分页响应Map
     */
    public static <T> Map<String, Object> buildPageResponse(List<T> content, int page, int size, long totalElements) {
        Map<String, Object> result = new HashMap<>();
        
        int totalPages = totalElements > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        int offset = (page - 1) * size;
        
        // 内容
        result.put("content", content != null ? content : new ArrayList<>());
        
        // 构建pageable
        Map<String, Object> pageable = new HashMap<>();
        pageable.put("pageNumber", page - 1); // 前端从1开始，后端从0开始
        pageable.put("pageSize", size);
        pageable.put("offset", offset);
        pageable.put("paged", true);
        pageable.put("unpaged", false);
        
        // 构建sort
        Map<String, Object> sort = new HashMap<>();
        sort.put("empty", false);
        sort.put("sorted", true);
        sort.put("unsorted", false);
        pageable.put("sort", sort);
        
        result.put("pageable", pageable);
        result.put("sort", sort);
        
        // 分页信息
        result.put("totalPages", totalPages);
        result.put("totalElements", totalElements);
        result.put("size", size);
        result.put("number", page - 1);
        result.put("first", page == 1);
        result.put("last", page >= totalPages || totalPages == 0);
        result.put("numberOfElements", content != null ? content.size() : 0);
        result.put("empty", content == null || content.isEmpty());
        
        return result;
    }
    
    /**
     * 对列表进行内存分页
     * @param list 原始列表
     * @param page 当前页码（从1开始）
     * @param size 每页大小
     * @return 分页后的列表
     */
    public static <T> List<T> paginate(List<T> list, int page, int size) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        
        int startIndex = (page - 1) * size;
        if (startIndex >= list.size()) {
            return new ArrayList<>();
        }
        
        int endIndex = Math.min(startIndex + size, list.size());
        return new ArrayList<>(list.subList(startIndex, endIndex));
    }
    
    /**
     * 计算总页数
     * @param totalElements 总元素数
     * @param size 每页大小
     * @return 总页数
     */
    public static int calculateTotalPages(long totalElements, int size) {
        if (totalElements <= 0 || size <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / size);
    }
    
    /**
     * 计算偏移量
     * @param page 当前页码（从1开始）
     * @param size 每页大小
     * @return 偏移量
     */
    public static int calculateOffset(int page, int size) {
        return (Math.max(page, 1) - 1) * size;
    }
    
    /**
     * 安全获取页码
     * @param page 原始页码
     * @param defaultPage 默认页码
     * @return 安全的页码（至少为1）
     */
    public static int safePage(Integer page, int defaultPage) {
        if (page == null || page < 1) {
            return defaultPage;
        }
        return page;
    }
    
    /**
     * 安全获取每页大小
     * @param size 原始大小
     * @param defaultSize 默认大小
     * @param maxSize 最大大小
     * @return 安全的每页大小
     */
    public static int safeSize(Integer size, int defaultSize, int maxSize) {
        if (size == null || size < 1) {
            return defaultSize;
        }
        return Math.min(size, maxSize);
    }
}
