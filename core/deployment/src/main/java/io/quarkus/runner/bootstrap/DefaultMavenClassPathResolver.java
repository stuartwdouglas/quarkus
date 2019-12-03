package io.quarkus.runner.bootstrap;

import java.nio.file.Path;

/**
 * A class that resolves the class path if no explicit class path information is provided
 */
public class DefaultMavenClassPathResolver implements QuarkusClassPathResolver {

    @Override
    public QuarkusClassPath resolveClassPath(Path applicationRoot) {
        return null;

    }
}
