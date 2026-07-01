package com._202510007517.platform.agent.tool;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

final class AgentAttachmentMultipartSupport {

    private AgentAttachmentMultipartSupport() {
    }

    static MultipartFile[] toMultipartFiles(Object rawAttachments) {
        if (!(rawAttachments instanceof List<?> attachments) || attachments.isEmpty()) {
            return new MultipartFile[0];
        }
        List<MultipartFile> files = new ArrayList<>();
        for (Object rawAttachment : attachments) {
            if (!(rawAttachment instanceof Map<?, ?> attachment)) {
                continue;
            }
            String name = asString(attachment.get("name"));
            String contentType = asString(attachment.get("contentType"));
            String base64 = asString(attachment.get("base64"));
            if (name == null || name.isBlank() || base64 == null || base64.isBlank()) {
                continue;
            }
            byte[] bytes = Base64.getDecoder().decode(base64);
            files.add(new InMemoryMultipartFile(name, contentType, bytes));
        }
        return files.toArray(MultipartFile[]::new);
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private record InMemoryMultipartFile(String originalFilename, String contentType, byte[] bytes)
            implements MultipartFile {

        @Override
        public String getName() {
            return "files";
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return bytes == null || bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes == null ? 0L : bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes == null ? new byte[0] : bytes.clone();
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(getBytes());
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            java.nio.file.Files.write(dest.toPath(), getBytes());
        }
    }
}
