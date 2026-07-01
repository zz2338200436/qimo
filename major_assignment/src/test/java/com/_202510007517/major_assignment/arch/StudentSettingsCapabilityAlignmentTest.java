package com._202510007517.major_assignment.arch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentSettingsCapabilityAlignmentTest {

    @Test
    void student_settings_pages_should_keep_connected_capabilities_available_in_jwt_mode() throws IOException {
        assertStudentSettingsPage("major_assignment/src/main/resources/static/student-settings.html");
        assertStudentSettingsPage("frontend/dist/student-settings.html");
    }

    private static void assertStudentSettingsPage(String relativePath) throws IOException {
        String content = Files.readString(repoRoot().resolve(relativePath)).replace("\r\n", "\n");

        assertAll(
                () -> assertTrue(content.contains("const response = await studentAPI.getStudentProfile();"),
                        relativePath + " should load student profile through the connected Gateway route"),
                () -> assertTrue(content.contains("const response = await fetch(`${API_BASE_URL}/api/student/export-data`"),
                        relativePath + " should export through /api/student/export-data"),
                () -> assertNotContains(content, relativePath,
                        "return buildUnsupportedResult('当前 JWT 微服务环境暂未接通学生资料接口，已展示本地会话信息。');"),
                () -> assertNotContains(content, relativePath,
                        "showMessage('当前 JWT 微服务环境暂未接通学生资料保存接口。', 'info');"),
                () -> assertNotContains(content, relativePath,
                        "showMessage('当前 JWT 微服务环境暂未接通学生数据导出接口。', 'info');"),
                () -> assertTrue(content.contains("当前环境下通知偏好以本浏览器保存为准。"),
                        relativePath + " should clearly mark notification preferences as browser-local"),
                () -> assertTrue(content.contains("当前环境下隐私设置以本浏览器保存为准。"),
                        relativePath + " should clearly mark privacy preferences as browser-local")
        );
    }

    private static void assertNotContains(String content, String relativePath, String snippet) {
        assertTrue(!content.contains(snippet),
                () -> relativePath + " should not disable connected student settings capability with snippet: " + snippet);
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
