package com._202510007517.platform.exam.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@IdClass(ExamKnowledgePointId.class)
@Table(name = "exam_knowledge_points")
public class ExamKnowledgePointEntity {

    @Id
    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Id
    @Column(name = "knowledge_point_id", nullable = false)
    private Long knowledgePointId;

    public ExamKnowledgePointEntity() {
    }

    public ExamKnowledgePointEntity(Long examId, Long knowledgePointId) {
        this.examId = examId;
        this.knowledgePointId = knowledgePointId;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public Long getKnowledgePointId() {
        return knowledgePointId;
    }

    public void setKnowledgePointId(Long knowledgePointId) {
        this.knowledgePointId = knowledgePointId;
    }
}
