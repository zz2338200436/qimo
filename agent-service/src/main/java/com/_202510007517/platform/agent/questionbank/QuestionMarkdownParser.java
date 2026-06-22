package com._202510007517.platform.agent.questionbank;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuestionMarkdownParser {
    private static final Pattern QUESTION_HEADING = Pattern.compile("(?m)^## Question\\s*$");

    public List<QuestionChunk> parse(QuestionBankDocument document) {
        Map<String, String> metadata = document.metadata() == null ? Map.of() : document.metadata();
        List<QuestionChunk> questions = new ArrayList<>();
        int order = 1;
        for (String block : questionBlocks(document.content())) {
            if (block.isBlank()) {
                continue;
            }
            Map<String, String> fields = parseScalarFields(block);
            String topic = required("topic", valueOrDefault(fields.get("topic"), metadata.get("topic")), document.sourcePath());
            String difficulty = required("difficulty", fields.get("difficulty"), document.sourcePath());
            String type = required("type", fields.get("type"), document.sourcePath());
            String content = required("content", fields.get("content"), document.sourcePath());
            String answer = required("answer", fields.get("answer"), document.sourcePath());
            String roleScope = valueOrDefault(fields.get("roleScope"), valueOrDefault(metadata.get("roleScope"), "all"));
            String analysis = trimToNull(fields.get("analysis"));
            List<String> tags = parseTags(valueOrDefault(fields.get("tags"), metadata.get("tags")));
            List<String> options = parseOptions(block);
            questions.add(new QuestionChunk(
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
            ));
            order++;
        }
        return questions;
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

    private String required(String fieldName, String value, String sourcePath) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException("question bank field '" + fieldName + "' is required: " + sourcePath);
        }
        return normalized;
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
