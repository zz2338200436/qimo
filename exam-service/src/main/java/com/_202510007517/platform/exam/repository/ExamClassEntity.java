package com._202510007517.platform.exam.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@IdClass(ExamClassId.class)
@Table(name = "exam_classes")
public class ExamClassEntity {

    @Id
    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Id
    @Column(name = "class_id", nullable = false)
    private Long classId;

    public ExamClassEntity() {
    }

    public ExamClassEntity(Long examId, Long classId) {
        this.examId = examId;
        this.classId = classId;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }
}
