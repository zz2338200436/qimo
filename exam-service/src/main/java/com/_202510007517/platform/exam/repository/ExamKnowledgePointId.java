package com._202510007517.platform.exam.repository;

import java.io.Serializable;
import java.util.Objects;

public class ExamKnowledgePointId implements Serializable {

    private Long examId;
    private Long knowledgePointId;

    public ExamKnowledgePointId() {
    }

    public ExamKnowledgePointId(Long examId, Long knowledgePointId) {
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

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ExamKnowledgePointId that)) {
            return false;
        }
        return Objects.equals(examId, that.examId)
                && Objects.equals(knowledgePointId, that.knowledgePointId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(examId, knowledgePointId);
    }
}
