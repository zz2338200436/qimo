package com._202510007517.platform.exam.repository;

import java.io.Serializable;
import java.util.Objects;

public class ExamClassId implements Serializable {

    private Long examId;
    private Long classId;

    public ExamClassId() {
    }

    public ExamClassId(Long examId, Long classId) {
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

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ExamClassId that)) {
            return false;
        }
        return Objects.equals(examId, that.examId)
                && Objects.equals(classId, that.classId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(examId, classId);
    }
}
