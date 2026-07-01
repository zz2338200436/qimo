package com._202510007517.platform.assignment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentKnowledgePointJpaRepository
        extends JpaRepository<AssignmentKnowledgePointEntity, AssignmentKnowledgePointId> {

    List<AssignmentKnowledgePointEntity> findByAssignmentIdOrderByKnowledgePointIdAsc(Long assignmentId);

    void deleteByAssignmentId(Long assignmentId);
}
