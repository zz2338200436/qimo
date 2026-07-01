package com._202510007517.major_assignment.arch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentationSkeletonTest {

    private static final Pattern FRONT_MATTER_PATTERN = Pattern.compile(
            "\\A---\\R(.*?)\\R---\\R",
            Pattern.DOTALL);

    private static final Pattern README_LINK_PATTERN = Pattern.compile("\\[[^\\]]+\\]\\(([^)]+)\\)");

    private static final Map<String, String> CORE_DOCS = new LinkedHashMap<>();

    static {
        CORE_DOCS.put("diagnosis-report.md", "现状诊断报告");
        CORE_DOCS.put("architecture.md", "目标架构与组件选型");
        CORE_DOCS.put("auth-design.md", "认证授权方案");
        CORE_DOCS.put("data-ownership.md", "数据所有权矩阵");
        CORE_DOCS.put("migration-plan.md", "迁移计划");
        CORE_DOCS.put("coding-guidelines.md", "编码规范");
        CORE_DOCS.put("observability.md", "可观测性方案");
        CORE_DOCS.put("deployment.md", "部署与运维手册");
    }

    @Test
    void core_documents_must_have_front_matter_and_change_log() throws IOException {
        Path docsDir = repoRoot().resolve("docs");

        for (Map.Entry<String, String> entry : CORE_DOCS.entrySet()) {
            Path document = docsDir.resolve(entry.getKey());
            assertTrue(Files.isRegularFile(document), () -> "Missing document: " + document);

            String markdown = Files.readString(document);
            Matcher matcher = FRONT_MATTER_PATTERN.matcher(markdown);
            assertTrue(matcher.find(), () -> "Document must start with YAML front matter: " + entry.getKey());

            Map<String, String> frontMatter = parseFrontMatter(matcher.group(1));
            assertEquals(entry.getValue(), frontMatter.get("title"),
                    () -> "Front matter title mismatch for " + entry.getKey());
            assertNonBlank(frontMatter.get("version"), entry.getKey(), "version");
            assertNonBlank(frontMatter.get("last_updated"), entry.getKey(), "last_updated");
            assertNonBlank(frontMatter.get("author"), entry.getKey(), "author");

            assertTrue(markdown.contains("# " + entry.getValue()),
                    () -> "Document heading mismatch for " + entry.getKey());
            assertTrue(markdown.contains("## 变更记录"),
                    () -> "Document must contain 变更记录 section: " + entry.getKey());
        }
    }

    @Test
    void readme_must_link_all_core_documents() throws IOException {
        String readme = Files.readString(repoRoot().resolve("docs/README.md"));

        Map<String, Boolean> seen = new LinkedHashMap<>();
        for (String fileName : CORE_DOCS.keySet()) {
            seen.put(fileName, false);
        }

        Matcher matcher = README_LINK_PATTERN.matcher(readme);
        while (matcher.find()) {
            String target = matcher.group(1).replace("./", "");
            if (seen.containsKey(target)) {
                seen.put(target, true);
            }
        }

        seen.forEach((fileName, linked) ->
                assertTrue(linked, () -> "README must link document " + fileName));
    }

    private static Map<String, String> parseFrontMatter(String block) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String line : block.lines().toList()) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int separator = trimmed.indexOf(':');
            assertTrue(separator > 0, () -> "Invalid front matter line: " + line);
            String key = trimmed.substring(0, separator).trim();
            String value = trimmed.substring(separator + 1).trim();
            values.put(key, value);
        }
        return values;
    }

    private static void assertNonBlank(String value, String document, String field) {
        assertFalse(value == null || value.isBlank(),
                () -> "Front matter field `" + field + "` must be non-blank in " + document);
    }

    private static Path repoRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("docs"))
                    && Files.isRegularFile(current.resolve("pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate repository root from " + System.getProperty("user.dir"));
    }
}
