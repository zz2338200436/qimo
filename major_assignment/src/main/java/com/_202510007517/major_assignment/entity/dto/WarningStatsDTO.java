package com._202510007517.major_assignment.entity.dto;

import lombok.Data;

@Data
public class WarningStatsDTO {
    private Long totalWarnings;
    private Long pendingWarnings;
    private Long processingWarnings;
    private Long resolvedWarnings;
    private Long scoreWarningCount;
    private Long attendanceWarningCount;
    private Long homeworkWarningCount;
    private Long progressWarningCount;
}
