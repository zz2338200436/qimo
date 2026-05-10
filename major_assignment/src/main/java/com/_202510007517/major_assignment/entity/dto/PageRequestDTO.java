package com._202510007517.major_assignment.entity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * 分页请求参数 DTO，统一管理分页参数的校验。
 *
 * <p>对齐 Requirement 5.5 与 Design §7.5：单页最大条数 100。</p>
 *
 * <ul>
 *   <li>字段级约束 {@code @Min(1)} / {@code @Max(100)} 是第一道防线，
 *       配合 Controller 上的 {@code @Valid} 触发
 *       {@link org.springframework.web.bind.MethodArgumentNotValidException}，
 *       由 {@code GlobalExceptionHandler} 映射为 HTTP 400。</li>
 *   <li>{@link com._202510007517.major_assignment.controller.BaseController#clampPageSize(int)}
 *       提供第二道兜底：未走 {@code @Valid} 的入口（例如直接使用
 *       {@code @RequestParam}）仍可夹紧到合法区间，防止意外全表扫描。</li>
 * </ul>
 *
 * <p>注意：setter 仅处理 {@code null}→默认值，不再对越界值做静默夹紧，
 * 以免绕过上面提到的字段级校验。</p>
 */
public class PageRequestDTO {

    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    @Min(value = 1, message = "每页数量必须大于0")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer size = 10;

    @Pattern(regexp = "^[a-zA-Z_][a-zA-Z0-9_]*$", message = "排序字段格式不正确")
    private String sortBy = "id";

    @Pattern(regexp = "^(ASC|DESC|asc|desc)$", message = "排序方向只能是ASC或DESC")
    private String order = "DESC";

    public PageRequestDTO() {
    }

    public PageRequestDTO(Integer page, Integer size) {
        this.page = page != null ? page : 1;
        this.size = size != null ? size : 10;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        // 仅把 null 规整为默认值；越界值原样保留，交由 @Valid 校验（R5.5）
        this.page = page != null ? page : 1;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        // 仅把 null 规整为默认值；越界值原样保留，交由 @Valid 校验（R5.5）
        this.size = size != null ? size : 10;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy != null && !sortBy.isEmpty() ? sortBy : "id";
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order != null && (order.equalsIgnoreCase("ASC") || order.equalsIgnoreCase("DESC"))
                ? order.toUpperCase() : "DESC";
    }

    /**
     * 获取偏移量。
     */
    public int getOffset() {
        return (page - 1) * size;
    }
}
