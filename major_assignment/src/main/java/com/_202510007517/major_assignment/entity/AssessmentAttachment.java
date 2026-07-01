package com._202510007517.major_assignment.entity;

import lombok.Data;

import java.util.Date;

@Data
public class AssessmentAttachment {
    private Long id;
    private String assessmentType;
    private Long assessmentId;
    private String originalFilename;
    private String storedFilename;
    private String relativePath;
    private String contentType;
    private Long fileSize;
    private Long uploadedBy;
    private Date createdAt;
}
