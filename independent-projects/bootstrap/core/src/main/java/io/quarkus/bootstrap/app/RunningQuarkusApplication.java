package io.quarkus.bootstrap.app;

import java.util.Optional;

public interface RunningQuarkusApplication extends AutoCloseable {
    ClassLoader getClassLoader();

    @Override
    void close() throws Exception;

    <T> Optional<T> getConfigValue(String key, Class<T> type);
}
