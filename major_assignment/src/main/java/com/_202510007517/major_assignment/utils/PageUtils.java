package com._202510007517.major_assignment.utils;

import com._202510007517.major_assignment.entity.dto.PageResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分页工具类
 * 统一构建分页响应结构
 */
public final class PageUtils {

    public record PageWindow(int page, int size, long totalElements, int totalPages, int offset) {
    }

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;
    
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
        PageWindow window = resolvePageWindow(page, size, totalElements);
        int safePageSize = window.size();
        int safePage = window.page();
        int totalPages = window.totalPages();
        int offset = window.offset();
        
        // 内容
        result.put("content", content != null ? content : new ArrayList<>());
        
        // 构建pageable
        Map<String, Object> pageable = new HashMap<>();
        pageable.put("pageNumber", safePage - 1); // 前端从1开始，后端从0开始
        pageable.put("pageSize", safePageSize);
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
        result.put("size", safePageSize);
        result.put("number", safePage - 1);
        result.put("first", safePage == 1);
        result.put("last", safePage >= totalPages || totalPages == 0);
        result.put("numberOfElements", content != null ? content.size() : 0);
        result.put("empty", content == null || content.isEmpty());
        
        return result;
    }

    public static <T> PageResult<T> buildPageResult(List<T> content, int page, int size, long totalElements) {
        PageWindow window = resolvePageWindow(page, size, totalElements);
        int safePageSize = window.size();
        int safePage = window.page();
        int totalPages = window.totalPages();
        int offset = window.offset();
        List<T> safeContent = content != null ? content : new ArrayList<>();

        PageResult<T> result = new PageResult<>();
        result.setContent(safeContent);
        result.setPageNumber(safePage);
        result.setPageSize(safePageSize);
        result.setTotalElements(totalElements);
        result.setTotalPages(totalPages);
        result.setFirst(safePage == 1);
        result.setLast(safePage >= totalPages || totalPages == 0);
        result.setOffset(offset);
        result.setNumberOfElements(safeContent.size());
        result.setEmpty(safeContent.isEmpty());
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

        PageWindow window = resolvePageWindow(page, size, list.size());
        int safeSize = window.size();
        int startIndex = window.offset();
        if (startIndex >= list.size()) {
            return new ArrayList<>();
        }

        int endIndex = Math.min(startIndex + safeSize, list.size());
        return new ArrayList<>(list.subList(startIndex, endIndex));
    }

    public static PageWindow resolvePageWindow(int page, int size, long totalElements) {
        int safeSize = clampPageSize(size);
        int safePage = boundPageNum(page, totalElements, safeSize);
        int totalPages = calculateTotalPages(totalElements, safeSize);
        int offset = calculateOffset(safePage, safeSize);
        return new PageWindow(safePage, safeSize, totalElements, totalPages, offset);
    }

    public static int clampPageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        if (size > MAX_PAGE_SIZE) {
            return MAX_PAGE_SIZE;
        }
        return size;
    }

    public static int clampPageNum(int page) {
        return page <= 0 ? 1 : page;
    }
    
    /**
     * 计算总页数
     * @param totalElements 总元素数
     * @param size 每页大小
     * @return 总页数
     */
    public static int calculateTotalPages(long totalElements, int size) {
        if (totalElements <= 0) {
            return 0;
        }
        int safeSize = clampPageSize(size);
        return (int) Math.ceil((double) totalElements / safeSize);
    }

    public static int boundPageNum(int page, long totalElements, int size) {
        int safePage = clampPageNum(page);
        int totalPages = calculateTotalPages(totalElements, size);
        if (totalPages == 0) {
            return 1;
        }
        return Math.min(safePage, totalPages);
    }
    
    /**
     * 计算偏移量
     * @param page 当前页码（从1开始）
     * @param size 每页大小
     * @return 偏移量
     */
    public static int calculateOffset(int page, int size) {
        return (clampPageNum(page) - 1) * clampPageSize(size);
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
