package com._202510007517.platform.assignment.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@IdClass(AssignmentClassId.class)
@Table(name = "assignment_classes")
public class AssignmentClassEntity {

    @Id
    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Id
    @Column(name = "class_id", nullable = false)
    private Long classId;

    public AssignmentClassEntity() {
    }

    public AssignmentClassEntity(Long assignmentId, Long classId) {
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
}
