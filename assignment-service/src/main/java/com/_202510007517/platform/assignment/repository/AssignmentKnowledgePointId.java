package com._202510007517.platform.assignment.repository;

import java.io.Serializable;
import java.util.Objects;

public class AssignmentKnowledgePointId implements Serializable {

    private Long assignmentId;
    private Long knowledgePointId;

    public AssignmentKnowledgePointId() {
    }

    public AssignmentKnowledgePointId(Long assignmentId, Long knowledgePointId) {
        this.assignmentId = assignmentId;
        this.knowledgePointId = knowledgePointId;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
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
        if (!(other instanceof AssignmentKnowledgePointId that)) {
            return false;
        }
        return Objects.equals(assignmentId, that.assignmentId)
                && Objects.equals(knowledgePointId, that.knowledgePointId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignmentId, knowledgePointId);
    }
}
