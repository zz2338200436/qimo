package com._202510007517.platform.assignment.service;

import com._202510007517.platform.assignment.domain.AssignmentRecord;
import com._202510007517.platform.assignment.repository.AssignmentRepository;
import com._202510007517.platform.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class AssignmentKnowledgePointService {

    private final AssignmentRepository assignmentRepository;

    public AssignmentKnowledgePointService(AssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    public List<Map<String, Object>> listKnowledgePoints(Long teacherId, Long assignmentId) {
        requireTeacherAssignment(teacherId, assignmentId);
        return assignmentRepository.findKnowledgePointIdsByAssignmentId(assignmentId)
                .stream()
                .map(AssignmentKnowledgePointService::toLegacyKnowledgePoint)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void replaceKnowledgePoints(Long teacherId, Long assignmentId, List<Long> knowledgePointIds) {
        requireTeacherAssignment(teacherId, assignmentId);
        List<Long> distinctIds = knowledgePointIds == null
                ? List.of()
                : new LinkedHashSet<>(knowledgePointIds).stream().toList();
        assignmentRepository.replaceAssignmentKnowledgePoints(assignmentId, distinctIds);
    }

    private AssignmentRecord requireTeacherAssignment(Long teacherId, Long assignmentId) {
        AssignmentRecord assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("作业不存在"));
        if (teacherId == null || !teacherId.equals(assignment.getTeacherId())) {
            throw new ResourceNotFoundException("作业不存在");
        }
        return assignment;
    }

    private static Map<String, Object> toLegacyKnowledgePoint(Long knowledgePointId) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", knowledgePointId);
        item.put("knowledgePointId", knowledgePointId);
        item.put("pointName", "知识点 " + knowledgePointId);
        item.put("name", "知识点 " + knowledgePointId);
        return item;
    }
}
