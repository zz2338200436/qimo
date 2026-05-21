package com._202510007517.major_assignment.arch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CiWorkflowStructureTest {

    @Test
    void ci_workflow_must_define_required_stage_chain() throws IOException {
        Path repoRoot = repoRoot();
        Path workflow = repoRoot.resolve(".github/workflows/ci.yml");
        assumeTrue(Files.isRegularFile(workflow),
                () -> "Skipping workflow structure check because "
                        + repoRoot.relativize(workflow).toString().replace('\\', '/')
                        + " is not present in this checkout.");

        String yaml = Files.readString(workflow);
        List<String> stages = List.of(
                "lint",
                "unit",
                "pbt",
                "integration",
                "contract",
                "scan",
                "docker-build",
                "docker-push",
                "deploy-test",
                "e2e-smoke");

        Map<String, String> expectedNeeds = new LinkedHashMap<>();
        expectedNeeds.put("unit", "lint");
        expectedNeeds.put("pbt", "unit");
        expectedNeeds.put("integration", "pbt");
        expectedNeeds.put("contract", "integration");
        expectedNeeds.put("scan", "contract");
        expectedNeeds.put("docker-build", "scan");
        expectedNeeds.put("docker-push", "docker-build");
        expectedNeeds.put("deploy-test", "docker-push");
        expectedNeeds.put("e2e-smoke", "deploy-test");

        for (String stage : stages) {
            assertTrue(yaml.contains(stage + ":"),
                    () -> "Workflow must define stage job `" + stage + "`");
        }
        for (Map.Entry<String, String> entry : expectedNeeds.entrySet()) {
            Pattern block = Pattern.compile(
                    "(?m)^\\s*" + Pattern.quote(entry.getKey()) + ":\\R\\s+needs:\\s+" + Pattern.quote(entry.getValue()) + "\\b");
            assertTrue(block.matcher(yaml).find(),
                    () -> "Workflow stage `" + entry.getKey() + "` must depend on `" + entry.getValue() + "`");
        }
    }

    private static Path repoRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("deploy"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate repository root from " + System.getProperty("user.dir"));
    }
}
