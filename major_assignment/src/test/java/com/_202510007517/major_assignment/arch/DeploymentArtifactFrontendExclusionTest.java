package com._202510007517.major_assignment.arch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DeploymentArtifactFrontendExclusionTest {

    private static final Pattern MODULE_PATTERN = Pattern.compile("<module>([^<]+)</module>");
    private static final Set<String> FORBIDDEN_SUFFIXES = Set.of(".html", ".js", ".css", ".vue", ".tsx");
    private static final Set<String> JAR_NAME_EXCLUDES = Set.of("-sources.jar", "-javadoc.jar", "-tests.jar", "-stubs.jar");

    @Test
    void service_build_outputs_must_not_contain_frontend_assets() throws IOException {
        Path repoRoot = repoRoot();
        List<String> violations = new ArrayList<>();

        for (String moduleName : moduleNames(repoRoot)) {
            Path moduleRoot = repoRoot.resolve(moduleName);
            violations.addAll(targetClassesViolations(repoRoot, moduleRoot));
            violations.addAll(packagedJarViolations(repoRoot, moduleRoot));
        }

        assertTrue(violations.isEmpty(), () -> "Service build outputs must not contain frontend assets:%n%s"
                .formatted(String.join(System.lineSeparator(), violations)));
    }

    private static List<String> targetClassesViolations(Path repoRoot, Path moduleRoot) throws IOException {
        Path classesDir = moduleRoot.resolve("target/classes");
        if (!Files.isDirectory(classesDir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(classesDir)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(DeploymentArtifactFrontendExclusionTest::isForbiddenAsset)
                    .map(path -> "classes:" + normalize(repoRoot, path))
                    .toList();
        }
    }

    private static List<String> packagedJarViolations(Path repoRoot, Path moduleRoot) throws IOException {
        Path targetDir = moduleRoot.resolve("target");
        if (!Files.isDirectory(targetDir)) {
            return List.of();
        }

        List<Path> jars;
        try (Stream<Path> files = Files.list(targetDir)) {
            jars = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .filter(path -> isRuntimeJar(path.getFileName().toString()))
                    .toList();
        }

        List<String> violations = new ArrayList<>();
        for (Path jar : jars) {
            try (JarFile jarFile = new JarFile(jar.toFile())) {
                jarFile.stream()
                        .filter(entry -> !entry.isDirectory())
                        .map(entry -> entry.getName())
                        .filter(DeploymentArtifactFrontendExclusionTest::isForbiddenAsset)
                        .map(entry -> "jar:" + normalize(repoRoot, jar) + "!" + entry)
                        .forEach(violations::add);
            }
        }
        return violations;
    }

    private static boolean isRuntimeJar(String jarName) {
        if (jarName.startsWith("original-")) {
            return false;
        }
        return JAR_NAME_EXCLUDES.stream().noneMatch(jarName::endsWith);
    }

    private static boolean isForbiddenAsset(Path path) {
        return isForbiddenAsset(path.getFileName().toString());
    }

    private static boolean isForbiddenAsset(String fileName) {
        String lower = fileName.toLowerCase();
        return FORBIDDEN_SUFFIXES.stream().anyMatch(lower::endsWith);
    }

    private static List<String> moduleNames(Path repoRoot) throws IOException {
        String pom = Files.readString(repoRoot.resolve("pom.xml"));
        Matcher matcher = MODULE_PATTERN.matcher(pom);
        List<String> modules = new ArrayList<>();
        while (matcher.find()) {
            modules.add(matcher.group(1).trim());
        }
        return modules;
    }

    private static String normalize(Path repoRoot, Path path) {
        return repoRoot.relativize(path).toString().replace('\\', '/');
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
