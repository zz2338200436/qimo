package com._202510007517.major_assignment.arch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentFrontendApiCapabilityAlignmentTest {

    @Test
    void static_student_api_should_guard_unavailable_microservice_capabilities() throws IOException {
        assertGuardedStudentApi("major_assignment/src/main/resources/static/api.js");
        assertGuardedStudentApi("frontend/dist/api.js");
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

    private static void assertGuardedStudentApi(String relativePath) throws IOException {
        String apiScript = Files.readString(repoRoot().resolve(relativePath))
                .replace("\r\n", "\n");

        assertAll(
                () -> assertTrue(apiScript.contains("const STUDENT_MICROSERVICE_CAPABILITIES = Object.freeze({"),
                        relativePath + " must declare student capability matrix"),
                () -> assertTrue(apiScript.contains("activities: false"),
                        relativePath + " should mark student activities unsupported in JWT-only mode"),
                () -> assertTrue(apiScript.contains("dashboardPerformance: false"),
                        relativePath + " should mark student dashboard performance unsupported in JWT-only mode"),
                () -> assertTrue(apiScript.contains("function guardStudentCapability("),
                        relativePath + " must expose guardStudentCapability helper"),
                () -> assertContainsSnippet(apiScript, relativePath,
                        "getRecentActivities() {\n        return guardStudentCapability(\n            'activities',"),
                () -> assertContainsSnippet(apiScript, relativePath,
                        "getCurrentStudentPerformance() {\n        return guardStudentCapability(\n            'dashboardPerformance',"),
                () -> assertContainsSnippet(apiScript, relativePath,
                        "getExamSubmissions() {\n        return guardStudentCapability(\n            'examSubmit',"),
                () -> assertContainsSnippet(apiScript, relativePath,
                        "submitExam(examId, formData) {\n        return guardStudentCapability(\n            'examSubmit',"),
                () -> assertContainsSnippet(apiScript, relativePath,
                        "submitExamJson(examId, data) {\n        return guardStudentCapability(\n            'examSubmit',"),
                () -> assertContainsSnippet(apiScript, relativePath,
                        "getExams(params = {}) {\n        return guardStudentCapability(\n            'exams',"),
                () -> assertContainsSnippet(apiScript, relativePath,
                        "getExamDetail(examId) {\n        return guardStudentCapability(\n            'examDetail',"),
                () -> assertContainsSnippet(apiScript, relativePath,
                        "getScores() {\n        return guardStudentCapability(\n            'scores',"),
                () -> assertContainsSnippet(apiScript, relativePath,
                        "getCourseProgress() {\n        return guardStudentCapability(\n            'dashboardPerformance',")
        );
    }

    private static void assertContainsSnippet(String apiScript, String relativePath, String snippet) {
        assertTrue(apiScript.contains(snippet),
                () -> relativePath + " is missing guarded snippet: " + snippet);
    }
}
