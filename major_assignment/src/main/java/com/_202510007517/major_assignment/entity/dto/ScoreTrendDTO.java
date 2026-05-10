package com._202510007517.major_assignment.entity.dto;

import lombok.Data;

/**
 * 单个时间点的成绩趋势数据
 */
@Data
public class ScoreTrendDTO {
    /** 日期（yyyy-MM-dd） */
    private String date;

    /** 该日期的平均成绩 */
    private Double averageScore;
}

