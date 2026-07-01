package com._202510007517.platform.agent.support;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public final class ConfigDocumentPathResolver {

    private ConfigDocumentPathResolver() {
    }

    public static Optional<Path> resolve(String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            return Optional.empty();
        }
        try {
            Path rawPath = Path.of(configuredPath);
            if (rawPath.isAbsolute()) {
                return Optional.of(rawPath.normalize());
            }
            Set<Path> candidates = new LinkedHashSet<>();
            Path workingDirectory = Path.of("").toAbsolutePath().normalize();
            candidates.add(workingDirectory.resolve(rawPath).normalize());
            Path parent = workingDirectory.getParent();
            if (parent != null) {
                candidates.add(parent.resolve(rawPath).normalize());
            }
            candidates.add(workingDirectory.resolve("agent-service").resolve(rawPath).normalize());
            return candidates.stream()
                    .filter(Files::exists)
                    .findFirst()
                    .or(() -> candidates.stream().findFirst());
        } catch (InvalidPathException ex) {
            return Optional.empty();
        }
    }
}
