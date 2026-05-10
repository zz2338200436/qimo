package com._202510007517.major_assignment.service;

import com._202510007517.major_assignment.entity.Assignment;

import java.util.List;

import java.util.Map;

public interface AssignmentService {
    // 获取所有作业
    List<Assignment> getAllAssignments();

    // 根据作业ID获取作业详情
    Assignment getAssignmentById(Long id);

    // 根据课程ID获取作业列表
    List<Assignment> getAssignmentsByCourseId(Long courseId);

    // 创建作业
    void create(Assignment assignment);

    // 更新作业
    void update(Assignment assignment);

    // 删除作业
    void delete(Long id);
    
    // 获取作业列表（支持分页、排序和筛选）
    Map<String, Object> getAssignmentsWithPagination(Long studentId, Integer page, Integer size, String sortBy, String order, 
                                                   Long courseId, Boolean submitted, Boolean isActive);
    
    // 获取带有提交信息的作业详情
    Map<String, Object> getAssignmentDetailsWithSubmissions(Long assignmentId);
}
