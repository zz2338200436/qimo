package com._202510007517.major_assignment.arch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentationTraceabilityTest {

    private static final Pattern REQUIREMENT_HEADING = Pattern.compile("^### Requirement\\s+(\\d+)：.*$");
    private static final Pattern ACCEPTANCE_CRITERION =
            Pattern.compile("^(\\d+)\\.\\s+(THE|WHEN|IF|WHERE)\\b.*$");
    private static final Pattern PROPERTY_HEADING = Pattern.compile("^### Property\\s+(\\d+)：.*$");

    @Test
    void migration_documents_reference_every_acceptance_criterion_and_property() throws IOException {
        Path repoRoot = repoRoot();
        String requirements = Files.readString(repoRoot.resolve(".kiro/specs/spring-cloud-migration/requirements.md"));
        String designAndPlan = Files.readString(repoRoot.resolve(".kiro/specs/spring-cloud-migration/design.md"))
                + System.lineSeparator()
                + Files.readString(repoRoot.resolve("docs/migration-plan.md"));

        List<String> missing = new ArrayList<>();
        for (String criterionId : acceptanceCriterionIds(requirements)) {
            if (!designAndPlan.contains(criterionId)) {
                missing.add(criterionId);
            }
        }
        for (String propertyId : propertyIds(designAndPlan)) {
            if (!designAndPlan.contains(propertyId)) {
                missing.add(propertyId);
            }
        }

        assertTrue(missing.isEmpty(), () -> "Documentation traceability gaps: " + missing);
    }

    private static List<String> acceptanceCriterionIds(String requirements) {
        List<String> ids = new ArrayList<>();
        int currentRequirement = -1;
        for (String line : requirements.lines().toList()) {
            Matcher heading = REQUIREMENT_HEADING.matcher(line);
            if (heading.matches()) {
                currentRequirement = Integer.parseInt(heading.group(1));
                continue;
            }
            Matcher criterion = ACCEPTANCE_CRITERION.matcher(line);
            if (criterion.matches() && currentRequirement > 0) {
                ids.add("R" + currentRequirement + "." + criterion.group(1));
            }
        }
        return ids;
    }

    private static List<String> propertyIds(String designAndPlan) {
        List<String> ids = new ArrayList<>();
        for (String line : designAndPlan.lines().toList()) {
            Matcher matcher = PROPERTY_HEADING.matcher(line);
            if (matcher.matches()) {
                ids.add("P" + matcher.group(1));
            }
        }
        return ids;
    }

    private static Path repoRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve(".kiro"))
                    && Files.isRegularFile(current.resolve("docs/migration-plan.md"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate repository root from " + System.getProperty("user.dir"));
    }
}
