package com._202510007517.major_assignment.arch;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformVersionAlignmentTest {

    @Test
    void pom_versions_and_architecture_document_must_align() throws Exception {
        Path repoRoot = repoRoot();
        Document parentPom = readXml(repoRoot.resolve("parent-pom/pom.xml"));
        String bootVersion = textContent(parentPom, "spring-boot.version");
        String cloudVersion = textContent(parentPom, "spring-cloud.version");

        assertEquals("3.5.3", bootVersion, "parent-pom must lock expected Spring Boot baseline");
        assertFalse(cloudVersion.isBlank(), "parent-pom must declare spring-cloud.version");

        List<String> mismatches = new ArrayList<>();
        try (var paths = Files.walk(repoRoot)) {
            for (Path pom : paths.filter(path -> path.getFileName().toString().equals("pom.xml"))
                    .filter(path -> !path.toString().contains("\\target\\"))
                    .toList()) {
                Document document = readXml(pom);
                assertPropertyIfPresent(document, pom, "spring-boot.version", bootVersion, mismatches);
                assertPropertyIfPresent(document, pom, "spring-cloud.version", cloudVersion, mismatches);
            }
        }

        String architecture = Files.readString(repoRoot.resolve("docs/architecture.md"));
        assertTrue(Pattern.compile("\\|\\s*Spring Boot\\s*\\|\\s*" + Pattern.quote(bootVersion) + "\\s*\\|")
                        .matcher(architecture).find(),
                "docs/architecture.md must declare the same Spring Boot version as parent-pom");
        assertTrue(Pattern.compile("\\|\\s*Spring Cloud\\s*\\|\\s*" + Pattern.quote(cloudVersion) + "\\s*\\|")
                        .matcher(architecture).find(),
                "docs/architecture.md must declare the same Spring Cloud version as parent-pom");

        assertTrue(mismatches.isEmpty(), () -> "POM version mismatches: " + mismatches);
    }

    private static void assertPropertyIfPresent(Document document,
                                                Path pom,
                                                String propertyName,
                                                String expected,
                                                List<String> mismatches) {
        String actual = textContent(document, propertyName);
        if (!actual.isBlank() && !expected.equals(actual)) {
            mismatches.add(repoRoot().relativize(pom) + " -> " + propertyName + "=" + actual);
        }
    }

    private static Document readXml(Path path) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder().parse(path.toFile());
    }

    private static String textContent(Document document, String tagName) {
        NodeList nodes = document.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return "";
        }
        return nodes.item(0).getTextContent().trim();
    }

    private static Path repoRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("parent-pom/pom.xml"))
                    && Files.isRegularFile(current.resolve("docs/architecture.md"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate repository root from " + System.getProperty("user.dir"));
    }
}
