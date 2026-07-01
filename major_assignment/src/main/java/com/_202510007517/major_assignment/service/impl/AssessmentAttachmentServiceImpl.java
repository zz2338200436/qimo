package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.entity.AssessmentAttachment;
import com._202510007517.major_assignment.mapper.AssessmentAttachmentMapper;
import com._202510007517.major_assignment.service.AssessmentAttachmentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AssessmentAttachmentServiceImpl implements AssessmentAttachmentService {

    private final AssessmentAttachmentMapper attachmentMapper;
    private final Path storageRoot;

    public AssessmentAttachmentServiceImpl(
            AssessmentAttachmentMapper attachmentMapper,
            @Value("${app.upload.root:uploads}") String storageRoot) {
        this.attachmentMapper = attachmentMapper;
        this.storageRoot = Paths.get(storageRoot).toAbsolutePath().normalize();
        this.attachmentMapper.ensureTable();
    }

    @Override
    public List<AssessmentAttachment> saveAttachments(String assessmentType,
                                                      Long assessmentId,
                                                      Long uploadedBy,
                                                      MultipartFile[] files) throws IOException {
        List<AssessmentAttachment> saved = new ArrayList<>();
        if (files == null || files.length == 0) {
            return saved;
        }

        String normalizedType = normalizeAssessmentType(assessmentType);
        Path assessmentDirectory = storageRoot.resolve(normalizedType).resolve(String.valueOf(assessmentId)).normalize();
        Files.createDirectories(assessmentDirectory);

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            String originalFilename = sanitizeFilename(file.getOriginalFilename());
            String storedFilename = UUID.randomUUID() + extensionOf(originalFilename);
            Path destination = assessmentDirectory.resolve(storedFilename).normalize();
            if (!destination.startsWith(storageRoot)) {
                throw new IOException("非法文件路径");
            }

            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            AssessmentAttachment attachment = new AssessmentAttachment();
            attachment.setAssessmentType(normalizedType);
            attachment.setAssessmentId(assessmentId);
            attachment.setOriginalFilename(originalFilename);
            attachment.setStoredFilename(storedFilename);
            attachment.setRelativePath(storageRoot.relativize(destination).toString().replace('\\', '/'));
            attachment.setContentType(file.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setUploadedBy(uploadedBy);
            attachmentMapper.insert(attachment);
            saved.add(attachment);
        }
        return saved;
    }

    @Override
    public List<Map<String, Object>> getAttachmentDtos(String assessmentType, Long assessmentId) {
        return attachmentMapper.findByAssessment(normalizeAssessmentType(assessmentType), assessmentId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public AssessmentAttachment getAttachment(Long attachmentId) {
        return attachmentMapper.findById(attachmentId);
    }

    @Override
    public Resource loadAsResource(AssessmentAttachment attachment) throws IOException {
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
    public void deleteAttachments(String assessmentType, Long assessmentId) {
        String normalizedType = normalizeAssessmentType(assessmentType);
        List<AssessmentAttachment> attachments = attachmentMapper.findByAssessment(normalizedType, assessmentId);
        for (AssessmentAttachment attachment : attachments) {
            if (attachment.getRelativePath() == null) {
                continue;
            }
            Path file = storageRoot.resolve(attachment.getRelativePath()).normalize();
            if (file.startsWith(storageRoot)) {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException ignored) {
                    // Record cleanup still proceeds; stale files can be cleared manually from uploads/.
                }
            }
        }
        attachmentMapper.deleteByAssessment(normalizedType, assessmentId);
    }

    private Map<String, Object> toDto(AssessmentAttachment attachment) {
        return Map.of(
                "id", attachment.getId(),
                "name", attachment.getOriginalFilename(),
                "originalFilename", attachment.getOriginalFilename(),
                "contentType", attachment.getContentType() == null ? "" : attachment.getContentType(),
                "size", attachment.getFileSize() == null ? 0L : attachment.getFileSize(),
                "downloadUrl", "/api/attachments/" + attachment.getId() + "/download"
        );
    }

    private String normalizeAssessmentType(String assessmentType) {
        String normalized = assessmentType == null ? "" : assessmentType.toLowerCase(Locale.ROOT).trim();
        if (!ASSIGNMENT_TYPE.equals(normalized) && !EXAM_TYPE.equals(normalized)) {
            throw new IllegalArgumentException("不支持的附件类型: " + assessmentType);
        }
        return normalized;
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
