package com._202510007517.major_assignment.service;

import com._202510007517.major_assignment.entity.EarlyWarning;
import com._202510007517.major_assignment.entity.dto.WarningStatsDTO;
import com._202510007517.major_assignment.entity.dto.PageResult;
import com._202510007517.major_assignment.entity.dto.WarningStatusUpdateDTO;

import java.util.List;

public interface EarlyWarningService {
    List<EarlyWarning> getUnresolvedWarnings(Long teacherId);
    
    WarningStatsDTO getWarningStats(Long teacherId, Long classId, Long courseId);
    
    PageResult<EarlyWarning> getWarningList(Long teacherId, Long classId, Long courseId, 
                                          String warningType, String status, 
                                          Integer page, Integer size);
    
    EarlyWarning getWarningDetail(Long warningId);
    
    List<EarlyWarning> getByCourseId(Long courseId, String warningType, String warningLevel, Boolean isResolved);
    
    boolean updateWarningStatus(Long warningId, WarningStatusUpdateDTO updateDTO, Long userId);
    
    List<EarlyWarning> getWarningsForExport(Long teacherId, Long classId, Long courseId, 
                                          String warningType, String status);
    
    boolean addEarlyWarning(EarlyWarning earlyWarning);
    
    boolean deleteEarlyWarning(Long id);
}