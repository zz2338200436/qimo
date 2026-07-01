package com._202510007517.platform.assignment.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@IdClass(AssignmentKnowledgePointId.class)
@Table(name = "assignment_knowledge_points")
public class AssignmentKnowledgePointEntity {

    @Id
    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Id
    @Column(name = "knowledge_point_id", nullable = false)
    private Long knowledgePointId;

    public AssignmentKnowledgePointEntity() {
    }

    public AssignmentKnowledgePointEntity(Long assignmentId, Long knowledgePointId) {
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
}
