package com._202510007517.platform.exam.controller;

import com._202510007517.platform.exam.repository.AssessmentAttachmentEntity;
import com._202510007517.platform.exam.service.AssessmentAttachmentService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/attachments/exam")
public class ExamAttachmentController {

    private final AssessmentAttachmentService attachmentService;

    public ExamAttachmentController(AssessmentAttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long attachmentId) throws IOException {
        AssessmentAttachmentEntity attachment = attachmentService.getAttachment(attachmentId);
        Resource resource = attachmentService.loadAsResource(attachment);
        String contentType = attachment.getContentType() == null || attachment.getContentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : attachment.getContentType();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(attachment.getOriginalFilename(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(resource);
    }
}
