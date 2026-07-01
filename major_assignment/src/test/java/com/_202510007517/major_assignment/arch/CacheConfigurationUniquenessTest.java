package com._202510007517.major_assignment.arch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheConfigurationUniquenessTest {

    private static final Pattern CACHEABLE_PATTERN = Pattern.compile(
            "@Cacheable\\(value\\s*=\\s*([^,]+),\\s*key\\s*=\\s*([^,\\)]+)");

    @Test
    void cacheable_value_and_key_pairs_must_be_unique() throws IOException {
        Path sourceRoot = repoRoot().resolve("major_assignment/src/main/java");
        Map<String, String> firstSeenAt = new LinkedHashMap<>();
        List<String> duplicates = new ArrayList<>();
        int cacheableCount = 0;

        try (var paths = Files.walk(sourceRoot)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(path);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i).trim();
                    if (!line.startsWith("@Cacheable(")) {
                        continue;
                    }
                    Matcher matcher = CACHEABLE_PATTERN.matcher(line);
                    int lineNumber = i + 1;
                    assertTrue(matcher.find(),
                            () -> "Unable to parse @Cacheable annotation at " + path + ":" + lineNumber);

                    String cacheName = normalize(matcher.group(1));
                    String key = normalize(matcher.group(2));
                    String pair = cacheName + " :: " + key;
                    String location = repoRoot().relativize(path) + ":" + (i + 1);
                    String firstLocation = firstSeenAt.putIfAbsent(pair, location);
                    if (firstLocation != null) {
                        duplicates.add(pair + " duplicated at " + firstLocation + " and " + location);
                    }
                    cacheableCount++;
                }
            }
        }

        assertTrue(cacheableCount > 0, "Expected at least one @Cacheable annotation");
        assertFalse(duplicates.isEmpty() && cacheableCount == 0);
        assertTrue(duplicates.isEmpty(), () -> "Duplicate @Cacheable (cacheName,key) pairs: " + duplicates);
    }

    private static String normalize(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }

    private static Path repoRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("major_assignment/src/main/java"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate repository root from " + System.getProperty("user.dir"));
    }
}
