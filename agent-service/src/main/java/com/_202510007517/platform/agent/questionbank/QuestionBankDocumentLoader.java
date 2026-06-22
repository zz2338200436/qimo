package com._202510007517.platform.agent.questionbank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class QuestionBankDocumentLoader {
    private static final Logger log = LoggerFactory.getLogger(QuestionBankDocumentLoader.class);
    private static final Pattern FIRST_HEADING = Pattern.compile("(?m)^#\\s+(.+)$");

    public List<QuestionBankDocument> load(List<String> configuredPaths) {
        List<QuestionBankDocument> documents = new ArrayList<>();
        for (String configuredPath : configuredPaths == null ? List.<String>of() : configuredPaths) {
            if (configuredPath == null || configuredPath.isBlank()) {
                continue;
            }
            Path path;
            try {
                path = Path.of(configuredPath).toAbsolutePath().normalize();
            } catch (InvalidPathException ex) {
                log.info("question bank path is invalid: {}", configuredPath);
                continue;
            }
            if (!Files.exists(path)) {
                log.info("question bank path does not exist: {}", path);
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

    private List<QuestionBankDocument> loadDirectory(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                    .filter(this::isMarkdown)
                    .sorted(Comparator.comparing(Path::toString))
                    .map(this::loadFile)
                    .flatMap(Optional::stream)
                    .toList();
        } catch (IOException ex) {
            log.info("failed to load question bank directory: {}", directory);
            return List.of();
        }
    }

    private Optional<QuestionBankDocument> loadFile(Path file) {
        try {
            String raw = Files.readString(file);
            ParsedMarkdown parsed = parseFrontMatter(raw);
            String source = file.toAbsolutePath().normalize().toString();
            return Optional.of(new QuestionBankDocument(
                    source,
                    title(parsed.metadata(), parsed.content(), file),
                    source,
                    parsed.metadata(),
                    parsed.content()
            ));
        } catch (IOException ex) {
            log.info("failed to load question bank markdown file: {}", file);
            return Optional.empty();
        }
    }

    private ParsedMarkdown parseFrontMatter(String raw) {
        if (raw == null || !raw.startsWith("---")) {
            return new ParsedMarkdown(Map.of(), raw == null ? "" : raw);
        }
        String normalized = raw.replace("\r\n", "\n");
        int end = frontMatterEnd(normalized);
        if (end < 0) {
            return new ParsedMarkdown(Map.of(), raw);
        }
        String frontMatter = normalized.substring(3, end).trim();
        String content = normalized.substring(end + 4).trim();
        return new ParsedMarkdown(parseMetadata(frontMatter), content);
    }

    private Map<String, String> parseMetadata(String frontMatter) {
        Map<String, String> metadata = new LinkedHashMap<>();
        String listKey = null;
        List<String> listValues = new ArrayList<>();
        for (String line : frontMatter.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            if (trimmed.startsWith("- ")) {
                if (listKey != null) {
                    listValues.add(trimmed.substring(2).trim());
                }
                continue;
            }
            if (listKey != null) {
                metadata.put(listKey, String.join(",", listValues));
                listKey = null;
                listValues = new ArrayList<>();
            }
            int separator = line.indexOf(':');
            if (separator <= 0) {
                continue;
            }
            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            if (value.isBlank()) {
                listKey = key;
            } else {
                metadata.put(key, value);
            }
        }
        if (listKey != null) {
            metadata.put(listKey, String.join(",", listValues));
        }
        return metadata;
    }

    private int frontMatterEnd(String normalized) {
        int searchFrom = 3;
        while (searchFrom < normalized.length()) {
            int lineStart = normalized.indexOf('\n', searchFrom);
            if (lineStart < 0) {
                return -1;
            }
            int delimiterStart = lineStart + 1;
            int lineEnd = normalized.indexOf('\n', delimiterStart);
            String line = lineEnd < 0 ? normalized.substring(delimiterStart) : normalized.substring(delimiterStart, lineEnd);
            if ("---".equals(line)) {
                return lineStart;
            }
            if (line.isBlank()) {
                return -1;
            }
            searchFrom = delimiterStart;
        }
        return -1;
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
        return fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".md")
                ? fileName.substring(0, fileName.length() - 3)
                : fileName;
    }

    private boolean isMarkdown(Path path) {
        return path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".md");
    }

    private record ParsedMarkdown(Map<String, String> metadata, String content) {
    }
}
