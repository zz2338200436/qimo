package com._202510007517.major_assignment.arch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosisReportStructureTest {

    private static final Pattern ISSUE_HEADING = Pattern.compile("(?m)^####\\s+(D-\\d+)\\b.*$");
    private static final Pattern STAGE1_REQUIRED_PATTERN = Pattern.compile("阶段 1 必修问题[^\\r\\n]*");

    @Test
    void diagnosis_report_entries_must_include_all_required_fields() throws IOException {
        String markdown = Files.readString(repoRoot().resolve("docs/diagnosis-report.md"));
        List<String> failures = new ArrayList<>();

        List<IssueSection> issues = parseIssues(markdown);
        assertFalse(issues.isEmpty(), "Diagnosis report must contain issue sections");

        for (IssueSection issue : issues) {
            assertContains(issue, "- **编号**：", failures);
            assertContains(issue, "- **描述**：", failures);
            assertContains(issue, "- **证据**：", failures);
            assertContains(issue, "- **影响等级**：", failures);
            assertContains(issue, "- **建议阶段**：", failures);
            assertContains(issue, "- **验证手段**：", failures);
        }

        assertTrue(failures.isEmpty(), () -> "Diagnosis report field gaps: " + failures);
    }

    @Test
    void diagnosis_report_high_impact_count_must_cover_stage1_required_issues() throws IOException {
        String diagnosis = Files.readString(repoRoot().resolve("docs/diagnosis-report.md"));
        String migrationPlan = Files.readString(repoRoot().resolve("docs/migration-plan.md"));

        long highImpactCount = parseIssues(diagnosis).stream()
                .filter(issue -> issue.body.contains("- **影响等级**：高"))
                .count();

        Matcher matcher = STAGE1_REQUIRED_PATTERN.matcher(migrationPlan);
        assertTrue(matcher.find(), "migration-plan.md must declare 阶段 1 必修问题");

        long requiredCount = Pattern.compile("D-\\d+")
                .matcher(matcher.group())
                .results()
                .count();

        assertTrue(requiredCount > 0, "阶段 1 必修问题列表不能为空");
        assertTrue(highImpactCount >= requiredCount,
                () -> "High impact issue count must cover stage-1 required issues: highImpact="
                        + highImpactCount + ", required=" + requiredCount);
    }

    private static List<IssueSection> parseIssues(String markdown) {
        List<IssueSection> issues = new ArrayList<>();
        Matcher matcher = ISSUE_HEADING.matcher(markdown);
        List<Integer> starts = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        while (matcher.find()) {
            ids.add(matcher.group(1));
            starts.add(matcher.start());
        }
        for (int i = 0; i < ids.size(); i++) {
            int start = starts.get(i);
            int end = (i + 1 < starts.size()) ? starts.get(i + 1) : markdown.length();
            issues.add(new IssueSection(ids.get(i), markdown.substring(start, end)));
        }
        return issues;
    }

    private static void assertContains(IssueSection issue, String token, List<String> failures) {
        if (!issue.body.contains(token)) {
            failures.add(issue.id + " missing " + token);
        }
    }

    private record IssueSection(String id, String body) {
    }

    private static Path repoRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("docs/diagnosis-report.md"))
                    && Files.isRegularFile(current.resolve("docs/migration-plan.md"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate repository root from " + System.getProperty("user.dir"));
    }
}
