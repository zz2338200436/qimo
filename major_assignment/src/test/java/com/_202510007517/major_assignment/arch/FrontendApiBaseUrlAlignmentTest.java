package com._202510007517.major_assignment.arch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendApiBaseUrlAlignmentTest {

    @Test
    void critical_frontend_pages_should_not_hardcode_localhost_api_urls() throws IOException {
        assertNoLocalhostApi("major_assignment/src/main/resources/static/api.js");
        assertNoLocalhostApi("major_assignment/src/main/resources/static/teacher-courses.html");
        assertNoLocalhostApi("major_assignment/src/main/resources/static/teacher-warning.html");
        assertNoLocalhostApi("major_assignment/src/main/resources/static/student-settings.html");
        assertNoLocalhostApi("frontend/dist/api.js");
        assertNoLocalhostApi("frontend/dist/student-login.html");
        assertNoLocalhostApi("frontend/dist/teacher-login.html");
        assertNoLocalhostApi("frontend/dist/teacher-student-dashboard.html");
    }

    private static void assertNoLocalhostApi(String relativePath) throws IOException {
        String content = Files.readString(repoRoot().resolve(relativePath)).replace("\r\n", "\n");
        assertFalse(content.contains("http://localhost:8080/api"),
                () -> relativePath + " should not hardcode http://localhost:8080/api");
        assertFalse(content.contains("http://localhost:8080/api/auth/captcha"),
                () -> relativePath + " should not hardcode captcha API host");
    }

    private static Path repoRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("major_assignment"))
                    && Files.isRegularFile(current.resolve("pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate repository root from " + System.getProperty("user.dir"));
    }
}
