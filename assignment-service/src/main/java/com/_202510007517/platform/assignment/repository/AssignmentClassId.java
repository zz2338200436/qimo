package com._202510007517.platform.assignment.repository;

import java.io.Serializable;
import java.util.Objects;

public class AssignmentClassId implements Serializable {

    private Long assignmentId;
    private Long classId;

    public AssignmentClassId() {
    }

    public AssignmentClassId(Long assignmentId, Long classId) {
        this.assignmentId = assignmentId;
        this.classId = classId;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
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
        if (!(other instanceof AssignmentClassId that)) {
            return false;
        }
        return Objects.equals(assignmentId, that.assignmentId)
                && Objects.equals(classId, that.classId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignmentId, classId);
    }
}
