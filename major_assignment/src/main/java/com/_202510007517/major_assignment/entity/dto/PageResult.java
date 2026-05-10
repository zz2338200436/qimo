package com._202510007517.major_assignment.entity.dto;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private long offset;
    private int numberOfElements;
    private boolean empty;
}
