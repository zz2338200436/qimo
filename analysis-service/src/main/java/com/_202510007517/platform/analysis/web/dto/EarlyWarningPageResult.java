package com._202510007517.platform.analysis.web.dto;

import java.util.List;

public record EarlyWarningPageResult(
        List<EarlyWarningDTO> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        long offset,
        int numberOfElements,
        boolean empty) {
}
