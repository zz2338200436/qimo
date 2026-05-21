package com._202510007517.major_assignment.arch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTableOwnershipArchTest {

    private static final Pattern USER_TABLE_WRITE = Pattern.compile(
            "(?is)\\b(insert\\s+into|update|delete\\s+from)\\s+(?:[a-zA-Z0-9_]+\\.)?(users|user_roles)\\b");
    private static final Pattern LEGACY_USER_SERVICE_WRITE_CALL = Pattern.compile(
            "\\buserService\\s*\\.\\s*(create|update|delete)\\s*\\(");
    private static final List<String> SOURCE_EXTENSIONS = List.of(
            ".java", ".xml", ".sql", ".yml", ".yaml", ".properties");

    @Test
    void users_and_user_roles_are_written_only_by_user_service() throws IOException {
        Path repoRoot = repoRoot();
        List<String> violations;

        try (Stream<Path> files = productionSourceFiles(repoRoot)) {
            violations = files
                    .flatMap(path -> tableWriteViolations(repoRoot, path).stream())
                    .toList();
        }

        assertTrue(violations.isEmpty(), () -> "users/user_roles write SQL must live in user-service only:%n%s"
                .formatted(String.join(System.lineSeparator(), violations)));
    }

    @Test
    void legacy_monolith_must_not_call_user_service_write_methods() throws IOException {
        Path repoRoot = repoRoot();
        Path monolithSource = repoRoot.resolve("major_assignment/src/main/java");
        List<String> violations;

        try (Stream<Path> files = Files.walk(monolithSource)) {
            violations = files
                    .filter(Files::isRegularFile)
                    .filter(UserTableOwnershipArchTest::isProductionSource)
                    .flatMap(path -> legacyUserServiceWriteCalls(repoRoot, path).stream())
                    .toList();
        }

        assertTrue(violations.isEmpty(), () -> "major_assignment must call User_Service/Auth_Service for user writes:%n%s"
                .formatted(String.join(System.lineSeparator(), violations)));
    }

    private static List<String> tableWriteViolations(Path repoRoot, Path path) {
        String relativePath = repoRoot.relativize(path).toString().replace('\\', '/');
        if (relativePath.startsWith("user-service/src/main/")) {
            return List.of();
        }

        String content;
        try {
            content = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }

        Matcher matcher = USER_TABLE_WRITE.matcher(content);
        return matcher.results()
                .map(match -> "%s:%d contains %s %s".formatted(
                        relativePath,
                        lineNumber(content, match.start()),
                        match.group(1).toUpperCase(Locale.ROOT),
                        match.group(2)))
                .toList();
    }

    private static boolean isProductionSource(Path path) {
        String normalized = path.toString().replace('\\', '/');
        return SOURCE_EXTENSIONS.stream().anyMatch(normalized::endsWith);
    }

    private static List<String> legacyUserServiceWriteCalls(Path repoRoot, Path path) {
        String content;
        try {
            content = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }
        String relativePath = repoRoot.relativize(path).toString().replace('\\', '/');
        Matcher matcher = LEGACY_USER_SERVICE_WRITE_CALL.matcher(content);
        return matcher.results()
                .map(match -> "%s:%d calls userService.%s".formatted(
                        relativePath,
                        lineNumber(content, match.start()),
                        match.group(1)))
                .toList();
    }

    private static Stream<Path> productionSourceFiles(Path repoRoot) {
        try {
            return Files.list(repoRoot)
                    .filter(Files::isDirectory)
                    .map(module -> module.resolve("src/main"))
                    .filter(Files::isDirectory)
                    .flatMap(UserTableOwnershipArchTest::walk)
                    .filter(Files::isRegularFile)
                    .filter(UserTableOwnershipArchTest::isProductionSource);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to list modules under " + repoRoot, e);
        }
    }

    private static Stream<Path> walk(Path root) {
        try {
            return Files.walk(root);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to walk " + root, e);
        }
    }

    private static int lineNumber(String content, int offset) {
        int line = 1;
        for (int i = 0; i < offset; i++) {
            if (content.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private static Path repoRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("major_assignment"))
                    && Files.isDirectory(current.resolve("user-service"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate repository root from " + System.getProperty("user.dir"));
    }
}
