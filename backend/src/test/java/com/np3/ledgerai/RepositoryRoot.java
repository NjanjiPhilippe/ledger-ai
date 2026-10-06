package com.np3.ledgerai;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The tests run from backend/, while shared files (docker-compose.yml, keycloak/, docs/) live at the repository
 * root: walk up until docker-compose.yml is found instead of assuming the working directory.
 */
public final class RepositoryRoot {

    private static final Path ROOT = find();

    private RepositoryRoot() {
    }

    public static Path path() {
        return ROOT;
    }

    private static Path find() {
        Path start = Path.of("").toAbsolutePath();
        for (Path dir = start; dir != null; dir = dir.getParent()) {
            if (Files.exists(dir.resolve("docker-compose.yml"))) {
                return dir;
            }
        }
        throw new IllegalStateException("docker-compose.yml not found in or above " + start);
    }
}
