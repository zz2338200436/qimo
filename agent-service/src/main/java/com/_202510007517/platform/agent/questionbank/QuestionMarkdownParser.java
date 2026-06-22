package com._202510007517.platform.agent.questionbank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuestionMarkdownParser {
    private static final Logger log = LoggerFactory.getLogger(QuestionMarkdownParser.class);
    private static final Pattern QUESTION_HEADING = Pattern.compile("(?m)^## Question\\s*$");

    public List<QuestionChunk> parse(QuestionBankDocument document) {
        Map<String, String> metadata = document.metadata() == null ? Map.of() : document.metadata();
        List<QuestionChunk> questions = new ArrayList<>();
        int order = 1;
        for (String block : questionBlocks(document.content())) {
            if (block.isBlank()) {
                continue;
            }
            String context = questionContext(document.sourcePath(), order);
            try {
                questions.add(parseQuestion(document, metadata, block, order, context));
            } catch (IllegalArgumentException ex) {
                log.warn("skipping malformed question block [{}]: {}", context, ex.getMessage());
            }
            order++;
        }
        return questions;
    }

    private QuestionChunk parseQuestion(QuestionBankDocument document,
                                        Map<String, String> metadata,
                                        String block,
                                        int order,
                                        String context) {
        Map<String, String> fields = parseScalarFields(block);
        String topic = required("topic", valueOrDefault(fields.get("topic"), metadata.get("topic")), context);
        String difficulty = required("difficulty", fields.get("difficulty"), context);
        String type = required("type", fields.get("type"), context);
        String content = required("content", fields.get("content"), context);
        String answer = required("answer", fields.get("answer"), context);
        String roleScope = valueOrDefault(fields.get("roleScope"), valueOrDefault(metadata.get("roleScope"), "all"));
        String analysis = trimToNull(fields.get("analysis"));
        List<String> tags = parseTags(valueOrDefault(fields.get("tags"), metadata.get("tags")));
        List<String> options = parseOptions(block);
        return new QuestionChunk(
                document.documentId() + "#" + order,
                document.sourcePath(),
                document.title(),
                topic,
                difficulty,
                type,
                tags,
                content,
                options,
                answer,
                analysis,
                roleScope,
                order
        );
    }

    private List<String> questionBlocks(String rawContent) {
        String content = rawContent == null ? "" : rawContent.replace("\r\n", "\n").trim();
        if (content.isBlank()) {
            return List.of();
        }
        Matcher matcher = QUESTION_HEADING.matcher(content);
        List<Integer> starts = new ArrayList<>();
        while (matcher.find()) {
            starts.add(matcher.start());
        }
        if (starts.isEmpty()) {
            return List.of();
        }
        List<String> blocks = new ArrayList<>();
        for (int i = 0; i < starts.size(); i++) {
            int blockStart = starts.get(i);
            int contentStart = content.indexOf('\n', blockStart);
            if (contentStart < 0) {
                continue;
            }
            int blockEnd = i + 1 < starts.size() ? starts.get(i + 1) : content.length();
            blocks.add(content.substring(contentStart + 1, blockEnd).trim());
        }
        return blocks;
    }

    private Map<String, String> parseScalarFields(String block) {
        Map<String, String> fields = new LinkedHashMap<>();
        boolean inOptions = false;
        for (String line : normalizeLines(block)) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            if ("options:".equals(trimmed)) {
                inOptions = true;
                continue;
            }
            if (inOptions) {
                if (trimmed.startsWith("- ")) {
                    continue;
                }
                inOptions = false;
            }
            int separator = trimmed.indexOf(':');
            if (separator <= 0) {
                continue;
            }
            String key = trimmed.substring(0, separator).trim();
            String value = trimmed.substring(separator + 1).trim();
            fields.put(key, value);
        }
        return fields;
    }

    private List<String> parseOptions(String block) {
        List<String> options = new ArrayList<>();
        boolean inOptions = false;
        for (String line : normalizeLines(block)) {
            String trimmed = line.trim();
            if ("options:".equals(trimmed)) {
                inOptions = true;
                continue;
            }
            if (!inOptions) {
                continue;
            }
            if (trimmed.isBlank()) {
                continue;
            }
            if (trimmed.startsWith("- ")) {
                options.add(trimmed.substring(2).trim());
                continue;
            }
            if (looksLikeField(trimmed)) {
                break;
            }
            break;
        }
        return List.copyOf(options);
    }

    private List<String> parseTags(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Pattern.compile("[,\\n]")
                .splitAsStream(value)
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .toList();
    }

    private String required(String fieldName, String value, String context) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException("question bank field '" + fieldName + "' is required: " + context);
        }
        return normalized;
    }

    private String questionContext(String sourcePath, int order) {
        return (sourcePath == null || sourcePath.isBlank() ? "unknown-source" : sourcePath) + ", question #" + order;
    }

    private boolean looksLikeField(String line) {
        int separator = line.indexOf(':');
        if (separator <= 0) {
            return false;
        }
        String key = line.substring(0, separator).trim();
        return key.matches("[A-Za-z][A-Za-z0-9_-]*");
    }

    private List<String> normalizeLines(String block) {
        return List.of((block == null ? "" : block.replace("\r\n", "\n")).split("\n"));
    }

    private String valueOrDefault(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
