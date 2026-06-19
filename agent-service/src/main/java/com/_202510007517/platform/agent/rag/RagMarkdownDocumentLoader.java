package com._202510007517.platform.agent.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class RagMarkdownDocumentLoader {
    private static final Logger log = LoggerFactory.getLogger(RagMarkdownDocumentLoader.class);
    private static final Pattern FIRST_HEADING = Pattern.compile("(?m)^#\\s+(.+)$");

    public List<RagDocument> load(List<String> configuredPaths) {
        List<RagDocument> documents = new ArrayList<>();
        for (String configuredPath : configuredPaths == null ? List.<String>of() : configuredPaths) {
            if (configuredPath == null || configuredPath.isBlank()) {
                continue;
            }
            Path path = Path.of(configuredPath).toAbsolutePath().normalize();
            if (!Files.exists(path)) {
                log.info("rag document path does not exist: {}", path);
                continue;
            }
            if (Files.isDirectory(path)) {
                documents.addAll(loadDirectory(path));
            } else if (isMarkdown(path)) {
                loadFile(path).ifPresent(documents::add);
            }
        }
        return documents;
    }

    private List<RagDocument> loadDirectory(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                    .filter(this::isMarkdown)
                    .sorted(Comparator.comparing(Path::toString))
                    .map(this::loadFile)
                    .flatMap(java.util.Optional::stream)
                    .toList();
        } catch (IOException ex) {
            log.info("failed to load rag directory: {}", directory);
            return List.of();
        }
    }

    private java.util.Optional<RagDocument> loadFile(Path file) {
        try {
            String raw = Files.readString(file);
            ParsedMarkdown parsed = parseFrontMatter(raw);
            String title = title(parsed.metadata(), parsed.content(), file);
            String source = file.toAbsolutePath().normalize().toString();
            return java.util.Optional.of(new RagDocument(source, title, source, parsed.metadata(), parsed.content()));
        } catch (IOException ex) {
            log.info("failed to load rag markdown file: {}", file);
            return java.util.Optional.empty();
        }
    }

    private ParsedMarkdown parseFrontMatter(String raw) {
        if (raw == null || !raw.startsWith("---")) {
            return new ParsedMarkdown(Map.of(), raw == null ? "" : raw);
        }
        String normalized = raw.replace("\r\n", "\n");
        int end = normalized.indexOf("\n---", 3);
        if (end < 0) {
            return new ParsedMarkdown(Map.of(), raw);
        }
        String frontMatter = normalized.substring(3, end).trim();
        String content = normalized.substring(end + 4).trim();
        Map<String, String> metadata = new LinkedHashMap<>();
        for (String line : frontMatter.split("\n")) {
            int separator = line.indexOf(':');
            if (separator > 0) {
                metadata.put(line.substring(0, separator).trim(), line.substring(separator + 1).trim());
            }
        }
        return new ParsedMarkdown(metadata, content);
    }

    private String title(Map<String, String> metadata, String content, Path file) {
        String explicit = metadata.get("title");
        if (explicit != null && !explicit.isBlank()) {
            return explicit;
        }
        Matcher matcher = FIRST_HEADING.matcher(content == null ? "" : content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        String fileName = file.getFileName().toString();
        return fileName.endsWith(".md") ? fileName.substring(0, fileName.length() - 3) : fileName;
    }

    private boolean isMarkdown(Path path) {
        return path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".md");
    }

    private record ParsedMarkdown(Map<String, String> metadata, String content) {
    }
}
