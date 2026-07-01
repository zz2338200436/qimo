package com._202510007517.major_assignment.service;

import com._202510007517.major_assignment.entity.dto.ScoreTrendDTO;
import com._202510007517.major_assignment.entity.dto.StudentLearningSummaryDTO;
import com._202510007517.major_assignment.entity.dto.TeacherDashboardDTO;
import java.util.List;

public interface TeacherDashboardService {
    TeacherDashboardDTO getDashboardData(Long teacherId, Long classId, Long courseId, String timeRange);
    StudentLearningSummaryDTO getStudentLearningSummary(Long teacherId, Long classId, Long courseId, String timeRange);
    List<ScoreTrendDTO> getScoreTrend(Long teacherId, Long classId, Long courseId, Long studentId, String timeRange);
}
