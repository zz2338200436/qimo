package com._202510007517.platform.agent.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RagMarkdownChunker {
    private static final Pattern SECTION_HEADING = Pattern.compile("(?m)^#{2,3}\\s+(.+)$");

    public List<RagChunk> chunk(RagDocument document) {
        String content = document.content() == null ? "" : document.content().trim();
        if (content.isBlank()) {
            return List.of();
        }
        List<Section> sections = sections(document.title(), content);
        List<RagChunk> chunks = new ArrayList<>();
        Map<String, String> metadata = document.metadata() == null ? Map.of() : document.metadata();
        int order = 1;
        for (Section section : sections) {
            String text = section.content().trim();
            if (text.isBlank()) {
                continue;
            }
            chunks.add(new RagChunk(
                    document.documentId() + "#" + order,
                    document.documentId(),
                    document.title(),
                    section.title(),
                    document.sourcePath(),
                    valueOrDefault(metadata.get("roleScope"), "all"),
                    metadata.get("courseId"),
                    text,
                    order
            ));
            order++;
        }
        return chunks;
    }

    private List<Section> sections(String documentTitle, String content) {
        Matcher matcher = SECTION_HEADING.matcher(content);
        List<SectionBoundary> boundaries = new ArrayList<>();
        while (matcher.find()) {
            boundaries.add(new SectionBoundary(matcher.start(), matcher.end(), matcher.group(1).trim()));
        }
        if (boundaries.isEmpty()) {
            return List.of(new Section(documentTitle, stripTopTitle(content)));
        }
        List<Section> sections = new ArrayList<>();
        String intro = stripTopTitle(content.substring(0, boundaries.get(0).start())).trim();
        if (!intro.isBlank()) {
            sections.add(new Section(documentTitle, intro));
        }
        for (int i = 0; i < boundaries.size(); i++) {
            SectionBoundary current = boundaries.get(i);
            int end = i + 1 < boundaries.size() ? boundaries.get(i + 1).start() : content.length();
            sections.add(new Section(current.title(), content.substring(current.end(), end)));
        }
        return sections;
    }

    private String stripTopTitle(String content) {
        return content.replaceFirst("(?s)^#\\s+.+?(\\R\\R|\\R)", "").trim();
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private record SectionBoundary(int start, int end, String title) {
    }

    private record Section(String title, String content) {
    }
}
