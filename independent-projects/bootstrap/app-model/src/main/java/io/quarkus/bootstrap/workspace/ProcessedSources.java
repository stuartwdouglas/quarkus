package io.quarkus.bootstrap.workspace;

import java.nio.file.Path;

public interface ProcessedSources {

    Path getSourceDir();

    Path getDestinationDir();

    default <T> T getValue(Object key, Class<T> type) {
        return null;
    }
}
