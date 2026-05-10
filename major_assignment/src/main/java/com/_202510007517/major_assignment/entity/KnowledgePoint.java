package com._202510007517.major_assignment.entity;

import lombok.Data;

@Data
public class KnowledgePoint {
    private Long id;
    private String pointName;
    private String description;
    private String difficulty;
    private Integer orderIndex;
    private Long courseId;
}