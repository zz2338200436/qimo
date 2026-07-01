package com._202510007517.platform.assignment.service;

import com._202510007517.platform.assignment.repository.AssessmentAttachmentEntity;
import com._202510007517.platform.assignment.repository.AssessmentAttachmentJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AssessmentAttachmentServiceImpl implements AssessmentAttachmentService {

    static final String ASSIGNMENT_TYPE = "assignment";
    static final String ASSIGNMENT_SUBMISSION_TYPE = "assignment_submission";

    private final AssessmentAttachmentJpaRepository attachmentRepository;
    private final Path storageRoot;

    public AssessmentAttachmentServiceImpl(AssessmentAttachmentJpaRepository attachmentRepository,
                                           @Value("${app.upload.root:uploads}") String storageRoot) {
        this.attachmentRepository = attachmentRepository;
        this.storageRoot = Paths.get(storageRoot).toAbsolutePath().normalize();
    }

    @Override
    @Transactional
    public List<AssessmentAttachmentEntity> saveAttachments(Long assignmentId,
                                                            Long uploadedBy,
                                                            MultipartFile[] files) throws IOException {
        return saveTypedAttachments(ASSIGNMENT_TYPE, assignmentId, uploadedBy, files);
    }

    @Override
    @Transactional
    public List<AssessmentAttachmentEntity> saveSubmissionAttachments(Long submissionId,
                                                                      Long uploadedBy,
                                                                      MultipartFile[] files) throws IOException {
        return saveTypedAttachments(ASSIGNMENT_SUBMISSION_TYPE, submissionId, uploadedBy, files);
    }

    private List<AssessmentAttachmentEntity> saveTypedAttachments(String assessmentType,
                                                                  Long assessmentId,
                                                                  Long uploadedBy,
                                                                  MultipartFile[] files) throws IOException {
        List<AssessmentAttachmentEntity> saved = new ArrayList<>();
        if (assessmentId == null || files == null || files.length == 0) {
            return saved;
        }

        Path assignmentDirectory = storageRoot.resolve(assessmentType).resolve(String.valueOf(assessmentId)).normalize();
        Files.createDirectories(assignmentDirectory);

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            String originalFilename = sanitizeFilename(file.getOriginalFilename());
            String storedFilename = UUID.randomUUID() + extensionOf(originalFilename);
            Path destination = assignmentDirectory.resolve(storedFilename).normalize();
            if (!destination.startsWith(storageRoot)) {
                throw new IOException("非法文件路径");
            }

            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            AssessmentAttachmentEntity attachment = new AssessmentAttachmentEntity();
            attachment.setAssessmentType(assessmentType);
            attachment.setAssessmentId(assessmentId);
            attachment.setOriginalFilename(originalFilename);
            attachment.setStoredFilename(storedFilename);
            attachment.setRelativePath(storageRoot.relativize(destination).toString().replace('\\', '/'));
            attachment.setContentType(file.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setUploadedBy(uploadedBy);
            saved.add(attachmentRepository.save(attachment));
        }
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAttachmentDtos(Long assignmentId) {
        if (assignmentId == null) {
            return List.of();
        }
        return attachmentRepository.findByAssessmentTypeAndAssessmentIdOrderByIdAsc(ASSIGNMENT_TYPE, assignmentId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSubmissionAttachmentDtos(Long submissionId) {
        if (submissionId == null) {
            return List.of();
        }
        return attachmentRepository.findByAssessmentTypeAndAssessmentIdOrderByIdAsc(ASSIGNMENT_SUBMISSION_TYPE, submissionId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AssessmentAttachmentEntity getAttachment(Long attachmentId) {
        if (attachmentId == null) {
            return null;
        }
        return attachmentRepository.findById(attachmentId).orElse(null);
    }

    @Override
    public Resource loadAsResource(AssessmentAttachmentEntity attachment) throws IOException {
        if (attachment == null || attachment.getRelativePath() == null) {
            throw new IOException("附件不存在");
        }
        Path file = storageRoot.resolve(attachment.getRelativePath()).normalize();
        if (!file.startsWith(storageRoot) || !Files.exists(file) || !Files.isRegularFile(file)) {
            throw new IOException("附件文件不存在");
        }
        return new PathResource(file);
    }

    @Override
    @Transactional
    public void deleteAttachments(Long assignmentId) {
        if (assignmentId == null) {
            return;
        }
        List<AssessmentAttachmentEntity> attachments =
                attachmentRepository.findByAssessmentTypeAndAssessmentIdOrderByIdAsc(ASSIGNMENT_TYPE, assignmentId);
        for (AssessmentAttachmentEntity attachment : attachments) {
            deleteFileQuietly(attachment);
        }
        attachmentRepository.deleteByAssessmentTypeAndAssessmentId(ASSIGNMENT_TYPE, assignmentId);
    }

    private Map<String, Object> toDto(AssessmentAttachmentEntity attachment) {
        return Map.of(
                "id", attachment.getId(),
                "name", attachment.getOriginalFilename(),
                "originalFilename", attachment.getOriginalFilename(),
                "contentType", attachment.getContentType() == null ? "" : attachment.getContentType(),
                "size", attachment.getFileSize() == null ? 0L : attachment.getFileSize(),
                "downloadUrl", "/api/attachments/assignment/" + attachment.getId() + "/download"
        );
    }

    private void deleteFileQuietly(AssessmentAttachmentEntity attachment) {
        if (attachment == null || attachment.getRelativePath() == null) {
            return;
        }
        Path file = storageRoot.resolve(attachment.getRelativePath()).normalize();
        if (!file.startsWith(storageRoot)) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // Database cleanup should still proceed; stale files can be cleared manually from uploads/.
        }
    }

    private String sanitizeFilename(String filename) {
        String value = filename == null || filename.isBlank() ? "attachment" : filename;
        return Paths.get(value).getFileName().toString().replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String extensionOf(String filename) {
        int index = filename.lastIndexOf('.');
        if (index <= 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index);
    }
}
