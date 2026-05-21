package com._202510007517.platform.analysis.web.dto;

public record WarningStatsDTO(
        long totalWarnings,
        long pendingWarnings,
        long processingWarnings,
        long resolvedWarnings,
        long scoreWarningCount,
        long attendanceWarningCount,
        long homeworkWarningCount,
        long progressWarningCount) {
}
