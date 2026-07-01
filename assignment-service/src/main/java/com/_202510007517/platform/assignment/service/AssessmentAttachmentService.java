package com._202510007517.platform.assignment.service;

import com._202510007517.platform.assignment.repository.AssessmentAttachmentEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface AssessmentAttachmentService {

    List<AssessmentAttachmentEntity> saveAttachments(Long assignmentId, Long uploadedBy, MultipartFile[] files) throws IOException;

    List<AssessmentAttachmentEntity> saveSubmissionAttachments(Long submissionId, Long uploadedBy, MultipartFile[] files) throws IOException;

    List<Map<String, Object>> getAttachmentDtos(Long assignmentId);

    List<Map<String, Object>> getSubmissionAttachmentDtos(Long submissionId);

    AssessmentAttachmentEntity getAttachment(Long attachmentId);

    Resource loadAsResource(AssessmentAttachmentEntity attachment) throws IOException;

    void deleteAttachments(Long assignmentId);

    static AssessmentAttachmentService none() {
        return new AssessmentAttachmentService() {
            @Override
            public List<AssessmentAttachmentEntity> saveAttachments(Long assignmentId, Long uploadedBy, MultipartFile[] files) {
                return List.of();
            }

            @Override
            public List<Map<String, Object>> getAttachmentDtos(Long assignmentId) {
                return List.of();
            }

            @Override
            public List<AssessmentAttachmentEntity> saveSubmissionAttachments(Long submissionId, Long uploadedBy, MultipartFile[] files) {
                return List.of();
            }

            @Override
            public List<Map<String, Object>> getSubmissionAttachmentDtos(Long submissionId) {
                return List.of();
            }

            @Override
            public AssessmentAttachmentEntity getAttachment(Long attachmentId) {
                return null;
            }

            @Override
            public Resource loadAsResource(AssessmentAttachmentEntity attachment) throws IOException {
                throw new IOException("附件不存在");
            }

            @Override
            public void deleteAttachments(Long assignmentId) {
            }
        };
    }
}
