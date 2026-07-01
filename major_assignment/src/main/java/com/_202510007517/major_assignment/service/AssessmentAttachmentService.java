package com._202510007517.major_assignment.service;

import com._202510007517.major_assignment.entity.AssessmentAttachment;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface AssessmentAttachmentService {
    String ASSIGNMENT_TYPE = "assignment";
    String EXAM_TYPE = "exam";

    List<AssessmentAttachment> saveAttachments(String assessmentType,
                                               Long assessmentId,
                                               Long uploadedBy,
                                               MultipartFile[] files) throws IOException;

    List<Map<String, Object>> getAttachmentDtos(String assessmentType, Long assessmentId);

    AssessmentAttachment getAttachment(Long attachmentId);

    Resource loadAsResource(AssessmentAttachment attachment) throws IOException;

    void deleteAttachments(String assessmentType, Long assessmentId);
}
